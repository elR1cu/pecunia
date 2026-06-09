# ADR-0023: OpenAPI Schema-First Pipeline

## Status

Accepted

## Context

Pecunia is a monorepo with a Java backend (`apps/api`) and an Angular
frontend (`apps/frontend`) that must share a single, authoritative API
contract. The contract drives backend controller signatures, frontend
HTTP client code, and the documentation surface (Swagger UI) used during
development.

Several questions are entangled in the choice of pipeline.

### Schema-first versus code-first

In a **code-first** workflow, the source of truth is the Java code: the
controllers and their annotations. A library like Springdoc introspects
the running application and produces an OpenAPI document. Consumers
(frontend codegen, external docs) fetch this document from a runtime
endpoint.

In a **schema-first** workflow, the source of truth is a hand-edited
OpenAPI document. The backend generates Spring interfaces and DTOs from
that document at build time; controllers must implement the generated
interfaces. The frontend generates its HTTP client from the same
document. The document evolves through pull requests, not as a
side-effect of code.

Schema-first imposes a planning step before each new endpoint, but it
makes API drift impossible to introduce silently: a controller that no
longer matches the spec fails to compile.

### Where the contract lives

The spec is shared by backend and frontend, so colocating it inside
`apps/api/src/main/resources/` would entangle two consumers with one
provider's folder. A repo-root location (`contracts/openapi.yaml`) is
neutral and matches the monorepo's logical structure (ADR-0002).

### How the contract is exposed at runtime

Two distinct concerns:

1. **The spec itself**, served as a file so the frontend codegen and
   external tools can fetch it.
2. **Swagger UI**, an interactive HTML rendering of the spec, useful
   during development but unnecessary in production.

Both can be served by Springdoc, or they can be decoupled: the spec
served as a static file (independent of Springdoc), the UI activated
only in dev.

### Spring Boot 4 / Springdoc 3.x interaction

Spring Boot 4 requires Springdoc 3.x (the 2.x line targets Boot 3).
Springdoc 3.0.3 was the stable version at the time of this decision.
Its `springdoc.api-docs.enabled` flag is documented as controlling the
`/v3/api-docs` endpoint only, suggesting that `swagger-ui.enabled` is
independent. In practice, large parts of the Swagger UI autoconfiguration
in Springdoc 3.0.3 are gated on `api-docs.enabled=true`. Disabling
api-docs while enabling swagger-ui produces an effectively non-functional
UI (404 on the entry endpoint, Swagger UI bootstrap fails on missing
`/v3/api-docs/swagger-config`).

## Decision

Adopt schema-first OpenAPI with `contracts/openapi.yaml` as the single
source of truth. Generate backend artifacts at build time. Serve the
spec as a static file. Expose Swagger UI only under the `dev` Spring
profile.

### Spec location

```
contracts/openapi.yaml
```

Versioned in Git. Edited by hand. No tooling writes back to this file.

### Backend codegen

Plugin: `openapi-generator-maven-plugin` 7.22.0.

Configuration highlights:

| Option | Value | Reason |
|---|---|---|
| `generatorName` | `spring` | Emits Spring controller interfaces and DTOs. |
| `useSpringBoot4` | `true` | Targets the Boot 4 baseline (also enables `useJakartaEe`). |
| `useJackson3` | `true` | Aligns with the Jackson 3 shipped by Boot 4. |
| `skipDefaultInterface` | `true` | Strips mock `default` method bodies that reference `ApiUtil` (otherwise required by `generateSupportingFiles=true`). |
| `generateSupportingFiles` | `false` | The project does not need the generator's stub `ApiUtil`. |
| Records | not used | The `spring` generator does not emit Java records (openapi-generator issue #10490, open since 2021). POJOs accepted as the cost of stability. |

Each OpenAPI tag is wired as a separate `<execution>` so that future
splits along bounded contexts remain mechanical.

### Static spec serving

`maven-resources-plugin` copies `contracts/openapi.yaml` to
`target/classes/static/openapi.yaml` at build time. Spring's default
static resource handler (registered by `WebMvcAutoConfiguration` for
`classpath:/static/`) serves it at `/openapi.yaml` without any
controller code or Springdoc involvement.

This independence matters: the frontend codegen, external clients, and
Swagger UI all reach the same static document. If Springdoc is upgraded
or removed, the URL keeps working.

### Swagger UI under the dev profile only

`springdoc-openapi-starter-webmvc-ui:3.0.3` is on the classpath.

`application.yml` (the production default) disables Springdoc entirely:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
    url: /openapi.yaml
    disable-swagger-default-url: true
```

`application-dev.yml` re-enables both flags:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

Both flags are required in dev because Springdoc 3.0.3's
`swagger-ui` autoconfiguration is internally gated on
`api-docs.enabled` (see Context). Setting only `swagger-ui.enabled=true`
yields a broken UI.

`swagger-ui.url: /openapi.yaml` tells the UI to fetch the static spec
instead of the runtime-generated `/v3/api-docs`. The runtime endpoint
still exists in dev but is not consumed by the UI.
`disable-swagger-default-url: true` prevents the UI from falling back to
its own default URL.

### Security wiring for the dev profile

A second `SecurityFilterChain` (`SwaggerSecurityConfig`,
`@Profile("dev")`, `@Order(0)`) permits anonymous access to
`/swagger-ui/**`, `/swagger-ui.html`, `/openapi.yaml`, and
`/v3/api-docs/swagger-config`. The last path is the Swagger UI bootstrap
endpoint; without it, the UI cannot initialize even in dev.

The runtime spec at `/v3/api-docs` is intentionally not in the matcher.
It falls back to the main chain (authenticated) so an unauthenticated
caller cannot retrieve the runtime-introspected spec, even in dev.

### Frontend codegen (deferred)

The Angular frontend will generate its HTTP client from the same
`contracts/openapi.yaml` via an `npm` script invoking the
OpenAPI Generator CLI. Out of scope for Block 1.

## Consequences

### Positive

- **Drift is a compile error.** Adding a controller method without the
  matching spec change fails the Maven build because the generated
  interface no longer matches.
- **Single source of truth** for backend, frontend, and documentation
  consumers. No parallel evolution of three artifacts.
- **The spec lives outside any consumer's tree.** The repo root
  `contracts/` directory matches the spec's role as a shared
  contract, not as backend resource.
- **Springdoc fully disabled in production.** No autoconfiguration,
  no exposed runtime endpoint, no overhead. Only the static
  `/openapi.yaml` is reachable, and only by authenticated callers
  (the main chain protects it as any non-Swagger path).
- **Swagger UI integration is reversible.** Springdoc 3.0.3 sits on
  the prod classpath but contributes nothing. Removing it later is a
  pom edit, no controller or service code depends on it.

### Negative

- **Springdoc jar in production artifact.** A few hundred KB of unused
  code. Acceptable for the MVP's deployment target.
- **POJOs instead of records.** The `spring` generator does not emit
  records (issue #10490). Domain entities and value objects use records
  freely; DTOs do not. This is a deliberate boundary, but it creates a
  small asymmetry between transport and domain.
- **Springdoc gotcha is undocumented.** The need to enable
  `api-docs.enabled` for Swagger UI to function is not in the official
  docs and was discovered empirically. A future contributor unaware of
  this ADR may be misled by the doc into setting `api-docs.enabled=false`
  and breaking the dev UI.
- **Two specs coexist in dev.** The static `/openapi.yaml` (source of
  truth) and the Springdoc-generated `/v3/api-docs` (runtime
  introspection). They can diverge if a controller emits annotations
  not reflected in the static spec. The UI ignores the runtime spec, so
  the divergence is silent and harmless, but a curious dev fetching
  `/v3/api-docs` may be confused by the difference.

### Neutral

- **Static serving is independent of Springdoc.** Replacing Springdoc
  with another OpenAPI tool would not affect `/openapi.yaml`.
- **Build determinism.** Codegen runs in `generate-sources` and the
  resource copy runs in `process-resources`. IntelliJ's native
  compiler may not trigger Maven goals on every IDE build; a full
  `mvn package` (or `mvn process-resources`) is required to refresh
  generated sources and the static spec after spec edits.

## Alternatives Considered

### Code-first via Springdoc runtime introspection

Generate the OpenAPI document from controller annotations at runtime,
serve it at `/v3/api-docs`, expose Swagger UI directly from it.

Rejected because:

- The contract becomes implicit in code: there is no canonical artifact
  to review in a PR before the implementation lands.
- Frontend codegen would have to fetch from a running backend, making
  the build pipeline depend on a live service.
- Schema-first contract negotiation (designing the API before writing
  code) becomes impossible.

### Spec colocated under `apps/api/src/main/resources/`

Keep `openapi.yaml` inside the backend module so Spring serves it
natively without `maven-resources-plugin`.

Rejected because:

- The spec is a shared artifact between backend and frontend; placing it
  under `apps/api` privileges one consumer.
- Frontend codegen would have to reach across module boundaries with a
  relative path, weakening monorepo hygiene.

### Springdoc always-on in production

Set `springdoc.api-docs.enabled: true` in `application.yml`, accepting
`/v3/api-docs` as a publicly-reachable endpoint in prod.

Rejected because:

- The static `/openapi.yaml` already serves the same purpose, with the
  source of truth being the file in Git rather than runtime
  introspection.
- Even with the path protected by the main chain, exposing it as a
  controller adds attack surface for no benefit.

### `openapi-generator` records preview templates (Chrimle)

Third-party Mustache templates that emit Java records instead of POJOs.

Rejected because:

- Preview-grade, not maintained by the openapi-generator core team.
- Risk of breakage on each `openapi-generator` version bump.
- The asymmetry between record-based domain and POJO-based DTOs is
  acceptable; mapping happens through MapStruct (see ADR-0024).

Revisit if record support lands in `openapi-generator` proper.

### `useJackson3: false`

Generate Jackson 2 annotations to match older Boot baselines.

Rejected: Spring Boot 4 ships Jackson 3. A mismatch between the
generator's emitted annotations and the runtime Jackson would cause
serialization edge cases that are not worth catching at runtime.

## References

- `contracts/openapi.yaml` — the spec itself
- `apps/api/pom.xml` — codegen and resource copy configuration
- `apps/api/src/main/resources/application.yml` and
  `application-dev.yml` — Springdoc configuration
- `apps/api/src/main/java/com/pecunia/shared/security/SwaggerSecurityConfig.java`
  — dev-only security chain for Swagger paths
- [openapi-generator-maven-plugin documentation](https://openapi-generator.tech/docs/plugins/#maven)
- [openapi-generator issue #10490 — records support in the `spring` generator](https://github.com/OpenAPITools/openapi-generator/issues/10490)
- [Springdoc reference documentation](https://springdoc.org/)
- [ADR-0002](0002-monorepo-over-multi-repo.md) — monorepo structure
- [ADR-0005](0005-schema-first-api-design.md) — schema-first API design (the principle this ADR implements concretely)
- [ADR-0021](0021-actuator-endpoints-and-security.md) — actuator endpoints and security policy
- [ADR-0022](0022-csrf-spa-shortcut.md) — CSRF configuration via the `spa()` shortcut
- [ADR-0024](0024-mapstruct-mapper-convention.md) — MapStruct mapper convention (consumes the DTOs generated here)

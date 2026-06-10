# Pecunia — Work Journal

Personal journal capturing progress, decisions in-the-moment, learnings,
and blockers throughout the project.

Format: one entry per work session, most recent first.

Each entry follows this structure:
- **Date**
- **Block / Task**: which block or task was worked on.
- **Done**: what was accomplished.
- **Learned**: any technical or process insights.
- **Next**: what comes next.
- **Notes**: blockers, doubts, ideas to revisit.

---

## Template (copy for new entries)

### YYYY-MM-DD — Session N

**Block / Task**: [e.g., Block 0 — Foundations, Setup repository]

**Done**:
- [Item 1]
- [Item 2]

**Learned**:
- [Insight or new knowledge]

**Next**:
- [Planned next action]

**Notes**:
- [Any blockers, doubts, ideas]

---

## Entries

---

### 2026-06-10 — Session 08

**Block / Task**: Block 1 — Secured Skeleton, logging substep (sub-sessions A + B)

**Done**:
- Sub-session A: enabled Spring Boot 4 native structured logging (`logging.structured.format.console: logstash`, global custom field `service: pecunia-api`); dev profile     
  overrides to empty to keep the colored text pattern. Two commits.
- Sub-session B: Micrometer Tracing wired via `spring-boot-starter-opentelemetry` (after a failed first attempt with the bare bridge — SB4 autoconfig split bit us), sampling
  forced to 1.0, OTLP exporters disabled for traces / logs / metrics under the SB4 asymmetric namespaces, dev pattern enriched with `[app,traceId,spanId]` plus `%kvp` after the
  message to render SLF4J 2.x fluent `addKeyValue` pairs. `MeController` emits its first non-PII structured log. Two commits.
- ADR-0018 revised **three times** in the session: encoder (native vs `logstash-logback-encoder`), correlation (Micrometer Tracing vs custom `RequestIdFilter`), dependency   
  (starter vs bare bridge). Each revision triggered by web verification after I had proposed a pre-SB4 pattern.
- Sub-session C (sanitization) scoped and time-estimated: C-min ~2h, C-full ~3h30–4h. Recommendation C-min — concrete IBAN / amount patterns deferred to Block 2 / Block 3    
  when the data actually appears.

**Learned**:
- Spring Boot 3.4+ ships native structured logging (`logging.structured.format.console=logstash`) with the same JSON wire format as `logstash-logback-encoder`, without the   
  dependency. Spring Boot 4 carries it forward.
- Micrometer Tracing populates `traceId` / `spanId` in the MDC automatically once a bridge (OTel or Brave) is on the classpath. No custom `RequestIdFilter` needed.
- Spring Boot 4 split autoconfigurations (cf. memory `project-spring-boot-4-autoconfig-modules`): `micrometer-tracing-bridge-otel` alone brings the runtime but not the       
  autoconfig. The `spring-boot-starter-opentelemetry` starter is the one-line SB4 path.
- SB4 namespace asymmetry: `management.opentelemetry.*.export.otlp.*` for traces and logs (OTel SDK), `management.otlp.metrics.export.*` for metrics (Micrometer              
  `OtlpMeterRegistry`). Intentional and historical.
- OTLP = OpenTelemetry Protocol, vendor-neutral wire format (protobuf over gRPC :4317 or HTTP :4318) that supersedes proprietary ingestion protocols (Jaeger Thrift, Zipkin   
  B3, Datadog API).
- SLF4J 2.x fluent API: `log.atInfo().addKeyValue(k, v).log(msg)` separates structured payload from message. Spring Boot rejected adding `%kvp` to the default pattern for    
  security reasons — opt-in required in dev.
- The span starts in `ServerHttpObservationFilter`. Logs emitted before this filter (startup banner, scheduler, non-propagated threads) legitimately have no `traceId`.       
  `%X{traceId:-}` renders them as empty rather than crashing.

**Next**:
- Push the branch (5 commits ahead of origin).
- Sub-session C — sanitization at C-min scope (~2h): `SensitiveDataLoggingCustomizer` + one demonstrative test + minimal `logback-spring.xml` + ADR-0018 follow-up.
- Angular skeleton — the gating item for the Block 1 exit criterion once the backend logging story is closed.

**Notes**:
- 5 commits on the branch: `feat(api): enable native structured JSON logging`, `docs: revise ADR-0018 to use Spring Boot 4 native structured logging`, `docs: use Micrometer  
  Tracing for correlation IDs in ADR-0018`, `docs: switch ADR-0018 to spring-boot-starter-opentelemetry`, `feat(api): enable Micrometer Tracing for log correlation IDs`.
- Spring Boot bug #49304 noted in ADR-0018: the `enabled: false` OTLP flags may not fully suppress export attempts. Invisible at Block 1 (no collector reachable). Re-check at
  Block 9.
- Logback 1.5.22+ auto-masks variables whose names contain `password`, `secret`, `confidential`. Worth leaning on as a free safety net in Sub-session C.

See [detailed recap](docs/session-recaps/2026-06/2026-06-10-session-08.md).

### 2026-06-09 — Session 07

**Block / Task**: Block 1 — Secured Skeleton, step 7/7 (DelegatingAuthenticationEntryPoint, SwaggerSecurityConfig, ADR-0023, ADR-0024)

**Done**:
- Reverted a `MeController` Optional-chain refactor back to Session 06's pattern-matching form after re-litigating                        
  Optional-vs-pattern-matching for invariant extraction.
- Wired `DelegatingAuthenticationEntryPoint` on the main chain via Spring Security 7's `.builder()` (after self-correcting from the       
  deprecated `LinkedHashMap` pattern). `MediaTypeRequestMatcher(APPLICATION_JSON)` with `useEquals(true)` and                               
  `ignoredMediaTypes(MediaType.ALL)` routes AJAX → 401 and browser → 302 to `/oauth2/authorization/pecunia`. Main chain marked `@Order(1)`.
- New `SwaggerSecurityConfig` (`@Profile("dev")`, `@Order(0)`): second `SecurityFilterChain` matching `/swagger-ui/**`,                   
  `/swagger-ui.html`, `/openapi.yaml`, `/v3/api-docs/swagger-config` with `permitAll`, CSRF disabled, stateless. Slice tests in two classes
  (parameterized dev + default-profile).
- End-to-end walk-through validated (logged-in 200, AJAX 401, browser 302, Swagger UI 200 in private browsing). Surfaced two Springdoc    
  3.0.3 gotchas during the walk-through: `api-docs.enabled` gates `swagger-ui` autoconfig (fixed by enabling only in `application-dev.yml`),
  and `/v3/api-docs/swagger-config` must be reachable without auth (added to the matcher).
- ADR-0023 (OpenAPI schema-first pipeline) and ADR-0024 (MapStruct mapper convention) drafted and committed.

**Learned**:
- `DelegatingAuthenticationEntryPoint`'s `LinkedHashMap` constructor + `setDefaultEntryPoint` is `@Deprecated(forRemoval = true)` in      
  Spring Security 7; the modern form is `(default, List<RequestMatcherEntry>)`, and `.builder()` is the most idiomatic.
- Springdoc 3.0.3 gates a chunk of `swagger-ui` autoconfig on `api-docs.enabled=true` despite docs implying independence. Setting         
  `api-docs.enabled=false` in main config and overriding in `application-dev.yml` keeps prod minimal.
- `/v3/api-docs/swagger-config` is the Swagger UI bootstrap endpoint; the UI can't initialize without it reachable.
- `WebMvcAutoConfiguration` registers static resource serving even in `@WebMvcTest` slices, so `/openapi.yaml` returns 200 from           
  `target/classes/static/` if `maven-resources-plugin` has run.
- `@WebMvcTest(controllers = {})` does NOT mean "no controllers" — the empty default means "include all".

**Next**:
- Push the branch (4 commits ahead).
- Logging (ADR-0018): still pending choice.
- Angular skeleton + login redirect — required for the formal Block 1 exit criterion.

**Notes**:
- 4 commits sur la branche : `feat(api): delegate authentication entry point on Accept header` / `feat(api): expose Swagger UI under dev  
  profile` / `test(api): cover delegating entry point and swagger security` / `docs: add ADR-0023 and ADR-0024`.
- Le commentaire de classe sur `SwaggerSecurityConfig` est placé entre annotations et déclaration — convertir en Javadoc reste une        
  coquetterie en suspens.

See [detailed recap](docs/session-recaps/2026-06/2026-06-09-session-07.md).

### 2026-06-08 — Session 06

**Block / Task**: Block 1 — Secured Skeleton, step 7/7 (MeController, MapStruct pipeline, first slice tests)

**Done**:
- Wrote `MeController` with pattern matching + `throw IllegalStateException` for principal extraction; converged on assertive (not defensive) coding through several review iterations, with IntelliJ NPE warnings resolved cleanly by Java 25 pattern matching.
- Introduced MapStruct as the project's mapper convention (Option A): pom wiring (`1.6.3`), `CurrentUserMapper` interface with `componentModel = "spring"` and `ReportingPolicy.ERROR`, method named `toDto` (establishing the `toDto` / `toEntity` / `toDomain` convention), `expression = "java(...)"` escape hatch for the multi-source             
  `displayName`.
- Added Lombok to the pom (selective per ADR-0013) with annotation processor ordered before MapStruct; `@RequiredArgsConstructor` on `MeController`.
- Enriched the OpenAPI contract: added `emailVerified` (required boolean, fallback to `false` in mapper) and `description` on all `CurrentUser` fields.
- First slice test `MeControllerTest`: `@WebMvcTest` + `@Import({SecurityConfig.class, CurrentUserMapperImpl.class})` + `oidcLogin()`, 4 scenarios covering the happy path and the two mapper fallbacks. `@DisplayName` for human-readable labels; constants extracted to `SUBJECT_UUID` and `StandardClaimNames.*`.

**Post-session follow-up**:
- Silenced the Mockito self-attach warning: wired `maven-surefire-plugin` with `-javaagent:${org.mockito:mockito-core:jar}` (path exposed by the `maven-dependency-plugin` `properties` goal). Future-proofs against the JDK change that will forbid dynamic agent attach.
- Re-added `@MockitoBean ClientRegistrationRepository` to `MeControllerTest` to short-circuit `OAuth2ClientAutoConfiguration`'s OIDC discovery against Keycloak at context load. Reverses the in-session "unnecessary mock removed" call — it's needed for CI safety and slice hermeticity.
- Unchecked **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → "Delegate IDE build/run actions to Maven"** in IntelliJ. IntelliJ now uses its native incremental compiler, which lets `automake` regenerate `target/classes` while the app runs — the trigger DevTools watches for. Hot reload from Session 03's TODO finally works. Side effect: `spring-boot:build-info` no longer runs on every IDE build, so `/actuator/info` may serve stale `time`/`version` until the next `mvn` invocation. Acceptable in dev.

**Learned**:
- Spring Boot 4 split test slice autoconfigs the same way it split runtime autoconfigs: `@WebMvcTest` moved to `org.springframework.boot.webmvc.test.autoconfigure`, pulled via `spring-boot-starter-webmvc-test`. Same pattern for `spring-boot-starter-security-test`.
- `Authentication` wraps the `OidcUser`; the principal lives in `.getPrincipal()`. Casting the wrapper directly was the original bug.
- Assertive coding (`throw IllegalStateException`) is the right shape for invariants the `SecurityFilterChain` already guarantees — defensive `Optional` chains hide bugs.
- IntelliJ's flow analysis propagates non-null through `Objects.requireNonNull` but not always through a cast; pattern matching with `instanceof` is recognized natively and gives the warning-free path.
- `Optional.orElse(...)` is eager, `.orElseGet(...)` is lazy — choose by whether the fallback is a constant or a computation.
- MapStruct doesn't elegantly handle multi-source field computation; `expression = "java(...)"` is the least-ugly escape hatch. The generated impl uses setters, not the required-args constructor — implication for Jakarta Validation.
- `oidcLogin()` builds an `OAuth2AuthenticationToken` with `DefaultOidcUser` from configured claims; bypasses the actual OAuth2 flow but populates the `SecurityContext`. `StandardClaimNames` exposes canonical OIDC claim constants — use them instead of string literals.

**Next**:
- Add `DelegatingAuthenticationEntryPoint` to `SecurityConfig` (`Accept: application/json` → 401, fallback → 302). Update the 4th slice test accordingly.
- Write `SwaggerSecurityConfig` (`@Profile("dev")`, second `SecurityFilterChain`, `@Order(0)`, `permitAll` on Swagger paths).
- End-to-end validation: activate dev profile locally, walk through login, `/me` 200, fetch logged-out 401, browser logged-out 302, Swagger UI loads.

**Notes**:
- ADR-0023 still pending; this session added more material for it (MapStruct convention, `toDto` naming, `ReportingPolicy.ERROR`).
- Memory `project-spring-boot-4-autoconfig-modules` should be extended to cover test slice autoconfigs (same per-feature module pattern).
- Logging setup deferred. Options laid out, no choice made yet.

See [detailed recap](docs/session-recaps/2026-06/2026-06-08-session-06.md).

### 2026-06-07 — Session 05

**Block / Task**: Block 1 — Secured Skeleton, step 7/7 (OpenAPI codegen + Springdoc pipeline)

**Done**:
- Verified Session 04 TODOs: `/actuator/info` returns the full `build` block, `SecurityConfig` typo fixed.
- Settled the design choices for `GET /me` (schema `CurrentUser`, 4 required fields, `id` = Keycloak `sub` UUID v4 for MVP, refactor to   
  internal UserId v7 in Block 2).
- Research on current state of `openapi-generator-maven-plugin` 7.22.0 and Springdoc 3.0.3: confirmed `useSpringBoot4` / `useJackson3`,   
  confirmed records are NOT natively supported by the `spring` generator (issue #10490 open since 2021), MapStruct 1.6 supports both POJOs  
  and records via canonical constructor.
- Wired `openapi-generator-maven-plugin` (one `<execution>` per tag, scoped to `Identity`) + `maven-resources-plugin` (copies             
  `contracts/openapi.yaml` → `target/classes/static/`) + `springdoc-openapi-starter-webmvc-ui:3.0.3` dependency in `apps/api/pom.xml`.      
  Springdoc configured for static-spec serving (`api-docs.enabled: false`, `documentationProvider: none`).
- Created `contracts/openapi.yaml` (3.1.0) with the `GET /me` operation, `CurrentUser` schema, and `bffSession` security scheme (cookie   
  SESSION).
- Established the dev-profile convention: `application-dev.yml` enables Swagger UI, `spring.profiles.active=dev` documented in            
  `.env.example`.
- `mvn clean compile`: BUILD SUCCESS after adding `skipDefaultInterface: true` (fix for `ApiUtil` symbol-not-found when supporting files  
  are disabled).

**Learned**:
- The `spring` generator has no record option; only the Chrimle preview templates produce them. Stable default stays POJOs.
- `useSpringBoot4` + `useJackson3` are the right flags for Boot 4; enabling `useSpringBoot4` also enables `useJakartaEe`.
- `skipDefaultInterface: true` strips the default mock method bodies that reference `ApiUtil` — required combo with                       
  `generateSupportingFiles: false`.
- BFF pattern: `DelegatingAuthenticationEntryPoint` discriminates by `Accept` header to return 401 for AJAX and 302 for browser           
  navigation. Without it, a `fetch('/me')` while unauthenticated receives unusable Keycloak HTML.
- Java override constraint: you cannot add `@AuthenticationPrincipal OidcUser` as a parameter to an override of a generated interface     
  method — must use `SecurityContextHolder` to retrieve the principal.
- Spring Boot 4 needs Springdoc 3.x (currently 3.0.3); Springdoc 2.x targets Boot 3.

**Next**:
- Write `MeController` (`com.pecunia.identity.api`) implementing `IdentityApi`.
- Add `DelegatingAuthenticationEntryPoint` to the main `SecurityFilterChain` to align runtime behavior with the spec's documented 401.
- Write `SwaggerSecurityConfig` (`@Profile("dev")`), activate the dev profile in the local `.env`, restart, validate end-to-end (browser +
  Swagger UI).

**Notes**:
- ADR-0023 candidate to capture the OpenAPI pipeline decisions (B/C/D/E1/F1).
- OpenAPI 3.1 "beta" warning from the codegen is non-blocking; downgrade to 3.0.3 possible if it becomes noisy.
- The running app must be restarted to pick up the new dependencies and the static spec copy.

See [detailed recap](docs/session-recaps/2026-06/2026-06-07-session-05.md).

### 2026-06-06 — Session 04

**Block / Task**: Block 1 — Secured Skeleton, step 6/7 (`SecurityFilterChain` + ergonomie de build)

**Done**:
- Wrote `SecurityConfig` with the BFF chain: typed actuator allowlist (`EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class)`), OAuth2 login against Keycloak, OIDC RP-initiated logout, `SessionCreationPolicy.IF_REQUIRED`, and CSRF.
- Built CSRF manually first (`CookieCsrfTokenRepository` + plain handler + custom `CsrfCookieFilter`), then replaced the three pieces with `csrf(CsrfConfigurer::spa)` — Spring Security 6.5+ shortcut. Wrote ADR-0022 to capture the decision and the BREACH analysis behind the plain-handler choice.
- Wired Spotless with Palantir Java Format (Java + conservative `sortPom`). First attempt failed on Java 25 internals — bumped `palantir-java-format` to 2.91.0 (latest Maven Central). Picked `spotless-maven-plugin` 3.6.0 over 2.46.1 because Spotless docs flag the 2.x line as "JRE 11 only"; XML config unchanged. `check` bound to `verify`; `apply` manual.
- Diagnosed `mvn -pl apps/api …` as silently broken (no root pom by design). Replaced all 5 doc occurrences with `mvn -f apps/api/pom.xml …`.

**Learned**:
- `csrf.spa()` (Spring Security 6.5+) bundles `CookieCsrfTokenRepository.withHttpOnlyFalse()` + plain `CsrfTokenRequestAttributeHandler` + a built-in materialization filter. Replaces the entire historical three-part pattern.
- Spring Security 6+ uses a `DeferredCsrfToken`: the `XSRF-TOKEN` cookie is only written when something calls `CsrfToken#getToken()` during the request. Plain GETs never materialize it without help.
- BREACH attack only applies when the secret is rendered in a compressible response body — not the case in a BFF/SPA where the token lives in a cookie header. The Xor handler defends against an attack our architecture closes structurally.
- Palantir Java Format depends on internal JDK APIs (`com.sun.tools.javac.*`); a new JDK major can break older Palantir releases — pin and bump deliberately.
- `mvn -pl <module>` requires a reactor parent pom. No root pom = use `mvn -f apps/api/pom.xml …`.

**Next**:
- Commit the session work — likely split as (a) `feat(api): SecurityFilterChain`, (b) `build(api): wire Spotless`, (c) `docs: fix mvn invocation form`.
- Wire `BuildInfoContributor` so `/actuator/info` returns build metadata (pending since Session 03).
- Write the applicative `GET /me` controller to formally satisfy Block 1's exit criterion.

**Notes**:
- Minor typo in `SecurityConfig.java` line 30 comment ("what spa() bundle" → "bundles"). Cosmetic.

See [detailed recap](docs/session-recaps/2026-06/2026-06-06-session-04.md).

### 2026-06-05 — Session 03

**Block / Task**: Block 1 — Secured Skeleton, step 5/7 (Flyway baseline + dev workflow tooling)

**Done**:
- Wrote `V1__init.sql`, a portable baseline migration pinning the database timezone to UTC via `DO $$ … $$` + `current_database()`.
- Added `spring-boot-docker-compose` (`start-only` lifecycle, `optional: true`) so launching `PecuniaApplication` auto-starts Postgres, Keycloak, and Redis. Documented in ADR-0020.
- Added `spring-boot-devtools` for classpath-change restart, with a note on the two IntelliJ settings required to make it fire.
- Created `docs/dev-setup.md` (full local-dev guide) and updated `README.md` + `apps/api/.env.example` to point at it.
- Created ADR-0021 documenting the two-layer actuator policy (HTTP exposure allowlist + Spring Security tiered access) and updated `architecture.md` (Observability section, PostgreSQL 18 fix, restructure of the BFF H3 subsections under `## Authentication and Authorization`).
- Diagnosed Flyway silently not running (no `flyway_schema_history` table) and fixed it by replacing the direct `flyway-core` dep with `spring-boot-starter-flyway` (commit `706a940`).

**Learned**:
- Spring service connections are one abstraction shared by `spring-boot-docker-compose` and Testcontainers' `@ServiceConnection`.
- Keycloak has no built-in `ConnectionDetails` factory in Spring Boot 4 → its OIDC config stays driven by `application.yml`, only Postgres/Redis are silently overridden.
- DevTools requires a classpath change to fire, and IntelliJ does not recompile by default while an app is running.
- Spring Boot 4 split auto-configurations out of `spring-boot-autoconfigure` into per-feature modules (`spring-boot-flyway`, `spring-boot-data-jpa`, etc.). Declaring only the third-party lib gives the runtime code but no autoconfig — and no error log either. Rule of thumb: prefer `spring-boot-starter-<feature>` over the raw dep.

**Next**:
- Toggle the two IntelliJ DevTools settings and verify hot reload.
- Write `SecurityConfig` / `SecurityFilterChain` (target shape now specified by ADR-0021 + Session 02 BFF concept set).
- Wire `/actuator/info` to expose build-info metadata (the Maven goal is already active).

**Notes**:
- Repo root must be the working directory for both CLI and IntelliJ launches (both `apps/api/.env` and the compose file are referenced by relative paths).
- The Flyway gotcha shipped in the initial Session 03 commits — the bug was inert because there are no entities yet under `ddl-auto: validate`. Worth checking the same pattern for any future Spring-integrated tech I add (Cache, Mail, Batch, Quartz, etc.).

See [detailed recap](docs/session-recaps/2026-06/2026-06-05-session-03.md).

### 2026-06-03 — Session 02

**Block / Task**: Block 1 — Secured Skeleton, steps 4–5/7 (Keycloak realm import + Spring Security wiring)

**Done**:
- Verified the dev stack, then bumped Postgres 17→18, Keycloak 26.1→26.6, Redis 7→8 (with Postgres 18 volume layout fix and `KC_BOOTSTRAP_ADMIN_*` rename).
- Added Flyway, JPA, Postgres, Spring Security, OAuth2 Client and `spring-session-data-redis` to `apps/api/pom.xml`.
- Wrote `application.yml`: datasource, Flyway, Redis session, OAuth2 client registration against the `pecunia` realm via OIDC discovery.
- Sourced the BFF client secret strictly from `${PECUNIA_BFF_CLIENT_SECRET}` (no fallback); loaded from `apps/api/.env` via `spring.config.import`; `.env.example` documents the variable.
- Replaced the plaintext `testuser` password in `pecunia-realm.json` with the Argon2id hash actually produced by Keycloak 26.6.
- Committed everything as `feat(api): scaffold OIDC login via Keycloak realm and BFF config`.

**Learned**:
- `spring.session.store-type` was removed in Spring Boot 3.0 (M4); classpath detection now picks the store.
- Keycloak intentionally does not substitute env vars in realm import (issue #20199, "not planned").
- Keycloak 26.6 uses Argon2id (type `id`, v1.3) as the default password hash and accepts the same format on import.
- Postgres 18 changed its Docker volume layout: mount at `/var/lib/postgresql` rather than `/var/lib/postgresql/data`.

**Next**:
- Write `V1__init.sql` for Flyway.
- Implement `SecurityConfig`/`SecurityFilterChain` (BFF login, public health probes, CSRF cookie, OIDC logout).
- Boot the backend end-to-end against the docker-compose stack and walk through the login flow in a browser.

**Notes**:
- Account console "Something went wrong" on `/realms/pecunia/account` is a known quirk of minimal realm imports, not a blocker for the OAuth2 flow.
- `apps/api/.env` is gitignored; document the IntelliJ Run Configuration env-var setup at some point.

See [detailed recap](docs/session-recaps/2026-06/2026-06-03-session-02.md).

### 2026-06-01 — Session 01

**Block / Task**: Block 1 — Secured Skeleton, step 1/7 (backend scaffolding)

**Done**:
- Decided single-module Maven structure (Option A, consistent with architecture.md)
- Created `apps/api/pom.xml` with Spring Boot 4.0.6 BOM, Java 25, web + actuator
- Created `PecuniaApplication.java` and `application.yml` (graceful shutdown, actuator probes)
- Activated Actuator `build` info contributor via `build-info` Maven goal
- Configured persistent memory for BOM preference
- Added `/recap` and `/resume` Claude Code slash commands (`.claude/commands/`) for session management and context resumption

**Learned**:
- `spring-boot-dependencies` BOM gives more control than inheriting `spring-boot-starter-parent` — no forced inheritance, explicit plugin versions.
- Actuator `InfoContributor`s are disabled by default since Spring Boot 2.6; `build` contributor requires the `build-info` Maven goal and IDE delegating builds to Maven.
- `server.shutdown: graceful` is a zero-cost safety net for rolling deploys.

**Next**:
- Verify app starts and actuator endpoints respond.
- Configure IntelliJ to delegate build to Maven.
- Step 2: `docker-compose.dev.yml` (PostgreSQL 17, Keycloak, Redis).

**Notes**:
- Spring Boot 4.0.6 is GA. Maven Central search was unreliable for it — use mvnrepository.com to verify versions.

See [detailed recap](docs/session-recaps/2026-06/2026-06-01-session-01.md).

---

### 2026-05-27 — From Claude Desktop instructions

**Block / Task**: Block 0 — Foundations, repository setup

**Done**:
- Created the GitHub repository and pushed the initial documentation commit.
- Set up the monorepo structure: `apps/`, `contracts/`, `deploy/`, `docs/`, `samples/`.
- Added 4 additional ADRs (ADR-0016 through ADR-0019): ArchUnit, SonarCloud, logging strategy, RestClient.
- Repository is currently **private** — will be made public before Block 8 (production deployment) or earlier if the documentation is ready to be shown.

**Learned**:
- Nothing notable.

**Next**:
- Start Block 1: backend scaffolding (Spring Boot 4, pom.xml, application.yml).

**Notes**:
- The repository is private for now. Target: make it public once the codebase has at least Block 1 complete and CI is green.

---

### 2026-05-24 — [Conversation with Claude Desktop](https://claude.ai/chat/557140c5-328d-459d-9a56-d57016952a64)

**Block / Task**: Block 0 — Foundations, project planning

**Done**:
- Defined project scope, requirements, and constraints with Claude.
- Wrote initial CLAUDE.md, README.md, project-overview.md,
  architecture.md, domain-model.md, roadmap.md, learning-plan.md,
  ui-mockups.md.
- Wrote 15 ADRs covering all major architectural decisions.
- Validated technology stack: Java 25, Spring Boot 4, Angular, Keycloak
  with BFF, Redis, PostgreSQL, hexagonal architecture, schema-first
  OpenAPI, monorepo, Conventional Commits with squash-and-merge,
  multi-tenant from day one, UUID v7 application-side.

**Learned**:
- ADRs are an effective tool to make architectural reasoning explicit.
- UUID v7 (RFC 9562) is the modern choice for primary keys, balancing
  uniqueness and index locality.
- The BFF pattern is OWASP's current recommendation for SPA + OIDC,
  more secure than browser-side token storage.

**Next**:
- Set up the GitHub repository, commit the documentation.
- Switch to Claude Code in IntelliJ / VS Code for the development phase.
- Start Block 0: reactivate Spring Boot fundamentals.

**Notes**:
- Decision deadlines for the project: MVP by end of October 2026, with
  iteration thereafter during the job search.
- The repository will be public from day one; commits and documentation
  are written with external readers (recruiters, technical interviewers)
  in mind.


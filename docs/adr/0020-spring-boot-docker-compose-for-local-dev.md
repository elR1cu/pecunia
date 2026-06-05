# ADR-0020: `spring-boot-docker-compose` for Local Development

## Status

Accepted

## Context

`PecuniaApplication` depends on three infrastructure services to boot
end-to-end: PostgreSQL (datastore), Keycloak (OIDC provider), and Redis
(session store). All three are declared in
`deploy/docker-compose/docker-compose.dev.yml`.

Until now, a developer had to remember to run `docker compose up -d` before
starting the backend. Forgetting it produces an opaque JDBC connection
failure at startup. The friction is small but constant, and it scales
badly when adding a frontend dev loop or one-off scripts.

Three options were considered to remove this friction.

1. **`spring-boot-docker-compose`**: a first-party Spring Boot module
   (since 3.1) that detects a compose file at startup, starts services
   that are not running, and exposes their connection details to Spring
   Boot via the *service connections* mechanism (overriding properties
   such as `spring.datasource.url`).
2. **Testcontainers in dev mode** via `SpringApplication.from(...).with(...)`
   with `@ServiceConnection` beans declared in a `TestPecuniaApplication`
   class under `src/test/java`. Containers are started fresh on every run.
3. **IDE-side "Before Launch" hook**: configure IntelliJ (and every other
   IDE used by future contributors) to run `docker compose up -d` before
   launching the backend.

## Decision

Adopt **`spring-boot-docker-compose`** with the following configuration:

- Dependency declared as `<optional>true</optional>` in `apps/api/pom.xml`
  to exclude it from production jars.
- `spring.docker.compose.file: deploy/docker-compose/docker-compose.dev.yml`
  to point at the project's non-default compose file location.
- `spring.docker.compose.lifecycle-management: start-only` so the stack
  starts when the backend boots but is not stopped on shutdown. Containers
  survive across application restarts, preserving the imported Keycloak
  realm and Postgres state.

The working directory of the backend process must be the repository root
so that the relative path to the compose file resolves. This constraint
is documented in `docs/dev-setup.md`.

## Consequences

### Positive

- **Single command to run the app**: starting `PecuniaApplication`
  guarantees the stack is up. Removes a recurring source of friction.
- **Auto-configured connections**: PostgreSQL and Redis credentials and
  ports are read directly from the compose file via service connections,
  eliminating the need to keep `application.yml` and the compose file
  manually in sync.
- **Transferable abstraction**: the same *service connections* mechanism
  is used by Testcontainers (Block 2+ for integration tests), so the
  learning investment carries over.
- **No production impact**: declared `optional`, so the module is absent
  from the production jar and never attempts to find a Docker socket in
  prod.

### Negative

- **Silent property overrides**: service connections override
  `spring.datasource.*` and `spring.data.redis.*` in `application.yml`.
  A developer who changes those properties expecting them to take effect
  in dev will see no change. Documented in `docs/dev-setup.md` to make
  this discoverable.
- **Hard dependency on Docker Desktop in dev**: a developer who cannot
  run Docker locally (corporate restrictions, low-resource laptop) has
  no fallback. Acceptable today given the project's single-developer
  scope.
- **Working directory constraint**: the relative path to the compose file
  forces all run methods (CLI, IDE Run Configurations) to use the repo
  root as their working directory. A one-time setup cost.

### Neutral

- **`start-only` is a deliberate trade-off**: it favours iteration speed
  (no Keycloak realm re-import on every restart) over a clean slate at
  every run. Developers who want a fresh stack run
  `docker compose -f deploy/docker-compose/docker-compose.dev.yml down -v`
  explicitly.
- **Keycloak is not auto-configured**: Spring Boot 4 ships no
  `ConnectionDetails` factory for Keycloak. The OIDC configuration in
  `application.yml` (issuer URI, client id) remains the source of truth.
  Acceptable because the Keycloak port is fixed in the compose file.

## Alternatives Considered

### Testcontainers in dev mode

Rejected for daily development. Each run spins up fresh containers,
which means:
- The Keycloak realm is re-imported every time (~5 s added to each
  startup).
- All Postgres data accumulated during the dev session is lost on
  restart, including the `flyway_schema_history` table that the developer
  may want to inspect.

Testcontainers remain the right tool for integration tests in Block 2+,
where ephemeral state is a feature, not a cost. Spring Boot's service
connections abstraction means the two approaches share most concepts, so
adopting one now does not preclude the other later.

### IDE-side "Before Launch" hook

Rejected because:
- IDE-coupled: every contributor (today only one, but the project is
  public) would need to configure IntelliJ, VS Code, or another IDE
  independently.
- No service-connection benefit: properties in `application.yml` must
  stay manually in sync with the compose file.
- Misses a learning opportunity on an idiomatic Spring Boot feature
  worth knowing.

## References

- [Spring Boot reference — Docker Compose Support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose)
- [Spring Boot reference — Service Connections](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.service-connections)
- [ADR-0006](0006-bff-authentication-pattern.md) — BFF authentication pattern
- [ADR-0011](0011-redis-for-session-storage.md) — Redis for session storage

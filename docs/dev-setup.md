# Local Development Setup

This guide walks through everything needed to run the Pecunia backend
(`PecuniaApplication`) on a developer machine. The frontend, OpenAPI
generation, and test infrastructure are covered in separate docs once
those parts of the project land.

## Prerequisites

- **JDK 25** (Temurin or any OpenJDK distribution). Verify with `java -version`.
- **Maven 3.9+**. Verify with `mvn -version`. (No wrapper is committed yet.)
- **Docker Desktop** running. Verify with `docker info`.
- A Git client and a JVM-capable IDE (IntelliJ IDEA recommended).

The infrastructure stack (PostgreSQL, Keycloak, Redis) does **not** need to
be started by hand — it is started automatically by Spring Boot's
`spring-boot-docker-compose` integration. See
[ADR-0020](adr/0020-spring-boot-docker-compose-for-local-dev.md) for the
rationale.

## First-Time Setup

```bash
# 1. Clone
git clone https://github.com/elR1cu/pecunia.git
cd pecunia

# 2. Provide the local environment file
cp apps/api/.env.example apps/api/.env
# .env contains the OAuth2 client secret for the BFF.
# The default dev value matches the Keycloak realm fixture and works out of the box.

# 3. Make sure Docker Desktop is running
docker info >/dev/null && echo "Docker is up"
```

That's it for first-time setup. No `docker compose up` is required.

## Running the Backend

### From the CLI

Always run from the **repository root**, not from `apps/api/`. Relative
paths in `application.yml` (`spring.config.import: …apps/api/.env…` and
`spring.docker.compose.file: …deploy/docker-compose/…`) are resolved
against the process working directory.

```bash
# From the repo root:
mvn -pl apps/api spring-boot:run
```

### From IntelliJ IDEA

1. Open the project at the repository root (not at `apps/api/`).
2. Locate `PecuniaApplication` and let IntelliJ generate an "Application"
   Run Configuration (or create one manually with the main class set to
   `com.pecunia.api.PecuniaApplication`).
3. **Important**: open the Run Configuration and change *Working directory*
   from the default (`$MODULE_WORKING_DIR$`, which resolves to `apps/api/`)
   to the **repository root** (`$PROJECT_DIR$`).
4. No environment variables need to be set manually. Spring Boot loads
   `apps/api/.env` automatically via `spring.config.import`.
5. Run.

The same working-directory rule applies to a Debug configuration.

## What `spring-boot-docker-compose` Does (and Does Not)

When the backend boots:

- It reads `deploy/docker-compose/docker-compose.dev.yml`, starts any
  service that is not already running, and waits for healthchecks.
- For **PostgreSQL**, it detects the `postgres:18` image and provides a
  `JdbcConnectionDetails` bean that **overrides** `spring.datasource.{url,
  username, password}` in `application.yml`. The YAML values are kept as
  documentation and as a fallback when the module is disabled.
- For **Redis**, same mechanism: `spring.data.redis.{host,port}` are
  overridden by an auto-detected connection.
- For **Keycloak**: no built-in `ConnectionDetails` factory exists in
  Spring Boot 4, so the container is started but the OAuth2 configuration
  (`spring.security.oauth2.client…issuer-uri`) is kept as written in
  `application.yml`. This is fine because the Keycloak port (`9090`) is
  fixed in the compose file.

Lifecycle: `spring.docker.compose.lifecycle-management` is set to
`start-only`. The stack is started by the backend but **never stopped
automatically**. The stack survives across application restarts so that
the Keycloak realm import (~5 seconds) and the PostgreSQL state are paid
only once.

## Hot Reload with Spring DevTools

`spring-boot-devtools` is on the classpath (declared `optional`, so it is
excluded from production jars). It watches the classpath and triggers an
automatic application restart on change, using a dedicated classloader so
the restart cost is a fraction of a full JVM cold start. It also runs a
LiveReload server on port `35729` for browser-extension integration.

### IntelliJ configuration

DevTools only fires when something changes on the **classpath**. IntelliJ
does not, by default, recompile while a launched application is running.
Two one-off settings unlock this:

1. **Settings → Build, Execution, Deployment → Compiler → Build project automatically** — check.
2. **Settings → Advanced Settings → Allow auto-make to start even if developed application is currently running** — check.

After this, saving a `.java` or `.yml` file triggers a recompile, which
triggers a DevTools restart in ~1–2 seconds.

### Interaction with `spring-boot-docker-compose`

Each DevTools restart re-evaluates the Docker Compose lifecycle. Because
the stack is already running (lifecycle is `start-only`), this is a
healthcheck verification — typically under a second. The containers are
never recreated by a restart.

## Sanity Checks

After a successful boot:

```bash
# Health probes
curl -i http://localhost:8080/actuator/health/liveness
curl -i http://localhost:8080/actuator/health/readiness

# Flyway applied V1
docker exec -it $(docker ps -qf name=postgres) \
  psql -U pecunia -d pecunia -c "SELECT version, description, success FROM flyway_schema_history;"

# Keycloak admin console (Quarkus-based)
open http://localhost:9090/admin   # admin / admin (dev only)

# Pecunia realm
open http://localhost:9090/realms/pecunia/.well-known/openid-configuration
```

> Note on Spring Security defaults: the application-level endpoints
> (everything except actuator probes once `SecurityFilterChain` is wired
> in Block 1 step 6) require authentication. A browser request to a
> protected URL triggers a redirect to Keycloak.

## Day-to-Day Workflow

- **Start the stack + app**: run `PecuniaApplication` from IntelliJ or
  `mvn -pl apps/api spring-boot:run` from the repo root. The stack auto-starts
  if not already running.
- **Restart the app only**: kill the JVM and re-run. The Docker stack is
  not touched.
- **Stop the stack manually** (e.g., to reclaim resources or reset state):

  ```bash
  docker compose -f deploy/docker-compose/docker-compose.dev.yml down
  ```

- **Reset Postgres and Keycloak to a clean slate**: same command with
  `down -v` (also deletes the named volumes).
- **Inspect Redis sessions** (after logging in via the BFF):

  ```bash
  docker exec -it $(docker ps -qf name=redis) redis-cli KEYS 'pecunia:session:*'
  ```

## Troubleshooting

**`No such file or directory: deploy/docker-compose/docker-compose.dev.yml`
on startup**
The working directory is wrong. The process must run from the repository
root. Fix the IntelliJ Run Configuration's *Working directory* or run
`mvn` from the repo root.

**`Could not resolve placeholder 'PECUNIA_BFF_CLIENT_SECRET'`**
The `.env` file is missing or unreachable. Confirm:

- `apps/api/.env` exists (copy from `.env.example`).
- The working directory is the repo root (so the relative path
  `apps/api/.env` resolves).

**`Cannot connect to the Docker daemon` / `error during connect`**
Docker Desktop is not running. Start it and retry.

**Port already in use (`5432`, `6379`, `9090`)**
Another service is bound to that port. Either stop it or change the
host-side port mapping in `deploy/docker-compose/docker-compose.dev.yml`.
Note that PostgreSQL and Redis ports are also referenced in
`application.yml` (as a fallback) and would need to be aligned.

**Keycloak realm not present after login attempt**
The realm import only runs on **first** Keycloak startup. To force a
re-import, stop the stack with `docker compose … down -v` to delete the
Keycloak database, then start the backend again.

## Out of Scope (covered elsewhere)

- Frontend (Angular) workflow — Block 1 step 6 deliverable.
- OpenAPI generator workflow — Block 1 step 7 deliverable.
- Integration tests with Testcontainers — Block 2 deliverable.
- Production deployment — Block 8 deliverable.

## References

- [ADR-0020: `spring-boot-docker-compose` for local development](adr/0020-spring-boot-docker-compose-for-local-dev.md)
- [ADR-0006: BFF authentication pattern](adr/0006-bff-authentication-pattern.md)
- [ADR-0011: Redis for session storage](adr/0011-redis-for-session-storage.md)
- [Spring Boot reference — Docker Compose support](https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose)

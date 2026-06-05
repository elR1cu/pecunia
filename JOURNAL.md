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


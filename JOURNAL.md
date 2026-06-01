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


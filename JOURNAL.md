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

### 2026-05-24 — Session 1

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

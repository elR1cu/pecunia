# ADR-0038: PostgreSQL Session Storage, Removing Redis

## Status

Accepted

Supersedes [ADR-0011](0011-redis-for-session-storage.md).

## Context

ADR-0011 adopted Redis via `spring-session-data-redis` for BFF session
storage, following the canonical BFF blueprint. The wiring landed in the
identity work (Session 23) and works end to end.

Block 8 preparation (ADR-0035) re-examined every production component
against the question "which actual requirement justifies it at this
scale?". For sessions, the requirement is **persistence** — sessions must
survive application restarts and deployments so the user is not logged
out — plus readiness for horizontal scaling *some day*. With a single
application instance (the deliberate MVP and foreseeable-future topology,
per the accepted-SPOF doctrine of ADR-0035), Redis is one possible
*implementation* of persistence, not the requirement itself. PostgreSQL
is already in the stack, already backed up, already secured.

Keeping Redis has a permanent carrying cost (a production container,
~100–150 MB RAM, a password, CVE patching, monitoring, one more failure
point), while re-adopting it later is a cheap, localized change —
`spring-session` makes the backing store swappable by dependency and
configuration. The asymmetry favors removal; keeping unused capability
"because it is already wired" is inverted YAGNI.

This supersession is also a process lesson worth recording: ADR-0011
inherited the component from the canonical BFF architecture diagram
without testing it against the project's scale. Architecture blueprints
embed scale assumptions; every component must be justified by a
requirement, not by its presence in the reference diagram.

## Decision

HTTP sessions are stored in **PostgreSQL** via **`spring-session-jdbc`**;
**Redis is removed from the stack** (application dependencies, local
Docker Compose, and the future production Compose).

Implementation outline (dedicated PR, before the Block 8 production
compose is written):

- Swap `spring-session-data-redis` (+ Redis starter) for the
  `spring-session-jdbc` starter (Spring Boot 4 module discipline).
- Flyway migration `V5__spring_sessions.sql` with Spring Session's
  official PostgreSQL schema; `spring.session.jdbc.initialize-schema:
  never` so Flyway remains the single owner of the schema.
- Remove Redis properties and the session-namespace customizer; `UserId`
  stays `Serializable` (session attributes are serialized in both
  backends).
- Session cookie settings and timeout are unchanged from ADR-0011.
- Validation: real Keycloak login, then a backend restart must preserve
  the session — the test that materializes the property being bought.

**Reintroduction trigger** (recorded in the roadmap): a second application
instance (shared session store), application-level caching (Block 5/6
triggers), or distributed rate limiting. Reintroduction is a dependency
and configuration change, not a redesign.

## Consequences

### Positive

- Production shrinks to four components (app, PostgreSQL, Keycloak,
  reverse proxy): one fewer service, secret, patch surface, and failure
  point — aligned with the ADR-0035 doctrine that small-scale robustness
  comes from fewer moving parts.
- Sessions gain the same operational treatment as all other state for
  free: PostgreSQL backups, monitoring, and restore procedures cover them.
- Session persistence across restarts/deployments is preserved — the
  actual requirement from ADR-0011 still holds.

### Negative

- Slightly higher session-access latency than Redis (irrelevant at
  single-user scale, worth re-measuring if scale changes).
- Expired-session cleanup runs as a periodic delete job
  (`spring-session-jdbc` default) instead of Redis TTL eviction.
- If multi-instance or caching needs materialize, Redis returns and this
  ADR is superseded in turn — accepted, the migration cost is small and
  the trigger is documented.

### Neutral

- ADR-0011's alternatives analysis (in-memory, Hazelcast) remains valid;
  only the scale-fit conclusion changes.
- The sessions table is operationally invisible next to business tables;
  its rows are ephemeral and excluded from any data-retention reasoning.

## Alternatives Considered

### Keep Redis (status quo)

Zero migration work and free multi-instance readiness. Rejected: a
permanent, certain carrying cost paid to avoid a one-off, improbable,
cheap future migration. The scale trigger that would justify Redis has no
date and may never arrive within the MVP's life.

### In-memory sessions

Simplest possible setup. Rejected for the same reason as in ADR-0011: the
user is logged out on every deployment — a real UX regression given
frequent deployments — and it abandons the persistence property that is
actually required.

## References

- ADR-0011 (superseded), ADR-0035 (recoverability/SPOF doctrine)
- Spring Session JDBC: https://docs.spring.io/spring-session/reference/
- Spring Session PostgreSQL schema:
  `org/springframework/session/jdbc/schema-postgresql.sql`

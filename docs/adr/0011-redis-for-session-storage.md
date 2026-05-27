# ADR-0011: Redis for Session Storage

## Status

Accepted

## Context

The Backend-for-Frontend (BFF) pattern adopted in ADR-0006 requires
server-side session storage. Sessions hold access tokens, refresh tokens,
and ID tokens issued by Keycloak. The choice of session store affects:

- Application restart behavior (do sessions survive?)
- Horizontal scalability (can multiple instances share sessions?)
- Operational complexity
- Resource footprint and cost

Three options were considered:

1. **In-memory sessions** (default HttpSession): no external dependency,
   sessions lost on restart, no horizontal scaling.
2. **Redis with `spring-session-data-redis`**: external cache, sessions
   persist across restarts and scale horizontally.
3. **Hazelcast embedded**: distributed cache embedded in the JVM, no
   separate service, more complex for true multi-instance scenarios.

## Decision

Pecunia uses **Redis** as the session store via
`spring-session-data-redis`.

Redis runs as a Docker container in local development and on the production
VPS. The session cookie is configured `HttpOnly`, `Secure`,
`SameSite=Strict`, with a default 8-hour inactivity timeout.

## Consequences

### Positive

- **Session persistence across restarts**: users are not forced to
  re-authenticate after a deployment.
- **Horizontal scaling readiness**: future addition of a second backend
  instance does not require session-handling redesign.
- **Industry standard**: Redis is ubiquitous in financial services and
  fintech contexts. Familiarity is a recognized CV asset.
- **Mature Spring integration**: `spring-session-data-redis` is well-
  documented and battle-tested.
- **Cache opportunities**: Redis can also serve as an application-level
  cache (rate limiting with Bucket4j, computed dashboards, etc.) if needed
  later.

### Negative

- **Additional infrastructure**: one more container to run, monitor, and
  back up.
- **Resource footprint**: Redis uses ~50 MB RAM at idle, plus session data.
  Modest but non-zero.
- **Operational complexity**: another moving piece in the deployment
  pipeline.

### Neutral

- **Persistence configuration**: Redis is configured with AOF
  (append-only-file) persistence for durability, with regular snapshots.

## Alternatives Considered

### In-memory sessions (default HttpSession)

Rejected because:
- Sessions are lost on every restart, forcing re-authentication after
  deployments.
- Cannot scale horizontally.
- Acceptable for a single-instance MVP, but the cost of adopting Redis
  upfront is small relative to the future migration cost.

### Hazelcast embedded

Considered seriously. Hazelcast embedded would avoid running a separate
service. However:
- Less common than Redis in the targeted industry (Swiss private banking
  and fintech).
- Configuration is more subtle, especially when planning multi-instance
  scenarios.
- Redis is the safer CV choice and broadly recognized.

## References

- Spring Session documentation:
  https://spring.io/projects/spring-session
- Redis documentation: https://redis.io/docs/

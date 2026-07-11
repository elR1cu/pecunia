# ADR-0033: Owner Resolution in the Application Layer

## Status

Accepted

## Context

Pecunia is multi-tenant by design: every aggregate root has a `UserId` owner,
every query filters by owner, and ownership failures map to HTTP 404
(CLAUDE.md, ADR-0031). Tenancy has two distinct halves:

- **Enforcement** — driven-port read signatures force the owner into every
  lookup (`findByIdAndOwner`, `findAllByOwner`), so a use case cannot forget
  the filter. This is settled (ADR-0031) and unchanged by this ADR.
- **Resolution** — deciding *which* `UserId` a use case acts for. Until now,
  web controllers resolved it from the `CurrentUserProvider` port (backed by
  the Spring Security context) and stamped it into every command/query record
  (`OpenAccountCommand(owner, …)`).

Resolution-in-the-controller has a structural weakness: the application layer
receives a well-typed `UserId` and cannot tell whether it came from the
authenticated principal or from a request payload. The invariant "the owner is
never client-supplied" therefore rests on every driving adapter individually —
each new adapter (REST today; a Kafka consumer, batch job, or admin CLI later)
must re-implement the stamping correctly, and a single controller binding a
`userId` request parameter into a command would create an
insecure-direct-object-reference hole that no layer below can detect. Guarding
this per adapter (contract tests on the OpenAPI spec, DTO field checks) is
O(N adapters) and must be re-invented for every non-HTTP adapter.

The literature on Clean/Hexagonal Architecture converges on resolving the
current user *inside* the application layer through an abstraction it owns:
an injected user-context port implemented by a security adapter (Jovanović),
security concerns handled in the use-case layer via a port + secondary adapter
(UNIL), an injected authentication interface rather than user-id parameters
because different IO channels authenticate differently (lessthan12ms).

Pecunia already has the required abstraction: `CurrentUserProvider` is a
shared-kernel port (like `IdGenerator`) with a `sharedinfra.security` adapter
reading the `SecurityContext`. Only its consumption point is at the wrong
layer.

## Decision

1. **Application services resolve the owner themselves** by injecting
   `CurrentUserProvider` and calling `currentUserId()` at the start of the
   use case. Controllers no longer resolve or forward the owner.
2. **Commands and queries carry no `UserId`.** The owner is not an input of a
   user-facing use case; it is ambient, authenticated state. Query records
   left empty by this change are deleted (the port method takes no argument)
   rather than kept as empty envelopes.
3. **Two trust sources, and only two.** The owner comes either from the
   authenticated principal (user-initiated flows, via `CurrentUserProvider`)
   or from a **trusted internal event** that carries it explicitly
   (system-initiated flows: the event was produced by our own code from an
   originally authenticated action). It never comes from a payload exposed to
   an external caller. How system flows will consume the owner — dedicated
   system ports vs a propagated user context around the same ports — is
   deliberately deferred to the first real system consumer (Kafka is
   post-MVP); this ADR only fixes the trust rule.
4. **An ArchUnit rule locks the pattern**: no record in
   `..application.port.in..` may declare a `UserId` component, so no adapter
   can reintroduce a client-suppliable owner. (Complemented by the existing
   convention that `owner`/`userId` never appears as an input in
   `contracts/openapi.yaml`.)

Ownership *enforcement* is untouched: driven-port signatures keep the explicit
`owner` parameter and the 404 mapping stands. `ProvisionUser` (identity
context) is out of scope — it runs during login, before a principal exists,
and takes an `IdpIdentity`, not an owner.

## Consequences

### Positive

- The "owner is never client-supplied" invariant becomes structural — one
  resolution point instead of one per adapter, verified by a fitness rule
  instead of vigilance. An IDOR via a bound request parameter is impossible
  by construction.
- Commands shrink and describe only the business intent.
- A service's dependency list now documents its nature: tenant-scoped use
  cases visibly inject `CurrentUserProvider`; future system use cases visibly
  will not.

### Negative

- Use cases gain an ambient input (the security context via the port): they
  are no longer pure functions of their command. Every tenant-scoped service
  test must stub `CurrentUserProvider` (one `given` line per test).
- Replaying a use case "as another user" now requires going through the
  provider — deliberate for a personal-finance application, but it removes a
  convenience some batch/admin tooling might have wanted (addressed later by
  the system-flow mechanism).
- Reverses the Session 22/24 convention of passing the owner explicitly
  through commands; `account` and `category` are refactored in the same PR to
  avoid a split-brain codebase.

### Neutral

- `application → sharedkernel` is already an allowed dependency (ADR-0026,
  ADR-0032); no ArchUnit relaxation is needed. The adapter stays in
  `sharedinfra.security`.
- The web layer keeps translating request DTOs into domain value objects; it
  simply stops handling identity.

## Alternatives Considered

### Keep owner in commands, guard the web layer (contract/DTO tests)

A test asserting that `owner`/`userId` never appears in OpenAPI request
schemas or generated request DTOs. Rejected: it protects one adapter type;
every future non-HTTP adapter needs its own guard, and the application still
cannot distinguish a stamped owner from a forwarded one. O(N adapters) versus
O(1).

### Resolve in an aspect/interceptor that enriches commands

Keeps commands carrying the owner while centralising resolution. Rejected:
invisible magic — the command appears to be an input when part of it is
injected infrastructure state; harder to test and to reason about than an
explicit port call in the service.

### Read `SecurityContextHolder` directly in services

No port indirection. Rejected: couples the application layer to Spring
Security and a thread-local — exactly what the hexagonal boundary and the
ArchUnit rule `domain_and_application_do_not_depend_on_shared_infra` exist to
prevent. The port already exists; there is no saving to be had.

## References

- [Getting the Current User in Clean Architecture — Milan Jovanović](https://www.milanjovanovic.tech/blog/getting-the-current-user-in-clean-architecture)
- [Securing Use Cases in Clean Architecture — UNIL engineering](https://medium.com/unil-ci-software-engineering/securing-use-cases-in-clean-architecture-7f39d07b8ed2)
- [Authorization and authentication in clean architecture — lessthan12ms](https://lessthan12ms.com/authorization-and-authentication-in-clean-architecture.html)
- ADR-0026 (ports in the application layer), ADR-0031 (persistence tenancy
  isolation), ADR-0032 (shared kernel vs shared infrastructure)
- CLAUDE.md — Multi-tenancy section (ownership → 404, owner-scoped queries)

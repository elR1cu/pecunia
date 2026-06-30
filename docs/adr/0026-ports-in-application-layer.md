# ADR-0026: Driving and Driven Ports Live in the Application Layer

## Status

Accepted

Refines [ADR-0003](0003-hexagonal-architecture.md), which originally placed
ports in the domain layer. ADR-0003 remains the canonical statement of the
hexagonal style; this ADR settles the single open question of *where ports
live* and supersedes ADR-0003 on that one point.

## Context

Pecunia adopts Hexagonal Architecture (ADR-0003): domain at the center,
adapters on the edge, dependencies pointing inward. ADR-0003 and
`architecture.md` describe ports — the interfaces that decouple the core from
adapters — as living in the **domain** layer (e.g. `TransactionRepository`,
`DomainEventPublisher`).

Block 2 introduces the first real ports of the codebase (the `account`
bounded context: an `AccountRepository` driven port, and the use-case
interfaces). Before writing them, the convention for *where ports live* must
be fixed once, consistently, because it shapes every bounded context and the
ArchUnit rules that police them.

Two coherent schools exist:

1. **Ports in the domain** — the classic DDD reading (Evans): a repository is
   a domain concept; the domain declares the interfaces it needs and the
   infrastructure implements them. This is what ADR-0003 and
   `architecture.md` currently state.
2. **Ports in the application** — the "Clean/Hexagonal" reading popularised by
   Tom Hombergs (*Get Your Hands Dirty on Clean Architecture*): the domain is
   strictly the model (entities, value objects, invariants), and the
   application layer owns both the **driving** ports (use-case interfaces,
   `port.in`) and the **driven** ports (SPI such as repositories, `port.out`).

A further forcing function: the Session 19 design of the cross-context balance
read already placed the `AccountMovements` port in the application layer. A
single rule must cover *all* ports rather than leaving an unexplained
exception.

## Decision

**All ports live in the application layer.** The domain layer contains only
the model and is free of any outward-facing interface.

### Placement

- **Driving ports** (use-case interfaces) live in `application.port.in`.
  Each use case is an interface (e.g. `OpenAccount`) implemented by a service
  (e.g. `OpenAccountService` in `application.service`). Web controllers depend
  on the `port.in` interface, never on the service.
- **Driven ports** (SPI: repositories, the event publisher, cross-context
  query ports) live in `application.port.out` (e.g. `AccountRepository`,
  `DomainEventPublisher`, and — at Block 3 — `AccountMovements`).
- **Adapters** implement the driven ports from `infrastructure`
  (e.g. `AccountRepositoryAdapter implements AccountRepository`).
- **The domain** keeps entities, value objects, aggregates, domain services
  and domain events. No port interfaces.

### One deliberate exception — cross-cutting kernel SPIs

A pure, framework-free interface that *every* context needs — not a use-case
port of any single context — lives in the shared kernel (`com.pecunia.shared`),
not in some context's `application.port.out`. The first instance is
`IdGenerator` (abstracting UUID v7 generation), implemented by an adapter
outside the kernel (`com.pecunia.id.Uuidv7IdGenerator`). Placing such an SPI in
one context's application layer would force either duplication across contexts
or a cross-context dependency on that context's internals. It is a *kernel-level
SPI*, categorically different from a context's driving/driven ports, so it does
not contradict the rule above. The kernel stays pure (the ArchUnit
"shared kernel is framework-free" rule still holds, since an interface carries no
framework dependency), so the boundary is not weakened.

### Worked example — `account` context (Block 2)

```
com.pecunia.account
├── domain                      # pure model, no ports
│   ├── Account                 # aggregate root; balanceFrom(Money)
│   ├── AccountType             # CURRENT / CREDIT_CARD
│   ├── AccountStatus           # ACTIVE / ARCHIVED
│   └── Iban                    # value object
├── application
│   ├── port
│   │   ├── in                  # OpenAccount, ArchiveAccount, ListAccounts
│   │   └── out                 # AccountRepository (+ AccountMovements @ Block 3)
│   └── service                 # OpenAccountService, ArchiveAccountService, …
├── web                         # AccountController (implements OpenAPI iface)
└── infrastructure              # JPA entity + adapter, MapStruct mapper
```

Typed identifiers and `Money` live in the shared kernel (`com.pecunia.shared`),
not in `account.domain` — see the Session 19 recap and ADR-0014 (multi-tenancy)
context.

### Dependency rule (enforced by ArchUnit from Block 2)

- `domain` depends only on the shared kernel and the JDK — no Spring, JPA,
  Lombok, and **no port interfaces**.
- `application` depends on `domain` and on its own `port.*`; never on `web`
  or `infrastructure`.
- `web` and `infrastructure` depend on `application` (the ports); never on
  each other.

## Consequences

### Positive

- **One rule, no exception**: every port — driving and driven, including the
  cross-context `AccountMovements` from Session 19 — sits in the same place.
- **The simplest ArchUnit invariant**: "`domain` contains no interface port
  and has no outward dependency" is trivial to express and to defend.
- **Pure domain**: the model carries business rules only; there is no debate
  about whether an interface in the domain is "still pure".
- **In/out symmetry**: use cases are abstractions (`port.in`) just like the
  SPI (`port.out`), which improves controller testability and reads as a
  recognisable Hombergs-style hexagon — a clear signal for senior reviewers.

### Negative

- **More ceremony**: each use case is an interface plus an implementing
  service, rather than a single service class.
- **Departs from classic DDD**: moving the repository out of the domain will
  surprise reviewers who expect the Evans placement; the rationale must be
  visible (this ADR).
- **Documentation debt to repay**: ADR-0003, ADR-0008 and `architecture.md`
  were written with ports in the domain and must be reconciled (this ADR plus
  the `architecture.md` update; ADR-0003/0008 carry a forward pointer here).

### Neutral

- No runtime impact: this is a structural convention, not a performance
  trade-off.
- The `DomainEventPublisher` port (ADR-0008) moves to
  `application.port.out`; its Spring `ApplicationEventPublisher` adapter and
  the migration path to Kafka are unaffected.

## Alternatives Considered

### Ports in the domain (classic DDD, the original ADR-0003 wording)

Rejected as the project-wide rule because it forces a per-port judgement
("is this interface a domain concept or an integration concern?"), it makes
the ArchUnit rule fuzzier (the domain would legitimately contain some
interfaces), and it would leave the Session 19 `AccountMovements` placement as
an unexplained exception. It remains a perfectly valid school; the choice here
is for consistency and a crisp boundary.

### Hybrid — aggregate repository in the domain, integration ports in the application

Place a self-persistence port (`AccountRepository`) in the domain but
cross-context ports (`AccountMovements`) in the application. Rejected because
two port locations require a subtler rule to teach and to police, for no
tangible gain at Pecunia's scale.

### Enforcing layer and context boundaries with the Java Platform Module System (JPMS)

`module-info.java` could turn the layer and bounded-context boundaries this ADR
relies on into compile-time and runtime guarantees (`exports` / `requires`).
Rejected as overkill — and counterproductive with this stack — for three
reasons. JPMS only encapsulates *between* modules, so segmenting the contexts
would force one JPMS module per context, hence one Maven module per context —
the multi-module structure already declined for the shared kernel (single
Maven module, no root pom). Spring Boot on the module path is poorly supported:
reflective DI, CGLIB proxies, Spring Data and Hibernate enhancement, and the
test slices would each require `opens` directives that dissolve the very
encapsulation sought. And the need is already met more expressively by ArchUnit
(ADR-0016), which can assert rules JPMS cannot — "the domain carries no `@Entity`
or Spring import", "no cycle between contexts", the cross-user isolation
discipline. The pragmatic next step beyond ArchUnit, if its boundary checks ever
become cumbersome, is Spring Modulith (a documented adoption trigger in the
roadmap), not JPMS.

## References

- [ADR-0003: Hexagonal Architecture](0003-hexagonal-architecture.md)
- [ADR-0008: Spring Events with Port/Adapter](0008-spring-events-with-port-adapter.md)
- [ADR-0014: Multi-tenant Architecture](0014-multi-tenant-architecture.md)
- Tom Hombergs, *Get Your Hands Dirty on Clean Architecture* — `port.in` /
  `port.out` / `service` package convention.
- `docs/session-recaps/2026-06/2026-06-28-session-19.md` — origin of the
  `AccountMovements` placement that this rule generalises.

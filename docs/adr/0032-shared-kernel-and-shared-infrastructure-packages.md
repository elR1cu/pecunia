# ADR-0032: Top-level package taxonomy — `sharedkernel` and `sharedinfra`

## Status

Accepted

Refines the shared-kernel notion introduced in Session 19 and referenced by
[ADR-0026](0026-ports-in-application-layer.md) (the cross-cutting kernel SPI
exception), and resolves the open question deferred by
[ADR-0030](0030-jpa-persistence-adapter-conventions.md), which anticipated "a
dedicated *shared infrastructure* location distinct from the pure kernel … to be
recorded in its own ADR at that point".

## Context

The codebase organises its top-level packages under `com.pecunia` in two
distinct categories that the naming did not make visible:

- **Bounded contexts** — `account`, `identity` — each a full hexagonal *vertical
  slice* (`domain` / `application` / `web` / `infrastructure`).
- **Cross-cutting technical concerns** — `id` (the UUID v7 adapter), `security`
  (the BFF/OIDC configuration and the login-time provisioning glue),
  `observability` (structured-logging configuration and sanitization) — plus the
  pure shared kernel `shared` (typed IDs, `Money`, the `IdGenerator` and
  `CurrentUserProvider` ports, shared exceptions).

Reading the top-level list, nothing distinguishes a bounded context from a
technical concern: `account`, `id`, `identity`, `observability`, `security`,
`shared` all sit flat and alphabetically interleaved. This obscures the mental
model a reviewer needs first — *"what are the business modules, and what is the
supporting machinery?"*.

Two further forces converge on the same point:

- `shared` is an overloaded name. It holds the **framework-free** kernel (an
  ArchUnit rule forbids Spring / JPA / Hibernate / Lombok anywhere under
  `com.pecunia.shared..`), yet the word "shared" also naturally attracts
  *shared infrastructure* — precisely what `id` already is (a Spring
  `@Component` implementing a kernel port).
- ADR-0030 flagged that `MoneyEmbeddable`, once reused by a second context
  (Block 3 `transaction`), needs a shared home. Placing it under
  `com.pecunia.shared.persistence` would sit **inside** `com.pecunia.shared..`
  and collide with the kernel's framework-free rule, forcing a carve-out in that
  rule. ADR-0030 named this "an expansion of what *shared* means" and deferred
  it here.

## Decision

Adopt an explicit three-way top-level taxonomy under `com.pecunia`:

1. **Bounded contexts** — `account`, `identity`, and future `transaction`,
   `category`, `budget`. Each keeps its own `infrastructure` sub-package;
   context-specific JPA is **not** shared and does not move.
2. **`sharedkernel`** (renamed from `shared`) — the pure, framework-free shared
   core: typed IDs (`AccountId`, `UserId`), `Money`, the cross-cutting kernel
   SPIs (`IdGenerator`, `CurrentUserProvider`), and shared exceptions. Depended
   on by every context; depends on no context.
3. **`sharedinfra`** (new) — the cross-cutting, framework-bearing technical
   concerns, as sub-packages:
   - `sharedinfra.id` — `Uuidv7IdGenerator` (implements the kernel `IdGenerator`).
   - `sharedinfra.security` — the BFF/OIDC configuration and login-time glue.
   - `sharedinfra.observability` — structured-logging configuration and
     sanitization.
   - `sharedinfra.persistence` — future home of `MoneyEmbeddable` at the second
     `Money`-persisting context (Block 3), resolving ADR-0030's open point.

`sharedinfra` is a **sibling** of `sharedkernel`, not a sub-package of it. This
is deliberate: it keeps `sharedinfra` outside `com.pecunia.sharedkernel..`, so
the "kernel is framework-free" ArchUnit rule applies to the kernel proper
without any carve-out, while `sharedinfra` is free to carry Spring and JPA.

```
com.pecunia
├── account               # bounded context (vertical slice)
├── identity              # bounded context (vertical slice)
├── sharedkernel          # pure, framework-free shared core (ports + value objects)
├── sharedinfra           # cross-cutting technical concerns (framework-bearing)
│   ├── id
│   ├── security
│   ├── observability
│   └── persistence       # (Block 3: MoneyEmbeddable)
└── PecuniaApplication.java
```

### ArchUnit impact

- The two kernel rules key on `com.pecunia.sharedkernel..` instead of
  `com.pecunia.shared..`. The framework-free invariant and the "kernel is a
  sink" invariant are otherwise unchanged.
- The no-cycles slice rule `slices().matching("com.pecunia.(*)..")` now yields
  the slices `account`, `identity`, `sharedkernel`, `sharedinfra`. The three
  former top-level concerns (`id`, `security`, `observability`) collapse into
  the single `sharedinfra` slice — see the negative consequence below.
- A future fitness rule becomes cleanly expressible: `..domain..` and
  `..application..` must not depend on `sharedinfra..` (they see only the kernel
  ports). It is left for a follow-up, when adopted deliberately.

## Consequences

### Positive

- **Legible top-level**: four entries that answer "business vs supporting
  machinery" at a glance — two contexts, one kernel, one infra bucket.
- **`sharedkernel` / `sharedinfra` symmetry**: the pure core and its
  framework-bearing counterpart read as a matched pair.
- **Resolves ADR-0030 cleanly**: `MoneyEmbeddable`'s shared home becomes
  `sharedinfra.persistence` — no overloading of "shared", no carve-out in the
  framework-free rule (which the tentative `shared.persistence` idea would have
  required).

### Negative

- **One wide mechanical rename**: every `com.pecunia.shared` import and every
  `id` / `security` / `observability` package declaration changes once
  (performed as an IntelliJ Move/Rename refactor).
- **Coarser cycle detection**: collapsing `id` / `security` / `observability`
  into a single `sharedinfra` slice makes a cycle *between* those three
  invisible to the slice rule. Accepted — they are leaf technical concerns with
  no mutual dependency.

### Neutral

- No runtime impact: component scanning targets `com.pecunia`, so the moves are
  transparent to Spring. This is a structural convention only.

## Alternatives Considered

### Keep the flat per-concern top-level packages

Leave `id`, `security`, `observability` as top-level siblings and only rename
`shared` → `sharedkernel`. Rejected: it does not solve the stated problem — the
top-level list stays noisy and interleaves contexts with technical concerns.

### `com.pecunia.shared.persistence` sub-package + scope the ArchUnit rule

ADR-0030's tentative idea: keep everything under `shared` and scope the
framework-free rule to the kernel sub-package. Rejected: it keeps the pure
kernel and framework-bearing infra tangled under one overloaded name and
requires a special-case in the ArchUnit rule. Renaming to `sharedkernel` +
sibling `sharedinfra` achieves the separation without any rule carve-out.

### Group the contexts instead (`com.pecunia.context.*`)

Nest the bounded contexts under a `context` (or `modules`) package and leave the
technical concerns flat. Rejected: it adds a level of nesting to the *main
event* (the business contexts) and buries them, while the noise being removed is
on the technical-concern side.

## References

- [ADR-0003: Hexagonal Architecture](0003-hexagonal-architecture.md)
- [ADR-0016: ArchUnit Architectural Fitness Tests](0016-archunit-architectural-fitness-tests.md)
- [ADR-0026: Ports Live in the Application Layer](0026-ports-in-application-layer.md) — the cross-cutting kernel SPI exception.
- [ADR-0030: JPA persistence-adapter conventions](0030-jpa-persistence-adapter-conventions.md) — deferred the shared-infrastructure location resolved here.
- `docs/session-recaps/2026-07/2026-07-01-session-20.md` — origin of the shared kernel and the `id` adapter.

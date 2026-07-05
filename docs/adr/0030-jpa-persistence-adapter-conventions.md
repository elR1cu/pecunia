# ADR-0030: JPA persistence-adapter conventions — value-object mapping and application-assigned IDs

## Status

Accepted

> **Follow-up.** The "shared infrastructure location distinct from the pure
> kernel" this ADR anticipated for a reused `MoneyEmbeddable` is now settled by
> [ADR-0032](0032-shared-kernel-and-shared-infrastructure-packages.md): its home
> is `com.pecunia.sharedinfra.persistence` (not `com.pecunia.shared.persistence`),
> and the kernel is renamed to `com.pecunia.sharedkernel`.

## Context

The `account` context is the second bounded context to get a persistence
adapter, after `identity`. The `identity` adapter was deliberately minimal:
`UserEntity` maps three scalar columns, the `User` aggregate is immutable, and
persistence uses a PostgreSQL native upsert keyed on a natural key
(see [ADR-0029](0029-idempotent-user-provisioning.md)).

`account` introduces two mapping concerns that `identity` did not exercise, and
that will recur in every future context (`transaction`, budgets, …). Deciding
them once, on the first adapter that needs them, avoids per-adapter drift:

1. **A value object made of two coupled fields.** `Account` carries
   `Money initialBalance`, a domain record of `(BigDecimal amount, Currency
   currency)` (scale 4, `HALF_EVEN`). The pair must be persisted together and
   will reappear on `Transaction`, budgets, and savings figures.

2. **An application-assigned primary key.** `AccountId` is a UUID v7 generated
   application-side (see [ADR-0015](0015-uuid-v7-application-side-generation.md)),
   so the entity's `@Id` is never null by the time it reaches the repository.
   Spring Data's default new-entity detection treats a non-null `@Id` as "not
   new" and routes `save()` through `EntityManager.merge()`, which issues a
   `SELECT` before every `INSERT` — a phantom read on what is actually a fresh
   row.

Two constraints frame the choices. First, the domain layer stays free of
persistence annotations ([ADR-0013](0013-selective-use-of-lombok.md) forbids
Lombok in the domain; the same discipline forbids JPA annotations there): the
domain `Money` and `AccountId` must not gain `@Embeddable`/`@Embedded`. Second,
unlike `identity`, `account` has **no natural key**: opening an account mints a
fresh surrogate id, so there is no idempotent "same row" to converge on and no
`ON CONFLICT` target.

## Decision

### 1. Map `Money` as an infrastructure `@Embeddable`

Introduce a `MoneyEmbeddable` in `account.infrastructure` (an infrastructure
type, distinct from the domain `Money` record, which stays annotation-free).
It maps two columns and is embedded into the entity via `@Embedded` +
`@AttributeOverride` so the column names can be qualified per use
(`initial_balance_amount`, `initial_balance_currency`). The adapter translates
`Money ⇄ MoneyEmbeddable` in its `toEntity` / `toDomain` methods.

- Amount → `numeric(19, 4)`, matching `Money`'s fixed scale of 4.
- Currency → Hibernate 6 maps `java.util.Currency` natively to its ISO-4217
  code as a `VARCHAR`; no `AttributeConverter` is required.

`MoneyEmbeddable` is designed for reuse wherever `Money` is persisted (with
`@AttributeOverride` renaming the columns per field), but its **location**
follows the rule of three rather than being shared pre-emptively:

- **Now** it lives in `account.infrastructure`: `account` is its only consumer,
  so there is no cross-context coupling, and the move later is a trivial refactor
  in the monorepo.
- It is deliberately **not** placed in the `com.pecunia.shared` kernel: the
  kernel is a *framework-free* sink (an ArchUnit rule forbids Spring / JPA /
  Hibernate / Lombok anywhere under `com.pecunia.shared..`), so a JPA
  `@Embeddable` cannot live there.
- It is equally not a permanent fit for `account.infrastructure`: once a second
  context persists `Money` (e.g. `transaction` at Block 3), leaving it in
  `account` would force `transaction.infrastructure` to depend on
  `account.infrastructure` — a context coupling to another context's adapters.

**Promotion trigger:** when the second consumer appears, promote
`MoneyEmbeddable` to a dedicated *shared infrastructure* location distinct from
the pure kernel (e.g. `com.pecunia.shared.persistence`), scoping the
framework-freedom ArchUnit rule to the kernel proper. That expansion of what
"shared" means is a deliberate decision to be recorded in its own ADR at that
point.

### 2. Carry an optimistic-lock `@Version` on the aggregate to drive new-entity detection

`AccountEntity` declares a JPA `@Version` field of a **wrapper** type
(`private Long version;` — never the primitive `long`). Spring Data's
`save(Account)` keeps its single method, and its default new-entity detection
inspects the `@Version` **before** the `@Id`:

- **insert** (opening an account) → `version == null` → `persist()`, **no
  phantom `SELECT`**;
- **update** (archiving an account) → `version != null` → `merge()`; Hibernate
  also increments the version and fails a concurrent stale write with
  `OptimisticLockException`.

The version is a wrapper so that "not yet persisted" is representable as `null`;
a primitive `long` cannot be null, which is precisely why Spring Data would fall
back to the (always-non-null) `@Id` and mis-route every insert through `merge()`.

**The version lives on the `Account` aggregate, not only on the entity.** Our
adapter rebuilds a fresh `AccountEntity` from the domain object on every
`save()` (two-way mapping). An entity rebuilt for an *update* would otherwise
carry a `null` version and be misdetected as new → `persist()` → a duplicate-key
`INSERT`. The version must therefore survive the round-trip through the domain:

- `Account.open(...)` creates an **unversioned** aggregate (no persisted state
  yet);
- `Account.reconstitute(..., version)` carries the value loaded from the
  database;
- the adapter maps it both ways (`toEntity` writes it, `toDomain` reads it back).

To respect the project's *no `null` across boundaries* rule, the aggregate
exposes the version as `Optional<Long> version()` (empty for a freshly opened
account, present once persisted); the field is nullable only internally. Placing
the version on the aggregate is a deliberate, small concession: an
optimistic-lock token is legitimately part of an aggregate's **consistency
contract**, not merely a storage detail, so it is defensible domain surface
rather than a leak.

This choice buys **two** things at once — the elimination of the insert-time
`SELECT` *and* optimistic locking against lost updates — for the cost of one
technical field threaded through the aggregate.

### Scope note — duplicate `OpenAccount` submissions are not a persistence concern

Because `account` uses a fresh surrogate PK per insert (no natural key), the
persistence layer **cannot** and **must not** deduplicate a double submission:
two concurrent `OpenAccount` calls produce two distinct, individually valid
rows. Guarding against a double-click / client retry is an **application/web**
responsibility — an idempotency key carried by the request and checked upstream
of the adapter. This is deliberately out of scope for this slice and deferred
(likely post-MVP for a single user). This contrasts with `identity`, whose
natural-key upsert ([ADR-0029](0029-idempotent-user-provisioning.md)) made
provisioning intrinsically idempotent at the persistence layer; that mechanism
does not transfer here precisely because there is no natural key to converge on.

## Consequences

### Positive

- **No mapping drift.** `Money` has one persistence representation reused by
  every future entity; assigned-ID entities have one lifecycle pattern.
- **No phantom `SELECT` on insert** (the `@Version` routes a fresh aggregate
  straight to `persist()`), and standard JPA lifecycle preserved (unlike a native
  upsert, which bypasses dirty checking).
- **Optimistic locking for free.** The same `@Version` guards against lost
  updates: a concurrent stale write fails with `OptimisticLockException` rather
  than silently overwriting.
- The domain stays annotation-free; the JPA coupling lives only in
  `infrastructure`.

### Negative

- `MoneyEmbeddable` plus `@AttributeOverride` is more machinery than two flat
  columns for the single entity that exists today (accepted: `Money` recurs
  from Block 3 onwards).
- The aggregate gains a **technical `version` field** (a small concession of
  domain purity, mitigated by exposing it as `Optional<Long>` and by its being a
  genuine consistency-contract concept). The adapter must map it both ways; a
  version dropped on `reconstitute` would resurface as a new-entity misdetection
  (a loud duplicate-key error, not silent corruption — acceptable).

### Neutral

- `Account` is immutable except for `status`; today `save()` on update only ever
  changes one column. Should the aggregate gain more mutable state, the mapping
  is unaffected — `merge()` already handles arbitrary dirty fields.
- Optimistic locking is unnecessary for a single-user MVP, but it costs nothing
  extra here (it falls out of the mechanism chosen to avoid the insert `SELECT`)
  and is correct the day concurrency matters.

## Alternatives Considered

### 1A. Two flat columns on the entity (no embeddable)

Map `initialBalanceAmount` / `initialBalanceCurrency` directly on
`AccountEntity`, as `UserEntity` maps its scalars. Simpler for one entity, and
consistent with the `identity` precedent. Rejected because `Money` reappears on
`Transaction` and budgets almost immediately; the flat approach duplicates the
column pair and its mapping in every entity, which the embeddable eliminates.

### 2A. Plain `save()` with `merge()` everywhere (no `@Version`)

Do nothing; let Spring Data `merge()` every assigned-ID entity — the approach
most hexagonal examples take (e.g. Arho Huttunen's, which uses the same
application-generated-UUID + two-way-mapping setup and never adds new-entity
detection). Correct and simplest, and at MVP scale the insert-time `SELECT` is
negligible (the update path is already free — the entity is a persistence-context
hit). Rejected only because adding a `@Version` costs little, removes that
`SELECT`, **and** yields optimistic locking; kept documented as the fallback if
the version field on the aggregate is ever judged too intrusive.

### 2B. `Persistable<UUID>` with a `@Transient isNew` flag

Implement `Persistable` and flip a transient `isNew` flag via `@PostLoad` /
`@PostPersist`. Rejected: the flag pattern assumes the *same* managed instance is
loaded, mutated and saved. Our adapter **rebuilds** the entity from the domain on
every `save()`, so the entity reaching `save()` on an update is a brand-new
object with `isNew == true` → `persist()` → duplicate-key `INSERT`. Making it work
would require threading provenance through the domain anyway — which is exactly
what the `@Version` does, more cleanly and with optimistic locking as a bonus
(Vlad Mihalcea explicitly prefers `@Version` over the transient flag for assigned
identifiers).

### 2C. PostgreSQL native upsert (as in `identity`)

`INSERT ... ON CONFLICT (id) DO UPDATE SET status = excluded.status`. Rejected:
`account` has no natural-key idempotency requirement (a fresh surrogate id per
open account means there is no conflict to resolve), so the upsert solves a
problem that does not exist here, while bypassing JPA dirty checking and
optimistic locking. The `identity` upsert was justified by a natural-key
find-or-create race; that rationale does not transfer.

## References

- [Spring Data JPA — Persisting Entities (`isNew` / `@Version` / `Persistable`)](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
- [Vlad Mihalcea — How do `persist` and `merge` work in JPA (assigned id → `@Version`)](https://vladmihalcea.com/jpa-persist-and-merge/)
- [Arho Huttunen — Hexagonal Architecture with Spring Boot (two-way mapping persistence adapter, application UUIDs)](https://www.arhohuttunen.com/hexagonal-architecture-spring-boot/)
- [JPA Buddy — The Ultimate Guide on Client-Generated IDs in JPA Entities](https://jpa-buddy.com/blog/the-ultimate-guide-on-client/)
- [Vlad Mihalcea — The best way to map a MonetaryAmount with JPA and Hibernate](https://vladmihalcea.com/monetaryamount-jpa-hibernate/)
- [Java Persistence with Spring Data and Hibernate — Mapping value types](https://livebook.manning.com/book/java-persistence-with-spring-data-and-hibernate/chapter-6/v-10)
- [ADR-0013](0013-selective-use-of-lombok.md) — domain stays framework-free (extended here to JPA annotations)
- [ADR-0015](0015-uuid-v7-application-side-generation.md) — application-assigned UUID v7 IDs (why `@Id` is never null)
- [ADR-0029](0029-idempotent-user-provisioning.md) — the natural-key upsert whose rationale does not transfer to `account`
- [ADR-0031](0031-persistence-tenancy-isolation-and-context-boundaries.md) — tenancy isolation and cross-context references at the persistence layer

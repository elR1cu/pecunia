# ADR-0031: Persistence-layer tenancy isolation and cross-context references

## Status

Accepted

## Context

Pecunia is multi-tenant from day one ([ADR-0014](0014-multi-tenant-architecture.md)):
every aggregate root has an owning `UserId`, every query filters by owner, and
ownership failures map to HTTP 404. ADR-0014 defines three isolation layers —
authentication, application-level ownership checks, and PostgreSQL Row-Level
Security (RLS) **planned post-MVP** as defense in depth.

The `account` persistence adapter is the first place these principles meet real
JPA/SQL, and it forces two decisions ADR-0014 left open:

1. **Cross-context reference.** An account's `owner_id` holds a `UserId`, whose
   canonical row lives in the `users` table of the **`identity`** context. Does
   `accounts.owner_id` get a database foreign key to `users.id`?

2. **Isolation mechanism at the persistence layer.** How is "filter by owner"
   actually enforced — by explicit parameters, by a Hibernate ambient tenant
   filter (`@TenantId` / `@Filter`), or by RLS now?

Two facts constrain the choices. First, the driven port already forces the
owner into every read: `AccountRepository` deliberately exposes
`findByIdAndOwner(id, owner)` and `findAllByOwner(owner)` and **no**
`findById(id)`, so it is structurally impossible to write a read that forgets
the ownership predicate (see the port Javadoc, [ADR-0026](0026-ports-in-application-layer.md),
[ADR-0027](0027-error-modeling-strategy.md)). Second, `owner` is never user
input: it is taken from the authenticated principal on every use case, so an
`owner_id` pointing at a non-existent user cannot arise from normal flow.

## Decision

### 3. No cross-context foreign key; `owner_id` is a plain indexed `uuid`

`accounts.owner_id` is a `uuid NOT NULL` with **no** `REFERENCES users(id)`
constraint, and a non-unique index `idx_accounts_owner (owner_id)`.

The DDD guidance for a modular monolith is to **share identifiers across
bounded contexts, never entities or schema-level joins**. A FK from `account`
to the `identity`-owned `users` table would couple the two contexts' schemas,
constrain their migration order, and block a future extraction. Referential
integrity for `owner_id` is instead guaranteed by the application: the value
always comes from an authenticated `UserId` that was provisioned before any
account can be opened.

The index is required regardless of the FK question: every `AccountRepository`
read filters on `owner_id`.

### 4. Enforce isolation with explicit `owner` parameters, not an ambient tenant filter

Ownership is enforced by the **explicit `owner` argument in the port signatures**,
translated by the adapter into a `WHERE owner_id = ?` predicate on every read.
Pecunia does **not** adopt Hibernate `@TenantId` / `@Filter` ambient
current-tenant resolution, and does **not** add RLS in this slice.

RLS remains the **post-MVP** third isolation layer defined in ADR-0014, to be
introduced (with its own ADR) as defense in depth. The explicit-parameter
approach already provides the "impossible to forget the filter" guarantee that
ambient mechanisms are usually adopted for, without the ambient state.

### Mandatory cross-user isolation test

Per ADR-0014, this slice ships the cross-user isolation test as an integration
test (Testcontainers): user B must not read user A's account
(`findByIdAndOwner` returns empty; the use case maps it to 404), and no data
leaks. It runs on every CI build.

## Consequences

### Positive

- **Clean context boundary.** No schema coupling between `account` and
  `identity`; each context owns its tables and migrations independently.
- **Isolation guaranteed by the port shape**, not by developer vigilance or
  ambient configuration: a read without an owner predicate is not expressible.
- **No ambient state to manage** (no thread-local tenant, no filter
  enable/disable lifecycle), which suits the per-request explicit-owner model.

### Negative

- **No database-level referential integrity for `owner_id`.** A bug that
  fabricated an owner would not be caught by a FK. Mitigated by `owner` always
  originating from the authenticated principal, by the ownership use-case
  checks, and later by RLS.
- **No defense-in-depth at the storage layer yet.** A query written outside the
  port (e.g. a future reporting path) would not be automatically owner-scoped
  until RLS lands. Mitigated by routing all reads through the port and by the
  cross-user isolation test.

### Neutral

- Filtering by owner is effectively free with `idx_accounts_owner` in place
  (ADR-0014 already assumes an indexed `owner_id` on every aggregate).
- If user-to-user features ever arrive (explicitly out of MVP scope), the
  explicit-owner model and the missing FK would both be revisited.

## Alternatives Considered

### 3A. Foreign key `accounts.owner_id → users.id`

Enforce referential integrity in the database. Rejected: it couples the
`account` and `identity` schemas across a bounded-context boundary, complicates
independent migrations, and blocks extraction — for a guarantee the application
already provides, since `owner` is never user input.

### 4A. Hibernate `@TenantId` / `@Filter` (ambient current-tenant)

Resolve the current tenant from a thread-local / request scope and let Hibernate
append the predicate automatically. Rejected for this slice: it adds ambient
state and a filter lifecycle to obtain the "can't forget the filter" property
that the explicit-parameter port design already guarantees. Reconsiderable if
the number of owner-scoped queries grows unwieldy.

### 4B. PostgreSQL Row-Level Security now

The strongest ("hard") isolation — even a query outside the port cannot escape
the fence. Rejected **for now**, not on merit: ADR-0014 schedules RLS as a
post-MVP defense-in-depth layer, and adding session-variable plumbing and
policies now duplicates the query-level guarantee the ports already provide.
Planned as a later ADR.

## References

- [ADR-0014](0014-multi-tenant-architecture.md) — multi-tenant architecture; three isolation layers; RLS post-MVP
- [ADR-0026](0026-ports-in-application-layer.md) — ports in the application layer
- [ADR-0027](0027-error-modeling-strategy.md) — error modeling (ownership failure → 404)
- [The Reformed Programmer — Passing data between bounded contexts](https://www.thereformedprogrammer.net/evolving-modular-monoliths-3-passing-data-between-bounded-contexts/)
- [kgrzybek — Modular Monolith with DDD (share IDs, not entities)](https://github.com/kgrzybek/modular-monolith-with-ddd)
- [Baeldung — Multitenancy with Spring Data JPA](https://www.baeldung.com/multitenancy-with-spring-data-jpa)
- [PostgreSQL — Row Security Policies](https://www.postgresql.org/docs/current/ddl-rowsecurity.html)

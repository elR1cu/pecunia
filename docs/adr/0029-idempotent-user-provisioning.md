# ADR-0029: Concurrency-safe JIT user provisioning via a database upsert

## Status

Accepted

## Context

The `identity` context provisions the internal `User` aggregate **just-in-time
at the first OIDC login** (see [ADR-0028](0028-internal-user-identity-mapping.md):
an internal `UserId` decoupled from the IdP, keyed by the OpenID Connect
`(iss, sub)` pair, persisted in the `users` table with a
`UNIQUE (idp_issuer, idp_subject)` constraint). The `ProvisionUser` use case is
a **find-or-create** by `IdpIdentity`.

`find` then `create` is a *check-then-act* sequence, and it is not atomic. Two
concurrent **first** logins for the same `(iss, sub)` can interleave:

| time | request A | request B |
|------|-----------|-----------|
| t1 | `findByIdpIdentity` → empty | |
| t2 | | `findByIdpIdentity` → empty |
| t3 | `save` → inserts the row | |
| t4 | | `save` → violates `uq_users_idp_identity` |

The unique constraint correctly prevents the duplicate, but request B then
fails. Without handling, that surfaces as an **HTTP 500 at the very first
login** — the worst possible moment. The window is narrow (only before the
user exists; afterwards `find` returns the row and no insert is attempted) but
real: double-click on "sign in", two open tabs, network retries, browser
prefetch.

Two mechanisms can make the find-or-create idempotent under concurrency:
handle the unique violation in application code, or delegate atomicity to the
database. The choice interacts with Spring's transaction semantics and with
PostgreSQL's write behavior.

Constraint of the environment: Pecunia targets **PostgreSQL only**, and the
hexagonal architecture confines all persistence details behind the
`UserRepository` port.

## Decision

**Delegate atomicity to PostgreSQL.** The `UserRepository` JPA adapter
implements an idempotent *insert-or-ignore* with
`INSERT ... ON CONFLICT (idp_issuer, idp_subject) DO NOTHING`, followed by a
`SELECT` (the **insert-then-select** pattern — necessary because
`ON CONFLICT DO NOTHING` returns no row when it skips the insert).

Port shape (`identity.application.port.out`):

- `Optional<User> findByIdpIdentity(IdpIdentity idpIdentity)`
- `void save(User user)` — **idempotent insert-or-ignore**. Because `User` is
  immutable (its `id` and `IdpIdentity` are fixed for life), `save` never
  updates: a pre-existing row for the same identity is a no-op, never an error.

The `ProvisionUserService` runs as a single `@Transactional` method:

```
find(idpIdentity)
  → present? return user.id()
  → absent?  id = new UserId(idGenerator.newId())
             save(User.register(id, idpIdentity))   // insert-or-ignore
             return find(idpIdentity).id()          // ours, or the concurrent winner's
```

The `find`-first keeps the common case (a returning user) at a single `SELECT`;
the upsert + re-find path runs only at the first login.

## Consequences

### Positive

- **No transactional trap.** `ON CONFLICT DO NOTHING` raises no exception, so
  the transaction is never marked `rollback-only`; the re-`find` executes in the
  same transaction. This avoids the `REQUIRES_NEW` propagation plumbing that the
  application-level catch would require.
- **No table/index bloat.** `ON CONFLICT` checks before writing. A plain
  `INSERT` that fails on the unique index writes the row first, then marks it
  dead — accumulating dead tuples in the table and index over time.
- **Correctness proven by the database**, not by careful thread reasoning; the
  service stays a simple single transaction.
- Common path is a single `SELECT`; the upsert cost is paid once, at first login.

### Negative

- **Vendor coupling.** `ON CONFLICT` is PostgreSQL-specific native SQL; the
  adapter uses a native query rather than the stock Spring Data `save()`.
  Accepted: the project is Postgres-only, and the coupling is confined to one
  adapter behind the port.
- The **idempotent semantics of `save()` are not obvious from the name**; they
  are documented on the port's Javadoc and here.
- The race handling is verified by an **integration test**, not a unit test.

### Neutral

- `User` being immutable, `save()` is purely insert-if-absent; there is no
  update path to reason about. Should `User` gain mutable state later, `save()`
  semantics must be revisited (e.g. split into add/update, or move to
  `ON CONFLICT ... DO UPDATE`).
- The candidate `UserId` generated before the upsert may be discarded when a
  concurrent login wins the insert. `UserId`s are cheap (UUID v7), so the waste
  is negligible.

## Alternatives Considered

### A. Application-level catch of `DataIntegrityViolationException`

Attempt the insert; on a unique violation, assume a concurrent insert happened
and re-`find`. Rejected because: (1) inside a single `@Transactional` method the
failed insert marks the transaction `rollback-only`, so the subsequent re-`find`
fails at commit with `UnexpectedRollbackException` — correct handling requires
isolating the insert in a separate transaction
(`@Transactional(propagation = REQUIRES_NEW)`), adding non-trivial propagation
plumbing; (2) the failing `INSERT` causes table/index bloat (see above).

### B. Pessimistic / advisory lock

Serialize provisioning with a lock (a row lock, or `pg_advisory_lock` on a hash
of `(iss, sub)`). Rejected as overkill for a rare first-login race: it adds
contention and complexity for no benefit over the upsert.

### C. Pre-check only (find, then plain insert)

The naive version — this *is* the race. Rejected: it is exactly the
check-then-act bug this ADR removes.

## References

- [PostgreSQL — INSERT / ON CONFLICT](https://www.postgresql.org/docs/current/sql-insert.html)
- [Haki Benita — How to Get or Create in PostgreSQL](https://hakibenita.com/postgresql-get-or-create)
- [credativ — Unique constraint violations cause table and index bloat](https://www.credativ.de/en/blog/credativ-inside/inserts-failing-on-unique-constraint-violations-cause-table-and-index-bloat-in-postgresql/)
- [Baeldung — Spring DataIntegrityViolationException](https://www.baeldung.com/spring-dataintegrityviolationexception)
- [ADR-0028](0028-internal-user-identity-mapping.md) — internal user identity mapping (the decision this ADR makes concurrency-safe)
- [ADR-0027](0027-error-modeling-strategy.md) — error modeling (why an expected, idempotent outcome is not modeled as an exception)

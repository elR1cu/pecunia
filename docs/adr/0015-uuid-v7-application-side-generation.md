# ADR-0015: UUID v7 Generated Application-side

## Status

Accepted

## Context

Pecunia uses UUIDs as primary keys for all aggregate roots. Two
orthogonal decisions must be made:

1. **Where are UUIDs generated?**
   - Application-side (in Java code, before persistence).
   - Database-side (PostgreSQL generates the UUID at INSERT time).

2. **Which UUID variant?**
   - UUID v4 (random).
   - UUID v7 (RFC 9562, time-ordered).

These decisions have significant implications for performance,
testability, and architecture.

### Performance concerns with UUID v4

UUID v4 is random by design. When used as a primary key in a B-tree
index (the default in PostgreSQL), random insertions cause:

- Frequent page splits across the index.
- Poor cache locality (consecutive inserts hit unrelated pages).
- Larger index size due to fragmentation.
- Insertion performance 30%–70% slower than sequential keys in benchmarks.

For tables with high insertion rates (e.g., `transactions` during a
camt.053 import), this matters.

### Database-side generation

PostgreSQL supports `gen_random_uuid()` (UUID v4) natively. UUID v7
generation can be added via extensions or custom functions. Letting the
database generate UUIDs avoids the random-distribution problem because
the DB can produce sortable UUIDs.

However, database-side generation has significant drawbacks:

- Entities cannot be tested in isolation: until persisted, they have no
  ID.
- Domain events must be published after persistence, complicating
  patterns like outbox.
- Couples the domain layer to a specific persistence strategy.

## Decision

Pecunia generates UUIDs **application-side**, using **UUID v7**.

### Library

Until JDK 26 ships with native `UUID.ofEpochMillis(long)`, Pecunia uses
the `com.github.f4b6a3:uuid-creator` library. The wrapper for ID
generation is encapsulated in a small infrastructure utility, so
migration to the JDK 26 API is a single-file change.

### Type safety

Each entity has a typed wrapper for its identifier:

```java
public record AccountId(UUID value) {
    public static AccountId newId() {
        return new AccountId(UuidCreator.getTimeOrderedEpoch()); // UUID v7
    }
}
```

This prevents passing an `AccountId` where a `TransactionId` is expected.

## Consequences

### Positive

- **Index locality**: UUID v7 is time-ordered, providing B-tree
  performance comparable to sequential keys.
- **Testability**: aggregates have IDs at creation time, without database
  access. Unit tests run without persistence infrastructure.
- **Event-driven friendliness**: domain events carry the entity ID at
  creation, before any DB interaction. Simplifies patterns like
  transactional outbox.
- **Domain decoupling**: the domain layer does not depend on persistence
  technology. Switching databases requires no change to identifier
  semantics.
- **Type-safe IDs**: typed wrappers prevent identifier-mixing bugs at
  compile time.
- **Unpredictability**: UUID v7's random bits preserve the privacy
  benefits of UUIDs (cannot be enumerated externally).

### Negative

- **External library** until JDK 26: one additional dependency
  (`uuid-creator`). Small footprint (~30 KB), no transitive dependencies,
  well-maintained.
- **Slightly larger storage** than `bigint`: 16 bytes vs 8 bytes per
  primary key. Insignificant at Pecunia's scale.
- **Clock dependency**: UUID v7's timestamp depends on the system clock.
  Severe clock skew could produce non-monotonic IDs (still unique, but
  not perfectly sorted). Mitigated in practice by NTP.

### Neutral

- **JDK 26 migration**: when JDK 26 is adopted, the library is replaced
  by `java.util.UUID.ofEpochMillis(long)`. The change is localized to a
  single utility class.

## Alternatives Considered

### Application-side UUID v4

Rejected because:
- Index fragmentation degrades insertion performance.
- B-tree page splits increase storage overhead.
- The benefits of v4 (pure randomness) are not needed; v7 retains enough
  randomness for unpredictability.

### Database-side UUID generation

Rejected because:
- Aggregates lack an ID until persisted, breaking unit test ergonomics
  and event-driven patterns.
- Couples the domain layer to the persistence technology.
- The performance argument is weaker than expected once UUID v7 is
  considered.

### Bigint auto-increment

Rejected because:
- Auto-incremented IDs leak business information (order of creation,
  approximate volumes).
- Hard to merge data across environments (e.g., production and staging
  collide on IDs).
- Distributed-system unfriendly: multiple writers cannot generate IDs
  without coordination.

### ULID, KSUID, NanoID

Considered. All offer some flavor of sortable, random identifiers. UUID
v7 is preferred because:
- It is an IETF standard (RFC 9562), unlike ULID and KSUID.
- It uses the standard UUID 128-bit format, compatible with all
  databases and tooling expecting UUIDs.
- The JDK 26 API will provide it natively.

## References

- RFC 9562, "Universally Unique IDentifiers (UUIDs)":
  https://datatracker.ietf.org/doc/html/rfc9562
- JEP for UUID v7 in JDK 26: https://bugs.openjdk.org/browse/JDK-8334015
- UUID Creator library:
  https://github.com/f4b6a3/uuid-creator
- "Choosing a Postgres Primary Key" by Supabase (excellent comparison):
  https://supabase.com/blog/choosing-a-postgres-primary-key

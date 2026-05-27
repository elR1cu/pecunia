# ADR-0014: Multi-tenant Architecture from Day One

## Status

Accepted

## Context

The MVP version of Pecunia serves a single real user (the author) in
production. The temptation, common in personal projects, is to simplify
the data model and code by assuming a single user globally: no `userId`
field on entities, no filtering by user in queries, no ownership checks.

This approach has significant long-term costs:

- Retrofitting multi-tenancy onto an existing codebase is one of the most
  invasive refactorings possible. Every query, every endpoint, every test,
  every authorization check is affected.
- The "single-user assumption" leaks into design decisions (e.g., caching
  strategies, identifier generation, database constraints) in ways that
  are difficult to unwind.
- A portfolio project that assumes a single user signals a lack of
  awareness of production-grade concerns.

The alternative is to architect the application as multi-tenant from day
one, even when the production deployment hosts a single user. The cost is
discipline (every query filters by owner, every entity has an owner field);
the benefit is correctness, security, and demonstrability.

## Decision

Pecunia is architected as a **multi-tenant application from Block 1
onwards**.

### What multi-tenancy means here

- Every aggregate root in the domain has an owning `UserId`.
- Every query against the database filters by the current user.
- Every use case verifies that the entities it operates on belong to the
  current user before exposing data.
- The architecture supports adding new user accounts without code change.

### What multi-tenancy does NOT mean in the MVP

- **No user-to-user features**: the MVP does not implement family sharing,
  joint accounts, delegations, or any form of data sharing between users.
  These are scoped as future features and would require explicit design
  work.
- **No user management UI**: user registration, profile management, and
  account deletion flows are minimal in the MVP (Keycloak handles them).
- **No tenant-level customization**: features, branding, and configuration
  are uniform across users.

### Three layers of isolation

1. **Authentication** (Spring Security): no anonymous access.
2. **Application-level ownership checks**: every use case verifies
   ownership before returning data. Ownership failures map to HTTP 404
   (not 403) to avoid leaking the existence of resources.
3. **Database Row-Level Security** (planned post-MVP): PostgreSQL RLS
   policies enforce ownership at the storage layer as defense in depth.

### Test discipline

A cross-user isolation test is mandatory from Block 2:

```
Given user A has data D
And user B is authenticated
When user B requests data D
Then the response is 404 Not Found
And no data leaks in the response body or logs
```

This test is the canary for multi-tenancy regressions and runs on every
CI build.

## Consequences

### Positive

- **No retrofit risk**: the most painful refactoring is avoided entirely.
- **Production-grade signal**: the architecture demonstrates awareness of
  enterprise concerns (data isolation, defense in depth).
- **Security by design**: ownership checks are uniform and predictable.
- **Future evolution is easy**: adding family sharing, organization
  accounts, or B2B tenants becomes additive work, not a rewrite.
- **Strong CV signal**: explaining the choice in interviews demonstrates
  senior-level architectural judgment.

### Negative

- **Discipline overhead**: every query, every use case, every test must
  respect the ownership pattern.
- **More test surface**: cross-user isolation tests add to the test
  matrix.
- **Slightly more code in queries**: explicit owner predicates are
  required even when the application has one user.

### Neutral

- **Performance**: filtering by owner is essentially free when the
  `owner_id` column is indexed (which it is, by default for all aggregate
  roots in Pecunia).

## Alternatives Considered

### Single-user MVP with multi-tenancy retrofit later

Rejected because:
- Retrofitting is significantly more expensive than designing it in.
- The single-user assumption tends to bake into many design choices in
  subtle ways that are hard to find later.
- Signals a less senior approach in the portfolio.

### Multi-tenancy via schema-per-tenant or database-per-tenant

Considered for completeness. Rejected because:
- Overkill for the scale (single user in MVP, low expected user count
  even post-MVP).
- Operationally complex (migrations across N schemas, connection pool
  management).
- The row-level approach with `owner_id` columns is the right scale-down
  of multi-tenancy for B2C-style applications like Pecunia.

## References

- "Multi-Tenant Data Architecture" by Microsoft:
  https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/data-architecture
- "Saas Tenant Isolation Strategies" by AWS:
  https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/tenant-isolation.html
- PostgreSQL Row-Level Security documentation:
  https://www.postgresql.org/docs/current/ddl-rowsecurity.html

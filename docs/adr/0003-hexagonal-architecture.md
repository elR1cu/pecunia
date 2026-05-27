# ADR-0003: Hexagonal Architecture

## Status

Accepted

## Context

Pecunia must be testable, evolvable, and resistant to framework or
infrastructure churn. Business logic — categorizing transactions, computing
savings capacity, validating imports — should not be coupled to Spring,
Hibernate, HTTP, or any specific persistence technology.

The project is intended as a long-lived personal tool **and** a portfolio
demonstration of senior engineering practices. Both audiences (the author's
future self and external reviewers) benefit from a codebase where:

- Business rules are expressed in plain Java, easy to read and reason about.
- Infrastructure concerns can be replaced without touching the domain
  (e.g., swap Spring Events for Kafka, switch persistence technology, change
  the HTTP framework).
- Tests for business logic do not require Spring contexts, databases, or
  network access.

Three styles were considered:

1. **Layered / N-tier architecture**: controllers → services → repositories.
   The default for many Spring projects.
2. **Hexagonal architecture (Ports & Adapters)**: domain at the center,
   surrounded by adapters that implement ports defined by the domain.
3. **Clean Architecture** (Uncle Bob): similar to hexagonal but with stricter
   concentric layers (entities, use cases, interface adapters, frameworks).

## Decision

Pecunia adopts **Hexagonal Architecture** (Ports & Adapters), as described
by Alistair Cockburn.

### Layer responsibilities

- **Domain layer**: pure Java. Contains entities, value objects, aggregates,
  domain services, ports (interfaces), and domain events. No dependency on
  Spring, JPA, or any infrastructure library.
- **Application layer**: use case classes that orchestrate domain objects
  and call ports. Defines transaction boundaries. Maps between domain types
  and DTOs.
- **Web layer (driving adapter)**: Spring controllers implementing
  OpenAPI-generated interfaces. Thin: validates input, calls use cases, maps
  results to HTTP responses.
- **Infrastructure layer (driven adapters)**: implements the domain's ports.
  JPA repositories, event publishers, external API clients, file parsers.

### Dependency direction

Dependencies always point **inward**: web depends on application, application
depends on domain. Infrastructure depends on domain (to implement ports) but
the domain has no knowledge of infrastructure.

### Package structure

Code is organized by **bounded context first**, then by layer within the
context:

```
com.pecunia.transaction
├── domain
├── application
├── web
└── infrastructure
```

This makes each context self-contained and prevents the common "package by
layer" anti-pattern where features are scattered across the codebase.

## Consequences

### Positive

- **Testability**: business logic tests run in milliseconds, without Spring
  or databases.
- **Replaceability**: adapters can be swapped (e.g., Spring Events → Kafka)
  without modifying domain code.
- **Clarity of intent**: the domain layer reads as business logic, not as
  framework plumbing.
- **Evolvability**: adding a new entry point (e.g., a CLI, a scheduled job)
  is straightforward — write a new driving adapter that calls existing use
  cases.
- **Strong signal in the codebase**: senior reviewers immediately recognize
  the discipline.

### Negative

- **More indirection**: a use case typically goes through a port to reach an
  adapter, adding a layer compared to direct repository injection.
- **Learning curve**: the discipline of "no Spring in domain" requires
  vigilance, especially for developers new to the pattern.
- **More files**: each port has at least one implementation; this multiplies
  the count of files.
- **Mapping overhead**: domain entities and persistence entities may diverge,
  requiring mappers.

### Neutral

- **No enforced runtime cost**: hexagonal is a structural discipline, not a
  performance trade-off. The JVM optimizes through the indirection.

## Alternatives Considered

### Layered / N-tier (controllers → services → repositories)

Rejected because:
- It does not enforce a clear domain boundary; Spring annotations and JPA
  entities tend to pollute business logic.
- Tests for "services" often require Spring contexts in practice.
- It is the conventional choice and would not differentiate the project.

### Clean Architecture (Uncle Bob)

Considered but rejected as primary reference because:
- It is essentially a more prescriptive variant of hexagonal.
- The stricter naming and layering conventions add ceremony without
  significant benefit at Pecunia's scale.
- Hexagonal's port/adapter vocabulary is more widely understood and easier
  to communicate.

The two are compatible. Pecunia uses hexagonal as the primary frame; some
Clean Architecture ideas (e.g., explicit use case classes) are also adopted.

## References

- Alistair Cockburn, "Hexagonal Architecture" (2005):
  https://alistair.cockburn.us/hexagonal-architecture/
- Tom Hombergs, "Get Your Hands Dirty on Clean Architecture" — practical
  introduction to hexagonal in a Spring Boot context.
- Vlad Khononov, "Learning Domain-Driven Design" — bounded contexts and
  domain modeling.

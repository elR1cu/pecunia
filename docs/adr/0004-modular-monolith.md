# ADR-0004: Modular Monolith over Microservices

## Status

Accepted

## Context

Modern Java systems are often built as microservices: independent services
communicating over the network, each with its own database and deployment
lifecycle. Microservices are well-suited to large teams, independent
scaling needs, and heterogeneous technology stacks.

Pecunia is a single-author personal application with modest scale
requirements (one user, ~100 transactions per month). However, it is also a
portfolio demonstration intended for senior engineering positions, where
familiarity with both monolithic and microservice architectures is expected.

Three options were considered:

1. **Plain monolith**: a single deployable, internally organized by technical
   layers (e.g., controllers, services, repositories) with no enforced
   module boundaries.
2. **Modular monolith**: a single deployable, internally organized by
   business modules with clear interfaces and minimal coupling.
3. **Microservices**: multiple independently deployable services, each
   responsible for a bounded context.

## Decision

Pecunia is built as a **modular monolith**.

The application is a single Spring Boot deployable. Internally, it is
organized into bounded contexts (`account`, `transaction`, `category`,
`budget`, etc.), each containing its own domain, application, web, and
infrastructure layers.

Inter-context communication happens through:
- **Domain events**, published via the `DomainEventPublisher` port.
- **Explicit application-level contracts** (use case invocations) when
  synchronous interaction is required.

Cross-context access to internal entities or repositories is forbidden. Each
context is treated as if it were a separate microservice, even though it
runs in the same JVM.

## Consequences

### Positive

- **Operational simplicity**: one process to deploy, monitor, and back up.
  Matches the modest scale.
- **Performance**: in-process method calls are orders of magnitude faster
  than network calls.
- **Transactional consistency**: a single database transaction can span
  multiple contexts when truly needed (though this is avoided in favor of
  eventual consistency via events).
- **Lower cost**: single VPS deployment, no service mesh, no inter-service
  authentication, no distributed tracing required initially.
- **Extraction path preserved**: the strict module boundaries mean a
  bounded context could be extracted into a separate microservice with
  limited refactoring.
- **Demonstrates senior judgment**: choosing a monolith for a personal
  project, while justifying it explicitly, is a stronger signal than building
  premature microservices.

### Negative

- **Discipline required**: without enforced boundaries, modules tend to grow
  tangled. Pecunia relies on:
  - Package-level visibility (no public access to internal classes outside
    the module's public API).
  - Code review (self-review for now).
  - Static analysis with ArchUnit (planned post-MVP).
- **Single deployment unit**: any change requires redeploying the entire
  application.
- **Single technology stack**: all modules share the same Java/Spring runtime,
  no opportunity to mix languages or frameworks per context.

### Neutral

- **Shared database**: all modules use the same PostgreSQL instance. Each
  module owns its own schemas/tables; cross-module access at the database
  level is forbidden. This is enforced by convention.

## Alternatives Considered

### Plain monolith (layer-first organization)

Rejected because:
- It encourages cross-context coupling through shared service classes.
- It does not demonstrate the discipline expected of senior engineers.
- It would make a future extraction to microservices significantly harder.

### Microservices from the start

Rejected because:
- The scale does not justify the operational complexity.
- Implementing distributed tracing, service-to-service authentication,
  schema versioning, and saga orchestration is significant work for a
  personal-scale system.
- The cost of multiple instances (compute, networking) is prohibitive on
  the chosen frugal hosting model.
- Premature microservices are widely recognized as an anti-pattern.

A microservice extraction may happen post-MVP if the project's scope grows
substantially, or as a portfolio demonstration of refactoring.

## References

- Sam Newman, "Building Microservices" — emphasizes starting with a
  monolith.
- Simon Brown, "Modular Monoliths" talk:
  https://www.youtube.com/watch?v=5OjqD-ow8GE
- Kamil Grzybek, "Modular Monolith: A Primer":
  https://www.kamilgrzybek.com/design/modular-monolith-primer/

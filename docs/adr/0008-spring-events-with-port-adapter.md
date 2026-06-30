# ADR-0008: Spring ApplicationEventPublisher with Port/Adapter for Future Kafka Migration

## Status

Accepted

Note: the `DomainEventPublisher` port lives in the application layer
(`port.out`), not the domain — see
[ADR-0026](0026-ports-in-application-layer.md). The eventing design below is
otherwise unchanged.

## Context

Pecunia is designed to be event-driven internally: when a transaction is
created, downstream actions (budget threshold check, audit log, future AI
categorization) react to the event. This decouples the producer from the
consumers and enables asynchronous processing.

Three options were considered for the eventing mechanism:

1. **Direct method calls**: the producer invokes consumers directly. No
   eventing infrastructure.
2. **Spring `ApplicationEventPublisher`**: in-process eventing, part of
   Spring Core. Synchronous by default, configurable to be asynchronous.
3. **Kafka** (or Redpanda): distributed event log, durable, scalable, with
   strong delivery guarantees.

The choice involves trade-offs between simplicity, operational cost, and
future scalability.

## Decision

Pecunia implements eventing through a **domain port**
(`DomainEventPublisher`) with a **Spring `ApplicationEventPublisher` adapter**
as the only implementation for the MVP.

### Design

In the domain layer:

```java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
```

In the infrastructure layer:

```java
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher publisher;
    
    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
```

Listeners use `@TransactionalEventListener(phase = AFTER_COMMIT)` to ensure
they execute only after the publishing transaction commits.

### Future migration to Kafka

When (and if) the project requires distributed events, a `KafkaDomainEventPublisher`
adapter can replace the Spring one without modifying the domain. The migration
involves:

1. Adding the new adapter implementation.
2. Configuring Kafka/Redpanda connectivity.
3. Switching the active adapter via Spring profile.

This is documented as a future enhancement; it is not implemented in the MVP.

## Consequences

### Positive

- **Zero additional infrastructure** in the MVP: no Kafka, no broker, no
  ZooKeeper.
- **Architectural readiness**: the port/adapter design makes the future
  migration mechanical.
- **Demonstrates discipline**: the choice signals that the author considered
  Kafka and explicitly deferred it, with a clear migration path.
- **In-process performance**: events are dispatched in microseconds.
- **Transactional semantics**: `@TransactionalEventListener` provides
  fine-grained control over when listeners execute relative to transaction
  boundaries.

### Negative

- **No durability**: events are lost if the application crashes between
  publish and listener execution. This is acceptable for the MVP scope but
  not for systems requiring strict reliability.
- **No distribution**: cannot fan out to multiple service instances or
  external consumers.
- **No replay**: cannot replay past events to rebuild state or recover from
  consumer bugs.

For Pecunia's single-user, single-instance MVP, these limitations are
acceptable.

### Neutral

- **Sealed `DomainEvent` interface**: all events share a marker interface,
  making the event hierarchy explicit and pattern-matchable.

## Alternatives Considered

### Direct method calls

Rejected because:
- Tightly couples producers to consumers.
- Violates the open-closed principle: adding a consumer requires modifying
  the producer.
- Prevents future asynchronous processing without refactoring.

### Kafka (or Redpanda) from the start

Rejected because:
- Operational complexity not justified by the MVP scope.
- Resource overhead (Kafka requires significant RAM; Redpanda is lighter
  but still an additional component).
- Learning curve for partition management, consumer groups, schema
  registries.
- The port/adapter design preserves the option to adopt Kafka later with
  minimal refactoring.

A future ADR may supersede this one when migrating to Kafka.

## References

- Spring Framework documentation, "Application Events":
  https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
- Eric Evans, "Domain-Driven Design", chapter on domain events.
- Vaughn Vernon, "Implementing Domain-Driven Design", chapter on domain
  events and integration.

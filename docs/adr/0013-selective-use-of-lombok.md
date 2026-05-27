# ADR-0013: Selective Use of Lombok, Excluding the Domain Layer

## Status

Accepted

## Context

Lombok is a popular Java library that generates boilerplate code (getters,
setters, builders, constructors, loggers) at compile time via annotations.
It reduces visual noise in the codebase but introduces a non-trivial trade-off:

- The generated code is invisible in the source files.
- An IDE plugin is required for IntelliJ, VS Code, and others to recognize
  generated members.
- Some tools (notably JaCoCo for coverage) have historically had quirks
  with Lombok-generated code.

In Java 25, the language itself reduces the need for Lombok:

- **Records** eliminate the need for `@Value`, `@Data` on immutable types.
- **Sealed interfaces** provide algebraic data types without external help.
- **Pattern matching** simplifies deconstruction.

However, Lombok remains useful in contexts where records are not
applicable (notably JPA entities, which require mutable state and a no-arg
constructor) and where boilerplate would otherwise dominate
(e.g., `@RequiredArgsConstructor` for Spring service injection, `@Slf4j`
for logging).

The question is **where** Lombok belongs in the codebase.

## Decision

Pecunia uses Lombok **selectively**, with a strict exclusion of the
**domain layer**.

### Where Lombok IS used

- **Infrastructure layer**: JPA entities, Spring configuration classes,
  external API clients.
  - Common annotations: `@Getter`, `@Setter`, `@NoArgsConstructor`,
    `@AllArgsConstructor`, `@Builder`, `@Slf4j`.
- **Application layer**: use case services and supporting classes.
  - Common annotations: `@RequiredArgsConstructor`, `@Slf4j`.
- **Web layer**: rarely needed (OpenAPI Generator produces records).
  When custom DTOs are required, `@Builder` may be used.

### Where Lombok is NOT used

- **Domain layer**: pure Java 25.
  - Value objects and events are records.
  - Entities are explicit, immutable where possible, with hand-written
    accessors named in business terms (e.g., `balance()` not `getBalance()`).
  - No Lombok annotations of any kind.

### Rationale for excluding Lombok from the domain

- The domain expresses business intent and must remain readable without IDE
  assistance.
- Hand-written accessors allow business-meaningful naming
  (`account.recordTransaction(...)`, `account.balance()`).
- Java 25 native features cover the domain's expressivity needs entirely.
- Keeping the domain dependency-free (not even Lombok) reinforces hexagonal
  discipline: the most important layer of the application has zero external
  coupling.
- Demonstrates senior judgment: choosing the right tool for the right
  layer, not applying a uniform practice mindlessly.

## Consequences

### Positive

- **Concise infrastructure code**: JPA entities and Spring services lose
  significant boilerplate.
- **Pure domain**: the domain layer is pristine Java, idiomatic and
  framework-agnostic.
- **Clear discipline**: the rule is simple and easy to enforce in code
  review.
- **Strong CV signal**: explaining "I use Lombok where it adds value, not
  everywhere" demonstrates deliberate design.

### Negative

- **Two styles in the codebase**: developers must understand that the
  conventions differ between layers.
- **Lombok-specific tooling required**: IDE plugins must be installed.
- **Discipline required**: the temptation to add `@Getter` to a domain
  entity for convenience must be resisted.

### Neutral

- **Performance**: identical at runtime (Lombok generates bytecode equivalent
  to hand-written code).

## Alternatives Considered

### Lombok everywhere

Rejected because:
- Pollutes the domain layer with framework-style annotations.
- Loses the opportunity to demonstrate Java 25's expressiveness in the most
  visible part of the codebase.
- Sends a "less senior" signal in interviews.

### No Lombok at all

Considered. Would demonstrate the most pure Java approach. Rejected
because:
- Infrastructure code (JPA entities especially) becomes significantly more
  verbose.
- The cost of Lombok in non-domain layers is low; the benefit is high.

## References

- Project Lombok: https://projectlombok.org/
- Brian Goetz, "Data-Oriented Programming in Java" (records and sealed
  classes as the modern approach to data modeling).

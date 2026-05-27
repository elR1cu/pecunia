# ADR-0009: Java 25 Native Features over Third-party Functional Libraries

## Status

Accepted

## Context

Functional programming idioms (immutable data, pattern matching, monadic
error handling, persistent collections) improve code clarity and reduce
defects. Java has historically lagged behind Scala, Kotlin, and Clojure on
these features, leading to the creation of libraries like Vavr (formerly
Javaslang) that retrofit functional constructs onto Java.

With Java 25 as the project's minimum version, the language offers
substantially expanded functional capabilities:

- **Records**: concise immutable data classes.
- **Sealed interfaces**: closed type hierarchies for algebraic data types.
- **Pattern matching for switch**: exhaustive matching on sealed types.
- **Pattern matching for instanceof**: typed deconstruction.
- **Stream API**: functional collection operations.
- **Optional**: explicit absence handling.
- **Virtual threads**: structured concurrency for asynchronous code.

The question is whether to additionally adopt Vavr for `Try`, `Either`,
`Tuple`, persistent collections, and richer pattern matching.

## Decision

Pecunia uses **only Java 25 native features** for functional programming.
No third-party functional library (Vavr, Functional Java, etc.) is adopted.

### Patterns used

- **Records** for value objects, DTOs, and events.
- **Sealed interfaces with record cases** for typed results (replacing
  `Either<L, R>` from Vavr):

```java
  public sealed interface ImportResult {
      record Success(int imported, int duplicates) implements ImportResult {}
      record Failure(String reason) implements ImportResult {}
  }
```

- **Pattern matching for switch** for exhaustive case handling.
- **Optional<T>** for explicit absence.
- **Stream API** for collection transformations.
- **Records as tuples** when ad-hoc pairing is needed (named records, not
  anonymous tuples).

## Consequences

### Positive

- **No external dependency**: smaller dependency surface, fewer version
  conflicts, less to maintain.
- **Idiomatic Java**: code is readable to any Java developer without
  learning a third-party library.
- **Better interoperability**: standard Java types integrate seamlessly with
  Spring, Jackson, JPA, and other libraries (which sometimes need adapters
  for Vavr types).
- **Modern Java showcase**: the project demonstrates mastery of Java 25
  language features.
- **Strong CV signal**: senior Java engineers value depth in the standard
  library over reliance on third-party abstractions.

### Negative

- **No persistent immutable collections**: Java's `List.copyOf` returns
  defensive copies, not persistent structures. For Pecunia's scale, this is
  irrelevant.
- **No built-in `Try` monad**: exception handling remains imperative.
  Sealed result types fill the gap for expected failures; truly exceptional
  cases use exceptions normally.
- **Slightly more verbose for some patterns**: e.g., constructing a result
  type involves naming the record case explicitly.

### Neutral

- **Decision can be revisited**: if a specific use case strongly benefits
  from Vavr (e.g., complex parsing pipelines), Vavr can be adopted in a
  bounded scope. A new ADR would document this.

## Alternatives Considered

### Vavr

Rejected for the reasons above. Vavr is mature and well-designed, but its
value proposition shrinks as Java itself becomes more expressive. Java 25
covers ~90% of Vavr's commonly-used features natively.

### Mixed approach (Java native for most code, Vavr for specific modules)

Considered. Could be adopted in the future if a clear case emerges. For the
MVP, the simplicity of a uniform approach (no Vavr anywhere) is preferred.

## References

- Brian Goetz, "Data-Oriented Programming in Java" (2022):
  https://www.infoq.com/articles/data-oriented-programming-java/
- Vavr Roadmap discussion:
  https://github.com/orgs/vavr-io/discussions/2953
- Java 25 release notes: https://openjdk.org/projects/jdk/25/

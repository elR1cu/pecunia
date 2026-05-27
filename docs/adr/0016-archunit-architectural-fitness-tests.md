# ADR-0016: Architectural Fitness Tests with ArchUnit

## Status

Accepted

## Context

Pecunia adopts hexagonal architecture (ADR-0003) and a modular monolith
(ADR-0004) with strict module boundaries. These architectural decisions
require ongoing enforcement: without verification, boundaries erode under
the pressure of feature delivery and forgetfulness.

Several mechanisms can enforce architectural rules:

1. **Code review only**: discipline relies entirely on human attention.
2. **Static analysis tools** generic to Java (e.g., SonarQube, jQAssistant).
3. **ArchUnit**: a JUnit-based library for writing architectural rules as
   tests.
4. **Spring Modulith**: official Spring toolkit with built-in boundary
   verification (deferred adoption, see roadmap.md).

For a project of Pecunia's scope and ambition, code review alone is not
sufficient. The author works alone; "reviewing oneself" misses
violations. Automated enforcement is required.

## Decision

Pecunia uses **ArchUnit** for architectural fitness tests, introduced at
**Block 2** (the first block where a real bounded context with all four
layers exists).

### Initial rule set (Block 2)

The minimum viable rule set protects the most important invariants:

- Domain layer has no dependency on Spring, JPA, Lombok, or any other
  framework.
- Domain layer does not depend on application, web, or infrastructure
  layers.
- Bounded contexts do not depend on each other directly (only via
  events or shared kernel).
- Web layer does not access infrastructure layer directly.
- Application layer does not depend on web layer.

### Evolution

The rule set grows as the project grows. New rules are added when:

- A new pattern emerges that should be enforced uniformly.
- A boundary violation is caught in review and could be prevented by a
  test.
- A naming or structural convention is established.

### Test location

Architectural tests live in `apps/api/src/test/java/com/pecunia/architecture/`
and run on every CI build, in the same suite as unit and integration
tests.

### Interaction with Spring Modulith (deferred)

If Spring Modulith is adopted later (see roadmap.md "Technology
Adoption Triggers"), ArchUnit and Modulith will coexist:

- Spring Modulith handles module-level boundaries with its own
  verification mechanism.
- ArchUnit continues to enforce the broader rules (no Spring in domain,
  no Lombok in domain, naming conventions, etc.) that Modulith does not
  cover.

## Consequences

### Positive

- **Architecture enforced, not just documented**: violations are caught
  at compile/test time, not after they accumulate.
- **Safety net during learning**: the author is learning hexagonal
  architecture; ArchUnit catches accidental violations.
- **Strong CV signal**: an architecturally-enforced codebase
  demonstrates senior-level engineering practice.
- **Faster review**: structural issues are caught automatically,
  freeing review attention for logic and design.
- **Living documentation**: the architectural tests are themselves a
  description of the rules.

### Negative

- **Initial setup cost**: 2-3 hours at Block 2 to introduce and
  configure.
- **Maintenance overhead**: as the codebase grows, rules need
  refinement and occasionally relaxation for legitimate exceptions.
- **Potential friction**: an overly strict rule can block legitimate
  code; the author must judge when to relax versus enforce.

### Neutral

- **No runtime impact**: ArchUnit is a `test` scope dependency.

## Alternatives Considered

### Code review only

Rejected because the author works alone. Self-review consistently
misses violations that automated tests catch instantly.

### Spring Modulith from the start

Rejected because Modulith adds a learning curve and conceptual overhead
that is not justified during the early blocks (see roadmap.md
"Technology Adoption Triggers"). ArchUnit is lighter weight and
sufficient for current needs.

### SonarQube or similar generic static analysis

Rejected as the primary tool because:
- Generic tools are not architecture-aware. They detect smells but do
  not understand hexagonal boundaries.
- ArchUnit's expressiveness in Java (DSL fluent API) makes
  architectural rules first-class citizens, readable by any developer.

SonarQube or similar may be adopted later for additional code quality
checks; it would complement ArchUnit, not replace it.

## References

- ArchUnit official site: https://www.archunit.org/
- "Fitness Functions for Your Architecture" by Neal Ford:
  https://nealford.com/memeagora/2013/04/02/architectural-fitness-functions.html
- Sam Newman, "Building Microservices" — discusses architectural
  fitness functions extensively.

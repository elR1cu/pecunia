# ADR-0017: Code Quality Enforcement with SonarCloud

## Status

Accepted

## Context

Pecunia is a public portfolio project targeting Senior Backend Java
positions in Swiss private banking and financial services. Code quality
is a primary differentiator. Beyond architectural fitness tests
(ArchUnit, ADR-0016), the codebase must be checked for:

- Code smells (long methods, complex conditions, duplications)
- Bugs (null dereferences, resource leaks, logic errors)
- Security hotspots (injection vectors, weak cryptography)
- Maintainability (cognitive complexity, technical debt)
- Test coverage

Several tools provide static code analysis:

1. **SonarQube self-hosted** (Community Edition): free, full control,
   requires infrastructure to operate.
2. **SonarCloud**: managed by SonarSource, free for public repositories,
   integrated with GitHub.
3. **Generic linters** (SpotBugs, PMD, Checkstyle): focused tools, less
   comprehensive than Sonar's offering.

## Decision

Pecunia uses **SonarCloud** for continuous code quality analysis,
introduced at **Block 1** with GitHub Actions CI.

### Configuration

- SonarCloud project created against the public GitHub repository.
- Quality Gate enforced on Pull Requests: a PR cannot be merged if it
  fails the gate.
- Quality Gate metrics (on new code):
  - Coverage: ≥ 80%
  - Duplicated lines: < 3%
  - Maintainability rating: A
  - Reliability rating: A
  - Security rating: A
- Existing code is analyzed but not retroactively held to the same
  standards (clean-as-you-go strategy).
- Sonar badge displayed in the README.

### Coverage measurement

JaCoCo generates coverage reports during the Maven `verify` phase.
SonarCloud ingests these reports for backend coverage. Frontend coverage
will be provided by an LCOV report from Vitest (the Angular CLI 22 default
test runner, which replaced Karma/Istanbul — see Session 10) when the
frontend is wired in a later session.

### Implementation (introduced Block 1)

- **CI-based analysis**, not SonarCloud's Automatic Analysis: only the
  CI-based path can ingest an external coverage report (JaCoCo). Automatic
  Analysis is disabled on the SonarCloud project to avoid a conflicting
  double analysis.
- **Backend scanner**: `sonar-maven-plugin` 5.7.0.6970, declared (version
  pinned) in `apps/api/pom.xml` so `mvn sonar:sonar` resolves the short
  prefix in CI and locally. Java 25 is fully supported by the analyzer.
- **Coverage tool**: `jacoco-maven-plugin` 0.8.14 (first release with
  official Java 25 support). Surefire's `argLine` uses `@{argLine}` so the
  JaCoCo agent coexists with the existing Mockito agent.
- **Analysis identifiers** live in the pom: `sonar.organization`,
  `sonar.projectKey`, `sonar.host.url`. The token is supplied via the
  `SONAR_TOKEN` env var (GitHub Actions secret), never committed.
- **Checkout** uses `fetch-depth: 0` so SonarCloud can compute blame and
  the "new code" period.
- **Exclusions**:
  - `sonar.exclusions=**/generated-sources/**` — generated code is excluded
    from all analysis (OpenAPI client and MapStruct `*Impl`).
  - `sonar.coverage.exclusions=**/PecuniaApplication.java` — the bootstrap
    class is excluded from the coverage metric only.

## Consequences

### Positive

- **Continuous quality enforcement**: every PR is automatically
  evaluated.
- **Public credibility**: SonarCloud badges in the README provide
  external validation of code quality.
- **Zero infrastructure**: no Sonar server to maintain.
- **Industry standard**: SonarCloud is widely used in financial
  services. Familiarity is a recognized asset.
- **Quality Gates discipline**: forces small, focused PRs that pass
  quality bars rather than large unreviewable ones.

### Negative

- **Tooling friction**: a failing Quality Gate can block a PR for
  reasons that feel pedantic. Discipline is required to fix issues
  rather than ignore them.
- **External dependency**: SonarCloud availability is outside the
  author's control.
- **Public exposure**: any quality drop is publicly visible.

### Neutral

- **Migration path**: SonarCloud and SonarQube self-hosted use the same
  underlying engine. Migration is possible but unlikely to be needed.

## Alternatives Considered

### SonarQube self-hosted

Rejected for MVP because:
- Adds operational complexity (server to deploy and maintain).
- No benefit for a public repository where SonarCloud is free.
- May be revisited if specific advanced features are needed later.

### Multiple specialized linters (SpotBugs, PMD, Checkstyle)

Considered. Rejected because:
- Less comprehensive than Sonar's integrated offering.
- Requires aggregating reports from multiple tools.
- Less recognized as a quality signal in the targeted industry.

These tools may be used as complements (e.g., SpotBugs detects some
patterns Sonar misses) but not as primary tools.

## References

- SonarCloud documentation: https://docs.sonarcloud.io/
- Quality Gates: https://docs.sonarsource.com/sonarqube/latest/user-guide/quality-gates/

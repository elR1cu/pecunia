# ADR-0019: RestClient for HTTP Calls

## Status

Accepted

## Context

Pecunia will make HTTP calls to external services starting from Block 6
(Anthropic Claude API for AI categorization). The choice of HTTP client
affects code clarity, testability, and the signal sent to reviewers
exploring the codebase.

Four options were considered:

1. **`RestTemplate`**: the historical Spring HTTP client. Synchronous,
   widely known, but in maintenance-only mode since Spring 5. No new
   features will be added; Spring recommends migrating to RestClient
   or WebClient.

2. **`WebClient`**: Spring's reactive HTTP client, based on Project
   Reactor. Powerful and async-native, but introduces reactive
   programming concepts (Mono, Flux) that are out of place in an
   otherwise imperative codebase.

3. **`java.net.HttpClient`** (JDK 11+): the standard library's HTTP
   client. Modern, no Spring dependency, but lacks integration with
   Spring's request interception, retry mechanisms, and observability.

4. **`RestClient`**: introduced in Spring 6.1 (2023). Synchronous with
   a fluent API, designed as the modern successor to RestTemplate
   without the reactive overhead of WebClient. Officially recommended
   by Spring for new synchronous use cases.

## Decision

Pecunia uses **`RestClient`** for all HTTP calls to external services.

### Configuration

A single `RestClient.Builder` bean is configured per external service,
with appropriate defaults:

- Base URL
- Authentication headers (via interceptors, never hard-coded)
- Timeouts (connect, read)
- Observability hooks (Micrometer, OpenTelemetry once introduced)
- Resilience integration (Resilience4j when introduced at Block 6)

### Service-specific clients

Each external service gets its own typed client class wrapping
`RestClient`, exposing domain-level methods:

```java
public final class AnthropicCategorizationClient {
    private final RestClient restClient;

    public CategorizationSuggestion suggestCategory(TransactionDescription description) {
        // ... uses restClient under the hood
    }
}
```

This keeps the domain free of HTTP details and makes the client easily
mockable in tests.

## Consequences

### Positive

- **Modern Spring practice**: RestClient is the current Spring
  recommendation for synchronous HTTP. Demonstrates awareness of
  current best practices.
- **No reactive overhead**: synchronous calls remain simple to read,
  debug, and test. No need to learn Reactor for a use case that does
  not benefit from it.
- **Fluent API**: the request-building syntax is more readable than
  RestTemplate's, making intent clearer.
- **Future-proof**: RestClient is actively developed; new features
  land here rather than in RestTemplate.
- **Good interview signal**: choosing RestClient over RestTemplate or
  WebClient shows current awareness and pragmatic judgment.

### Negative

- **Less mature documentation than RestTemplate**: there are fewer
  third-party tutorials and Stack Overflow answers (still ample, but
  fewer than RestTemplate).
- **Newer team members may default to RestTemplate**: requires
  awareness during reviews.

### Neutral

- **Migration to WebClient possible**: if a future use case truly
  requires reactive HTTP (e.g., high-concurrency external streaming),
  WebClient remains available. The application code is already isolated
  behind per-service client classes, limiting the migration scope.

## Alternatives Considered

### RestTemplate

Rejected because:
- In maintenance-only mode; not the right choice for new code in 2026.
- No future feature development.
- Sends a "legacy" signal in code review.

### WebClient

Rejected because:
- Requires Project Reactor knowledge for what is fundamentally
  synchronous use cases.
- Adds complexity (Mono/Flux types, schedulers, backpressure) without
  benefit.
- The single use case in the MVP roadmap (Anthropic API) is a simple
  request-response pattern that does not need reactive semantics.

### java.net.HttpClient

Considered. Modern and dependency-free, but:
- Lacks Spring integration (no central interception, no
  observability hooks, no Bean Validation of request DTOs).
- Bypasses the Spring HTTP abstraction, breaking consistency with the
  rest of the application.
- Useful for non-Spring projects; not the best fit here.

## References

- Spring Framework documentation, RestClient:
  https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient
- "RestClient: A Modern HTTP Client for Spring" (Spring Blog).
- Spring Framework migration guide: RestTemplate to RestClient.

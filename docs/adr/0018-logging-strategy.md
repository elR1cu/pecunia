# ADR-0018: Logging Strategy with Logback and Structured JSON

## Status

Accepted (2026-05-24). Revised 2026-06-10 to use Spring Boot 4's native
structured logging support (instead of the `logstash-logback-encoder`
third-party library) and Micrometer Tracing (instead of a custom
`OncePerRequestFilter`) for correlation IDs.

## Context

Pecunia handles personal financial data and must meet high logging
standards: complete enough to debug production issues, structured
enough to be searchable, sanitized enough to avoid leaking sensitive
data.

Spring Boot uses Logback by default. The alternative, Log4j2, is often
cited as more performant under heavy load due to its lock-free
asynchronous loggers.

For Pecunia's scale (single user, low request volume), the performance
difference between Logback and Log4j2 is imperceptible. The dimensions
that actually matter for the project are:

- **Structured output** (JSON) for searchability in production
- **Correlation IDs** (MDC) for tracing a request end-to-end
- **Sanitization** of sensitive financial data (IBANs, amounts,
  identifiers)
- **Appropriate log levels** for production hygiene
- **Future OpenTelemetry integration** for distributed tracing

These dimensions are framework-agnostic: they can be implemented with
either Logback or Log4j2 with comparable effort.

Two facts about the modern Spring Boot ecosystem reshape the
sub-decisions inside this strategy:

- Spring Boot 3.4 (August 2024) introduced **native structured
  logging**, exposing the `logstash`, `ecs`, and `gelf` JSON formats
  as built-in configuration options. The third-party
  `logstash-logback-encoder` library is no longer necessary to obtain
  the same JSON wire format that ELK, Loki, and Grafana Cloud consume.
  Spring Boot 4 carries this feature forward.
- Since Spring Boot 3.0, **Micrometer Tracing** populates the MDC with
  `traceId` and `spanId` automatically when a tracing bridge
  (OpenTelemetry or Brave) is on the classpath, propagates the W3C
  `traceparent` header to downstream calls, and handles async / reactive
  context propagation correctly. A custom servlet filter is no longer
  required to obtain correlation IDs in logs.

## Decision

Pecunia uses **Logback** (the Spring Boot default), with structured
JSON output configured through **Spring Boot 4's native structured
logging support**, and correlation IDs provided by **Micrometer
Tracing**:

### Structured JSON output

In production, logs are emitted as JSON in the `logstash` format via
the property:

```yaml
logging:
  structured:
    format:
      console: logstash
```

This produces the same JSON wire format that
`logstash-logback-encoder` produces (`@timestamp`, `@version`,
`message`, `logger_name`, `thread_name`, `level`, `level_value`, plus
MDC entries promoted as top-level fields), without any third-party
dependency.

In development, JSON is disabled and the human-readable Spring Boot
default pattern is used (color-aware, single-line).

Global custom fields (e.g. `service`, `env`) are declared via
properties:

```yaml
logging:
  structured:
    json:
      add:
        service: pecunia-api
```

### Correlation IDs via Micrometer Tracing

Micrometer Tracing populates the MDC with `traceId` and `spanId`
automatically at the start of every HTTP request, propagates the W3C
`traceparent` context to downstream calls, and integrates cleanly with
Spring Boot 4's `logstash` format (MDC entries are promoted to
top-level JSON fields, so `traceId` and `spanId` become queryable
fields without further configuration).

The Spring Boot 4 starter wires the Micrometer Tracing bridge, the
OpenTelemetry SDK, and the matching autoconfig (split into a dedicated
module per Spring Boot 4's autoconfig layout) in one step:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
</dependency>
```

The starter also bundles the OTLP exporters for traces, metrics, and
logs. At Block 1 all three exporters are disabled by configuration:

```yaml
management:
  opentelemetry:
    tracing:
      export:
        otlp:
          enabled: false
    logging:
      export:
        otlp:
          enabled: false
  otlp:
    metrics:
      export:
        enabled: false
```

The Spring Boot 4 property namespace is asymmetric on purpose:
`management.opentelemetry.*.export.otlp.*` for traces and logs,
`management.otlp.metrics.export.*` for metrics. Spans are generated,
their IDs land in the MDC, then are dropped at the end of the request
— nothing is shipped over the wire. Block 9 will flip the tracing
flag and configure an endpoint to ship spans to a tracing backend.

A known Spring Boot bug
([#49304](https://github.com/spring-projects/spring-boot/issues/49304))
may keep some exporters publishing despite these flags; the fallback
is the `OTEL_SDK_DISABLED=true` environment variable. Harmless at
Block 1 since no collector is reachable; revisit if collector
configuration arrives before #49304 is fixed.

If a separate per-request identifier echoed to the client (e.g. via an
`X-Request-Id` response header) is later needed for support workflows,
it can be layered on top of Micrometer Tracing via a small filter
that copies `traceId` from the MDC into a response header.

### Sanitization

Sensitive patterns are masked before reaching the appender. Two
extension points cover the two output modes:

- For JSON output: a `StructuredLoggingJsonMembersCustomizer` (Spring
  Boot 4 SPI) rewrites the `message` member and any custom fields
  prone to leak.
- For the dev pattern: a custom Logback `ClassicConverter` registered
  in `logback-spring.xml`, applied only if developer-side logs are
  judged sensitive enough to warrant masking locally.

Patterns masked:
- IBANs are partially redacted (`CH** **** **** **** ****1`)
- Amounts above a threshold are redacted in INFO and above
- Bearer tokens are never logged
- Email addresses are partially redacted in INFO and above

### Log levels

- `DEBUG`: detailed troubleshooting information, off in production by
  default
- `INFO`: lifecycle events, successful operations, business milestones
- `WARN`: recoverable issues, deprecations, suspicious patterns
- `ERROR`: failures requiring attention

### Future evolution

- **Block 9**: add the OpenTelemetry OTLP exporter to ship spans to a
  tracing backend (Tempo, Jaeger, Grafana Cloud). The Micrometer
  Tracing bridge is already in place from Block 1, so this is an
  exporter dependency and configuration only — not a new architecture.

## Consequences

### Positive

- **Zero external logging dependency**: no `logstash-logback-encoder`,
  no transitive Jackson 2 to manage alongside Spring Boot 4's
  Jackson 3.
- **No custom request-id filter** to write or maintain: Micrometer
  Tracing handles MDC population, async / reactive context
  propagation, and W3C `traceparent` header propagation out of the
  box.
- **Configuration in YAML**: the structured-logging knobs live next to
  the rest of the Spring Boot config, not in a separate XML file.
- **Maintained by the Spring Boot team**: structured logging and
  tracing evolve with the framework's release cycle rather than a
  third-party cadence.
- **Same JSON wire format** (`logstash`): consumers (Loki, ELK,
  Grafana Cloud, Datadog) are unaffected.
- **Security-first**: sanitization plugs into an explicit Spring Boot
  SPI rather than monkey-patching a third-party encoder.
- **OpenTelemetry-ready by construction**: choosing the OTel bridge
  for tracing in Block 1 makes Block 9 a one-dependency addition (the
  exporter) rather than an architectural pivot.
- **Format pivot is easy**: switching to `ecs` or `gelf` later is a
  one-line change.

### Negative

- **Younger than `logstash-logback-encoder`**: Spring Boot's native
  support landed in 3.4 (August 2024) versus a decade of production
  use for the third-party library. The risk at Pecunia's scale (single
  user, low traffic) is judged minimal; falling back to
  `logstash-logback-encoder` 9.0+ if a blocking bug appears is a
  localized change.
- **JSON logs are less readable in development**: leaving
  `logging.structured.format.console` unset in the dev profile keeps
  human-readable output locally.
- **Anticipates the OTel commitment**: the Micrometer Tracing bridge
  pulls a transitive OpenTelemetry runtime in Block 1, earlier than
  the roadmap originally placed it (Block 9). The runtime is small
  and dormant (no exporter, no remote calls) until Block 9 activates
  it.
- **Hex IDs, not UUIDs**: `traceId` is a 32-character hex string from
  the W3C Trace Context standard, not a project-style UUID v7. This
  is correct (interop with tracing platforms requires it) but
  cosmetically inconsistent with database IDs.

### Neutral

- **No vendor lock-in**: SLF4J remains the API, Micrometer Tracing is
  a facade over Brave and OpenTelemetry, and the JSON formats are
  open standards. The underlying implementations can still be swapped
  if a strong reason emerges.

## Alternatives Considered

### `logstash-logback-encoder` (third-party library)

Until Spring Boot 3.4, this was the de facto choice for structured
JSON logging in Spring Boot applications. The original 2026-05 version
of this ADR selected it.

Rejected on revision because:
- Spring Boot 4's native support produces the same JSON wire format,
  removing the main reason to take the dependency.
- The library adds a transitive Jackson 2 dependency alongside Spring
  Boot 4's Jackson 3 (no package conflict, but unnecessary bloat).
- Version 9.0 (which targets Jackson 3) follows a slower release
  cadence than Spring Boot's structured logging, which evolves with
  the framework.
- Two configuration formats (YAML for application config, XML for
  Logback) are required when one is now sufficient.

It remains a credible fallback if Spring Boot's native support hits a
blocking bug.

### ECS-formatted output via `ecs-logging-java`

Elastic's library produces logs in the Elastic Common Schema (ECS)
naming convention (`service.name`, `log.level`, `trace.id`, etc.).

Rejected because:
- Pecunia is not committed to an Elastic-only stack; the more neutral
  `logstash` format keeps the door open to Loki/Grafana, ELK, and
  Datadog equally.
- Spring Boot 4's native support exposes `ecs` as a one-line opt-in
  anyway, should the operational target change later.

### Log4j2

Rejected because:
- Performance benefits are negligible at Pecunia's scale.
- Adds configuration complexity (different XML/YAML format).
- Spring Boot integration requires excluding the default starter.
- The dimensions that actually matter (structured output, MDC,
  sanitization) are achievable with Logback equally well.
- Difficult to justify in an interview for a single-user application
  without a clear performance need.

### Default Spring Boot logging (no customization)

Rejected because:
- Plain text logs are not searchable in production aggregators.
- No correlation IDs by default.
- No sanitization of sensitive data.

### Custom `RequestIdFilter` (`OncePerRequestFilter` + MDC)

Before Micrometer Tracing became the idiomatic Spring Boot path, the
classic pattern for correlation IDs was a servlet filter that
generates an identifier per request, places it in the MDC, and echoes
it in a response header. For reference and as a learning artifact,
the equivalent shape is:

```java
package com.pecunia.shared.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "trace_id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER);
        return (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming;
    }
}
```

Registered with an order that runs **before** the Spring Security
filter chain so that 401/302 responses also carry `trace_id` in their
log lines (Spring Security registers at `-100` by default;
`HIGHEST_PRECEDENCE` runs first):

```java
package com.pecunia.shared.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
class RequestIdFilterConfig {

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration() {
        var registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
```

Notes for a real implementation of this alternative:
- Replace `UUID.randomUUID()` with `UuidCreator.getTimeOrderedEpoch()`
  to align with ADR-0015 (UUID v7 across the project) — at the cost
  of pulling the `com.github.f4b6a3:uuid-creator` dependency.
- The MDC key is `trace_id` (snake_case) to match the `logstash`
  format convention (`logger_name`, `thread_name`, `level_value`).
- The filter does **not** propagate context across reactive or async
  boundaries; doing so correctly is non-trivial and is one of the
  reasons Micrometer Tracing was preferred.

Rejected because:
- Micrometer Tracing covers the same need with zero application code,
  handles async / reactive context propagation correctly, propagates
  the W3C `traceparent` header to downstream HTTP calls, and provides
  a clean path to span export when distributed tracing arrives.
- A custom filter adds a maintenance surface (cleanup edge cases,
  ThreadLocal leaks, future async support) for a problem the framework
  already solves.

## References

- Logback documentation: https://logback.qos.ch/
- Spring Boot Reference — Logging:
  https://docs.spring.io/spring-boot/reference/features/logging.html
- Spring Boot Reference — Tracing:
  https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- "Structured logging in Spring Boot 3.4" (Spring blog, 2024-08-23):
  https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4/
- Micrometer Tracing documentation:
  https://docs.micrometer.io/tracing/reference/
- `logstash-logback-encoder`:
  https://github.com/logfellow/logstash-logback-encoder
- "Logging Best Practices" by OWASP:
  https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html

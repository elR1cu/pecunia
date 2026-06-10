# ADR-0018: Logging Strategy with Logback and Structured JSON

## Status

Accepted (2026-05-24). Revised 2026-06-10 to use Spring Boot 4's native
structured logging support instead of the `logstash-logback-encoder`
third-party library.

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

A second fact became relevant during Block 1 implementation: Spring
Boot 3.4 (August 2024) introduced **native structured logging**,
exposing the `logstash`, `ecs`, and `gelf` JSON formats as built-in
configuration options. This makes the third-party
`logstash-logback-encoder` library no longer necessary to obtain the
same JSON wire format that ELK, Loki, and Grafana Cloud consume. Spring
Boot 4 carries this feature forward.

## Decision

Pecunia uses **Logback** (the Spring Boot default), with structured
JSON output configured through **Spring Boot 4's native structured
logging support**:

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

### Correlation IDs via MDC

A `RequestIdFilter` populates the MDC with a `traceId` at the start of
every HTTP request. The `traceId` is included in all log statements
during the request and is also returned in the response header for
client correlation. Spring Boot's native `logstash` format promotes
MDC entries to top-level JSON fields automatically, so `traceId` is
queryable as `traceId:abc123` in log platforms without further
configuration.

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

- **Block 9**: OpenTelemetry integration for distributed tracing.
  Logback supports this natively via the `logback-mdc-otel` integration.

## Consequences

### Positive

- **Zero external dependency**: no `logstash-logback-encoder`, no
  transitive Jackson 2 to manage alongside Spring Boot 4's Jackson 3.
- **Configuration in YAML**: the structured-logging knobs live next to
  the rest of the Spring Boot config, not in a separate XML file.
- **Maintained by the Spring Boot team**: structured logging evolves
  with the framework's release cycle rather than a third-party cadence.
- **Same JSON wire format** (`logstash`): consumers (Loki, ELK,
  Grafana Cloud, Datadog) are unaffected.
- **Security-first**: sanitization plugs into an explicit Spring Boot
  SPI rather than monkey-patching a third-party encoder.
- **OpenTelemetry-ready**: the choice does not preclude advanced
  observability later.
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

### Neutral

- **No vendor lock-in**: SLF4J remains the API, and the JSON formats
  are open standards. The underlying implementation can still be
  swapped if a strong reason emerges.

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

## References

- Logback documentation: https://logback.qos.ch/
- Spring Boot Reference — Logging:
  https://docs.spring.io/spring-boot/reference/features/logging.html
- "Structured logging in Spring Boot 3.4" (Spring blog, 2024-08-23):
  https://spring.io/blog/2024/08/23/structured-logging-in-spring-boot-3-4/
- `logstash-logback-encoder`:
  https://github.com/logfellow/logstash-logback-encoder
- "Logging Best Practices" by OWASP:
  https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html

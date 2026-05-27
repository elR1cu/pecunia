# ADR-0018: Logging Strategy with Logback and Structured JSON

## Status

Accepted

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

## Decision

Pecunia uses **Logback** (the Spring Boot default) with the following
configuration:

### Structured JSON output

In production, logs are emitted as JSON using the `logstash-logback-
encoder` library. This makes logs ingestible by log aggregation
platforms (ELK, Loki, Grafana Cloud) without parsing.

### Correlation IDs via MDC

A `RequestIdFilter` populates the MDC with a `traceId` at the start of
every HTTP request. The `traceId` is included in all log statements
during the request and is also returned in the response header for
client correlation.

### Sanitization

A custom Logback converter masks sensitive patterns:
- IBANs are partially redacted (`CH** **** **** **** ****1`)
- Amounts above a threshold are redacted in logs (only logged in DEBUG)
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

- **Zero configuration to start**: Logback is the Spring Boot default,
  no additional setup needed.
- **Structured logs by design**: JSON output enables log aggregation
  and querying from day one.
- **Security-first**: sanitization patterns prevent accidental data
  leaks.
- **Debugging-friendly**: correlation IDs make request tracing
  straightforward.
- **OpenTelemetry-ready**: the choice does not preclude advanced
  observability later.

### Negative

- **Slightly less performant than Log4j2** under extreme load. This is
  not relevant at Pecunia's scale.
- **JSON logs are less readable in development**: a separate Logback
  profile keeps human-readable output in local dev.

### Neutral

- **No vendor lock-in**: SLF4J is the API; the underlying
  implementation can be swapped if a strong reason emerges.

## Alternatives Considered

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
- `logstash-logback-encoder`: https://github.com/logfellow/logstash-logback-encoder
- "Logging Best Practices" by OWASP:
  https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html

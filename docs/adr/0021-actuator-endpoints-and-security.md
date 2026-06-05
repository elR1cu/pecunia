# ADR-0021: Actuator Endpoints and Security

## Status

Accepted

## Context

Spring Boot Actuator ships approximately 20 endpoints (`health`, `info`,
`metrics`, `env`, `beans`, `configprops`, `mappings`, `threaddump`,
`heapdump`, `loggers`, …). They provide deep operational visibility but
they can also leak operational details, secrets (`env`, `configprops`),
or allow destructive actions (`shutdown`, `loggers` runtime mutation).

For a financial application that:
- runs behind Kubernetes-style probes from Block 8 onwards,
- protects user financial data,
- aims to keep its attack surface minimal,

the actuator strategy must answer three questions:

1. Which endpoints are reachable over HTTP at all?
2. Which require authentication, and at what level?
3. How does the policy balance Kubernetes probes (anonymous access by
   design) against information leakage?

The MVP needs a minimal but functional baseline. Block 9 will introduce
the full observability stack (Prometheus, traces).

## Decision

The MVP applies a **two-layer policy**: a narrow HTTP exposure list at
the Actuator level, and a tiered access policy at the Spring Security
level.

### Layer 1 — HTTP exposure (`application.yml`)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
```

Only `health` (with its `liveness`/`readiness` sub-endpoints) and `info`
are reachable over HTTP. All other Actuator endpoints (`env`, `beans`,
`metrics`, `threaddump`, …) are inaccessible until explicitly added to
the allowlist.

### Layer 2 — Security policy (enforced by `SecurityFilterChain`)

| Endpoint | Access | Rationale |
|---|---|---|
| `/actuator/health/liveness` | `permitAll()` | Kubernetes liveness probe; never includes component details. |
| `/actuator/health/readiness` | `permitAll()` | Kubernetes readiness probe; same. |
| `/actuator/health` (root) | `permitAll()` | External monitoring (e.g., Uptime Kuma). Component details are masked by `show-details: when-authorized`. |
| `/actuator/info` | `permitAll()` | Build/version metadata, no secret. |
| Any other Actuator endpoint (future) | `authenticated()`, likely role-restricted | Metrics, env, configprops, etc. can leak. |

### `show-details: when-authorized` trade-off

- Anonymous probe → `{"status":"UP"}`. Enough for Kubernetes probes and
  external monitoring.
- Authenticated probe → component-level statuses (datasource, Redis, …).
  Useful for the operator (logged in via the BFF) when troubleshooting.

The two values `never` and `always` were considered; `when-authorized` is
the tightest setting that still gives a logged-in operator full
visibility without leaking the dependency list to anonymous callers.

## Consequences

### Positive

- **Minimal attack surface**: only two endpoints exposed at MVP, both
  designed for public consumption.
- **Kubernetes-ready**: `liveness` and `readiness` work without
  authentication, as required.
- **Tiered diagnostics**: an authenticated operator sees richer health
  output without anonymous leakage.
- **Build info ready**: thanks to the `build-info` Maven goal (enabled
  in session 01), `/actuator/info` will return version and Git commit
  metadata once the `BuildInfoContributor` is wired.
- **Forces deliberation**: any future endpoint addition requires
  explicit consideration in both `application.yml` (exposure) and the
  `SecurityFilterChain` (access). This is deliberate friction in line
  with the project's "security by default" stance.

### Negative

- **No metrics or Prometheus until Block 9**. Operational visibility
  during the MVP is limited to logs and the bare health endpoint. Trade-
  off accepted because the MVP runs a single user on a single VPS.
- **Ongoing maintenance cost**: every new endpoint added later requires
  two coordinated changes (allowlist + filter chain). Acceptable.

### Neutral

- **`show-details: when-authorized` relies on a Spring Security
  authentication context**. The BFF session pattern provides this for
  the application user. External monitoring tools that only need the
  status (not the details) work transparently against the anonymous
  output.

## Alternatives Considered

### Expose everything, authenticate everything

Allow `management.endpoints.web.exposure.include: "*"` and trust
`SecurityFilterChain` to gate access. Rejected because:
- **Defense in depth**: if Spring Security is misconfigured, an exposed
  `env` endpoint leaks secrets. Not exposing it at all is a stronger
  guarantee than relying on a single layer.
- The "allowlist by default" posture matches the rest of the project
  (CSRF, multi-tenancy ownership checks, etc.).

### Separate management port (`management.server.port: 8081`)

Putting Actuator on a non-public port simplifies the security policy:
the port is firewalled from the internet, no per-endpoint auth needed.
Rejected for the MVP because:
- The MVP is a single-VPS deployment; adding a firewalled port is
  meaningful operational complexity for marginal gain.
- Kubernetes probes still need anonymous access regardless of port.
- May be revisited in Block 10 (k3d), where multi-port services and
  NetworkPolicies make the pattern natural.

### Anonymous `/actuator/health` with full details (`show-details: always`)

Simpler to configure, useful for one-developer debugging. Rejected
because it leaks the existence of dependencies (Redis, datasource,
mail, …) to anonymous callers — useful information for an attacker
enumerating the stack. The cost of `when-authorized` is negligible since
the developer is already authenticated via the BFF.

## References

- [Spring Boot reference — Actuator endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Spring Boot reference — Kubernetes probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)
- [ADR-0006](0006-bff-authentication-pattern.md) — BFF authentication pattern (provides the auth context for `when-authorized`)
- [ADR-0014](0014-multi-tenant-architecture.md) — Multi-tenant architecture (same "allowlist by default" posture)
- [ADR-0018](0018-logging-strategy.md) — Logging strategy (companion observability decision)

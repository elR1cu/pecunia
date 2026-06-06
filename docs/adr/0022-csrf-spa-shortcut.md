# ADR-0022: CSRF Configuration via `csrf.spa()` Shortcut

## Status

Accepted

## Context

Pecunia uses a BFF + SPA architecture (see [ADR-0006](0006-bff-authentication-pattern.md)):
the backend manages OAuth2 tokens and exposes session-based authentication
to the Angular frontend through a `SESSION` cookie. Because the
authentication is session-based and the browser sends the session cookie
automatically on every request, CSRF protection is mandatory.

The CSRF setup for an SPA + BFF requires three coordinated pieces of
configuration, each addressing a specific concern:

### 1. A non-HttpOnly cookie repository

The SPA's JavaScript must be able to read the CSRF token from a cookie and
send it back in a request header (`X-XSRF-TOKEN`). This requires
`CookieCsrfTokenRepository.withHttpOnlyFalse()` rather than the default
session-stored repository.

### 2. The "plain" `CsrfTokenRequestAttributeHandler` (not Xor)

Spring Security 6+ ships two handlers:

- `XorCsrfTokenRequestAttributeHandler` (default) — xors the canonical
  token with per-request randomness to defend against BREACH-style
  compression-side-channel attacks on response bodies.
- `CsrfTokenRequestAttributeHandler` ("plain") — emits the canonical
  token without xor transformation.

For a BFF + SPA, the token is never rendered in the HTML response body
(it lives in a cookie, not in a `<meta>` tag or hidden form field), so
the BREACH attack surface does not apply. The Xor handler adds friction
with Angular's native `HttpXsrfTokenExtractor`, which expects the cookie
value and the header value to match byte-for-byte. The plain handler
integrates with Angular natively.

### 3. A filter that forces token materialization on every request

This is the non-obvious piece. Spring Security 6+ uses a
`DeferredCsrfToken`: the token is generated and the cookie is written to
the response **only when `CsrfToken#getToken()` is actually invoked**
during the request. On state-changing requests (POST/PUT/DELETE/PATCH)
the `CsrfFilter` forces materialization to validate the incoming token.
On GET requests, nothing materializes the token by default — so the
cookie is never written.

Without an explicit materialization filter, the SPA never receives the
`XSRF-TOKEN` cookie, has nothing to read, and cannot send the header back.
Subsequent POSTs are rejected with HTTP 403. The Spring Security
reference documentation acknowledges this and prescribes a custom
`OncePerRequestFilter` that calls `getToken()` on the request attribute
to force the deferred token to render.

### The integration question

The MVP needs all three pieces wired correctly. They can be assembled
manually (verbose, three independent classes/configurations to maintain
and understand), or via a single declarative call introduced in Spring
Security 6.5 and available in 7.x: `CsrfConfigurer.spa()`.

## Decision

Use the `csrf(CsrfConfigurer::spa)` shortcut in the `SecurityFilterChain`
bean. This single call applies all three configurations as one cohesive
unit, semantically tagged as "SPA-friendly CSRF setup".

```java
http.csrf(CsrfConfigurer::spa)
```

### What `spa()` bundles internally

| Concern | What `spa()` sets up |
|---|---|
| Token storage | `CookieCsrfTokenRepository.withHttpOnlyFalse()` — token in a non-HttpOnly `XSRF-TOKEN` cookie, readable by JavaScript. |
| Token rendering | `CsrfTokenRequestAttributeHandler` (plain, not Xor) — canonical token in the cookie matches what Spring expects in the `X-XSRF-TOKEN` header. |
| Token materialization | An internal filter equivalent to a custom `CsrfCookieFilter` that calls `CsrfToken#getToken()` on every request, forcing the deferred token to render and the cookie to be written. |

The result: every request from the SPA receives an `XSRF-TOKEN` cookie,
Angular's `HttpXsrfTokenExtractor` reads it and adds the
`X-XSRF-TOKEN` header on state-changing requests, and Spring validates
the incoming header against the cookie. No glue code required.

## Consequences

### Positive

- **Single, declarative line** replacing three pieces of plumbing. Less
  code to maintain, less surface for configuration drift.
- **Semantic clarity**: `csrf.spa()` reads as "I'm configuring CSRF for
  an SPA", whereas the manual three-part setup hides intent behind
  implementation detail.
- **Forward-compatible**: if Spring evolves the recommended SPA pattern
  (e.g., adds defense layers, changes filter ordering), the bundle
  evolves with the framework rather than requiring manual catch-up.
- **Idiomatic Spring Security 7**: signals modern API familiarity in a
  codebase that will be read by recruiters.

### Negative

- **Hides implementation details**. A developer encountering a CSRF
  problem (cookie missing, header mismatch) must know what `spa()`
  expands to in order to debug. Mitigated by this ADR and by the brief
  inline comment in `SecurityConfig.java`.
- **Tied to Spring Security 6.5+ API**. Not portable to projects pinned
  to older Security versions. Acceptable: we are on 7.x via Spring Boot 4.

### Neutral

- **No behavior change** compared to the manual three-part setup. The
  cookie, the handler, and the materialization filter are functionally
  identical to what the shortcut produces.
- **Defaults remain explicit elsewhere in the filter chain** (session
  policy, OAuth2 login, logout handler). The CSRF block is the only one
  using a bundle shortcut; the rest of the chain stays at the granular
  level the project prefers for clarity.

## Alternatives Considered

### Manual three-part configuration

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
);
http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
```

Implemented first as a learning exercise and to make each concern
visible. Rejected as the final form because:

- Three pieces must remain coordinated (cookie repository ↔ handler ↔
  filter ordering). Any one drifting silently breaks CSRF without
  explicit error.
- The custom `CsrfCookieFilter` is boilerplate that adds maintenance
  surface for behavior the framework now ships natively.
- Less readable: intent must be reconstructed from three independent
  declarations.

The manual version was kept in version control history as a teaching
artifact (Session 04 recap) before being replaced.

### `XorCsrfTokenRequestAttributeHandler` (Spring Security default)

The default handler in Spring Security 6+. Adds defense against BREACH
(Browser Reconnaissance and Exfiltration via Adaptive Compression of
Hypertext, 2013) by xoring the token with per-request randomness, so
the token's representation in response bodies changes on each request
and cannot be progressively extracted via compression-side-channel
observation.

Rejected because BREACH requires the token to be rendered in a
**compressible response body** alongside attacker-controlled reflected
content. In a BFF + SPA setup, the token lives in a `Set-Cookie`
header (not gzipped), and no HTML response body contains it. The
attack surface is closed by architecture, not by handler choice.

The Xor handler also creates friction with Angular's
`HttpXsrfTokenExtractor`, which expects the cookie value and the
header value to match byte-for-byte — the Xor transformation requires
custom integration code to reconcile cookie and header values.

For server-rendered Thymeleaf/JSP applications, the Xor handler is the
correct choice. For Pecunia, the plain handler delivers identical
practical security with native Angular integration.

### Stateless CSRF (no token, rely on `SameSite=Strict`)

Skip CSRF protection entirely on the assumption that
`SameSite=Strict` on the `SESSION` cookie blocks cross-origin
state-changing requests. Rejected because:

- `SameSite` is a defense layer, not a replacement. Browsers without
  full `SameSite` support, or specific browser bugs, leave a gap.
- The OWASP recommendation for session-based authentication is to keep
  CSRF tokens as a primary defense regardless of `SameSite`.
- Disabling CSRF would require explicit ADR-level reasoning to
  reverse later. Defense in depth costs one declarative line; not
  worth removing it.

## References

- [Spring Security reference — Cross Site Request Forgery (CSRF)](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Spring Security reference — Integrating with CSRF Protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration)
- [Spring Security 6.5 release notes — `CsrfConfigurer.spa()`](https://docs.spring.io/spring-security/reference/whats-new.html)
- [BREACH attack paper (2013)](https://www.breachattack.com/)
- [Angular `HttpXsrfTokenExtractor` documentation](https://angular.dev/api/common/http/HttpXsrfTokenExtractor)
- [ADR-0006](0006-bff-authentication-pattern.md) — BFF authentication pattern (provides the session-based context that requires CSRF)
- [ADR-0011](0011-redis-for-session-storage.md) — Redis-backed sessions (the session that CSRF protects)
- [ADR-0021](0021-actuator-endpoints-and-security.md) — Actuator endpoints and security (companion security policy decision)

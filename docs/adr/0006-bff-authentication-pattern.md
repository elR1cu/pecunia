# ADR-0006: Backend-for-Frontend Authentication Pattern

## Status

Accepted

## Context

Pecunia is a Single Page Application (Angular) consuming a Spring Boot REST
API, with authentication handled by Keycloak (OIDC provider). Two patterns
exist for SPA + OIDC authentication:

1. **SPA-as-public-client**: the Angular app handles the OIDC Authorization
   Code + PKCE flow directly. Access tokens (JWT) are stored in the browser
   (localStorage, sessionStorage, or memory) and sent as `Authorization:
   Bearer <token>` headers to the API. The API is an OAuth2 resource server
   validating JWTs.
2. **Backend-for-Frontend (BFF)**: the Spring Boot backend handles the OIDC
   flow as an OAuth2 client. Tokens are stored server-side. The Angular
   frontend communicates with the backend over HttpOnly session cookies.
   The frontend never sees tokens.

Both patterns are used in production. The SPA-as-public-client pattern is
historically more common; BFF has gained traction since OWASP began
recommending it in 2021 for security-sensitive applications.

Pecunia handles personal financial data, making the security trade-off
significant.

## Decision

Pecunia adopts the **Backend-for-Frontend (BFF) pattern**.

### Architecture

- The Spring Boot backend is configured as an **OAuth2 client** (not a
  resource server) using `spring-boot-starter-oauth2-client`.
- Keycloak's Authorization Code + PKCE flow is initiated by the backend on
  user login.
- Access tokens, refresh tokens, and ID tokens are stored server-side in a
  Redis-backed session.
- The Angular frontend uses an HttpOnly, Secure, SameSite=Strict session
  cookie. The frontend never sees or stores any token.
- CSRF protection is enabled via Spring Security's default mechanism for
  cookie-based sessions.

### Future evolution

When the application grows into multiple microservices, the BFF can extract
tokens from the session and forward them as `Authorization: Bearer` headers
to internal services configured as OAuth2 resource servers. This creates a
two-tier architecture: BFF at the edge (sessions), resource servers
internally (JWT). This is documented but not implemented in the MVP.

## Consequences

### Positive

- **No tokens in the browser**: eliminates XSS exfiltration risk for access
  and refresh tokens.
- **CSRF protection is straightforward** with session cookies and Spring
  Security defaults.
- **Immediate session revocation**: invalidating a session server-side is
  instant, vs waiting for JWT expiration.
- **Simpler frontend**: no token management, no refresh logic, no expiration
  handling in Angular.
- **Recommended by OWASP** for SPAs with sensitive APIs.
- **Audit-friendly**: all token activity is server-side and loggable.

### Negative

- **Server-side session storage required**: sessions must be persisted (Redis
  in Pecunia's case) for the application to survive restarts and to scale
  horizontally.
- **Less stateless than JWT-only**: the backend has state per user (the
  session).
- **Slightly more complex backend configuration**: configuring Spring as an
  OAuth2 client + session manager + CSRF guard is more involved than a
  resource server.

### Neutral

- **Performance**: session lookup adds a Redis call per request, but Redis
  latency is sub-millisecond.

## Alternatives Considered

### SPA-as-public-client with JWT in localStorage

Rejected because:
- localStorage is accessible to any JavaScript code: a successful XSS
  exfiltrates the token.
- Token revocation is difficult: tokens remain valid until expiration.
- CSRF protection requires custom headers, which Angular handles but adds
  complexity.

### SPA-as-public-client with JWT in HttpOnly cookie

Considered. The token is in a cookie, not localStorage, mitigating XSS.
However:
- The backend must still validate JWT on every request (slower than session
  lookup with Redis cache for tokens that have many claims).
- CSRF protection still required.
- Token revocation still requires an introspection mechanism or blocklist,
  adding complexity.

The BFF pattern offers similar XSS protection with simpler ergonomics for
the use case.

## References

- OWASP Cheat Sheet, "OAuth 2.0 for Browser-Based Apps":
  https://cheatsheetseries.owasp.org/cheatsheets/OAuth2_for_Browser-Based_Apps_Cheat_Sheet.html
- Curity, "The Token Handler Pattern":
  https://curity.io/resources/learn/token-handler-overview/
- Jérôme Wacongne (ch4mpy), "Spring Addons Starter OIDC":
  https://github.com/ch4mpy/spring-addons

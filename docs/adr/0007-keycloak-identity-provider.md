# ADR-0007: Keycloak as Identity Provider

## Status

Accepted

## Context

Pecunia requires an OIDC-compliant identity provider for user authentication
and authorization. Several options exist:

1. **Keycloak** (Red Hat): open-source, self-hosted, mature, widely adopted
   in enterprise contexts.
2. **Spring Authorization Server**: official Spring project, lightweight,
   tightly integrated with Spring ecosystem.
3. **Authentik**: open-source, modern, fast-growing alternative to Keycloak.
4. **Cloud providers** (Auth0, AWS Cognito, Azure AD B2C): managed
   solutions, no self-hosting, but with cost and vendor lock-in.
5. **Custom implementation**: not seriously considered (security risk).

Pecunia is a self-hosted, low-cost personal project. The IdP must:
- Support OIDC Authorization Code + PKCE flow.
- Run reliably on a small VPS with limited resources.
- Be widely recognized for CV value.

## Decision

Pecunia uses **Keycloak** as its identity provider.

Keycloak runs in a Docker container during local development and on the
production VPS. A pre-configured realm is exported as JSON and versioned in
the repository, ensuring reproducible setup across environments.

The realm includes:
- A confidential OIDC client for the Spring Boot backend (BFF).
- Standard claims and roles (`USER`, `ADMIN` reserved for future use).
- A development user for local testing (production uses real registration
  or pre-created accounts).

## Consequences

### Positive

- **Industry standard**: Keycloak is widely used in financial services and
  enterprise contexts. Familiarity is a recognized CV asset.
- **Feature-rich**: built-in support for MFA, social login, identity
  brokering, and custom flows — overkill for the MVP but available if needed.
- **Self-hosted**: full control, no vendor lock-in, no per-user pricing.
- **Strong Spring integration**: extensive documentation and community
  support for Spring Security + Keycloak setups.
- **Realm export/import**: configuration is reproducible and versionable.

### Negative

- **Resource footprint**: Keycloak requires its own database (PostgreSQL) and
  uses ~500 MB RAM at idle. Significant on a small VPS.
- **Configuration complexity**: Keycloak's admin UI is comprehensive but not
  always intuitive. Initial setup requires careful attention.
- **Operational burden**: another service to monitor, back up, and update.

### Neutral

- **Versioning**: Keycloak releases major versions frequently with
  occasional breaking changes. Pinning the Docker image version is essential.

## Alternatives Considered

### Spring Authorization Server

Considered seriously. Strong fit for an all-Spring project. However:
- Less broadly recognized than Keycloak (a Spring-internal tool vs an
  industry-standard IdP).
- Lacks the polished admin UI of Keycloak.
- Requires building user management, MFA, and other features from scratch
  if needed.

Spring Authorization Server may be revisited if Keycloak's resource
footprint becomes problematic.

### Authentik

Modern, lighter than Keycloak, growing community. Rejected because:
- Less recognized in the financial services context.
- Smaller community than Keycloak.
- Less mature documentation for Spring integration.

### Managed cloud IdP

Rejected because:
- Adds external cost (per-user pricing models).
- Adds vendor lock-in.
- Loses the demonstration of self-hosted identity management.

## References

- Keycloak documentation: https://www.keycloak.org/documentation
- Stian Thorgersen, "Keycloak — Identity and Access Management for Modern
  Applications" (Packt, 2nd edition).
- Spring Security + Keycloak integration guides on baeldung.com.

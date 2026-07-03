# ADR-0028: Internal user identity decoupled from the identity provider

## Status

Accepted

## Context

Pecunia authenticates users through Keycloak (OIDC, BFF pattern). After login,
the authenticated principal carries the OpenID Connect claims, including the
issuer (`iss`) and the subject (`sub`).

Pecunia is multi-tenant by design: every aggregate root has an `owner` of type
`UserId`, every query filters by it, and every use case verifies ownership.
Something must therefore produce the `UserId` that stamps ownership, from the
authenticated principal.

Two ways to source that `UserId`:

1. **Use the Keycloak `sub` directly as the `UserId`.** Zero infrastructure.
2. **Keep an internal `UserId`, distinct from the IdP subject**, and map the
   IdP identity to it through a `User` aggregate persisted locally.

Relevant forces:

- The project convention is **UUID v7** for all identifiers (RFC 9562,
  time-ordered). The Keycloak `sub` is a **UUID v4** (random).
- Per **OpenID Connect Core**, only the `(iss, sub)` pair is a stable, unique
  identifier for an end-user: `sub` is unique *only within a given issuer*, and
  `email` / `preferred_username` MUST NOT be used as identity keys.
- The IdP may change over the application's life; ownership data must not be
  hostage to that.
- The MVP hosts a single real user, but the architecture is multi-tenant from
  day one.

## Decision

Adopt option 2: **an internal `UserId` (UUID v7, application-generated),
distinct from the IdP subject.** A `User` aggregate in the `identity` bounded
context maps the federated identity to that internal id.

- **`IdpIdentity(issuer, subject)`** value object captures the federated
  identity. Matching is done on the **`(iss, sub)` pair**, never on `sub`
  alone, and never on profile claims.
- **`User(UserId id, IdpIdentity idpIdentity)`** aggregate, persisted in
  `users(id, idp_issuer, idp_subject, created_at)` with
  `UNIQUE (idp_issuer, idp_subject)`.
- `idp_issuer` / `idp_subject` are typed **`text`**: the OIDC `sub` is an
  opaque string (Keycloak emits a UUID today, but that must not leak into the
  schema and couple us to one IdP's format).
- **Minimal aggregate.** Profile attributes (display name, email) are **not**
  copied into the domain: they live in the token and are exposed by
  `GET /api/me`. They are used only to populate a token, never to identify.
- **Just-in-time provisioning**: the `User` row is created on the first login
  (custom `OidcUserService`), matched on `(iss, sub)`. Every other aggregate's
  `owner` references the internal `UserId`, never the IdP subject.

The concurrency of that first-login provisioning is addressed separately in
[ADR-0029](0029-idempotent-user-provisioning.md).

## Consequences

### Positive

- **The domain is decoupled from the IdP.** Switching identity provider (or
  Keycloak realm) updates `idp_issuer` / `idp_subject`; the internal `UserId`
  and every `owner` reference across the system survive unchanged.
- **Uniform UUID v7 identifiers** everywhere, consistent with the project
  convention, instead of leaking a v4 IdP subject into every table.
- A stable internal anchor for future user-owned data (preferences, GDPR
  deletion/anonymization).
- Correct OIDC identity semantics: the `(iss, sub)` pair, stored opaquely.

### Negative

- More than option 1's zero-infrastructure: a `User` aggregate, a `users`
  table, a repository, and a JIT-provisioning path must exist.
- One indirection (lookup / provision) at login to resolve the internal
  `UserId` from the principal.

### Neutral

- The MVP serves a single user, but the mapping is multi-tenant-ready by
  construction.
- Profile data stays authoritative at the IdP; Pecunia never becomes its
  system of record.

## Alternatives Considered

### Option 1 — Keycloak `sub` as the `UserId`

`UserId.of(UUID.fromString(principal.getSubject()))`. Rejected: it couples the
domain identity to the IdP (an IdP change would rewrite every `owner`), the
`sub` is a UUID v4 which breaks the project's UUID v7 convention, and it leaves
no place to anchor internal user data.

### Pre-provisioning / SCIM

Create users ahead of time (batch import, or an IdP pushing changes via SCIM).
Rejected for the MVP: heavier than needed for a single real user; JIT covers
first-login creation. Noted as a future option for full lifecycle management
(deactivation, near-real-time attribute sync).

## References

- [OpenID Connect Core — subject identifier stability](https://openid.net/specs/openid-connect-core-1_0.html)
- [Authelia — OIDC claims (`iss`+`sub` as the unique identifier)](https://www.authelia.com/integration/openid-connect/openid-connect-1.0-claims/)
- [Microsoft Learn — keying users with OIDC](https://learn.microsoft.com/en-us/answers/questions/1031142/how-to-key-users-when-using-oidc-and-scim)
- [ADR-0029](0029-idempotent-user-provisioning.md) — concurrency-safe JIT provisioning (the operational complement of this decision)
- `docs/domain-model.md` — the `User` aggregate

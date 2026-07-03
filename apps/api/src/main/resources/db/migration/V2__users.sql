-- First domain table: the internal representation of an authenticated user
-- (identity bounded context). Pecunia decouples its own identity from the IdP.
-- A row is provisioned just-in-time on the first login (custom OidcUserService),
-- and every aggregate's `owner` references this internal id -- never the IdP
-- subject -- so a change of identity provider never rewrites ownership.
--
-- Named `users` (plural) on purpose: `user` is a reserved word in PostgreSQL and
-- would otherwise need quoting on every reference.
CREATE TABLE users (
    -- Internal UserId, generated application-side as a UUID v7 (RFC 9562). No
    -- database default: the application always supplies the id.
    id           uuid        NOT NULL,

    -- OIDC federated identity. Per OpenID Connect Core, only the (iss, sub) pair
    -- is a stable, unique identifier for an end-user; `sub` alone is unique only
    -- within a given issuer. Stored as text because the OIDC `sub` is an opaque
    -- string -- Keycloak emits a UUID today, but that must not leak into the schema.
    idp_issuer   text        NOT NULL,
    idp_subject  text        NOT NULL,

    -- Provisioning instant (audit). timestamptz combined with the UTC default
    -- pinned in V1 keeps values deterministic across dev, test and prod.
    created_at   timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_users PRIMARY KEY (id),

    -- The federated identity is the natural key used to match a returning user.
    -- The unique index it creates is also the lookup path for findByIdpIdentity.
    CONSTRAINT uq_users_idp_identity UNIQUE (idp_issuer, idp_subject)
);

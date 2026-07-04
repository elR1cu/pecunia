-- The account aggregate (account bounded context): the registry of tracked
-- accounts owned by a user. See ADR-0030 (mapping conventions) and ADR-0031
-- (tenancy isolation and cross-context references).
CREATE TABLE accounts (
    -- AccountId, generated application-side as a UUID v7 (RFC 9562). No database
    -- default: the application always supplies the id (ADR-0015).
    id                        uuid          NOT NULL,

    -- Owning UserId. A plain uuid with NO foreign key to identity's `users`
    -- table: bounded contexts share identifiers, not schema-level joins
    -- (ADR-0031). `owner_id` never comes from user input -- it is the
    -- authenticated principal -- so an orphan owner cannot arise in normal flow.
    owner_id                  uuid          NOT NULL,

    -- AccountType / AccountStatus persisted as their enum names (never ordinals),
    -- guarded by CHECK constraints as defense in depth against out-of-band writes.
    type                      text          NOT NULL,
    status                    text          NOT NULL,

    name                      text          NOT NULL,

    -- Nullable: a CREDIT_CARD has no IBAN (it is repaid from a current account);
    -- the other types require one. The invariant is enforced in the domain and
    -- mirrored here (chk_accounts_iban_by_type) as defense in depth.
    iban                      text,

    -- Money initialBalance flattened into (amount, currency) via the
    -- MoneyEmbeddable (ADR-0030). numeric(19,4) matches Money's fixed scale of 4;
    -- the currency is stored as its ISO-4217 code (Hibernate maps java.util.Currency).
    initial_balance_amount    numeric(19, 4) NOT NULL,
    initial_balance_currency  text          NOT NULL,

    -- Optimistic-lock version (JPA @Version, mapped to a wrapper Long). A null
    -- version is what routes a freshly opened account to persist() rather than
    -- merge() (no phantom SELECT on insert); Hibernate then sets 0 on insert and
    -- increments on each update, failing a concurrent stale write. See ADR-0030.
    version                   bigint        NOT NULL,

    -- Creation instant (audit). timestamptz + the UTC default pinned in V1 keeps
    -- values deterministic across dev, test and prod. Not a domain field; the
    -- entity maps it read-only, as UserEntity does.
    created_at                timestamptz   NOT NULL DEFAULT now(),

    CONSTRAINT pk_accounts PRIMARY KEY (id),

    CONSTRAINT chk_accounts_type   CHECK (type IN ('CURRENT', 'SAVINGS', 'CREDIT_CARD')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),

    -- CREDIT_CARD => no IBAN; every other type => an IBAN is present.
    CONSTRAINT chk_accounts_iban_by_type CHECK (
        (type = 'CREDIT_CARD' AND iban IS NULL)
        OR (type <> 'CREDIT_CARD' AND iban IS NOT NULL)
    )
);

-- Every AccountRepository read filters by owner (findByIdAndOwner, findAllByOwner);
-- the index makes that predicate effectively free (ADR-0014, ADR-0031).
CREATE INDEX idx_accounts_owner ON accounts (owner_id);

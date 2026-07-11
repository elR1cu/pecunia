-- The category aggregate (category bounded context): the user-defined
-- hierarchical classification of transactions. See ADR-0030 (mapping
-- conventions), ADR-0031 (tenancy isolation and cross-context references)
-- and ADR-0033 (owner resolution).
CREATE TABLE categories (
    -- CategoryId, generated application-side as a UUID v7 (RFC 9562). No database
    -- default: the application always supplies the id (ADR-0015).
    id             uuid        NOT NULL,

    -- Owning UserId. A plain uuid with NO foreign key to identity's `users`
    -- table: bounded contexts share identifiers, not schema-level joins
    -- (ADR-0031). `owner_id` never comes from user input -- it is resolved from
    -- the authenticated principal by the application services (ADR-0033).
    owner_id       uuid        NOT NULL,

    -- CategoryType persisted as its enum name (never ordinals), guarded by a
    -- CHECK constraint as defense in depth against out-of-band writes.
    type           text        NOT NULL,

    name           text        NOT NULL,

    -- HexColor value object, normalised to '#RRGGBB' (uppercase) by the domain.
    -- The CHECK mirrors that invariant as defense in depth, like
    -- chk_accounts_iban_by_type does for accounts.
    color          text        NOT NULL,

    -- Nullable: icons are optional (Category.icon() is Optional<String>).
    icon           text,

    -- Self-referencing hierarchy of arbitrary depth; NULL marks a root. The FK
    -- guarantees referential integrity but NOT same-owner parenting -- that
    -- cross-aggregate invariant is enforced by the application services
    -- (ParentCategoryValidator), and the ancestor-walk CTE re-filters by owner
    -- as defense in depth. No ON DELETE action: categories are archived, never
    -- deleted (GDPR erasure will be handled wholesale, post-MVP).
    parent_id      uuid,

    -- Sibling ordering for display; the domain rejects negative values, the
    -- CHECK mirrors it.
    display_order  integer     NOT NULL,

    -- Lifecycle flag (Category.archived); an archived category is kept for
    -- historical transactions but rejects modification.
    archived       boolean     NOT NULL,

    -- Optimistic-lock version (JPA @Version, mapped to a wrapper Long). A null
    -- version routes a freshly created category to persist() rather than merge();
    -- Hibernate sets 0 on insert and increments on each update, failing a
    -- concurrent stale write. See ADR-0030.
    version        bigint      NOT NULL,

    -- Creation instant (audit). timestamptz + the UTC default pinned in V1 keeps
    -- values deterministic across dev, test and prod. Not a domain field; the
    -- entity maps it read-only, as AccountEntity does.
    created_at     timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT pk_categories PRIMARY KEY (id),

    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id),

    CONSTRAINT chk_categories_type          CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT chk_categories_color         CHECK (color ~ '^#[0-9A-F]{6}$'),
    CONSTRAINT chk_categories_display_order CHECK (display_order >= 0)
);

-- Every CategoryRepository read filters by owner (findByIdAndOwner,
-- findAllByOwner, findAncestorIds); the index makes that predicate effectively
-- free (ADR-0014, ADR-0031).
CREATE INDEX idx_categories_owner ON categories (owner_id);

-- The ancestor-walk recursive CTE (findAncestorIds) and future children lookups
-- traverse the self-FK; unlike accounts, this table needs the join column
-- indexed. Also serves the FK's referencing side.
CREATE INDEX idx_categories_parent ON categories (parent_id);

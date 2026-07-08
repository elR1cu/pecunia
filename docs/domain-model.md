# Domain Model

This document describes the business concepts of Pecunia, the entities that
represent them, and the rules that govern their behavior. It is the
canonical reference for the domain layer.

For the technical architecture supporting this model, see
[`architecture.md`](architecture.md).

## Bounded Contexts

Pecunia is organized into bounded contexts, each owning a coherent slice of
the business model.

### MVP contexts

- **Account context**: bank accounts and their balances.
- **Transaction context**: individual debit and credit movements.
- **Category context**: hierarchical classification of transactions.
- **Import context**: ingestion of camt.053 bank statements.

### Post-MVP contexts

- **Budget context**: monthly budgets per category and threshold alerts.
- **Savings context**: savings capacity computation and savings goals.
- **Recurrence context**: detection and management of recurring
  transactions and fixed charges.

Cross-context interactions occur exclusively through domain events or
explicit application-level use case invocations. Contexts never reach into
each other's internals.

## Core Concepts

### Entities and value objects

The domain distinguishes two kinds of building blocks, and the distinction
drives their mutability, their equality, and how they are implemented.

**Value objects** describe *something* — an amount, an account number, an
identifier. They have no identity of their own; two value objects with the
same attributes are interchangeable.

- **Immutable**: any "change" produces a new instance.
- **Value-based equality**: equal when all attributes are equal.
- **Implemented as Java records**, which give both properties for free.
- Examples: `Money`, `Iban`, `AccountId`, `UserId`, `CategoryPath`.

**Entities** (and the aggregate roots that lead them) describe *something
that exists over time* — an account, a category. They have a stable identity
that survives changes to their state.

- **Mutable**: state evolves in place through intention-revealing methods
  (e.g. `account.archive()`), never through public setters.
- **Identity-based equality**: equal when their identifier is equal, whatever
  their current state. Two in-memory copies of the same account — one active,
  one archived — are still the same account.
- **Implemented as plain Java classes** with a private canonical constructor,
  named factory methods (`open` for birth, `reconstitute` for rehydration),
  and `equals`/`hashCode` computed on the identifier alone.
- Examples: `Account`, `Category`.

**Why mutable entities, immutable value objects.** An entity models an
identity whose state changes over its lifetime; persistence stores the
*current state* of that identity, which aligns naturally with in-place
mutation of a managed record. A value object models a value, for which
"changing" makes no more sense than changing the number 3 — immutability
makes it safe to share and compare. Modelling an entity as immutable is
possible (event sourcing does it) but adds re-attachment friction with
classic JPA, for no gain here.

This convention is enforced, not merely documented: the domain layer is kept
framework-free (no Spring, JPA, Hibernate or Lombok — see
[ADR-0013](adr/0013-selective-use-of-lombok.md)) by the ArchUnit fitness
tests ([ADR-0016](adr/0016-archunit-architectural-fitness-tests.md)).

### Money

All monetary values are represented as a `Money` value object:

- **Amount**: `BigDecimal`, never `double` or `float`.
- **Currency**: ISO 4217 currency code (`CHF` for the MVP).
- **Scale**: fixed at 4 with `HALF_EVEN` (banker's) rounding, decoupled from
  display precision. Computations keep 4 fraction digits to avoid rounding
  drift in derived values; amounts are rounded to the currency's minor units
  (2 for CHF) only at the presentation/settlement boundary.
- **Invariants**:
  - Operations between different currencies are not allowed (throw at the
    domain level).
  - Subtraction may yield negative values; this is meaningful (refunds,
    overdrafts).
  - Equality is value-based.

The `Money` type is a Java record in the shared kernel.

### User

A `User` represents the authenticated person using the application. In the
MVP, there is a single user. The `User` aggregate is intentionally minimal and
**decouples Pecunia's internal identity from the identity provider**:

- `UserId`: internal identifier, generated application-side as a UUID v7,
  distinct from any IdP identifier.
- `IdpIdentity`: the federated identity at the OIDC provider — the
  `(issuer, subject)` pair from the ID token. Per OpenID Connect Core, only
  this pair is a stable, unique identifier for an end-user; `subject` alone is
  unique only within a given issuer.

The `User` is provisioned just-in-time on the first login and matched on its
`IdpIdentity`. Profile attributes (display name, email) are deliberately **not**
copied into the domain: they live in the token and are exposed by
`GET /api/me`. Authentication is delegated to Keycloak; the domain knows only
the internal identifier and the federated-identity mapping. See
[ADR-0028](adr/0028-internal-user-identity-mapping.md) (identity mapping) and
[ADR-0029](adr/0029-idempotent-user-provisioning.md) (concurrency-safe
provisioning).

**Multi-user note**: every aggregate root in Pecunia references a `UserId`.
The application is designed to host multiple isolated users from day one,
even though the MVP serves a single user in production. There is never a
"global" entity shared across users.

### Account

An `Account` represents a bank account or credit card tracked by Pecunia.

**Attributes**:
- `accountId`: UUID.
- `owner`: `UserId` of the user owning the account.
- `name`: user-defined display name (e.g., "UBS Current", "UBS Visa").
- `type`: enum (`CURRENT`, `SAVINGS`, `CREDIT_CARD`).
- `iban`: present for bank accounts (`CURRENT`, `SAVINGS`), absent for credit
  cards.
- `currency`: ISO 4217 code.
- `initialBalance`: `Money`. The opening balance at the moment of account
  registration in Pecunia.
- `status`: `AccountStatus` enum (`ACTIVE`, `ARCHIVED`). Archived accounts
  remain visible historically but no new transactions are recorded.

**Computed**:
- `currentBalance`: derived from `initialBalance` and the sum of the account's
  transactions. Not stored. The domain never imports `Transaction`: the
  movements sum is supplied by the transaction context (see Session 19's
  anti-cycle design).

**Invariants**:
- An account always has an owner.
- A credit card has no IBAN; a current or savings account requires one.
- An archived account cannot receive new transactions.

**Business operations**:
- `Account.open(...)`: factory method to open a new account (always `ACTIVE`).
- `Account.reconstitute(...)`: factory method to rehydrate a persisted account
  (used by the infrastructure mapper), preserving its stored `status`.
- `account.archive()`: archives the account; archiving an already archived
  account is rejected.
- `account.balanceFrom(movementsSum)`: returns the current balance as
  `initialBalance + movementsSum`.

### Transaction

A `Transaction` represents an individual movement on an account: a debit
(expense), a credit (income), or a transfer between owned accounts.

**Attributes**:
- `transactionId`: UUID.
- `account`: `AccountId` of the source account.
- `bookingDate`: `LocalDate`. The date on which the bank recorded the
  transaction.
- `valueDate`: `LocalDate`. The date on which the transaction takes effect
  on the balance.
- `amount`: `Money`. Always positive; direction is indicated by `kind`.
- `kind`: enum (`EXPENSE`, `INCOME`, `TRANSFER`).
- `counterparty`: optional, the merchant or counterparty name extracted from
  the bank statement.
- `rawDescription`: the original description from the bank statement (kept
  for reference).
- `cleanedDescription`: a user-readable version, possibly edited.
- `category`: optional `CategoryId`. Until categorized, the transaction is
  "unclassified".
- `tags`: a set of `TagId` (zero or more).
- `source`: enum (`CAMT053_IMPORT`, `MANUAL`).
- `externalReference`: stable identifier used for deduplication (from
  camt.053 EndToEndId or a deterministic hash).
- `status`: enum (`CONFIRMED`, `PENDING`). Pending transactions are
  provisional (e.g., authorized but not settled card payments).

**Invariants**:
- `amount` is positive; the sign of the impact is determined by `kind`.
- `kind = TRANSFER` is excluded from spending and saving computations.
- `bookingDate >= valueDate - 7 days` (sanity check, configurable).
- An imported transaction cannot be created with the same
  `externalReference` as an existing one (deduplication enforced).

**Business operations**:
- `Transaction.fromCamt053Entry(...)`: factory method for imported entries.
- `Transaction.recordManually(...)`: factory method for user-entered
  transactions.
- `transaction.categorize(categoryId)`: assigns or changes the category.
- `transaction.tag(tagId)` / `transaction.untag(tagId)`: manages tags.
- `transaction.markAsTransfer()`: changes `kind` to `TRANSFER` (e.g., when
  the user recognizes a Visa payment as an internal transfer).

### Category

A `Category` classifies transactions by purpose.

**Attributes**:
- `categoryId`: UUID.
- `owner`: `UserId`.
- `name`: display name (e.g., "Groceries", "Restaurants").
- `type`: enum (`EXPENSE`, `INCOME`). Determines which transactions can be
  assigned this category. There is no `TRANSFER` category: transfers move a
  user's own money between their accounts and are never classified by purpose
  (see "Category type constraints"), so a `TRANSFER` category would be dead,
  unassignable surface.
- `parent`: optional `CategoryId` for a hierarchy of arbitrary depth (e.g.,
  "Groceries" has parent "Food", which may itself have a parent).
- `color`: hex color for UI display, validated as `#RRGGBB`.
- `icon`: optional icon identifier for UI display.
- `displayOrder`: integer for sorting in the UI.
- `archived`: boolean; archived categories remain in historical data but
  cannot be assigned to new transactions.

**Invariants**:
- A category cannot be its own parent. This direct self-reference is the only
  hierarchy rule the aggregate can enforce on its own, and it does.
- A category cannot be its own ancestor (no cycles in the hierarchy) and must
  share its parent's `type`. Because each `Category` is its own aggregate root
  that references its `parent` by id only, these two rules span multiple
  categories: they are enforced by the application service, which walks the
  ancestor chain via the `CategoryRepository` port, not by the pure aggregate.
- Archiving a category does not break existing transactions referencing it.

**Business operations**:
- `Category.create(...)`: factory method.
- `category.rename(...)`, `category.recolor(...)`, etc.
- `category.archive()`.
- `category.moveTo(newParent)`: reparenting.

### Tag

A `Tag` is a lightweight, user-defined label that can be applied to
transactions. Unlike categories, tags are not hierarchical and a transaction
may have multiple tags.

**Attributes**:
- `tagId`: UUID.
- `owner`: `UserId`.
- `name`: short text.
- `color`: optional hex color.

**Invariants**:
- Tag names are unique per user.

## Domain Events

Domain events are facts about something that happened. They are published
via the `DomainEventPublisher` port (see ADR-0008).

### MVP events

- **`Camt053StatementImported`**: emitted after a successful camt.053
  import, with statistics (count of new transactions, duplicates skipped).
- **`TransactionCreated`**: emitted when a new transaction is persisted
  (manual or imported).
- **`TransactionCategorized`**: emitted when a transaction's category is
  assigned or changed.
- **`AccountArchived`**: emitted when an account is archived.

### Post-MVP events

- **`BudgetThresholdReached`**: emitted when a budget reaches 80%, 100%, or
  120% utilization.
- **`RecurringExpenseDetected`**: emitted when the system identifies a new
  recurring expense pattern.
- **`SavingsGoalAchieved`**: emitted when a savings goal reaches 100%.

All events are `record` types implementing a marker `DomainEvent` interface:

```java
public sealed interface DomainEvent
    permits Camt053StatementImported,
            TransactionCreated,
            TransactionCategorized,
            AccountArchived {
    Instant occurredAt();
}
```

## Use Cases

Use cases are the application's business operations, expressed as commands
or queries. Each use case is implemented in the application layer as a
single class with a single public method.

### MVP use cases

#### Account context

- `OpenAccount`: register a new bank account or credit card.
- `ArchiveAccount`: archive an existing account.
- `ListAccounts`: retrieve all accounts for the current user.
- `GetAccountBalance`: compute the current balance of an account.

#### Transaction context

- `RecordManualTransaction`: create a transaction manually entered by the
  user.
- `CategorizeTransaction`: assign or change the category of a transaction.
- `TagTransaction` / `UntagTransaction`: manage tags.
- `MarkTransactionAsTransfer`: reclassify a transaction as an internal
  transfer.
- `ListTransactions`: paginated, filtered list of transactions.
- `GetTransactionDetails`: retrieve a single transaction.

#### Category context

- `CreateCategory`: create a new category.
- `RenameCategory`, `RecolorCategory`, `MoveCategoryToParent`,
  `ArchiveCategory`.
- `ListCategories`: retrieve the user's category tree.

#### Import context

- `ParseCamt053File`: parse an uploaded camt.053 XML file and return a
  preview (no persistence).
- `ConfirmCamt053Import`: persist the previewed transactions, deduplicating
  against existing ones.

### Post-MVP use cases

- `DefineBudget`, `ListBudgets`, `GetBudgetStatus`.
- `ComputeSavingsCapacity`.
- `CreateSavingsGoal`, `TrackSavingsGoalProgress`.
- `DefineCategorizationRule`, `ApplyCategorizationRules`.
- `RequestAiCategorizationSuggestion`.
- `DetectRecurringTransactions`.

## Business Rules

### Budget month

The default budget month starts on the 1st and ends on the last day of the
calendar month. This is configurable per user (post-MVP).

### Savings capacity

Savings capacity for a given month M is computed as:

```
SavingsCapacity(M) =
  average(income, M-3 to M-1)
  - sum(fixed_charges_monthlyized)
  - average(variable_expenses, M-3 to M-1)
```


Where:
- `income` includes all transactions of `kind = INCOME` excluding refunds.
- `fixed_charges_monthlyized` converts non-monthly recurring expenses to
  their monthly equivalent (e.g., a 120 CHF annual subscription = 10
  CHF/month).
- `variable_expenses` includes all `EXPENSE` transactions not flagged as
  recurring.
- The lookback period (3 months by default) is configurable.
- `TRANSFER` transactions are excluded entirely.

### Credit card payment handling

When a transaction on the current account represents a payment to the
credit card (matched by counterparty pattern and amount equal to a credit
card balance), it is reclassified as `TRANSFER`. The individual transactions
on the credit card remain `EXPENSE`.

This avoids double-counting: the user spent on the card (expense), and
later paid the card from the current account (transfer, not new spending).

### Deduplication of imported transactions

When importing a camt.053 file:
1. Each entry in the file has a stable identifier (EndToEndId, or if
   absent, a deterministic hash of date, amount, counterparty, and
   description).
2. Before persisting, the system checks whether an existing transaction
   has the same `externalReference` for the same account.
3. Matching entries are skipped and reported as duplicates in the import
   summary.

### Category type constraints

- A transaction of `kind = EXPENSE` can only be assigned a category of
  `type = EXPENSE`.
- A transaction of `kind = INCOME` can only be assigned a category of
  `type = INCOME`.
- A transaction of `kind = TRANSFER` cannot be assigned any category
  (transfers are not classified).

## Identifiers

All entity identifiers are UUIDs generated **application-side** (not by the
database).

- **Format**: UUID v7 (RFC 9562) for all aggregate roots. The time-ordered
  property of UUID v7 ensures B-tree index locality, avoiding the
  fragmentation issues that plague UUID v4 as primary keys.
- **Library**: `com.github.f4b6a3:uuid-creator` (until JDK 26 introduces
  `UUID.ofEpochMillis()` natively).
- **Encoding**: stored as `UUID` (16 bytes binary) in PostgreSQL, exposed
  as canonical string form in the API.
- **Type safety**: each entity has a typed wrapper record (e.g.,
  `AccountId`, `TransactionId`) to prevent mixing identifiers at compile
  time.

### Why application-side generation

- **Testability**: aggregates can be created in unit tests without database
  access. The ID is available immediately.
- **Domain events**: events carry the entity ID at creation time, before
  any persistence. This simplifies event-driven patterns (and future
  migration to Kafka).
- **Decoupling**: the domain has no knowledge of the persistence strategy.
  Switching databases or persistence frameworks requires no change to
  identifier generation.

### Why UUID v7 over UUID v4

UUID v4 is random, causing B-tree index fragmentation when used as a
primary key. UUID v7 embeds a 48-bit Unix timestamp in milliseconds in its
high-order bits, making it lexicographically sortable by creation time
while remaining globally unique and unpredictable. This combines the
benefits of UUIDs (no central coordination, no information leak) with the
indexing performance of sequential keys.

When JDK 26 ships with `UUID.ofEpochMillis(long)`, the external library
will be removed in favor of the standard JDK API.

## Persistence Mapping

The domain entities have corresponding JPA entities in the infrastructure
layer. They are **distinct types**: domain entities are pure Java with
business semantics; JPA entities are mutable structures optimized for
Hibernate's lifecycle.

Mapping between them is performed by repository adapters or dedicated
mappers. This separation ensures the domain remains framework-free.

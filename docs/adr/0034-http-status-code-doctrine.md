# ADR-0034: HTTP status code doctrine — 400 schema, 422 semantics, 409 state, 404 tenancy

## Status

Accepted

Amends ADR-0027 (the HTTP column of its Rule 2 mapping table: domain
invariant violations move from 400 to 422).

## Context

The `category` web slice introduces requests that reference *another*
resource in their body: creating a category under a parent, and moving a
category to a new parent. Those references can be semantically invalid in
ways a schema cannot express — the parent does not exist, belongs to another
user, is archived, or has a different `CategoryType` — and the contract must
pick a status code for them.

The current convention (ADR-0027, applied in `AccountRestControllerAdvice`)
maps every `DomainException` to **400 Bad Request**, alongside the 400s that
Spring produces for Jakarta Bean Validation failures on DTOs. This conflates
two different failure families: "the request does not match the schema" and
"the request matches the schema but is business-wise unprocessable".

Outside references converge on separating them:

- **RFC 9110** promoted **422 Unprocessable Content** from WebDAV into core
  HTTP: the request is well-formed but semantically invalid.
- **GitHub's API** is the canonical practitioner: malformed JSON → 400;
  well-formed payload referencing a nonexistent branch → 422.
- **404 is rejected by consensus for referenced-resource failures**: 404
  speaks about the resource identified by the *URL*, and suggests "try again
  later" — both wrong for a bad reference inside the body.
- Zalando's guidelines prescribe "the most specific status code" and Problem
  JSON, but do not arbitrate 400 vs 422; Stripe pragmatically uses 400 for
  everything. The split is therefore a convention to choose, not a rule to
  obey.

A finance API whose portfolio value includes error-response quality
(ADR-0027 rejected generic exceptions for exactly that reason) benefits from
the finer signal. The question is transversal — every future context
(transaction, budget) will face it — so it deserves a project-wide doctrine
rather than a per-context choice.

## Decision

Adopt a four-line doctrine, applied uniformly across all bounded contexts:

| Status | Meaning | Producer |
|---|---|---|
| **400** | The request violates the *schema*: malformed JSON, missing required field, pattern/size violation | Spring MVC + Jakarta Bean Validation (`@Valid` on DTOs); no handler of ours |
| **422** | The request is well-formed but *semantically unprocessable*: a domain invariant or business rule rejects it, including invalid references to other resources | `@ExceptionHandler` on `DomainException` subtypes and application-level validation exceptions |
| **409** | The request conflicts with the resource's current *state*: illegal transition (already archived, archived-modification) or structural conflict (hierarchy cycle) | `@ExceptionHandler` on the dedicated exceptions |
| **404** | The resource identified by the *URL* does not exist **for this user** (absent or owned by someone else — indistinguishable, per the multi-tenancy rule) | `@ExceptionHandler` on `*NotFoundException` |

Complementary rules:

- **Cross-tenant indistinguishability extends to referenced resources**: a
  422 for "invalid parent" must be byte-identical whether the parent is
  absent, archived, or owned by another user. The response never reveals
  which.
- Programmer errors (`NullPointerException`, invariants mirrored by DTO
  validation that still reach the service) keep falling through to **500** —
  they are bugs, not client errors.
- All error bodies remain RFC 9457 `ProblemDetail` (unchanged).

### Migration

`account` is migrated immediately to keep the convention transversal:

- `AccountRestControllerAdvice`: the `DomainException` handler moves from
  `HttpStatus.BAD_REQUEST` to `HttpStatus.UNPROCESSABLE_ENTITY`. Affected
  types: `InvalidIbanException`, `IbanRequiredException`,
  `IbanForbiddenForTypeException`.
- `contracts/openapi.yaml`: the `POST /api/accounts` 400 response is split
  into 400 (schema) and 422 (domain invariants); descriptions updated.
- ADR-0027's Rule 2 table is amended by this ADR (400 → 422 on the three
  IBAN rows); the ADR itself stays Accepted.

The `category` contract applies the doctrine from day one:

| Failure | Status |
|---|---|
| `CategoryNotFoundException` (URL resource) | 404 |
| `InvalidParentCategoryException` (absent / foreign / archived parent) | 422 |
| `CategoryTypeMismatchException` | 422 |
| Domain invariants escaping DTO validation (`InvalidHexColorException`, …) | 422 |
| `CategoryCycleException` | 409 |
| `ArchivedCategoryModificationException`, `CategoryAlreadyArchivedException` | 409 |

## Consequences

### Positive

- Clients (and the Angular frontend) can distinguish "fix the request
  format" (400) from "the business refused" (422) from "refresh, the state
  moved" (409) without parsing messages.
- One doctrine for all present and future bounded contexts; the next
  contract (transaction import) inherits it instead of re-debating.
- Aligned with RFC 9110 and the most-cited practitioner convention (GitHub),
  which reads well in a public portfolio project.

### Negative

- One more status code for the frontend error handling to branch on.
- The 400/422 boundary depends on which invariants are mirrored in DTO
  validation (already noted in ADR-0027): moving a rule between DTO and
  domain can silently move its status code. The contract's per-endpoint
  response documentation is the guard.

### Neutral

- 404-for-ownership and 409-for-state are unchanged; this ADR only
  re-homes the semantic-violation family.
- Stripe-style "400 for everything" remains a respectable industry position;
  this project simply values the finer signal more.

## Alternatives Considered

### Keep 400 for everything (Stripe style)

Consistent with the existing advice and requires no migration, but erases
the schema/semantics distinction the error-modeling strategy (ADR-0027) was
built to expose, and forces clients back to message-sniffing to tell a
malformed payload from a rejected business rule. Rejected.

### 404 for invalid references

Extends the multi-tenancy 404 rule to resources referenced in the body.
Rejected: 404 is bound to the URL's resource by HTTP semantics, and the
consensus (GitHub, RFC discussions) treats bad body references as 422. The
anti-leak property is preserved anyway by the indistinguishability rule
above.

### Scope 422 to `category` only, migrate `account` later

Less work now, but ships a public contract that is internally inconsistent
(IBAN invariant → 400, parent invariant → 422 for the same failure family),
and the migration cost only grows with each new context. Rejected in favor
of immediate migration while the API surface is still small.

## References

- RFC 9110 §15.5.21 — 422 Unprocessable Content
- RFC 9457 — Problem Details for HTTP APIs
- ADR-0027 — Error modeling strategy (amended by this ADR)
- ADR-0014 / ADR-0031 — multi-tenancy and the 404-not-403 rule
- GitHub REST API documentation — 400 vs 422 usage
- Zalando RESTful API Guidelines — status code specificity, Problem JSON
- J. Geewax, *API Design Patterns* (Manning, 2021) — standard vs custom
  methods, error semantics

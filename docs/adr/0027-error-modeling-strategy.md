# ADR-0027: Error modeling strategy — sealed Results and a fine-grained domain exception hierarchy

## Status

Accepted

## Context

Block 2 introduces the first real use cases (`OpenAccount`, `ArchiveAccount`,
`ListAccounts`). Each can end in several ways: success, a domain invariant
being violated (invalid IBAN, IBAN required/forbidden for the account type),
an aggregate not found (or owned by another user), or an illegal state
transition (archiving an already-archived account).

The project needs a consistent rule for *how* an operation reports its
outcome, because two mechanisms are available and mixing them ad hoc produces
inconsistent controllers and leaky abstractions:

- **Return values** — a sealed `Result` interface with `record`
  implementations (the pattern CLAUDE.md already sketches under "Result
  Modeling"). Exhaustiveness is checked by the compiler in a `switch`.
- **Exceptions** — thrown and mapped to HTTP status codes at the web edge.

Two independent questions have to be answered together:

1. When does an *expected* business outcome travel as a `Result` value versus
   as an exception?
2. When a domain invariant is violated, should the domain throw generic JDK
   exceptions (`IllegalArgumentException`, `IllegalStateException`,
   `NullPointerException`) or a **fine-grained, named** exception hierarchy?

These interact: a rich exception hierarchy only pays off if exceptions are
allowed to propagate to the web layer and be mapped by type. If every
expected outcome is forced into a `Result`, the domain exceptions get caught
and re-expressed, duplicating the error vocabulary.

## Decision

Adopt **two complementary rules**.

### Rule 1 — When to use a sealed `Result` vs an exception ("Option C")

A use case returns a sealed `Result` case **only** when the outcome is, all
at once:

1. **expected** in the normal flow (not a bug, not an infrastructure
   failure), **and**
2. **specific to this use case** and **carries data** the caller must read to
   react.

Everything else is an exception:

- **unexpected** (violated invariant reaching the service, DB down) → exception;
- **expected but transverse / data-less** (aggregate not found, ownership
  mismatch) → a **fine application exception**, mapped once centrally.

In one line: *`Result` = an expected, use-case-specific, data-carrying
business value; exception = everything else (the unexpected, or the expected
but transverse / data-less).*

Consequence for `account`: none of the three use cases needs a `Result`.
`open` returns the created `AccountId`; `archive` returns `void` and signals
its two outcomes (not found → 404, already archived → 409) via exceptions;
`list` returns a (possibly empty) `List`. Sealed `Result` types are held in
reserve for richer use cases (e.g. `ImportTransactions` in Block 3:
`Success(imported, duplicates)`, `PartiallyImported(...)`, `EmptyFile()`).

### Rule 2 — A fine-grained domain exception hierarchy, introduced parsimoniously ("Option 2, parsimonious")

Domain invariant violations throw a **named** exception extending a shared
abstract base `DomainException`, rather than a bare
`IllegalArgumentException`. This gives the web layer **type-based** mapping
(robust, no message parsing) and lets the domain *state its own rules* in
code.

Parsimony rule: **create a subtype only for an invariant that is
domain-only** (not already mirrored by DTO validation) **and reachable by
ordinary user input**, i.e. one that deserves a distinct client-facing
reaction. Do not pre-build the hierarchy for hypothetical rules; let it grow
as real use cases appear.

Applied to `account`:

| Violation | Type thrown | HTTP | Rationale |
|---|---|---|---|
| `null` argument | `NullPointerException` (`Objects.requireNonNull`) | 500 | programmer error, never user input |
| blank `name` | `IllegalArgumentException` | 400 | mirrored by `@NotBlank` → a bug if it reaches the service |
| IBAN structure/checksum invalid | `InvalidIbanException` | 400 | domain-only, reachable by input |
| IBAN required but absent | `IbanRequiredException` | 400 | domain-only, reachable by input |
| IBAN present but forbidden for type | `IbanForbiddenForTypeException` | 400 | domain-only, reachable by input |
| already archived | `AccountAlreadyArchivedException` | 409 | domain-only state transition |
| not found / wrong owner | `AccountNotFoundException` (application layer) | 404 | transverse multi-tenant, not a domain invariant |

`DomainException` is **not** `sealed`: sealing would require the shared-kernel
base to `permit` types living in the bounded contexts, inverting the
dependency (a layering violation ArchUnit would reject), and it buys nothing
because exceptions are not matched exhaustively in a `catch`.

`AccountNotFoundException` lives in the **application** layer, not the domain:
"not found" is a repository-lookup concern, not a domain invariant. It maps
to **404, never 403**, so the existence of another user's resource is not
leaked (multi-tenancy rule).

### Why the two rules belong together

Rules 1 and 2 sit on different axes but reinforce each other. A fine
exception hierarchy (Rule 2) only pays off when exceptions **propagate** to
the web layer for type-based mapping — which is exactly what Rule 1 ("C")
does. The alternative of modeling *every* expected outcome as a `Result`
would force services to catch domain exceptions and re-express them as
`Result` cases, building two parallel error vocabularies. Rule 1 avoids that
by letting transverse/invariant failures stay exceptions.

## Consequences

### Positive

- Consistent, documented rule for every use case's outcome shape.
- Type-based HTTP mapping; no fragile message parsing.
- The domain self-documents its rules through named exceptions.
- Multi-tenant "not found → 404" enforced by a single application exception.
- `Result` reserved for cases where it genuinely adds value (rich, data-
  carrying outcomes), avoiding ceremony on simple use cases.

### Negative

- More exception classes than a single `IllegalArgumentException` approach;
  requires discipline to keep the hierarchy parsimonious.
- The boundary "expected vs unexpected" depends on which invariants are
  mirrored in DTO validation, which the team must keep in mind.

### Neutral

- Domain invariant violations that are *domain-only* (IBAN rules) are thrown
  by the domain and translated to `400` at the web edge via
  `@ExceptionHandler`. Those that are *mirrored* (blank name) stay
  `IllegalArgumentException` and, if they reach the service, indicate a bug.
- Sealed `Result` types will appear first in Block 3 (camt.053 import).

## Alternatives Considered

### Option A — model every expected outcome as a sealed `Result`

Faithful to CLAUDE.md's "Result Modeling" to the letter, but on `account`'s
simple use cases it produces several thin `Result` types (including a
transverse `NotFound` case repeated across use cases). Combined with a fine
exception hierarchy it forces catch-and-convert, duplicating the error
vocabulary. Rejected as ceremony without payoff here.

### Option B — exceptions only, no `Result`

Lightweight and idiomatic Spring, but routes *every* expected business
outcome (including data-carrying ones) through the exception control flow,
losing compiler-checked exhaustiveness and the ability to carry structured
outcome data cleanly. Rejected as a poor fit for the richer use cases coming
in Block 3.

### Option 1 — generic JDK exceptions for domain invariants

`IllegalArgumentException` / `IllegalStateException` only. Zero ceremony, and
the argument/state split already encodes 400 vs 409, but two distinct
argument violations share one type, forcing message parsing for field-level
`400` responses. Rejected in favor of named exceptions for a finance API
where error-response quality matters.

### `sealed DomainException`

Rejected: sealing across the shared kernel and the bounded contexts would
invert the dependency direction (a layering violation), and it provides no
benefit since exceptions are not exhaustively matched.

## References

- CLAUDE.md — "Result Modeling", "Multi-tenancy", "Security Requirements"
- ADR-0026 — Ports in the application layer
- ADR-0016 — ArchUnit architectural fitness tests (domain must stay framework-free)

# ADR-0001: Use Architecture Decision Records

## Status

Accepted

## Context

The Pecunia project is designed as a portfolio-grade engineering demonstration
in addition to a personal tool. Significant architectural decisions will be
made throughout its lifecycle. Without a structured way to capture these
decisions, the rationale behind them is easily lost — both for the author
returning to the codebase months later and for external reviewers exploring
the repository.

Common alternatives for capturing decisions include:
- Wiki pages (separate from the code, drift over time)
- Code comments (scattered, hard to discover, no historical view)
- Verbal/informal records (lost entirely)
- Pull request descriptions (buried in Git history, no central index)

None of these are satisfactory for a portfolio project where decisions must
be discoverable, justified, and immutable.

## Decision

The project adopts **Architecture Decision Records (ADRs)** as defined by
Michael Nygard. Each significant architectural decision is captured in a
short Markdown document under `docs/adr/`, numbered sequentially.

ADRs follow a standard template (see `docs/adr/template.md`):
- Status (Proposed, Accepted, Deprecated, Superseded)
- Context
- Decision
- Consequences (positive, negative, neutral)
- Alternatives considered
- References

ADRs are **immutable** once accepted. If a decision needs to change, a new
ADR is created that supersedes the previous one. The previous ADR is updated
only to reference its successor.

An ADR is created when a decision:
- Affects the structure of the codebase
- Involves choosing between non-trivial technical alternatives
- Has long-term consequences difficult to reverse
- Is likely to be questioned by future readers

Trivial decisions (e.g., choice of a specific utility library, formatting
preferences) do not require an ADR.

## Consequences

### Positive

- Decisions are documented at the time they are made, not reconstructed
  later.
- The rationale is explicit and reviewable, supporting interview discussions.
- New readers (including the author after a break) can understand the
  codebase's evolution.
- ADRs serve as a self-imposed discipline: writing an ADR forces deliberation.

### Negative

- Maintaining ADRs requires effort and consistency.
- Risk of either over-documenting (ADR for every minor choice) or
  under-documenting (skipping ADRs under time pressure).

### Neutral

- ADRs are immutable, requiring a new ADR to overturn an old decision. This
  is intentional but means the `docs/adr/` directory grows monotonically.

## Alternatives Considered

### Wiki or external documentation

Rejected because:
- Drifts from the codebase over time.
- Not versioned with the code.
- Harder to discover when exploring the repository.

### Inline code comments

Rejected because:
- Scattered across files.
- No central index.
- Often deleted during refactoring.

### No formal record

Rejected because:
- The project's portfolio purpose requires defensible, traceable decisions.
- Memory fades; rationale must outlive the moment.

## References

- Michael Nygard, "Documenting Architecture Decisions" (2011):
  https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions
- ADR GitHub organization: https://adr.github.io/

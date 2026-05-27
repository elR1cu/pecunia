# Project Overview

## Vision

Pecunia is a personal finance application designed around one core question:

> "How much can I really save each month?"

The application imports Swiss bank transactions in the ISO 20022 camt.053
format, categorizes them, and computes a realistic savings capacity based on
recurring income, fixed charges, and rolling averages of variable spending.

Pecunia is not a generic budgeting tool. It is built for **a single Swiss
banking customer** (the author) and optimized for that use case, with the
discipline of production-grade engineering applied throughout.

## Purpose

Pecunia is built with three explicit, prioritized objectives:

1. **CV / portfolio asset** — primary objective.
   Demonstrate production-grade engineering practices to support a job search
   targeting Senior Backend Java positions at Swiss private banks and financial services
   companies. Application target: late October / early November 2026.

2. **Daily personal tool** — secondary objective.
   Once deployed, the application is used daily by the author to manage real
   finances. This forces realistic feature design and prevents over-engineering.

3. **Structured learning** — supporting objective.
   The project is an opportunity to deepen and demonstrate expertise across the
   full modern Java backend stack and adjacent disciplines (Angular, Keycloak,
   observability, DevOps).

These objectives are intentionally ordered. When trade-offs arise, the CV/
portfolio dimension takes precedence — meaning production-grade code quality,
public documentation, and thoughtful architecture matter more than rapid
feature delivery.

## Target Audience

This project is built for and by one user. However, the **repository itself**
has three audiences:

- **Recruiters and hiring managers** at Swiss private banks and financial services
  companies, evaluating the author's engineering practices through the public
  repository.
- **Technical interviewers** (senior engineers, architects, tech leads) who
  may explore the codebase, architecture decisions, and documentation in
  depth as part of an interview process.
- **The author** as an evolving knowledge base, journal, and reference for
  technologies practiced during the project.

The codebase, commits, and documentation are written with these audiences in
mind from day one.

## Functional Scope (MVP)

The MVP is the version targeted for end of October 2026. It is intentionally
narrow.

### What the MVP does

- **Authenticated access** via Keycloak (OAuth2/OIDC, BFF pattern). The MVP is
  used by a single real user in production, but the architecture, data model, and
  security are designed for multi-user operation from day one. Adding new users
  requires no architectural change.
- **Manages two Swiss bank accounts**: one UBS current account in CHF and one
  UBS Visa credit card.
- **Imports camt.053 XML files** exported from UBS e-banking, with
  deduplication of previously imported transactions.
- **Categorizes transactions** manually through a hierarchical category system
  (e.g., Food > Groceries, Transport > Train).
- **Visualizes spending** in a dashboard: total spent this month, comparison
  with previous month, distribution by category, account balances.
- **Tracks transactions** in a paginated, filterable list with edit
  capabilities.
- **Distinguishes** between expenses, income, and internal transfers (the
  monthly Visa payment is a transfer, not an expense).

### What the MVP does NOT do

The following features are intentionally excluded from the MVP scope. They
are either deferred to post-MVP blocks or out of scope entirely.

**Deferred to post-MVP** (planned for after October 2026):

- Budgets per category with progress tracking
- Savings capacity computation and savings goals
- Simulation features ("if I reduce X by 20%, I save Y")
- Proactive alerts (budget thresholds, anomalies)
- End-of-month forecasting
- AI-assisted categorization (Anthropic Claude integration)
- Recurring transaction detection
- Categorization rules engine
- Full observability stack (Prometheus, Grafana)
- k3d Kubernetes demo environment

**Out of scope entirely** (not planned):

- Multi-currency support beyond CHF
- Multi-user *features* such as family sharing, joint accounts, or any form of data
  sharing between users (the architecture supports multiple isolated users; what is
  out of scope is letting users share data with each other)
- Investment tracking (stocks, funds, third pillar)
- Loan, mortgage, or debt management
- Direct Open Banking integration (Salt Edge, GoCardless, bLink)
- Email, SMS, or push notifications (alerts are in-app only)
- Native mobile applications (PWA covers mobile usage)
- AWS or any cloud-provider-specific deployment (cost-prohibitive for personal
  use; k3d demo covers the cloud-native architecture story)

## Non-Functional Requirements

These requirements apply to the MVP and beyond. They are not optional.

### Security

- **Authentication and authorization required** on every endpoint by default.
- **Ownership checks**: a user may only access their own data, enforced at
  the application layer.
- **No secrets in the codebase**: all credentials and secrets are loaded from
  environment variables or a secret store.
- **No sensitive data in logs**: IBANs, full transaction amounts, and personal
  identifiers are sanitized or masked.
- **TLS everywhere**, including local development (via mkcert).
- **CSRF protection** active (the BFF pattern uses session cookies).
- **Bean Validation** on all DTOs to prevent invalid input.
- **SQL injection prevention** via JPA / parameterized queries only.

### Compliance

- **GDPR / Swiss nLPD** alignment: the application must provide:
  - An endpoint to export all user data in a machine-readable format
  - An endpoint to delete the user's account and all associated data
  - Clear data retention policies documented in `docs/architecture.md`

### Quality

- **Code style enforced**: Spotless (Java), ESLint + Prettier (TypeScript).
- **80%+ unit test coverage** on business code (services, domain logic).
- **Integration tests with Testcontainers** for repository and adapter layers.
- **CI must pass** before any merge to `main`.
- **Conventional Commits** strictly enforced.

### Documentation

- **README** kept up to date with current state.
- **ADR for every significant architectural decision.**
- **Architecture diagrams** versioned in the repository (Mermaid or PlantUML).
- **Public, comprehensive, English-only documentation.**

### Performance

The MVP has trivial performance requirements (single user, ~100 transactions
per month). However, code should not introduce obvious anti-patterns:

- No N+1 queries in JPA
- Indexed columns for all frequent filter criteria
- Pagination on any list that may grow
- Reasonable resource limits in Kubernetes manifests

## Constraints

### Time

- **Available capacity**: limited part-time work, with variability.
- **MVP deadline**: end of October 2026 (approximately 22 weeks from project start
  in early June 2026).
- **Effort budgeting**: scope is calibrated to the available time. Quality and learning
- take precedence over feature breadth.

### Budget

- **Cloud infrastructure**: kept minimal. Production runs on a single VPS in
  Switzerland or EU (Hetzner Cloud or Infomaniak), targeted at under
  CHF 15 / month.
- **AWS deployment**: explicitly out of scope (cost-prohibitive for sustained
  personal use; replaced by a local k3d demo for cloud-native architecture).
- **Tooling**: free or already-owned licenses (IntelliJ IDEA Ultimate,
  Anthropic Claude Pro, Hyperskill, Udemy).

### Skill calibration

The author is a Java backend engineer with intermediate familiarity with the
Spring ecosystem. The frontend stack (Angular, TypeScript) is new ground.
The learning curve is factored into the roadmap, with Angular and TypeScript
introduced gradually.

See [`docs/learning-plan.md`](learning-plan.md) for the detailed approach.

## Success Criteria

The project will be considered successful if **all** of the following are
true by the end of October 2026:

1. The MVP scope (see above) is fully implemented and deployed publicly.
2. The repository is public on GitHub with a clean commit history.
3. Documentation is complete: README, architecture, ADRs, roadmap,
   domain model.
4. CI/CD is green on `main`, with automated tests and quality checks.
5. The author uses the application daily for personal finance management.
6. The repository can be confidently shown to a technical interviewer at a Swiss private
   bank or financial services company as a demonstration of the author's engineering
   practices.

Post-MVP success is measured continuously: the application gets richer
features (budgets, AI, observability, k3d demo), and the engineering practices
deepen accordingly.

## Risks and Mitigations

### Risk: Loss of motivation

Personal projects often die at month four. The author has explicitly named
this risk.

**Mitigations**:
- Block-based delivery: every block produces a visible, satisfying result.
- Daily usefulness reached as early as Block 3 (camt.053 import).
- Public repository: visibility creates positive pressure.
- Work journal (`JOURNAL.md`) to make progress tangible.

### Risk: Scope creep

The author is naturally inclined to add interesting technologies. The
deadline-driven scope discipline is critical.

**Mitigations**:
- Explicit out-of-scope list in this document and in `CLAUDE.md`.
- Post-MVP backlog documented separately.
- AI collaboration guidelines instruct Claude not to suggest out-of-scope
  features during MVP work.

### Risk: Quality regression under deadline pressure

The author has stated that code quality and learning are non-negotiable.

**Mitigations**:
- Quality gates in CI (tests, linting, code style).
- Mentor-style code reviews via Claude before any merge.
- Block-based delivery: prefer cutting scope to cutting quality.

### Risk: Technology decisions made too quickly

A wrong early architectural decision can be costly.

**Mitigations**:
- Trade-off-first decision protocol with Claude (options first, then choice,
  then justification).
- ADR for every significant decision (forces deliberation).
- Hexagonal architecture isolates technology choices behind ports, allowing
  replacement without domain rewrite (e.g., Spring Events → Kafka).

## Project Status

The project is in the **planning and bootstrap phase**.

The current effort focuses on:
- Project documentation and ADRs (this document, architecture, ADRs, roadmap)
- Repository setup and infrastructure (CLAUDE.md, README, scaffolding)
- Reactivation of Spring Boot fundamentals (Block 0)

Active development begins with Block 1.

# Roadmap

Pecunia is built block by block. Each block delivers either user-visible
value or a foundational capability. The MVP scope corresponds to blocks 0
through 4, plus block 8 (deployment).

For the underlying rationale and constraints, see
[`project-overview.md`](project-overview.md).

## Scope Strategy

The MVP is intentionally narrow. The goal is to ship a deployed, working,
publicly demonstrable application by **end of October 2026**, then
continuously enrich it during the job search period.

Post-MVP blocks are part of the long-term plan but explicitly **out of scope
for the deadline**.

## Block Sequence

The blocks are ordered to maximize motivation and demonstration value:

1. Foundations and language/framework reactivation.
2. Secured skeleton ready for development.
3. Core domain model.
4. The first delightful moment: importing real bank data.
5. **Production deployment**: a live URL to put on the CV.
6. Visualization that makes the imported data useful.
7. Post-MVP: budgets, savings, AI, observability, k3d demo.

## MVP Blocks

### Block 0 — Foundations and Reactivation

**Goal**: prepare the engineering environment and refresh Spring Boot
fundamentals after a months-long pause.

**Deliverables**:
- Local development environment ready: IntelliJ IDEA Ultimate, VS Code,
  Docker, Git, Maven.
- Public GitHub repository created with the documentation written so far
  (CLAUDE.md, README, ADRs, this roadmap, domain model).
- A small disposable Spring Boot 4 application built from scratch to
  reactivate Spring fundamentals (not part of Pecunia, just a warm-up).
- Brief familiarization with Java 25 language features (records, sealed
  interfaces, pattern matching, virtual threads).

**Exit criteria**: the author feels confident to start Block 1 and the
repository is publicly visible with initial documentation.

### Block 1 — Secured Skeleton

**Goal**: build a deployable skeleton with end-to-end authentication, ready
to receive features.

**Deliverables**:
- Repository scaffolding: `apps/api`, `apps/frontend`, `contracts`,
  `deploy/docker-compose`, `docs/`.
- `docker-compose.dev.yml` with PostgreSQL, Keycloak, and Redis.
- Keycloak realm exported and versioned in the repository.
- Spring Boot 4 backend: minimal endpoints, Flyway initialized, Spring
  Security with BFF/OIDC configuration against Keycloak, Redis-backed
  sessions.
- Angular frontend: Angular Material set up, basic routing, login redirect
  to Keycloak, authenticated landing page showing the user's display name.
- OpenAPI specification skeleton in `contracts/openapi.yaml`.
- OpenAPI generation wired into both backend (Maven plugin) and frontend
  (npm script).
- GitHub Actions CI: build, test, lint on every push.
- Conventional Commits enforced (commitlint).
- **SonarCloud integration**: project setup with Quality Gates enforced
  on Pull Requests. Badge displayed in README.
- **Structured JSON logging**: Spring Boot 4 native structured logging
  (`logging.structured.format.console=logstash`) with MDC correlation IDs
  via Micrometer Tracing (OpenTelemetry bridge, no exporter yet) and
  sanitization patterns for financial data. See ADR-0018.
- **Frontend internationalization**: ngx-translate v18 wired with runtime
  language switching (EN/FR/DE/IT, English fallback) and a persisted
  language choice. Application chrome only, not user data. See ADR-0025.

**Exit criteria**: the user can log in via Keycloak, the session cookie
works end to end, and a protected endpoint returns the user's identity.

### Block 2 — Domain Model

**Goal**: model and persist the core entities (accounts, categories) with
their UI.

**Deliverables**:
- Hexagonal architecture in place: domain/application/web/infrastructure
  packages per bounded context (`account`, `category`).
- Domain entities: `Account`, `Category` with value objects (`Money`,
  `CategoryPath`) and ports.
- JPA adapters and Flyway migrations.
- Use cases: `OpenAccount`, `ArchiveAccount`, `ListAccounts`,
  `CreateCategory`, `MoveCategoryToParent`, `ArchiveCategory`,
  `ListCategories`.
- REST endpoints generated from the OpenAPI spec, implemented in
  controllers.
- Angular UI: accounts management page, hierarchical category management
  page.
- Unit tests on domain logic (written by the author).
- Integration tests on adapters (generated on request).
- **ArchUnit setup**: introduce architectural fitness tests in CI to
  enforce hexagonal boundaries. Initial test set: domain isolation
  (no Spring, no JPA, no Lombok), bounded contexts independence, web
  layer cannot access infrastructure directly.

**Exit criteria**: the user can register UBS Current and UBS Visa accounts
and create a category hierarchy through the UI.

### Block 3 — camt.053 Import

**Goal**: the application becomes truly useful — real bank data flows in.

**Deliverables**:
- camt.053 parser as an infrastructure adapter implementing a
  `BankStatementParser` port.
- Deduplication logic based on `EndToEndId` or computed hash.
- Use cases: `ParseCamt053File`, `ConfirmCamt053Import`.
- REST endpoint for file upload with multipart support.
- Angular UI: drag-and-drop upload zone, preview screen with statistics
  (new, duplicates), confirmation flow.
- Sample anonymized camt.053 files in `samples/` for testing.

**Exit criteria**: the user successfully imports a real camt.053 file from
UBS e-banking; transactions are persisted and de-duplicated.

### Block 8 — Production Deployment

**Goal**: bring the application online with a public URL, ready to be put
on the CV.

**Note**: Block 8 is intentionally executed before Blocks 4-7 in the MVP
sequence. Having a live URL early enables the CV value to materialize
quickly.

**Deliverables**:
- VPS provisioned with Hetzner Cloud or Infomaniak (one of the two,
  decided when the block starts).
- Domain name registered (e.g., `pecunia.example.ch`).
- Docker Compose production configuration: reverse proxy with TLS (Caddy
  or Traefik with Let's Encrypt), application container, PostgreSQL,
  Keycloak, Redis.
- Encrypted off-site backup of PostgreSQL (Backblaze B2 or equivalent).
- GitHub Actions deployment job: on merge to `main`, build images, push,
  deploy over SSH.
- Supply-chain security, extending the Trivy/Dependabot baseline added in
  Block 1:
  - Container image scanning (`trivy image`) of the built application image,
    plus Dockerfile/IaC misconfiguration scanning (`trivy` `misconfig`).
  - SBOM generation (CycloneDX) for both backend and frontend: backend via
    the `cyclonedx-maven-plugin` (declared explicitly with a pinned version,
    since the imported BOM does not contribute its `pluginManagement`),
    frontend via `@cyclonedx/cyclonedx-npm`. Distributed through controlled
    channels (CI artifact, attached to the GitHub release), **not** exposed
    over `/actuator/sbom`, to avoid disclosing exact dependency versions to
    the network. Enables incident response and continuous monitoring of
    released artifacts against future CVEs (e.g. via Dependency-Track). May
    warrant a short ADR.
  - Add Dependabot's `docker` ecosystem for the production compose/Dockerfile
    image tags (postgres, keycloak, redis).
- Basic Ansible playbook for VPS provisioning (Docker installation,
  firewall, fail2ban, automatic security updates).
- Updated README with the live URL and a short demo description.
- Recorded 2-3 minute demo video (Loom or YouTube) embedded in the README.

**Exit criteria**: the application is reachable at a public URL, secured
with TLS, with working authentication and the user can log in and use the
features available so far (Block 1, 2, 3).

### Block 4 — Transaction Visualization

**Goal**: make the imported data visible and useful.

**Deliverables**:
- Domain entity: `Transaction` with value objects (`Money`,
  `TransactionKind`).
- JPA adapter, Flyway migration, repository port.
- Use cases: `ListTransactions` (paginated, filtered),
  `GetTransactionDetails`, `CategorizeTransaction`, `TagTransaction`,
  `UntagTransaction`, `MarkTransactionAsTransfer`.
- REST endpoints for transactions.
- Angular UI:
  - Transactions list page with filters (account, category, date range,
    amount, status, free text), pagination, sorting.
  - Transaction detail modal with inline editing.
  - Bulk categorization assistant for unclassified transactions.
- Dashboard page:
  - KPI cards: total balance, total spent this month, comparison to
    previous month.
  - Donut chart: spending breakdown by category for the current month.
  - Recent transactions panel.

**Exit criteria**: the user opens the application daily to check spending
and categorize new transactions.

## MVP Completion

At the end of Block 4 (or alternatively, end of Block 8 if executed last),
the MVP is **complete**:

- The application is publicly deployed with a live URL.
- The user uses it daily for personal finance management.
- The repository is public on GitHub with clean documentation, CI, ADRs,
  and Conventional Commits history.
- The project can be presented in interviews as a portfolio piece.

The MVP is the **floor**, not the ceiling. From this point, post-MVP blocks
enrich the application continuously during the job search.

## Post-MVP Blocks

Post-MVP blocks are implemented opportunistically after the MVP deadline,
in the priority order below. Each block adds visible value to the
application and additional depth to the CV.

### Block 5 — Budgets and Savings Capacity

**Goal**: the application answers the question "how much can I save this
month?".

**Deliverables**:
- `Budget` bounded context: domain, persistence, use cases.
- Monthly budgets per category with progress tracking.
- Savings capacity computation (see formula in `domain-model.md`).
- Savings goals with target amount, target date, and suggested monthly
  contribution.
- Simulation feature: "if I reduce category X by Y%, my savings would be
  Z".
- Angular UI: budgets page, savings page, simulation interactions.

### Block 9 — Observability

**Goal**: production-grade visibility into the running application.

**Deliverables**:
- Spring Boot Actuator endpoints exposed (`/actuator/health`,
  `/actuator/metrics`, `/actuator/prometheus`).
- Micrometer metrics: business KPIs (imports per day, categorization
  rate, etc.) in addition to JVM defaults.
- Structured JSON logging with correlation IDs.
- Grafana dashboards (local or Grafana Cloud free tier).
- Alerting on basic conditions (application down, error rate spike).

### Block 6 — AI Categorization

**Goal**: automatic categorization with high accuracy.

**Deliverables**:
- `CategorizationRule` entity: user-defined rules (e.g., "if counterparty
  contains MIGROS, category = Groceries").
- Automatic application of rules during import.
- Integration with the Anthropic Claude API for transactions not matched
  by rules.
- "Suggested categorization" workflow: AI proposes, user accepts or
  corrects, the system learns from corrections.
- Angular UI: rules management page, AI suggestion validation inline in
  the transactions list.

### Block 7 — Recurring Transactions and Forecasting

**Goal**: anticipate fixed charges and forecast end-of-month balance.

**Deliverables**:
- `RecurringExpense` entity: declared manually or detected automatically.
- Detection algorithm: transactions of similar amount and counterparty
  occurring at regular intervals.
- End-of-month forecasting based on current pace.
- Angular UI: recurring expenses page, forecast widget on the dashboard.

### Block 10 — k3d Kubernetes Demo

**Goal**: demonstrate cloud-native deployment skills without cloud cost.

**Deliverables**:
- Helm charts for the Pecunia application stack.
- k3d cluster setup scripts.
- Kubernetes manifests with probes, resource limits, HPA, NetworkPolicies.
- Ingress controller with TLS.
- Observability stack deployed in the cluster (Prometheus, Grafana).
- Documentation: "5-minute demo" runbook for interview presentations.

### Block 11 — Performance and Resilience Engineering

**Goal**: learn and demonstrate load testing, soak testing, and chaos
engineering — a coherent, CV-grade story for a Senior Backend role in
financial services.

**Dependencies**: builds on Block 9 (Observability). Load and chaos results
are only meaningful when metrics and dashboards are in place to interpret
them. Infrastructure-level chaos additionally depends on Block 10 (k3d).

**Deliverables**:
- Load tests with **Gatling** (JVM-native, widely used in Swiss banking):
  scenarios for the heavy endpoints (camt.053 import, paginated/filtered
  transaction listing). Run on demand and/or nightly in CI.
- **Soak / long-running tests**: the same scenarios sustained over hours to
  surface memory leaks and connection-pool exhaustion, observed through the
  Block 9 metrics.
- **Application-level chaos** with **Chaos Monkey for Spring Boot**: inject
  latency, exceptions, and bean kills to validate graceful degradation.
- **Dependency/network fault injection** with **Toxiproxy**: simulate
  PostgreSQL/Redis latency and outages. Ties into the Resilience4j adoption
  planned at Block 6.
- **Infrastructure chaos** on the k3d cluster (**Chaos Mesh** or
  **LitmusChaos**): pod kills validating probes, HPA, and self-healing.
- Documented performance baseline and target SLOs (latency percentiles,
  throughput). A short ADR records the tooling choice.

### Future considerations

The following are noted but not formally planned:

- Native mobile applications (PWA covers mobile usage).
- Migration of eventing to Kafka or Redpanda.
- Multi-currency support.
- Investment tracking module.
- Direct Open Banking integration when feasible for individuals in
  Switzerland.
- AWS deployment as a dedicated mini-project.

## Technology Adoption Triggers

Some technologies have been deliberately deferred for adoption based on
concrete need rather than upfront commitment. This avoids dependency
bloat and ensures every adopted library earns its place.

### Spring Modulith

Spring Modulith is the official Spring toolkit for building modular
monoliths. Pecunia is architecturally aligned with what Modulith offers
(boundary verification, persisted application events, module
documentation). However, Modulith is not adopted in the MVP.

Adoption will be considered when one of these signals appears:

- Boundary verification through ArchUnit becomes cumbersome or
  repetitive.
- Lost events cause a tangible bug (durability becomes a real need).
- Module documentation becomes a manual chore that gets neglected.
- A specific Modulith feature (e.g., per-module integration tests,
  module observability) becomes valuable.

If adopted, the introduction will be documented in a dedicated ADR
explaining the trigger and the migration path. The hexagonal port
design (`DomainEventPublisher`) makes the eventing migration
non-invasive.

### Resilience4j

Resilience4j provides circuit breakers, retries, rate limiters, and
related patterns for resilient external service calls. The MVP has no
external HTTP service calls (Keycloak, PostgreSQL, and Redis are
accessed via specialized clients with their own resilience mechanisms),
so Resilience4j is not adopted.

Adoption is planned for Block 6 (AI categorization with the Anthropic
API), where it becomes clearly necessary:

- **Circuit Breaker** to handle API outages gracefully
- **Retry** with exponential backoff for rate-limited requests
- **Time Limiter** to bound the wait on slow responses

A dedicated ADR will be written at adoption time.

### Application-level caching

Redis is in the stack for session storage (ADR-0011), but no application-
level caching is configured in the MVP. The single-user scale makes
caching unnecessary, and premature caching introduces consistency
complexity without measurable benefit.

Adoption will be considered when:

- The Anthropic API is integrated (Block 6): cache categorization
  suggestions to reduce API costs and latency.
- Savings capacity computation becomes noticeable in dashboard load
  time (Block 5): cache the computation with invalidation on new
  transactions.
- Any specific query or computation is identified as a bottleneck.

When introduced, Redis is the backend (consistency with existing stack).
A dedicated ADR will be written at adoption time.

### Other deferred technologies

- **Kafka**: ADR-0008 covers this. Migration when scaling needs justify.
- **NgRx**: planned only if Angular state complexity grows substantially.
- **AWS**: ADR-0010 covers this. Out of scope; k3d local demo replaces.

### Periodic review

At the end of every 2 blocks, the author conducts a 2-minute review:
"Has any adoption signal appeared during these blocks?" This protects
against the bias of having already decided and no longer looking.

## Tracking Progress

Progress on blocks is tracked in `JOURNAL.md` (work journal) and GitHub
issues. Each block exit is marked by a Git tag (`block-1-complete`, etc.)
and a brief journal entry.

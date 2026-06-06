# Pecunia

[![Build Status](https://github.com/elR1cu/pecunia/actions/workflows/ci.yml/badge.svg)](https://github.com/elR1cu/pecunia/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-green.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-latest-red.svg)](https://angular.io/)

> Personal budget management for Swiss bank accounts.
> Import camt.053 statements, track expenses, plan your savings.

**🌐 Live demo:** _TODO: link will be added after Block 8 (deployment)_

---

## Why Pecunia?

Pecunia answers a simple question: **"How much can I really save each month?"**

Most budgeting apps either oversimplify (ignoring the difference between fixed
charges and variable spending) or overcomplicate (asking for too much manual
input). Pecunia takes a pragmatic approach designed for Swiss banking:

- **Import camt.053 files** directly from UBS e-banking (ISO 20022 standard)
- **Categorize automatically** using rules and AI-assisted suggestions
- **Compute savings capacity** from real income, fixed charges, and rolling
  average of variable expenses
- **Forecast end-of-month balance** based on current spending pace
- **Track savings goals** with suggested monthly contributions

---

## Project Context

Pecunia is a personal project built by a Java backend engineer to:

1. Solve a real personal need (knowing how much can be saved each month)
2. Demonstrate professional-grade engineering practices in a public portfolio
3. Deepen expertise in modern Java/Spring stack, Angular, and DevOps practices

The codebase follows production-grade conventions: hexagonal architecture,
schema-first API design, security by default, comprehensive testing, ADRs for
every significant decision.

This is **work in progress** — see the [roadmap](docs/roadmap.md) for details.

---

## Screenshots

_TODO: screenshots will be added at the end of Block 4 (dashboard + transactions UI)._

---

## Tech Stack

### Backend
- **Java 25** with modern language features (records, sealed interfaces,
  pattern matching, virtual threads)
- **Spring Boot 4** with Spring Security 7 (OAuth2 Client / BFF pattern)
- **Spring Data JPA** with Hibernate
- **PostgreSQL 18** as the primary datastore
- **Redis** for distributed session storage (BFF pattern)
- **Flyway** for database migrations
- **MapStruct** for type-safe object mapping
- **JUnit 6**, **AssertJ**, **Mockito**, **Testcontainers** for testing
- **Jakarta Bean Validation 3.x** for input validation
- **OpenAPI Generator** (Maven plugin) for schema-first API design

### Frontend
- **Angular** (latest) with **Angular Material**
- **TypeScript** (strict mode)
- **Signals** for component state, **RxJS** for async flows
- **PWA** support for mobile usage
- **OpenAPI Generator** (CLI) for typed API clients

### Identity & Security
- **Keycloak** as OIDC Identity Provider
- **Backend-for-Frontend (BFF)** pattern: backend manages tokens, frontend uses
  HttpOnly session cookies (no JWT in the browser)
- HTTPS everywhere (including local dev with mkcert)
- CSRF protection, sanitized logging, GDPR/nLPD compliance built-in

### DevOps & Infrastructure
- **Docker** & **Docker Compose** for local development
- **k3d** for local Kubernetes (learning environment and interview demo)
- **Helm** for Kubernetes deployment
- **GitHub Actions** for CI/CD
- Production hosting on Swiss/EU VPS (Hetzner or Infomaniak)
- **Spring Boot Actuator** + **Micrometer** + **Prometheus** + **Grafana** for
  observability

### AI (planned)
- **Anthropic Claude API** for transaction auto-categorization and a finance
  chatbot

---

## Architecture

Pecunia follows **Hexagonal Architecture** (Ports & Adapters) with **Domain-
Driven Design** principles. The application is a **modular monolith** with
clear internal boundaries — ready for extraction to microservices if needed,
but not prematurely fragmented.

Key architectural decisions are documented in
[Architecture Decision Records](docs/adr/).

### High-level diagram

_TODO: Mermaid diagram will be added in `docs/architecture.md` and embedded here._

### Highlights

- **Schema-first API design**: a single `contracts/openapi.yaml` is the source
  of truth. Backend DTOs and controller interfaces are generated, as is the
  typed Angular client.
- **BFF authentication**: no tokens leak to the browser. The Angular SPA talks
  to the Spring Boot backend over a session cookie; the backend handles the
  OIDC flow with Keycloak and stores tokens in a Redis-backed session.
- **Domain events via a port**: business events are published through a
  `DomainEventPublisher` port, implemented today by Spring's
  `ApplicationEventPublisher`. The port is designed to swap in a Kafka adapter
  without touching domain code.
- **Typed results, not exceptions for expected failures**: sealed interfaces
  with record implementations model expected business outcomes (e.g.,
  `ImportResult.Success` vs `ImportResult.Failure`).

See [`docs/architecture.md`](docs/architecture.md) for the full picture.

---

## Repository Structure

```
pecunia/
├── apps/
│   ├── api/                  # Spring Boot backend
│   └── frontend/             # Angular frontend
├── contracts/
│   └── openapi.yaml          # OpenAPI specification (source of truth)
├── deploy/
│   ├── docker-compose/       # Local development infrastructure
│   ├── helm/                 # Kubernetes deployment charts
│   └── ansible/              # VPS provisioning
├── docs/
│   ├── adr/                  # Architecture Decision Records
│   ├── architecture.md       # Architecture overview
│   ├── domain-model.md       # Business model and concepts
│   ├── project-overview.md   # Project vision and scope
│   ├── roadmap.md            # Block-based roadmap
│   ├── learning-plan.md      # Learning resources per technology
│   └── ui-mockups.md         # UI wireframes and Figma links
├── samples/                  # Sample (anonymized) camt.053 files
├── .github/workflows/        # CI/CD pipelines
├── CLAUDE.md                 # AI collaboration guidelines
├── README.md                 # This file
└── JOURNAL.md                # Work journal
```

---

## Getting Started

### Prerequisites

- Java 25 (Temurin or any OpenJDK distribution)
- Node.js 22+ and npm
- Docker and Docker Compose
- Maven 3.9+ (or use the wrapper `./mvnw`)
- (Optional) k3d for local Kubernetes

### Local development

The backend uses `spring-boot-docker-compose` to start PostgreSQL, Keycloak
and Redis automatically — no manual `docker compose up` is required.

```bash
# 1. Clone
git clone https://github.com/elR1cu/pecunia.git
cd pecunia

# 2. Copy the local environment file
cp apps/api/.env.example apps/api/.env

# 3. Start the backend (Docker Desktop must be running)
mvn -f apps/api/pom.xml spring-boot:run

# 4. (Once Block 1 step 6 is delivered) start the frontend
cd apps/frontend
npm install
npm start
```

Always run from the **repository root** so the relative paths in
`application.yml` resolve correctly. See
[`docs/dev-setup.md`](docs/dev-setup.md) for the detailed guide
(IntelliJ Run Configuration, sanity checks, troubleshooting).

### Running tests

```bash
# Backend unit + integration tests
cd apps/api
./mvnw test

# Frontend unit tests
cd apps/frontend
npm test

# E2E tests (Playwright)
cd apps/frontend
npm run e2e
```

---

## Roadmap

Pecunia is built block by block. Each block delivers user-visible value or
foundational capability. See [`docs/roadmap.md`](docs/roadmap.md) for the
detailed plan.

**MVP target: end of October 2026.**

| Block | Theme | Status |
|-------|-------|--------|
| 0 | Foundations & Java/Spring refresh | _Planned_ |
| 1 | Secured skeleton (Keycloak + BFF) | _Planned_ |
| 2 | Domain model: accounts & categories | _Planned_ |
| 3 | camt.053 import | _Planned_ |
| 8 | Deployment on Swiss VPS | _Planned_ |
| 4 | Transactions visualization & dashboard | _Planned_ |
| 5 | Budgets & savings capacity | _Post-MVP_ |
| 9 | Observability | _Post-MVP_ |
| 6 | AI categorization | _Post-MVP_ |
| 7 | Recurring transactions & forecasting | _Post-MVP_ |
| 10 | k3d Kubernetes demo | _Post-MVP_ |

---

## Engineering Practices

This project demonstrates production-grade engineering practices:

- **Schema-first API design** (OpenAPI as source of truth)
- **Hexagonal architecture** with strict module boundaries
- **Comprehensive testing**: unit (JUnit 6), integration (Testcontainers),
  E2E (Playwright)
- **Security by default**: BFF pattern, ownership checks, sanitized logging,
  TLS-everywhere
- **CI/CD**: every commit builds, tests, and lints; releases are versioned
- **Conventional Commits** for clean Git history
- **Architecture Decision Records** documenting every significant choice
- **Code quality**: Spotless (Java), ESLint + Prettier (TypeScript)

---

## Documentation

- [Project Overview](docs/project-overview.md) — vision, scope, target audience
- [Architecture](docs/architecture.md) — technical design and trade-offs
- [Domain Model](docs/domain-model.md) — business concepts and entities
- [Roadmap](docs/roadmap.md) — block-by-block plan
- [Local Development Setup](docs/dev-setup.md) — running the backend on your machine
- [Learning Plan](docs/learning-plan.md) — resources used during the project
- [UI Mockups](docs/ui-mockups.md) — wireframes and Figma references
- [ADRs](docs/adr/) — Architecture Decision Records

---

## Demo Video

_TODO: a 2-3 minute walkthrough will be linked here after Block 8._

---

## Contributing

Pecunia is primarily a personal learning project. Contributions are not
actively sought, but feedback, suggestions, and discussions are welcome via
GitHub Issues.

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE)
file for details.

---

## Acknowledgments

- ISO 20022 standard and UBS for the camt.053 specification
- The Spring Boot, Angular, and Keycloak open-source communities
- All the authors of the books, courses, and articles referenced in
  [`docs/learning-plan.md`](docs/learning-plan.md)

# Learning Plan

This document captures the technologies used in Pecunia, the author's
current skill level on each, the resources used to learn or deepen each,
and the sequencing of learning relative to the project blocks.

This is a living document: as learning progresses and resources prove
more or less useful, entries are updated.

## Approach

The author values learning depth over breadth. Each technology in the
stack is studied with enough depth to use it confidently and to explain
its trade-offs in an interview setting. The chosen approach mixes:

- **Just-in-time learning** for technologies needed at a specific block.
- **Anticipated learning** for foundational topics (Spring Security,
  hexagonal architecture, OAuth2) where understanding before coding
  avoids costly missteps.

## Self-assessment (May 2026)

The author's starting skill level on each technology, on a scale from 1
(never used) to 5 (could teach it):

| Technology | Level | Notes |
|------------|-------|-------|
| Java (modern features) | 4 | Missing Java 17+ specifics: records, sealed, pattern matching, virtual threads |
| Spring Boot | 3 | Solid foundation, paused for 6 months |
| Spring Security | 3 | Familiar with basics; OAuth2/OIDC less practiced |
| JPA / Hibernate | 2 | Knows the basics; performance and advanced features need work |
| PostgreSQL | 2 | Comfortable with queries; indexes, EXPLAIN, RLS less so |
| Kafka | 1 | Conceptual familiarity only |
| Docker | 2 | Basics in place; multi-stage builds, networking, healthchecks less so |
| Kubernetes | 2 | Conceptual familiarity; hands-on practice needed |
| Angular | 1 | Has not used it |
| TypeScript | 1 | Knows JavaScript; needs the TypeScript layer |
| OAuth2 / OIDC | 2 | Conceptual understanding; needs practical depth |
| Keycloak | 1 | Never used directly |
| Terraform | 1 | Conceptual only |
| Ansible | 2 | Some prior exposure |
| AWS | 1 | Conceptual only |
| GitHub Actions | 2 | Basic workflows; advanced patterns less so |
| Observability (Prom/Grafana) | 2 | Basics in place |
| Architecture (DDD, hexagonal) | 2 | Conceptual; needs practice |
| Tests (unit, integration, E2E) | 3 | Comfortable; Testcontainers and Playwright less so |
| AI / LLM integration | 1 | New ground |

## Learning Resources by Technology

For each technology below: the recommended resources, estimated time
investment, and the project block in which the learning becomes
relevant.

### Java 17+ language features (target level: 5)

**Resources**:
- *Modern Java in Action* by Raoul-Gabriel Urma, Mario Fusco, Alan Mycroft (book). The reference for modern Java features.
- Nicolai Parlog's YouTube channel and articles (nipafx.dev). Best source for recent features (records, sealed, pattern matching, virtual threads, structured concurrency).
- Oracle's "Inside Java" podcast.
- JEPs in JDK 25 page on openjdk.org for the specifics of Java 25.

**Focus areas**: records, sealed interfaces, pattern matching for switch, virtual threads, structured concurrency.

**When**: Block 0 (foundations).

**Estimated time**: 15–20 hours, spread across the project.

### Spring Boot 4 (target level: 4)

**Resources**:
- Official Spring Boot 4 documentation (`docs.spring.io`). Primary reference.
- Spring blog (`spring.io/blog`) for release notes and migration guides.
- Marco Behler's video courses (`marcobehler.com`). Practitioner-oriented, no fluff.
- Dan Vega's YouTube channel for shorter explanatory videos.
- Eazy Bytes courses on Udemy (when updated for Boot 4) — currently Boot 3, but 90% transferable.

**Focus areas**: Spring Boot 4 specifics (configuration changes from Boot 3, virtual threads integration, new actuator features).

**When**: Block 0 reactivation, then continuously.

**Estimated time**: 10–15 hours.

### Spring Security with OAuth2/OIDC and BFF pattern (target level: 4)

**Resources**:
- *Spring Security in Action, 2nd Edition* by Laurentiu Spilca (book, Manning).
- Eazy Bytes "Spring Security 6 Zero to Master along with JWT, OAuth2" on Udemy (Boot 3 but 95% applicable).
- Jérôme Wacongne (ch4mpy) articles on Baeldung and his `spring-addons` GitHub repository. The best practical reference for BFF + Keycloak in Spring.
- Curity Identity Server's articles on the BFF pattern.
- OWASP Cheat Sheet "OAuth 2.0 for Browser-Based Apps".

**Focus areas**: BFF pattern, session-based authentication, CSRF, OIDC flows, Keycloak integration.

**When**: Block 1 (secured skeleton). Critical foundation.

**Estimated time**: 20–25 hours.

### JPA / Hibernate (target level: 4)

**Resources**:
- Vlad Mihalcea's blog (`vladmihalcea.com`). The reference for performance.
- *High-Performance Java Persistence* by Vlad Mihalcea (book) for in-depth.
- Bharath Thippireddy's "Spring Data JPA using Hibernate" on Udemy for the basics.
- Hypersistence Optimizer (Vlad's tool) for detecting common mistakes — useful as a learning aid.

**Focus areas**: transaction management, lazy vs eager loading, N+1 detection, fetch joins, EntityGraph, projections, optimistic locking, batch operations.

**When**: Block 2 (domain model and persistence).

**Estimated time**: 15–20 hours.

### PostgreSQL (target level: 4)

**Resources**:
- *PostgreSQL: Up and Running* (3rd ed.) by Regina Obe and Leo Hsu (book, O'Reilly).
- Official PostgreSQL documentation for indexes, EXPLAIN, Row-Level Security.
- pganalyze blog (`pganalyze.com`) for advanced topics.
- "Mastering PostgreSQL" YouTube playlists.

**Focus areas**: index types (B-tree, GIN, BRIN, partial, expression), EXPLAIN/EXPLAIN ANALYZE, transaction isolation levels, CTEs and window functions, Row-Level Security for multi-tenancy.

**When**: Block 2 onwards, with RLS becoming important in the security hardening block (post-MVP).

**Estimated time**: 15 hours.

### Docker (target level: 3)

**Resources**:
- *Docker Deep Dive* by Nigel Poulton (book, regularly updated).
- TechWorld with Nana's Docker tutorial on YouTube (free, excellent).
- Docker's official documentation for specific topics.

**Focus areas**: multi-stage builds, Dockerfile optimization, networking, healthchecks, security best practices.

**When**: Block 1 onwards.

**Estimated time**: 5–8 hours.

### Kubernetes with k3d (target level: 3–4)

**Resources**:
- TechWorld with Nana's Kubernetes tutorial on YouTube (the best free intro).
- *Kubernetes Up & Running* (3rd ed.) by Kelsey Hightower et al. (book, O'Reilly).
- KodeKloud's "Kubernetes for the Absolute Beginners — Hands-on" on Udemy for hands-on practice.
- KodeKloud's CKAD course if pushing toward certification.
- Official k3d documentation (`k3d.io`).
- Helm's official documentation.

**Focus areas**: deployments, services, ingress, ConfigMaps, secrets, probes, resource limits, HPA, Helm.

**When**: Block 10 (post-MVP), with optional early exposure.

**Estimated time**: 20–25 hours.

### Angular (target level: 3)

**Resources**:
- Maximilian Schwarzmüller's "Angular - The Complete Guide" on Udemy. The reference.
- Joshua Morony's YouTube channel for modern Angular (Signals-first approach).
- Official Angular documentation for Signals and the current best practices.

**Focus areas**: components, services, dependency injection, routing, reactive forms, HTTP client, Signals, RxJS integration, PWA setup, Angular Material.

**When**: Block 1 onwards. The largest single time investment.

**Estimated time**: 30–35 hours.

### TypeScript (target level: 3)

**Resources**:
- *TypeScript Deep Dive* by Basarat Ali Syed (free online book).
- Maximilian Schwarzmüller's "Understanding TypeScript" on Udemy (companion to his Angular course).
- Matt Pocock's "Total TypeScript" content (totaltypescript.com) for advanced types.

**Focus areas**: strict mode, generics, type narrowing, utility types, discriminated unions.

**When**: In parallel with Angular learning.

**Estimated time**: 10–15 hours.

### OAuth2 / OIDC (target level: 4)

**Resources**:
- *OAuth 2 in Action* by Justin Richer and Antonio Sanso (book, Manning).
- Curity's articles on OAuth flows and security patterns (`curity.io/resources/learn`).
- Auth0's blog for practical guides.
- Okta's "OAuth 2.0 and OpenID Connect (in plain English)" YouTube video.

**Focus areas**: Authorization Code + PKCE, BFF pattern, token introspection, JWT vs opaque tokens, refresh token rotation, security considerations.

**When**: Block 1 (secured skeleton).

**Estimated time**: 10 hours.

### Keycloak (target level: 3)

**Resources**:
- Official Keycloak documentation. Surprisingly comprehensive.
- *Keycloak - Identity and Access Management for Modern Applications* by Stian Thorgersen and Pedro Igor Silva (book, Packt). Written by Keycloak's creator.
- Eazy Bytes' microservices security course on Udemy includes Keycloak.

**Focus areas**: realm configuration, client registration (confidential vs public), roles and groups, realm export/import for reproducibility.

**When**: Block 1 (secured skeleton).

**Estimated time**: 8–10 hours.

### Architecture (Hexagonal, DDD) (target level: 4)

**Resources**:
- *Get Your Hands Dirty on Clean Architecture* by Tom Hombergs (book). Practical hexagonal architecture in Spring Boot.
- *Domain-Driven Design Distilled* by Vaughn Vernon (short book).
- *Implementing Domain-Driven Design* by Vaughn Vernon (longer reference).
- Martin Fowler's `martinfowler.com` for foundational articles.
- Alistair Cockburn's original hexagonal architecture article.

**Focus areas**: ports and adapters, bounded contexts, aggregates, value objects, domain events.

**When**: Block 0 (foundations) and continuously throughout the project.

**Estimated time**: 20–30 hours, spread across the project.

### Testing (target level: 4)

**Resources**:
- *Effective Software Testing* by Maurício Aniche (book, Manning). Modern, practical, complete.
- Testcontainers official documentation and YouTube channel.
- Philip Riecks' blog (`rieckpil.de`) for Spring Boot testing patterns.
- Playwright official documentation.

**Focus areas**: testing pyramid in practice, Testcontainers for integration tests, Playwright for E2E, contract testing concepts.

**When**: Continuously from Block 1.

**Estimated time**: 10 hours additional to existing knowledge.

### Observability (target level: 3)

**Resources**:
- *Observability Engineering* by Charity Majors, Liz Fong-Jones, George Miranda (book, O'Reilly). Concepts.
- Spring Boot Actuator and Micrometer official documentation.
- OpenTelemetry documentation (`opentelemetry.io`).
- Grafana Labs' tutorials.

**Focus areas**: Micrometer, structured logging with correlation IDs, basic OpenTelemetry, Prometheus + Grafana setup.

**When**: Block 9 (post-MVP observability).

**Estimated time**: 10–15 hours.

### AI / LLM integration (target level: 3)

**Resources**:
- Anthropic's official documentation (`docs.claude.com`).
- Anthropic's free prompt engineering course (`anthropic.skilljar.com`).
- Anthropic Java SDK documentation on GitHub.

**Focus areas**: API basics, tool use (function calling), prompt engineering for categorization, controlling output structure.

**When**: Block 6 (post-MVP AI categorization).

**Estimated time**: 8–10 hours.

### Git, Conventional Commits, GitHub Actions (target level: 3)

**Resources**:
- Conventional Commits specification (`conventionalcommits.org`).
- "Pro Git" book (free online).
- GitHub Actions official documentation.
- Maximilian Schwarzmüller's "GitHub Actions - The Complete Guide" on Udemy.

**Focus areas**: rebase workflows, squash and merge, conventional commits enforcement, GitHub Actions paths filters and matrix builds.

**When**: Block 0 (setup) and continuously.

**Estimated time**: 5–8 hours.

## Total Estimated Learning Time

Approximately **190–220 hours** of learning, spread across the project
duration. This is in addition to development time.

## Tracking Learning Progress

The author maintains brief notes in `JOURNAL.md` about completed
learning sessions, resources consulted, and key takeaways. This helps:

- Avoid re-reading the same content unnecessarily.
- Identify patterns of what types of resources work best.
- Demonstrate continuous learning when discussed in interviews.

# ADR-0002: Monorepo over Multi-repo

## Status

Accepted

## Context

Pecunia has multiple deliverables: a Spring Boot backend, an Angular
frontend, infrastructure manifests (Docker Compose, Helm charts, Ansible
playbooks), and shared contracts (OpenAPI specification). These components
must evolve together and remain consistent.

Two organizational options were considered:

1. **Multi-repo**: separate Git repositories for backend, frontend, and
   infrastructure.
2. **Monorepo**: a single Git repository containing all components, organized
   into subdirectories.

The choice influences day-to-day workflow, CI/CD configuration, onboarding,
and how external reviewers (recruiters, interviewers) experience the project.

## Decision

Pecunia uses a **monorepo**. All components — backend, frontend, contracts,
infrastructure, and documentation — live in a single Git repository
(`pecunia`).

Top-level structure:

```
pecunia/
├── apps/
│   ├── api/                  # Spring Boot backend
│   └── frontend/             # Angular frontend
├── contracts/
│   └── openapi.yaml          # API contract, source of truth
├── deploy/                   # Infrastructure (compose, Helm, Ansible)
├── docs/                     # Documentation, ADRs
└── samples/                  # Sample data files
```

## Consequences

### Positive

- **Single source of truth**: one `git clone` retrieves the entire project.
- **Atomic cross-cutting changes**: a single commit can update the API
  contract, backend implementation, and frontend consumer simultaneously.
- **Simpler contract synchronization**: the OpenAPI specification lives at a
  natural location accessible to both backend and frontend builds.
- **Easier discovery**: a recruiter exploring the repo sees the entire system
  at once, without hunting for related repositories.
- **One CI/CD configuration**: a single GitHub Actions workflow orchestrates
  the entire build.
- **Lower onboarding friction**: one README, one set of conventions, one
  setup procedure.

### Negative

- **CI complexity**: the workflow must conditionally build only the affected
  components on each commit (using path filters in GitHub Actions).
- **Repository size**: grows faster than individual repos, though the Pecunia
  scope keeps this manageable.
- **Risk of unwanted coupling**: developers may be tempted to introduce
  shortcuts across modules. This is mitigated by the strict hexagonal
  architecture and clear module boundaries.

### Neutral

- **Versioning**: all components share a single version, which matches the
  monolithic deployment model.

## Alternatives Considered

### Multi-repo (three repositories: backend, frontend, infrastructure)

Rejected because:
- Synchronizing API contract changes across repositories adds significant
  friction (multi-repo pull requests, mismatched merges).
- Project setup requires cloning three repositories and following three
  README files.
- For a single-author project, the organizational overhead exceeds the
  benefit.
- Recruiters exploring the project would have to navigate multiple
  repositories, reducing the clarity of the demonstration.

### Multi-repo with a shared contract repository

Rejected for similar reasons. Adds a fourth repository to manage and
synchronize. The complexity is justified for large organizations with
independent teams; it is not justified here.

## References

- "Monorepo vs Multi-repo: A Comparative Analysis":
  https://medium.com/@brennanwilkes/monorepos-vs-multi-repos-9b2e7e7e2e91
- Google's monorepo at scale (informational):
  https://research.google/pubs/why-google-stores-billions-of-lines-of-code-in-a-single-repository/

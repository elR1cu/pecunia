# ADR-0012: Conventional Commits and Git Workflow

## Status

Accepted

## Context

Git history is a primary artifact of the project, especially for a public
portfolio repository. A disciplined history communicates professional
practice; an unstructured history undermines the impression.

Three aspects need explicit decisions:

1. **Commit message format**: free-form vs structured.
2. **Branch model**: single-branch development vs feature branches with
   pull requests.
3. **Release tagging**: ad-hoc vs semantic versioning.

## Decision

Pecunia adopts:

1. **Conventional Commits** for all commit messages.
2. **Feature branch workflow** with pull requests, even for a single-author
   project.
3. **Semantic Versioning** for release tags.

### Conventional Commits

All commits follow the format:

```
<type>(<scope>): <description>
[optional body]
[optional footer(s)]
```

Allowed types:
- `feat`: new feature
- `fix`: bug fix
- `docs`: documentation changes
- `refactor`: code restructuring without behavior change
- `test`: adding or modifying tests
- `chore`: dependency updates, configuration changes
- `ci`: CI/CD pipeline changes
- `perf`: performance improvements
- `style`: code style changes (whitespace, formatting)
- `build`: build system or dependency changes

Breaking changes are indicated with `!` after the type/scope or with a
`BREAKING CHANGE:` footer.

Examples:
- `feat(transaction): add camt.053 import endpoint`
- `fix(auth): correct CSRF token rotation on session refresh`
- `docs(adr): add ADR-0008 on event publishing`
- `refactor(category)!: rename Category to TransactionCategory`

### Feature branch workflow

- The `main` branch is always in a deployable state.
- All work happens in feature branches named `feat/<short-description>`,
  `fix/<short-description>`, etc.
- Branches are merged to `main` via Pull Request using **squash and merge**.
- Feature branches are **rebased** onto `main` before merge (no merge commits in
  feature branches).
- The squash commit message on `main` follows Conventional Commits format.
- The resulting `main` history is **linear**: one commit per merged PR, no merge
  commits.
- Even when working alone, the PR provides a review checkpoint and a
  visible artifact of completed work.

### Semantic Versioning

Releases are tagged using semantic versioning (`v<major>.<minor>.<patch>`):

- **Major**: breaking changes to the public API or significant user-facing
  changes.
- **Minor**: backwards-compatible new features.
- **Patch**: backwards-compatible bug fixes.

The MVP targets `v0.x.y` until production-ready, then `v1.0.0`.

## Consequences

### Positive

- **Clean, readable history**: commits communicate intent at a glance.
- **Automated changelog generation**: tools like `git-cliff` or
  `release-please` can generate changelogs from Conventional Commits.
- **Pull request discipline**: each PR is a unit of completed work,
  documented and reviewable.
- **Professional signal**: recruiters exploring the repo see the practices
  expected in mature engineering teams.
- **Release clarity**: semantic versioning communicates the impact of each
  release.
- **Linear, readable history on** `main`: every commit on `main` represents a
  complete, deliberate change, equivalent to a changelog entry.
- **Local commit noise is absorbed**: in-progress commits, fixups, and review
  iterations disappear into the final squash commit.

### Negative

- **Overhead**: writing structured commit messages and creating PRs for
  single-developer work adds time.
- **Discipline required**: the temptation to commit directly to `main`
  must be resisted.
- **Granularity discipline required**: a single squash commit per PR forces smaller,
  focused PRs. Large multi-topic PRs lose useful history.
- **Rebase requires care**: rebasing a shared branch is dangerous. The rule "rebase
  only your own unmerged branches" must be respected.

### Neutral

- **Tooling support**: commitlint or similar tools can enforce
  Conventional Commits at the local level (pre-commit hook).

## Alternatives Considered

### Merge commits (default GitHub behavior)

Rejected because:
- The history of `main` becomes cluttered with both feature commits and
  merge commits.
- `git log` becomes harder to read as a deliberate, curated changelog.
- Reverting a feature requires reverting multiple commits.

### Rebase and merge (no squash)

Considered. Preserves the individual commits of a PR on `main`. Rejected
because:
- Local commits often include work-in-progress noise that pollutes the
  main history.
- Squashing forces deliberate commit messages on `main`, aligning with
  Conventional Commits discipline.

### Squash without rebase first

Possible but suboptimal. Rebasing before merge ensures the feature branch
is conflict-free at merge time and that the squash applies cleanly.

### Free-form commit messages

Rejected because:
- History becomes opaque, especially when revisited months later.
- Cannot generate changelogs automatically.
- Sends a weak signal to external readers.

### Trunk-based development (no feature branches)

Considered for solo work. Rejected because:
- The portfolio demonstration benefits from visible PRs.
- Feature branches separate work-in-progress from production-ready code.
- The added overhead is minor compared to the demonstration value.

## References

- Conventional Commits specification: https://www.conventionalcommits.org/
- Semantic Versioning: https://semver.org/
- "How to Write a Git Commit Message" by Chris Beams:
  https://cbea.ms/git-commit/

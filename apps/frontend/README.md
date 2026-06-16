# Pecunia — Frontend

Angular 22 SPA for the Pecunia personal-budget management application,
consuming the Spring Boot BFF over session cookies.

See the project root [`README.md`](../../README.md) for the overall
vision and [`docs/dev-setup.md`](../../docs/dev-setup.md) for the full
local-development guide.

## Prerequisites

- **Node** 24.16.0 (Krypton LTS) — pinned in [`.nvmrc`](.nvmrc).
  Use [`nvm`](https://github.com/nvm-sh/nvm) to switch:
  ```bash
  nvm use
  ```
- **npm** 11.x (bundled with Node 24.16.0).

## Common commands

Run from `apps/frontend/`:

```bash
npm install           # install dependencies (first time, or after a lock change)
npm start             # ng serve on http://localhost:4200/
npm run build         # production build into dist/pecunia-frontend/
npm test              # unit tests (Vitest + jsdom)
```

## Stack

- Angular 22, standalone components, zoneless change detection.
- TypeScript 6 (strict by default).
- SCSS, Angular Material *(to be added)*.
- HTTP through the generated OpenAPI client *(to be added — single source
  of truth in [`contracts/openapi.yaml`](../../contracts/openapi.yaml))*.
- Vitest for unit tests.

## Architecture

See [`docs/architecture.md`](../../docs/architecture.md) — section
"Frontend Architecture" — for the target structure under `src/app/`
(core / features / shared) and the state-management approach
(Signals + RxJS, no NgRx in the MVP).

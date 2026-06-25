# End-to-End Tests (Playwright)

System-level tests that drive a real browser against the full Pecunia stack
(Angular dev server → BFF backend → Keycloak / Postgres / Redis). They live
here, at the repository root, rather than under `apps/frontend`, because they
exercise the whole system rather than the Angular app in isolation.

## Scope

One happy-path test closing **Block 1**: an anonymous visit to a protected
route → Keycloak login → authenticated dashboard. It asserts the three Block 1
exit criteria:

1. login via Keycloak,
2. an HttpOnly session cookie set by the BFF,
3. the protected `/api/me` endpoint returning the user identity.

Wiring this into CI is intentionally deferred — reliably booting Keycloak in CI
is the hard part. For now the suite runs locally, on demand.

## Prerequisites

1. **The full dev stack must already be running** (see `docs/dev-setup.md`):
   - Backend, from the repository root:
     `mvn -f apps/api/pom.xml spring-boot:run`
     (auto-starts Postgres, Keycloak and Redis via `spring-boot-docker-compose`).
   - Frontend: `cd apps/frontend && npm start` (serves http://localhost:4200).
2. **Node 24**: `nvm use`.
3. **Install dependencies and the browser binary**:
   ```bash
   cd e2e
   npm install
   npx playwright install chromium
   ```

## Running

The `testuser` password is stored only as an Argon2id hash in the realm
fixture, so it cannot be read from the repo. Provide the plaintext behind that
hash via an environment variable:

```bash
cd e2e
E2E_TEST_USER_PASSWORD='<testuser-password>' npm test
```

Other modes:

```bash
E2E_TEST_USER_PASSWORD='...' npm run test:headed   # watch the browser
E2E_TEST_USER_PASSWORD='...' npm run test:ui       # interactive UI mode
npm run report                                     # open the last HTML report
```

Without `E2E_TEST_USER_PASSWORD`, the test is **skipped** (not failed), so a
bare `npm test` stays green.

## Configuration knobs

- `E2E_BASE_URL` — frontend URL (default `http://localhost:4200`).
- `E2E_TEST_USER` — username (default `testuser`).

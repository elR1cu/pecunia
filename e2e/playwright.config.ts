import { defineConfig, devices } from '@playwright/test';

// The suite runs against an already-running local dev stack: the backend on
// :8080 (which auto-starts Postgres/Keycloak/Redis via spring-boot-docker-
// compose) and the Angular dev server on :4200 (proxying /api, /oauth2,
// /login, /logout to the backend). See README.md.
//
// No `webServer` is configured on purpose: reliably booting Keycloak is the
// hard part of wiring this into CI, which is deliberately deferred. For now
// the developer starts the stack, then runs the suite on demand.
export default defineConfig({
  testDir: './tests',
  // Auth flows touch shared server-side session state; keep runs deterministic.
  fullyParallel: false,
  // Fail a CI run that accidentally contains test.only (no-op locally for now).
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:4200',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});

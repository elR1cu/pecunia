import { test, expect } from '@playwright/test';

// The testuser password lives only as an Argon2id hash in the Keycloak realm
// fixture, so it cannot be derived from the repo. Provide the plaintext behind
// that hash via E2E_TEST_USER_PASSWORD. Username and base URL have local
// defaults (see playwright.config.ts and README.md).
const USERNAME = process.env.E2E_TEST_USER ?? 'testuser';
const PASSWORD = process.env.E2E_TEST_USER_PASSWORD;

test('logs in through Keycloak and reaches the authenticated dashboard', async ({ page }) => {
  test.skip(!PASSWORD, 'Set E2E_TEST_USER_PASSWORD to the testuser password to run this test.');

  // Exit criterion 1 — visiting a protected route while anonymous bounces to
  // Keycloak: the auth guard loads /api/me, gets a 401, and the error
  // interceptor redirects to /oauth2/authorization/pecunia, which the BFF
  // turns into a 302 to the Keycloak-hosted login form.
  await page.goto('/dashboard');
  await expect(page.locator('#username')).toBeVisible();
  expect(page.url()).toContain('/realms/pecunia/protocol/openid-connect/auth');

  // Authenticate on the Keycloak login form.
  await page.fill('#username', USERNAME);
  await page.fill('#password', PASSWORD!);
  await page.click('#kc-login');

  // Exit criterion 2 — the BFF completes the OIDC code exchange, sets the
  // HttpOnly SESSION cookie, and redirects to the post-login landing page.
  await page.waitForURL(/\/dashboard(\?.*)?$/);

  // Exit criterion 3 — the protected endpoint returns the user's identity. The
  // request shares the browser context's cookie jar, so the SESSION cookie is
  // sent automatically (no token held by the SPA — the BFF pattern).
  const meResponse = await page.request.get('/api/me');
  expect(meResponse.status()).toBe(200);
  const me = await meResponse.json();
  expect(me.username).toBe(USERNAME);

  // The dashboard renders the greeting with the resolved display name, proving
  // the identity round-trips all the way to the UI.
  await expect(page.getByText(me.displayName, { exact: false })).toBeVisible();
});

import { test, expect, type Page } from '@playwright/test';

// Same credential handling as auth.happy-path.spec.ts: the testuser password
// lives only as an Argon2id hash in the Keycloak realm fixture, so it is
// supplied out-of-band via E2E_TEST_USER_PASSWORD. Without it the test skips.
const USERNAME = process.env.E2E_TEST_USER ?? 'testuser';
const PASSWORD = process.env.E2E_TEST_USER_PASSWORD;

// A canonical, mod-97-valid Swiss IBAN (Wikipedia's reference example). The
// domain enforces ISO 7064, so an invalid IBAN would be rejected with a 400.
// There is no uniqueness constraint on IBAN, so reusing it across runs is fine;
// the per-run account `name` below is what disambiguates the created card.
const VALID_CH_IBAN = 'CH9300762011623852957';

/**
 * Drives the real BFF login flow, mirroring auth.happy-path.spec.ts: an
 * anonymous visit to a protected route bounces to Keycloak, we authenticate,
 * and the BFF redirects back to the dashboard with the SESSION cookie set.
 */
async function login(page: Page): Promise<void> {
  await page.goto('/dashboard');
  await expect(page.locator('#username')).toBeVisible();
  await page.fill('#username', USERNAME);
  await page.fill('#password', PASSWORD!);
  await page.click('#kc-login');
  await page.waitForURL(/\/dashboard(\?.*)?$/);
}

test('opens a current account and archives it through the UI', async ({ page }) => {
  test.skip(!PASSWORD, 'Set E2E_TEST_USER_PASSWORD to the testuser password to run this test.');

  // A unique name makes the account we create identifiable among any accounts
  // left over from previous runs (the local stack keeps a persistent database).
  const accountName = `E2E Current ${Date.now()}`;

  await login(page);

  // Navigate to the accounts page via the dashboard link (exercises the lazy
  // /accounts route the way a user reaches it).
  await page.getByRole('link', { name: 'My accounts' }).click();
  await page.waitForURL(/\/accounts$/);

  // --- Open account use case: fill the create dialog and submit -------------
  await page.getByRole('button', { name: 'New account' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();

  // Type is a MatSelect; its options render in a CDK overlay at the body level.
  await dialog.locator('mat-select[formcontrolname="type"]').click();
  await page.getByRole('option', { name: 'Current account' }).click();

  await dialog.locator('input[formcontrolname="name"]').fill(accountName);
  await dialog.locator('input[formcontrolname="iban"]').fill(VALID_CH_IBAN);
  await dialog.locator('input[formcontrolname="amount"]').fill('1000.00');

  await dialog.getByRole('button', { name: 'Create' }).click();

  // On a 201 the dialog closes and the new account is appended to the list.
  await expect(dialog).toBeHidden();

  // Scope every assertion to the card we just created.
  const card = page.locator('mat-card', { hasText: accountName });
  await expect(card).toBeVisible();
  await expect(card).toContainText('Current account');
  await expect(card).toContainText(VALID_CH_IBAN);
  await expect(card).toContainText('Active');

  // --- Archive account use case: confirm the dialog and check the status ----
  await card.getByRole('button', { name: 'Archive' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirm' }).click();

  // listAccounts returns archived accounts too, so the card stays visible but
  // flips to ARCHIVED and loses its (Active-only) Archive action.
  await expect(card).toContainText('Archived');
  await expect(card.getByRole('button', { name: 'Archive' })).toHaveCount(0);
});

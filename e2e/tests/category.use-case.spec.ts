import { test, expect, type Page, type Locator } from '@playwright/test';

// Same credential handling as auth.happy-path.spec.ts: the testuser password
// lives only as an Argon2id hash in the Keycloak realm fixture, so it is
// supplied out-of-band via E2E_TEST_USER_PASSWORD. Without it the test skips.
const USERNAME = process.env.E2E_TEST_USER ?? 'testuser';
const PASSWORD = process.env.E2E_TEST_USER_PASSWORD;

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

/** The tree row for a category, scoped by its (per-run unique) name. */
function row(scope: Locator, name: string): Locator {
  return scope.locator('.category-row', { hasText: name });
}

/** Opens the per-row actions menu and clicks one of its items. */
async function rowAction(page: Page, categoryRow: Locator, action: string): Promise<void> {
  // The actions button fades in on hover; hover first like a real user.
  await categoryRow.hover();
  await categoryRow.getByRole('button').click();
  await page.getByRole('menuitem', { name: action }).click();
}

test('creates, nests, moves, renames and archives categories through the UI', async ({ page }) => {
  test.skip(!PASSWORD, 'Set E2E_TEST_USER_PASSWORD to the testuser password to run this test.');

  // Unique names make the categories of this run identifiable among leftovers
  // from previous runs (the local stack keeps a persistent database).
  const parentName = `E2E Household ${Date.now()}`;
  const childName = `E2E Utilities ${Date.now()}`;
  const renamedChild = `${childName} (renamed)`;

  await login(page);

  // Navigate to the categories page via the dashboard link (exercises the
  // lazy /categories route the way a user reaches it).
  await page.getByRole('link', { name: 'My categories' }).click();
  await page.waitForURL(/\/categories$/);

  // The page is split into an Expenses and an Income section; everything in
  // this scenario happens on the Expenses side.
  const expenses = page.locator('.category-section', { hasText: 'Expenses' });

  // --- Create use case: a root expense category ----------------------------
  await expenses.getByRole('button', { name: 'New category' }).click();
  const createDialog = page.getByRole('dialog');
  await expect(createDialog).toBeVisible();
  await createDialog.locator('input[formcontrolname="name"]').fill(parentName);

  // Color defaults to the first preset; pick an icon like a real user would.
  await createDialog.getByRole('button', { name: 'restaurant' }).click();
  await createDialog.getByRole('button', { name: 'Create' }).click();

  // On a 201 the dialog closes and the tree is refetched with the new root.
  await expect(createDialog).toBeHidden();
  await expect(row(expenses, parentName)).toBeVisible();

  // --- Create use case: a child nested under the new root ------------------
  await expenses.getByRole('button', { name: 'New category' }).click();
  const childDialog = page.getByRole('dialog');
  await childDialog.locator('input[formcontrolname="name"]').fill(childName);
  await childDialog.locator('mat-select[formcontrolname="parentId"]').click();
  await page.getByRole('option', { name: parentName }).click();
  await childDialog.getByRole('button', { name: 'Create' }).click();
  await expect(childDialog).toBeHidden();

  // The child renders inside the parent's nested sub-tree. The parent's <li>
  // wraps its own row plus the nested <app-category-tree> with the children.
  const parentItem = expenses.locator('li').filter({ hasText: parentName });
  await expect(row(parentItem.locator('app-category-tree'), childName)).toBeVisible();

  // --- Move use case: promote the child back to the top level --------------
  await rowAction(page, row(expenses, childName), 'Move');
  const moveDialog = page.getByRole('dialog');
  await moveDialog.locator('mat-select').click();
  await page.getByRole('option', { name: 'Top level' }).click();
  await moveDialog.getByRole('button', { name: 'Move' }).click();
  await expect(moveDialog).toBeHidden();

  // After the refetch the parent has no sub-tree left and the child sits at
  // the top level of the section.
  await expect(parentItem.locator('app-category-tree')).toHaveCount(0);
  await expect(row(expenses, childName)).toBeVisible();

  // --- Update use case: rename through the edit dialog ---------------------
  await rowAction(page, row(expenses, childName), 'Edit');
  const editDialog = page.getByRole('dialog');
  await editDialog.locator('input[formcontrolname="name"]').fill(renamedChild);
  await editDialog.getByRole('button', { name: 'Save' }).click();
  await expect(editDialog).toBeHidden();
  await expect(row(expenses, renamedChild)).toBeVisible();

  // --- Archive use case: soft-delete keeps the row, drops the actions ------
  await rowAction(page, row(expenses, renamedChild), 'Archive');
  await page.getByRole('dialog').getByRole('button', { name: 'Confirm' }).click();

  // listCategories returns archived categories too: the row stays visible,
  // flagged as archived, and loses its actions menu.
  const archivedRow = row(expenses, renamedChild);
  await expect(archivedRow).toContainText('Archived');
  await expect(archivedRow.getByRole('button')).toHaveCount(0);
});

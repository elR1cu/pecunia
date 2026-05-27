# UI Mockups

This document captures the user interface designs for Pecunia. It evolves
through three phases:

1. **Low-fidelity wireframes** (current): textual descriptions of each
   screen's structure, components, and responsive behavior. Sufficient
   to guide initial development.
2. **High-fidelity mockups** (Figma): visual designs with the actual design
   system applied. Linked here once produced.
3. **Storybook component library** (Angular): living implementations of
   the design system, browsable as documentation. Linked here once
   produced.

The author is a backend engineer learning frontend craft as part of this
project. UI evolution is incremental and pragmatic: ship usable interfaces
first, refine appearance progressively.

## Design Principles

- **Clarity over decoration**: information density matters more than visual
  flourish for a personal finance tool.
- **Mobile-first thinking**: although desktop is the primary target, every
  screen must remain usable on a phone (PWA).
- **Accessibility from the start**: keyboard navigation, sufficient color
  contrast, semantic HTML, screen reader compatibility.
- **Angular Material as the foundation**: leverage the design system's
  components and theming rather than building from scratch.
- **Consistent navigation**: a persistent top bar with primary sections,
  contextual actions on each page.

## Design System (planned)

Once the application stabilizes, a formal design system is documented in
Figma and implemented in Storybook. Until then, the following defaults
apply:

- **Theme**: Angular Material default theme, lightly customized.
- **Primary color**: TBD (a neutral, professional tone — discussed before
  Block 4).
- **Typography**: Roboto (Angular Material default) for the MVP.
- **Spacing**: Angular Material's 8px grid.
- **Iconography**: Material Icons.

## Pages

The MVP includes the following screens. Detailed wireframes follow.

| Page | Block | Purpose |
|------|-------|---------|
| Login | 1 | Keycloak login redirection |
| Dashboard | 4 | Overview of finances |
| Transactions list | 4 | Browse and categorize transactions |
| Transaction details (modal) | 4 | Edit a single transaction |
| Accounts | 2 | Manage bank accounts |
| Categories | 2 | Manage transaction categories |
| camt.053 Import | 3 | Upload and confirm bank statement imports |
| Profile | (deferred) | User settings and data export |

Post-MVP pages: Budgets, Savings, Categorization Rules, Recurring
Transactions, Forecasts.

## Wireframe: Dashboard

The landing page after login. Provides an overview of the current
financial situation.

### Layout

```
+------------------------------------------------------------------+
| Pecunia      Dashboard  Transactions  Accounts  Categories  [👤] |
+------------------------------------------------------------------+
|                                                                  |
|  May 2026                                  [< Previous month >]  |
|                                                                  |
|  +----------------+  +----------------+                          |
|  | Total balance  |  | Spent this    |                          |
|  | 12,450.30 CHF  |  | month         |                          |
|  | (+2.3% vs M-1) |  | 3,287.50 CHF  |                          |
|  |                |  | (-12% vs M-1) |                          |
|  +----------------+  +----------------+                          |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | Spending by category (current month)                     |    |
|  |                                                           |    |
|  |  [Donut chart]   Housing      1,200 CHF (37%)            |    |
|  |                   Food         650 CHF (20%)             |    |
|  |                   Transport    250 CHF  (8%)             |    |
|  |                   Restaurants  180 CHF  (5%)             |    |
|  |                   Other ...                               |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | Recent transactions                          [See all >] |    |
|  |  23/05  MIGROS GENEVE        Food          -45.30 CHF   |    |
|  |  22/05  CFF                  Transport      -8.40 CHF   |    |
|  |  22/05  STARBUCKS            (uncategorized) -6.50 CHF   |    |
|  |  20/05  SALAIRE              Income     +8,500.00 CHF   |    |
|  +----------------------------------------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### Components

- **Top navigation bar**: app name, primary navigation, user avatar with
  dropdown (profile, logout).
- **Month selector**: shows the current month, allows navigating to past
  months (no future navigation for this view).
- **KPI cards** (MVP):
  - Total balance across all accounts, with month-over-month comparison.
  - Total spent this month (excluding transfers), with month-over-month
    comparison.
- **Category breakdown**: donut chart with a legend showing top
  categories by spending amount and percentage. Categories beyond the
  top 5 are grouped under "Other".
- **Recent transactions panel**: the 5 most recent transactions with
  inline category badge. Click navigates to the transactions page with
  the transaction selected.

### Post-MVP additions

- Additional KPI cards: savings capacity, end-of-month forecast.
- Alerts panel: budget warnings, anomaly notifications.
- Savings goals progress.

### Responsive behavior

- **Desktop**: layout as shown, two-column KPI cards.
- **Tablet**: KPI cards in 2x2 grid below the month selector.
- **Mobile**: single column, all sections stacked. Top navigation
  collapses to a hamburger menu.

## Wireframe: Transactions

Browse, filter, and categorize transactions.

### Layout

```
+------------------------------------------------------------------+
| Pecunia      [navigation]                                  [👤]  |
+------------------------------------------------------------------+
|                                                                  |
|  Transactions                            [+ Import camt.053]    |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | Filters                                            [v]   |    |
|  | Account [All v] Category [All v] Period [v] Status [v]  |    |
|  | [🔍 Search...]                                            |    |
|  | Active: Category="Restaurants" × Account="Visa" ×        |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  127 results                          Sort: [Date desc v]       |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | 23/05/2026  MIGROS GENEVE              -45.30 CHF        |    |
|  |             [Food > Groceries]              [Visa]       |    |
|  +----------------------------------------------------------+    |
|  | 22/05/2026  CFF                         -8.40 CHF        |    |
|  |             [Transport > Train]             [Current]    |    |
|  +----------------------------------------------------------+    |
|  | 22/05/2026  STARBUCKS                   -6.50 CHF        |    |
|  |             [⚠ Uncategorized]               [Visa]       |    |
|  +----------------------------------------------------------+    |
|  | 20/05/2026  SALAIRE                +8,500.00 CHF        |    |
|  |             [Income > Salary]               [Current]    |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  [< Previous]   Page 1 of 8   [Next >]                          |
|                                                                  |
+------------------------------------------------------------------+
```

### Components

- **Import button**: prominent CTA at the top right, navigates to the
  import flow.
- **Filters panel**: collapsible. Active filters shown as chips with
  remove buttons. Filters include:
  - Account (multi-select)
  - Category (multi-select)
  - Period (preset ranges + custom)
  - Status (categorized, uncategorized)
  - Amount range
  - Free-text search (matches description and counterparty)
- **Result count and sort**: results count and current sort order, with
  a dropdown to change sort.
- **Transaction rows**: each transaction shows date, counterparty,
  category (or "Uncategorized" badge), account, and amount (color-coded
  for income/expense).
- **Inline category assignment**: clicking the category badge opens an
  inline category picker for quick categorization.
- **Click on a row**: opens the transaction detail modal.
- **Pagination**: page-based navigation at the bottom.

### Responsive behavior

- **Mobile**: filters collapse into a bottom sheet. Rows simplify to show
  date and amount on one line, counterparty and category below.

## Wireframe: Transaction Detail (Modal)

Triggered by clicking a transaction row. Opens as a modal dialog on
desktop, full-screen view on mobile.

### Layout

```
+----------------------------------------------------------+
|  Transaction details                              [✕]    |
+----------------------------------------------------------+
|                                                          |
|  MIGROS GENEVE                          -45.30 CHF       |
|  23 May 2026 · Visa                                       |
|                                                          |
|  Category:    [Food > Groceries v]                      |
|  Tags:        [grocery] [+ Add tag]                     |
|  Description: [Grocery shopping at Migros________]      |
|                                                          |
|  Original description:                                   |
|  "ACHAT MIGROS GENEVE 23.05.2026 VISA"                  |
|                                                          |
|  Booking date: 23/05/2026                                |
|  Value date:   24/05/2026                                |
|  Reference:    PAINST20260523083014                      |
|                                                          |
|  [Mark as transfer]              [Save changes]          |
|                                                          |
+----------------------------------------------------------+
```

### Components

- Counterparty, amount, date, account summary at the top.
- Editable fields: category, tags, cleaned description.
- Read-only fields: original description, booking date, value date,
  external reference.
- Action button "Mark as transfer" for reclassifying internal transfers.
- Save/Cancel buttons at the bottom.

## Wireframe: Accounts

Manage bank accounts and credit cards.

### Layout

```
+------------------------------------------------------------------+
| Pecunia      [navigation]                                  [👤]  |
+------------------------------------------------------------------+
|                                                                  |
|  Accounts                                  [+ New account]      |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | UBS Current                              12,450.30 CHF   |    |
|  | Type: Current account | IBAN: CH** **** **** **** ****1  |    |
|  | [Edit]  [Archive]                                         |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | UBS Visa                                   -287.50 CHF   |    |
|  | Type: Credit card                                         |    |
|  | [Edit]  [Archive]                                         |    |
|  +----------------------------------------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### Components

- "New account" CTA opens a form modal: name, type, IBAN (if applicable),
  currency, initial balance.
- Each account card shows: name, current balance, type, masked IBAN if
  applicable.
- Edit and Archive actions per account.
- Archived accounts are listed in a separate section below, collapsed by
  default.

## Wireframe: Categories

Manage the hierarchical category tree.

### Layout

```
+------------------------------------------------------------------+
| Pecunia      [navigation]                                  [👤]  |
+------------------------------------------------------------------+
|                                                                  |
|  Categories                                [+ New category]     |
|                                                                  |
|  EXPENSES                                                        |
|  +----------------------------------------------------------+    |
|  | 🏠 Housing                                                |    |
|  |    └ Rent                                                 |    |
|  |    └ Utilities                                            |    |
|  |    └ Insurance                                            |    |
|  | 🛒 Food                                                   |    |
|  |    └ Groceries                                            |    |
|  |    └ Restaurants                                          |    |
|  | 🚆 Transport                                              |    |
|  |    └ Train                                                |    |
|  |    └ Public transport                                     |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  INCOME                                                          |
|  +----------------------------------------------------------+    |
|  | 💰 Salary                                                 |    |
|  | 🎁 Gifts                                                  |    |
|  | ↩️ Refunds                                                |    |
|  +----------------------------------------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### Components

- "New category" CTA opens a form: name, type (Expense/Income), parent
  category (optional), color, icon.
- Tree view: expandable nodes, drag-and-drop reordering (post-MVP), edit
  and archive actions per category.
- Separation by type (Expenses and Income) at the top level.

## Wireframe: camt.053 Import

Upload and import a bank statement.

### Initial state

```
+------------------------------------------------------------------+
|  Import a camt.053 statement                                     |
+------------------------------------------------------------------+
|                                                                  |
|  +----------------------------------------------------------+    |
|  |                                                          |    |
|  |              📁  Drag and drop your camt.053             |    |
|  |              file here, or click to browse               |    |
|  |                                                          |    |
|  |                       [Browse...]                        |    |
|  |                                                          |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  ℹ️ How to obtain a camt.053 file from UBS?                      |
|     1. Log in to UBS E-Banking.                                  |
|     2. Go to Payments > Report settings.                         |
|     3. Activate the camt.053 end-of-day report.                  |
|     4. Download the file for the desired period.                 |
|                                                                  |
+------------------------------------------------------------------+
```

### After upload, preview state

```
+------------------------------------------------------------------+
|  Import preview                                                  |
+------------------------------------------------------------------+
|                                                                  |
|  Detected account: CH** **** **** **** ****1 (UBS Current)      |
|  Period: 01/05/2026 → 23/05/2026                                 |
|  Transactions found: 47                                          |
|                                                                  |
|  ✓ 42 new transactions                                           |
|  ⚠ 5 duplicates detected (will be skipped)                      |
|                                                                  |
|  +----------------------------------------------------------+    |
|  | Transactions to import (preview)                          |    |
|  | ✓ 23/05  MIGROS GENEVE             -45.30 CHF             |    |
|  | ✓ 22/05  CFF                        -8.40 CHF             |    |
|  | ⚠ 22/05  STARBUCKS                  -6.50 CHF  (skip)     |    |
|  | ✓ 20/05  SALAIRE               +8,500.00 CHF              |    |
|  | ...                                                       |    |
|  +----------------------------------------------------------+    |
|                                                                  |
|  [Cancel]                    [Confirm import (42 transactions)] |
|                                                                  |
+------------------------------------------------------------------+
```

### Components

- Drag-and-drop zone with visual feedback on hover.
- Help section explaining how to obtain the file from UBS.
- After upload: a parsed summary with statistics, a scrollable preview
  list, and explicit confirm/cancel actions.
- Imported transactions are not persisted until the user clicks Confirm.

## Future Mockups

When the design system matures, this document is updated with:

- Links to Figma frames.
- Links to Storybook components.
- Screenshots of the deployed application.

Until then, this textual wireframe document serves as the authoritative
reference for UI structure.

import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

import { AccountResponse, AccountStatus, AccountType } from '../../generated/api';
import { AccountsState } from '../services/accounts-state';
import { NotificationService } from '../services/notification-service';
import { Accounts } from './accounts';

function anAccount(overrides: Partial<AccountResponse> = {}): AccountResponse {
  return {
    id: 'id-1',
    type: AccountType.Current,
    name: 'UBS Current',
    status: AccountStatus.Active,
    initialBalance: { amount: '1000.00', currency: 'CHF' },
    ...overrides,
  };
}

describe('Accounts', () => {
  let component: Accounts;
  let accountsState: {
    accounts: ReturnType<typeof signal<AccountResponse[]>>;
    load: ReturnType<typeof vi.fn>;
    archive: ReturnType<typeof vi.fn>;
  };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let notifications: { error: ReturnType<typeof vi.fn> };

  // openCreate()/archive() are protected: reach them through a loose handle
  const asAny = () =>
    component as unknown as { openCreate: () => void; archive: (a: AccountResponse) => void };

  beforeEach(() => {
    accountsState = {
      accounts: signal<AccountResponse[]>([]),
      load: vi.fn(),
      archive: vi.fn(() => of(undefined)),
    };
    dialog = { open: vi.fn() };
    notifications = { error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [Accounts],
      providers: [
        { provide: AccountsState, useValue: accountsState },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationService, useValue: notifications },
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
      ],
    });
    component = TestBed.createComponent(Accounts).componentInstance;
  });

  it('maps each account type to its statement-row icon', () => {
    const handle = component as unknown as { icon: (type: AccountType) => string };

    expect(handle.icon(AccountType.Current)).toBe('account_balance');
    expect(handle.icon(AccountType.Savings)).toBe('savings');
    expect(handle.icon(AccountType.CreditCard)).toBe('credit_card');
  });

  it('loads the accounts on init', () => {
    component.ngOnInit();
    expect(accountsState.load).toHaveBeenCalledOnce();
  });

  it('openCreate() opens the create dialog', () => {
    asAny().openCreate();
    expect(dialog.open).toHaveBeenCalledOnce();
  });

  it('archive() archives the account when the confirm dialog resolves true', () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(true) });

    asAny().archive(anAccount({ id: 'id-42' }));

    expect(accountsState.archive).toHaveBeenCalledWith('id-42');
  });

  it('archive() does nothing when the confirm dialog is dismissed', () => {
    dialog.open.mockReturnValue({ afterClosed: () => of(false) });

    asAny().archive(anAccount());

    expect(accountsState.archive).not.toHaveBeenCalled();
  });
});

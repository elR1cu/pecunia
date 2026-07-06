import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import {
  AccountResponse,
  AccountService,
  AccountStatus,
  AccountType,
  OpenAccountRequest,
} from '../../generated/api';
import { AccountsState } from './accounts-state';

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

describe('AccountsState', () => {
  let state: AccountsState;
  let api: {
    listAccounts: ReturnType<typeof vi.fn>;
    openAccount: ReturnType<typeof vi.fn>;
    archiveAccount: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    // fake AccountService: each method returns an observable we control
    api = {
      listAccounts: vi.fn(() => of([])),
      openAccount: vi.fn(() => of(anAccount())),
      archiveAccount: vi.fn(() => of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: AccountService, useValue: api }],
    });
    state = TestBed.inject(AccountsState);
  });

  it('load() populates the accounts signal from the API', () => {
    const accounts = [anAccount()];
    api.listAccounts.mockReturnValue(of(accounts));

    state.load();

    expect(state.accounts()).toEqual(accounts);
  });

  it('openAccount() appends the account returned by the API to the signal', () => {
    // given one account already loaded
    const existing = anAccount({ id: 'id-1' });
    api.listAccounts.mockReturnValue(of([existing]));
    state.load();

    // when a new one is created
    const created = anAccount({ id: 'id-2', name: 'UBS Visa' });
    api.openAccount.mockReturnValue(of(created));
    const request = { type: AccountType.Current } as OpenAccountRequest;
    state.openAccount(request).subscribe();

    // then it is posted and appended immutably (no refetch needed)
    expect(api.openAccount).toHaveBeenCalledWith({ openAccountRequest: request });
    expect(state.accounts()).toEqual([existing, created]);
  });

  it('archive() deletes then refetches the list (204 has no body)', () => {
    const afterArchive = [anAccount({ status: AccountStatus.Archived })];
    api.archiveAccount.mockReturnValue(of(undefined));
    api.listAccounts.mockReturnValue(of(afterArchive));

    state.archive('id-1').subscribe();

    expect(api.archiveAccount).toHaveBeenCalledWith({ accountId: 'id-1' });
    expect(state.accounts()).toEqual(afterArchive);
  });
});

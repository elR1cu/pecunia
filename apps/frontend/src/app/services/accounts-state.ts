import { inject, Service, signal } from '@angular/core';
import { AccountResponse, AccountService, OpenAccountRequest } from '../../generated/api';
import { Observable, tap } from 'rxjs';

@Service()
export class AccountsState {
  private readonly accountService = inject(AccountService);

  private readonly _accounts = signal<AccountResponse[]>([]);
  readonly accounts = this._accounts.asReadonly();

  load(): void {
    this.accountService.listAccounts().subscribe((list) => this._accounts.set(list));
  }

  openAccount(request: OpenAccountRequest): Observable<AccountResponse> {
    return this.accountService
      .openAccount({ openAccountRequest: request })
      .pipe(tap((created) => this._accounts.update((list) => [...list, created])));
  }

  archive(id: string): Observable<void> {
    return this.accountService.archiveAccount({ accountId: id }).pipe(tap(() => this.load()));
  }
}

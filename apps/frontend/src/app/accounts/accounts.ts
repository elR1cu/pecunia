import { Component, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { AccountsState } from '../services/accounts-state';
import { MatDialog } from '@angular/material/dialog';
import { OpenAccountDialog } from './open-account-dialog/open-account-dialog';
import { MatButton } from '@angular/material/button';
import { ConfirmDialog } from '../components/confirm-dialog/confirm-dialog';
import { AccountResponse, AccountStatus } from '../../generated/api';
import { NotificationService } from '../services/notification-service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-accounts',
  imports: [MatCardModule, MatButton, TranslatePipe],
  templateUrl: './accounts.html',
})
export class Accounts implements OnInit {
  private readonly accountsState = inject(AccountsState);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly translate = inject(TranslateService);
  protected readonly accounts = this.accountsState.accounts;
  protected readonly AccountStatus = AccountStatus;

  ngOnInit(): void {
    this.accountsState.load();
  }

  protected openCreate(): void {
    this.dialog.open(OpenAccountDialog);
  }

  protected archive(account: AccountResponse): void {
    const ref = this.dialog.open(ConfirmDialog, {
      data: {
        title: this.translate.instant('accounts.archiveConfirmTitle'),
        message: this.translate.instant('accounts.archiveConfirmMessage', { name: account.name }),
      },
    });
    ref.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.accountsState.archive(account.id).subscribe({
          error: (err) => this.notifications.error(err),
        });
      }
    });
  }
}

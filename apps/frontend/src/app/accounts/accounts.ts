import { Component, inject, OnInit } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AccountResponse, AccountStatus, AccountType } from '../../generated/api';
import { ConfirmDialog } from '../components/confirm-dialog/confirm-dialog';
import { AccountsState } from '../services/accounts-state';
import { NotificationService } from '../services/notification-service';
import { OpenAccountDialog } from './open-account-dialog/open-account-dialog';

const TYPE_ICONS: Record<AccountType, string> = {
  [AccountType.Current]: 'account_balance',
  [AccountType.Savings]: 'savings',
  [AccountType.CreditCard]: 'credit_card',
};

@Component({
  selector: 'app-accounts',
  imports: [MatCardModule, MatButton, MatIconModule, TranslatePipe],
  templateUrl: './accounts.html',
  styleUrl: './accounts.scss',
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

  protected icon(type: AccountType): string {
    return TYPE_ICONS[type];
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

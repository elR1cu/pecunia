import { Component, inject, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AccountType, OpenAccountRequest } from '../../../generated/api';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AccountsState } from '../../services/accounts-state';
import { NotificationService } from '../../services/notification-service';
import { TranslatePipe } from '@ngx-translate/core';

function ibanMatchesType(group: AbstractControl): ValidationErrors | null {
  const type = group.get('type')?.value;
  const iban = group.get('iban')?.value?.trim();
  if ((type === 'CURRENT' || type === 'SAVINGS') && !iban) return { ibanRequired: true };
  if (type === 'CREDIT_CARD' && iban) return { ibanForbidden: true };
  return null;
}

@Component({
  selector: 'app-open-account-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButton,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './open-account-dialog.html',
  styleUrl: './open-account-dialog.scss',
})
export class OpenAccountDialog {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<OpenAccountDialog>);
  private readonly accountsState = inject(AccountsState);
  private readonly notifications = inject(NotificationService);

  protected readonly submitting = signal(false);

  protected readonly form = this.fb.group(
    {
      type: this.fb.control<AccountType | null>(null, Validators.required),
      name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(100)]),
      iban: this.fb.control<string>(''),
      initialBalance: this.fb.group({
        amount: this.fb.nonNullable.control('', Validators.required),
        currency: this.fb.nonNullable.control('CHF', Validators.required),
      }),
    },
    { validators: ibanMatchesType },
  );

  protected submit(): void {
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();
    const request: OpenAccountRequest = {
      type: raw.type!,
      name: raw.name,
      iban: raw.iban?.trim() || undefined,
      initialBalance: raw.initialBalance,
    };
    this.submitting.set(true);
    this.accountsState.openAccount(request).subscribe({
      next: () => this.dialogRef.close(),
      error: (err) => {
        this.submitting.set(false);
        this.notifications.error(err);
      },
    });
  }
}

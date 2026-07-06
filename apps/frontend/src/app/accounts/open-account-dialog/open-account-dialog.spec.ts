import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { AccountType } from '../../../generated/api';
import { AccountsState } from '../../services/accounts-state';
import { NotificationService } from '../../services/notification-service';
import { OpenAccountDialog } from './open-account-dialog';

describe('OpenAccountDialog', () => {
  let component: OpenAccountDialog;
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let accountsState: { openAccount: ReturnType<typeof vi.fn> };
  let notifications: { error: ReturnType<typeof vi.fn> };

  // the form and submit() are protected: reach them through a typed-loose handle
  const asAny = () =>
    component as unknown as { form: FormGroup; submit: () => void; submitting: () => boolean };

  beforeEach(() => {
    dialogRef = { close: vi.fn() };
    accountsState = { openAccount: vi.fn(() => of({})) };
    notifications = { error: vi.fn() };

    TestBed.configureTestingModule({
      imports: [OpenAccountDialog],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AccountsState, useValue: accountsState },
        { provide: NotificationService, useValue: notifications },
        provideTranslateService(), // the template imports TranslatePipe
      ],
    });
    // no detectChanges: we exercise the form logic, not the rendered template
    component = TestBed.createComponent(OpenAccountDialog).componentInstance;
  });

  function fillForm(type: AccountType, iban: string) {
    asAny().form.patchValue({
      type,
      name: 'UBS',
      iban,
      initialBalance: { amount: '100', currency: 'CHF' },
    });
  }

  describe('conditional IBAN validation', () => {
    it('requires an IBAN for a CURRENT account', () => {
      fillForm(AccountType.Current, '');
      expect(asAny().form.hasError('ibanRequired')).toBe(true);
    });

    it('forbids an IBAN for a CREDIT_CARD account', () => {
      fillForm(AccountType.CreditCard, 'CH9300762011623852957');
      expect(asAny().form.hasError('ibanForbidden')).toBe(true);
    });

    it('accepts a CURRENT account that carries an IBAN', () => {
      fillForm(AccountType.Current, 'CH9300762011623852957');
      expect(asAny().form.valid).toBe(true);
    });
  });

  describe('submit()', () => {
    it('maps the form to the request, posts, and closes on success', () => {
      fillForm(AccountType.Current, 'CH9300762011623852957');

      asAny().submit();

      expect(accountsState.openAccount).toHaveBeenCalledWith({
        type: AccountType.Current,
        name: 'UBS',
        iban: 'CH9300762011623852957',
        initialBalance: { amount: '100', currency: 'CHF' },
      });
      expect(dialogRef.close).toHaveBeenCalled();
    });

    it('sends an empty IBAN as undefined (credit card)', () => {
      fillForm(AccountType.CreditCard, '');

      asAny().submit();

      expect(accountsState.openAccount).toHaveBeenCalledWith(
        expect.objectContaining({ iban: undefined }),
      );
    });

    it('does nothing when the form is invalid', () => {
      // type left null → required fails
      asAny().submit();
      expect(accountsState.openAccount).not.toHaveBeenCalled();
    });

    it('keeps the dialog open, resets pending and notifies on error', () => {
      accountsState.openAccount.mockReturnValue(throwError(() => new Error('400')));
      fillForm(AccountType.Current, 'CH9300762011623852957');

      asAny().submit();

      expect(notifications.error).toHaveBeenCalled();
      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(asAny().submitting()).toBe(false);
    });
  });
});

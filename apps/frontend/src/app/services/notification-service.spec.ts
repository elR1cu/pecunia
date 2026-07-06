import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';

import { NotificationService } from './notification-service';

describe('NotificationService', () => {
  let service: NotificationService;
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let translate: { instant: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    snackBar = { open: vi.fn() };
    // echo the key back so we can assert which translation key was requested
    translate = { instant: vi.fn((key: string) => key) };

    TestBed.configureTestingModule({
      providers: [
        { provide: MatSnackBar, useValue: snackBar },
        { provide: TranslateService, useValue: translate },
      ],
    });
    // @Service() makes it providedIn root, so injecting is enough
    service = TestBed.inject(NotificationService);
  });

  it('shows the RFC 9457 ProblemDetail.detail when the error carries one', () => {
    // given a backend problem+json response
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'An IBAN is required for account type CURRENT.' },
    });

    // when
    service.error(error);

    // then the human-readable detail is shown, with the translated close action
    expect(snackBar.open).toHaveBeenCalledWith(
      'An IBAN is required for account type CURRENT.',
      'common.close',
      { duration: 5000 },
    );
  });

  it('falls back to the translated generic message for a non-HTTP error', () => {
    service.error(new Error('boom'));

    expect(snackBar.open).toHaveBeenCalledWith('common.error', 'common.close', {
      duration: 5000,
    });
  });

  it('falls back to the generic message when the HTTP body has no detail', () => {
    service.error(new HttpErrorResponse({ status: 500, error: {} }));

    expect(snackBar.open).toHaveBeenCalledWith('common.error', 'common.close', {
      duration: 5000,
    });
  });
});

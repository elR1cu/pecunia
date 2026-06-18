import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';

import { authGuard } from './auth-guard';
import { IdentityService } from '../../generated/api';

describe('authGuard', () => {
  // the guard calls inject(), so it must run inside an injection context
  const runGuard: CanActivateFn = (...params) =>
    TestBed.runInInjectionContext(() => authGuard(...params));

  // the guard uses neither route nor state: empty objects are enough
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  // replace the real IdentityService with a fake whose getCurrentUser() we control
  function provideIdentity(getCurrentUser: () => Observable<unknown>) {
    TestBed.configureTestingModule({
      providers: [{ provide: IdentityService, useValue: { getCurrentUser } }],
    });
  }

  it('allows activation when /api/me returns a user (200)', () => {
    provideIdentity(() => of({ displayName: 'Test User' }));

    let allowed: boolean | undefined;
    (runGuard(route, state) as Observable<boolean>).subscribe((result) => (allowed = result));

    expect(allowed).toBe(true);
  });

  it('blocks activation when /api/me fails (401)', () => {
    provideIdentity(() => throwError(() => new HttpErrorResponse({ status: 401 })));

    let allowed: boolean | undefined;
    (runGuard(route, state) as Observable<boolean>).subscribe((result) => (allowed = result));

    expect(allowed).toBe(false);
  });
});

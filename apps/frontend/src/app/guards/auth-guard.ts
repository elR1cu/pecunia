import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { IdentityService } from '../../generated/api';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const identityService = inject(IdentityService);

  return identityService.getCurrentUser().pipe(
    map(() => true),
    catchError(() => of(false)),
  );
};

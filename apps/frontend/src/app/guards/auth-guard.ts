import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { IdentityState } from '../services/identity-state';

export const authGuard: CanActivateFn = () => {
  return inject(IdentityState).load();
};

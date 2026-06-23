import { inject, Service, signal } from '@angular/core';
import { catchError, map, Observable, of, shareReplay, tap } from 'rxjs';
import { CurrentUser, IdentityService } from '../../generated/api';

@Service()
export class IdentityState {
  private readonly identityService = inject(IdentityService);

  // Private writable source of truth; only this service mutates it.
  private readonly _user = signal<CurrentUser | null>(null);

  // Public read-only view: any component can read the user reactively.
  readonly user = this._user.asReadonly();

  // The in-flight request, cached so concurrent callers share a single HTTP call.
  private request$?: Observable<CurrentUser>;

  /**
   * Ensures the current user is loaded, fetching at most once, and reports
   * whether the user is authenticated. Used by the route guard.
   */
  load(): Observable<boolean> {
    if (this._user()) {
      return of(true); // already loaded: no network call
    }
    // ??= creates the stream only the first time; later callers reuse it.
    this.request$ ??= this.identityService.getCurrentUser().pipe(
      tap((user) => this._user.set(user)),
      shareReplay(1),
    );
    return this.request$.pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }
}

import { inject, Service } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { LogoutResponse } from './logout';

@Service()
export class AuthService {
  private readonly httpClient = inject(HttpClient);

  logout(): void {
    this.httpClient.post<LogoutResponse>('/logout', null).subscribe({
      next: (res) => (window.location.href = res.logoutUrl),
      // TODO: replace with user-facing feedback (MatSnackBar)
      error: (err: HttpErrorResponse) => console.error(err),
    });
  }
}

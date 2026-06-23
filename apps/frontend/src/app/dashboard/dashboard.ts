import { Component, inject } from '@angular/core';
import { AuthService } from '../services/auth-service';
import { IdentityState } from '../services/identity-state';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-dashboard',
  imports: [MatButton, TranslatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly authService = inject(AuthService);
  private readonly identityState = inject(IdentityState);
  // The guard has already loaded the user; we just read the shared signal.
  protected readonly user = this.identityState.user;

  protected logout(): void {
    this.authService.logout();
  }
}

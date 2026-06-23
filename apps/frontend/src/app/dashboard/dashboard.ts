import { Component, inject } from '@angular/core';
import { IdentityService } from '../../generated/api';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../services/auth-service';
import { MatButton } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-dashboard',
  imports: [MatButton, TranslatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly identityService = inject(IdentityService);
  private readonly authService = inject(AuthService);
  protected readonly user = toSignal(this.identityService.getCurrentUser());

  protected logout(): void {
    this.authService.logout();
  }
}

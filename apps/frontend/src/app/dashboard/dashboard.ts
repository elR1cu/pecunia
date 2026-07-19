import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { IdentityState } from '../services/identity-state';

@Component({
  selector: 'app-dashboard',
  imports: [MatIconModule, TranslatePipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly identityState = inject(IdentityState);
  // The guard has already loaded the user; we just read the shared signal.
  protected readonly user = this.identityState.user;
}

import { Component, inject } from '@angular/core';
import { MatIconButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageSwitcher } from './components/language-switcher/language-switcher';
import { AuthService } from './services/auth-service';
import { IdentityState } from './services/identity-state';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconButton,
    MatIconModule,
    TranslatePipe,
    LanguageSwitcher,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly authService = inject(AuthService);
  private readonly identityState = inject(IdentityState);

  // The auth guard populates the identity on protected routes; on the public
  // landing it stays null, which is what hides the app header there.
  protected readonly user = this.identityState.user;

  protected initials(displayName: string): string {
    return displayName
      .split(/\s+/)
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }

  protected logout(): void {
    this.authService.logout();
  }
}

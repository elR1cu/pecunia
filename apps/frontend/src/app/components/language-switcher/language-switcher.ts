import { Component, inject } from '@angular/core';
import { LanguageService } from '../../services/language-service';
import { MatButton } from '@angular/material/button';

@Component({
  selector: 'app-language-switcher',
  imports: [MatButton],
  templateUrl: './language-switcher.html',
  styleUrl: './language-switcher.scss',
})
export class LanguageSwitcher {
  protected readonly languageService = inject(LanguageService);
}

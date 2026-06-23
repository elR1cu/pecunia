import { inject, Service, Signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

const STORAGE_KEY = 'pecunia.lang';
const SUPPORTED = ['en', 'fr', 'de', 'it'] as const;
type SupportedLang = (typeof SUPPORTED)[number];
const DEFAULT_LANG: SupportedLang = 'en';

@Service()
export class LanguageService {
  private readonly translate = inject(TranslateService);

  readonly supported = SUPPORTED;
  readonly current: Signal<string | null> = this.translate.currentLang;

  /** Called once at bootstrap via provideAppInitializer. */
  init(): Observable<unknown> {
    return this.translate.use(this.resolveInitialLang());
  }

  switch(lang: SupportedLang): void {
    this.translate.use(lang);
    localStorage.setItem(STORAGE_KEY, lang);
  }

  private resolveInitialLang(): SupportedLang {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (this.isSupported(saved)) {
      return saved;
    }
    const browser = this.translate.getBrowserLang();
    if (this.isSupported(browser)) {
      return browser;
    }
    return DEFAULT_LANG;
  }

  private isSupported(lang: string | null | undefined): lang is SupportedLang {
    return SUPPORTED.includes(lang as SupportedLang);
  }
}

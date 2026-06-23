import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { provideApi } from '../generated/api';
import { routes } from './app.routes';
import { authErrorInterceptor } from './interceptors/auth-error-interceptor';
import { LanguageService } from './services/language-service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authErrorInterceptor])),
    provideApi(''),
    // i18n: JSON files served from public/i18n/ (no /assets/ in Angular 22).
    // No initial `lang` here on purpose — LanguageService.init() picks the
    // persisted language so we avoid loading a default file then replacing it.
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: 'en',
    }),
    provideAppInitializer(() => inject(LanguageService).init()),
  ],
};

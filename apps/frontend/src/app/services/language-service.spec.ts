import { TestBed } from '@angular/core/testing';
import { signal, Signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { of, Observable } from 'rxjs';

import { LanguageService } from './language-service';

const STORAGE_KEY = 'pecunia.lang';

describe('LanguageService', () => {
  let service: LanguageService;
  let translate: {
    use: ReturnType<typeof vi.fn>;
    getBrowserLang: ReturnType<typeof vi.fn>;
    currentLang: Signal<string | null>;
  };

  beforeEach(() => {
    // Minimal TranslateService stand-in: we assert how the service drives it,
    // we don't load any real translation file.
    translate = {
      use: vi.fn((lang: string): Observable<unknown> => of({})),
      getBrowserLang: vi.fn<() => string | undefined>(() => undefined),
      currentLang: signal<string | null>(null),
    };

    localStorage.clear(); // isolate each test from persisted state

    TestBed.configureTestingModule({
      providers: [
        { provide: TranslateService, useValue: translate as unknown as TranslateService },
      ],
    });

    // @Service() makes LanguageService providedIn root, so injecting it is enough
    service = TestBed.inject(LanguageService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('init() uses the persisted language when it is supported', () => {
    localStorage.setItem(STORAGE_KEY, 'de');
    translate.getBrowserLang.mockReturnValue('fr'); // persisted must win over browser

    service.init();

    expect(translate.use).toHaveBeenCalledExactlyOnceWith('de');
  });

  it('init() falls back to the browser language when nothing is persisted', () => {
    translate.getBrowserLang.mockReturnValue('fr');

    service.init();

    expect(translate.use).toHaveBeenCalledExactlyOnceWith('fr');
  });

  it('init() falls back to the default language when neither source is supported', () => {
    translate.getBrowserLang.mockReturnValue('es'); // not in the supported set

    service.init();

    expect(translate.use).toHaveBeenCalledExactlyOnceWith('en');
  });

  it('init() ignores a tampered (unsupported) persisted value', () => {
    localStorage.setItem(STORAGE_KEY, 'xx'); // not a supported language
    // browser lang left undefined → must land on the default

    service.init();

    expect(translate.use).toHaveBeenCalledExactlyOnceWith('en');
  });

  it('init() returns the observable from use() so the app initializer can await it', () => {
    const load$ = of('loaded');
    translate.use.mockReturnValue(load$);

    expect(service.init()).toBe(load$);
  });

  it('switch() applies the language and persists the choice', () => {
    service.switch('it');

    expect(translate.use).toHaveBeenCalledExactlyOnceWith('it');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('it');
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { authErrorInterceptor } from './auth-error-interceptor';

describe('authErrorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let originalLocation: Location;

  beforeEach(() => {
    // jsdom forbids real navigation: replace window.location with a plain
    // object whose href we can read back afterwards.
    originalLocation = window.location;
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { href: '' },
    });

    TestBed.configureTestingModule({
      providers: [
        // register the interceptor in the HTTP chain, then the testing backend
        provideHttpClient(withInterceptors([authErrorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify(); // no outstanding requests left unhandled
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
  });

  it('redirects to the login endpoint and relays the error on 401', () => {
    let relayedStatus: number | undefined;
    http.get('/api/me').subscribe({
      next: () => {},
      error: (error) => (relayedStatus = error.status),
    });

    // simulate the backend response
    httpTesting.expectOne('/api/me').flush('Unauthorized', {
      status: 401,
      statusText: 'Unauthorized',
    });

    expect(window.location.href).toBe('/oauth2/authorization/pecunia'); // the side effect
    expect(relayedStatus).toBe(401); // throwError relayed the 401 to the caller (the guard)
  });

  it('does not redirect on a non-401 error', () => {
    http.get('/api/me').subscribe({ next: () => {}, error: () => {} });

    httpTesting.expectOne('/api/me').flush('Boom', {
      status: 500,
      statusText: 'Server Error',
    });

    expect(window.location.href).toBe(''); // unchanged
  });

  it('passes a successful response through untouched', () => {
    let body: unknown;
    http.get('/api/me').subscribe((response) => (body = response));

    httpTesting.expectOne('/api/me').flush({ ok: true });

    expect(body).toEqual({ ok: true });
    expect(window.location.href).toBe('');
  });
});

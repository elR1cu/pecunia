import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth-service';

describe('AuthService', () => {
  let service: AuthService;
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
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    // AuthService is providedIn root (@Service), so injecting it is enough
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify(); // no outstanding requests left unhandled
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    });
  });

  it('POSTs to /logout and navigates to the returned logoutUrl on success', () => {
    service.logout();

    const req = httpTesting.expectOne('/logout');
    expect(req.request.method).toBe('POST'); // mutating call, carries the CSRF header at runtime
    expect(req.request.body).toBeNull();

    req.flush({ logoutUrl: 'http://localhost:8080/keycloak/logout' });

    expect(window.location.href).toBe('http://localhost:8080/keycloak/logout'); // the side effect
  });

  it('does not navigate when /logout fails', () => {
    // the error path logs to console.error; silence it to keep the test output clean
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});

    service.logout();

    httpTesting.expectOne('/logout').flush('Forbidden', {
      status: 403,
      statusText: 'Forbidden',
    });

    expect(window.location.href).toBe(''); // unchanged
    expect(consoleError).toHaveBeenCalled();

    consoleError.mockRestore();
  });
});

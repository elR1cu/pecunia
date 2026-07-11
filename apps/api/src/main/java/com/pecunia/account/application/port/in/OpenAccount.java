package com.pecunia.account.application.port.in;

import com.pecunia.sharedkernel.AccountId;

/**
 * Driving port: register a new account for the owner.
 *
 * <p>Returns only the new {@link AccountId} — a command yields metadata, never
 * a read model (CQRS, mirroring {@code CreateCategory}). The web layer builds
 * the 201 response body via {@link GetAccount}. Invariant violations surface
 * as domain exceptions rather than a sealed Result (ADR-0027).
 */
public interface OpenAccount {

    AccountId open(OpenAccountCommand command);
}

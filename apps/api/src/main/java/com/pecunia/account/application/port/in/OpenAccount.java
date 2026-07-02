package com.pecunia.account.application.port.in;

import com.pecunia.shared.AccountId;

/**
 * Driving port: register a new account for the owner.
 *
 * <p>Single success outcome, so it returns the created {@link AccountId}
 * directly rather than a sealed Result. Invariant violations surface as domain
 * exceptions (see ADR-0027).
 */
public interface OpenAccount {

    AccountId open(OpenAccountCommand command);
}

package com.pecunia.account.application.port.in;

import com.pecunia.account.domain.Account;

/**
 * Driving port: register a new account for the owner.
 *
 * <p>Single success outcome, so it returns the created {@link Account} aggregate
 * directly rather than a sealed Result. Invariant violations surface as domain
 * exceptions (see ADR-0027).
 */
public interface OpenAccount {

    Account open(OpenAccountCommand command);
}

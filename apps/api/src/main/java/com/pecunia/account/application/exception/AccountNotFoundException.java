package com.pecunia.account.application.exception;

import com.pecunia.shared.AccountId;

/**
 * Thrown when an account does not exist <em>or</em> is not owned by the
 * requesting user. Both collapse into "not found" and map to HTTP 404 (never
 * 403), so the existence of another user's resource is not leaked
 * (multi-tenancy rule). Lives in the application layer, not the domain: it is
 * a repository-lookup concern, not a domain invariant. See ADR-0027.
 */
public final class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(AccountId accountId) {
        super("Account not found: " + accountId);
    }
}

package com.pecunia.account.domain.exception;

import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.exception.DomainException;

/** Thrown when archiving an account that is already archived. Maps to HTTP 409. */
public final class AccountAlreadyArchivedException extends DomainException {

    public AccountAlreadyArchivedException(AccountId accountId) {
        super("Account already archived: " + accountId);
    }
}

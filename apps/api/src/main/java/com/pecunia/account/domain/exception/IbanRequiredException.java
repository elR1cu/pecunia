package com.pecunia.account.domain.exception;

import com.pecunia.account.domain.AccountType;
import com.pecunia.shared.exception.DomainException;

/** Thrown when an account type requires an IBAN but none was provided. */
public final class IbanRequiredException extends DomainException {

    public IbanRequiredException(AccountType type) {
        super("IBAN is required for account type " + type);
    }
}

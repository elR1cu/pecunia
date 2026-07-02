package com.pecunia.account.domain.exception;

import com.pecunia.account.domain.AccountType;
import com.pecunia.shared.exception.DomainException;

/** Thrown when an IBAN is provided for an account type that forbids one (e.g. CREDIT_CARD). */
public final class IbanForbiddenForTypeException extends DomainException {

    public IbanForbiddenForTypeException(AccountType type) {
        super("IBAN is forbidden for account type " + type);
    }
}

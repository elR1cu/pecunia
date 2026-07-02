package com.pecunia.account.domain.exception;

import com.pecunia.shared.exception.DomainException;

/**
 * Thrown when an IBAN fails structural or ISO 7064 mod-97 validation.
 *
 * <p>Security: the raw IBAN value is intentionally never carried in the
 * message, to avoid leaking it into logs (see the logging sanitization
 * policy). A non-sensitive reason may be added later if needed.
 */
public final class InvalidIbanException extends DomainException {

    public InvalidIbanException() {
        super("IBAN is invalid");
    }
}

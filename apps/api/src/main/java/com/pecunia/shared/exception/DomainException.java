package com.pecunia.shared.exception;

/**
 * Base type for domain invariant violations across all bounded contexts.
 *
 * <p>Deliberately <em>not</em> {@code sealed}: sealing would require this
 * shared-kernel base to {@code permit} subtypes living in the bounded
 * contexts, inverting the dependency direction (a layering violation). It
 * would also buy nothing, since exceptions are not exhaustively matched in a
 * {@code catch}. Each context extends this base freely. See ADR-0027.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

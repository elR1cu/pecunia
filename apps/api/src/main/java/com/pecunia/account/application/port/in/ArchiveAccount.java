package com.pecunia.account.application.port.in;

/**
 * Driving port: archive an existing account.
 *
 * <p>No data-carrying outcome, so it returns {@code void}. The two expected
 * failures travel as exceptions: {@code AccountNotFoundException} (404) and
 * {@code AccountAlreadyArchivedException} (409). See ADR-0027.
 */
public interface ArchiveAccount {

    void archive(ArchiveAccountCommand command);
}

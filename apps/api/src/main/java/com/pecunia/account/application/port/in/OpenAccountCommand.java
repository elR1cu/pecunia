package com.pecunia.account.application.port.in;

import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
import java.util.Optional;

/**
 * Input for {@link OpenAccount}.
 *
 * <p>Speaks the domain's language (value objects), not primitives: the web
 * layer translates the request DTO into these types (which is where an
 * {@code InvalidIbanException} may be raised while building the {@link Iban}).
 * {@code iban} is {@link Optional} rather than {@code null} to keep the module
 * boundary null-free.
 */
public record OpenAccountCommand(
        UserId owner, AccountType type, String name, Optional<Iban> iban, Money initialBalance) {}

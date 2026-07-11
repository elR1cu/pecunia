package com.pecunia.account.application.readmodel;

import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.Money;
import java.util.Optional;

/**
 * Flat read model for a single account. Returned by query use cases so the
 * mutable {@link com.pecunia.account.domain.Account} aggregate never leaves
 * the application layer. Value objects are immutable, hence safe to expose.
 */
public record AccountView(
        AccountId id, AccountType type, String name, Optional<Iban> iban, AccountStatus status, Money initialBalance) {

    public static AccountView fromAccount(Account account) {
        return new AccountView(
                account.id(),
                account.type(),
                account.name(),
                account.iban(),
                account.status(),
                account.initialBalance());
    }
}

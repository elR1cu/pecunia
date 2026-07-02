package com.pecunia.account.domain;

import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.account.domain.exception.IbanForbiddenForTypeException;
import com.pecunia.account.domain.exception.IbanRequiredException;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
import java.util.Objects;

public final class Account {

    private final AccountId id;
    private final UserId owner;
    private final AccountType type;
    private AccountStatus status;
    private final String name;
    private final Iban iban;
    private final Money initialBalance;

    private Account(
            AccountId id,
            UserId owner,
            AccountType type,
            AccountStatus status,
            String name,
            Iban iban,
            Money initialBalance) {
        Objects.requireNonNull(id, "AccountId cannot be null");
        Objects.requireNonNull(owner, "UserId cannot be null");
        Objects.requireNonNull(type, "AccountType cannot be null");
        Objects.requireNonNull(status, "AccountStatus cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(initialBalance, "initialBalance cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        if (type.requiresIban() && iban == null) {
            throw new IbanRequiredException(type);
        }
        if (!type.requiresIban() && iban != null) {
            throw new IbanForbiddenForTypeException(type);
        }
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.status = status;
        this.name = name.strip();
        this.iban = iban;
        this.initialBalance = initialBalance;
    }

    public static Account open(
            AccountId id, UserId owner, AccountType type, String name, Iban iban, Money initialBalance) {
        return new Account(id, owner, type, AccountStatus.ACTIVE, name, iban, initialBalance);
    }

    public static Account reconstitute(
            AccountId id,
            UserId owner,
            AccountType type,
            AccountStatus status,
            String name,
            Iban iban,
            Money initialBalance) {
        return new Account(id, owner, type, status, name, iban, initialBalance);
    }

    public void archive() {
        if (this.status == AccountStatus.ARCHIVED) {
            throw new AccountAlreadyArchivedException(id);
        }
        this.status = AccountStatus.ARCHIVED;
    }

    public Money balanceFrom(Money movementsSum) {
        Objects.requireNonNull(movementsSum, "movementsSum cannot be null");
        return initialBalance.add(movementsSum);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Account{" + "id="
                + id + ", owner="
                + owner + ", type="
                + type + ", status="
                + status + ", name='"
                + name + '\'' + '}';
    }

    public AccountId id() {
        return id;
    }

    public UserId owner() {
        return owner;
    }

    public AccountType type() {
        return type;
    }

    public AccountStatus status() {
        return status;
    }

    public String name() {
        return name;
    }

    public Iban iban() {
        return iban;
    }

    public Money initialBalance() {
        return initialBalance;
    }
}

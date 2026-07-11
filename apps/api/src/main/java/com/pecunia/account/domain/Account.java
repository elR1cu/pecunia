package com.pecunia.account.domain;

import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.account.domain.exception.IbanForbiddenForTypeException;
import com.pecunia.account.domain.exception.IbanRequiredException;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.Money;
import com.pecunia.sharedkernel.UserId;
import java.util.Objects;
import java.util.Optional;

public final class Account {

    private final AccountId id;
    private final UserId owner;
    private final AccountType type;
    private AccountStatus status;
    private final String name;
    private final Iban iban;
    private final Money initialBalance;
    private final Long version;

    // S107: an aggregate root legitimately carries its full state; a parameter object would
    // fragment the model without cohesion.
    @SuppressWarnings("java:S107")
    private Account(
            AccountId id,
            UserId owner,
            AccountType type,
            AccountStatus status,
            String name,
            Iban iban,
            Money initialBalance,
            Long version) {
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
        this.version = version;
    }

    public static Account open(
            AccountId id, UserId owner, AccountType type, String name, Iban iban, Money initialBalance) {
        return new Account(id, owner, type, AccountStatus.ACTIVE, name, iban, initialBalance, null);
    }

    @SuppressWarnings("java:S107") // rehydrates the full aggregate state from persistence
    public static Account reconstitute(
            AccountId id,
            UserId owner,
            AccountType type,
            AccountStatus status,
            String name,
            Iban iban,
            Money initialBalance,
            Long version) {
        return new Account(id, owner, type, status, name, iban, initialBalance, version);
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
                + name + '\'' + ", version="
                + version + '}';
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

    public Optional<Iban> iban() {
        return Optional.ofNullable(iban);
    }

    public Money initialBalance() {
        return initialBalance;
    }

    public Optional<Long> version() {
        return Optional.ofNullable(version);
    }
}

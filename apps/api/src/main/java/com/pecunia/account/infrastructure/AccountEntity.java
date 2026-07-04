package com.pecunia.account.infrastructure;

import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.UserId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(updatable = false)
    private String iban;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "initial_balance_amount", nullable = false, precision = 19, scale = 4))
    @AttributeOverride(
            name = "currency",
            column = @Column(name = "initial_balance_currency", nullable = false, length = 3))
    private MoneyEmbeddable initialBalance;

    @Version
    private Long version;

    // read-only, handled by db
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @SuppressWarnings("java:S107") // the entity mirrors the aggregate's full state
    private AccountEntity(
            UUID id,
            UUID ownerId,
            AccountType type,
            AccountStatus status,
            String name,
            String iban,
            MoneyEmbeddable initialBalance,
            Long version) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.status = status;
        this.name = name;
        this.iban = iban;
        this.initialBalance = initialBalance;
        this.version = version;
        // no createdAt
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountEntity that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AccountEntity{" + "id="
                + id + ", ownerId="
                + ownerId + ", type="
                + type + ", status="
                + status + ", name='"
                + name + '\'' + ", version="
                + version + '}';
    }

    public static AccountEntity fromDomain(Account account) {
        return new AccountEntity(
                account.id().value(),
                account.owner().value(),
                account.type(),
                account.status(),
                account.name(),
                Optional.ofNullable(account.iban()).map(Iban::value).orElse(null),
                MoneyEmbeddable.fromDomain(account.initialBalance()),
                account.version().orElse(null));
    }

    public Account toDomain() {
        return Account.reconstitute(
                AccountId.of(id),
                UserId.of(ownerId),
                type,
                status,
                name,
                Optional.ofNullable(iban).map(Iban::new).orElse(null),
                initialBalance.toDomain(),
                version);
    }
}

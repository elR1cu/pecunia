package com.pecunia.account.infrastructure;

import com.pecunia.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoneyEmbeddable {

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    public static MoneyEmbeddable fromDomain(Money money) {
        return new MoneyEmbeddable(money.amount(), money.currency());
    }

    public Money toDomain() {
        return Money.of(amount, currency);
    }
}

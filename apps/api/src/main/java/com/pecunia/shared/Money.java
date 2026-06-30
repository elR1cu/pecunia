package com.pecunia.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    private static final Currency CHF = Currency.getInstance("CHF");

    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        amount = amount.setScale(4, RoundingMode.HALF_EVEN); // banker rounding
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money chf(BigDecimal amount) {
        return new Money(amount, CHF);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroChf() {
        return chf(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currencies do not match: %s != %s".formatted(currency, other.currency));
        }
        BigDecimal sum = amount.add(other.amount);
        return new Money(sum, currency);
    }
}

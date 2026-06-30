package com.pecunia.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

    private static final Currency CHF = Currency.getInstance("CHF");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("normalizes the amount to scale 4")
        void normalizes_scale_to_four() {
            Money money = Money.of(new BigDecimal("10"), CHF);

            assertThat(money.amount().scale()).isEqualTo(4);
            assertThat(money.amount()).isEqualTo(new BigDecimal("10.0000"));
        }

        @Test
        @DisplayName("rounds half to even (banker's rounding), down toward the even neighbour")
        void rounds_half_to_even_down() {
            // 2.00005 is exactly halfway between 2.0000 and 2.0001 -> even neighbour 2.0000
            assertThat(Money.of(new BigDecimal("2.00005"), CHF).amount()).isEqualTo(new BigDecimal("2.0000"));
        }

        @Test
        @DisplayName("rounds half to even (banker's rounding), up toward the even neighbour")
        void rounds_half_to_even_up() {
            // 2.00015 is exactly halfway between 2.0001 and 2.0002 -> even neighbour 2.0002
            assertThat(Money.of(new BigDecimal("2.00015"), CHF).amount()).isEqualTo(new BigDecimal("2.0002"));
        }
    }

    @Nested
    @DisplayName("factories")
    class Factories {

        @Test
        @DisplayName("of carries the given amount and currency")
        void of_carries_amount_and_currency() {
            Money money = Money.of(new BigDecimal("12.34"), EUR);

            assertThat(money.amount()).isEqualByComparingTo("12.34");
            assertThat(money.currency()).isEqualTo(EUR);
        }

        @Test
        @DisplayName("chf uses the CHF currency")
        void chf_uses_chf_currency() {
            assertThat(Money.chf(new BigDecimal("5.00")).currency()).isEqualTo(CHF);
        }

        @Test
        @DisplayName("zero is the additive identity for the given currency")
        void zero_is_additive_identity() {
            Money zero = Money.zero(CHF);

            assertThat(zero.currency()).isEqualTo(CHF);
            assertThat(zero.amount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("zeroChf equals zero in CHF")
        void zero_chf_equals_zero_in_chf() {
            assertThat(Money.zeroChf()).isEqualTo(Money.zero(CHF));
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects a null amount")
        void rejects_null_amount() {
            assertThatNullPointerException().isThrownBy(() -> Money.of(null, CHF));
        }

        @Test
        @DisplayName("rejects a null currency")
        void rejects_null_currency() {
            assertThatNullPointerException().isThrownBy(() -> Money.of(BigDecimal.ONE, null));
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("two amounts equal in value but different in input scale are equal")
        void value_equality_ignores_input_scale() {
            assertThat(Money.of(new BigDecimal("10"), CHF)).isEqualTo(Money.of(new BigDecimal("10.0000"), CHF));
        }

        @Test
        @DisplayName("the same amount in different currencies is not equal")
        void different_currency_is_not_equal() {
            assertThat(Money.of(new BigDecimal("10.00"), CHF)).isNotEqualTo(Money.of(new BigDecimal("10.00"), EUR));
        }
    }

    @Nested
    @DisplayName("add")
    class Add {

        @Test
        @DisplayName("sums two amounts of the same currency")
        void sums_same_currency() {
            Money result = Money.chf(new BigDecimal("10.50")).add(Money.chf(new BigDecimal("5.25")));

            assertThat(result).isEqualTo(Money.chf(new BigDecimal("15.75")));
        }

        @Test
        @DisplayName("handles a negative operand (a debit)")
        void handles_negative_operand() {
            Money result = Money.chf(new BigDecimal("100.00")).add(Money.chf(new BigDecimal("-30.00")));

            assertThat(result).isEqualTo(Money.chf(new BigDecimal("70.00")));
        }

        @Test
        @DisplayName("keeps the result at scale 4")
        void keeps_scale_four() {
            Money result = Money.chf(new BigDecimal("1.10")).add(Money.chf(new BigDecimal("2.20")));

            assertThat(result.amount().scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("rejects adding a different currency")
        void rejects_currency_mismatch() {
            assertThatThrownBy(() -> Money.chf(BigDecimal.TEN).add(Money.of(BigDecimal.TEN, EUR)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CHF")
                    .hasMessageContaining("EUR");
        }
    }
}

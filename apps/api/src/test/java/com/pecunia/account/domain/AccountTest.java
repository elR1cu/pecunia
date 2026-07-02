package com.pecunia.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.account.domain.exception.IbanForbiddenForTypeException;
import com.pecunia.account.domain.exception.IbanRequiredException;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final AccountId ID = AccountId.of(UUID.randomUUID());
    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final Iban IBAN = new Iban("CH9300762011623852957");
    private static final Money INITIAL = Money.chf(new BigDecimal("100.00"));
    private static final Currency EUR = Currency.getInstance("EUR");

    @Nested
    @DisplayName("open")
    class Open {

        @Test
        @DisplayName("opens an active account carrying the given fields")
        void opens_active_account() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);

            assertThat(account.id()).isEqualTo(ID);
            assertThat(account.owner()).isEqualTo(OWNER);
            assertThat(account.type()).isEqualTo(AccountType.CURRENT);
            assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.name()).isEqualTo("Main");
            assertThat(account.iban()).isEqualTo(IBAN);
            assertThat(account.initialBalance()).isEqualTo(INITIAL);
        }

        @Test
        @DisplayName("opens a credit card without an IBAN")
        void opens_credit_card_without_iban() {
            Account account = Account.open(ID, OWNER, AccountType.CREDIT_CARD, "Visa", null, INITIAL);

            assertThat(account.iban()).isNull();
            assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("strips surrounding whitespace from the name")
        void strips_name() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "  Main  ", IBAN, INITIAL);

            assertThat(account.name()).isEqualTo("Main");
        }
    }

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        @DisplayName("rejects a null id")
        void rejects_null_id() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Account.open(null, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL));
        }

        @Test
        @DisplayName("rejects a null owner")
        void rejects_null_owner() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Account.open(ID, null, AccountType.CURRENT, "Main", IBAN, INITIAL));
        }

        @Test
        @DisplayName("rejects a null initial balance")
        void rejects_null_initial_balance() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, null));
        }

        @Test
        @DisplayName("rejects a blank name")
        void rejects_blank_name() {
            assertThatThrownBy(() -> Account.open(ID, OWNER, AccountType.CURRENT, "   ", IBAN, INITIAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name cannot be blank");
        }

        @Test
        @DisplayName("requires an IBAN for a current account")
        void current_requires_iban() {
            assertThatThrownBy(() -> Account.open(ID, OWNER, AccountType.CURRENT, "Main", null, INITIAL))
                    .isInstanceOf(IbanRequiredException.class)
                    .hasMessageContaining("IBAN is required for account type CURRENT");
        }

        @Test
        @DisplayName("requires an IBAN for a savings account")
        void savings_requires_iban() {
            assertThatThrownBy(() -> Account.open(ID, OWNER, AccountType.SAVINGS, "Savings", null, INITIAL))
                    .isInstanceOf(IbanRequiredException.class)
                    .hasMessageContaining("IBAN is required for account type SAVINGS");
        }

        @Test
        @DisplayName("forbids an IBAN on a credit card")
        void credit_card_forbids_iban() {
            assertThatThrownBy(() -> Account.open(ID, OWNER, AccountType.CREDIT_CARD, "Visa", IBAN, INITIAL))
                    .isInstanceOf(IbanForbiddenForTypeException.class)
                    .hasMessageContaining("IBAN is forbidden for account type CREDIT_CARD");
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("archives an active account")
        void archives_active_account() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);

            account.archive();

            assertThat(account.status()).isEqualTo(AccountStatus.ARCHIVED);
        }

        @Test
        @DisplayName("rejects archiving an already archived account")
        void rejects_double_archive() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
            account.archive();

            assertThatThrownBy(account::archive)
                    .isInstanceOf(AccountAlreadyArchivedException.class)
                    .hasMessage("Account already archived: " + ID);
        }
    }

    @Nested
    @DisplayName("balanceFrom")
    class BalanceFrom {

        @Test
        @DisplayName("adds the movements sum to the initial balance")
        void adds_movements_to_initial_balance() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);

            Money balance = account.balanceFrom(Money.chf(new BigDecimal("25.50")));

            assertThat(balance).isEqualTo(Money.chf(new BigDecimal("125.50")));
        }

        @Test
        @DisplayName("rejects a null movements sum")
        void rejects_null_movements() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);

            assertThatNullPointerException().isThrownBy(() -> account.balanceFrom(null));
        }

        @Test
        @DisplayName("propagates a currency mismatch from Money")
        void rejects_currency_mismatch() {
            Account account = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
            Money eurMovements = Money.of(BigDecimal.TEN, EUR);

            assertThatThrownBy(() -> account.balanceFrom(eurMovements)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("two accounts with the same id are equal despite different state")
        void equal_by_id() {
            Account active = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
            Account archived = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
            archived.archive();

            assertThat(active).isEqualTo(archived);
            assertThat(active).hasSameHashCodeAs(archived);
        }

        @Test
        @DisplayName("two accounts with different ids are not equal")
        void not_equal_by_different_id() {
            Account one = Account.open(ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
            Account other =
                    Account.open(AccountId.of(UUID.randomUUID()), OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);

            assertThat(one).isNotEqualTo(other);
        }
    }
}

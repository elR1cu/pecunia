package com.pecunia.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Cross-user isolation test for the account persistence adapter against a real
 * PostgreSQL (Flyway V1..V3 applied). Mandatory from Block 2 (ADR-0014): a user
 * must never reach another user's account through the repository. The
 * {@link AccountRepository} port bakes the {@code owner} into every read, so
 * isolation is a property of the adapter — verified here end-to-end against the
 * database, not just by unit-testing the domain.
 */
@Testcontainers
@DataJpaTest
@Import(AccountRepositoryAdapter.class)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class AccountCrossUserIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private AccountRepository accountRepository;

    private static Account currentAccount(AccountId id, UserId owner) {
        return Account.open(
                id,
                owner,
                AccountType.CURRENT,
                "UBS Current",
                new Iban("CH9300762011623852957"),
                Money.chf(new BigDecimal("1000.00")));
    }

    @Test
    @DisplayName("findByIdAndOwner returns the account for its owner")
    void finds_account_for_its_owner() {
        // given
        UserId owner = UserId.of(UUID.randomUUID());
        AccountId accountId = AccountId.of(UUID.randomUUID());
        accountRepository.save(currentAccount(accountId, owner));

        // when
        // then
        assertThat(accountRepository.findByIdAndOwner(accountId, owner))
                .get()
                .extracting(Account::id)
                .isEqualTo(accountId);
    }

    @Test
    @DisplayName("findByIdAndOwner returns empty when another user requests the account")
    void does_not_leak_account_to_another_user() {
        // given — user A owns an account
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        AccountId accountId = AccountId.of(UUID.randomUUID());
        accountRepository.save(currentAccount(accountId, userA));

        // when — user B asks for A's account by its id
        // then — the row is invisible to B (the use case maps this to 404)
        assertThat(accountRepository.findByIdAndOwner(accountId, userB)).isEmpty();
    }

    @Test
    @DisplayName("findAllByOwner returns only the caller's own accounts")
    void lists_only_own_accounts() {
        // given — one account per user
        UserId userA = UserId.of(UUID.randomUUID());
        UserId userB = UserId.of(UUID.randomUUID());
        AccountId accountOfA = AccountId.of(UUID.randomUUID());
        AccountId accountOfB = AccountId.of(UUID.randomUUID());
        accountRepository.save(currentAccount(accountOfA, userA));
        accountRepository.save(currentAccount(accountOfB, userB));

        // when
        // then — B sees its own account only, never A's
        assertThat(accountRepository.findAllByOwner(userB))
                .extracting(Account::id)
                .containsExactly(accountOfB);
    }
}

package com.pecunia.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.account.domain.exception.AccountAlreadyArchivedException;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArchiveAccountServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID());
    private static final Iban IBAN = new Iban("CH9300762011623852957");
    private static final Money INITIAL = Money.chf(new BigDecimal("100.00"));

    @Mock
    private AccountRepository accountRepository;

    private ArchiveAccountService service;

    @BeforeEach
    void setUp() {
        service = new ArchiveAccountService(accountRepository);
    }

    private static Account activeAccount() {
        return Account.open(ACCOUNT_ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
    }

    @Test
    @DisplayName("archives an active account and persists it")
    void archives_active_account() {
        // given
        ArchiveAccountCommand command = new ArchiveAccountCommand(OWNER, ACCOUNT_ID);
        Account account = activeAccount();
        when(accountRepository.findByIdAndOwner(ACCOUNT_ID, OWNER)).thenReturn(Optional.of(account));

        // when
        service.archive(command);

        // then
        assertThat(account.status()).isEqualTo(AccountStatus.ARCHIVED);
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("throws AccountNotFoundException when the account is absent or not owned")
    void rejects_missing_account() {
        // given
        ArchiveAccountCommand command = new ArchiveAccountCommand(OWNER, ACCOUNT_ID);
        when(accountRepository.findByIdAndOwner(ACCOUNT_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.archive(command)).isInstanceOf(AccountNotFoundException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("propagates AccountAlreadyArchivedException and does not persist")
    void rejects_double_archive() {
        // given
        ArchiveAccountCommand command = new ArchiveAccountCommand(OWNER, ACCOUNT_ID);
        Account account = activeAccount();
        account.archive(); // already archived
        when(accountRepository.findByIdAndOwner(ACCOUNT_ID, OWNER)).thenReturn(Optional.of(account));

        // when + then
        assertThatThrownBy(() -> service.archive(command)).isInstanceOf(AccountAlreadyArchivedException.class);
        verify(accountRepository, never()).save(any());
    }
}

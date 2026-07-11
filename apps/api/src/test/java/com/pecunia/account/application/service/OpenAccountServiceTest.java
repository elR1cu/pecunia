package com.pecunia.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.account.application.port.in.OpenAccountCommand;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.IdGenerator;
import com.pecunia.sharedkernel.Money;
import com.pecunia.sharedkernel.UserId;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAccountServiceTest {

    private static final UUID GENERATED = UUID.randomUUID();
    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final Iban IBAN = new Iban("CH9300762011623852957");
    private static final Money INITIAL = Money.chf(new BigDecimal("100.00"));

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private OpenAccountService service;

    @BeforeEach
    void setUp() {
        service = new OpenAccountService(idGenerator, accountRepository, currentUserProvider);
    }

    @Test
    @DisplayName("mints an id, persists an active account carrying the command fields, and returns the id")
    void opens_account() {
        // given
        OpenAccountCommand command = new OpenAccountCommand(AccountType.CURRENT, "Main", Optional.of(IBAN), INITIAL);
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(idGenerator.newId()).thenReturn(GENERATED);

        // when
        AccountId result = service.open(command);

        // then
        assertThat(result).isEqualTo(AccountId.of(GENERATED));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(AccountId.of(GENERATED));
        assertThat(saved.owner()).isEqualTo(OWNER);
        assertThat(saved.type()).isEqualTo(AccountType.CURRENT);
        assertThat(saved.name()).isEqualTo("Main");
        assertThat(saved.iban()).contains(IBAN);
        assertThat(saved.initialBalance()).isEqualTo(INITIAL);
        assertThat(saved.status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("maps an empty IBAN to a credit card without an IBAN")
    void opens_credit_card_without_iban() {
        // given
        OpenAccountCommand command = new OpenAccountCommand(AccountType.CREDIT_CARD, "Visa", Optional.empty(), INITIAL);
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(idGenerator.newId()).thenReturn(GENERATED);

        // when
        service.open(command);

        // then
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().iban()).isEmpty();
    }
}

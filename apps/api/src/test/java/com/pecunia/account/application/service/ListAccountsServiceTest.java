package com.pecunia.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.Money;
import com.pecunia.sharedkernel.UserId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListAccountsServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID());
    private static final Iban IBAN = new Iban("CH9300762011623852957");
    private static final Money INITIAL = Money.chf(new BigDecimal("100.00"));

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private ListAccountsService service;

    @BeforeEach
    void setUp() {
        service = new ListAccountsService(accountRepository, currentUserProvider);
    }

    @Test
    @DisplayName("returns the owner's accounts as views carrying every aggregate field")
    void returns_owned_accounts() {
        // given
        Account account = Account.open(ACCOUNT_ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(accountRepository.findAllByOwner(OWNER)).thenReturn(List.of(account));

        // when
        List<AccountView> result = service.list();

        // then
        assertThat(result).hasSize(1);
        AccountView view = result.getFirst();
        assertThat(view.id()).isEqualTo(ACCOUNT_ID);
        assertThat(view.type()).isEqualTo(AccountType.CURRENT);
        assertThat(view.name()).isEqualTo("Main");
        assertThat(view.iban()).contains(IBAN);
        assertThat(view.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(view.initialBalance()).isEqualTo(INITIAL);
    }

    @Test
    @DisplayName("returns an empty list when the user has no accounts")
    void returns_empty_when_none() {
        // given
        when(currentUserProvider.currentUserId()).thenReturn(OWNER);
        when(accountRepository.findAllByOwner(OWNER)).thenReturn(List.of());

        // when
        List<AccountView> result = service.list();

        // then
        assertThat(result).isEmpty();
    }
}

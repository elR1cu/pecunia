package com.pecunia.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.GetAccountQuery;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountStatus;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.Money;
import com.pecunia.sharedkernel.UserId;
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
class GetAccountServiceTest {

    private static final UserId OWNER = UserId.of(UUID.randomUUID());
    private static final AccountId ACCOUNT_ID = AccountId.of(UUID.randomUUID());
    private static final Iban IBAN = new Iban("CH9300762011623852957");
    private static final Money INITIAL = Money.chf(new BigDecimal("100.00"));

    @Mock
    private AccountRepository accountRepository;

    private GetAccountService service;

    @BeforeEach
    void setUp() {
        service = new GetAccountService(accountRepository);
    }

    @Test
    @DisplayName("maps every aggregate field onto the returned AccountView")
    void returns_account_view() {
        // given
        GetAccountQuery query = new GetAccountQuery(OWNER, ACCOUNT_ID);
        Account account = Account.open(ACCOUNT_ID, OWNER, AccountType.CURRENT, "Main", IBAN, INITIAL);
        when(accountRepository.findByIdAndOwner(ACCOUNT_ID, OWNER)).thenReturn(Optional.of(account));

        // when
        AccountView view = service.getById(query);

        // then
        assertThat(view.id()).isEqualTo(ACCOUNT_ID);
        assertThat(view.type()).isEqualTo(AccountType.CURRENT);
        assertThat(view.name()).isEqualTo("Main");
        assertThat(view.iban()).contains(IBAN);
        assertThat(view.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(view.initialBalance()).isEqualTo(INITIAL);
    }

    @Test
    @DisplayName("throws AccountNotFoundException when the account is absent or not owned")
    void rejects_missing_account() {
        // given
        GetAccountQuery query = new GetAccountQuery(OWNER, ACCOUNT_ID);
        when(accountRepository.findByIdAndOwner(ACCOUNT_ID, OWNER)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.getById(query)).isInstanceOf(AccountNotFoundException.class);
    }
}

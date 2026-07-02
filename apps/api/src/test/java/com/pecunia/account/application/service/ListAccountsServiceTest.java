package com.pecunia.account.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pecunia.account.application.port.in.ListAccountsQuery;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.account.domain.AccountType;
import com.pecunia.account.domain.Iban;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.Money;
import com.pecunia.shared.UserId;
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

    @Mock
    private AccountRepository accountRepository;

    private ListAccountsService service;

    @BeforeEach
    void setUp() {
        service = new ListAccountsService(accountRepository);
    }

    @Test
    @DisplayName("returns the accounts owned by the user")
    void returns_owned_accounts() {
        // given
        ListAccountsQuery query = new ListAccountsQuery(OWNER);
        Account account = Account.open(
                AccountId.of(UUID.randomUUID()),
                OWNER,
                AccountType.CURRENT,
                "Main",
                new Iban("CH9300762011623852957"),
                Money.chf(new BigDecimal("100.00")));
        when(accountRepository.findAllByOwner(OWNER)).thenReturn(List.of(account));

        // when
        List<Account> result = service.list(query);

        // then
        assertThat(result).containsExactly(account);
    }

    @Test
    @DisplayName("returns an empty list when the user has no accounts")
    void returns_empty_when_none() {
        // given
        ListAccountsQuery query = new ListAccountsQuery(OWNER);
        when(accountRepository.findAllByOwner(OWNER)).thenReturn(List.of());

        // when
        List<Account> result = service.list(query);

        // then
        assertThat(result).isEmpty();
    }
}

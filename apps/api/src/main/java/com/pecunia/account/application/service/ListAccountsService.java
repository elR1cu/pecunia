package com.pecunia.account.application.service;

import com.pecunia.account.application.port.in.ListAccounts;
import com.pecunia.account.application.port.in.ListAccountsQuery;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAccountsService implements ListAccounts {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Account> list(ListAccountsQuery query) {
        return accountRepository.findAllByOwner(query.owner());
    }
}

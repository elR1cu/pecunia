package com.pecunia.account.application.service;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.GetAccount;
import com.pecunia.account.application.port.in.GetAccountQuery;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.application.readmodel.AccountView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAccountService implements GetAccount {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountView getById(GetAccountQuery query) {
        return accountRepository
                .findByIdAndOwner(query.accountId(), query.owner())
                .map(AccountView::fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(query.accountId()));
    }
}

package com.pecunia.account.application.service;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.GetAccount;
import com.pecunia.account.application.port.in.GetAccountQuery;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetAccountService implements GetAccount {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public AccountView getById(GetAccountQuery query) {
        UserId owner = currentUserProvider.currentUserId();
        return accountRepository
                .findByIdAndOwner(query.accountId(), owner)
                .map(AccountView::fromAccount)
                .orElseThrow(() -> new AccountNotFoundException(query.accountId()));
    }
}

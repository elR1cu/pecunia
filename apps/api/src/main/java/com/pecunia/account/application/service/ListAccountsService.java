package com.pecunia.account.application.service;

import com.pecunia.account.application.port.in.ListAccounts;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAccountsService implements ListAccounts {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public List<AccountView> list() {
        UserId owner = currentUserProvider.currentUserId();
        return accountRepository.findAllByOwner(owner).stream()
                .map(AccountView::fromAccount)
                .toList();
    }
}

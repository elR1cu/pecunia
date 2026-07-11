package com.pecunia.account.application.service;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.ArchiveAccount;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchiveAccountService implements ArchiveAccount {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void archive(ArchiveAccountCommand command) {
        UserId owner = currentUserProvider.currentUserId();
        Account account = accountRepository
                .findByIdAndOwner(command.accountId(), owner)
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));
        account.archive();
        accountRepository.save(account);
    }
}

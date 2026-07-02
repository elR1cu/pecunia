package com.pecunia.account.application.service;

import com.pecunia.account.application.exception.AccountNotFoundException;
import com.pecunia.account.application.port.in.ArchiveAccount;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArchiveAccountService implements ArchiveAccount {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void archive(ArchiveAccountCommand command) {
        Account account = accountRepository
                .findByIdAndOwner(command.accountId(), command.owner())
                .orElseThrow(() -> new AccountNotFoundException(command.accountId()));
        account.archive();
        accountRepository.save(account);
    }
}

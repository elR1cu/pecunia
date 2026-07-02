package com.pecunia.account.application.service;

import com.pecunia.account.application.port.in.OpenAccount;
import com.pecunia.account.application.port.in.OpenAccountCommand;
import com.pecunia.account.application.port.out.AccountRepository;
import com.pecunia.account.domain.Account;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OpenAccountService implements OpenAccount {

    private final IdGenerator idGenerator;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountId open(OpenAccountCommand command) {
        AccountId id = new AccountId(idGenerator.newId());
        Account account = Account.open(
                id,
                command.owner(),
                command.type(),
                command.name(),
                command.iban().orElse(null),
                command.initialBalance());
        accountRepository.save(account);
        return id;
    }
}

package com.pecunia.account.web;

import com.pecunia.account.application.port.in.ArchiveAccount;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.in.ListAccounts;
import com.pecunia.account.application.port.in.ListAccountsQuery;
import com.pecunia.account.application.port.in.OpenAccount;
import com.pecunia.account.application.port.in.OpenAccountCommand;
import com.pecunia.account.domain.Account;
import com.pecunia.account.web.dto.AccountResponse;
import com.pecunia.account.web.dto.OpenAccountRequest;
import com.pecunia.account.web.generated.AccountApi;
import com.pecunia.account.web.mapper.AccountMapper;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.CurrentUserProvider;
import com.pecunia.shared.UserId;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final ArchiveAccount archiveAccount;
    private final ListAccounts listAccounts;
    private final OpenAccount openAccount;
    private final AccountMapper accountMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ResponseEntity<Void> archiveAccount(UUID accountId) {
        UserId owner = currentUserProvider.currentUserId();
        archiveAccount.archive(new ArchiveAccountCommand(owner, AccountId.of(accountId)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        UserId owner = currentUserProvider.currentUserId();
        List<AccountResponse> accountResponses = listAccounts.list(new ListAccountsQuery(owner)).stream()
                .map(accountMapper::toDto)
                .toList();
        return ResponseEntity.ok(accountResponses);
    }

    @Override
    public ResponseEntity<AccountResponse> openAccount(OpenAccountRequest openAccountRequest) {
        UserId owner = currentUserProvider.currentUserId();
        OpenAccountCommand command = accountMapper.toCommand(openAccountRequest, owner);
        Account account = openAccount.open(command);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{accountId}")
                .buildAndExpand(account.id().value())
                .toUri();
        return ResponseEntity.created(location).body(accountMapper.toDto(account));
    }
}

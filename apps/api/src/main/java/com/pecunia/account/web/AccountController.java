package com.pecunia.account.web;

import com.pecunia.account.application.port.in.ArchiveAccount;
import com.pecunia.account.application.port.in.ArchiveAccountCommand;
import com.pecunia.account.application.port.in.GetAccount;
import com.pecunia.account.application.port.in.GetAccountQuery;
import com.pecunia.account.application.port.in.ListAccounts;
import com.pecunia.account.application.port.in.OpenAccount;
import com.pecunia.account.application.port.in.OpenAccountCommand;
import com.pecunia.account.application.readmodel.AccountView;
import com.pecunia.account.web.dto.AccountResponse;
import com.pecunia.account.web.dto.OpenAccountRequest;
import com.pecunia.account.web.generated.AccountApi;
import com.pecunia.account.web.mapper.AccountMapper;
import com.pecunia.sharedkernel.AccountId;
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
    private final GetAccount getAccount;
    private final AccountMapper accountMapper;

    @Override
    public ResponseEntity<Void> archiveAccount(UUID accountId) {
        archiveAccount.archive(new ArchiveAccountCommand(AccountId.of(accountId)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        List<AccountResponse> accountResponses =
                listAccounts.list().stream().map(accountMapper::toDto).toList();
        return ResponseEntity.ok(accountResponses);
    }

    @Override
    public ResponseEntity<AccountResponse> openAccount(OpenAccountRequest openAccountRequest) {
        OpenAccountCommand command = accountMapper.toCommand(openAccountRequest);
        AccountId accountId = openAccount.open(command);
        AccountView accountView = getAccount.getById(new GetAccountQuery(accountId));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{accountId}")
                .buildAndExpand(accountId.value())
                .toUri();
        return ResponseEntity.created(location).body(accountMapper.toDto(accountView));
    }
}

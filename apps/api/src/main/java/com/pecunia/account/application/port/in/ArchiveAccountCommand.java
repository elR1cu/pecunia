package com.pecunia.account.application.port.in;

import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link ArchiveAccount}. The {@code owner} is resolved from the security context by the web layer. */
public record ArchiveAccountCommand(UserId owner, AccountId accountId) {}

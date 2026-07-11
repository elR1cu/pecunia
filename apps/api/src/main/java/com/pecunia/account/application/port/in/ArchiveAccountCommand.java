package com.pecunia.account.application.port.in;

import com.pecunia.sharedkernel.AccountId;

/** Input for {@link ArchiveAccount}. */
public record ArchiveAccountCommand(AccountId accountId) {}

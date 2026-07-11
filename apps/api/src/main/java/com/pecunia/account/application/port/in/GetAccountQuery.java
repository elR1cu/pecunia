package com.pecunia.account.application.port.in;

import com.pecunia.sharedkernel.AccountId;

/** Input for {@link GetAccount}. */
public record GetAccountQuery(AccountId accountId) {}

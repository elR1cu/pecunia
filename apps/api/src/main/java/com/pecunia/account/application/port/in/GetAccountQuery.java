package com.pecunia.account.application.port.in;

import com.pecunia.sharedkernel.AccountId;
import com.pecunia.sharedkernel.UserId;

/** Input for {@link GetAccount}. */
public record GetAccountQuery(UserId owner, AccountId accountId) {}

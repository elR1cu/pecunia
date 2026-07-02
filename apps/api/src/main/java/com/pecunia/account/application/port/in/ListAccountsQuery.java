package com.pecunia.account.application.port.in;

import com.pecunia.shared.UserId;

/**
 * Input for {@link ListAccounts}. A {@code boolean includeArchived} field can
 * be added later if filtering on status becomes a requirement.
 */
public record ListAccountsQuery(UserId owner) {}

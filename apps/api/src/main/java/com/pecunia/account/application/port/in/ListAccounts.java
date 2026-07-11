package com.pecunia.account.application.port.in;

import com.pecunia.account.application.readmodel.AccountView;
import java.util.List;

/**
 * Driving port: list the accounts owned by a user.
 *
 * <p>A pure read: returns a (possibly empty) list, never a Result (ADR-0027).
 * Returns read models, never the domain aggregate, so nothing outside the
 * application can mutate an account.
 */
public interface ListAccounts {

    List<AccountView> list(ListAccountsQuery query);
}

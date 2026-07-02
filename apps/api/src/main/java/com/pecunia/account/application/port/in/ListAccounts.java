package com.pecunia.account.application.port.in;

import com.pecunia.account.domain.Account;
import java.util.List;

/**
 * Driving port: list the accounts owned by a user.
 *
 * <p>A pure read: returns a (possibly empty) list, never a Result. Returns the
 * {@link Account} aggregate for now; a dedicated read model can be introduced
 * later if the list view diverges from the write model. See ADR-0027.
 */
public interface ListAccounts {

    List<Account> list(ListAccountsQuery query);
}

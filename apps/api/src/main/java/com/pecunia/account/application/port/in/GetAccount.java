package com.pecunia.account.application.port.in;

import com.pecunia.account.application.readmodel.AccountView;

/**
 * Driving port: a single account as a flat read model. Used by the web layer to
 * build the 201 body after an open. Throws {@code AccountNotFoundException}
 * when the account is absent or not owned.
 */
public interface GetAccount {

    AccountView getById(GetAccountQuery query);
}

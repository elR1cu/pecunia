package com.pecunia.account.application.port.out;

import com.pecunia.account.domain.Account;
import com.pecunia.shared.AccountId;
import com.pecunia.shared.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Driven port for account persistence.
 *
 * <p>Multi-tenant by design: there is deliberately no {@code findById(id)}
 * lookup. Forcing the {@code owner} into every read signature makes it
 * impossible to <em>write</em> a query that forgets the ownership filter — the
 * port guarantees isolation, not the developer's vigilance. See ADR-0027 and
 * the multi-tenancy rules in CLAUDE.md.
 */
public interface AccountRepository {

    void save(Account account);

    Optional<Account> findByIdAndOwner(AccountId id, UserId owner);

    List<Account> findAllByOwner(UserId owner);
}

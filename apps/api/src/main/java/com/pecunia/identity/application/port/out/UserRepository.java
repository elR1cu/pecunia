package com.pecunia.identity.application.port.out;

import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.identity.domain.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByIdpIdentity(IdpIdentity idpIdentity);

    /**
     * Idempotent insert-or-ignore. If a row already exists for the user's
     * {@link IdpIdentity} it is left untouched (User is immutable — there is
     * nothing to update, and a concurrent duplicate is a no-op, never an
     * error). Implemented with INSERT ... ON CONFLICT DO NOTHING; see ADR-0029.
     */
    void save(User user);
}

package com.pecunia.sharedinfra.security;

import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the internal {@link UserId} from the {@link PecuniaOidcUser} principal in the security context. */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UserId currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof PecuniaOidcUser user)) {
            throw new IllegalStateException("Expected a PecuniaOidcUser principal");
        }
        return user.userId();
    }
}

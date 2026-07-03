package com.pecunia.security;

import com.pecunia.shared.UserId;
import java.io.Serial;
import java.util.Collection;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

@Accessors(fluent = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class PecuniaOidcUser extends DefaultOidcUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UserId userId;

    public PecuniaOidcUser(
            Collection<? extends GrantedAuthority> authorities,
            OidcIdToken idToken,
            OidcUserInfo userInfo,
            UserId userId) {
        super(authorities, idToken, userInfo);
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
    }
}

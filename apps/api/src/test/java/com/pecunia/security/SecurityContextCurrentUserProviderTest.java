package com.pecunia.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pecunia.shared.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns the internal userId carried by the PecuniaOidcUser principal")
    void returns_userId_from_principal() {
        // given
        UserId expected = UserId.of(UUID.randomUUID());
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(pecuniaOidcUser(expected), null));

        // when
        UserId userId = provider.currentUserId();

        // then
        assertThat(userId).isEqualTo(expected);
    }

    @Test
    @DisplayName("throws when there is no authentication in the context")
    void throws_when_unauthenticated() {
        // given no authentication set

        // when + then
        assertThatThrownBy(provider::currentUserId).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws when the principal is not a PecuniaOidcUser")
    void throws_when_principal_is_not_pecunia_oidc_user() {
        // given
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("someUser", null));

        // when + then
        assertThatThrownBy(provider::currentUserId).isInstanceOf(IllegalStateException.class);
    }

    private static PecuniaOidcUser pecuniaOidcUser(UserId userId) {
        OidcIdToken idToken =
                OidcIdToken.withTokenValue("token").subject("sub-123").build();
        return new PecuniaOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken, null, userId);
    }
}

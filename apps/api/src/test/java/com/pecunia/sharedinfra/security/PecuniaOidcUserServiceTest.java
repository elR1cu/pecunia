package com.pecunia.sharedinfra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.identity.application.port.in.ProvisionUser;
import com.pecunia.identity.application.port.in.ProvisionUserCommand;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.sharedkernel.UserId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class PecuniaOidcUserServiceTest {

    private static final String ISSUER = "https://keycloak.local/realms/pecunia";
    private static final String SUBJECT = "sub-123";

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Mock
    private ProvisionUser provisionUser;

    @Mock
    private OidcUserRequest userRequest;

    private PecuniaOidcUserService service;

    @Test
    @DisplayName("provisions from (iss, sub) and returns a PecuniaOidcUser carrying the internal id")
    void provisions_and_enriches_principal() {
        // given
        service = new PecuniaOidcUserService(delegate, provisionUser);
        OidcIdToken idToken = OidcIdToken.withTokenValue("token")
                .claim(IdTokenClaimNames.ISS, ISSUER)
                .subject(SUBJECT)
                .build();
        OidcUser delegateUser = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        when(delegate.loadUser(userRequest)).thenReturn(delegateUser);
        UserId provisioned = UserId.of(UUID.randomUUID());
        when(provisionUser.provision(any())).thenReturn(provisioned);

        // when
        OidcUser result = service.loadUser(userRequest);

        // then
        assertThat(result).isInstanceOfSatisfying(PecuniaOidcUser.class, principal -> {
            assertThat(principal.userId()).isEqualTo(provisioned);
            assertThat(principal.getSubject()).isEqualTo(SUBJECT);
        });

        ArgumentCaptor<ProvisionUserCommand> captor = ArgumentCaptor.forClass(ProvisionUserCommand.class);
        verify(provisionUser).provision(captor.capture());
        assertThat(captor.getValue().idpIdentity()).isEqualTo(new IdpIdentity(ISSUER, SUBJECT));
    }
}

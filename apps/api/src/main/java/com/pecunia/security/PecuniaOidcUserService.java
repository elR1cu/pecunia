package com.pecunia.security;

import com.pecunia.identity.application.port.in.ProvisionUser;
import com.pecunia.identity.application.port.in.ProvisionUserCommand;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.shared.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PecuniaOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final ProvisionUser provisionUser;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        IdpIdentity idpIdentity = new IdpIdentity(oidcUser.getIssuer().toString(), oidcUser.getSubject());
        UserId userId = provisionUser.provision(new ProvisionUserCommand(idpIdentity));

        return new PecuniaOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), userId);
    }
}

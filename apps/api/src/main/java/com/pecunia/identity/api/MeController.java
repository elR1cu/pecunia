package com.pecunia.identity.api;

import com.pecunia.identity.api.dto.CurrentUser;
import com.pecunia.identity.api.generated.IdentityApi;
import com.pecunia.identity.api.mapper.CurrentUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MeController implements IdentityApi {

    private final CurrentUserMapper currentUserMapper;

    @Override
    public ResponseEntity<CurrentUser> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new IllegalStateException("Expected an OIDC-authenticated principal on /me");
        }
        CurrentUser currentUser = currentUserMapper.toDto(oidcUser);
        log.atInfo().addKeyValue("userId", currentUser.getId()).log("/me resolved");
        return ResponseEntity.ok(currentUser);
    }
}

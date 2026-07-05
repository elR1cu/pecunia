package com.pecunia.identity.web;

import com.pecunia.identity.web.dto.CurrentUser;
import com.pecunia.identity.web.generated.IdentityApi;
import com.pecunia.identity.web.mapper.CurrentUserMapper;
import com.pecunia.sharedkernel.CurrentUserProvider;
import com.pecunia.sharedkernel.UserId;
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
    private final CurrentUserProvider currentUserProvider;

    @Override
    public ResponseEntity<CurrentUser> getCurrentUser() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new IllegalStateException("Expected an OIDC-authenticated principal on /me");
        }
        log.atDebug().addKeyValue("sub", oidcUser.getSubject()).log("/me principal verified");
        UserId userId = currentUserProvider.currentUserId();
        return ResponseEntity.ok(currentUserMapper.toDto(oidcUser, userId));
    }
}

package com.pecunia.identity.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.EMAIL_VERIFIED;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.NAME;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pecunia.identity.web.mapper.CurrentUserMapperImpl;
import com.pecunia.security.PecuniaOidcUserService;
import com.pecunia.security.SecurityConfig;
import com.pecunia.shared.CurrentUserProvider;
import com.pecunia.shared.UserId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import({SecurityConfig.class, CurrentUserMapperImpl.class})
class MeControllerTest {

    private static final String SUBJECT_UUID = "11111111-2222-3333-4444-555555555555";
    private static final UUID INTERNAL_USER_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Autowired
    private MockMvc mockMvc;

    /**
     * Prevents OAuth2ClientAutoConfiguration from building a real ClientRegistrationRepository
     * (which would trigger an OIDC discovery HTTP call to Keycloak). Keeps the slice hermetic.
     */
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    /** Referenced by SecurityConfig's OAuth2 login wiring; not exercised by this slice. */
    @MockitoBean
    private PecuniaOidcUserService pecuniaOidcUserService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    @DisplayName("returns the current user profile with the internal id when authenticated")
    void returnsCurrentUserProfileWhenAuthenticated() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(UserId.of(INTERNAL_USER_ID));

        mockMvc.perform(get("/api/me")
                        .with(oidcLogin()
                                .idToken(token -> token.subject(SUBJECT_UUID)
                                        .claim(PREFERRED_USERNAME, "testuser")
                                        .claim(NAME, "Test User")
                                        .claim(EMAIL, "test@pecunia.local")
                                        .claim(EMAIL_VERIFIED, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INTERNAL_USER_ID.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@pecunia.local"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @DisplayName("falls back to preferred username for display name when the name claim is absent")
    void fallsBackToPreferredUsernameForDisplayNameWhenNameClaimAbsent() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(UserId.of(INTERNAL_USER_ID));

        mockMvc.perform(get("/api/me")
                        .with(oidcLogin()
                                .idToken(token -> token.subject(SUBJECT_UUID)
                                        .claim(PREFERRED_USERNAME, "fallbackuser")
                                        .claim(EMAIL, "fallback@pecunia.local")
                                        .claim(EMAIL_VERIFIED, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("fallbackuser"));
    }

    @Test
    @DisplayName("returns false for emailVerified when the claim is absent")
    void returnsFalseForEmailVerifiedWhenClaimAbsent() throws Exception {
        when(currentUserProvider.currentUserId()).thenReturn(UserId.of(INTERNAL_USER_ID));

        mockMvc.perform(get("/api/me")
                        .with(oidcLogin()
                                .idToken(token -> token.subject(SUBJECT_UUID)
                                        .claim(PREFERRED_USERNAME, "unverifieduser")
                                        .claim(NAME, "Unverified User")
                                        .claim(EMAIL, "unverified@pecunia.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    @DisplayName("redirects browser to login when unauthenticated")
    void redirectsBrowserToLogin() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/pecunia"));
    }

    @Test
    @DisplayName("returns 401 for json request when unauthenticated")
    void returnsUnauthorizedForJsonRequest() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }
}

package com.pecunia.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pecunia.identity.api.mapper.CurrentUserMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({SecurityConfig.class, SwaggerSecurityConfig.class, CurrentUserMapperImpl.class})
class SwaggerSecurityConfigDefaultProfileTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Prevents OAuth2ClientAutoConfiguration from building a real ClientRegistrationRepository
     * (which would trigger an OIDC discovery HTTP call to Keycloak). Keeps the slice hermetic.
     */
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("swagger-ui returns 401 for json request outside dev profile")
    void swaggerUiReturnsUnauthorizedForJsonRequest() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("swagger-ui redirects browser to login outside dev profile")
    void swaggerUiRedirectsBrowserToLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/pecunia"));
    }
}

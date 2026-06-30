package com.pecunia.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pecunia.identity.api.mapper.CurrentUserMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({SecurityConfig.class, SwaggerSecurityConfig.class, CurrentUserMapperImpl.class})
@ActiveProfiles("dev")
class SwaggerSecurityConfigDevProfileTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Prevents OAuth2ClientAutoConfiguration from building a real ClientRegistrationRepository
     * (which would trigger an OIDC discovery HTTP call to Keycloak). Keeps the slice hermetic.
     */
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @ParameterizedTest
    @ValueSource(
            strings = {"/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs/swagger-config", "/openapi.yaml"})
    @DisplayName("security allows public access to swagger paths in dev profile")
    void swaggerPathsBypassSecurity(String path) throws Exception {
        int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 302);
    }

    @Test
    @DisplayName("non-swagger paths still require auth in dev profile")
    void nonSwaggerPathStillProtected() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
    }
}

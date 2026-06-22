package com.pecunia.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

class JsonLogoutRedirectStrategyTest {

    private final JsonLogoutRedirectStrategy strategy = new JsonLogoutRedirectStrategy(new ObjectMapper());

    @Test
    @DisplayName("writes a 200 JSON response carrying the logout URL instead of redirecting")
    void writesJsonLogoutResponse() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        strategy.sendRedirect(request, response, "https://keycloak.local/logout");

        verify(response).setStatus(HttpStatus.OK.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding(StandardCharsets.UTF_8);
        assertThat(body).hasToString("{\"logoutUrl\":\"https://keycloak.local/logout\"}");
    }
}

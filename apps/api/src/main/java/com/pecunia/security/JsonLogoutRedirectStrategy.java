package com.pecunia.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.RedirectStrategy;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
class JsonLogoutRedirectStrategy implements RedirectStrategy {

    private final ObjectMapper mapper;

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);
        mapper.writeValue(response.getWriter(), Map.of("logoutUrl", url));
    }
}

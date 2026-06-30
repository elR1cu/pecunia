package com.pecunia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Dev-only chain. Separated from the main chain to keep production
// config free of Swagger paths and to bypass OAuth2 login on static assets
@Configuration
@Profile("dev")
public class SwaggerSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) {
        return http.securityMatcher(
                        "/swagger-ui/**", "/swagger-ui.html", "/openapi.yaml", "/v3/api-docs/swagger-config")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}

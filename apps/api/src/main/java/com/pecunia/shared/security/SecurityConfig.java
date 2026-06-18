package com.pecunia.shared.security;

import java.util.Set;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OidcClientInitiatedLogoutSuccessHandler oidcClientInitiatedLogoutSuccessHandler,
            AuthenticationEntryPoint authenticationEntryPoint) {
        return http.authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                        .permitAll()
                        .requestMatchers("/login/**", "/oauth2/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // See ADR-0022 for what spa() bundles
                .csrf(CsrfConfigurer::spa)
                .exceptionHandling(eh -> eh.authenticationEntryPoint(authenticationEntryPoint))
                .oauth2Login(oauth2LoginConfigurer -> oauth2LoginConfigurer.defaultSuccessUrl("/dashboard", true))
                .logout(logoutConfigurer ->
                        logoutConfigurer.logoutSuccessHandler(oidcClientInitiatedLogoutSuccessHandler))
                .sessionManagement(sessionManagementConfigurer ->
                        sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    @Bean
    OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint(MediaTypeRequestMatcher jsonRequestMatcher) {
        return DelegatingAuthenticationEntryPoint.builder()
                .addEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), jsonRequestMatcher)
                .defaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/pecunia"))
                .build();
    }

    @Bean
    MediaTypeRequestMatcher jsonRequestMatcher() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON);
        matcher.setUseEquals(true);
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }
}

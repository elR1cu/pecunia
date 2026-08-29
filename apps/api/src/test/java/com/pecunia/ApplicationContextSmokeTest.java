package com.pecunia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Smoke test for the whole application context.
 *
 * <p>Every other test in this module is a slice ({@code @WebMvcTest},
 * {@code @DataJpaTest}, plain unit tests), so nothing else verifies that the
 * beans actually wire together at startup: infrastructure could be misconfigured
 * and the build would stay green. This test closes that gap by starting the real
 * context against a real PostgreSQL.
 *
 * <p>The only bean replaced is {@link ClientRegistrationRepository}. Its
 * autoconfigured implementation performs OIDC discovery against the {@code
 * issuer-uri} while being instantiated, which would require a running Keycloak.
 * The real OIDC flow is covered end-to-end by the Playwright suite; what matters
 * here is that everything around it wires up.
 */
@Testcontainers
// A real servlet container, not MOCK: without one, ServerProperties is never
// bound and `server.servlet.session.timeout` silently falls back to Spring
// Session's own 30-minute default. Starting the server also makes this a
// truthful smoke test -- the filter chain is built for real.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(
        properties = {
            // Resolves the placeholder that application.yml reads from apps/api/.env.
            "PECUNIA_BFF_CLIENT_SECRET=smoke-test-secret",
            "spring.test.database.replace=none"
        })
class ApplicationContextSmokeTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private SessionRepository<? extends Session> sessionRepository;

    @Test
    @DisplayName("HTTP sessions are backed by PostgreSQL, not by an in-memory store")
    void sessions_are_backed_by_postgresql() {
        // then
        // Guards ADR-0038 and, more importantly, the failure mode it inherited from
        // ADR-0011: sessions once silently ran in memory while the configuration
        // claimed an external store. An in-memory fallback would fail here.
        assertThat(sessionRepository).isInstanceOf(JdbcIndexedSessionRepository.class);
    }

    @Test
    @DisplayName("sessions expire after the documented 8 hours of inactivity")
    void sessions_use_the_documented_timeout() {
        // when
        // createSession() only builds the session in memory -- nothing is written
        // until save(), so this leaves no row behind.
        Session session = sessionRepository.createSession();

        // then
        // architecture.md ("Session lifetimes") documents 8 h; this pins the claim to
        // the running configuration so the two cannot drift apart again.
        assertThat(session.getMaxInactiveInterval()).isEqualTo(Duration.ofHours(8));
    }
}

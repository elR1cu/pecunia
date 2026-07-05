package com.pecunia.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.pecunia.identity.application.port.in.ProvisionUser;
import com.pecunia.identity.application.port.in.ProvisionUserCommand;
import com.pecunia.identity.application.port.out.UserRepository;
import com.pecunia.identity.application.service.ProvisionUserService;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.identity.domain.User;
import com.pecunia.sharedinfra.id.Uuidv7IdGenerator;
import com.pecunia.sharedkernel.UserId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration test for the User provisioning slice against a real PostgreSQL
 * (Flyway V1 + V2 applied). Proves the idempotent, concurrency-safe find-or-create
 * of ADR-0029 — which a unit test cannot: the ON CONFLICT DO NOTHING behaviour and
 * the concurrent first-login race only exist against the real database.
 *
 * <p>{@code NOT_SUPPORTED} disables the test-managed transaction so each provisioning
 * call commits for real (required to observe idempotence and to run the true race);
 * the table is cleaned before each test instead of rolled back.
 */
@Testcontainers
@DataJpaTest
@Import({UserRepositoryAdapter.class, ProvisionUserService.class, Uuidv7IdGenerator.class})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserProvisioningIntegrationTest {

    private static final IdpIdentity IDP_IDENTITY =
            new IdpIdentity("https://keycloak.local/realms/pecunia", "sub-it-123");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private ProvisionUser provisionUser;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clean() {
        userJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("finds nothing for an unknown identity")
    void finds_nothing_when_absent() {
        assertThat(userRepository.findByIdpIdentity(IDP_IDENTITY)).isEmpty();
    }

    @Test
    @DisplayName("provisions a new user on first login and persists exactly one row")
    void provisions_new_user() {
        // when
        UserId id = provisionUser.provision(new ProvisionUserCommand(IDP_IDENTITY));

        // then
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByIdpIdentity(IDP_IDENTITY)).get().satisfies(user -> {
            assertThat(user.id()).isEqualTo(id);
            assertThat(user.idpIdentity()).isEqualTo(IDP_IDENTITY);
        });
    }

    @Test
    @DisplayName("is idempotent across sequential calls: same id, single row")
    void is_idempotent_across_calls() {
        // when
        UserId first = provisionUser.provision(new ProvisionUserCommand(IDP_IDENTITY));
        UserId second = provisionUser.provision(new ProvisionUserCommand(IDP_IDENTITY));

        // then
        assertThat(second).isEqualTo(first);
        assertThat(userJpaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("save is a no-op on a conflicting identity: the first id wins (ADR-0029)")
    void save_is_no_op_on_conflict() {
        // given
        UserId first = UserId.of(UUID.randomUUID());
        UserId second = UserId.of(UUID.randomUUID());

        // when — save() drives a @Modifying upsert, so it must run in a transaction
        // (in production the ProvisionUserService supplies one)
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            userRepository.save(User.register(first, IDP_IDENTITY));
            userRepository.save(User.register(second, IDP_IDENTITY)); // ON CONFLICT DO NOTHING
        });

        // then
        assertThat(userJpaRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByIdpIdentity(IDP_IDENTITY))
                .get()
                .extracting(User::id)
                .isEqualTo(first);
    }

    @Test
    @DisplayName("handles concurrent first logins: one row, one id, no exception")
    void handles_concurrent_first_logins() throws Exception {
        // given
        // >1 to force the race; <= the HikariCP pool size (default 10) so every thread runs
        // provision() in parallel instead of blocking on a connection.
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<UserId>> futures = IntStream.range(0, threads)
                    .mapToObj(i -> pool.submit(() -> {
                        start.await();
                        return provisionUser.provision(new ProvisionUserCommand(IDP_IDENTITY));
                    }))
                    .toList();

            // when — release all threads at once
            start.countDown();
            List<UserId> ids = new ArrayList<>();
            for (Future<UserId> future : futures) {
                ids.add(future.get());
            }

            // then — every call returns the same id, and a single row exists
            assertThat(userJpaRepository.count()).isEqualTo(1);
            assertThat(ids).hasSize(threads).containsOnly(ids.getFirst());
        }
    }
}

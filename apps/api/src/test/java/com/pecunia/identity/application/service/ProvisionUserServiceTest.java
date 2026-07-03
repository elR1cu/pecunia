package com.pecunia.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pecunia.identity.application.port.in.ProvisionUserCommand;
import com.pecunia.identity.application.port.out.UserRepository;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.identity.domain.User;
import com.pecunia.shared.IdGenerator;
import com.pecunia.shared.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionUserServiceTest {

    private static final IdpIdentity IDP_IDENTITY =
            new IdpIdentity("https://keycloak.local/realms/pecunia", "sub-abc-123");
    private static final UUID GENERATED = UUID.randomUUID();
    private static final UUID EXISTING = UUID.randomUUID();
    private static final UUID WINNER = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdGenerator idGenerator;

    private ProvisionUserService service;

    @BeforeEach
    void setUp() {
        service = new ProvisionUserService(userRepository, idGenerator);
    }

    @Test
    @DisplayName("returns the existing user's id without generating an id or saving, when already provisioned")
    void returns_existing_user() {
        // given
        ProvisionUserCommand command = new ProvisionUserCommand(IDP_IDENTITY);
        User existing = User.register(UserId.of(EXISTING), IDP_IDENTITY);
        when(userRepository.findByIdpIdentity(IDP_IDENTITY)).thenReturn(Optional.of(existing));

        // when
        UserId id = service.provision(command);

        // then
        assertThat(id).isEqualTo(UserId.of(EXISTING));
        verify(idGenerator, never()).newId();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("mints an id, saves the registered user, and returns the new id on first login")
    void provisions_new_user() {
        // given
        ProvisionUserCommand command = new ProvisionUserCommand(IDP_IDENTITY);
        User persisted = User.register(UserId.of(GENERATED), IDP_IDENTITY);
        when(idGenerator.newId()).thenReturn(GENERATED);
        when(userRepository.findByIdpIdentity(IDP_IDENTITY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(persisted));

        // when
        UserId id = service.provision(command);

        // then
        assertThat(id).isEqualTo(UserId.of(GENERATED));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.id()).isEqualTo(UserId.of(GENERATED));
        assertThat(saved.idpIdentity()).isEqualTo(IDP_IDENTITY);
    }

    @Test
    @DisplayName("returns the concurrent winner's id when the insert was a no-op (ADR-0029)")
    void returns_concurrent_winner_id() {
        // given
        ProvisionUserCommand command = new ProvisionUserCommand(IDP_IDENTITY);
        User winner = User.register(UserId.of(WINNER), IDP_IDENTITY);
        when(idGenerator.newId()).thenReturn(GENERATED); // our candidate, which loses the race
        when(userRepository.findByIdpIdentity(IDP_IDENTITY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));

        // when
        UserId id = service.provision(command);

        // then
        assertThat(id).isEqualTo(UserId.of(WINNER)).isNotEqualTo(UserId.of(GENERATED));
    }

    @Test
    @DisplayName("throws IllegalStateException when no row exists after the idempotent save")
    void throws_when_invariant_violated() {
        // given
        ProvisionUserCommand command = new ProvisionUserCommand(IDP_IDENTITY);
        when(idGenerator.newId()).thenReturn(GENERATED);
        when(userRepository.findByIdpIdentity(IDP_IDENTITY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> service.provision(command)).isInstanceOf(IllegalStateException.class);
    }
}

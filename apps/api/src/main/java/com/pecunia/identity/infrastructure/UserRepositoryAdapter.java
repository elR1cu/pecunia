package com.pecunia.identity.infrastructure;

import com.pecunia.identity.application.port.out.UserRepository;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.identity.domain.User;
import com.pecunia.shared.UserId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByIdpIdentity(IdpIdentity idpIdentity) {
        return userJpaRepository
                .findByIdpIssuerAndIdpSubject(idpIdentity.issuer(), idpIdentity.subject())
                .map(this::toDomain);
    }

    @Override
    public void save(User user) {
        userJpaRepository.insertIfAbsent(
                user.id().value(),
                user.idpIdentity().issuer(),
                user.idpIdentity().subject());
    }

    private User toDomain(UserEntity e) {
        return User.reconstitute(UserId.of(e.getId()), new IdpIdentity(e.getIdpIssuer(), e.getIdpSubject()));
    }
}

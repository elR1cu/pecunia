package com.pecunia.identity.application.service;

import com.pecunia.identity.application.port.in.ProvisionUser;
import com.pecunia.identity.application.port.in.ProvisionUserCommand;
import com.pecunia.identity.application.port.out.UserRepository;
import com.pecunia.identity.domain.IdpIdentity;
import com.pecunia.identity.domain.User;
import com.pecunia.sharedkernel.IdGenerator;
import com.pecunia.sharedkernel.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProvisionUserService implements ProvisionUser {

    private final UserRepository userRepository;
    private final IdGenerator idGenerator;

    @Override
    @Transactional
    public UserId provision(ProvisionUserCommand command) {
        IdpIdentity idpIdentity = command.idpIdentity();
        return userRepository
                .findByIdpIdentity(idpIdentity)
                .orElseGet(() -> createAndReload(idpIdentity))
                .id();
    }

    private User createAndReload(IdpIdentity idpIdentity) {
        UserId newId = UserId.of(idGenerator.newId());
        User newUser = User.register(newId, idpIdentity);
        userRepository.save(newUser);
        return userRepository
                .findByIdpIdentity(idpIdentity)
                .orElseThrow(() -> new IllegalStateException("User must exist after idempotent provisioning save"));
    }
}

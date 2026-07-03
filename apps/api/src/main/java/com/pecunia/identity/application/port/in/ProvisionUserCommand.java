package com.pecunia.identity.application.port.in;

import com.pecunia.identity.domain.IdpIdentity;

public record ProvisionUserCommand(IdpIdentity idpIdentity) {}

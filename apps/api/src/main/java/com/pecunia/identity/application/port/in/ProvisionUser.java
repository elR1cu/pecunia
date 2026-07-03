package com.pecunia.identity.application.port.in;

import com.pecunia.shared.UserId;

public interface ProvisionUser {
    UserId provision(ProvisionUserCommand command);
}

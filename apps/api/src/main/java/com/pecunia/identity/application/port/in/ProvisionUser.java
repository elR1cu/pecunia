package com.pecunia.identity.application.port.in;

import com.pecunia.sharedkernel.UserId;

public interface ProvisionUser {
    UserId provision(ProvisionUserCommand command);
}

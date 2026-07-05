package com.pecunia.sharedkernel;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}

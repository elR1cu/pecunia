package com.pecunia.sharedkernel;

import java.util.Objects;
import java.util.UUID;

public record CategoryId(UUID value) {

    public CategoryId {
        Objects.requireNonNull(value, "CategoryId value must not be null");
    }

    public static CategoryId of(UUID value) {
        return new CategoryId(value);
    }
}

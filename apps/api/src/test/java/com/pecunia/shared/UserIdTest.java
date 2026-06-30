package com.pecunia.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    @DisplayName("of carries the given UUID")
    void of_carries_value() {
        UUID uuid = UUID.randomUUID();

        assertThat(UserId.of(uuid).value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("rejects a null value")
    void rejects_null_value() {
        assertThatNullPointerException().isThrownBy(() -> UserId.of(null)).withMessageContaining("UserId value");
    }
}

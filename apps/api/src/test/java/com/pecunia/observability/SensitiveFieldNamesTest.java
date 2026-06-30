package com.pecunia.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SensitiveFieldNamesTest {

    @ParameterizedTest
    @ValueSource(strings = {"password", "token", "authorization", "iban", "email", "amount", "cvv"})
    @DisplayName("flags blacklisted field names as sensitive")
    void flagsBlacklistedNames(String fieldName) {
        assertThat(SensitiveFieldNames.isSensitive(fieldName)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSWORD", "Iban", "Email", "AuThOrIzAtIoN"})
    @DisplayName("matches the blacklist case-insensitively")
    void matchesCaseInsensitively(String fieldName) {
        assertThat(SensitiveFieldNames.isSensitive(fieldName)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"userPassword", "accountId", "ibanCountry", "username", "level", "traceId", "colour"})
    @DisplayName("does not flag null, unknown, or partial-match names (exact match only)")
    void doesNotFlagNonBlacklistedNames(String fieldName) {
        assertThat(SensitiveFieldNames.isSensitive(fieldName)).isFalse();
    }
}

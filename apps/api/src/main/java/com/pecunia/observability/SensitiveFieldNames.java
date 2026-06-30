package com.pecunia.observability;

import java.util.Locale;
import java.util.Set;

final class SensitiveFieldNames {

    static final String MASK = "***";

    private static final Set<String> BLACKLIST = Set.of(
            // secrets
            "password",
            "passwd",
            "secret",
            "token",
            "authorization",
            "credential",
            "apikey",
            "api_key",
            "cookie",
            // PII / finance (anticipated, Block 2/3)
            "iban",
            "account",
            "card",
            "cvv",
            "ssn",
            "email",
            "phone",
            "address",
            "amount");

    private SensitiveFieldNames() {}

    /** Exact-match (case-insensitive) against the blacklist. {@code userPassword} is NOT masked, only {@code password}. */
    static boolean isSensitive(String fieldName) {
        return fieldName != null && BLACKLIST.contains(fieldName.toLowerCase(Locale.ROOT));
    }
}

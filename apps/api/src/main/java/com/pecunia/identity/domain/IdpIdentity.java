package com.pecunia.identity.domain;

import java.util.Objects;

/**
 * A user's federated identity at an OpenID Connect provider.
 *
 * <p>Per OpenID Connect Core, only the {@code (iss, sub)} pair is a stable,
 * unique identifier for an end-user: {@code sub} is unique only within a given
 * issuer. This pair is the natural key used to match a returning user during
 * just-in-time provisioning.
 */
public record IdpIdentity(String issuer, String subject) {

    public IdpIdentity {
        Objects.requireNonNull(issuer, "issuer cannot be null");
        Objects.requireNonNull(subject, "subject cannot be null");
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer cannot be blank");
        }
        if (subject.isBlank()) {
            throw new IllegalArgumentException("subject cannot be blank");
        }
        issuer = issuer.strip();
        subject = subject.strip();
    }
}

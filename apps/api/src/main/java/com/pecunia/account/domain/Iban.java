package com.pecunia.account.domain;

import com.pecunia.account.domain.exception.InvalidIbanException;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record Iban(String value) {

    // Country code (2 letters) + check digits (2) + BBAN (1..30 alphanumeric).
    private static final Pattern STRUCTURE = Pattern.compile("^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$");
    private static final BigInteger NINETY_SEVEN = BigInteger.valueOf(97);

    public Iban {
        Objects.requireNonNull(value, "Iban value cannot be null");
        value = normalize(value);
        if (!hasValidStructure(value) || !hasValidChecksum(value)) {
            throw new InvalidIbanException();
        }
    }

    private static String normalize(String raw) {
        return raw.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static boolean hasValidStructure(String iban) {
        return STRUCTURE.matcher(iban).matches();
    }

    private static boolean hasValidChecksum(String iban) {
        // ISO 7064 MOD-97-10: move the first four characters to the end,
        // convert each letter to two digits (A=10 .. Z=35), the remainder mod 97 must be 1.
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder(rearranged.length() * 2);
        for (int i = 0; i < rearranged.length(); i++) {
            numeric.append(Character.digit(rearranged.charAt(i), 36));
        }
        return new BigInteger(numeric.toString()).mod(NINETY_SEVEN).equals(BigInteger.ONE);
    }
}

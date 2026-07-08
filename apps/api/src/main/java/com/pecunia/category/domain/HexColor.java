package com.pecunia.category.domain;

import com.pecunia.category.domain.exception.InvalidHexColorException;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record HexColor(String value) {

    private static final Pattern STRUCTURE = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    public HexColor {
        Objects.requireNonNull(value, "Hex color value must not be null");
        value = normalize(value);
        if (!hasValidStructure(value)) {
            throw new InvalidHexColorException(value);
        }
    }

    private static String normalize(String raw) {
        return raw.strip().toUpperCase(Locale.ROOT);
    }

    private static boolean hasValidStructure(String color) {
        return STRUCTURE.matcher(color).matches();
    }
}

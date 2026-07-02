package com.pecunia.account.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pecunia.account.domain.exception.InvalidIbanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IbanTest {

    // Canonical valid Swiss IBAN (21 chars).
    private static final String VALID = "CH9300762011623852957";

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("accepts a structurally valid IBAN with a correct checksum")
        void accepts_valid_iban() {
            assertThat(new Iban(VALID).value()).isEqualTo(VALID);
        }

        @Test
        @DisplayName("rejects a null value")
        void rejects_null() {
            assertThatNullPointerException().isThrownBy(() -> new Iban(null));
        }

        @ParameterizedTest
        @DisplayName("rejects a malformed structure")
        @ValueSource(
                strings = {
                    "CH93", // too short (no BBAN)
                    "1293007620116238529", // country code is not letters
                    "CHXX00762011623852957", // check digits are not digits
                    "CH93-0076-2011-6238", // contains a forbidden separator
                })
        void rejects_bad_structure(String malformed) {
            assertThatThrownBy(() -> new Iban(malformed))
                    .isInstanceOf(InvalidIbanException.class)
                    .hasMessage("IBAN is invalid");
        }

        @Test
        @DisplayName("rejects a well-formed IBAN whose checksum is wrong (transposed digit)")
        void rejects_bad_checksum() {
            // last digit 7 -> 8: structure still valid, checksum now invalid
            String transposed = "CH9300762011623852958";

            assertThatThrownBy(() -> new Iban(transposed))
                    .isInstanceOf(InvalidIbanException.class)
                    .hasMessage("IBAN is invalid");
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("strips whitespace and upper-cases, storing the canonical form")
        void normalizes_to_canonical_form() {
            Iban iban = new Iban("ch93 0076 2011 6238 5295 7");

            assertThat(iban.value()).isEqualTo(VALID);
        }

        @Test
        @DisplayName("two inputs differing only by spacing and case are equal")
        void equality_ignores_spacing_and_case() {
            assertThat(new Iban("ch93 0076 2011 6238 5295 7")).isEqualTo(new Iban(VALID));
        }
    }
}

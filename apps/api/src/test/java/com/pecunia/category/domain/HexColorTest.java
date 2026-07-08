package com.pecunia.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pecunia.category.domain.exception.InvalidHexColorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HexColorTest {

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("accepts a structurally valid #RRGGBB color")
        void accepts_valid_color() {
            assertThat(new HexColor("#1A2B3C").value()).isEqualTo("#1A2B3C");
        }

        @Test
        @DisplayName("rejects a null value")
        void rejects_null() {
            assertThatNullPointerException().isThrownBy(() -> new HexColor(null));
        }

        @ParameterizedTest
        @DisplayName("rejects a malformed structure")
        @ValueSource(
                strings = {
                    "1A2B3C", // missing leading '#'
                    "#1A2B3", // too short (5 hex digits)
                    "#1A2B3CC", // too long (7 hex digits)
                    "#GG0000", // non-hexadecimal characters
                    "#1A2B 3C", // internal whitespace (strip only trims the edges)
                    "#12G", // clearly not a color
                })
        void rejects_bad_structure(String malformed) {
            assertThatThrownBy(() -> new HexColor(malformed))
                    .isInstanceOf(InvalidHexColorException.class)
                    .hasMessageContaining("Invalid hex color");
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("upper-cases the hexadecimal digits")
        void upper_cases_digits() {
            assertThat(new HexColor("#aabbcc").value()).isEqualTo("#AABBCC");
        }

        @Test
        @DisplayName("strips surrounding whitespace")
        void strips_edges() {
            assertThat(new HexColor("  #AABBCC  ").value()).isEqualTo("#AABBCC");
        }

        @Test
        @DisplayName("two inputs differing only by case and edge spacing are equal")
        void equality_ignores_case_and_edge_spacing() {
            assertThat(new HexColor(" #aabbcc ")).isEqualTo(new HexColor("#AABBCC"));
        }
    }
}

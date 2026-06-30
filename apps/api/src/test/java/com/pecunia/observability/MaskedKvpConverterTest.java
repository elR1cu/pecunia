package com.pecunia.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;

class MaskedKvpConverterTest {

    private final MaskedKvpConverter converter = new MaskedKvpConverter();

    private String convert(KeyValuePair... pairs) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getKeyValuePairs()).thenReturn(List.of(pairs));
        return converter.convert(event);
    }

    @Test
    @DisplayName("returns an empty string when the key-value pair list is null")
    void returnsEmptyWhenPairsNull() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getKeyValuePairs()).thenReturn(null);
        assertThat(converter.convert(event)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty string when there are no key-value pairs")
    void returnsEmptyWhenNoPairs() {
        assertThat(convert()).isEmpty();
    }

    @Test
    @DisplayName("renders a non-sensitive pair as key=\"value\"")
    void rendersNonSensitivePair() {
        assertThat(convert(new KeyValuePair("username", "alice"))).isEqualTo("username=\"alice\"");
    }

    @Test
    @DisplayName("masks the value of a sensitive pair")
    void masksSensitiveValue() {
        assertThat(convert(new KeyValuePair("password", "hunter2"))).isEqualTo("password=\"***\"");
    }

    @Test
    @DisplayName("masks case-insensitively while keeping the original key casing")
    void masksCaseInsensitively() {
        assertThat(convert(new KeyValuePair("IBAN", "CH9300762011623852957"))).isEqualTo("IBAN=\"***\"");
    }

    @Test
    @DisplayName("masks only exact blacklist matches, not partial ones")
    void doesNotMaskPartialMatch() {
        assertThat(convert(new KeyValuePair("accountId", "42"))).isEqualTo("accountId=\"42\"");
    }

    @Test
    @DisplayName("joins multiple pairs with a single space and masks selectively")
    void joinsMultiplePairsAndMasksSelectively() {
        String result = convert(
                new KeyValuePair("username", "alice"),
                new KeyValuePair("password", "hunter2"),
                new KeyValuePair("traceId", "abc123"));
        assertThat(result).isEqualTo("username=\"alice\" password=\"***\" traceId=\"abc123\"");
    }
}

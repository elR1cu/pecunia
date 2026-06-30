package com.pecunia.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.json.JsonWriter;

class SensitiveDataLoggingCustomizerTest {

    private JsonWriter.ValueProcessor<Object> processor;

    /**
     * The customizer registers its masking logic by passing a {@link JsonWriter.ValueProcessor} to
     * {@code members.applyingValueProcessor(...)}. We capture that processor and exercise it directly.
     */
    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void captureProcessor() {
        JsonWriter.Members<ILoggingEvent> members = mock(JsonWriter.Members.class);
        new SensitiveDataLoggingCustomizer().customize(members);
        ArgumentCaptor<JsonWriter.ValueProcessor> captor = ArgumentCaptor.forClass(JsonWriter.ValueProcessor.class);
        verify(members).applyingValueProcessor(captor.capture());
        processor = captor.getValue();
    }

    private static JsonWriter.MemberPath named(String name) {
        return new JsonWriter.MemberPath(null, name, JsonWriter.MemberPath.UNINDEXED);
    }

    @Test
    @DisplayName("masks the value of a sensitive field")
    void masksSensitiveField() {
        assertThat(processor.processValue(named("password"), "hunter2")).isEqualTo("***");
    }

    @Test
    @DisplayName("masks case-insensitively")
    void masksCaseInsensitively() {
        assertThat(processor.processValue(named("IBAN"), "CH9300762011623852957"))
                .isEqualTo("***");
    }

    @Test
    @DisplayName("leaves a non-sensitive field untouched")
    void leavesNonSensitiveFieldUntouched() {
        assertThat(processor.processValue(named("message"), "hello")).isEqualTo("hello");
    }

    @Test
    @DisplayName("masks only exact blacklist matches, not partial ones")
    void doesNotMaskPartialMatch() {
        assertThat(processor.processValue(named("accountId"), "42")).isEqualTo("42");
    }

    @Test
    @DisplayName("returns the value unchanged when the field name is empty")
    void returnsValueWhenNameEmpty() {
        assertThat(processor.processValue(named(""), "x")).isEqualTo("x");
    }

    @Test
    @DisplayName("returns the value unchanged when the field name is null")
    void returnsValueWhenNameNull() {
        // A null name is only valid on an indexed (array element) path per MemberPath's invariant.
        JsonWriter.MemberPath arrayElement = new JsonWriter.MemberPath(null, null, 0);
        assertThat(processor.processValue(arrayElement, "x")).isEqualTo("x");
    }
}

package com.pecunia.observability;

import static com.pecunia.observability.SensitiveFieldNames.MASK;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

public final class SensitiveDataLoggingCustomizer implements StructuredLoggingJsonMembersCustomizer<ILoggingEvent> {
    @Override
    public void customize(JsonWriter.Members<ILoggingEvent> members) {
        members.applyingValueProcessor((path, value) -> {
            String fieldName = path.name();
            if (fieldName == null || fieldName.isEmpty()) {
                return value;
            }
            if (SensitiveFieldNames.isSensitive(fieldName)) {
                return MASK;
            }
            return value;
        });
    }
}

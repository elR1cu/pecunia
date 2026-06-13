package com.pecunia.shared.observability;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import org.slf4j.event.KeyValuePair;

import java.util.List;

import static com.pecunia.shared.observability.SensitiveFieldNames.MASK;

public final class MaskedKvpConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        List<KeyValuePair> pairs = event.getKeyValuePairs();
        if (pairs == null || pairs.isEmpty()) {
            return CoreConstants.EMPTY_STRING;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            KeyValuePair kvp = pairs.get(i);
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(kvp.key).append('=').append('"');
            if (SensitiveFieldNames.isSensitive(kvp.key)) {
                sb.append(MASK);
            } else {
                sb.append(kvp.value);
            }
            sb.append('"');
        }
        return sb.toString();
    }
}

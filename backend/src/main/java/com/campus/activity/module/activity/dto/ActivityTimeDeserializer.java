package com.campus.activity.module.activity.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Accept the existing date-only, datetime-local, space-separated and ISO contracts. */
public class ActivityTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter SPACE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (LocalDateTime) context.handleUnexpectedToken(LocalDateTime.class, parser);
        }
        String value = parser.getText().trim();
        if (value.isEmpty()) return null;
        if (value.length() == 16) value += ":00";
        if (value.length() == 10) value += " 00:00:00";
        return value.indexOf(' ') > 0 ? LocalDateTime.parse(value, SPACE_FORMAT) : LocalDateTime.parse(value);
    }
}

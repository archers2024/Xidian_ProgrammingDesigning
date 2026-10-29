package com.campus.events.config;

import com.campus.events.common.TimeSupport;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer offsetDateTimeJson() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        return builder -> {
            builder.serializerByType(OffsetDateTime.class, new JsonSerializer<OffsetDateTime>() {
                @Override
                public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    gen.writeString(value.atZoneSameInstant(TimeSupport.ZONE).format(formatter));
                }
            });
            builder.deserializerByType(OffsetDateTime.class, new JsonDeserializer<OffsetDateTime>() {
                @Override
                public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context)
                        throws IOException {
                    String text = parser.getValueAsString();
                    if (text == null || text.isBlank()) {
                        return null;
                    }
                    return OffsetDateTime.parse(text);
                }
            });
        };
    }
}

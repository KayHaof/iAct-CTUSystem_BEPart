package com.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * The database and service layer use LocalDateTime as a UTC wall-clock value.
 * Keep that legacy storage type, but make the HTTP contract an explicit UTC
 * instant so the frontend cannot apply the offset more than once.
 */
@Configuration
public class UtcDateTimeConfig {

    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    public UtcDateTimeConfig() {
        // Existing LocalDateTime.now() calls must use the same UTC clock as the database.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Bean
    public SimpleModule utcLocalDateTimeModule() {
        SimpleModule module = new SimpleModule("utc-local-date-time");
        module.addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new UtcLocalDateTimeDeserializer());
        return module;
    }

    private static final class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {
            generator.writeString(value.atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    private static final class UtcLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String raw = parser.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return null;
            }

            String normalized = raw.trim().replace(' ', 'T');
            try {
                if (hasOffset(normalized)) {
                    return OffsetDateTime.parse(normalized, ISO_DATE_TIME)
                            .withOffsetSameInstant(UTC)
                            .toLocalDateTime();
                }
                return LocalDateTime.parse(normalized, ISO_LOCAL);
            } catch (DateTimeParseException exception) {
                return (LocalDateTime) context.handleWeirdStringValue(
                        LocalDateTime.class,
                        raw,
                        "Expected an ISO date-time with UTC offset or a legacy UTC LocalDateTime"
                );
            }
        }

        private boolean hasOffset(String value) {
            return value.endsWith("Z") || value.matches(".*[+-]\\d{2}:?\\d{2}$");
        }
    }
}

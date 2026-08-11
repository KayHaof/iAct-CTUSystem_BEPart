package com.example.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** UTC parsing for non-JSON inputs such as query parameters. */
public final class UtcDateTime {

    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_DATE_TIME;

    private UtcDateTime() {
    }

    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Date-time must not be blank");
        }

        String normalized = value.trim().replace(' ', 'T');
        try {
            if (normalized.endsWith("Z") || normalized.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                return OffsetDateTime.parse(normalized, ISO_DATE_TIME)
                        .withOffsetSameInstant(UTC)
                        .toLocalDateTime();
            }
            return LocalDateTime.parse(normalized, ISO_LOCAL);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Expected an ISO date-time with UTC offset, received: " + value,
                    exception
            );
        }
    }

    public static String format(LocalDateTime value) {
        return value == null
                ? null
                : value.atOffset(UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

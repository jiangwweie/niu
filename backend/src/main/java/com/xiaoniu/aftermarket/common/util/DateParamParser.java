package com.xiaoniu.aftermarket.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateParamParser {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private DateParamParser() {}

    public static LocalDateTime parseStartDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseAsLocalDate(value).atStartOfDay();
    }

    public static LocalDateTime parseEndDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDate date = parseAsLocalDate(value);
        return LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 23, 59, 59);
    }

    private static LocalDate parseAsLocalDate(String value) {
        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // not a pure date, try datetime
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(trimmed, DATE_TIME_FMT);
            return dt.toLocalDate();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "日期格式无效，支持 yyyy-MM-dd 或 yyyy-MM-dd'T'HH:mm:ss，收到: " + value);
        }
    }
}

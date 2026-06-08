package com.xiaoniu.aftermarket.common.util;

public final class SearchKeywordUtils {

    private static final char LIKE_ESCAPE_CHAR = '!';
    private static final String LIKE_ESCAPE_SQL = " ESCAPE '!'";

    private SearchKeywordUtils() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String escapeLike(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (ch == LIKE_ESCAPE_CHAR || ch == '%' || ch == '_' || ch == '\\') {
                escaped.append(LIKE_ESCAPE_CHAR);
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }

    public static String buildContainsPattern(String value) {
        String escaped = escapeLike(value);
        return escaped == null ? null : "%" + escaped + "%";
    }

    public static String containsCondition(String columnName) {
        return columnName + " LIKE {0}" + LIKE_ESCAPE_SQL;
    }
}

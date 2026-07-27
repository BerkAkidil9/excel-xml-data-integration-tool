package com.berk.dataintegration.domain;

final class DomainText {
    private DomainText() {
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String optional(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}

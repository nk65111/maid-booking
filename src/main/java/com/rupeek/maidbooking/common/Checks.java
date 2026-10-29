package com.rupeek.maidbooking.common;

public final class Checks {

    private Checks() {
    }

    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    public static <T> T require(T value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
        return value;
    }
}

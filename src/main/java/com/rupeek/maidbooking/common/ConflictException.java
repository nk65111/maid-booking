package com.rupeek.maidbooking.common;

/**
 * The request is valid but clashes with the current state: slot already taken,
 * occurrence already paid/cancelled, idempotency key reused, etc.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

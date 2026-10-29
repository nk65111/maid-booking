package com.rupeek.maidbooking.booking;

/**
 * PENDING_PAYMENT -> CONFIRMED (payment succeeded)
 * PENDING_PAYMENT -> CANCELLED
 * CONFIRMED       -> CANCELLED (refund as per policy)
 * CANCELLED is terminal.
 */
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED
}

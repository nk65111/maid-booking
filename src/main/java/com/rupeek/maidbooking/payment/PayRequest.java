package com.rupeek.maidbooking.payment;

/**
 * occurrenceId is optional: when omitted, the earliest unpaid occurrence is charged, which is
 * what you want for instant/scheduled bookings (they only have one).
 */
public record PayRequest(String occurrenceId, PaymentMethod method, String instrument) {
}

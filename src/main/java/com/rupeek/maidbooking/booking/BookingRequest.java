package com.rupeek.maidbooking.booking;

import java.time.LocalDateTime;

import com.rupeek.maidbooking.maid.ServiceType;

/**
 * startAt is used by SCHEDULED, recurrence by RECURRING, INSTANT needs neither.
 */
public record BookingRequest(String customerId,
                             String maidId,
                             ServiceType serviceType,
                             BookingType type,
                             Integer durationMinutes,
                             LocalDateTime startAt,
                             Recurrence recurrence) {
}

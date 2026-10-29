package com.rupeek.maidbooking.common;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Half-open interval [start, end). Back-to-back slots (10-11 and 11-12) do not overlap.
 */
public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Checks.require(start, "start");
        Checks.require(end, "end");
        if (!end.isAfter(start)) {
            throw new ValidationException("slot end must be after start");
        }
    }

    public static TimeSlot of(LocalDateTime start, int minutes) {
        Checks.require(start, "start");
        return new TimeSlot(start, start.plusMinutes(minutes));
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public long durationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}

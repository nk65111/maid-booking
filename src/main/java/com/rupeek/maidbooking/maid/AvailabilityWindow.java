package com.rupeek.maidbooking.maid;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;

/**
 * A weekly working window, e.g. MONDAY 08:00-13:00. A maid can have several per day.
 */
public record AvailabilityWindow(DayOfWeek day, LocalTime from, LocalTime to) {

    public AvailabilityWindow {
        Checks.require(day, "availability day");
        Checks.require(from, "availability from");
        Checks.require(to, "availability to");
        if (!from.isBefore(to)) {
            throw new ValidationException("availability window on " + day + " must end after it starts");
        }
    }

    public boolean covers(TimeSlot slot) {
        boolean sameDay = slot.start().toLocalDate().equals(slot.end().toLocalDate());
        return sameDay
                && slot.start().getDayOfWeek() == day
                && !slot.start().toLocalTime().isBefore(from)
                && !slot.end().toLocalTime().isAfter(to);
    }

    public boolean overlaps(AvailabilityWindow other) {
        return day == other.day && from.isBefore(other.to) && other.from.isBefore(to);
    }
}

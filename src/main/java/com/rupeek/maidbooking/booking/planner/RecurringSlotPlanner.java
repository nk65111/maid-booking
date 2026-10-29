package com.rupeek.maidbooking.booking.planner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.booking.BookingRequest;
import com.rupeek.maidbooking.booking.BookingType;
import com.rupeek.maidbooking.booking.Recurrence;
import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;

/**
 * Expands the recurrence into one slot per matching day. Dates already in the past
 * (e.g. today's 9am when it is now 10am) are skipped rather than rejected.
 */
@Component
public class RecurringSlotPlanner implements SlotPlanner {

    @Override
    public BookingType type() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeSlot> plan(BookingRequest request, LocalDateTime now) {
        Recurrence recurrence = Checks.require(request.recurrence(), "recurrence");
        LocalDate lastAllowedDate = now.toLocalDate().plusDays(MAX_DAYS_AHEAD);
        if (recurrence.endDate().isAfter(lastAllowedDate)) {
            throw new ValidationException("recurrence can extend at most " + MAX_DAYS_AHEAD + " days ahead");
        }

        List<TimeSlot> slots = recurrence.startDate().datesUntil(recurrence.endDate().plusDays(1))
                .filter(date -> recurrence.days().contains(date.getDayOfWeek()))
                .map(date -> TimeSlot.of(date.atTime(recurrence.time()), request.durationMinutes()))
                .filter(slot -> slot.start().isAfter(now))
                .toList();

        if (slots.isEmpty()) {
            throw new ValidationException("recurrence has no upcoming occurrences");
        }
        return slots;
    }
}

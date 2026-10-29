package com.rupeek.maidbooking.booking.planner;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.booking.BookingRequest;
import com.rupeek.maidbooking.booking.BookingType;
import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;

@Component
public class ScheduledSlotPlanner implements SlotPlanner {

    @Override
    public BookingType type() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeSlot> plan(BookingRequest request, LocalDateTime now) {
        LocalDateTime start = Checks.require(request.startAt(), "startAt");
        if (!start.isAfter(now)) {
            throw new ValidationException("startAt must be in the future");
        }
        if (start.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new ValidationException("bookings can be made at most " + MAX_DAYS_AHEAD + " days ahead");
        }
        return List.of(TimeSlot.of(start, request.durationMinutes()));
    }
}

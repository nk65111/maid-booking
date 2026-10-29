package com.rupeek.maidbooking.booking.planner;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.booking.BookingRequest;
import com.rupeek.maidbooking.booking.BookingType;
import com.rupeek.maidbooking.common.TimeSlot;

@Component
public class InstantSlotPlanner implements SlotPlanner {

    @Override
    public BookingType type() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeSlot> plan(BookingRequest request, LocalDateTime now) {
        return List.of(TimeSlot.of(now.truncatedTo(ChronoUnit.MINUTES), request.durationMinutes()));
    }
}

package com.rupeek.maidbooking.booking.planner;

import java.time.LocalDateTime;
import java.util.List;

import com.rupeek.maidbooking.booking.BookingRequest;
import com.rupeek.maidbooking.booking.BookingType;
import com.rupeek.maidbooking.common.TimeSlot;

/**
 * Turns a booking request into the concrete slots it needs. This is the only thing that
 * differs between booking types: a new type is a new enum value plus one implementation.
 */
public interface SlotPlanner {

    int MAX_DAYS_AHEAD = 90;

    BookingType type();

    List<TimeSlot> plan(BookingRequest request, LocalDateTime now);
}

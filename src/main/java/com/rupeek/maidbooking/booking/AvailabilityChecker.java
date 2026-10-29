package com.rupeek.maidbooking.booking;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.maid.Maid;

/**
 * A slot is free when it falls inside one of the maid's working windows and does not
 * overlap any of her non-cancelled occurrences. Shared by search and booking so both
 * agree on what "available" means.
 */
@Component
public class AvailabilityChecker {

    private final BookingRepository bookings;

    public AvailabilityChecker(BookingRepository bookings) {
        this.bookings = bookings;
    }

    public boolean isFree(Maid maid, TimeSlot slot) {
        return unavailableSlots(maid, List.of(slot)).isEmpty();
    }

    public List<TimeSlot> unavailableSlots(Maid maid, List<TimeSlot> requested) {
        List<TimeSlot> busy = bookings.findActiveOccurrences(maid.id()).stream()
                .map(Occurrence::getSlot)
                .toList();
        return requested.stream()
                .filter(slot -> !maid.worksDuring(slot) || busy.stream().anyMatch(slot::overlaps))
                .toList();
    }
}

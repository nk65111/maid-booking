package com.rupeek.maidbooking.booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);

    /** Occurrences of the maid that still hold a slot, i.e. not cancelled. */
    List<Occurrence> findActiveOccurrences(String maidId);
}

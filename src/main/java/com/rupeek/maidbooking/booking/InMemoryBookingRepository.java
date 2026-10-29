package com.rupeek.maidbooking.booking;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public List<Occurrence> findActiveOccurrences(String maidId) {
        return bookings.values().stream()
                .filter(booking -> booking.getMaidId().equals(maidId))
                .flatMap(booking -> booking.getOccurrences().stream())
                .filter(Occurrence::isActive)
                .toList();
    }
}

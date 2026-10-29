package com.rupeek.maidbooking.booking;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rupeek.maidbooking.booking.planner.SlotPlanner;
import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.KeyedLocks;
import com.rupeek.maidbooking.common.NotFoundException;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.MaidService;
import com.rupeek.maidbooking.pricing.PricingStrategy;

@Service
public class BookingService {

    private final MaidService maidService;
    private final BookingRepository bookings;
    private final AvailabilityChecker availability;
    private final Map<BookingType, SlotPlanner> planners;
    private final PricingStrategy pricing;
    private final KeyedLocks locks;
    private final Clock clock;

    public BookingService(MaidService maidService, BookingRepository bookings, AvailabilityChecker availability,
                          List<SlotPlanner> planners, PricingStrategy pricing, KeyedLocks locks, Clock clock) {
        this.maidService = maidService;
        this.bookings = bookings;
        this.availability = availability;
        this.planners = planners.stream().collect(Collectors.toMap(SlotPlanner::type, Function.identity()));
        this.pricing = pricing;
        this.locks = locks;
        this.clock = clock;
    }

    public Booking book(BookingRequest request) {
        String customerId = Checks.requireText(request.customerId(), "customerId");
        Checks.requireText(request.maidId(), "maidId");
        Checks.require(request.serviceType(), "serviceType");
        Booking.requireValidDuration(request.durationMinutes());

        Maid maid = maidService.get(request.maidId());
        if (!maid.offers(request.serviceType())) {
            throw new ValidationException(maid.name() + " does not offer " + request.serviceType());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        List<TimeSlot> slots = plannerFor(request.type()).plan(request, now);

        // check-then-save must be atomic per maid, otherwise two customers can grab the same slot
        return locks.withLock("maid:" + maid.id(), () -> {
            ensureAvailable(maid, slots);
            List<Occurrence> occurrences = slots.stream()
                    .map(slot -> new Occurrence(UUID.randomUUID().toString(), slot,
                            pricing.price(maid, request.serviceType(), slot)))
                    .toList();
            return bookings.save(new Booking(UUID.randomUUID().toString(), customerId, maid.id(),
                    request.serviceType(), request.type(), occurrences, now));
        });
    }

    public Booking get(String bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
    }

    /**
     * Payment and cancellation both mutate occurrences of a booking; running them under the
     * same lock stops races like "pay and cancel at the same time".
     */
    public <T> T withLockedBooking(String bookingId, Function<Booking, T> action) {
        return locks.withLock("booking:" + bookingId, () -> action.apply(get(bookingId)));
    }

    private SlotPlanner plannerFor(BookingType type) {
        Checks.require(type, "type");
        SlotPlanner planner = planners.get(type);
        if (planner == null) {
            throw new ValidationException("booking type " + type + " is not supported");
        }
        return planner;
    }

    // a recurring series is all-or-nothing: if any occurrence clashes, nothing is booked
    private void ensureAvailable(Maid maid, List<TimeSlot> slots) {
        List<TimeSlot> unavailable = availability.unavailableSlots(maid, slots);
        if (!unavailable.isEmpty()) {
            String when = unavailable.stream()
                    .map(slot -> slot.start().toString())
                    .collect(Collectors.joining(", "));
            throw new ConflictException(maid.name() + " is not available at: " + when);
        }
    }
}

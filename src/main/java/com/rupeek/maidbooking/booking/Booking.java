package com.rupeek.maidbooking.booking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.NotFoundException;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.ServiceType;

public class Booking {

    public static final int MIN_DURATION_MINUTES = 30;
    public static final int MAX_DURATION_MINUTES = 8 * 60;

    private final String id;
    private final String customerId;
    private final String maidId;
    private final ServiceType serviceType;
    private final BookingType type;
    private final List<Occurrence> occurrences;
    private final LocalDateTime createdAt;

    public Booking(String id, String customerId, String maidId, ServiceType serviceType, BookingType type,
                   List<Occurrence> occurrences, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.maidId = maidId;
        this.serviceType = serviceType;
        this.type = type;
        this.occurrences = List.copyOf(occurrences);
        this.createdAt = createdAt;
    }

    public static int requireValidDuration(Integer minutes) {
        Checks.require(minutes, "durationMinutes");
        if (minutes < MIN_DURATION_MINUTES || minutes > MAX_DURATION_MINUTES) {
            throw new ValidationException("durationMinutes must be between "
                    + MIN_DURATION_MINUTES + " and " + MAX_DURATION_MINUTES);
        }
        return minutes;
    }

    /**
     * Derived from occurrences: cancelled only when every occurrence is cancelled,
     * confirmed when every remaining occurrence is paid.
     */
    public BookingStatus getStatus() {
        List<Occurrence> active = occurrences.stream().filter(Occurrence::isActive).toList();
        if (active.isEmpty()) {
            return BookingStatus.CANCELLED;
        }
        boolean allPaid = active.stream().allMatch(o -> o.getStatus() == BookingStatus.CONFIRMED);
        return allPaid ? BookingStatus.CONFIRMED : BookingStatus.PENDING_PAYMENT;
    }

    public Occurrence occurrence(String occurrenceId) {
        return occurrences.stream()
                .filter(o -> o.getId().equals(occurrenceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Occurrence " + occurrenceId + " not found in booking " + id));
    }

    public Optional<Occurrence> nextUnpaidOccurrence() {
        return occurrences.stream()
                .filter(o -> o.getStatus() == BookingStatus.PENDING_PAYMENT)
                .min(Comparator.comparing(o -> o.getSlot().start()));
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getMaidId() {
        return maidId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public BookingType getType() {
        return type;
    }

    public List<Occurrence> getOccurrences() {
        return occurrences;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

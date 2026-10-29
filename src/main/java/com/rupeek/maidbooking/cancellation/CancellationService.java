package com.rupeek.maidbooking.cancellation;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.booking.BookingService;
import com.rupeek.maidbooking.booking.BookingStatus;
import com.rupeek.maidbooking.booking.Occurrence;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.payment.PaymentService;

/**
 * Cancelling an occurrence marks it CANCELLED, which is all it takes to free the maid's slot:
 * availability only counts active occurrences.
 */
@Service
public class CancellationService {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationPolicy policy;
    private final Clock clock;

    public CancellationService(BookingService bookingService, PaymentService paymentService,
                               CancellationPolicy policy, Clock clock) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.policy = policy;
        this.clock = clock;
    }

    public Booking cancelOccurrence(String bookingId, String occurrenceId) {
        return bookingService.withLockedBooking(bookingId, booking -> {
            Occurrence occurrence = booking.occurrence(occurrenceId);
            LocalDateTime now = LocalDateTime.now(clock);
            if (occurrence.hasEndedBy(now)) {
                throw new ConflictException("Occurrence " + occurrenceId + " is already over");
            }
            cancel(occurrence, now);
            return booking;
        });
    }

    /**
     * Cancels every occurrence that is still upcoming or in progress. Past ones are left alone.
     */
    public Booking cancelBooking(String bookingId) {
        return bookingService.withLockedBooking(bookingId, booking -> {
            LocalDateTime now = LocalDateTime.now(clock);
            List<Occurrence> cancellable = booking.getOccurrences().stream()
                    .filter(o -> o.isActive() && !o.hasEndedBy(now))
                    .toList();
            if (cancellable.isEmpty()) {
                throw new ConflictException("Booking " + bookingId + " has nothing left to cancel");
            }
            cancellable.forEach(occurrence -> cancel(occurrence, now));
            return booking;
        });
    }

    // refund first: if the gateway refuses, the occurrence stays as it was
    private void cancel(Occurrence occurrence, LocalDateTime now) {
        BigDecimal refund = BigDecimal.ZERO;
        if (occurrence.getStatus() == BookingStatus.CONFIRMED) {
            refund = policy.refundFor(occurrence.getAmount(), Duration.between(now, occurrence.getSlot().start()));
            if (refund.signum() > 0) {
                paymentService.refund(occurrence.getPaymentId(), refund);
            }
        }
        occurrence.cancel(refund);
    }
}

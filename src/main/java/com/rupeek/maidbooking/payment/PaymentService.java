package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.booking.BookingService;
import com.rupeek.maidbooking.booking.Occurrence;
import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.KeyedLocks;
import com.rupeek.maidbooking.common.NotFoundException;
import com.rupeek.maidbooking.common.ValidationException;

@Service
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository payments;
    private final Map<PaymentMethod, PaymentProcessor> processors;
    private final KeyedLocks locks;
    private final Clock clock;

    public PaymentService(BookingService bookingService, PaymentRepository payments,
                          List<PaymentProcessor> processors, KeyedLocks locks, Clock clock) {
        this.bookingService = bookingService;
        this.payments = payments;
        this.processors = processors.stream().collect(Collectors.toMap(PaymentProcessor::method, Function.identity()));
        this.locks = locks;
        this.clock = clock;
    }

    /**
     * Same idempotency key + same request returns the original payment (success or failure)
     * without charging again. To retry a failed payment the client sends a new key.
     */
    public Payment pay(String bookingId, PayRequest request, String idempotencyKey) {
        String key = Checks.requireText(idempotencyKey, "Idempotency-Key header");
        PaymentProcessor processor = processorFor(request.method());

        return locks.withLock("payment-key:" + key, () -> payments.findByIdempotencyKey(key)
                .map(existing -> replay(existing, bookingId, request))
                .orElseGet(() -> bookingService.withLockedBooking(bookingId,
                        booking -> charge(booking, request, processor, key))));
    }

    public void refund(String paymentId, BigDecimal amount) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment " + paymentId + " not found"));
        GatewayResponse response = processorFor(payment.getMethod()).refund(payment.getGatewayReference(), amount);
        if (!response.success()) {
            throw new ConflictException("Refund for payment " + paymentId + " failed: " + response.message());
        }
        payment.recordRefund(amount);
        payments.save(payment);
    }

    private Payment charge(Booking booking, PayRequest request, PaymentProcessor processor, String key) {
        Occurrence occurrence = request.occurrenceId() == null
                ? booking.nextUnpaidOccurrence().orElseThrow(
                        () -> new ConflictException("Booking " + booking.getId() + " has nothing left to pay"))
                : booking.occurrence(request.occurrenceId());
        LocalDateTime now = LocalDateTime.now(clock);
        occurrence.requirePayable(now);

        GatewayResponse response = processor.charge(request.instrument(), occurrence.getAmount(), occurrence.getId());
        Payment payment = new Payment(UUID.randomUUID().toString(), booking.getId(), occurrence.getId(),
                processor.method(), occurrence.getAmount(), response, key, now);
        if (payment.isSuccessful()) {
            occurrence.confirm(payment.getId());
        }
        return payments.save(payment);
    }

    private Payment replay(Payment existing, String bookingId, PayRequest request) {
        boolean sameRequest = existing.getBookingId().equals(bookingId)
                && existing.getMethod() == request.method()
                && (request.occurrenceId() == null || Objects.equals(existing.getOccurrenceId(), request.occurrenceId()));
        if (!sameRequest) {
            throw new ConflictException("Idempotency key was already used for a different payment");
        }
        return existing;
    }

    private PaymentProcessor processorFor(PaymentMethod method) {
        Checks.require(method, "method");
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new ValidationException("payment method " + method + " is not supported");
        }
        return processor;
    }
}

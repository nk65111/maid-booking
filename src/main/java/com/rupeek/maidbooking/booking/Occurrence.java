package com.rupeek.maidbooking.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.TimeSlot;

/**
 * One visit of the maid. Instant and scheduled bookings have exactly one occurrence,
 * a recurring booking has one per repetition. Payment and cancellation work per occurrence.
 */
public class Occurrence {

    private final String id;
    private final TimeSlot slot;
    private final BigDecimal amount;
    private volatile BookingStatus status = BookingStatus.PENDING_PAYMENT;
    private String paymentId;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    public Occurrence(String id, TimeSlot slot, BigDecimal amount) {
        this.id = id;
        this.slot = slot;
        this.amount = amount;
    }

    public void requirePayable(LocalDateTime now) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Occurrence " + id + " is " + status + " and cannot be paid");
        }
        if (hasEndedBy(now)) {
            throw new ConflictException("Occurrence " + id + " is already over and cannot be paid");
        }
    }

    public void confirm(String paymentId) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new ConflictException("Occurrence " + id + " is " + status + " and cannot be paid");
        }
        this.paymentId = paymentId;
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel(BigDecimal refund) {
        if (status == BookingStatus.CANCELLED) {
            throw new ConflictException("Occurrence " + id + " is already cancelled");
        }
        this.refundedAmount = refund;
        this.status = BookingStatus.CANCELLED;
    }

    public boolean isActive() {
        return status != BookingStatus.CANCELLED;
    }

    public boolean hasEndedBy(LocalDateTime now) {
        return !slot.end().isAfter(now);
    }

    public String getId() {
        return id;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }
}

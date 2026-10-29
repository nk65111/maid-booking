package com.rupeek.maidbooking.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.rupeek.maidbooking.common.ConflictException;

/**
 * A charge for exactly one occurrence. Recurring bookings are paid occurrence by occurrence,
 * which keeps refunds simple: cancelling one visit refunds one payment.
 */
public class Payment {

    private final String id;
    private final String bookingId;
    private final String occurrenceId;
    private final PaymentMethod method;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final String gatewayReference;
    private final String failureReason;
    private final String idempotencyKey;
    private final LocalDateTime createdAt;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    public Payment(String id, String bookingId, String occurrenceId, PaymentMethod method, BigDecimal amount,
                   GatewayResponse response, String idempotencyKey, LocalDateTime createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.occurrenceId = occurrenceId;
        this.method = method;
        this.amount = amount;
        this.status = response.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        this.gatewayReference = response.reference();
        this.failureReason = response.success() ? null : response.message();
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public void recordRefund(BigDecimal refund) {
        if (status != PaymentStatus.SUCCESS) {
            throw new ConflictException("Payment " + id + " did not succeed, nothing to refund");
        }
        if (refundedAmount.add(refund).compareTo(amount) > 0) {
            throw new ConflictException("Refund exceeds the amount paid for payment " + id);
        }
        refundedAmount = refundedAmount.add(refund);
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    public String getId() {
        return id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getOccurrenceId() {
        return occurrenceId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }
}

package com.rupeek.maidbooking.cancellation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * 24h or more before start: full refund. 2h to 24h: half. Less than 2h or already started: nothing.
 */
@Component
public class TimeBasedCancellationPolicy implements CancellationPolicy {

    private static final Duration FULL_REFUND_NOTICE = Duration.ofHours(24);
    private static final Duration PARTIAL_REFUND_NOTICE = Duration.ofHours(2);
    private static final BigDecimal PARTIAL_REFUND_SHARE = new BigDecimal("0.5");

    @Override
    public BigDecimal refundFor(BigDecimal paidAmount, Duration timeBeforeStart) {
        if (timeBeforeStart.compareTo(FULL_REFUND_NOTICE) >= 0) {
            return paidAmount;
        }
        if (timeBeforeStart.compareTo(PARTIAL_REFUND_NOTICE) >= 0) {
            return paidAmount.multiply(PARTIAL_REFUND_SHARE).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}

package com.rupeek.maidbooking.cancellation;

import java.math.BigDecimal;
import java.time.Duration;

public interface CancellationPolicy {

    /**
     * @param timeBeforeStart negative if the occurrence has already started
     */
    BigDecimal refundFor(BigDecimal paidAmount, Duration timeBeforeStart);
}

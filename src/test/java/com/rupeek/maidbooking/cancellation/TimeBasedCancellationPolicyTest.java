package com.rupeek.maidbooking.cancellation;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class TimeBasedCancellationPolicyTest {

    private static final BigDecimal PAID = new BigDecimal("300.00");

    private final TimeBasedCancellationPolicy policy = new TimeBasedCancellationPolicy();

    @Test
    void fullRefundWithAtLeastADaysNotice() {
        assertThat(policy.refundFor(PAID, Duration.ofHours(24))).isEqualByComparingTo("300");
        assertThat(policy.refundFor(PAID, Duration.ofDays(5))).isEqualByComparingTo("300");
    }

    @Test
    void halfRefundBetweenTwoAndTwentyFourHours() {
        assertThat(policy.refundFor(PAID, Duration.ofHours(24).minusMinutes(1))).isEqualByComparingTo("150");
        assertThat(policy.refundFor(PAID, Duration.ofHours(2))).isEqualByComparingTo("150");
    }

    @Test
    void noRefundForLateCancellationOrAfterStart() {
        assertThat(policy.refundFor(PAID, Duration.ofMinutes(119))).isEqualByComparingTo("0");
        assertThat(policy.refundFor(PAID, Duration.ofMinutes(-30))).isEqualByComparingTo("0");
    }
}

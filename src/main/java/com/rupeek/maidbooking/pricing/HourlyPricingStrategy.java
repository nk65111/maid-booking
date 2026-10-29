package com.rupeek.maidbooking.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.ServiceType;

@Component
public class HourlyPricingStrategy implements PricingStrategy {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    @Override
    public BigDecimal price(Maid maid, ServiceType service, TimeSlot slot) {
        return maid.hourlyRate(service)
                .multiply(BigDecimal.valueOf(slot.durationMinutes()))
                .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}

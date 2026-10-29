package com.rupeek.maidbooking.pricing;

import java.math.BigDecimal;

import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.ServiceType;

/**
 * Swap this bean to introduce surge / weekend / dynamic pricing without touching booking code.
 */
public interface PricingStrategy {

    BigDecimal price(Maid maid, ServiceType service, TimeSlot slot);
}

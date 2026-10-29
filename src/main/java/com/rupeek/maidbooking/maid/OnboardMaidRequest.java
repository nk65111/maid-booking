package com.rupeek.maidbooking.maid;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record OnboardMaidRequest(String name,
                                 String phone,
                                 String locality,
                                 Gender gender,
                                 Map<ServiceType, BigDecimal> hourlyRates,
                                 List<AvailabilityWindow> availability,
                                 Double rating) {
}

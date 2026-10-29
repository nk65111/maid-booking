package com.rupeek.maidbooking.maid;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;

/**
 * The services a maid offers are the keys of {@code hourlyRates}, so skills and pricing
 * can never drift apart. Rating is optional: a newly onboarded maid has none yet.
 */
public record Maid(String id,
                   String name,
                   String phone,
                   String locality,
                   Gender gender,
                   Map<ServiceType, BigDecimal> hourlyRates,
                   List<AvailabilityWindow> availability,
                   Double rating) {

    public Maid {
        Checks.requireText(id, "id");
        name = Checks.requireText(name, "name");
        phone = Checks.requireText(phone, "phone");
        if (!phone.matches("\\d{10}")) {
            throw new ValidationException("phone must be 10 digits");
        }
        locality = Checks.requireText(locality, "locality");
        Checks.require(gender, "gender");
        hourlyRates = validRates(hourlyRates);
        availability = validAvailability(availability);
        if (rating != null && (rating < 0 || rating > 5)) {
            throw new ValidationException("rating must be between 0 and 5");
        }
    }

    public boolean offers(ServiceType service) {
        return hourlyRates.containsKey(service);
    }

    public BigDecimal hourlyRate(ServiceType service) {
        BigDecimal rate = hourlyRates.get(service);
        if (rate == null) {
            throw new ValidationException(name + " does not offer " + service);
        }
        return rate;
    }

    public boolean worksDuring(TimeSlot slot) {
        return availability.stream().anyMatch(window -> window.covers(slot));
    }

    private static Map<ServiceType, BigDecimal> validRates(Map<ServiceType, BigDecimal> rates) {
        if (rates == null || rates.isEmpty()) {
            throw new ValidationException("at least one service with an hourly rate is required");
        }
        rates.forEach((service, rate) -> {
            if (service == null || rate == null || rate.signum() <= 0) {
                throw new ValidationException("hourly rate for " + service + " must be positive");
            }
        });
        return Collections.unmodifiableMap(new EnumMap<>(rates));
    }

    private static List<AvailabilityWindow> validAvailability(List<AvailabilityWindow> windows) {
        if (windows == null || windows.isEmpty()) {
            throw new ValidationException("at least one availability window is required");
        }
        if (windows.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("availability windows cannot be null");
        }
        for (int i = 0; i < windows.size(); i++) {
            for (int j = i + 1; j < windows.size(); j++) {
                if (windows.get(i).overlaps(windows.get(j))) {
                    throw new ValidationException("availability windows overlap on " + windows.get(i).day());
                }
            }
        }
        return List.copyOf(windows);
    }
}

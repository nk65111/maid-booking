package com.rupeek.maidbooking.search;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.maid.Gender;
import com.rupeek.maidbooking.maid.ServiceType;

/**
 * Every field is optional; a filter whose field is empty lets every maid through.
 */
public record SearchCriteria(String locality,
                             ServiceType serviceType,
                             LocalDateTime start,
                             Integer durationMinutes,
                             Double minRating,
                             BigDecimal maxPrice,
                             Set<ServiceType> skills,
                             Gender gender) {

    static final int DEFAULT_DURATION_MINUTES = 60;

    public Optional<TimeSlot> slot() {
        if (start == null) {
            return Optional.empty();
        }
        int minutes = durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes;
        return Optional.of(TimeSlot.of(start, minutes));
    }
}

package com.rupeek.maidbooking.search;

import java.math.BigDecimal;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rupeek.maidbooking.booking.AvailabilityChecker;

/**
 * The simple attribute filters are one-liners, so they live together here instead of seven
 * near-empty classes. Anything bigger should get its own class implementing MaidFilter.
 */
@Configuration
public class Filters {

    @Bean
    public MaidFilter localityFilter() {
        return (maid, c) -> c.locality() == null || c.locality().isBlank()
                || maid.locality().equalsIgnoreCase(c.locality().trim());
    }

    @Bean
    public MaidFilter serviceTypeFilter() {
        return (maid, c) -> c.serviceType() == null || maid.offers(c.serviceType());
    }

    @Bean
    public MaidFilter skillsFilter() {
        return (maid, c) -> c.skills() == null || maid.hourlyRates().keySet().containsAll(c.skills());
    }

    @Bean
    public MaidFilter genderFilter() {
        return (maid, c) -> c.gender() == null || maid.gender() == c.gender();
    }

    @Bean
    public MaidFilter ratingFilter() {
        return (maid, c) -> c.minRating() == null || (maid.rating() != null && maid.rating() >= c.minRating());
    }

    /** Compares the rate of the requested service, or the cheapest service when none is given. */
    @Bean
    public MaidFilter priceFilter() {
        return (maid, c) -> {
            if (c.maxPrice() == null) {
                return true;
            }
            BigDecimal rate = c.serviceType() != null
                    ? maid.hourlyRates().get(c.serviceType())
                    : Collections.min(maid.hourlyRates().values());
            return rate != null && rate.compareTo(c.maxPrice()) <= 0;
        };
    }

    @Bean
    public MaidFilter availabilityFilter(AvailabilityChecker availability) {
        return (maid, c) -> c.slot().map(slot -> availability.isFree(maid, slot)).orElse(true);
    }
}

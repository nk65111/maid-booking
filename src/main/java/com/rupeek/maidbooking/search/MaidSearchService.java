package com.rupeek.maidbooking.search;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.MaidRepository;

@Service
public class MaidSearchService {

    private final MaidRepository maids;
    private final List<MaidFilter> filters;
    private final Clock clock;

    public MaidSearchService(MaidRepository maids, List<MaidFilter> filters, Clock clock) {
        this.maids = maids;
        this.filters = filters;
        this.clock = clock;
    }

    /**
     * Best rated first, unrated maids last.
     */
    public List<Maid> search(SearchCriteria criteria) {
        validate(criteria);
        return maids.findAll().stream()
                .filter(maid -> filters.stream().allMatch(filter -> filter.matches(maid, criteria)))
                .sorted(Comparator.comparing(Maid::rating, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void validate(SearchCriteria criteria) {
        if (criteria.durationMinutes() != null) {
            Booking.requireValidDuration(criteria.durationMinutes());
        }
        if (criteria.start() != null && criteria.start().isBefore(LocalDateTime.now(clock))) {
            throw new ValidationException("start must not be in the past");
        }
        if (criteria.minRating() != null && (criteria.minRating() < 0 || criteria.minRating() > 5)) {
            throw new ValidationException("minRating must be between 0 and 5");
        }
        if (criteria.maxPrice() != null && criteria.maxPrice().signum() < 0) {
            throw new ValidationException("maxPrice must not be negative");
        }
    }
}

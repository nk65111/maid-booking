package com.rupeek.maidbooking.booking;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

import com.rupeek.maidbooking.common.Checks;
import com.rupeek.maidbooking.common.ValidationException;

/**
 * "Every MON-FRI at 09:00 from 1 Nov to 30 Nov". An end date is mandatory so a series
 * is always finite and can be checked for clashes up front.
 */
public record Recurrence(Set<DayOfWeek> days, LocalTime time, LocalDate startDate, LocalDate endDate) {

    public Recurrence {
        if (days == null || days.isEmpty() || days.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("recurrence needs at least one day of the week");
        }
        Checks.require(time, "recurrence time");
        Checks.require(startDate, "recurrence startDate");
        Checks.require(endDate, "recurrence endDate");
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("recurrence endDate must not be before startDate");
        }
        days = Set.copyOf(days);
    }
}

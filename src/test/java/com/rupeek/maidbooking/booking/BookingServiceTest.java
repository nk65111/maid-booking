package com.rupeek.maidbooking.booking;

import static com.rupeek.maidbooking.Fixture.NOW;
import static com.rupeek.maidbooking.Fixture.recurring;
import static com.rupeek.maidbooking.Fixture.scheduled;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.rupeek.maidbooking.Fixture;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.NotFoundException;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.ServiceType;

class BookingServiceTest {

    private static final LocalDateTime TOMORROW_10AM = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final Set<DayOfWeek> WEEKDAYS = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    private final Fixture fixture = new Fixture();
    private final Maid maid = fixture.onboardMaid("9876543210");

    @Test
    void scheduledBookingHasOnePricedOccurrenceAwaitingPayment() {
        Booking booking = fixture.bookScheduled(maid.id(), TOMORROW_10AM, 120);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getOccurrences()).singleElement().satisfies(occurrence -> {
            assertThat(occurrence.getSlot().start()).isEqualTo(TOMORROW_10AM);
            assertThat(occurrence.getAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        });
    }

    @Test
    void instantBookingStartsNow() {
        Booking booking = fixture.bookingService.book(new BookingRequest("cust-1", maid.id(), ServiceType.COOKING,
                BookingType.INSTANT, 60, null, null));

        assertThat(booking.getOccurrences().get(0).getSlot().start()).isEqualTo(NOW);
        assertThat(booking.getOccurrences().get(0).getAmount()).isEqualByComparingTo("300");
    }

    @Test
    void instantBookingFailsWhenMaidIsNotWorkingRightNow() {
        fixture.clock.set(NOW.withHour(21));

        assertThatThrownBy(() -> fixture.bookingService.book(new BookingRequest("cust-1", maid.id(),
                ServiceType.COOKING, BookingType.INSTANT, 60, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void overlappingBookingForSameMaidIsRejected() {
        fixture.bookScheduled(maid.id(), TOMORROW_10AM, 120);

        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), TOMORROW_10AM.plusHours(1), 60))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void backToBackBookingsAreAllowed() {
        fixture.bookScheduled(maid.id(), TOMORROW_10AM, 60);

        Booking next = fixture.bookScheduled(maid.id(), TOMORROW_10AM.plusHours(1), 60);

        assertThat(next.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void sameSlotWithDifferentMaidIsFine() {
        Maid other = fixture.onboardMaid("9999999999");
        fixture.bookScheduled(maid.id(), TOMORROW_10AM, 60);

        assertThat(fixture.bookScheduled(other.id(), TOMORROW_10AM, 60)).isNotNull();
    }

    @Test
    void slotOutsideWorkingHoursIsRejected() {
        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), TOMORROW_10AM.withHour(19), 120))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void invalidRequestsAreRejected() {
        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), NOW.minusHours(1), 60))
                .isInstanceOf(ValidationException.class).hasMessageContaining("future");
        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), NOW.plusDays(91), 60))
                .isInstanceOf(ValidationException.class).hasMessageContaining("90 days");
        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), TOMORROW_10AM, 15))
                .isInstanceOf(ValidationException.class).hasMessageContaining("durationMinutes");
        assertThatThrownBy(() -> fixture.bookScheduled(maid.id(), null, 60))
                .isInstanceOf(ValidationException.class).hasMessageContaining("startAt");
        assertThatThrownBy(() -> fixture.bookingService.book(new BookingRequest(" ", maid.id(),
                ServiceType.CLEANING, BookingType.SCHEDULED, 60, TOMORROW_10AM, null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("customerId");
        assertThatThrownBy(() -> fixture.bookingService.book(new BookingRequest("cust-1", maid.id(),
                ServiceType.CLEANING, null, 60, TOMORROW_10AM, null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("type");
    }

    @Test
    void serviceTheMaidDoesNotOfferIsRejected() {
        assertThatThrownBy(() -> fixture.bookingService.book(new BookingRequest("cust-1", maid.id(),
                ServiceType.LAUNDRY, BookingType.SCHEDULED, 60, TOMORROW_10AM, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not offer");
    }

    @Test
    void unknownMaidIsNotFound() {
        assertThatThrownBy(() -> fixture.bookScheduled("nope", TOMORROW_10AM, 60))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void recurringBookingCreatesOneOccurrencePerMatchingDay() {
        // Mon 2 Nov to Fri 13 Nov, weekdays at 9am -> 10 visits; today's 9am is still ahead of 8am
        Booking booking = fixture.bookingService.book(recurring(maid.id(), WEEKDAYS, LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 15)));

        assertThat(booking.getOccurrences()).hasSize(10);
        assertThat(booking.getOccurrences()).allSatisfy(o ->
                assertThat(o.getSlot().start().getDayOfWeek()).isIn(WEEKDAYS));
    }

    @Test
    void recurringSkipsOccurrencesAlreadyInThePast() {
        fixture.clock.set(NOW.withHour(10));

        Booking booking = fixture.bookingService.book(recurring(maid.id(), Set.of(DayOfWeek.MONDAY),
                LocalTime.of(9, 0), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16)));

        assertThat(booking.getOccurrences()).extracting(o -> o.getSlot().start().toLocalDate())
                .containsExactly(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 16));
    }

    @Test
    void recurringIsAllOrNothingWhenOneOccurrenceClashes() {
        fixture.bookScheduled(maid.id(), LocalDateTime.of(2026, 11, 4, 9, 30), 60);

        assertThatThrownBy(() -> fixture.bookingService.book(recurring(maid.id(), WEEKDAYS, LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6))))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2026-11-04T09:00");

        // nothing from the failed series was kept, so Monday 9am is still free
        assertThat(fixture.bookScheduled(maid.id(), LocalDateTime.of(2026, 11, 2, 9, 0), 60)).isNotNull();
    }

    @Test
    void recurringValidation() {
        assertThatThrownBy(() -> fixture.bookingService.book(recurring(maid.id(), WEEKDAYS, LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 2), LocalDate.of(2027, 3, 1))))
                .isInstanceOf(ValidationException.class).hasMessageContaining("90 days");
        assertThatThrownBy(() -> fixture.bookingService.book(recurring(maid.id(), WEEKDAYS, LocalTime.of(9, 0),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30))))
                .isInstanceOf(ValidationException.class).hasMessageContaining("no upcoming");
        assertThatThrownBy(() -> new Recurrence(Set.of(), LocalTime.of(9, 0), LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 9)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Recurrence(WEEKDAYS, LocalTime.of(9, 0), LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 2)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> fixture.bookingService.book(new BookingRequest("cust-1", maid.id(),
                ServiceType.CLEANING, BookingType.RECURRING, 60, null, null)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("recurrence");
    }

    @Test
    void concurrentRequestsForTheSameSlotBookItOnlyOnce() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Booking>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            attempts.add(() -> fixture.bookingService.book(scheduled(maid.id(), TOMORROW_10AM, 60)));
        }

        int succeeded = 0;
        int conflicted = 0;
        for (Future<Booking> result : pool.invokeAll(attempts)) {
            try {
                result.get();
                succeeded++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(ConflictException.class);
                conflicted++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(1);
        assertThat(conflicted).isEqualTo(threads - 1);
        assertThat(fixture.bookingRepository.findActiveOccurrences(maid.id())).hasSize(1);
    }
}

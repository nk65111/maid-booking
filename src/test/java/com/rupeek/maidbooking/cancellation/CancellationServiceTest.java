package com.rupeek.maidbooking.cancellation;

import static com.rupeek.maidbooking.Fixture.recurring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rupeek.maidbooking.Fixture;
import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.booking.BookingStatus;
import com.rupeek.maidbooking.booking.Occurrence;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.NotFoundException;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.payment.Payment;

class CancellationServiceTest {

    private static final LocalDateTime IN_TWO_DAYS = LocalDateTime.of(2026, 11, 4, 10, 0);

    private final Fixture fixture = new Fixture();
    private final Maid maid = fixture.onboardMaid("9876543210");

    @Test
    void cancellingWellAheadRefundsInFull() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 120);
        Payment payment = fixture.payByCard(booking.getId());

        fixture.cancellationService.cancelBooking(booking.getId());

        Occurrence occurrence = booking.getOccurrences().get(0);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(occurrence.getRefundedAmount()).isEqualByComparingTo("400");
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("400");
    }

    @Test
    void lateCancellationRefundsAccordingToPolicy() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 120);
        fixture.payByCard(booking.getId());
        fixture.clock.set(IN_TWO_DAYS.minusHours(5));

        fixture.cancellationService.cancelBooking(booking.getId());

        assertThat(booking.getOccurrences().get(0).getRefundedAmount()).isEqualByComparingTo("200");
    }

    @Test
    void cancellingAnUnpaidBookingRefundsNothing() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 60);

        fixture.cancellationService.cancelBooking(booking.getId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getOccurrences().get(0).getRefundedAmount()).isZero();
    }

    @Test
    void cancelledSlotIsBookableAndSearchableAgain() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 60);
        assertThat(fixture.availability.isFree(maid, booking.getOccurrences().get(0).getSlot())).isFalse();

        fixture.cancellationService.cancelBooking(booking.getId());

        assertThat(fixture.availability.isFree(maid, booking.getOccurrences().get(0).getSlot())).isTrue();
        assertThat(fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 60).getStatus())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void singleOccurrenceOfASeriesCanBeCancelled() {
        Booking series = weeklySeries();
        Occurrence second = series.getOccurrences().get(1);

        fixture.cancellationService.cancelOccurrence(series.getId(), second.getId());

        assertThat(second.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(series.getOccurrences()).filteredOn(Occurrence::isActive).hasSize(2);
        assertThat(series.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(fixture.bookScheduled(maid.id(), second.getSlot().start(), 60)).isNotNull();
    }

    @Test
    void wholeSeriesCancelSkipsOccurrencesThatAreAlreadyOver() {
        Booking series = weeklySeries();
        fixture.clock.set(series.getOccurrences().get(0).getSlot().end().plusHours(1));

        fixture.cancellationService.cancelBooking(series.getId());

        assertThat(series.getOccurrences().get(0).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(series.getOccurrences().subList(1, 3))
                .allSatisfy(o -> assertThat(o.getStatus()).isEqualTo(BookingStatus.CANCELLED));
    }

    @Test
    void invalidCancellationsAreRejected() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 60);
        String occurrenceId = booking.getOccurrences().get(0).getId();
        fixture.cancellationService.cancelOccurrence(booking.getId(), occurrenceId);

        assertThatThrownBy(() -> fixture.cancellationService.cancelOccurrence(booking.getId(), occurrenceId))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already cancelled");
        assertThatThrownBy(() -> fixture.cancellationService.cancelBooking(booking.getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("nothing left");
        assertThatThrownBy(() -> fixture.cancellationService.cancelOccurrence(booking.getId(), "nope"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> fixture.cancellationService.cancelBooking("nope"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void finishedOccurrenceCannotBeCancelled() {
        Booking booking = fixture.bookScheduled(maid.id(), IN_TWO_DAYS, 60);
        fixture.clock.set(IN_TWO_DAYS.plusHours(1));

        assertThatThrownBy(() -> fixture.cancellationService.cancelOccurrence(booking.getId(),
                booking.getOccurrences().get(0).getId()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("over");
    }

    private Booking weeklySeries() {
        return fixture.bookingService.book(recurring(maid.id(), Set.of(DayOfWeek.SUNDAY), LocalTime.of(9, 0),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 22)));
    }
}

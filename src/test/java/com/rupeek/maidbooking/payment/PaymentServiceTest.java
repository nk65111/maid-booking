package com.rupeek.maidbooking.payment;

import static com.rupeek.maidbooking.Fixture.DECLINED_CARD;
import static com.rupeek.maidbooking.Fixture.GOOD_CARD;
import static com.rupeek.maidbooking.Fixture.recurring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.rupeek.maidbooking.Fixture;
import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.booking.BookingStatus;
import com.rupeek.maidbooking.booking.Occurrence;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.Maid;

class PaymentServiceTest {

    private static final LocalDateTime TOMORROW_10AM = LocalDateTime.of(2026, 11, 3, 10, 0);

    private final Fixture fixture = new Fixture();
    private final Maid maid = fixture.onboardMaid("9876543210");
    private final Booking booking = fixture.bookScheduled(maid.id(), TOMORROW_10AM, 60);

    @Test
    void successfulPaymentConfirmsTheBooking() {
        Payment payment = pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getAmount()).isEqualByComparingTo("200");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getOccurrences().get(0).getPaymentId()).isEqualTo(payment.getId());
    }

    @Test
    void everyMethodWorksThroughTheSameFlow() {
        Booking second = fixture.bookScheduled(maid.id(), TOMORROW_10AM.plusHours(2), 60);
        Booking third = fixture.bookScheduled(maid.id(), TOMORROW_10AM.plusHours(4), 60);

        pay(new PayRequest(null, PaymentMethod.UPI, "asha.k@okhdfc"), "upi");
        fixture.paymentService.pay(second.getId(), new PayRequest(null, PaymentMethod.WALLET, "9876543210"), "wallet");
        fixture.paymentService.pay(third.getId(), new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "card");

        assertThat(List.of(booking, second, third)).allSatisfy(b ->
                assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED));
    }

    @Test
    void declinedPaymentLeavesBookingPendingAndCanBeRetriedWithANewKey() {
        Payment declined = pay(new PayRequest(null, PaymentMethod.CARD, DECLINED_CARD), "key-1");

        assertThat(declined.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(declined.getFailureReason()).isNotBlank();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        Payment retry = pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-2");
        assertThat(retry.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void repeatingTheSameIdempotencyKeyDoesNotChargeAgain() {
        PayRequest request = new PayRequest(null, PaymentMethod.CARD, GOOD_CARD);

        Payment first = pay(request, "key-1");
        Payment again = pay(request, "key-1");

        assertThat(again.getId()).isEqualTo(first.getId());
    }

    @Test
    void reusingAnIdempotencyKeyForAnotherBookingIsRejected() {
        Booking other = fixture.bookScheduled(maid.id(), TOMORROW_10AM.plusHours(3), 60);
        pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-1");

        assertThatThrownBy(() -> fixture.paymentService.pay(other.getId(),
                new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Idempotency key");
    }

    @Test
    void alreadyPaidOccurrenceCannotBePaidAgain() {
        Occurrence occurrence = booking.getOccurrences().get(0);
        pay(new PayRequest(occurrence.getId(), PaymentMethod.CARD, GOOD_CARD), "key-1");

        assertThatThrownBy(() -> pay(new PayRequest(occurrence.getId(), PaymentMethod.CARD, GOOD_CARD), "key-2"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-3"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nothing left to pay");
    }

    @Test
    void cancelledOccurrenceCannotBePaid() {
        fixture.cancellationService.cancelBooking(booking.getId());

        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-1"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void occurrenceThatIsAlreadyOverCannotBePaid() {
        fixture.clock.set(TOMORROW_10AM.plusHours(2));

        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "key-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("over");
    }

    @Test
    void invalidPaymentInputsAreRejected() {
        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.CARD, "1234"), "k1"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.UPI, "not-a-vpa"), "k2"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.WALLET, " "), "k3"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pay(new PayRequest(null, null, GOOD_CARD), "k4"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), " "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Idempotency-Key");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void recurringBookingIsPaidOneOccurrenceAtATime() {
        Booking series = fixture.bookingService.book(recurring(maid.id(), Set.of(DayOfWeek.WEDNESDAY),
                LocalTime.of(14, 0), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 18)));
        assertThat(series.getOccurrences()).hasSize(3);

        Payment first = fixture.paymentService.pay(series.getId(),
                new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "series-1");

        assertThat(first.getOccurrenceId()).isEqualTo(series.getOccurrences().get(0).getId());
        assertThat(first.getAmount()).isEqualByComparingTo("200");
        assertThat(series.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);

        fixture.paymentService.pay(series.getId(), new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "series-2");
        fixture.paymentService.pay(series.getId(), new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), "series-3");
        assertThat(series.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void parallelPaymentsWithDifferentKeysChargeOnlyOnce() throws Exception {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Payment>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            attempts.add(() -> pay(new PayRequest(null, PaymentMethod.CARD, GOOD_CARD), UUID.randomUUID().toString()));
        }

        int charged = 0;
        for (Future<Payment> result : pool.invokeAll(attempts)) {
            try {
                result.get();
                charged++;
            } catch (Exception e) {
                assertThat(e.getCause()).isInstanceOf(ConflictException.class);
            }
        }
        pool.shutdown();

        assertThat(charged).isEqualTo(1);
    }

    private Payment pay(PayRequest request, String key) {
        return fixture.paymentService.pay(booking.getId(), request, key);
    }
}

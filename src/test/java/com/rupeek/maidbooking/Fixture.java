package com.rupeek.maidbooking;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.rupeek.maidbooking.booking.AvailabilityChecker;
import com.rupeek.maidbooking.booking.Booking;
import com.rupeek.maidbooking.booking.BookingRequest;
import com.rupeek.maidbooking.booking.BookingService;
import com.rupeek.maidbooking.booking.BookingType;
import com.rupeek.maidbooking.booking.InMemoryBookingRepository;
import com.rupeek.maidbooking.booking.Recurrence;
import com.rupeek.maidbooking.booking.planner.InstantSlotPlanner;
import com.rupeek.maidbooking.booking.planner.RecurringSlotPlanner;
import com.rupeek.maidbooking.booking.planner.ScheduledSlotPlanner;
import com.rupeek.maidbooking.cancellation.CancellationService;
import com.rupeek.maidbooking.cancellation.TimeBasedCancellationPolicy;
import com.rupeek.maidbooking.common.KeyedLocks;
import com.rupeek.maidbooking.maid.AvailabilityWindow;
import com.rupeek.maidbooking.maid.Gender;
import com.rupeek.maidbooking.maid.InMemoryMaidRepository;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.MaidService;
import com.rupeek.maidbooking.maid.OnboardMaidRequest;
import com.rupeek.maidbooking.maid.ServiceType;
import com.rupeek.maidbooking.payment.CardPaymentProcessor;
import com.rupeek.maidbooking.payment.InMemoryPaymentRepository;
import com.rupeek.maidbooking.payment.MockPaymentGateway;
import com.rupeek.maidbooking.payment.PayRequest;
import com.rupeek.maidbooking.payment.Payment;
import com.rupeek.maidbooking.payment.PaymentMethod;
import com.rupeek.maidbooking.payment.PaymentService;
import com.rupeek.maidbooking.payment.UpiPaymentProcessor;
import com.rupeek.maidbooking.payment.WalletPaymentProcessor;
import com.rupeek.maidbooking.pricing.HourlyPricingStrategy;
import com.rupeek.maidbooking.search.Filters;
import com.rupeek.maidbooking.search.MaidSearchService;

/**
 * Wires the real services by hand (no Spring context) around a clock the test controls.
 */
public class Fixture {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 8, 0); // a Monday
    public static final String GOOD_CARD = "4111111111111111";
    public static final String DECLINED_CARD = "4111111111110000";

    public final MutableClock clock = new MutableClock(NOW, ZONE);
    public final KeyedLocks locks = new KeyedLocks();
    public final InMemoryMaidRepository maidRepository = new InMemoryMaidRepository();
    public final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    public final InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
    public final AvailabilityChecker availability = new AvailabilityChecker(bookingRepository);
    public final MaidService maidService = new MaidService(maidRepository, locks);
    public final BookingService bookingService = new BookingService(maidService, bookingRepository, availability,
            List.of(new InstantSlotPlanner(), new ScheduledSlotPlanner(), new RecurringSlotPlanner()),
            new HourlyPricingStrategy(), locks, clock);
    private final MockPaymentGateway gateway = new MockPaymentGateway();
    public final PaymentService paymentService = new PaymentService(bookingService, paymentRepository,
            List.of(new CardPaymentProcessor(gateway), new UpiPaymentProcessor(gateway), new WalletPaymentProcessor(gateway)),
            locks, clock);
    public final CancellationService cancellationService = new CancellationService(bookingService, paymentService,
            new TimeBasedCancellationPolicy(), clock);
    public final MaidSearchService searchService = searchService();

    /** Works every day 07:00-20:00, cleaning at 200/hr and cooking at 300/hr. */
    public Maid onboardMaid(String phone) {
        return onboardMaid(phone, "Koramangala", Gender.FEMALE, 4.5);
    }

    public Maid onboardMaid(String phone, String locality, Gender gender, Double rating) {
        List<AvailabilityWindow> everyDay = Arrays.stream(DayOfWeek.values())
                .map(day -> new AvailabilityWindow(day, LocalTime.of(7, 0), LocalTime.of(20, 0)))
                .toList();
        return maidService.onboard(new OnboardMaidRequest("Maid " + phone, phone, locality, gender,
                Map.of(ServiceType.CLEANING, new BigDecimal("200"), ServiceType.COOKING, new BigDecimal("300")),
                everyDay, rating));
    }

    public static BookingRequest scheduled(String maidId, LocalDateTime start, int minutes) {
        return new BookingRequest("cust-1", maidId, ServiceType.CLEANING, BookingType.SCHEDULED, minutes, start, null);
    }

    public static BookingRequest recurring(String maidId, Set<DayOfWeek> days, LocalTime time,
                                           LocalDate from, LocalDate to) {
        return new BookingRequest("cust-1", maidId, ServiceType.CLEANING, BookingType.RECURRING, 60, null,
                new Recurrence(days, time, from, to));
    }

    public Booking bookScheduled(String maidId, LocalDateTime start, int minutes) {
        return bookingService.book(scheduled(maidId, start, minutes));
    }

    public Payment payByCard(String bookingId) {
        return paymentService.pay(bookingId, new PayRequest(null, PaymentMethod.CARD, GOOD_CARD),
                UUID.randomUUID().toString());
    }

    private MaidSearchService searchService() {
        Filters filters = new Filters();
        return new MaidSearchService(maidRepository, List.of(
                filters.localityFilter(), filters.serviceTypeFilter(), filters.skillsFilter(), filters.genderFilter(),
                filters.ratingFilter(), filters.priceFilter(), filters.availabilityFilter(availability)), clock);
    }
}

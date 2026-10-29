package com.rupeek.maidbooking.maid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.rupeek.maidbooking.Fixture;
import com.rupeek.maidbooking.common.ConflictException;
import com.rupeek.maidbooking.common.TimeSlot;
import com.rupeek.maidbooking.common.ValidationException;

class MaidOnboardingTest {

    private static final AvailabilityWindow MONDAY_MORNING =
            new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
    private static final Map<ServiceType, BigDecimal> CLEANING_200 = Map.of(ServiceType.CLEANING, new BigDecimal("200"));

    private final Fixture fixture = new Fixture();

    @Test
    void onboardsValidMaid() {
        Maid maid = fixture.onboardMaid("9876543210");

        assertThat(fixture.maidService.get(maid.id())).isEqualTo(maid);
        assertThat(maid.offers(ServiceType.COOKING)).isTrue();
        assertThat(maid.offers(ServiceType.LAUNDRY)).isFalse();
    }

    @Test
    void samePhoneCannotBeOnboardedTwice() {
        fixture.onboardMaid("9876543210");

        assertThatThrownBy(() -> fixture.onboardMaid("9876543210"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsInvalidProfiles() {
        assertInvalid(request(" ", "9876543210", CLEANING_200, List.of(MONDAY_MORNING), null), "name is required");
        assertInvalid(request("Asha", "12345", CLEANING_200, List.of(MONDAY_MORNING), null), "phone must be 10 digits");
        assertInvalid(request("Asha", "9876543210", Map.of(), List.of(MONDAY_MORNING), null), "at least one service");
        assertInvalid(request("Asha", "9876543210", Map.of(ServiceType.CLEANING, BigDecimal.ZERO),
                List.of(MONDAY_MORNING), null), "must be positive");
        assertInvalid(request("Asha", "9876543210", CLEANING_200, List.of(), null), "at least one availability");
        assertInvalid(request("Asha", "9876543210", CLEANING_200, List.of(MONDAY_MORNING), 5.5), "rating");
    }

    @Test
    void rejectsOverlappingAvailabilityWindows() {
        AvailabilityWindow overlapping = new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(14, 0));

        assertInvalid(request("Asha", "9876543210", CLEANING_200, List.of(MONDAY_MORNING, overlapping), null),
                "overlap");
    }

    @Test
    void availabilityWindowMustEndAfterItStarts() {
        assertThatThrownBy(() -> new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(8, 0)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void worksOnlyWhenSlotFitsEntirelyInsideAWindow() {
        Maid maid = fixture.maidService.onboard(
                request("Asha", "9876543210", CLEANING_200, List.of(MONDAY_MORNING), null));
        LocalDateTime monday = LocalDateTime.of(2026, 11, 2, 8, 0);

        assertThat(maid.worksDuring(TimeSlot.of(monday, 240))).isTrue();
        assertThat(maid.worksDuring(TimeSlot.of(monday.minusMinutes(30), 60))).isFalse();
        assertThat(maid.worksDuring(TimeSlot.of(monday.plusHours(3), 90))).isFalse();
        assertThat(maid.worksDuring(TimeSlot.of(monday.plusDays(1), 60))).isFalse();
    }

    private void assertInvalid(OnboardMaidRequest request, String message) {
        assertThatThrownBy(() -> fixture.maidService.onboard(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(message);
    }

    private static OnboardMaidRequest request(String name, String phone, Map<ServiceType, BigDecimal> rates,
                                              List<AvailabilityWindow> windows, Double rating) {
        return new OnboardMaidRequest(name, phone, "Koramangala", Gender.FEMALE, rates, windows, rating);
    }
}

package com.rupeek.maidbooking.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class TimeSlotTest {

    private static final LocalDateTime TEN = LocalDateTime.of(2026, 11, 3, 10, 0);

    @Test
    void overlappingSlotsAreDetected() {
        TimeSlot tenToTwelve = TimeSlot.of(TEN, 120);
        TimeSlot elevenToOne = TimeSlot.of(TEN.plusHours(1), 120);

        assertThat(tenToTwelve.overlaps(elevenToOne)).isTrue();
        assertThat(elevenToOne.overlaps(tenToTwelve)).isTrue();
    }

    @Test
    void slotContainedInAnotherOverlaps() {
        assertThat(TimeSlot.of(TEN, 180).overlaps(TimeSlot.of(TEN.plusHours(1), 30))).isTrue();
    }

    @Test
    void backToBackSlotsDoNotOverlap() {
        assertThat(TimeSlot.of(TEN, 60).overlaps(TimeSlot.of(TEN.plusHours(1), 60))).isFalse();
    }

    @Test
    void endMustBeAfterStart() {
        assertThatThrownBy(() -> new TimeSlot(TEN, TEN)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TimeSlot(TEN, TEN.minusMinutes(1))).isInstanceOf(ValidationException.class);
    }
}

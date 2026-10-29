package com.rupeek.maidbooking.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.rupeek.maidbooking.Fixture;
import com.rupeek.maidbooking.common.ValidationException;
import com.rupeek.maidbooking.maid.Gender;
import com.rupeek.maidbooking.maid.Maid;
import com.rupeek.maidbooking.maid.ServiceType;

class MaidSearchServiceTest {

    private static final LocalDateTime TOMORROW_10AM = LocalDateTime.of(2026, 11, 3, 10, 0);

    private final Fixture fixture = new Fixture();
    private final Maid asha = fixture.onboardMaid("9000000001", "Koramangala", Gender.FEMALE, 4.8);
    private final Maid ravi = fixture.onboardMaid("9000000002", "Koramangala", Gender.MALE, 3.9);
    private final Maid meena = fixture.onboardMaid("9000000003", "Indiranagar", Gender.FEMALE, null);

    @Test
    void noCriteriaReturnsEveryoneBestRatedFirst() {
        assertThat(search(criteria().build())).containsExactly(asha, ravi, meena);
    }

    @Test
    void filtersByLocalityIgnoringCase() {
        assertThat(search(criteria().locality(" koramangala ").build())).containsExactly(asha, ravi);
    }

    @Test
    void filtersByGenderAndRating() {
        assertThat(search(criteria().gender(Gender.FEMALE).build())).containsExactly(asha, meena);
        assertThat(search(criteria().minRating(4.0).build())).containsExactly(asha);
    }

    @Test
    void filtersByServiceSkillsAndPrice() {
        assertThat(search(criteria().serviceType(ServiceType.LAUNDRY).build())).isEmpty();
        assertThat(search(criteria().skills(Set.of(ServiceType.CLEANING, ServiceType.COOKING)).build())).hasSize(3);
        assertThat(search(criteria().serviceType(ServiceType.COOKING).maxPrice("250").build())).isEmpty();
        assertThat(search(criteria().serviceType(ServiceType.CLEANING).maxPrice("250").build())).hasSize(3);
    }

    @Test
    void excludesMaidsBookedOrOffDutyAtTheRequestedTime() {
        fixture.bookScheduled(asha.id(), TOMORROW_10AM, 120);

        assertThat(search(criteria().start(TOMORROW_10AM.plusHours(1), 60).build())).containsExactly(ravi, meena);
        assertThat(search(criteria().start(TOMORROW_10AM.plusHours(2), 60).build())).containsExactly(asha, ravi, meena);
        assertThat(search(criteria().start(TOMORROW_10AM.withHour(22), 60).build())).isEmpty();
    }

    @Test
    void rejectsNonsenseCriteria() {
        assertThatThrownBy(() -> search(criteria().start(Fixture.NOW.minusHours(1), 60).build()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> search(criteria().minRating(7.0).build()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> search(criteria().maxPrice("-1").build()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> search(criteria().start(TOMORROW_10AM, 5).build()))
                .isInstanceOf(ValidationException.class);
    }

    private java.util.List<Maid> search(SearchCriteria criteria) {
        return fixture.searchService.search(criteria);
    }

    private static CriteriaBuilder criteria() {
        return new CriteriaBuilder();
    }

    private static class CriteriaBuilder {
        private String locality;
        private ServiceType serviceType;
        private LocalDateTime start;
        private Integer duration;
        private Double minRating;
        private BigDecimal maxPrice;
        private Set<ServiceType> skills;
        private Gender gender;

        CriteriaBuilder locality(String locality) {
            this.locality = locality;
            return this;
        }

        CriteriaBuilder serviceType(ServiceType serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        CriteriaBuilder start(LocalDateTime start, int duration) {
            this.start = start;
            this.duration = duration;
            return this;
        }

        CriteriaBuilder minRating(double minRating) {
            this.minRating = minRating;
            return this;
        }

        CriteriaBuilder maxPrice(String maxPrice) {
            this.maxPrice = new BigDecimal(maxPrice);
            return this;
        }

        CriteriaBuilder skills(Set<ServiceType> skills) {
            this.skills = skills;
            return this;
        }

        CriteriaBuilder gender(Gender gender) {
            this.gender = gender;
            return this;
        }

        SearchCriteria build() {
            return new SearchCriteria(locality, serviceType, start, duration, minRating, maxPrice, skills, gender);
        }
    }
}

package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.InitializeOutcome;
import com.inventory.service.application.port.out.ReleaseOutcome;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAvailabilityServiceTest {

    private FakeAvailabilityRepository availabilityRepository;
    private GetAvailabilityService service;

    @BeforeEach
    void setUp() {
        availabilityRepository = new FakeAvailabilityRepository();
        service = new GetAvailabilityService(availabilityRepository);
    }

    @Test
    void returnsAvailabilityProvidedByTheRepository() {
        Availability expected = new Availability(
                10L,
                List.of(new LocalityAvailability("VIP", 5)));
        availabilityRepository.resultToReturn = expected;

        Availability result = service.getAvailability(10L).block();

        assertThat(availabilityRepository.requestedEventId).isEqualTo(10L);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void propagatesErrorFromRepository() {
        RuntimeException failure = new RuntimeException("redis unavailable");
        availabilityRepository.errorToReturn = failure;

        assertThatThrownBy(() -> service.getAvailability(10L).block())
                .isSameAs(failure);

        assertThat(availabilityRepository.requestedEventId).isEqualTo(10L);
    }

    private static final class FakeAvailabilityRepository implements AvailabilityRepository {
        private Availability resultToReturn;
        private RuntimeException errorToReturn;
        private Long requestedEventId;

        @Override
        public Mono<Availability> getAvailability(Long eventId) {
            this.requestedEventId = eventId;
            if (errorToReturn != null) {
                return Mono.error(errorToReturn);
            }
            return Mono.just(resultToReturn);
        }

        @Override
        public Mono<InitializeOutcome> initialize(Long eventId, String sectionId, Integer capacity) {
            return Mono.empty();
        }

        @Override
        public Mono<ReservationOutcome> tryReserve(Long eventId, String sectionId, Integer quantity) {
            return Mono.empty();
        }

        @Override
        public Mono<ReleaseOutcome> release(Long eventId, String sectionId, Integer quantity) {
            return Mono.empty();
        }
    }
}

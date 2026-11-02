package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.EventLocalitiesPort;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Composes the live availability of every locality of an event.
 *
 * <p>Locality ids and their declared capacity come from the catalog (static configuration);
 * the available quantity is owned by inventory and lives in Redis. The first time a locality
 * is queried its counter is seeded with the catalog capacity, so a freshly created event is
 * immediately sellable without an explicit initialization step.
 */
@Service
public class GetAvailabilityService implements GetAvailabilityUseCase {

    private final AvailabilityRepository availabilityRepository;
    private final EventLocalitiesPort eventLocalitiesPort;

    public GetAvailabilityService(AvailabilityRepository availabilityRepository,
                                  EventLocalitiesPort eventLocalitiesPort) {
        this.availabilityRepository = availabilityRepository;
        this.eventLocalitiesPort = eventLocalitiesPort;
    }

    @Override
    public Mono<Availability> getAvailability(String eventId) {
        return eventLocalitiesPort.findByEvent(eventId)
                .concatMap(locality -> availabilityRepository
                        .initialize(eventId, locality.localityId(), locality.capacity())
                        .then(availabilityRepository.readAvailability(eventId, locality.localityId()))
                        .map(available -> new LocalityAvailability(locality.localityId(), available)))
                .collectList()
                .map(localities -> new Availability(eventId, localities));
    }
}

package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AdjustStockService {

    private final AvailabilityRepository availabilityRepository;

    public AdjustStockService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public Mono<Void> adjust(String localityId, Integer newQuantity) {
        return availabilityRepository.adjust(localityId, newQuantity);
    }
}

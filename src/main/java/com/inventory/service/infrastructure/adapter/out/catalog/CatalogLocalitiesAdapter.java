package com.inventory.service.infrastructure.adapter.out.catalog;

import com.inventory.service.application.port.out.EventLocalitiesPort;
import com.inventory.service.domain.exception.CatalogUnavailableException;
import com.inventory.service.domain.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * Resolves the static configuration of an event locality from the catalog service
 * ({@code GET /api/eventos/{eventId}/localidades}, public read).
 *
 * <p>Inventory only needs the locality identifiers and their declared capacity: it uses them
 * to key the Redis counters and to seed them on first use. The catalog remains the single
 * source of truth for the event configuration.
 */
@Repository
public class CatalogLocalitiesAdapter implements EventLocalitiesPort {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final WebClient webClient;

    public CatalogLocalitiesAdapter(@Value("${catalog.base-url}") String catalogBaseUrl) {
        this.webClient = WebClient.builder().baseUrl(catalogBaseUrl).build();
    }

    @Override
    public Flux<EventLocality> findByEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return Flux.error(new IllegalArgumentException("eventId must be provided"));
        }
        return webClient.get()
                .uri("/api/eventos/{eventId}/localidades", eventId)
                .retrieve()
                .bodyToFlux(LocalityView.class)
                .map(view -> new EventLocality(view.id(), view.capacity()))
                .timeout(REQUEST_TIMEOUT)
                .onErrorMap(WebClientResponseException.NotFound.class,
                        ex -> new ResourceNotFoundException("Event not found: " + eventId))
                .onErrorMap(ex -> !(ex instanceof ResourceNotFoundException)
                                && !(ex instanceof IllegalArgumentException),
                        ex -> new CatalogUnavailableException(
                                "Catalog service is unavailable while resolving localities of event " + eventId,
                                ex));
    }

    /** Subset of the catalog locality payload that inventory needs. */
    private record LocalityView(String id, Integer capacity) {
    }
}

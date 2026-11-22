package com.inventory.service.infrastructure.adapter.in.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.service.application.port.out.EventLocalitiesPort.EventLocality;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the platform envelope published by catalog-service. Field names in {@code data} are
 * Spanish on purpose: they are a cross-service contract (see catalog EventPayloads).
 */
@Component
public class CatalogEventPayload {

    private final ObjectMapper mapper = new ObjectMapper();

    public String readMessageId(String json) {
        try {
            return root(json).path("eventId").asText(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid catalog integration event envelope", e);
        }
    }

    public Published readPublished(String json) {
        try {
            JsonNode data = root(json).path("data");
            String eventId = data.path("eventoId").asText(null);
            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Missing data.eventoId in eventos.publicados");
            }

            Instant fecha = data.hasNonNull("fecha")
                    ? Instant.parse(data.path("fecha").asText())
                    : Instant.now();

            List<EventLocality> localities = new ArrayList<>();
            JsonNode nodes = data.path("localidades");
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    String localityId = node.path("localidadId").asText(null);
                    int capacity = node.path("aforo").asInt(0);
                    if (localityId != null && !localityId.isBlank() && capacity > 0) {
                        localities.add(new EventLocality(localityId, capacity));
                    }
                }
            }
            return new Published(eventId, fecha, localities);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid eventos.publicados payload", e);
        }
    }

    public String readCancelledEventId(String json) {
        try {
            String eventId = root(json).path("data").path("eventoId").asText(null);
            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Missing data.eventoId in eventos.cancelados");
            }
            return eventId;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid eventos.cancelados payload", e);
        }
    }

    private JsonNode root(String json) throws Exception {
        return mapper.readTree(json);
    }

    public record Published(String eventId, Instant fecha, List<EventLocality> localities) {
    }
}

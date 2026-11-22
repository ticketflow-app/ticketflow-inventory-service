package com.inventory.service.infrastructure.adapter.in.messaging;

import com.inventory.service.application.port.out.EventLocalitiesPort.EventLocality;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogEventPayloadTest {

    private final CatalogEventPayload parser = new CatalogEventPayload();

    @Test
    void parsesPublishedEnvelopeWithLocalitiesAndCapacity() {
        String json = """
                {"eventId":"01a1e9a7-d302-7852-9c63-d4c982d6d0cf","type":"eventos.publicados",
                 "version":1,"occurredAt":"2026-11-17T13:17:32.280704511Z",
                 "producer":"ticketflow-catalog-service","traceId":null,
                 "data":{"eventoId":"01a1e9a7-4210-7810-81e3-3539624a8b88",
                         "fecha":"2026-12-24T08:16:00Z",
                         "localidades":[{"localidadId":"loc-1","nombre":"Vip","precio":100000.00,"aforo":2000},
                                        {"localidadId":"loc-2","nombre":"General","precio":50000.00,"aforo":5000}]}}
                """;

        CatalogEventPayload.Published published = parser.readPublished(json);

        assertThat(published.eventId()).isEqualTo("01a1e9a7-4210-7810-81e3-3539624a8b88");
        assertThat(published.fecha()).isEqualTo(Instant.parse("2026-12-24T08:16:00Z"));
        assertThat(published.localities()).containsExactly(
                new EventLocality("loc-1", 2000), new EventLocality("loc-2", 5000));
        assertThat(parser.readMessageId(json)).isEqualTo("01a1e9a7-d302-7852-9c63-d4c982d6d0cf");
    }

    @Test
    void parsesCancelledEnvelope() {
        String json = """
                {"eventId":"01a1e9a7-d302-7852-9c63-d4c982d6d0d0","type":"eventos.cancelados",
                 "data":{"eventoId":"01a1e9a7-4210-7810-81e3-3539624a8b88","motivo":"Suspension"}}
                """;

        assertThat(parser.readCancelledEventId(json)).isEqualTo("01a1e9a7-4210-7810-81e3-3539624a8b88");
    }

    @Test
    void rejectsMalformedEnvelope() {
        assertThatThrownBy(() -> parser.readPublished("{\"data\":{}}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventoId");
        assertThatThrownBy(() -> parser.readPublished("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ignoresLocalitiesWithoutUsableCapacity() {
        String json = """
                {"eventId":"msg-1","data":{"eventoId":"event-1","fecha":"2026-12-24T08:16:00Z",
                 "localidades":[{"localidadId":"","aforo":10},{"localidadId":"loc-1","aforo":0},
                                {"localidadId":"loc-2","aforo":25}]}}
                """;

        List<EventLocality> localities = parser.readPublished(json).localities();
        assertThat(localities).containsExactly(new EventLocality("loc-2", 25));
    }
}

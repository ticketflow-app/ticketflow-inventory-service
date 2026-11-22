# P-021 — Implementación de DA-009: proyección local por Kafka y ciclo de claves Redis

- **Fecha:** 2026-11-21
- **Estado:** IMPLEMENTADO
- **Relacionado:** `DA-009` (APROBADA), `P-016` … `P-020`, `DA-007`
- **Ámbito:** solo `ticketflow-inventory-service`. Sin commit ni push.

## Objetivo

Eliminar la llamada HTTP síncrona de inventario a catálogo en la consulta de disponibilidad,
sustituyéndola por una proyección local alimentada por los eventos Kafka de catálogo, y dar un
ciclo de vida explícito a las claves Redis resultantes.

## Prompt del usuario

> Elección: "DA-009 + ciclo Redis (Recommended)" y "Solo working tree (Recommended)".
> Continuación: "continua" / "continuo y ya explicame todo lo que hiciste, no me gusta tener que
> estar insistiendo a una ia para continua una tarea que ya le dije que hiciera".

Autorización humana explícita recibida para `DA-009 + ciclo Redis` y para entregar únicamente en
el working tree. Quedaron fuera de alcance `DA-008`, el contrato del frontend, la auditoría frente
a `Microservicios.docx` y cualquier cambio en `docs_ia/contexto/`.

## Documentos consultados

- `AGENTS.md` de la raíz y de `ticketflow-inventory-service`, `.clinerules`.
- `docs_ia/decisiones/DA-009-quitar-llamada-sincrona-inventario-catalogo.md` y `README.md`.
- `docs_ia/prompts/P-016` … `P-020` y `README.md`.
- `docs_ia/contexto/` (solo lectura; sin modificaciones).
- Envelope real verificado en el topic `eventos.publicados` con `kafka-console-consumer.sh`.

## Archivos analizados

- `CatalogLocalitiesAdapter.java` (antigua llamada HTTP, eliminado).
- `GetAvailabilityService.java`, `EventLocalitiesPort.java`, `RedisAvailabilityAdapter.java`.
- `application.properties`, `docker-compose.yml` del gateway (Kafka, Redis, inventario).
- Contrato `EventCancelledData` de catálogo (`eventoId`, `motivo`) para el evento de cancelación.

## Cambios aplicados

**Código eliminado**

- `infrastructure/adapter/out/catalog/CatalogLocalitiesAdapter.java`: ya no hay HTTP a catálogo.

**Código nuevo**

- `application/port/out/EventProjectionPort.java`: guardar/borrar proyección y registrar vencimiento.
- `infrastructure/adapter/out/availability/InventoryRedisKeys.java`: prefijos centralizados
  (`inv:stock:*`, `inv:localities:*`, `inv:events:expiry`, `inv:processed:*`).
- `infrastructure/adapter/out/projection/RedisEventProjectionAdapter.java`: escribe hash de
  localidades por evento, ZSET de expiración, borra todo el evento y recupera vencidos.
- `infrastructure/adapter/in/messaging/CatalogEventPayload.java`: parsea el envelope real
  (`data.eventoId`, `data.fecha`, `data.localidades[].aforo`, `data.motivo`).
- `infrastructure/adapter/in/messaging/CatalogEventConsumer.java`: consume `eventos.publicados` y
  `eventos.cancelados`, deduplica por `eventId` del envelope con TTL de 7 días y siembra los
  contadores con `EXISTS` para no resetear stock vendido.
- `infrastructure/config/KafkaConsumerConfig.java`: `ConsumerFactory` y listener container factory
  explícitos (necesarios porque el servicio ya define un `ProducerFactory` propio).
- `infrastructure/config/SchedulingConfig.java` + `infrastructure/adapter/in/scheduling/ExpiredEventCleanup.java`:
  limpieza cada hora (retardo inicial 60 s) de eventos cuya fecha + retención ya pasó.
- `src/test/java/.../messaging/CatalogEventPayloadTest.java`: 4 pruebas del parser.

**Modificados**

- `EventLocalitiesPort.java` y `GetAvailabilityService.java`: leen solo Redis.
- `RedisAvailabilityAdapter.java`: usa `InventoryRedisKeys`.
- `application.properties`: consumers Kafka, topics `eventos.publicados` / `eventos.cancelados`,
  `EVENT_RETENTION_DAYS=30`, intervalo de limpieza; se retiró `catalog.base-url`.

Sin dependencias Maven nuevas: se usan `spring-kafka`, `reactor-kafka` y `jackson-databind` ya
presentes.

## Comandos ejecutados

```powershell
docker build --target build -t ticketflow-inventory-build .
docker run --rm ticketflow-inventory-build ./mvnw test -B   # ver detalle de pruebas abajo
docker compose up -d --build --force-recreate inventory-service   # desde ticketflow-api-gateway
```

Para aislar el test nuevo de la suite heredada, dentro de un contenedor efímero se eliminaron los
demás `src/test/java/**/*.java` antes de ejecutar `-Dtest=CatalogEventPayloadTest`; el repo no se
modificó.

## Pruebas y resultados

- `docker build --target build`: **BUILD SUCCESS** (Java 25, ajuste de errores de compilación:
  `Acknowledgment` quitado en favor de auto-ack, `Range.closed` para `rangeByScore`, paquete
  correcto de `ConcurrentKafkaListenerContainerFactory`).
- `CatalogEventPayloadTest`: **Tests run: 4, Failures: 0, Errors: 0**.
- La suite completa no compila: fallos preexistentes por `long` → `String` y getters → records en
  `RedisAvailabilityAdapterIT`, `GetAvailabilityServiceTest`, `CreateReservationService*`,
  `LocalityAvailabilityTest` y `ReservationTest`. Fuera de alcance; no se tocó.
- Smoke test con stack real (mensajes sintéticos publicados en los topics):
  - `eventos.publicados` → `inv:localities:{id}`, `inv:stock:{id}:{loc}` con su `:capacity`,
    `ZSCORE inv:events:expiry` y `inv:processed:{messageId}`; `GET /api/inventario/eventos/{id}/disponibilidad`
    devuelve las localidades con `200`.
  - Reenvío del mismo envelope → log `Skipping duplicate`, stock intacto (deduplicación).
  - `eventos.cancelados` → desaparecen hash, contadores y entrada del ZSET; la HTTP devuelve
    `localities: []`.
  - Evento desconocido → `200` con `localities: []`, sin llamada a catálogo.
- Backfill: `auto-offset-reset=earliest`, grupo `inventory-service`; los eventos ya publicados se
  reproducen al arrancar, y `initialize` usa `EXISTS`, por lo que no resetea ventas previas.

## Estado final

Implementado y verificado en working tree; `DA-009` marcada APROBADA. Sin commit ni push.
Pendiente de autorización separada: `DA-008`, compatibilidad de `eventos.actualizados`, contrato
del frontend y arreglo de la suite de tests heredada.

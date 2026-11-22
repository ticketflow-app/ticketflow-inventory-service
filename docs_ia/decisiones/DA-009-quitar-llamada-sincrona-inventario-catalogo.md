# DA-009 — Quitar la llamada síncrona de inventario a catálogo

## Estado

**APROBADA** (autorización humana del 2026-11-21: "DA-009 + ciclo Redis"). Implementada el
2026-11-21 en el working tree, sin commit ni push; la registró `P-021`. No sobrescribe DA-007:
la complementa para reducir latencia sin cambiar la regla de negocio "catálogo = configuración,
inventario = disponibilidad".

## Contexto

DA-007 (APROBADA) eligió que `inventory-service` resolviera ids y aforos de localidades con
un `GET /api/eventos/{eventId}/localidades` al catálogo y sembrara los contadores de Redis la
primera vez. En la práctica esa consulta ocurre en **cada** petición de
`GET /api/inventario/eventos/{eventoId}/disponibilidad`, sin caché, con timeout de 3 s.

## Problema

- La disponibilidad —lectura caliente de la UI— depende de catálogo y de su PostgreSQL.
- Si catálogo está lento o caído, la disponibilidad falla aunque Redis tenga contadores válidos.
- Bajo carga, cada request de disponibilidad añade un servicio más al camino crítico.
- DA-007 dice que la siembra ocurre al "consultar o reservar", pero el código solo siembra al
  consultar; una reserva sin visita previa a disponibilidad falla con `NOT_INITIALIZED`.

## Alternativas consideradas

1. **Caché de la respuesta de localidades en Redis (TTL corto, p. ej. 60–300 s).**
   La usa el adaptador existente; el primer request tras arranque/expiración va a catálogo y
   el resto no. Sin dependencias nuevas (Redis ya está). Inconveniente: lista eventualmente
   desactualizada; los aforos ya sembrados no se sobrescriben, igual que hoy.
2. **Consumidor Kafka de `eventos.publicados` / `eventos.actualizados`.**
   Catálogo ya publica esos topics; inventario guarda una copia local y siembra al recibirlos.
   Cero HTTP en runtime y desacoplamiento real. Inconveniente: nuevo consumidor, idempotencia,
   backfill de eventos existentes y decidir qué hacer con `eventos.actualizados` tras ventas.

   **Aclaración (P-017):** esta alternativa **no vuelve a llamar a catálogo**; invierte la
   dirección de la comunicación (push en vez de pull). La que sí sigue consultando catálogo en
   cada miss/expiración es la alternativa 1. El beneficio principal de Kafka no es "datos más
   frescos", sino sacar a catálogo del camino caliente por completo —incluido el arranque en
   frío y los picos de cache misses— y mantener la disponibilidad operativa si catálogo cae.
   `spring-kafka` ya está en el `pom.xml`, así que no hay dependencia Maven nueva.

   **Matización:** si se elige TTL y además se quiere invalidación explícita al publicar, se
   acaba consumiendo el evento de catálogo de todos modos; en ese caso conviene directamente la
   alternativa 2.

   **Nota de contrato (P-018):** `eventos.publicados` ya trae
   `localidades[{localidadId, nombre, precio, aforo}]`, suficiente para sembrar los contadores;
   el propio javadoc de `EventPayloads` indica que inventory crea los contadores desde
   `localidades`, aunque hoy no exista consumidor. En cambio `eventos.actualizados` solo trae
   `campos` (nombres de campos cambiados), no el nuevo aforo: los cambios de capacidad posteriores
   a la primera venta seguirían ignorándose, igual que ocurre hoy con el script `EXISTS`.
   Soportarlos exigiría ampliar el payload (decisión adicional).
3. **Siembra también en `POST /reservas`.** Corrige el acoplamiento funcional, pero no quita
   la llamada HTTP ni mejora latencia por sí solo.
4. **No hacer nada.** Solo aceptable si las pruebas de carga demuestran que el impacto es
   despreciable frente a Redis/PostgreSQL.

## Recomendación

Se aplicó directamente la **alternativa 2** junto con el **ciclo de vida de la opción 1 de
`P-020`**: borrado explícito con `eventos.cancelados` y limpieza programada por fecha del evento
más 30 días de gracia. No se implementó la fase intermedia de TTL: el objetivo aprobado era sacar
a catálogo del camino caliente por completo, y un TTL seguiría exigiendo recarga (HTTP o replay).
`eventos.actualizados` se ignora porque su payload no trae el nuevo `aforo` (ver nota de contrato
más arriba); soportarlo requiere una decisión posterior.

## Ventajas

- Latencia típica de disponibilidad pasa de HTTP + BD de catálogo a un `GET` a Redis.
- Catálogo deja de ser un punto único de falla para la disponibilidad tras el calentamiento.
- No se requieren nuevas dependencias Maven ni contenedores para la fase 1.

## Desventajas / riesgos

- Introduce una caché que hay que invalidar; si se usa TTL, ventana de desactualización corta.
- El consumidor Kafka exige backfill y reglas claras para no "resetear" stock vendido.
- Requiere pruebas de concurrencia con Redis real para garantizar RB-11 (no sobreventa).
- Crecimiento de claves: hoy no hay TTL ni limpieza (`maxmemory_policy = noeviction`); con Kafka
  las claves se crean al publicar en vez de al consultar. La cardinalidad final es la misma, pero
  hay que definir el borrado (por `eventos.cancelados` y tras la fecha del evento) o mover la
  proyección a la BD de inventario. Detalle y magnitudes en `P-020`.

## Impacto

Archivos aplicados: se eliminó `CatalogLocalitiesAdapter`; se crearon `EventProjectionPort`,
`RedisEventProjectionAdapter`, `InventoryRedisKeys`, `CatalogEventConsumer`,
`CatalogEventPayload`, `KafkaConsumerConfig`, `SchedulingConfig`, `ExpiredEventCleanup` y
`CatalogEventPayloadTest`; se modificaron `EventLocalitiesPort`, `GetAvailabilityService`,
`RedisAvailabilityAdapter` y `application.properties` (consumers, topics, retención y limpieza).
Sin cambios en el contrato HTTP de inventario ni en el frontend. Sin dependencias Maven nuevas.

## Verificación y pendientes

Verificación con stack real: build Docker exitoso, `CatalogEventPayloadTest` 4/4, smoke test
Kafka → Redis → HTTP con publicación, deduplicación, cancelación y limpieza (detalle en `P-021`).
La suite antigua de tests sigue sin compilar por APIs previas a la migración de ids `long` →
`String`; no se modificó porque queda fuera del alcance autorizado.

`DA-008` (historial de reservas para cualquier rol autenticado) sigue pendiente de documentar y
aprobar; no se tocó. Tampoco se amplió `eventos.actualizados` ni se ajustó el contrato del
frontend.

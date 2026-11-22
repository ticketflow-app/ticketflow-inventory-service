# P-018 — Aclaración: dónde viven las localidades y cómo funcionaría Kafka

- **Fecha:** 2026-11-21
- **Estado:** PROPUESTA
- **Relacionado:** `P-016`, `P-017`, `DA-009` (PROPUESTA)

## Objetivo

Resolver la confusión sobre la propiedad de las localidades/capacidad y explicar por qué un
consumidor Kafka elimina la consulta HTTP a catálogo en tiempo de petición.

## Prompt del usuario (resumen literal)

> "Pensé que las localidades y su capacidad estaban en el microservicio de inventario. No
> entiendo por qué usando Kafka ya no se consultaría a catálogo, ya que en teoría se crea el
> evento y catálogo hace su trabajo al recibir el evento, o estoy en lo incorrecto? ¿O cómo
> funcionaría Kafka ahí?"

## Evidencia consultada

- `src/main/resources/db/migration/V1__create_reservations_table.sql` y `V2__uuid_event_and_locality_ids.sql`:
  la BD `inventory` solo tiene `reservations` (event_id, locality_id, ...). **No hay tabla de localidades.**
- `src/main/java/.../RedisAvailabilityAdapter.java:30-44`: Redis guarda `capacity:{event}:{locality}`
  y `inv:stock:{event}:{locality}`; el script Lua `EXISTS` evita sobrescribir contadores ya sembrados.
- `CatalogLocalitiesAdapter.java:38-54`: la única fuente de `localityId + capacity` es
  `GET /api/eventos/{id}/localidades` del catálogo.
- `ticketflow-catalog-service/.../EventPayloads.java:20-35`: `eventos.publicados` incluye
  `localidades[{localidadId, nombre, precio, aforo}]`; el javadoc dice explícitamente que
  "inventory creates the `inv:stock:*` counters from `localidades`". `eventos.actualizados`
  solo incluye `campos`, no el nuevo aforo.

## Resumen de la respuesta

1. **Las localidades autoritativas están en catálogo** (BD `catalog`: id, nombre, precio, aforo).
   Inventario no tiene tabla de localidades; solo guarda en Redis una **copia derivada** de
   `localityId + capacity` (`inv:stock:...:capacity`) y el contador vivo (`inv:stock:...`), creados
   de forma perezosa cuando alguien consulta disponibilidad. Por eso hoy depende de catálogo.
2. **Kafka no sustituye a catálogo, sustituye a la llamada HTTP de inventario a catálogo.**
   Catálogo sigue siendo el dueño y el productor: recibe un `POST /api/eventos` del
   organizador, guarda el evento y sus localidades, y al publicar emite `eventos.publicados`.
   Inventario consume ese mensaje, guarda la copia en Redis y desde entonces responde leyendo
   solo Redis. "Ya no se consulta catálogo" = desaparece la flecha HTTP en el camino caliente;
   el dato sigue originándose en catálogo, pero llega una vez por push.
3. Esto además corrige un problema funcional: hoy la siembra es **perezosa** (requiere que
   alguien consulte disponibilidad primero); con Kafka sería **ansiosa** al publicar.

## Matización del contrato

`eventos.actualizados` no trae el nuevo aforo, solo la lista de campos modificados. Los cambios
de capacidad después de la primera venta ya se ignoran hoy por el script `EXISTS`, así que el
comportamiento se mantiene; soportarlos exigiría ampliar el payload (decisión adicional).

## Pendientes

Sin cambios de código. `DA-009` permanece en PROPUESTA.

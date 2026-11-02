# DA-007 — Disponibilidad por localidad (UUID) con siembra desde catálogo

## Estado

**APROBADA** (2026-10-30). Aprobada explícitamente por el responsable del proyecto
al elegir la alternativa recomendada durante la sesión de corrección del fallo
"Inventario no disponible" del frontend.

## Contexto

El frontend y el `catalog-service` identifican eventos y localidades con **UUID** y el
contrato que consume la UI es:

```
GET /api/inventario/eventos/{eventId}/disponibilidad
{ "eventId": "<uuid>", "localities": [ { "localityId": "<uuid>", "availableTickets": 150 } ] }

POST /api/reservas  { "eventId": "<uuid>", "localityId": "<uuid>", "quantity": 2 }
```

`inventory-service` (rama `main`) había quedado con el contrato anterior
(`eventId` numérico, `sections[{sectionId, availableQuantity}]`, secciones fijas
`VIP`/`General`), sin ninguna relación con las localidades reales del catálogo:

- `GetEventAvailabilityService` leía SÓLO las claves `availability:{eventId}:VIP` y
  `availability:{eventId}:General`, mientras `RedisAvailabilityAdapter.adjust`
  escribía `availability:{localityId}` (claves incoherentes).
- Nadie inicializaba los contadores: el catálogo publica `eventos.publicados` en Kafka,
  pero inventario no tiene consumidor, así que toda consulta devolvía 0.
- El gateway reescribía `/api/inventario/...` a rutas legacy `/events/*/availability`
  que ya no existen y que `SecurityConfig` no permite → 401 → "Inventario no disponible".

## Problema

¿Cómo obtiene inventario la lista de localidades de un evento y la capacidad inicial
para sembrar los contadores de Redis, manteniendo al catálogo como fuente de verdad de
la configuración estática y a Redis como fuente de verdad de la disponibilidad?

## Alternativas consideradas

1. **Inventario consulta al catálogo por REST (elegida).**
   `GET /api/eventos/{eventId}/localidades` (lectura pública) y, la primera vez que se
   consulta o reserva una localidad, se siembra su contador con la capacidad declarada.
2. **Consumidor Kafka de `eventos.publicados`.** Inventario escucha el topic y persiste
   localidades/capacidades en su propia BD. Respeta la dirección del diseño
   (catálogo → inventario), pero requiere tabla nueva, consumidor, offset inicial y
   re-publicación de los eventos ya existentes.
3. **Catálogo llama a inventario por REST al crear/publicar el evento.** Dirección del
   diseño en esta fase, pero obliga a un cliente HTTP en el catálogo y no cubre los
   eventos ya creados sin re-publicarlos.

## Recomendación (aprobada)

Alternativa 1.

- `EventLocalitiesPort` (puerto de salida) + `CatalogLocalitiesAdapter` (WebClient,
  timeout 3 s). Si el catálogo no responde se devuelve `503` (`CatalogUnavailableException`).
- Si el evento no existe → `404` (`ResourceNotFoundException`).
- `GetAvailabilityService` compone: localidades del catálogo → `initialize` idempotente
  (script Lua con `EXISTS`) → lectura del contador por `availability:{eventId}:{localityId}`.
- El contador se siembra una sola vez con la capacidad del catálogo; los cambios
  posteriores de capacidad en el catálogo **no** pisan lo ya vendido (inventario manda).
- Ajuste administrativo: `POST /api/inventario/eventos/{eventoId}/localidades/{localidadId}/ajuste`
  (rol `admin`), ahora con `eventId` porque la clave lo requiere.

## Ventajas

- Cambio pequeño, sin dependencias nuevas (WebClient viene con `spring-boot-starter-webflux`).
- Funciona de inmediato con los eventos ya creados (siembra perezosa).
- Coherente con la regla "Catálogo ≠ Inventario": se lee configuración, no se duplica.
- Un único contrato (`localities[{localityId, availableTickets}]`) entre front, gateway e inventario.

## Desventajas

- Invierte la dirección de la integración descrita en la arquitectura (catálogo → inventario).
- Acopla la disponibilidad a la disponibilidad del catálogo (mitigado con timeout + 503).
- La siembra perezosa no replica la capacidad si el catálogo cambia antes de la primera venta.

## Impacto

- `inventory-service`: dominio (`String`/UUID), puerto y adaptador de catálogo, adaptador
  Redis, DTOs, `SecurityConfig` (HS384 + claim `role`), migraciones Flyway `V2` y `V3`.
- `api-gateway`: se eliminan los `RewritePath` legacy de inventario.
- `ticketflow-frontend`: `Idempotency-Key` en `POST /api/reservas` y reenvío del header en el BFF.

## Evolución prevista

Si se adopta el consumidor Kafka (alternativa 2), `EventLocalitiesPort` puede
mantenerse como puerto con un adaptador de lectura local, sin tocar casos de uso ni
controladores.

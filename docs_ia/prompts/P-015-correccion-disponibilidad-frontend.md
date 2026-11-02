# P-015 — Corrección del fallo "Inventario no disponible" en el frontend

- **Fecha:** 2026-10-30
- **Estado:** IMPLEMENTADO
- **Decisión asociada:** `DA-007-disponibilidad-por-localidad-uuid.md` (APROBADA)

## Objetivo

Diagnosticar si el error que muestra el frontend al abrir el detalle de un evento
("Inventario no disponible — No se pudo consultar la disponibilidad en tiempo real")
estaba en el backend o en el frontend, informarlo y corregirlo.

## Prompt del usuario (resumen literal)

> "al intentar ver las localidades y disponibilidades de un evento, me sale en el front:
> Inventario no disponible… Revisa si el error es en el back o en el front, me lo informas
> y lo arreglas."

## Archivos de contexto consultados

- `docs_ia/contexto/arquitectura/04_diseno_api_rest.md`
- `docs_ia/contexto/diseño/04_integracion_y_mensajeria.md`
- `docs_ia/decisiones/DA-001` … `DA-006`
- `ticketflow-frontend/docs_ia/contexto/contrato_api.md` (referencia cruzada)
- `ticketflow-catalog-service/src/main/java/com/ticketflow/catalog/application/event/EventPayloads.java`

## Diagnóstico

Reproducción con un evento real (`01a179d7-2f9b-7431-9d9b-6405b04ed552`):

| Paso | Resultado |
|---|---|
| BFF `GET /api/gateway/api/inventario/eventos/{uuid}/disponibilidad` | 401 |
| Gateway `GET /api/inventario/eventos/{uuid}/disponibilidad` | 401 |
| Inventario directo (misma ruta) | 500 |

Causas, todas en backend:

1. **Gateway**: `RewritePath` a `/events/{id}/availability` (controlador deshabilitado y
   ruta no permitida por `SecurityConfig`) → 401.
2. **Contrato**: `eventId` era `Long`; catálogo y front usan UUID → 500.
3. **Forma de respuesta**: `sections[{sectionId, availableQuantity}]` vs.
   `localities[{localityId, availableTickets}]` → el front no podía mapear.
4. **Sin datos**: los contadores se leían de claves fijas `VIP`/`General` que nadie
   inicializaba (Redis vacío, sin consumidor Kafka ni integración con catálogo).
5. **Reservas**: `POST /api/reservas` esperaba `{eventId: Long, sectionId}` y el front
   envía `{eventId: uuid, localityId: uuid}`; además el JWT se validaba como HS256
   mientras user-service firma HS384 y el rol viaja en el claim `role` (minúsculas).

## Cambios realizados

- `api-gateway`: se eliminan los `RewritePath` legacy de inventario y las rutas legacy
  del predicado; `JwtAuthFilter` deja pública sólo `/api/inventario/eventos/*/disponibilidad`.
- `inventory-service`:
  - UUID (`String`) en dominio, puertos, DTOs y persistencia.
  - `EventLocalitiesPort` + `CatalogLocalitiesAdapter` (consulta
    `GET /api/eventos/{id}/localidades` del catálogo con timeout de 3 s).
  - `GetAvailabilityService` siembra perezosamente los contadores
    (`availability:{eventId}:{localityId}`) y los lee.
  - `RedisAvailabilityAdapter` unificado (lectura, reserva, liberación y ajuste con la
    misma convención de claves).
  - `SecurityConfig`: decoder HS256/HS384/HS512 según la longitud del secreto y
    autoridades desde el claim `role` (`ROLE_<ROL>`).
  - `GET /api/reservas` devuelve `{reservations: [...]}`; se persiste `user_id`.
  - Migraciones `V2` (UUID + `locality_id`) y `V3` (`user_id`).
- `ticketflow-frontend`: `Idempotency-Key` en `POST /api/reservas` y reenvío del header
  desde el BFF.

## Pruebas ejecutadas

- `GET` disponibilidad por inventario directo, gateway y BFF → `200` con las tres
  localidades reales (150 / 450 / 1200).
- `POST /api/reservas` (VIP x2, VIP x3, Preferencial x1, General x2) → `201`; el contador
  de Redis baja de forma atómica (150 → 148 → 145).
- `GET /api/reservas` → devuelve las reservas del usuario autenticado.
- `docker compose build` de `inventory-service` y `api-gateway` → BUILD SUCCESS;
  Flyway aplica `V2` y `V3` al arrancar.

## Resultado

El error estaba en el backend (gateway + inventario). El frontend no requería cambios
funcionales, sólo el envío del `Idempotency-Key` para completar el flujo de reserva.

## Pendientes

- El ajuste de stock (`/ajuste`) no tiene UI ni pruebas automatizadas.
- La capacidad inicial no se re-sincroniza si el catálogo la modifica después de la
  primera venta (ver "Evolución prevista" en DA-007).
- Falta prueba de concurrencia con Redis real (N reservas en paralelo) en la suite.

# P-016 — Análisis del acoplamiento inventario → catálogo y de un CDN local

- **Fecha:** 2026-11-21
- **Estado:** PROPUESTA
- **Decisión asociada:** `DA-009-quitar-llamada-sincrona-inventario-catalogo.md` (PROPUESTA)

## Objetivo

Resolver dos dudas del responsable del proyecto: (1) si conviene montar un CDN en el
entorno local y (2) en qué acción `inventory-service` llama a `catalog-service`, por qué
eso encarece el sistema y cómo se podría eliminar.

## Prompt del usuario (resumen literal)

> "Es posible tener un cdn en local? o crees que es mejor no? eso solo como una decisión.
> Ahora me gustaría también plantear el tema que me mencionaste anteriormente sobre que al
> llamar a inventory service se llamaba también a catálogo y que eso hacía más lento al
> sistema, explicame en qué acción sucede eso y cómo inventory service se comunica con
> catálogo (kafka? redis? http?) y cómo podríamos solucionarlo para que sea más rápido."

## Archivos de contexto consultados

- `.clinerules` (gobernanza del repo).
- `docs_ia/decisiones/DA-007-disponibilidad-por-localidad-uuid.md` (APROBADA).
- `docs_ia/prompts/P-015-correccion-disponibilidad-frontend.md`.
- `docs_ia/contexto/arquitectura/`, `docs_ia/contexto/diseño/`.
- `ticketflow-catalog-service/src/main/java/com/ticketflow/catalog/application/event/Topics.java`.
- Grafo `inventory-service` (reindexado en esta sesión, 2298 nodos / 4783 aristas).

## Archivos analizados (evidencia directa)

- `src/main/java/com/inventory/service/infrastructure/adapter/in/controller/InventoryController.java`
- `src/main/java/com/inventory/service/application/usecase/GetEventAvailabilityService.java`
- `src/main/java/com/inventory/service/application/usecase/GetAvailabilityService.java`
- `src/main/java/com/inventory/service/application/port/out/EventLocalitiesPort.java`
- `src/main/java/com/inventory/service/infrastructure/adapter/out/catalog/CatalogLocalitiesAdapter.java`
- `src/main/java/com/inventory/service/infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java`
- `src/main/java/com/inventory/service/application/usecase/CreateReservationService.java`
- `src/main/java/com/inventory/service/application/usecase/ReservationService.java`
- `src/main/java/com/inventory/service/infrastructure/adapter/in/controller/AvailabilityController.java` (deshabilitado)
- `src/main/resources/application.properties` (línea 72: `catalog.base-url`)

## Hallazgos

### Acción exacta que dispara la llamada

`GET /api/inventario/eventos/{eventoId}/disponibilidad` (ruta pública que la UI consulta al
abrir el detalle de un evento):

```
InventoryController
  → GetEventAvailabilityService.getAvailability
    → GetAvailabilityService.getAvailability
      → EventLocalitiesPort.findByEvent
        → CatalogLocalitiesAdapter.findByEvent
          → WebClient GET {catalog.base-url}/api/eventos/{eventId}/localidades  (timeout 3 s)
```

Configuración: `catalog.base-url=${CATALOG_SERVICE_URL:http://catalog-service:8082}`.
Después, por cada localidad se ejecuta `RedisAvailabilityAdapter.initialize` (Lua idempotente,
`solo si no existe`) y `readAvailability`. Coste por petición: **1 HTTP a catálogo + N idas a Redis**.

### Mecanismo de comunicación

- **HTTP REST síncrono** con `WebClient` reactivo. **No** Kafka, **no** Redis compartido.
- Redis es propiedad exclusiva de inventario (`inv:stock:{eventId}:{localityId}` y su clave
  `:capacity`); catálogo no escribe ahí. En el Redis local quedan además claves huérfanas
  `availability:*` de una convención anterior (sin TTL ni lectura por el código actual).
- Catálogo sí publica por Kafka `eventos.publicados`, `eventos.actualizados` y `eventos.cancelados`
  (outbox + `Topics.java`), pero `inventory-service` **no tiene ningún consumidor** de esos topics.
- Inventario publica `reservas.confirmadas` para notificaciones/analítica (aún no implementadas).

### Reservas

`POST /api/reservas`, confirmar, cancelar y detalle **no llaman a catálogo**: usan Redis,
PostgreSQL y Kafka. Existe un acoplamiento indirecto: si nadie consultó disponibilidad antes,
el contador no existe y la reserva falla con `NOT_INITIALIZED` / `EventNotSellableException`.
DA-007 afirma "la primera vez que se consulta **o reserva**", pero el código siembra **solo en
consulta** (`grep .initialize(` devuelve una única ocurrencia, en `GetAvailabilityService:35`).

### CDN local

Técnicamente posible (proxy inverso Caddy/Nginx delante de LocalStack, o túnel cloudflared),
pero no recomendado: todo corre en la misma máquina, así que no hay distancia de red que
reducir; se añade un salto, otra capa de caché que invalidar y, con cloudflared, un viaje a
Internet. Decisión recomendada: **sin CDN en local**; Cloudflare solo en producción.

## Propuestas

- `DA-009`: caché de localidades en Redis con TTL corto (corto plazo, sin dependencias nuevas)
  y, como estado objetivo, consumidor de `eventos.publicados` / `eventos.actualizados`.
- CDN local: propuesta documentada en `ticketflow-api-gateway/docs_ia/decisiones/DA-002`
  con recomendación "no adoptar".

## Decisiones pendientes / autorización

No se modificó código, configuración, dependencias ni infraestructura. La implementación de
DA-009 y cualquier contenedor de CDN requieren autorización humana explícita.

> Nota: `DA-008` (historial de reservas para cualquier rol autenticado) y el registro de
> auditoría frente a `Microservicios.docx` siguen pendientes de la sesión anterior; este
> registro usa `P-016` y no los sobrescribe.

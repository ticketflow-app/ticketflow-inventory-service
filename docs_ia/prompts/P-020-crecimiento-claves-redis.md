# P-020 — Crecimiento de claves en Redis con el enfoque Kafka

- **Fecha:** 2026-11-21
- **Estado:** PROPUESTA
- **Relacionado:** `P-016` … `P-019`, `DA-009` (PROPUESTA)

## Objetivo

Valorar si el consumidor Kafka llenaría Redis con todos los eventos y cómo gestionar el ciclo
de vida de esas claves.

## Prompt del usuario (resumen literal)

> "Ah ya entendí, pero Redis se llenaría de todos los eventos, no?"

## Evidencia consultada

- `RedisAvailabilityAdapter.java:23,29,40-44,165`: prefijo real `inv:stock:`, clave de contador
  `inv:stock:{eventId}:{localityId}`, clave compañera `:capacity`, script Lua sin `EXPIRE`.
- Grep de `EXPIRE|@Scheduled|maxmemory` en `src/main/java` → **sin TTL ni limpieza programada**.
- `ticketflow-api-gateway/docker-compose.yml:76`: `redis:7-alpine`, sin `maxmemory`.
- Estado real del Redis local del stack: `DBSIZE = 20`, 10 claves `*:capacity`,
  `used_memory_human = 1.22M`, `maxmemory_human = 0B`, `maxmemory_policy = noeviction`.
- `--scan` muestra 10 claves `inv:stock:*` activas y 10 claves `availability:*` huérfanas de una
  convención anterior (no leídas por el código actual).

## Resumen de la respuesta

Sí, Redis crece — pero **ya crece hoy**, porque no hay TTL ni limpieza. Kafka no cambia la
cardinalidad final, solo el momento: hoy las claves se crean de forma perezosa cuando alguien
consulta disponibilidad; con Kafka se crearían al publicar el evento. En ambos casos hace falta
tener un contador por localidad vendible.

Orden de magnitud: un evento con 3 localidades produce ~6 claves de contador/capacidad; con la
proyección de localidades serían ~7–9. Con 1 000 eventos serían del orden de 1 MB. El Redis
local actual tiene 20 claves y 1.22 MB usados. El problema aparecería solo a escala de decenas
de miles de eventos o si se compartiera el Redis con otros usos (el de este compose también
sirve al rate limit del gateway y a user-service).

## Opciones de ciclo de vida

1. **Borrado explícito (recomendado):** consumir `eventos.cancelados` (trae `eventoId`) y borrar
   las claves del evento, más un job de limpieza tras la fecha del evento + gracia. Sin TTL,
   sin cold misses para eventos activos. Requiere nuevo consumidor y scheduler (sin dependencias nuevas).
2. **TTL en Redis:** simple, pero si expira mientras el evento está a la venta, la disponibilidad
   quedaría vacía; exigiría reconstruir desde Kafka (retención/replay) o volver a HTTP, que es
   justo lo que se quiere evitar. Solo seguro con TTL mayor que la vida del evento.
3. **Proyección en PostgreSQL de inventario + Redis solo para contadores calientes:** durable,
   reconstruible tras reinicio y sin presión de memoria; añade tabla/migración.
4. **`maxmemory` + política de evicción:** red de seguridad, no una estrategia de corrección.

## Pendientes

Sin cambios de código. `DA-009` sigue en PROPUESTA; el ciclo de vida de claves debe decidirse
como parte de esa propuesta o en una decisión posterior.

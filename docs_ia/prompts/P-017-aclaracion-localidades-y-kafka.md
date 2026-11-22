# P-017 — Aclaración: qué información se toma del catálogo y qué aporta Kafka

- **Fecha:** 2026-11-21
- **Estado:** PROPUESTA
- **Relacionado:** `P-016`, `DA-009` (PROPUESTA)

## Objetivo

Aclarar tres dudas derivadas de P-016: por qué inventario necesita datos del evento para
consultar disponibilidad, si esos datos pueden desactualizarse y si Kafka no acabaría
llamando igualmente al catálogo.

## Prompt del usuario (resumen literal)

> "¿Por qué se necesita la información del evento al consultar disponibilidad? Además creo que
> la información de un evento es algo que no cambia constantemente, entonces no va a estar
> desactualizado, como sí lo estaría la disponibilidad. Pero entonces, ¿qué ventajas daría usar
> Kafka si al final de cuentas se estaría volviendo a llamar al microservicio de catálogo?"

## Evidencia consultada

- `CatalogLocalitiesAdapter.java:38-54` (`LocalityView(String id, Integer capacity)`).
- `EventLocalitiesPort.java:12-20` (`EventLocality(localityId, capacity)`).
- `RedisAvailabilityAdapter.java:40-41` (script Lua `if EXISTS(availability) then return "0"`),
  `:88-99` (`initialize` con claves de disponibilidad y capacidad).
- `pom.xml:62` (`spring-kafka`, ya presente).
- `ticketflow-catalog-service/.../Topics.java:6-8`.

## Resumen de la respuesta

1. Inventario solo necesita el **id de cada localidad** (para saber qué contadores existen y
   devolver la lista completa) y su **capacidad** (para sembrar el contador la primera vez).
   No consulta nombre, fecha, precios ni imágenes: eso lo sirve catálogo directamente al front.
2. El dato es casi estático, por lo que la caché con TTL es razonable. Cambia solo cuando el
   organizador añade localidades o edita aforos; el contador ya sembrado **no se sobrescribe**
   por diseño (`EXISTS` en el script Lua), así que un cambio de aforo tras la primera venta se
   ignora igualmente. La disponibilidad, en cambio, cambia en cada reserva y nunca se cachea.
3. **Kafka no vuelve a llamar a catálogo.** Invierte la dirección: catálogo publica una vez
   (`eventos.publicados` / `eventos.actualizados`) e inventario guarda una proyección local;
   las peticiones de disponibilidad solo leen Redis. La alternativa que sí sigue llamando a
   catálogo en miss/expiración es la caché con TTL. El beneficio real de Kafka no es "datos más
   frescos", sino eliminar la dependencia de catálogo del camino caliente (arranque en frío,
   cache misses y picos de demanda) y seguir operativo si catálogo cae.

## Matización añadida a DA-009

Si se decide usar TTL, hay que aceptar una ventana de desactualización para localidades
recién añadidas. Si se quiere invalidación explícita, ya se está consumiendo el evento de
catálogo, es decir, casi todo el trabajo de la alternativa Kafka; conviene entonces hacer B
directamente.

## Pendientes

Sin cambios de código. `DA-009` sigue en PROPUESTA a la espera de autorización.

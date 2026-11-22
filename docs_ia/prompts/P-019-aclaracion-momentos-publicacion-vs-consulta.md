# P-019 — Aclaración: momento de publicación vs. momento de consulta

- **Fecha:** 2026-11-21
- **Estado:** PROPUESTA
- **Relacionado:** `P-016`, `P-017`, `P-018`, `DA-009` (PROPUESTA)

## Objetivo

Aclarar que el evento Kafka se publica al crear/publicar el evento (una vez), no al consultar
disponibilidad, y que el frontend sigue llamando únicamente a inventario.

## Prompt del usuario (resumen literal)

> "Había entendido mal, porque me explicaron que para ver la disponibilidad se hacía una llamada
> a inventario y ya, y esta internamente llamaba a catálogo. Pero en el diagrama con Kafka es como
> si para ver la disponibilidad se llamara a catálogo y este publicara un evento para que
> inventario lo consuma."

## Resumen de la respuesta

El modelo del usuario sobre el flujo actual es correcto: `Frontend → Inventario → (HTTP) → Catálogo`.

El diagrama anterior fue ambiguo al juntar dos momentos independientes:

- **Momento 1 (publicación del evento, una vez, acción del organizador):** catálogo recibe el
  comando HTTP, guarda el evento y publica `eventos.publicados`; inventario consume y guarda la
  copia de `localidad → aforo` en Redis.
- **Momento 2 (consulta de disponibilidad, cada vez):** frontend llama solo a inventario, que lee
  Redis. No intervienen ni catálogo ni Kafka.

Kafka no es petición/respuesta: es una suscripción. El dato se anuncia cuando nace, no cuando se
consulta. Lo que cambia no es a quién llama el frontend, sino cómo inventario obtuvo antes la
configuración estática (push en vez de pull por HTTP). La disponibilidad viva sigue en Redis y
se modifica con cada reserva; nunca viaja por Kafka.

## Pendientes

Sin cambios de código. `DA-009` permanece en PROPUESTA.

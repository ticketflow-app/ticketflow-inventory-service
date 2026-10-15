# P-009

## Identificador del prompt

P-009

## Fecha

2026-10-13

## Objetivo

Registrar la **aprobación humana** de la propuesta arquitectónica de la Fase 2 · Microtarea 1 (auditoría y propuesta de integración de Redis) y la creación de la **tarea de planificación T-004** (capacidad atómica de disponibilidad en Redis). La interacción se limitó a **preparación documental**: no se modificó código, dependencias, configuración, decisiones aprobadas ni documentos de contexto, y **no** se ejecutó implementación ni pruebas.

## Prompt completo utilizado

```text
La propuesta arquitectónica de la Fase 2, microtarea 2.1, queda aprobada con estas decisiones:
1. Implementar la reserva atómica mediante un script Lua en Redis.
2. Limitar la tarea a la capacidad atómica del adaptador y su puerto. No modificar CreateReservationService para orquestar reserva, persistencia y compensación; eso depende de resolver formalmente DA-001.
3. No modificar todavía el estado ni la dependencia relacionados con DA-002. Documentar la contradicción para una revisión posterior basada en evidencia.
4. Definir el contrato de inicialización y el comportamiento ante claves inexistentes. No interpretar silenciosamente una clave sin inicializar como disponibilidad cero.
5. Definir las condiciones de release para impedir que la disponibilidad supere la capacidad inicial.
6. Revisar las herramientas de pruebas existentes antes de incorporar dependencias o modificar pom.xml.

Ahora ejecuta únicamente la fase de preparación documental: [...] Genera el archivo de tarea [...]; registra las decisiones aprobadas y la trazabilidad [...] sin crear un sistema documental nuevo. No modifiques código, dependencias, decisiones previamente aprobadas ni documentos de contexto [...]. No cambies DA-001 ni DA-002 de estado. No ejecutes la implementación ni afirmes que se realizaron pruebas. [...] La aprobación de esta arquitectura y la creación de la tarea NO autorizan a escribir código.
```

## Archivos de contexto consultados

- `.clinerules` (§2 decisiones, §4 documentación, §6 dependencias, §8 autorización, §10 trazabilidad, §11 tareas).
- `docs_ia/tareas/PLANTILLA_TAREAS.md` y `PLANTILLA_PROMPT_CLINE.md` (formatos y nomenclatura).
- `docs_ia/tareas/T-003_verificacion_y_completado_puertos_salida.md` (ejemplo de tarea de implementación).
- `docs_ia/prompts/README.md` y `docs_ia/decisiones/README.md` (convenciones de ID y estados).
- `docs_ia/prompts/P-004-…md`, `P-005-…md`, `P-007-…md`, `P-008-…md` (formatos de análisis e implementación).
- `docs_ia/decisiones/DA-001-…redis-postgresql.md` (estado `PROPUESTA`), `DA-002-…resilience4j…md` (estado `PROPUESTA`), `DA-003-…eureka.md` (estado `APROBADA`).
- `docs_ia/contexto/contexto_negocio.md` (RB-08…RB-13, §10), `arquitectura/02_modelo_datos_persistencia.md`, `arquitectura/03_concurrencia_bloqueos.md`, `diseño/03_flujos_de_negocio.md`.

## Archivos del proyecto analizados

- `src/main/java/com/inventory/service/application/port/out/AvailabilityRepository.java` (solo `getAvailability`).
- `src/main/java/com/inventory/service/infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java` (solo `GET`; claves `inventory:availability:{eventId}:VIP|General`; `@Retry`).
- `src/main/java/com/inventory/service/application/usecase/CreateReservationService.java` (solo `save` + `publish`).
- `src/main/java/com/inventory/service/domain/model/Reservation.java` y `LocalityAvailability.java` (Fase 1).
- `pom.xml` (Redis reactivo presente; `resilience4j-spring-boot4:2.4.0` ya declarado).
- `src/main/resources/application.properties` (Redis `localhost:6379`; retry; sin timeouts).
- Classpath de pruebas (verificado en la Fase 1): JUnit 5, Mockito, AssertJ, Awaitility; **sin** `reactor-test`, Testcontainers ni Redis embebido.

## Archivos del proyecto creados o modificados

- `docs_ia/tareas/T-004_reserva_atomica_disponibilidad_redis.md` (**nuevo**): tarea de implementación de la capacidad atómica de Redis, con alcance aprobado, exclusiones, contrato de inicialización/claves inexistentes, tope de `release`, prueba de concurrencia obligatoria, riesgos y criterios de aceptación.
- `docs_ia/prompts/P-009-aprobacion-arquitectura-y-planificacion-reserva-redis.md` (**nuevo**, este registro).

> No se modificó código, `pom.xml`, `application.properties`, decisiones (`DA-001`/`DA-002` sin cambio de estado) ni documentos de `docs_ia/contexto/`.

## Resumen de la respuesta generada

- Se confirmó que Redis está integrado **solo para lectura**: `RedisAvailabilityAdapter` consulta dos contadores por evento y `CreateReservationService` no modifica disponibilidad; por tanto RB-09/RB-10/RB-11 (no-sobreventa bajo concurrencia) **no** están implementados.
- Se documentó que la atomicidad requiere **check-and-decrement en una sola operación atómica** (script Lua), y que integrar Redis no equivale por sí solo a resolver la no-sobreventa: debe demostrarse con una prueba de concurrencia.
- Se registró la **contradicción de gobernanza** de DA-002 (`PROPUESTA` vs. dependencia ya presente en `pom.xml`) para revisión posterior basada en evidencia, **sin** cambiar su estado.
- Se creó la tarea **T-004** con el alcance acordado y la **prueba de concurrencia obligatoria** como criterio de aceptación.

## Propuestas realizadas por la IA

- **T-004** (estado `PROPUESTA`): implementar `initialize`/`tryReserve` (Lua)/`release` (con tope de capacidad) y `getAvailability` en el puerto `AvailabilityRepository` y el adaptador `RedisAvailabilityAdapter`; unificar la clave a `availability:{event_id}:{section_id}`; definir comportamiento explícito ante claves inexistentes (nunca "0" silencioso).
- **Criterio obligatorio C1:** con `K` unidades y `M > K` solicitudes simultáneas, se aceptan exactamente `K`, la disponibilidad final es `0` y nunca negativa.
- Pendiente de autorización humana: herramienta de pruebas para Redis real (§9.1 de T-004) — si requiere Testcontainers u otra dependencia, se debe autorizar antes de tocar `pom.xml`.

## Decisiones pendientes

- **Autorización explícita para implementar** T-004 (la aprobación de la arquitectura y la creación de la tarea **no** autorizan escribir código).
- Herramienta de pruebas con Redis real (§9.1) y, de aplicar, autorización de la dependencia correspondiente.
- Regularización de la contradicción de DA-002 (marcar APROBADA con trazabilidad o revertir), con decisión humana independiente.
- Timeouts de Redis en `application.properties` (opcional, no requisito de esta tarea).
- Resolución de DA-001 para habilitar la futura orquestación reserva→persistencia→compensación en `CreateReservationService`.

## Estado

PROPUESTA

> La planificación quedó registrada y la arquitectura aprobada, pero la implementación de T-004 permanece **pendiente de autorización humana explícita**. No se ejecutó código ni pruebas en esta interacción.

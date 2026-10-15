# P-010

## Identificador del prompt

P-010

## Fecha

2026-10-13

## Objetivo

Actuar como `@architect` para la **revisión técnica final de T-004** (capacidad atómica de disponibilidad en Redis), precisando los contratos de `initialize` y `release`, confirmando el formato de claves y resolviendo la estrategia de pruebas con Redis real. La interacción se limitó a **revisión y preparación documental**: no se implementó código, no se modificaron dependencias ni configuración, no se ejecutó la tarea T-004 y no se hicieron commits.

## Prompt completo utilizado

```text
Actúa como @architect y revisor técnico [...] Dejar la tarea docs_ia/tareas/T-004 [...] técnicamente precisa y lista para implementar, resolviendo las observaciones pendientes sobre initialize, release y las pruebas con Redis real. [...] Precisar el contrato de initialize [...] Precisar el contrato de release [...] Confirmar el formato de claves [...] Resolver la estrategia de pruebas [...] No modifiques pom.xml: registra qué dependencia se requiere, por qué y qué autorización falta. [...] esta instrucción autoriza exclusivamente la revisión y preparación documental. No implementes código, no agregues dependencias, no ejecutes la tarea T-004 [...] Al terminar, espera mi autorización explícita para comenzar la implementación.
```

## Archivos de contexto consultados

- `.clinerules` (§2 decisiones, §6 dependencias, §8 autorización, §10 trazabilidad).
- `docs_ia/tareas/T-004_reserva_atomica_disponibilidad_redis.md` (objeto de la revisión).
- `docs_ia/prompts/P-009-aprobacion-arquitectura-y-planificacion-reserva-redis.md` (registro de aprobación previo).
- `docs_ia/contexto/arquitectura/02_modelo_datos_persistencia.md` (clave `availability:{event_id}:{section_id}`), `03_concurrencia_bloqueos.md` (DECRBY/INCRBY, compensación, idempotencia), `04_diseno_api_rest.md` (`sectionId` en request/response).
- `docs_ia/decisiones/DA-001` (`PROPUESTA`), `DA-002` (`PROPUESTA`, contradicción documentada en T-004 §4.1).

## Archivos del proyecto analizados

- `src/main/java/com/inventory/service/application/port/out/AvailabilityRepository.java` (solo `getAvailability(Long)`).
- `src/main/java/com/inventory/service/infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java` (solo `GET`; claves `inventory:availability:{eventId}:VIP|General`, líneas 24-25; `@Retry`).
- Entorno (comprobación no destructiva): `Test-NetConnection localhost:6379` → cerrado; `redis-cli` no instalado; **Docker disponible**; classpath de pruebas sin `reactor-test`/Testcontainers/Redis embebido.

## Archivos del proyecto creados o modificados

- `docs_ia/tareas/T-004_reserva_atomica_disponibilidad_redis.md` (**modificado**): se añadieron §4.2 (contratos de `initialize`/`tryReserve`/`release`), §4.3 (formato de claves y mapeo `sectionId`↔`locality`), se reescribió §9.1 (hallazgos de entorno y estrategia reproducible con Docker) y §9.2 (casos C1–C6), y se ampliaron §8 (criterios C4–C7) y §10 (decisiones pendientes).
- `docs_ia/prompts/P-010-revision-tecnica-t004-redis.md` (**nuevo**, este registro).

> No se modificó código, `pom.xml`, `application.properties`, decisiones (`DA-001`/`DA-002` sin cambio de estado) ni documentos de `docs_ia/contexto/`.

## Resumen de la respuesta generada

- **`initialize`:** siembra la disponibilidad **solo si no existe** (Lua atómico `EXISTS`+`SET`), distinguiendo `INITIALIZED` vs. `ALREADY_INITIALIZED`; valida `capacity >= 0` e identificadores; no sobrescribe ni reinicia una disponibilidad en uso.
- **`tryReserve`:** check-and-decrement atómico por Lua; `NOT_INITIALIZED` / `INSUFFICIENT_AVAILABILITY` (sin decrementar) / `RESERVED`; nunca deja el contador negativo (RB-08/RB-11).
- **`release`:** incremento acotado a la capacidad de referencia (`…:capacity`); valida `quantity > 0`; distingue `NOT_INITIALIZED`. Se documenta expresamente que el tope **no** impide liberaciones duplicadas (requiere identificar la reserva e idempotencia, fuera de alcance, ligado a DA-001).
- **Claves:** formato objetivo `availability:{event_id}:{section_id}` + clave compañera `…:capacity`; `section_id` = valor recibido como `sectionId` (equivalente a `locality`). Se documenta la discrepancia código↔doc (prefijo `inventory:` y secciones fijas) que T-004 corrige.
- **Pruebas:** el entorno no tiene Redis corriendo ni `redis-cli`, pero **sí Docker**. Estrategia por defecto sin dependencias nuevas: `docker run ... redis:7-alpine` + pruebas de integración con `.block()`/AssertJ, aisladas de `mvnw test`. **C1** cuenta explícitamente `K` `RESERVED` vs. `M−K` `INSUFFICIENT_AVAILABILITY`, disponibilidad final `0`, nunca negativa.

## Propuestas realizadas por la IA

- Contratos precisos de `initialize`/`tryReserve`/`release` (§4.2) y de claves (§4.3), con enums de resultado `InitializeOutcome`/`ReservationOutcome` en `application.port.out` (sin tocar el dominio).
- Estrategia de pruebas con Redis real vía Docker (§9.1/§9.2), con C1–C7 como criterios verificables.

## Decisiones pendientes

- **Autorización explícita para implementar** T-004 (sigue pendiente; esta revisión no la otorga).
- **Tooling de pruebas:** Redis real vía Docker (sin dependencias) **vs.** Testcontainers (requiere `pom.xml` + autorización). Registrado en §10 de T-004.
- Cómo aislar de `mvnw test` las pruebas que requieren Redis.
- Timeouts de Redis (opcional).
- Resolución de DA-001 (orquestación y compensación) y regularización de DA-002, ambas fuera de esta revisión.

## Estado

PROPUESTA

> Revisión técnica documental completada; T-004 queda técnicamente precisa y lista para una autorización de implementación posterior. No se ejecutó código ni pruebas de implementación en esta interacción.

# Backlog de tareas — ticketflow-inventory-service

> Registro de tareas según `.clinerules` §11. Cada entrada tiene objetivo concreto, alcance definido y criterios de aceptación verificables. Las tareas nuevas son **propuestas** y requieren autorización humana antes de ejecutarse. Un estado `En Progreso` solo es válido con autorización explícita. Solo se marca `COMPLETADA` con evidencia suficiente.

## Resumen de estados

| ID | Tarea | Tipo | Estado |
|---|---|---|---|
| T-004 | Reserva atómica de disponibilidad en Redis (Lua) | IMPLEMENTACIÓN | **COMPLETADA** |
| DA-001-R | Revisión de DA-001: patrón Saga/compensación para `CreateReservationService` | DOCUMENTACIÓN | **COMPLETADA** (APROBADA 2026-10-14) |
| DA-002-R | Revisión de DA-002: recomendación de retirar Resilience4j | DOCUMENTACIÓN | **COMPLETADA** (APROBADA 2026-10-14) |
| DA-005 | Nueva propuesta: Testcontainers (PostgreSQL/Redis, scope test) | DOCUMENTACIÓN | **COMPLETADA** (APROBADA 2026-10-14) |
| BL-001 | Creación de este backlog | DOCUMENTACIÓN | **COMPLETADA** (2026-10-14, orden humana explícita) |
| T-005 | Compensación Saga en `CreateReservationService` (DA-001) | IMPLEMENTACIÓN | **COMPLETADA** (2026-10-14) |
| T-006 | Retiro de Resilience4j + reintento nativo en `getAvailability` (DA-002) | IMPLEMENTACIÓN | **COMPLETADA** (2026-10-14) |
| T-007 | Eliminación de claves Redis obsoletas `inventory:availability:*` | MANTENIMIENTO | **COMPLETADA** (2026-10-14, purga autorizada) |
| T-008 | Migración de IT a Testcontainers (DA-005) | PRUEBAS | **COMPLETADA** (2026-10-14) |
| T-009 | Prueba de compensación por fallo de PostgreSQL en reserva | PRUEBAS | **COMPLETADA** (2026-10-14) |
| T-010 | Reporte final de verificación T-004 (`docs_ia/reportes/T-004_resolucion.md`) | DOCUMENTACIÓN | **COMPLETADA** (2026-10-14) |

## Detalle de tareas (T-005 a T-010 — todas COMPLETADAS el 2026-10-14)

### T-005 — Compensación Saga en `CreateReservationService` (DA-001)

- **Objetivo:** orquestar reserva→persistencia→compensación: `tryReserve` atómico en Redis; si la persistencia en PostgreSQL falla, ejecutar `release` para restaurar el stock y propagar error controlado; publicar el evento solo tras persistencia exitosa.
- **Alcance:** `application/usecase/CreateReservationService.java` (+ registro de incidente si se aprueba en DA-001). No toca dominio, controladores ni `ReservationPersistenceAdapter`.
- **Criterios de aceptación:** (1) reserva rechazada sin persistir si Redis devuelve `INSUFFICIENT_AVAILABILITY`/`NOT_INITIALIZED`; (2) fallo de `save` ⇒ `release` ejecutado y stock restaurado (verificable con T-009); (3) `publish` solo con `save` exitoso; (4) `mvnw -o -B test` en verde; (5) trazabilidad actualizada.
- **Resultado:** COMPLETADA el 2026-10-14. Implementada la saga corta en `CreateReservationService` (bloqueo previo por aprobación DA-001 resuelto). Verificado por T-009 y por `CreateReservationServiceCompensationIT`.

### T-006 — Retiro de Resilience4j + reintento nativo (DA-002)

- **Objetivo:** retirar `resilience4j-spring-boot4` y `resilience4j-reactor` del `pom.xml`; eliminar las propiedades `resilience4j.*` de `application.properties`; reintento nativo de Reactor (`Retry.backoff`/`retryWhen`) en `RedisAvailabilityAdapter.getAvailability` (3 intentos, backoff 500 ms, equivalente a la config huérfana actual).
- **Alcance:** `pom.xml`, `application.properties`, `RedisAvailabilityAdapter.java`. Sin cambios de contratos ni de dominio.
- **Criterios de aceptación:** (1) ninguna dependencia `io.github.resilience4j` en `pom.xml`; (2) ninguna propiedad `resilience4j.*` en `application.properties`; (3) `getAvailability` con reintento verificado (test con fallo simulado); (4) compile/test/IT en verde; (5) trazabilidad actualizada.
- **Resultado:** COMPLETADA el 2026-10-14. `resilience4j-spring-boot4`/`resilience4j-reactor` retiradas de `pom.xml`; propiedades `resilience4j.*` eliminadas de `application.properties`; reintento nativo de Reactor (`Retry.backoff`) en `getAvailability` (no se reincorporó `@Retry` para no reintroducir la dependencia retirada, según la contradicción documentada en P-013).

### T-007 — Eliminación de claves Redis obsoletas `inventory:availability:*`

- **Objetivo:** purgar del contenedor `inventory-redis` las claves con el prefijo antiguo `inventory:availability:*` (actualmente `inventory:availability:25:VIP` y `…:General`) para que solo opere el formato unificado `availability:{event_id}:{section_id}`.
- **Alcance:** operación `SCAN`+`DEL` **solo** sobre el patrón `inventory:availability:*`; sin tocar claves `availability:*` ni de otros servicios.
- **Criterios de aceptación:** (1) `--scan --pattern "inventory:availability:*"` vacío tras la operación; (2) claves ajenas intactas; (3) registro del comando y del antes/después en la trazabilidad.
- **Resultado:** COMPLETADA el 2026-10-14 (purga autorizada explícitamente por el humano). `--scan --pattern "inventory:availability:*"` vacío tras la operación; `dbsize` pasó de 2 a 0; solo existían las 2 claves antiguas (no había claves nuevas ni ajenas en este Redis). Detalle en `docs_ia/reportes/T-004_resolucion.md`.

### T-008 — Migración de IT a Testcontainers (DA-005)

- **Objetivo:** que `RedisAvailabilityAdapterIT` use contenedores efímeros de Redis (y PostgreSQL donde haga falta) en lugar del contenedor manual `inventory-redis`.
- **Alcance:** `pom.xml` (dependencias con `<scope>test</scope>`) y `src/test/`. Sin cambios en producción.
- **Criterios de aceptación:** (1) las IT pasan sin contenedores manuales en ejecución; (2) dependencias solo en scope test; (3) `mvnw -o -B verify` (o `test -Dtest=…IT`) en verde; (4) trazabilidad actualizada.
- **Resultado:** COMPLETADA el 2026-10-14. `RedisAvailabilityAdapterIT` corre contra Redis efímero (`redis:7-alpine`) vía Testcontainers; sin contenedor manual. Dependencias en scope `test` (BOM + junit-jupiter/postgresql).

### T-009 — Prueba de compensación por fallo de PostgreSQL

- **Objetivo:** prueba que simula fallo de `reservationRepository.save` tras un `tryReserve` exitoso y verifica que `release` restaura el stock en Redis (semántica exigida por la Fase 4 de la orden y por DA-001).
- **Alcance:** `src/test/` (Mockito para el puerto de persistencia y Redis efímero real o doble, según DA-005).
- **Criterios de aceptación:** (1) el fallo de `save` provoca exactamente un `release` con la misma cantidad; (2) la disponibilidad final en Redis vuelve al valor previo; (3) no se publica evento; (4) el error llega al cliente; (5) resultado registrado.
- **Resultado:** COMPLETADA el 2026-10-14. `CreateReservationServiceCompensationIT` (3 casos) verifica que el fallo de `save` provoca exactamente un `release` que restaura el stock, sin publicar evento y propagando el error. Suite IT en verde (12/12).

### T-010 — Reporte final T-004_resolucion.md

- **Objetivo:** generar `docs_ia/reportes/T-004_resolucion.md` verificando con evidencias que las 6 observaciones del informe T-004 quedaron resueltas (DA-001, DA-002, claves obsoletas, reintento en `getAvailability`, Testcontainers y trazabilidad).
- **Alcance:** inspección de repo/`pom.xml`/código/tests + verificación de la purga de claves; solo escritura del reporte.
- **Criterios de aceptación:** (1) las 6 observaciones evaluadas con evidencia comprobable; (2) comandos de verificación listados con resultados reales; (3) limitaciones declaradas; (4) reporte en `docs_ia/reportes/`.
- **Resultado:** COMPLETADA el 2026-10-14. Reporte generado en `docs_ia/reportes/T-004_resolucion.md` con evidencia de las 6 observaciones resueltas.

## Notas de gobernanza

- Las decisiones DA-001, DA-002 y DA-005 quedaron en estado `APROBADA` el 2026-10-14 tras la **aprobación humana explícita por chat** (registrada en P-013). Con ello se autorizó la modificación de `CreateReservationService`, `RedisAvailabilityAdapter`, `pom.xml` y `application.properties`, y la purga destructiva de claves (T-007).
- La purga de claves (T-007) fue una operación destructiva **autorizada explícitamente** por el humano antes de ejecutarse; se limitó al patrón `inventory:availability:*` y se documentó el antes/después (ver `docs_ia/reportes/T-004_resolucion.md`).
- Este archivo se crea por orden humana explícita de 2026-10-14 (resuelve la discrepancia registrada en T-004 §10 y P-012 respecto de `.clinerules` §11).

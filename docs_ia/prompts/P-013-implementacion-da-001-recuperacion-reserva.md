# P-013

## Identificador del prompt

P-013

## Fecha

2026-10-13

## Objetivo

Implementar `DA-001` del `inventory-service`: el mecanismo de recuperación ante fallo de persistencia en la reserva (consistencia Redis → PostgreSQL), incluyendo el flujo `DECRBY` del §10.4, la compensación con `INCRBY`, el registro durable de incidentes y la reconciliación periódica.

## Prompt completo utilizado

```text
"implementa el DA-001"
```

## Archivos de contexto consultados

- `AGENTS.md` (raíz y del servicio) — gobernanza §1, §2, §3, §5, §8, §9, §10.
- `docs_ia/decisiones/DA-001-mecanismo-recuperacion-reserva-redis-postgresql.md` (Alternativa A recomendada).
- `docs_ia/contexto/arquitectura/arquitectura.md` §10.2-§10.4, §14-§15 (clave `availability:{evento_id}:{seccion_id}`, `DECRBY`/`INCRBY`, idempotencia).
- `docs_ia/contexto/requisitos/casos_uso.md` CU-06 (flujos alternativos FA-01, FA-02, FA-03).
- `docs_ia/contexto/requisitos/historias_usuarios.md` HU-11 (configurar capacidad), HU-12 (crear reserva).
- `docs_ia/contexto/calidad/escenarios_calidad.md` §3 (disponibilidad limitada).
- `docs_ia/contexto/pruebas/estrategia_pruebas.md` §6 y `pruebas_cargas.md` §7 (concurrencia, disponibilidad inicial 100).

## Archivos del proyecto analizados

- `domain/model/{Reservation,Availability,LocalityAvailability}.java`.
- `application/port/{in,out}/...`, `application/usecase/{CreateReservationService,GetAvailabilityService}.java`.
- `infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java`.
- `infrastructure/adapter/out/persistence/{ReservationPersistenceAdapter,ReservationJpaEntity,ReservationJpaRepository}.java`.
- `infrastructure/adapter/in/controller/{ReservationController,AvailabilityController}.java`.
- `src/main/resources/db/migration/V1__create_reservations.sql`, `application.properties`.
- `src/test/.../ServiceApplicationTests.java` y `src/test/resources/application.properties`.

## Resumen de la respuesta generada

Se determinó que `DA-001` **asumía** un flujo `DECRBY` que no existía (el servicio solo persistía/publicaba y Redis solo se leía), por lo que la implementación incluyó primero el §10.4. Además, ni la reconciliación ni las pruebas de concurrencia eran posibles sin una fuente de capacidad (HU-11), hoy inexistente. Se implementó la Alternativa A completa: `DECRBY` + rechazo/reversión, compensación `INCRBY` con reintentos, incidentes durables, reconciliación periódica y capacidad de localidad.

Se expusieron al usuario las decisiones de implementación no definidas en el contexto (intervalo de reconciliación; endpoint/tabla de capacidad); el usuario no respondió a la consulta de alcance y había dado la orden explícita de implementar, por lo que se procedió con el plan completo y se documentaron las decisiones en `DA-001`.

## Propuestas realizadas por la IA

- `DA-001` (inventory) pasa a `APROBADA — IMPLEMENTADA` con la Alternativa A.
- Decisiones de implementación documentadas dentro de `DA-001`: intervalo de reconciliación (5 min, parametrizable) y endpoint/tabla de capacidad (HU-11).

## Decisiones pendientes

- **Idempotencia** de `POST /reservations` (§15) — fuera de alcance de esta decisión.
- **Exponer** `PUT /events/{eventId}/localities/{locality}/capacity` por el API Gateway (requiere ruta nueva en `ticketflow-api-gateway`).
- **Unificar** la clave de disponibilidad (hoy conviven `availability:{evento_id}:{seccion_id}` en docs y `inventory:availability:{eventId}:{locality}` en código).
- Pruebas de **integración (Testcontainers)** y de **concurrencia** pendientes según la estrategia.

## Autorización humana

Instrucción humana explícita: **"implementa el DA-001"** (2026-10-13). Se interpreta como autorización para implementar la decisión `DA-001`.

## Cambios aplicados

Migraciones:
- `db/migration/V2__create_reservation_incidents.sql` (nueva).
- `db/migration/V3__create_locality_capacity.sql` (nueva).

Dominio / aplicación:
- `domain/model/LocalityCapacity.java`, `domain/model/ReservationIncident.java` (nuevos).
- `application/exception/{InsufficientAvailabilityException,ReservationPersistenceException}.java` (nuevos).
- `application/port/out/{LocalityCapacityRepository,ReservationIncidentRepository}.java` (nuevos); `AvailabilityRepository` y `ReservationRepository` ampliados.
- `application/port/in/{ConfigureCapacityUseCase,ReconcileAvailabilityUseCase}.java` y `application/port/in/dto/ConfigureCapacityRequest.java` (nuevos).
- `application/usecase/{CreateReservationService (reescrito),ConfigureCapacityService,ReconcileAvailabilityService}.java`.

Infraestructura:
- `infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java` (reescrito: `tryReserve`/`release`/`setAvailability`/`getCounter`).
- `infrastructure/adapter/out/persistence/ReservationPersistenceAdapter.java` y `ReservationJpaRepository.java` (suma de CONFIRMED).
- `infrastructure/adapter/out/persistence/{LocalityCapacityJpaEntity,LocalityCapacityJpaRepository,LocalityCapacityPersistenceAdapter,ReservationIncidentJpaEntity,ReservationIncidentJpaRepository,ReservationIncidentPersistenceAdapter}.java` (nuevos).
- `infrastructure/adapter/in/controller/{CapacityController,GlobalExceptionHandler}.java` (nuevos).
- `infrastructure/scheduling/AvailabilityReconciliationJob.java` (nuevo).
- `ServiceApplication.java` (`@EnableScheduling`); `application.properties` (propiedades `inventory.reconciliation.*`).

Pruebas:
- `test/.../application/usecase/CreateReservationServiceTest.java` (nuevo, 6 pruebas).
- `test/.../application/usecase/ReconcileAvailabilityServiceTest.java` (nuevo, 3 pruebas).

## Comandos ejecutados

- `.\mvnw.cmd test` (desde `ticketflow-inventory-service`).

## Pruebas realizadas y resultado

- `.\mvnw.cmd test` → **10/10** pruebas verdes (`CreateReservationServiceTest` 6, `ReconcileAvailabilityServiceTest` 3, `ServiceApplicationTests` 1); sin fallos ni errores.
- Pendiente: verificación con infraestructura real (Redis/PostgreSQL/Kafka) del `DECRBY`, la compensación y el endpoint de capacidad, y las pruebas de integración/concurrencia con Testcontainers.

## Pendientes

- Idempotencia (§15).
- Ruta de gateway para el endpoint de capacidad.
- Testcontainers + prueba de concurrencia.
- Alinear `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§7/§8/§10 dicen WebFlux/R2DBC) y `AGENTS.md` del servicio, que siguen desactualizados respecto a `DA-005`.

## Estado

IMPLEMENTADO

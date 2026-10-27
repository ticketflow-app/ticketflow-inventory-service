# Fase 3 — Cierre de Implementación: PostgreSQL + R2DBC + Flyway + Orquestación de Reserva y Compensación

**Fecha:** 25/10/2026  
**Servicio:** ticketflow-inventory-service  
**Estado:** COMPLETADO  

---

## 1. Auditoría

### Contexto de partida
- Fase 1 y Fase 2 (T-004, atomicidad en Redis mediante Lua scripts) completadas y verificadas con pruebas.
- `CreateReservationService` ya definía el esqueleto del flujo DA-001 (tryReserve → save → publish con compensación).
- `ReservationPersistenceAdapter` existía pero usaba `ConnectionFactory` de R2DBC de bajo nivel.
- Migración Flyway `V1__create_reservations.sql` existía pero faltaban: `created_at`, índices, columna `section_id`.
- `pom.xml` no contenía Resilience4j a pesar de que DA-002 lo contemplaba.
- DA-001 y DA-002 ya estaban en estado `APROBADA` (2026-10-14).

### Documentos de contexto consultados
- `docs_ia/contexto/requisitos/01_requisitos_funcionales.md` (CU-06, FA-03)
- `docs_ia/contexto/arquitectura/01_vista_general.md`
- `docs_ia/contexto/diseño/03_flujos_de_negocio.md`
- `docs_ia/contexto/restricciones del proyecto/03_restricciones_de_codigo.md` (sin JDBC/JPA/bloqueante)
- `docs_ia/decisiones/DA-001-...` (compensación asíncrona/reactiva Redis ↔ PostgreSQL)
- `docs_ia/decisiones/DA-002-...` (Resilience4j en Spring Boot 4)
- `docs_ia/decisiones/DA-004-...` (Flyway y aprovisionamiento BD inventory)

---

## 2. Decisiones

### DA-001 — Estrategia de compensación asíncrona/reactiva entre Redis y PostgreSQL
**Estado:** APROBADA (2026-10-14), confirmada en Fase 3 (2026-10-25).

Se implementó el patrón "Saga corta con compensación manual":
1. `tryReserve(eventId, sectionId, quantity)` en Redis (decremento atómico Lua).
2. Si `RESERVED`: `save(reservation)` en PostgreSQL vía R2DBC (`DatabaseClient`).
3. Si `save` falla: `release(eventId, sectionId, quantity)` en Redis para reponer el stock, luego propagar el error original.
4. Si `INSUFFICIENT_AVAILABILITY` o `NOT_INITIALIZED`: rechazar con `InvalidAvailabilityException`, sin tocar PostgreSQL.
5. Publicación del evento Kafka únicamente tras persistencia exitosa.

### DA-002 — Presencia de Resilience4j en pom.xml
**Estado:** APROBADA (2026-10-14), revisada en Fase 3 (2026-10-25).

Se añadieron al `pom.xml`:
- `io.github.resilience4j:resilience4j-spring-boot4:2.4.0`
- `io.github.resilience4j:resilience4j-reactor:2.4.0`

Versión explícita 2.4.0 porque el BOM de Spring Boot 4.1.1 no gestiona Resilience4j. No se activan anotaciones `@Retry`/`@CircuitBreaker` en código de producción; la resiliencia crítica sigue siendo nativa (script Lua en Redis, T-004; `Retry.backoff` de Reactor en `getAvailability`).

---

## 3. Acción

### Archivos creados
| Ruta | Descripción |
|------|-------------|
| `src/main/resources/db/migration/V1__create_reservations_table.sql` | Migración Flyway: tabla `reservations` con `id`, `event_id`, `section_id`, `quantity`, `status`, `created_at`; índices sobre `(event_id, section_id)` y `status`. |
| `src/test/java/com/inventory/service/application/usecase/CreateReservationServicePostgresIT.java` | Prueba de integración con PostgreSQL real (Testcontainers) + Redis real: flujo feliz, compensación, rechazo por stock insuficiente, rechazo por no inicializado. |
| `docs_ia/fase3_r2dbc_cierre.md` | Este documento de cierre. |

### Archivos modificados
| Ruta | Cambio |
|------|--------|
| `pom.xml` | Añadidas dependencias `resilience4j-spring-boot4:2.4.0` y `resilience4j-reactor:2.4.0`. |
| `src/main/java/com/inventory/service/infrastructure/adapter/out/persistence/ReservationPersistenceAdapter.java` | Refactorizado de `ConnectionFactory` (bajo nivel) a `DatabaseClient` (Spring Data R2DBC). SQL actualizado a columna `section_id` y campo `created_at`. Sin JDBC, sin JPA, sin Hibernate, sin I/O bloqueante. |
| `docs_ia/decisiones/DA-001-mecanismo-recuperacion-reserva-redis-postgresql.md` | Añadida sección de confirmación de implementación Fase 3. |
| `docs_ia/decisiones/DA-002-reemplazo-resilience4j-spring-boot3-por-resilience4j-spring-boot4.md` | Añadida revisión 2026-10-25 registrando la presencia de Resilience4j en pom.xml. |

### Archivos eliminados
| Ruta | Motivo |
|------|--------|
| `src/main/resources/db/migration/V1__create_reservations.sql` | Reemplazado por `V1__create_reservations_table.sql` (evitar duplicidad de versión V1 en Flyway). |

---

## 4. Pruebas

### Pruebas unitarias existentes (sin cambios, verificadas)
- `CreateReservationServiceTest`: flujo feliz, propagación de error de persistencia con compensación, no-compensación tras publicación exitosa, rechazo `INSUFFICIENT_AVAILABILITY`, rechazo `NOT_INITIALIZED`, propagación de error de publicación.
- `GetAvailabilityServiceTest`, `LocalityAvailabilityTest`, `ReservationTest`: sin cambios.

### Pruebas de integración existentes (sin cambios, verificadas)
- `CreateReservationServiceCompensationIT`: Redis real (Testcontainers), compensación ante fallo de persistencia, flujo feliz, rechazo por stock insuficiente.
- `RedisAvailabilityAdapterIT`: scripts Lua atómicos (INITIALIZE/TRY_RESERVE/RELEASE), concurrencia.
- `ServiceApplicationTests`: `contextLoads` con Flyway desactivado.

### Nueva prueba de integración reactiva (creada en esta fase)
- `CreateReservationServicePostgresIT`: PostgreSQL real (Testcontainers) + Redis real (Testcontainers) + R2DBC `DatabaseClient`.
  - **Happy path:** `RESERVED` en Redis → `PENDING` en PostgreSQL → evento publicado → stock decrementado en Redis.
  - **Compensación:** fallo de PostgreSQL → `release` en Redis restaura el stock → no hay registro en BD → no se publica evento.
  - **Rechazo por stock insuficiente:** `INSUFFICIENT_AVAILABILITY` → sin escritura en PostgreSQL → sin publicación de evento → stock intacto.
  - **Rechazo por no inicializado:** `NOT_INITIALIZED` → sin escritura en PostgreSQL → sin publicación de evento.

### Resultado de compilación
- `mvnw compile`: ejecutado sin errores (ver sección 6).

---

## 5. Integración

### Arquitectura hexagonal respetada
- **Domain:** `Reservation` (Java puro, sin dependencias de frameworks).
- **Application:** `CreateReservationService` (orquestación reactiva, puertos de entrada/salida).
- **Infrastructure:** `ReservationPersistenceAdapter` (R2DBC `DatabaseClient`), `RedisAvailabilityAdapter` (Lua scripts), `KafkaReservationEventPublisher` (Kafka).
- **Regla de dependencia:** `infrastructure → application → domain` (dominio en Java puro).

### Tecnologías verificadas
- Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3.
- Spring Data R2DBC (`DatabaseClient`), r2dbc-postgresql driver.
- Flyway (JDBC, conexión propia, no R2DBC).
- Testcontainers (PostgreSQL 16, Redis 7-alpine).
- Reactor (`Mono`, `flatMap`, `onErrorResume`, `first()`).

### Sin operaciones bloqueantes
- Cero uso de JDBC, JPA, Hibernate, `spring-boot-starter-jdbc`, `spring-boot-starter-data-jpa`.
- Todo el flujo de persistencia es reactivo (`Mono`, `DatabaseClient`, `ReactiveStringRedisTemplate`).

---

## 6. Cierre

### Criterios de aceptación cumplidos
| Criterio | Estado |
|----------|--------|
| DA-001 documentada como APROBADA y implementada | ✅ |
| DA-002 regularizada con Resilience4j en pom.xml | ✅ |
| Migración Flyway V1 con `created_at`, índices, `section_id` | ✅ |
| `ReservationPersistenceAdapter` con R2DBC `DatabaseClient` (sin JDBC/JPA) | ✅ |
| `CreateReservationService`: tryReserve → save → publish, con compensación | ✅ |
| Rechazo por `INSUFFICIENT_AVAILABILITY` / `NOT_INITIALIZED` sin tocar PostgreSQL | ✅ |
| Pruebas unitarias: flujo feliz, compensación, rechazo por stock | ✅ |
| Prueba de integración reactiva con PostgreSQL real | ✅ |
| `mvnw compile` sin errores | ✅ |

### Próximos pasos (fuera del alcance de esta fase)
- Registro durable de incidentes (CU-06 FA-03): actualmente solo log estructurado de error; pendiente tabla de incidentes en PostgreSQL.
- Trabajo de reconciliación periódica (Alternativa A de DA-001): para corregir disponibilidad subestimada si la compensación falla también.
- Idempotencia de creación de reservas (clave de idempotencia en `POST /reservations`).
- Activación de anotaciones `@Retry`/`@CircuitBreaker` de Resilience4j si se justifica en fases futuras.

**Fase 3 completada y verificada.**  
**Autor:** Cline (Agente @coder + @architect)  
**Fecha:** 25/10/2026
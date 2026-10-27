# DA-006

## ID
DA-006

## Título
Cierre de gaps T-004/T-005: idempotencia de reservas, contrato HTTP/rutas/DTO, observabilidad de fallos de compensación y reconciliación Redis ↔ PostgreSQL

## Estado
PROPUESTA

---

## 1. Contexto

El microservicio Inventory (`ticketflow-inventory-service`, puerto 8082) implementa reservas de entradas con una arquitectura hexagonal reactiva (Java 21, Spring Boot 4.1.1, WebFlux):

- **Redis** (ReactiveStringRedisTemplate + scripts Lua atómicos): fuente de verdad operativa de disponibilidad; claves `availability:{event_id}:{section_id}`; operaciones `DECRBY` / `INCRBY`.
- **PostgreSQL** (Spring Data R2DBC): registro persistente de reservas; tabla `reservations` creada por Flyway (migración `V1__create_reservations.sql`).
- **Kafka**: publicación de eventos `reservationEventPublisher.publish(...)` tras persistencia exitosa.
- **Actuator + Micrometer**: métricas y endpoints de salud (`health`, `info`, `metrics`).
- **Eureka**: servicio registrado como `ticketflow-inventory-service` (DA-003).

Decisiones ya aprobadas y relevantes:

- **DA-001** (APROBADA 2026-10-14): flujo "saga corta" en `CreateReservationService` — `tryReserve` (Redis) → `save` (PostgreSQL) → `publish` (Kafka); si la persistencia falla, compensación `release` en Redis y error al cliente. El registro durable de incidentes y la reconciliación periódica quedan como trabajo futuro; hoy el incidente solo se registra mediante log estructurado.
- **DA-002** (APROBADA): Resilience4j retirado; reintento nativo de Reactor en lecturas.
- **DA-004** (APROBADA): Flyway + esquema `V1` sin columna `idempotency_key`.
- **DA-005** (APROBADA): Testcontainers para pruebas de integración.

Los documentos de contexto (`docs_ia/contexto/`) definen requisitos que todavía no están implementados:

- `04_diseno_api_rest.md`: `POST /api/v1/reservations` requiere cabecera obligatoria `Idempotency-Key: <UUID>`; cuerpo `{ eventId, sectionId, quantity }`; respuesta `201` con `{ reservationId, userId, eventId, sectionId, quantity, status, createdAt }`. Rutas con prefijo `/api/v1/`.
- `03_concurrencia_bloqueos.md` §3: idempotencia mediante `Idempotency-Key`; consulta a `reservations` por `idempotency_key`; si ya existe, retorna la respuesta almacenada sin `DECRBY` ni nueva reserva.
- `02_modelo_datos_persistencia.md`: tabla `reservations` con `idempotency_key VARCHAR(255) UNIQUE, INDEX` y `created_at`.
- `02_disponibilidad_y_resiliencia.md` §5 y `03_consistencia_y_concurrencia.md` §6.5/§8: los fallos de compensación deben quedar registrados y generar información observable (registros **y métricas**).
- `04_integracion_y_mensajeria.md` §5.4: detección y reparación de discrepancias entre Redis y PostgreSQL; el mecanismo concreto de reconciliación debe definirse explícitamente.
- `02_restricciones_arquitectonicas.md`: no introducir mecanismos de consistencia nuevos sin evaluar impacto y obtener autorización.

## 2. Problema

La auditoría de T-004/T-005 identificó cuatro gaps entre lo requerido por el contexto y el estado del código. Ninguno está implementado y todos fueron explícitamente dejados fuera del alcance de T-004 (ver `docs_ia/tareas/T-004_*` y `docs_ia/reportes/T-004_resolucion.md`):

1. **Idempotencia de reservas ausente.** No existe lectura de la cabecera `Idempotency-Key`, no hay consulta por `idempotency_key`, no existe la columna en PostgreSQL, y el caso de uso no evita reservas duplicadas ante reintentos.
2. **Contrato HTTP, rutas y DTO no alineados.** Las rutas actuales son `/reservations` y `/events/{eventId}/availability` (sin prefijo `/api/v1/`); faltan los endpoints `GET /api/v1/reservations/user/{userId}` y `DELETE /api/v1/reservations/{reservationId}`; no hay DTO (se exponen modelos de dominio directamente); la respuesta carece de `userId` y `createdAt`.
3. **Observabilidad incompleta de fallos de compensación.** `CreateReservationService.compensate()` solo escribe log estructurado (slf4j); no emite métrica Micrometer ni deja registro durable del incidente de compensación.
4. **No hay reconciliación Redis ↔ PostgreSQL.** No existe trabajo programado que compare contadores de Redis con reservas de PostgreSQL; la inconsistencia residual (si la compensación falla) solo se detecta manualmente.

Estos gaps requieren una decisión arquitectónica porque tocan el contrato público, el esquema de persistencia, nuevas responsabilidades de aplicación (reconciliación programada) y métricas, y porque `02_restricciones_arquitectonicas.md` exige autorización para mecanismos de consistencia nuevos.

---

## 3. Requisitos y documentos autoritativos

| Requisito | Fuente |
|---|---|
| Cabecera obligatoria `Idempotency-Key: <UUID>` en `POST /reservations` | `04_diseno_api_rest.md`, `03_concurrencia_bloqueos.md` §3, `05_observabilidad_y_mantenibilidad.md` |
| Retornar respuesta almacenada si `idempotency_key` ya existe (sin `DECRBY` ni nueva reserva) | `03_concurrencia_bloqueos.md` §3, `02_componentes_y_responsabilidades.md` §4.1 |
| Columna `idempotency_key VARCHAR(255) UNIQUE, INDEX` y `created_at` en `reservations` | `02_modelo_datos_persistencia.md` §3 |
| Rutas con prefijo `/api/v1/`: `GET /api/v1/availability/{eventId}`, `POST /api/v1/reservations`, `GET /api/v1/reservations/user/{userId}`, `DELETE /api/v1/reservations/{reservationId}` | `04_diseno_api_rest.md` §1 |
| DTO de respuesta con `reservationId, userId, eventId, sectionId, quantity, status, createdAt` | `04_diseno_api_rest.md` §2 |
| Fallos de compensación registrados y con información observable (registros y métricas) | `02_disponibilidad_y_resiliencia.md` §5, `03_consistencia_y_concurrencia.md` §6.5/§8 |
| Detección de discrepancias Redis ↔ PostgreSQL; mecanismo de reconciliación definido explícitamente | `04_integracion_y_mensajeria.md` §5.4, `05_observabilidad_y_mantenibilidad.md` §8.2 |
| RB-11 (no sobreventa): cualquier fallo deriva en venta perdida, nunca en sobreventa | `contexto_negocio.md` §12, `01_vista_general.md` |

---

## 4. Estado actual verificado (inspección de código, sin modificaciones)

### 4.1 Idempotencia
- `ReservationController.createReservation(@RequestBody Reservation reservation)` en ruta `/reservations`: **no lee** la cabecera `Idempotency-Key`.
- `CreateReservationUseCase.createReservation(Reservation reservation)`: **sin** parámetro de clave de idempotencia.
- `CreateReservationService.createReservation`: **no consulta** ninguna clave de idempotencia antes ni después de `tryReserve`.
- `Reservation` (dominio): campos `id, eventId, locality, quantity, status`. **Carece** de `idempotencyKey`, `userId`, `createdAt`.
- `ReservationPersistenceAdapter.save`: `INSERT INTO reservations (event_id, locality, quantity, status)`. **No persiste** clave de idempotencia.
- `ReservationRepository` (puerto): solo `save(Mono<Reservation>)`. **Sin** método de consulta por `idempotency_key`.
- Flyway `V1__create_reservations.sql`: columnas `id, event_id, locality, quantity, status`. **Faltan** `idempotency_key` y `created_at` (contradice el modelo del contexto).
- Búsqueda en `src/`: cero ocurrencias de `idempotency`, `Idempotency-Key` o `idempotency_key` en código fuente.

### 4.2 Contrato HTTP, rutas y DTO
- `AvailabilityController`: `@GetMapping("/events/{eventId}/availability")` (context exige `/api/v1/availability/{eventId}`). Retorna `Availability` (dominio) directamente.
- `ReservationController`: `@PostMapping("/reservations")` (context exige `/api/v1/reservations`). Retorna `Reservation` (dominio) directamente.
- **Faltan** endpoints `GET /api/v1/reservations/user/{userId}` y `DELETE /api/v1/reservations/{reservationId}`.
- No existen clases DTO; los modelos de dominio se exponen como contrato HTTP.
- La respuesta no incluye `userId` ni `createdAt`.

### 4.3 Observabilidad de fallos de compensación
- `CreateReservationService.compensate(...)`: `log.error(...)` al iniciar, `log.info(...)` al éxito, `log.error(...)` al fallo, y `onErrorResume(compensationFailure -> Mono.empty())` para tragar el error del `release`.
- **No emite métrica Micrometer** (ni `Counter` ni otra) para fallos de compensación.
- **No persiste** incidente durable (solo log estructurado), tal como establece DA-001 versión aprobada.
- `pom.xml` ya incluye `spring-boot-starter-actuator` (Micrometer disponible).

### 4.4 Reconciliación Redis ↔ PostgreSQL
- Búsqueda de `@Scheduled` y `reconcili`: **cero resultados** en código fuente.
- No existe clase de trabajo programado, ni comparación de contadores Redis vs reservas PostgreSQL.
- DA-001 y el reporte de T-004 dejan la reconciliación periódica como trabajo futuro.

### 4.5 Qué ya está implementado (no es gap, se preserva)
- Flujo saga corta `tryReserve → save → publish` con compensación `release` (DA-001, implementado y testeado en `CreateReservationServiceCompensationIT`).
- Scripts Lua atómicos en `RedisAvailabilityAdapter` (try/reserve/release con tope de capacidad).
- Reintento nativo de Reactor en `getAvailability` (DA-002).
- Flyway `V1`, `FlywayConfig`, Testcontainers (DA-004, DA-005).
- Eureka, puerto 8082, Actuator/health/metrics.

---

## 5. Gaps confirmados

| ID | Gap | Evidencia verificada | Impacto si no se resuelve |
|---|---|---|---|
| G-01 | Idempotencia de reservas no implementada | Cero lectura de `Idempotency-Key`; sin columna `idempotency_key`; sin consulta en caso de uso | Reservas duplicadas ante reintentos del cliente; violación de CU-06 y RB-11 implícita |
| G-02 | Rutas, contrato y DTO no alineados al contexto | Rutas `/reservations` / `/events/{eventId}/availability`; sin `/api/v1/`; sin DTO; endpoints faltantes | Clientes/gateway no pueden usar el contrato documentado; respuesta sin `userId`/`createdAt` |
| G-03 | Fallos de compensación sin métrica observable | Solo `log.error`; sin `MeterRegistry.Counter`; sin registro durable | No se pueden detectar ni alertar fallos de compensación; incumple criterio de aceptación de resiliencia |
| G-04 | Sin reconciliación Redis ↔ PostgreSQL | Sin `@Scheduled`; sin comparación de estados | Inconsistencia residual (ej. compensación fallida) permanece no detectada hasta intervención manual |

---

## 6. Alternativas consideradas y justificación

### G-01 — Idempotencia

**A. Clave de idempotencia almacenada en PostgreSQL (`reservations.idempotency_key`) + consulta previa al `DECRBY`** (recomendada)
- Antes de `tryReserve`, el caso de uso consulta `findByIdempotencyKey(key)`. Si existe, retorna la reserva almacenada sin tocar Redis. Si no, ejecuta `tryReserve → save` con la clave.
- Restricción `UNIQUE` en la columna como red de seguridad contra condiciones de carrera (dos requests concurrentes con misma clave: la segunda falla en el `INSERT` por duplicidad y se trata como acierto idempotente devolviendo la existente).
- Ventaja: durable, alinea con el modelo de datos del contexto, no añade dependencias.
- Desventaja: requiere migración Flyway `V2` (cambio de esquema aprobado en DA-004, se extiende).

**B. Clave de idempotencia en Redis (registro temporal con TTL)**
- Ventaja: rápido, sin cambios de esquema.
- Desventaja: volatilidad (reinicio de Redis pierde claves → duplicados posibles), menos duradero que PostgreSQL; el contexto prefiere la tabla `reservations`.

**C. Sin idempotencia (status quo)**
- Rechazada: incumple `03_concurrencia_bloqueos.md` §3 y `04_diseno_api_rest.md`.

### G-02 — Contrato HTTP, rutas y DTO

**A. Alinear rutas al contexto (`/api/v1/...`), crear DTOs y agregar endpoints faltantes** (recomendada)
- Rutas: `POST /api/v1/reservations`, `GET /api/v1/availability/{eventId}`, `GET /api/v1/reservations/user/{userId}`, `DELETE /api/v1/reservations/{reservationId}`.
- DTOs: `ReservationRequest`, `ReservationResponse`, `AvailabilityResponse`; mapper dominio ↔ DTO en el adaptador de entrada.
- Cabecera `Idempotency-Key` vía `@RequestHeader` en el controlador.
- Ventaja: contrato público conforme al contexto; separación dominio/DTO respeta restricciones de código.
- Desventaja: cambio de rutas puede afectar al gateway y clientes existentes; requiere coordinación (ver riesgos).

**B. Mantener rutas actuales y agregar solo los endpoints faltantes**
- Rechazada: deja el contrato inconsistente con el contexto (`04_diseno_api_rest.md`).

**C. Mantener rutas actuales como alias además de las nuevas**
- Opción de transición; se deja como decisión pendiente (ver §10).

### G-03 — Observabilidad de fallos de compensación

**A. Métrica Micrometer `Counter` para fallos de compensación + log estructurado mantenido** (recomendada)
- Inyectar `MeterRegistry`/`Counter` en `CreateReservationService`; incrementar cuando `release` falle.
- Aprovecha `spring-boot-starter-actuator` ya presente (sin nuevas dependencias).
- Ventaja: observable vía `/actuator/metrics`; bajo costo; no altera el flujo (el error original sigue propagándose).

**B. Registro durable de incidentes en tabla propia (tabla `reservation_incidents`)**
- Ventaja: trazabilidad operativa completa.
- Desventaja: nueva entidad, nuevo repositorio, nueva migración; mayor esfuerzo para el prototipo académico; DA-001 aprobada deja esto como trabajo futuro y autoriza solo log estructurado por ahora.
- Se propone posponer a una decisión posterior (ver §10).

**C. Solo log (status quo)**
- Rechazada: no cumple "registros y métricas" de `02_disponibilidad_y_resiliencia.md` §5.

### G-04 — Reconciliación Redis ↔ PostgreSQL

**A. Trabajo programado (`@Scheduled`, fixed-delay) de detección y log de discrepancias, sin autocorrección automática** (recomendada)
- Lee claves `availability:*` de Redis (SCAN/`keys`), lee reservas activas de PostgreSQL por `(event_id, section_id)`, compara `capacidad - sum(quantity)` vs valor Redis; registra y emite métrica `inventory.reconciliation.discrepancies.total` cuando difieren.
- Sin autocorrección automática (evita correcciones erróneas que pudieran causar sobreventa o stock fantasma).
- Ventaja: detección automática, bajo riesgo, reutiliza adaptadores existentes, sin nuevas dependencias (`spring-boot-starter` ya soporta `@Scheduled` con `@EnableScheduling`).
- Desventaja: solo detecta, no repara; la frecuencia define el tiempo máximo de detección.

**B. Reconciliación con autocorrección automática (ajuste de Redis a PostgreSQL)**
- Rechazada para esta fase: corregir Redis automáticamente puede enmascarar errores y, si tiene un bug, provocar sobreventa o stock fantasma; requiere una decisión aparte con mayor análisis de riesgo.

**C. Sin reconciliación (status quo)**
- Rechazada: G-04 permanece; DA-001 la deja pendiente.

---

## 7. Solución arquitectónica recomendada

Adoptar las alternativas A para G-01, G-02 y G-03, y la alternativa A (detección sin autocorrección) para G-04. Resumen de la solución:

1. **Esquema (Flyway `V2`)**: agregar columnas `idempotency_key VARCHAR(255) UNIQUE`, `created_at TIMESTAMP WITH TIME ZONE` y `user_id VARCHAR(100)` a `reservations`. (Extiende DA-004 sin modificar `V1`.)
2. **Dominio**: `Reservation` incorpora `idempotencyKey`, `userId`, `createdAt`.
3. **Puerto de persistencia**: agregar `Mono<Optional<Reservation>> findByIdempotencyKey(String key)` (o equivalente) a `ReservationRepository` e implementarlo en `ReservationPersistenceAdapter`.
4. **Caso de uso**: `CreateReservationService.createReservation` recibe la clave de idempotencia; antes de `tryReserve`, consulta `findByIdempotencyKey`; si existe, retorna la reserva almacenada (sin `DECRBY` ni `save`); si no, ejecuta el flujo saga corto con la clave.
5. **Controlador/DTO**: rutas con prefijo `/api/v1/`; `ReservationRequest`/`ReservationResponse`/`AvailabilityResponse`; `@RequestHeader("Idempotency-Key")` en `POST`; agregar endpoints `user/{userId}` y `DELETE /{reservationId}`.
6. **Observabilidad**: `Counter` de Micrometer `inventory.compensation.failures.total` (etiquetado por resultado) incrementado en `compensate` cuando el `release` falla; log estructurado mantenido.
7. **Reconciliación**: componente `@Scheduled` (con `@EnableScheduling`) que compara Redis vs PostgreSQL y registra/emite métrica `inventory.reconciliation.discrepancies.total` sin autocorrección.

No se requieren nuevas dependencias de Maven (todas las tecnologías ya están en `pom.xml`: WebFlux, R2DBC, Redis reactivo, Actuator/Micrometer, Flyway, Testcontainers). El esfuerzo se concentra en código, migración SQL y pruebas.

---

## 8. Impacto sobre los componentes existentes

| Componente | Cambio |
|---|---|
| `FlywayConfig` / `V1__create_reservations.sql` | Sin cambios a `V1`; nueva migración `V2__add_idempotency_columns.sql` |
| `Reservation` (dominio) | Nuevos campos `idempotencyKey`, `userId`, `createdAt`; constructores/getters |
| `ReservationRepository` (puerto) | Nuevo método de consulta por `idempotency_key` |
| `ReservationPersistenceAdapter` | `INSERT` incluye `idempotency_key`, `user_id`, `created_at`; implementa consulta |
| `CreateReservationService` | Parámetro de idempotency key; consulta previa; retorno de respuesta almacenada; métrica de fallo de compensación |
| `ReservationController` | Nuevas rutas `/api/v1/...`; `@RequestHeader Idempotency-Key`; DTOs; endpoints `user/{userId}` y `DELETE` |
| `AvailabilityController` | Ruta `/api/v1/availability/{eventId}`; DTO `AvailabilityResponse` |
| Nuevo: DTOs + mapper | `ReservationRequest`, `ReservationResponse`, `AvailabilityResponse`, mapper |
| Nuevo: reconciliación | Componente `@Scheduled` + `@EnableScheduling` en una clase de config |
| `pom.xml` | Sin cambios (todas las dependencias ya están) |
| `application.properties` | Posible propiedad de frecuencia de reconciliación (por definir) |
| Decisiones previas (DA-001 a DA-005) | No se modifican; se complementan |
| API Gateway (DA-003) | Las rutas de Inventory cambian de prefijo; el gateway debe revisarse para apuntar a `/api/v1/...` (dependencia externa, fuera de este servicio) |

Documentos ya aprobados que esta decisión complementa (no reemplaza): DA-001, DA-002, DA-003, DA-004, DA-005.

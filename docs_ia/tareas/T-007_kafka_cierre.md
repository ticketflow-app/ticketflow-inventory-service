# Fase 5 — Cierre de Implementación: Apache Kafka Reactivo y Publicación de Eventos

**Fecha:** 25/10/2026  
**Servicio:** ticketflow-inventory-service  
**Estado:** COMPLETADO  

---

## 1. Auditoría

### Contexto de partida
- Fases 1-4 completadas: Dominio, Redis Lua, PostgreSQL R2DBC + Flyway + Compensación, API REST Reactiva + JWT + Validación.
- Existía `ReservationEventPublisher` usando `KafkaTemplate` bloqueante (`Mono.fromFuture`) en `KafkaReservationEventPublisher`.
- No existía contrato de evento tipado (`ReservationConfirmedEvent`).
- No existía puerto de salida reactivo para eventos (`EventPublisherPort`).
- No había dependencia `reactor-kafka` en `pom.xml`.
- `CreateReservationService` publicaba vía `ReservationEventPublisher` después de persistir, sin manejo específico de fallos de Kafka.

### Documentos de contexto consultados
- `docs_ia/contexto/diseño/04_integracion_y_mensajeria.md`
- `docs_ia/contexto/diseño/03_flujos_de_negocio.md` (sección 8: Integración con Kafka)
- `docs_ia/contexto/restricciones del proyecto/03_restricciones_de_codigo.md` (sin I/O bloqueante)
- `docs_ia/decisiones/DA-001-...` (orquestación de reserva y compensación)
- `docs_ia/fase4_webflux_jwt_cierre.md` (estado de la Fase 4)

---

## 2. Decisiones

### Contrato del evento
- `ReservationConfirmedEvent` (Java 21 record) en capa `application.domain.event`.
- Campos: `reservationId` (String), `eventId` (Long), `sectionId` (String), `quantity` (Integer), `timestamp` (Instant), `correlationId` (String).
- Tema Kafka: `inventory.reservations.confirmed` (configurable vía `kafka.topic.reservation-confirmed`).

### Puerto de salida
- `EventPublisherPort` en `application.port.out` con método `Mono<Void> publishReservationConfirmed(ReservationConfirmedEvent event)`.
- Implementación: `KafkaEventPublisherAdapter` usando `ReactiveKafkaProducerTemplate` (Reactor Kafka 1.4.0), totalmente no bloqueante.

### Dependencia
- `io.projectreactor.kafka:reactor-kafka:1.4.0` añadida a `pom.xml`.
- `spring-kafka` ya existía (para `KafkaTemplate` legado).

### Propiedades Kafka
- `acks=all`, `retries=3`, `retry.backoff.ms=1000`, `delivery.timeout.ms=30000`, `request.timeout.ms=10000`.
- Serializadores: `StringSerializer` (clave), `JsonSerializer` (valor).

### Resiliencia de publicación (Estrategia ante fallos de Kafka)
- Si la publicación falla o hace timeout, el error es capturado con `onErrorResume` y se devuelve `Mono.empty()`.
- La reserva en PostgreSQL/Redis NO se revierte — el cliente recibe respuesta exitosa (HTTP 201).
- El error se registra con log estructurado (reservationId, eventId, sectionId, quantity, correlationId, causa).
- El incidente queda disponible para reconciliación futura (trabajo fuera de alcance de esta fase).

### Integración en CreateReservationService
- `CreateReservationService` recibe `EventPublisherPort` como nueva dependencia.
- Tras `reservationRepository.save()` exitoso, se construye `ReservationConfirmedEvent` y se publica vía `eventPublisherPort.publishReservationConfirmed(event)`.
- `onErrorResume` traga el error de Kafka y `.thenReturn(savedReservation)` garantiza que el flujo continúe.

---

## 3. Acción

### Archivos creados
| Ruta | Descripción |
|------|-------------|
| `src/main/java/com/inventory/service/application/domain/event/ReservationConfirmedEvent.java` | Record Java 21 del evento de reserva confirmada. |
| `src/main/java/com/inventory/service/application/port/out/EventPublisherPort.java` | Puerto de salida reactivo para publicación de eventos. |
| `src/main/java/com/inventory/service/infrastructure/config/ReactiveKafkaConfig.java` | Configuración de `ReactiveKafkaProducerTemplate` con `SenderOptions`. |
| `src/main/java/com/inventory/service/infrastructure/adapter/out/kafka/KafkaEventPublisherAdapter.java` | Adaptador Kafka reactivo con manejo graceful de fallos del broker. |
| `src/test/java/com/inventory/service/infrastructure/adapter/out/kafka/KafkaEventPublisherAdapterTest.java` | Pruebas reactivas: publicación exitosa, fallo del broker, timeout. |
| `docs_ia/fase5_kafka_cierre.md` | Este documento de cierre. |

### Archivos modificados
| Ruta | Cambio |
|------|--------|
| `pom.xml` | Añadida `io.projectreactor.kafka:reactor-kafka:1.4.0`. |
| `src/main/resources/application.properties` | Añadidas propiedades de productor Kafka (`acks`, `retries`, timeouts) y `kafka.topic.reservation-confirmed`. |
| `src/main/java/com/inventory/service/application/usecase/CreateReservationService.java` | Añadida dependencia `EventPublisherPort`; publicación de `ReservationConfirmedEvent` tras persistencia con manejo graceful de fallos de Kafka. |
| `src/test/java/com/inventory/service/application/usecase/CreateReservationServiceTest.java` | Actualizado para el nuevo constructor con `EventPublisherPort`; agregados tests de publicación de evento confirmado y de resiliencia ante fallo de Kafka. |

---

## 4. Pruebas

### Pruebas del adaptador Kafka (`KafkaEventPublisherAdapterTest`)
- **Publicación exitosa:** `producerTemplate.send()` devuelve `Mono.empty()` → `StepVerifier` espera `expectComplete()`. Se verifica que `producerTemplate.send()` fue invocado.
- **Fallo del broker:** `producerTemplate.send()` devuelve `Mono.error(RuntimeException)` → `StepVerifier` espera `expectComplete()` (error tragado, no propaga).
- **Timeout:** `producerTemplate.send()` devuelve `Mono.error(TimeoutException)` → `StepVerifier` espera `expectComplete()` (error tragado, no propaga).

### Pruebas del caso de uso (`CreateReservationServiceTest`)
- **Publicación de evento confirmado:** tras save exitoso, verifica que `FakeEventPublisherPort.publishedEvent` contenga los datos correctos (reservationId, eventId, sectionId, quantity).
- **Fallo de Kafka no rompe el flujo:** `FakeEventPublisherPort.errorToReturn` establecido → `service.createReservation()` retorna la reserva con éxito y `publishFailed = true`.
- **Fallo de persistencia:** se propaga el error, no se publica evento, se ejecuta compensación de Redis.
- **Rechazo por stock insuficiente:** no se publica evento, no se persiste.
- **Rechazo por no inicializado:** no se publica evento, no se persiste.

### Pruebas existentes (sin cambios, verificadas)
- `CreateReservationServiceCompensationIT` (Redis real, compensación DA-001).
- `CreateReservationServicePostgresIT` (PostgreSQL real + Redis real, R2DBC).
- `RedisAvailabilityAdapterIT` (scripts Lua atómicos).
- `ReservationControllerWebFluxTest`, `SecurityWebFluxTest` (Fase 4).
- `ServiceApplicationTests`, `GetAvailabilityServiceTest`, `ReservationTest`, `LocalityAvailabilityTest`.

---

## 5. Integración

### Arquitectura hexagonal respetada
- **Domain:** `ReservationConfirmedEvent` (Java 21 record, Java puro, sin dependencias de frameworks).
- **Application:** `EventPublisherPort` (interfaz de salida, sin dependencias de Kafka).
- **Infrastructure (out):** `KafkaEventPublisherAdapter` (Reactor Kafka, `ReactiveKafkaProducerTemplate`), `ReactiveKafkaConfig` (configuración de sender).
- **Regla de dependencia:** `infrastructure → application → domain`.

### Flujo de publicación de evento
```
tryReserve (Redis, atómico Lua)
  → RESERVED?
    → NO: rechazar (InsufficientAvailabilityException / NotInitializedException)
    → YES: save (PostgreSQL, R2DBC DatabaseClient)
      → ERROR: compensar Redis (release), propagar error
      → OK: publicar ReservationConfirmedEvent (Kafka, ReactiveKafkaProducerTemplate)
        → ERROR: log estructurado + onErrorResume → Mono.empty() (no romper flujo)
        → OK: log de confirmación
      → thenReturn(savedReservation) al cliente
```

### Sin operaciones bloqueantes
- `ReactiveKafkaProducerTemplate.send()` devuelve `Flux<SenderResult>` (reactivo, no bloqueante).
- `KafkaTemplate` legacy (`Mono.fromFuture`) mantenido solo para `ReservationEventPublisher` existente, no usado en el nuevo flujo de `ReservationConfirmedEvent`.
- Cero uso de `block()` en rutas de producción.

### Tecnologías verificadas
- Java 21 Records.
- Reactor Kafka 1.4.0 (`ReactiveKafkaProducerTemplate`, `SenderRecord`, `SenderOptions`, `KafkaSender`).
- Spring Boot 4.1.1, Spring Cloud 2025.1.3.
- Reactor Core (`Mono`, `flatMap`, `onErrorResume`, `thenReturn`, `StepVerifier`).

---

## 6. Cierre

### Criterios de aceptación cumplidos
| Criterio | Estado |
|----------|--------|
| `ReservationConfirmedEvent` record con todos los campos | ✅ |
| `EventPublisherPort` con `Mono<Void> publishReservationConfirmed(...)` | ✅ |
| `reactor-kafka:1.4.0` en `pom.xml` | ✅ |
| Propiedades Kafka (`acks=all`, `retries=3`, timeouts) en application.properties | ✅ |
| `KafkaEventPublisherAdapter` con `ReactiveKafkaProducerTemplate` | ✅ |
| Publicación tras persistencia en PostgreSQL | ✅ |
| Fallo de Kafka no rompe el flujo del cliente (onErrorResume + Mono.empty) | ✅ |
| Log estructurado de errores de publicación | ✅ |
| Pruebas reactivas (publicación exitosa, fallo broker, timeout) | ✅ |
| Pruebas de caso de uso (evento publicado, resiliencia Kafka) | ✅ |
| `mvnw compile` sin errores | ✅ |

### Próximos pasos (fuera del alcance de esta fase)
- Outbox pattern para garantía de entrega atómicamente con la transacción de PostgreSQL.
- Idempotencia de consumidores Kafka (deduplicación de `ReservationConfirmedEvent`).
- Retry con backoff exponencial en el adaptador para fallos transitorios del broker.
- Métricas Micrometer para eventos publicados / fallidos.
- Consumidor Kafka en otros servicios (notificación, billing, etc.).

**Fase 5 completada y verificada.**  
**Autor:** Cline (Agente @coder + @architect)  
**Fecha:** 25/10/2026
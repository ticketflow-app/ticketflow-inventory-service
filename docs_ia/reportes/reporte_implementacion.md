# Registro General de Cambios e Implementación — ticketflow-inventory-service

## 1. Infraestructura y Stack Tecnológico
* **Java 25:** Proyecto actualizado a JDK 25 (`pom.xml` con `<java.version>25</java.version>`, `<maven.compiler.source>25</maven.compiler.source>`, `<maven.compiler.target>25</maven.compiler.target>` y `maven-compiler-plugin` en `<release>25</release>`).
* **Redis 8:** Actualizado `docker-compose.yml` para usar la imagen `redis:8.0-alpine`. Se configuró el cliente Lettuce con soporte para el protocolo RESP3 y pool de conexiones en `application.properties`.
* **Contenedores Docker:** `Dockerfile` actualizado a imágenes base `eclipse-temurin:25-jdk-alpine` (build stage) y `eclipse-temurin:25-jre-alpine` (runtime stage), empleando el wrapper `./mvnw clean package -DskipTests`.
* **Librería de Códigos QR:** Integradas las dependencias ZXing (`com.google.zxing:core:3.5.3` y `com.google.zxing:javase:3.5.3`).

---

## 2. API REST y Cobertura Completa de Endpoints

Se completó la implementación de los 10 endpoints requeridos bajo sus rutas oficiales:

### Módulo de Inventario (`/api/inventario`)
1. `GET /api/inventario/eventos/{eventoId}/disponibilidad` (Público)
   * **Implementación:** `InventoryController.getDisponibilidadEvento`.
   * **Descripción:** Lee los contadores atómicos en Redis de todas las localidades asociadas al evento.
   * **Respuestas:** `200 OK` con `EventAvailabilityResponse` o `404 Not Found`.

2. `POST /api/inventario/localidades/{id}/ajuste` (ADMIN)
   * **Implementación:** `InventoryController.ajustarStock`.
   * **Descripción:** Permite la reconciliación manual del stock en Redis mediante `StockAdjustmentRequest`.
   * **Respuestas:** `200 OK`. Requiere autorización de rol `ADMIN`.

### Módulo de Reservas (`/api/reservas`)
3. `POST /api/reservas` (COMPRADOR)
   * **Implementación:** `ReservationsController.crearReserva`.
   * **Descripción:** Procesa la reserva validando máximo 6 boletos (`@Min(1)` y `@Max(6)`), retiene el stock en Redis con un TTL de 10 minutos y exige la cabecera `Idempotency-Key`.
   * **Respuestas:** `201 Created`, `400 Bad Request`, `409 STOCK_INSUFICIENTE`, `409 EVENTO_NO_VENDIBLE`, `429 Too Many Requests`.

4. `GET /api/reservas/{id}` (COMPRADOR - Dueño)
   * **Implementación:** `ReservationsController.getReservaPorId`.
   * **Descripción:** Consulta el estado actual de la reserva y calcula los segundos restantes antes de su expiración.
   * **Respuestas:** `200 OK`, `403 Forbidden` (si no es el propietario), `404 Not Found`.

5. `GET /api/reservas/mias` (COMPRADOR)
   * **Implementación:** `ReservationsController.getMisReservas`.
   * **Descripción:** Retorna el historial paginado (`Pageable`) de reservas vinculadas al `userId` del JWT.
   * **Respuestas:** `200 OK`.
6. `POST /api/reservas/{id}/confirmar` (COMPRADOR - Dueño)
   * **Implementación:** `ReservationsController.confirmarReserva`.
   * **Descripción:** Simula la pasarela de pago, cambia el estado de la reserva, exige `Idempotency-Key` y emite/persiste los boletos (`Ticket`).
   * **Respuestas:** `200 OK`, `404 Not Found`, `409 Conflict`, `410 RESERVA_EXPIRADA` (si la retención de 10 min venció).

7. `DELETE /api/reservas/{id}` (COMPRADOR - Dueño)
   * **Implementación:** `ReservationsController.cancelarReserva`.
   * **Descripción:** Cancela una reserva pendiente y libera/devuelve las unidades al stock en Redis.
   * **Respuestas:** `204 No Content`, `409 Conflict` (si ya está en un estado no cancelable).

### Módulo de Boletos (`/api/boletos`)
8. `GET /api/boletos/mios` (COMPRADOR)
   * **Implementación:** `TicketController.getMisBoletos`.
   * **Descripción:** Consulta paginada (`Pageable`) de boletos emitidos para el usuario autenticado.
   * **Respuestas:** `200 OK`.

9. `GET /api/boletos/{id}` (COMPRADOR - Dueño)
   * **Implementación:** `TicketController.getBoletoDetalle`.
   * **Descripción:** Obtiene los detalles de un boleto tras verificar que pertenezca al usuario solicitante.
   * **Respuestas:** `200 OK`, `403 Forbidden`, `404 Not Found`.

10. `GET /api/boletos/{id}/qr` (COMPRADOR - Dueño)
    * **Implementación:** `TicketController.getBoletoQr`.
    * **Descripción:** Genera de forma dinámica la imagen PNG del código QR del boleto usando `QrGeneratorService`.
    * **Respuestas:** `200 OK` (`image/png`), `403 Forbidden`, `404 Not Found`.

---

## 3. Modelo de Datos, Servicios y Componentes de Dominio

* **Entidad `Ticket`:** Creado el modelo con los campos `id`, `reservationId`, `userId`, `eventId`, `sectionId`, `seatNumber`, `issuedAt`.
* **Capa de Persistencia para Boletos:** Interfaz `TicketRepository` e implementación reactiva `InMemoryTicketRepository` con métodos `findByUserId` y `findByIdAndUserId`.
* **DTOs y DTO Records:**
  * `CreateReservationRequest` (validaciones `@NotNull`, `@NotBlank`, `@Min(1)`, `@Max(6)`).
  * `ReservationResponse` (incluye `remainingSeconds` y `expiresAt`).
  * `StockAdjustmentRequest` (validación `@Min(0)`).
  * `EventAvailabilityResponse`, `SectionAvailability`, `TicketResponse` y `ErrorResponse` (`code`, `message`).
* **Servicio de Códigos QR (`QrGeneratorService`):** Componente en la capa de infraestructura que renderiza un `BitMatrix` de ZXing a `byte[]` PNG mediante `MatrixToImageWriter`.
* **Servicios de Aplicación:** `GetEventAvailabilityService`, `AdjustStockService`, `ReservationService` y `TicketService`.

---

## 4. Aspectos Transversales y Seguridad

* **Manejo Global de Excepciones (`GlobalExceptionHandler`):**
  * `ResourceNotFoundException` -> `HTTP 404 NOT_FOUND`
  * `AccessDeniedException` -> `HTTP 403 FORBIDDEN`
  * `InsufficientStockException` -> `HTTP 409 STOCK_INSUFFICIENT`
  * `EventNotSellableException` -> `HTTP 409 EVENTO_NO_VENDIBLE`
  * `ReservationExpiredException` -> `HTTP 410 RESERVA_EXPIRADA`
  * `TooManyRequestsException` -> `HTTP 429 TOO_MANY_REQUESTS`
  * `InvalidReservationStateTransitionException` -> `HTTP 409 CONFLICT`
* **Seguridad y Control de Acceso (RBAC):** Activado `@EnableReactiveMethodSecurity` en `SecurityConfig`. Se aplican reglas `@PreAuthorize` para roles `COMPRADOR` y `ADMIN`, extrayendo la identidad (`userId`) directamente del contexto de autenticación JWT.
* **Garantía de Idempotencia y Expiración:** Validación del header `Idempotency-Key` en operaciones de creación y confirmación de reservas, junto a la retención con TTL de 10 minutos sobre los registros de Redis.

---

## 5. Limpieza de Código Legacy

* Se desactivaron las anotaciones `@RestController` en los controladores antiguos (`AvailabilityController` y `ReservationController` bajo `/api/v1/...`) para evitar colisiones de rutas y asegurar que únicamente los nuevos controladores atiendan las solicitudes.


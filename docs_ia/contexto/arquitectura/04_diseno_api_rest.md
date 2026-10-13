# Diseño de API REST y Mensajería - Microservicio Inventory

## 1. Endpoints de la API REST

Todos los endpoints utilizan respuestas reactivas (`Mono<T>` / `Flux<T>`) y nomenclatura estricta en inglés.

| Método | Ruta | Rol mínimo | Descripción | Código exitoso |
|---|---|---|---|---|
| `GET` | `/api/v1/availability/{eventId}` | Público / `BUYER` | Consulta la disponibilidad actual por sección. | `200 OK` |
| `POST` | `/api/v1/reservations` | `BUYER` | Solicita la reserva atómica de entradas. | `201 Created` |
| `GET` | `/api/v1/reservations/user/{userId}` | `BUYER` / `ADMIN` | Consulta el historial de reservas de un usuario. | `200 OK` |
| `DELETE` | `/api/v1/reservations/{reservationId}` | `BUYER` | Cancela una reserva activa y libera el stock. | `200 OK` / `204 No Content` |

## 2. Contratos DTO (JSON)

### Request: `POST /api/v1/reservations`

**Headers:**

```http
Authorization: Bearer <JWT>
Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d
Content-Type: application/json
```

**Body:**

```json
{
  "eventId": "EVT-2026-001",
  "sectionId": "VIP-NORTH",
  "quantity": 2
}
```

### Response: `201 Created`

```json
{
  "reservationId": "RES-88392019",
  "userId": "USR-102938",
  "eventId": "EVT-2026-001",
  "sectionId": "VIP-NORTH",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-10-13T01:06:00Z"
}
```

## 3. Mensajería Asíncrona con Apache Kafka

Cuando una reserva es confirmada, el adaptador de mensajería publica un evento asíncrono hacia Kafka para el consumo de otros servicios futuros.

- **Topic:** `reservas.confirmadas` (o `inventory.reservations.confirmed`).
- **Productor:** Microservicio `inventory`.

### Estructura del Evento

```json
{
  "eventType": "RESERVATION_CONFIRMED",
  "reservationId": "RES-88392019",
  "userId": "USR-102938",
  "eventId": "EVT-2026-001",
  "sectionId": "VIP-NORTH",
  "quantity": 2,
  "timestamp": "2026-10-13T01:06:00Z"
}
```

## 4. Observabilidad y Métricas

Mediante Spring Boot Actuator y Micrometer, el microservicio expone los siguientes endpoints:

- **`/actuator/health`**: Estado de salud de las conexiones a Redis, PostgreSQL (R2DBC) y Kafka.
- **`/actuator/metrics`**: Métricas de latencia en peticiones HTTP, tasa de reservas exitosas/fallidas y consumo de hilos reactivos Netty.
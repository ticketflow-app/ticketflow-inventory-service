# Reglas de Negocio - Microservicio de Inventario (Inventory)

## 1. Nomenclatura y Estructura

Las Reglas de Negocio siguen la convención `RN-XXX` y son de carácter obligatorio para toda implementación en la lógica de dominio.

---

## 2. Reglas de Capacidad e Inventario

- **RN-01 - Control de Límite Mínimo de Stock (No Sobreventa):**
  - El stock disponible de una localidad nunca puede ser menor que cero ($Stock \ge 0$).
  - Cualquier intento de reserva que resulte en un valor de stock negativo debe ser rechazado inmediatamente por el dominio lanzando una excepción de negocio.

- **RN-02 - Control de Capacidad Máxima:**
  - La suma del stock disponible más las reservas activas de una localidad nunca puede superar la capacidad máxima configurada originalmente para esa localidad ($StockDisponible + StockReservado \le CapacidadMaxima$).

- **RN-03 - Límite de Entradas por Transacción:**
  - Un comprador puede reservar un máximo de **10 entradas** por transacción/solicitud para evitar el acaparamiento por scripts o revendedores.
  - La cantidad mínima a reservar en una solicitud es de **1 entrada**.

---

## 3. Reglas de Gestión de Reservas

- **RN-04 - Expiración Temporal de Reservas (Tiempo de Vida / TTL):**
  - Toda reserva creada pasa a estado `PENDING_PAYMENT` y tendrá un tiempo límite de validez de **15 minutos**.
  - Si el pago no es confirmado dentro de ese periodo, la reserva pasa automáticamente a estado `EXPIRED` y las entradas deben ser reintegradas inmediatamente al stock disponible.

- **RN-05 - Inmutabilidad de Reservas Finalizadas:**
  - Una reserva en estado `CONFIRMED` (pagada) o `CANCELLED` no puede volver a pasar a estado `PENDING_PAYMENT`.
  - Solo se pueden liberar entradas al stock disponible desde reservas en estado `PENDING_PAYMENT` (por expiración/cancelación) o `CONFIRMED` (por devolución autorizada).

- **RN-06 - Aislamiento de Reservas por Usuario:**
  - Un comprador solo puede consultar, detallar o cancelar sus propias reservas activas. Ningún comprador puede modificar o consultar reservas asociadas a otro usuario.

---

## 4. Reglas de Configuración de Eventos y Localidades

- **RN-07 - Inmutabilidad de Capacidad con Reservas Activas:**
  - No se permite reducir la capacidad máxima de una localidad a un valor inferior a la suma del stock ya reservado o vendido.

- **RN-08 - Asignación de Precios Válidos:**
  - El precio asignado a una localidad/entrada debe ser siempre un valor mayor o igual a cero ($Precio \ge 0$).

---

## 5. Mapeo de Excepciones de Dominio (En Inglés)

Toda violación a estas reglas en el código fuente debe generar excepciones explícitas en inglés (cumpliendo la Regla 3):

| Regla | Excepción de Dominio | Código HTTP |
|---|---|---|
| **RN-01** | `InsufficientStockException` | `400 Bad Request` / `409 Conflict` |
| **RN-03** | `ExceededMaxTicketsPerRequestException` | `400 Bad Request` |
| **RN-04** | `ReservationExpiredException` | `410 Gone` |
| **RN-06** | `UnauthorizedReservationAccessException` | `403 Forbidden` |
| **RN-07** | `InvalidCapacityReductionException` | `422 Unprocessable Entity` |
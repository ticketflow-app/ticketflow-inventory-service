# Modelo de Datos y Persistencia - Microservicio Inventory

## 1. Estrategia Híbrida de Almacenamiento

El microservicio utiliza un modelo de persistencia políglota para resolver dos necesidades diferentes:

1. **Redis (In-Memory / Atómico):** Diseñado para consistencia inmediata y lecturas/escrituras masivas no bloqueantes de la disponibilidad en tiempo real.
2. **PostgreSQL (R2DBC / Relacional):** Diseñado para mantener el registro auditable, histórico y persistente de las reservas en disco.

## 2. Esquema en Redis (Tiempo Real)

Redis almacena la disponibilidad de entradas utilizando contadores atómicos por evento y sección.

### Estructura de Claves

```text
availability:{event_id}:{section_id}
```

- **Tipo de dato:** String (Contador numérico entero).
- **Operaciones autorizadas:** `DECRBY`, `INCRBY`, `GET`, `SET`.
- **Carga inicial:** La capacidad total de la sección se inicializa desde el servicio de Catálogo/Organizador.

## 3. Esquema en PostgreSQL (R2DBC)

Se utiliza un esquema dedicado denominado `inventario` (o tabla aislada dentro de la BD del prototipo), accedido mediante **Spring Data R2DBC** no bloqueante.

### Tabla: `reservations`

| Columna | Tipo de dato | Restricciones | Descripción |
|---|---|---|---|
| `id` | `UUID` / `BIGSERIAL` | `PRIMARY KEY` | Identificador único de la reserva |
| `user_id` | `VARCHAR(100)` | `NOT NULL` | ID del comprador extraído del JWT |
| `event_id` | `VARCHAR(100)` | `NOT NULL`, `INDEX` | ID del evento |
| `section_id` | `VARCHAR(100)` | `NOT NULL`, `INDEX` | ID de la sección/localidad |
| `quantity` | `INTEGER` | `NOT NULL`, `CHECK(quantity > 0)` | Cantidad de boletos reservados |
| `status` | `VARCHAR(20)` | `NOT NULL` | Estado: `PENDING`, `CONFIRMED`, `CANCELLED` |
| `idempotency_key` | `VARCHAR(255)` | `UNIQUE`, `INDEX` | Clave de idempotencia para evitar duplicados |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Fecha y hora de creación |

## 4. Entidades de Dominio (Modelos Java)

Los agregados de dominio no contienen anotaciones `@Table`, `@Entity` ni `@Id` de Spring/JPA/R2DBC:

- **`Inventory`**: Representa la disponibilidad actual y valida que las reservas no superen la capacidad.
- **`Reservation`**: Contiene la lógica de cambio de estado (`PENDING` → `CONFIRMED` / `CANCELLED`) e inmutabilidad.
- **`TicketAvailability`**: Objeto de valor para representar el stock actual por sección.

## 5. ADRs Relacionados con Persistencia

- **ADR-003 (Utilizar Redis para disponibilidad):** Permite procesar picos de alta demanda de stock en memoria mediante operaciones atómicas.
- **ADR-004 (Utilizar PostgreSQL para persistencia):** Garantiza durabilidad y consistencia transaccional para el registro histórico de reservas.
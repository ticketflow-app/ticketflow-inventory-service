# Flujos de negocio del microservicio Inventory

## 1. Propósito

Este documento describe los flujos de negocio correspondientes al microservicio Inventory, responsable de consultar la disponibilidad de entradas, gestionar reservas y mantener la información de las reservas dentro del sistema de venta de entradas.

Los flujos se describen desde la perspectiva del microservicio Inventory, sin atribuirle responsabilidades de otros microservicios.

Cuando el comportamiento de una operación todavía no está definido por completo, se indica explícitamente como pendiente de decisión.

## 2. Alcance

Este documento contempla los siguientes flujos:

- Consulta de disponibilidad de entradas.
- Creación de una reserva.
- Consulta de reservas de un usuario.
- Cancelación de una reserva.
- Integración con Redis, PostgreSQL y Kafka cuando corresponde.

Los endpoints REST definidos para Inventory son:

| Método | Endpoint | Propósito |
|---|---|---|
| GET | `/api/v1/availability/{eventId}` | Consultar disponibilidad de entradas para un evento. |
| POST | `/api/v1/reservations` | Solicitar la creación de una reserva. |
| GET | `/api/v1/reservations/user/{userId}` | Consultar las reservas asociadas a un usuario. |
| DELETE | `/api/v1/reservations/{reservationId}` | Solicitar la cancelación de una reserva. |

La existencia de estos endpoints no implica que todos sus comportamientos internos estén implementados o definidos en su totalidad.

## 3. Sistemas y componentes involucrados

### 3.1. Inventory

Es el microservicio responsable de ejecutar las operaciones relacionadas con disponibilidad y reservas.

### 3.2. Redis

Es la fuente de verdad operativa para la disponibilidad en tiempo real.

La clave de disponibilidad definida sigue el formato:

`availability:{event_id}:{section_id}`

Las operaciones consideradas para gestionar la disponibilidad incluyen `GET`, `SET`, `DECRBY` e `INCRBY`.

La estrategia exacta para coordinar las operaciones de Redis con la persistencia de reservas debe respetar las decisiones de arquitectura y los ADR correspondientes.

### 3.3. PostgreSQL

Se utiliza para persistir la información de las reservas mediante Spring Data R2DBC.

La información persistida debe permitir ejecutar las operaciones de consulta y gestión de reservas que estén definidas para Inventory.

### 3.4. Kafka

Se contempla como mecanismo de publicación de eventos relacionados con las reservas.

El contrato definitivo de los eventos, su publicación fiable y el momento exacto en el que deben emitirse requieren respetar las decisiones de integración pendientes y los ADR correspondientes.

## 4. Flujo de consulta de disponibilidad

**Endpoint:** `GET /api/v1/availability/{eventId}`

### 4.1. Objetivo

Consultar la disponibilidad de entradas asociada al evento solicitado.

### 4.2. Flujo general

1. El cliente envía una solicitud al endpoint con el identificador del evento.
2. Inventory recibe la solicitud mediante su adaptador de entrada REST.
3. La solicitud se dirige al componente de aplicación responsable de consultar la disponibilidad.
4. Inventory consulta la información de disponibilidad almacenada en Redis.
5. Inventory devuelve el resultado de la consulta al cliente.

### 4.3. Fuente de información

Redis es la fuente de verdad operativa para la disponibilidad en tiempo real. Por tanto, la consulta de disponibilidad debe utilizar esa fuente y no tratar PostgreSQL como la autoridad operativa de las cantidades disponibles.

### 4.4. Aspectos pendientes

Se deben definir o confirmar los siguientes detalles:

- La estructura exacta de la respuesta.
- El comportamiento cuando el evento no tiene información de disponibilidad.
- La representación de errores cuando Redis no está disponible.
- Si la consulta devuelve todas las secciones del evento o requiere algún parámetro adicional.

No se consideran definidos códigos HTTP específicos para estos escenarios mientras no estén respaldados por los contratos de API correspondientes.

## 5. Flujo de creación de una reserva

**Endpoint:** `POST /api/v1/reservations`

### 5.1. Objetivo

Solicitar la creación de una reserva para una cantidad determinada de entradas asociadas a un evento y una sección.

### 5.2. Datos de entrada definidos

La solicitud contempla los siguientes campos:

- `eventId`: identificador del evento.
- `sectionId`: identificador de la sección.
- `quantity`: cantidad de entradas solicitadas.

Además, se define el encabezado:

`Idempotency-Key: <UUID>`

Este encabezado permite identificar una solicitud para gestionar su idempotencia. Las reglas completas de almacenamiento, vigencia y tratamiento de solicitudes repetidas deben ajustarse a la especificación de idempotencia del proyecto.

### 5.3. Flujo general

1. El cliente envía la solicitud de creación de reserva con los datos requeridos y el encabezado de idempotencia.
2. Inventory recibe la solicitud mediante su adaptador REST.
3. La capa de aplicación procesa la solicitud y aplica las validaciones que estén definidas para la operación.
4. Inventory debe verificar y gestionar la disponibilidad utilizando Redis, que es la fuente de verdad operativa para las cantidades disponibles.
5. La reserva debe persistirse en PostgreSQL mediante el adaptador de persistencia.
6. Inventory devuelve una respuesta con el resultado de la solicitud.

La secuencia exacta de las operaciones sobre Redis y PostgreSQL, así como el tratamiento de fallos entre ambas, debe respetar las decisiones documentadas en los ADR de persistencia y consistencia.

### 5.4. Estado inicial de la reserva

La respuesta inicial definida para la creación de una reserva contempla el estado `PENDING`.

Este estado no debe interpretarse automáticamente como una reserva confirmada. La transición posterior a otros estados y las condiciones para considerar una reserva confirmada deben definirse mediante el contrato de negocio correspondiente.

### 5.5. Idempotencia

La operación utiliza el encabezado `Idempotency-Key` para identificar solicitudes.

El comportamiento esperado ante una repetición de la misma solicitud debe evitar que los reintentos generen reservas duplicadas. Sin embargo, los detalles de implementación, la persistencia de las claves y la respuesta que debe devolverse ante una repetición deben quedar alineados con la decisión específica de idempotencia.

### 5.6. Consistencia entre Redis y PostgreSQL

La creación de una reserva involucra dos sistemas con responsabilidades distintas:

- Redis administra la disponibilidad operativa.
- PostgreSQL persiste las reservas.

Una operación puede encontrar un fallo después de haber modificado uno de estos sistemas y antes de completar el proceso en el otro. Por ello, no debe asumirse que ambas operaciones constituyen una transacción atómica.

El tratamiento de estos escenarios debe seguir el ADR-007, que aborda la doble escritura y la compensación entre Redis y PostgreSQL.

La compensación mediante `INCRBY` puede considerarse una operación de recuperación, pero no debe suponerse que siempre tendrá éxito. El mecanismo definitivo para detectar y resolver posibles discrepancias debe estar respaldado por las decisiones de arquitectura.

### 5.7. Aspectos pendientes

Deben confirmarse los siguientes puntos:

- Orden definitivo de las operaciones sobre Redis y PostgreSQL.
- Validaciones completas de `quantity`, `eventId` y `sectionId`.
- Tratamiento de solicitudes con disponibilidad insuficiente.
- Estados permitidos para una reserva y sus transiciones.
- Respuesta ante fallos parciales de persistencia o actualización de disponibilidad.
- Reglas completas de idempotencia.
- Momento y condiciones en que una reserva se considera confirmada.

## 6. Flujo de consulta de reservas de un usuario

**Endpoint:** `GET /api/v1/reservations/user/{userId}`

### 6.1. Objetivo

Consultar las reservas asociadas al identificador de usuario proporcionado.

### 6.2. Flujo general

1. El cliente envía una solicitud con el identificador del usuario.
2. Inventory recibe la solicitud mediante su adaptador REST.
3. La capa de aplicación procesa la consulta.
4. Inventory consulta en PostgreSQL las reservas asociadas al identificador solicitado.
5. Inventory devuelve el resultado al cliente.

### 6.3. Fuente de información

PostgreSQL es la fuente de persistencia de las reservas. Esta consulta debe utilizar la información almacenada allí y no depender de Redis como repositorio de reservas.

### 6.4. Aspectos pendientes

Deben confirmarse:

- La estructura exacta de la respuesta.
- La paginación y los filtros disponibles, si se requieren.
- El comportamiento cuando no existen reservas para el identificador solicitado.
- Las reglas de autorización para consultar reservas.
- El código HTTP y la estructura de error para solicitudes no válidas.

Este documento no establece roles específicos ni supone que el usuario pueda consultar libremente las reservas de cualquier otro usuario.

## 7. Flujo de cancelación de una reserva

**Endpoint:** `DELETE /api/v1/reservations/{reservationId}`

### 7.1. Objetivo

Solicitar la cancelación de una reserva identificada mediante `reservationId`.

### 7.2. Flujo general

1. El cliente envía una solicitud de cancelación con el identificador de la reserva.
2. Inventory recibe la solicitud mediante su adaptador REST.
3. La capa de aplicación procesa la solicitud de cancelación.
4. Inventory consulta la información persistida de la reserva en PostgreSQL.
5. Inventory determina si la reserva cumple las condiciones definidas para permitir su cancelación.
6. Cuando corresponde cancelar la reserva, Inventory debe gestionar la actualización de su estado persistido y la liberación de disponibilidad.
7. Inventory devuelve el resultado de la operación.

Los pasos anteriores describen el objetivo general del flujo. El orden definitivo de las operaciones, las condiciones de cancelación y el tratamiento de fallos deben establecerse mediante las decisiones de negocio y arquitectura correspondientes.

### 7.3. Relación con la disponibilidad

Si una cancelación permite liberar las entradas reservadas, Inventory debe reflejar la cantidad liberada en Redis, respetando las reglas de consistencia y concurrencia definidas para la disponibilidad.

No debe asumirse que toda solicitud de cancelación libera entradas. Esta decisión depende del estado de la reserva y de las reglas de negocio que se definan.

### 7.4. Consistencia y recuperación

La cancelación puede implicar cambios tanto en PostgreSQL como en Redis. Estos cambios no deben tratarse como una transacción atómica entre ambos sistemas.

El tratamiento de fallos parciales debe respetar el ADR-007 y las decisiones definidas para las operaciones de compensación.

### 7.5. Aspectos pendientes

Deben confirmarse:

- Los estados de reserva que permiten la cancelación.
- El comportamiento ante una reserva inexistente.
- La regla para evitar cancelaciones repetidas o concurrentes.
- El orden de actualización de PostgreSQL y Redis.
- El comportamiento cuando falla la actualización de disponibilidad.
- El código HTTP y la estructura de respuesta de la operación.

## 8. Integración con Kafka

Inventory contempla Kafka para publicar eventos relacionados con las reservas.

La publicación de un evento debe representar una situación de negocio definida y no debe utilizarse para afirmar que una reserva está confirmada si el proceso de confirmación todavía no se ha completado.

Se ha considerado el nombre `inventory.reservations.confirmed` para el topic y `RESERVATION_CONFIRMED` para el tipo de evento. Estos nombres deben tratarse como propuestas mientras no exista una decisión aprobada que los establezca.

### 8.1. Aspectos pendientes

Antes de fijar el flujo definitivo de publicación deben definirse:

- Los eventos que Inventory debe publicar.
- El contrato y la estructura de cada evento.
- El momento exacto de publicación.
- La estrategia para manejar fallos de publicación.
- Las garantías de entrega requeridas.
- La relación entre la persistencia de reservas y la publicación de eventos.

La elección de un mecanismo como Outbox, si resulta necesaria, debe evaluarse y aprobarse explícitamente; no se considera una funcionalidad implementada por el solo hecho de mencionarse aquí.

## 9. Principios comunes a los flujos

Los flujos de Inventory deben respetar los siguientes principios:

1. **Responsabilidad delimitada:** Inventory gestiona disponibilidad y reservas, sin asumir las responsabilidades propias de otros microservicios.
2. **Fuente de verdad operativa:** Redis es la fuente de verdad para la disponibilidad en tiempo real.
3. **Persistencia de reservas:** PostgreSQL almacena la información persistente de las reservas.
4. **Consistencia explícita:** las operaciones distribuidas entre Redis y PostgreSQL deben considerar fallos parciales y respetar los ADR aplicables.
5. **Arquitectura hexagonal:** las reglas de negocio deben mantenerse independientes de los adaptadores y tecnologías externas.
6. **Contratos definidos:** los endpoints, los datos de entrada y las respuestas deben ajustarse a los contratos aprobados.
7. **Decisiones trazables:** los comportamientos no confirmados deben mantenerse como pendientes hasta que se aprueben.

## 10. Decisiones pendientes de completar

Antes de considerar definitivos todos los flujos, es necesario cerrar los siguientes puntos:

- Contratos completos de solicitud y respuesta para cada endpoint.
- Validaciones de negocio y códigos de error.
- Estados de las reservas y transiciones permitidas.
- Reglas de idempotencia para la creación de reservas.
- Secuencia de actualización entre Redis y PostgreSQL.
- Condiciones de cancelación y liberación de disponibilidad.
- Contratos y garantías de publicación de eventos Kafka.
- Reglas de autorización aplicables a cada operación.
- Estrategias de recuperación ante fallos parciales.

Estas decisiones deben documentarse en los requisitos, contratos de API o ADR correspondientes antes de presentarlas como comportamiento definitivo del microservicio.
# Componentes y Responsabilidades - Microservicio Inventory

## 1. Propósito

Este documento identifica los componentes principales del microservicio Inventory, define sus responsabilidades y establece las relaciones entre ellos.

El diseño se basa en la arquitectura hexagonal definida en `01_arquitectura_hexagonal.md` y busca mantener una separación clara entre las reglas de negocio, la coordinación de los casos de uso y los mecanismos de infraestructura.

Los nombres propuestos podrán ajustarse durante la implementación, siempre que se mantengan las responsabilidades y las restricciones arquitectónicas establecidas.

## 2. Componentes del Sistema

Los componentes se agrupan en tres capas:

- **Domain:** representa el modelo y las reglas del negocio.
- **Application:** contiene los casos de uso y los contratos de interacción.
- **Infrastructure:** implementa los adaptadores de entrada y salida y la configuración técnica.

## 3. Componentes del Dominio

### 3.1. Reservation

Representa una reserva de inventario.

Sus responsabilidades son:

- Representar el identificador de la reserva.
- Identificar al usuario que realiza la reserva.
- Identificar el evento y la sección reservada.
- Registrar la cantidad solicitada.
- Representar el estado de la reserva.
- Mantener las reglas de transición de estado que correspondan al dominio.

Los estados previstos son:

- `PENDING`: la reserva ha sido registrada, pero todavía no se considera confirmada.
- `CONFIRMED`: la reserva ha sido confirmada según el flujo de negocio definido.
- `CANCELLED`: la reserva ha sido cancelada.

Las transiciones permitidas deberán definirse explícitamente para evitar confirmaciones o cancelaciones inválidas.

### 3.2. TicketAvailability

Representa la disponibilidad de entradas para un evento y una sección determinados.

Sus responsabilidades son:

- Representar la disponibilidad conocida.
- Permitir expresar las restricciones sobre las cantidades disponibles.
- Servir como concepto del dominio para las operaciones de consulta y reserva.

La disponibilidad almacenada en Redis será la referencia operativa para realizar la actualización atómica de cantidades. El modelo de dominio no deberá depender directamente de la estructura física de las claves Redis.

### 3.3. Reglas de Dominio

Las reglas de dominio deberán garantizar que:

- La cantidad solicitada sea mayor que cero.
- Una reserva no pueda confirmarse si no cumple las condiciones del negocio.
- Las transiciones de estado sean válidas.
- Una cancelación respete las condiciones definidas para la reserva.
- Las operaciones de negocio no permitan exceder la capacidad disponible.

Las reglas deberán mantenerse independientes de los detalles de HTTP, persistencia y mensajería.

## 4. Componentes de Aplicación

### 4.1. CreateReservationUseCase

Coordina la creación de una reserva.

Responsabilidades:

- Recibir los datos necesarios para crear la reserva.
- Validar las condiciones de entrada y las reglas de negocio.
- Aplicar la política de idempotencia.
- Coordinar la actualización de disponibilidad mediante el puerto correspondiente.
- Coordinar el registro de la reserva en PostgreSQL.
- Gestionar los resultados y errores de la operación.
- Aplicar las estrategias de compensación definidas cuando una operación parcial falla.

Este componente no deberá ejecutar directamente comandos Redis ni consultas R2DBC. Utilizará los puertos definidos para esas capacidades.

La coordinación entre Redis y PostgreSQL no constituye una transacción distribuida automática. La implementación deberá considerar los fallos parciales y los mecanismos de recuperación correspondientes.

### 4.2. GetAvailabilityUseCase

Coordina la consulta de disponibilidad de un evento.

Responsabilidades:

- Recibir el identificador del evento.
- Consultar las disponibilidades correspondientes mediante el puerto de salida.
- Devolver los datos necesarios para construir la respuesta de la API.
- Gestionar los errores de consulta.

La respuesta deberá ajustarse al contrato de la API y no exponer información interna de Redis.

### 4.3. GetUserReservationsUseCase

Coordina la consulta de las reservas de un usuario.

Responsabilidades:

- Recibir la identidad del usuario autorizado.
- Solicitar las reservas correspondientes mediante el puerto de persistencia.
- Devolver los resultados al adaptador de entrada.
- Aplicar las reglas de acceso definidas para el caso de uso.

La autorización debe garantizar que un comprador no pueda consultar las reservas de otro usuario sin permisos suficientes.

### 4.4. CancelReservationUseCase

Coordina la cancelación de una reserva.

Responsabilidades:

- Identificar la reserva solicitada.
- Verificar su existencia y estado actual.
- Verificar que el usuario esté autorizado para cancelarla.
- Aplicar las reglas de negocio de cancelación.
- Coordinar la actualización de la reserva y la devolución de disponibilidad.
- Gestionar los fallos parciales y evitar liberar repetidamente las mismas unidades.

La cancelación deberá diseñarse para impedir que solicitudes concurrentes provoquen una devolución duplicada de inventario.

### 4.5. Política de Idempotencia

La política de idempotencia permite gestionar solicitudes repetidas de creación de reservas.

Responsabilidades:

- Identificar una solicitud mediante `Idempotency-Key`.
- Detectar si la misma operación ya fue procesada.
- Evitar la creación de reservas duplicadas por reintentos de una misma solicitud.
- Devolver un resultado coherente con la operación original cuando corresponda.

La clave de idempotencia deberá tener una restricción de unicidad apropiada en la persistencia. También deberán definirse las reglas para solicitudes concurrentes con la misma clave y para claves reutilizadas con un contenido diferente.

La implementación podrá materializar esta responsabilidad en un componente independiente o integrarla en el caso de uso, según el diseño detallado.

## 5. Puertos de Aplicación

Los siguientes puertos representan contratos propuestos. Sus nombres definitivos y firmas deberán concretarse durante la implementación.

### 5.1. Puertos de Entrada

| Puerto propuesto | Responsabilidad |
|---|---|
| `CreateReservationPort` | Exponer el caso de uso de creación de reservas. |
| `GetAvailabilityPort` | Exponer el caso de uso de consulta de disponibilidad. |
| `GetUserReservationsPort` | Exponer el caso de uso de consulta de reservas de un usuario. |
| `CancelReservationPort` | Exponer el caso de uso de cancelación de reservas. |

Los puertos de entrada permiten que los adaptadores invocan las operaciones de aplicación sin depender de sus implementaciones concretas.

### 5.2. Puertos de Salida

| Puerto propuesto | Responsabilidad |
|---|---|
| `AvailabilityRepositoryPort` | Consultar y actualizar la disponibilidad mediante operaciones apropiadas. |
| `ReservationRepositoryPort` | Guardar, consultar y actualizar reservas persistidas. |
| `ReservationEventPublisherPort` | Publicar los eventos de negocio definidos para las reservas. |

Podrán añadirse otros puertos cuando exista una necesidad arquitectónica justificada, por ejemplo, para separar explícitamente la idempotencia o las operaciones de compensación.

Los puertos no deberán incluir tipos específicos de clientes Redis, repositorios R2DBC ni productores de Kafka.

## 6. Adaptadores de Infraestructura

### 6.1. ReservationController

Adaptador REST responsable de exponer las operaciones de reservas.

Responsabilidades:

- Recibir las solicitudes HTTP.
- Validar los datos de entrada.
- Obtener la identidad autenticada mediante el mecanismo de seguridad configurado.
- Invocar el puerto de entrada correspondiente.
- Transformar los resultados en respuestas HTTP.
- Gestionar los códigos de estado y los errores de la API.

El controlador no deberá actualizar Redis ni ejecutar consultas de persistencia directamente.

### 6.2. AvailabilityController

Adaptador REST responsable de exponer la consulta de disponibilidad.

Responsabilidades:

- Recibir la solicitud de consulta.
- Validar el identificador del evento.
- Invocar `GetAvailabilityPort`.
- Construir la respuesta de disponibilidad.
- Gestionar los errores de consulta.

Podrá combinarse con el controlador de reservas si el diseño final favorece una única clase controladora, siempre que se mantengan separadas las responsabilidades de cada operación.

### 6.3. RedisAvailabilityAdapter

Implementa el acceso a Redis para gestionar la disponibilidad.

Responsabilidades:

- Consultar la disponibilidad de un evento y una sección.
- Ejecutar actualizaciones atómicas de cantidades.
- Interpretar los resultados de las operaciones Redis.
- Gestionar errores de comunicación.
- Aplicar las convenciones de claves definidas para el inventario.

La convención de claves propuesta es:

`availability:{event_id}:{section_id}`

La reserva deberá utilizar una operación atómica para modificar la cantidad disponible y evitar que varias solicitudes concurrentes reserven las mismas unidades.

La comprobación y la actualización deberán diseñarse conjuntamente para que la disponibilidad no quede negativa como resultado de reservas aceptadas.

### 6.4. PostgresReservationAdapter

Implementa la persistencia de las reservas mediante Spring Data R2DBC y PostgreSQL.

Responsabilidades:

- Guardar las reservas.
- Consultar reservas por identificador.
- Consultar las reservas asociadas a un usuario.
- Actualizar los estados de las reservas.
- Aplicar las restricciones de persistencia definidas.
- Gestionar errores de acceso a la base de datos.

La tabla prevista es `reservations`. Su esquema definitivo deberá permanecer alineado con el documento de modelo de datos.

Las restricciones de unicidad y las actualizaciones condicionadas deberán contribuir a proteger la idempotencia y las transiciones válidas de estado.

### 6.5. KafkaReservationEventAdapter

Implementa la publicación de eventos de negocio mediante Kafka.

Responsabilidades:

- Transformar los eventos de aplicación al formato de mensajería definido.
- Publicar los eventos correspondientes a las reservas.
- Gestionar los errores de publicación.
- Registrar los resultados necesarios para el diagnóstico.

El evento `RESERVATION_CONFIRMED` y el topic `inventory.reservations.confirmed` son propuestas para comunicar la confirmación de una reserva.

La publicación de un evento deberá corresponder a una confirmación válida. El mecanismo para garantizar la coherencia entre la persistencia y la publicación deberá definirse explícitamente.

### 6.6. Adaptadores de Seguridad y Observabilidad

Los mecanismos de seguridad deberán validar los JWT y aplicar las reglas de autorización antes de ejecutar las operaciones protegidas.

La observabilidad deberá recopilar métricas, registros y estados de salud mediante Micrometer y Spring Boot Actuator.

Estos componentes pertenecen a la infraestructura y no deberán introducir dependencias de Spring Security, Actuator o Micrometer en las entidades del dominio.

## 7. Matriz de Responsabilidades

| Operación | Aplicación | Infraestructura |
|---|---|---|
| Consultar disponibilidad | `GetAvailabilityUseCase` | `AvailabilityController`, `RedisAvailabilityAdapter` |
| Crear una reserva | `CreateReservationUseCase` | `ReservationController`, `RedisAvailabilityAdapter`, `PostgresReservationAdapter` |
| Consultar reservas | `GetUserReservationsUseCase` | `ReservationController`, `PostgresReservationAdapter` |
| Cancelar una reserva | `CancelReservationUseCase` | `ReservationController`, adaptadores de persistencia y disponibilidad |
| Publicar confirmación | Coordinar el evento dentro del flujo definido | `KafkaReservationEventAdapter` |
| Validar JWT | Aplicación recibe la identidad y permisos necesarios | Adaptadores y configuración de seguridad |

La tabla representa responsabilidades lógicas. No implica que cada responsabilidad deba implementarse en una clase independiente.

## 8. Manejo de Errores

Los componentes deberán distinguir entre errores de negocio, errores de entrada y errores técnicos.

Se consideran, entre otros:

- Cantidad solicitada inválida.
- Disponibilidad insuficiente.
- Reserva inexistente.
- Transición de estado no permitida.
- Clave de idempotencia reutilizada de manera incompatible.
- Acceso no autorizado.
- Fallo de Redis.
- Fallo de persistencia en PostgreSQL.
- Fallo de publicación en Kafka.

La aplicación deberá producir resultados o errores que permitan al adaptador REST construir respuestas coherentes. La infraestructura no deberá exponer detalles internos de las excepciones al cliente.

Los errores técnicos que puedan provocar operaciones parciales deberán activar los mecanismos de compensación o recuperación definidos por la arquitectura.

## 9. Consideraciones de Concurrencia y Consistencia

La distribución de responsabilidades deberá garantizar que los componentes colaboren de manera segura bajo concurrencia.

En particular:

- Redis deberá proporcionar la actualización atómica de la disponibilidad.
- PostgreSQL deberá proteger las restricciones de persistencia mediante mecanismos apropiados.
- La idempotencia deberá impedir reservas duplicadas por reintentos.
- Las cancelaciones deberán evitar liberar inventario más de una vez.
- Los errores entre Redis y PostgreSQL deberán tratarse mediante los mecanismos de compensación y recuperación definidos.
- La publicación de eventos deberá mantener la coherencia con el estado de la reserva.

No se asumirá que Redis, PostgreSQL y Kafka comparten una transacción atómica. Los fallos parciales y la recuperación deberán abordarse en el diseño de los flujos de negocio.

## 10. Criterios de Aceptación

El diseño de componentes se considerará conforme cuando:

- Cada componente tenga una responsabilidad identificable.
- Los casos de uso coordinen las operaciones sin depender directamente de clientes de infraestructura.
- Los adaptadores implementen los contratos definidos por la aplicación.
- Las reglas del dominio permanezcan independientes de Spring y de los mecanismos de persistencia.
- Las operaciones de reserva y cancelación contemplen concurrencia, idempotencia y fallos parciales.
- Los errores puedan gestionarse sin exponer detalles internos.
- La estructura permita probar los componentes de manera aislada y mediante pruebas de integración.
- Los nombres y responsabilidades definitivos se mantengan alineados con la arquitectura hexagonal y los ADR aprobados.

## 11. Decisiones Pendientes

Antes de considerar cerrado el diseño detallado, deberán concretarse:

- Las firmas de los puertos y los casos de uso.
- La estrategia para resolver fallos parciales entre Redis y PostgreSQL.
- El control de concurrencia para las cancelaciones.
- El mecanismo de publicación fiable de eventos Kafka, si es requerido.
- El contrato definitivo de los eventos de reserva.
- La estrategia exacta para gestionar solicitudes simultáneas con la misma clave de idempotencia.

Estas decisiones deberán documentarse mediante los ADR correspondientes cuando impliquen una elección arquitectónica relevante.
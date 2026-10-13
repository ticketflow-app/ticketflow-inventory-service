# Integración y mensajería del microservicio Inventory

## 1. Propósito

Este documento describe las integraciones del microservicio Inventory con los sistemas externos que utiliza para gestionar la disponibilidad, persistir las reservas y publicar eventos.

Se establecen las responsabilidades generales de Redis, PostgreSQL y Kafka, así como los aspectos de integración que deben definirse antes de considerar completo el diseño.

Este documento no sustituye los requisitos funcionales, los contratos de API ni las decisiones arquitectónicas registradas en los ADR.

## 2. Alcance

Las integraciones contempladas para Inventory son:

- Redis para gestionar la disponibilidad de entradas en tiempo real.
- PostgreSQL para persistir la información de las reservas.
- Kafka para la publicación de eventos relacionados con las reservas.
- Spring WebFlux y Spring Data R2DBC como tecnologías de acceso reactivo.
- Spring Boot Actuator y Micrometer para las capacidades de salud y observabilidad relacionadas con el servicio.

La responsabilidad del documento se limita a Inventory. No define los contratos internos de otros microservicios ni les atribuye funcionalidades.

## 3. Integración con Redis

### 3.1. Propósito

Redis es la fuente de verdad operativa para la disponibilidad de entradas. Inventory lo utiliza para consultar y modificar las cantidades disponibles durante las operaciones relacionadas con las reservas.

### 3.2. Convención de claves

La clave definida para identificar la disponibilidad de una sección de un evento utiliza el siguiente formato:

`availability:{event_id}:{section_id}`

Los valores concretos almacenados y las reglas completas de inicialización deben ajustarse al contrato de datos que se establezca para Inventory.

### 3.3. Operaciones contempladas

Las operaciones consideradas para administrar la disponibilidad son:

- `GET`: consultar el valor almacenado.
- `SET`: establecer un valor de disponibilidad.
- `DECRBY`: disminuir la cantidad disponible.
- `INCRBY`: incrementar la cantidad disponible.

La selección y secuencia de estas operaciones debe garantizar que la gestión de disponibilidad respete las reglas de concurrencia definidas para el servicio.

No debe suponerse que una secuencia de comandos independientes constituye una operación atómica completa de negocio. Las garantías necesarias deben quedar definidas en el diseño de la operación correspondiente.

### 3.4. Disponibilidad insuficiente

La creación de una reserva debe respetar la cantidad disponible en Redis. La operación no debe permitir que la disponibilidad se reduzca por debajo de los límites establecidos por las reglas de negocio.

El mecanismo exacto de validación y actualización atómica debe corresponder al diseño aprobado para la gestión concurrente de disponibilidad.

### 3.5. Fallos de Redis

La indisponibilidad de Redis puede impedir consultar o modificar correctamente la disponibilidad.

Inventory debe tratar estos errores sin asumir que la operación solicitada se completó correctamente. El resultado que debe devolverse al cliente y la estrategia de recuperación deben definirse en los contratos y decisiones de resiliencia correspondientes.

### 3.6. Aspectos pendientes

Deben confirmarse los siguientes detalles:

- Estructura exacta de los valores almacenados.
- Inicialización y actualización de la disponibilidad.
- Mecanismo definitivo de atomicidad para las operaciones concurrentes.
- Política de reintentos y límites temporales.
- Tratamiento de valores inexistentes o inválidos.
- Estrategia para detectar discrepancias entre Redis y PostgreSQL.

## 4. Integración con PostgreSQL

### 4.1. Propósito

PostgreSQL proporciona la persistencia de las reservas de Inventory.

La disponibilidad operativa en tiempo real corresponde a Redis; PostgreSQL no debe tratarse como la fuente de verdad operativa de las cantidades disponibles.

### 4.2. Acceso a datos

El acceso a PostgreSQL se contempla mediante Spring Data R2DBC, en concordancia con el enfoque reactivo del servicio.

La integración debe mantenerse detrás de los puertos de salida definidos por la arquitectura hexagonal. La capa de dominio no debe depender directamente de R2DBC, de PostgreSQL ni de anotaciones propias de la infraestructura de persistencia.

### 4.3. Responsabilidades de persistencia

La integración con PostgreSQL debe permitir almacenar y recuperar la información de reservas requerida por los casos de uso definidos para Inventory.

Entre las operaciones que necesitan persistencia se encuentran la creación de reservas, la consulta de reservas y la gestión de su cancelación.

El modelo físico definitivo, los campos de cada entidad, las restricciones y la estrategia de migraciones deben ajustarse al esquema de base de datos aprobado.

### 4.4. Fallos de persistencia

Un error de PostgreSQL puede producirse después de que Redis haya sido consultado o modificado.

Por este motivo, Inventory no debe asumir que una modificación de disponibilidad implica automáticamente que la reserva quedó persistida.

El comportamiento ante fallos parciales debe seguir las decisiones de consistencia y compensación documentadas en los ADR aplicables, especialmente el ADR-007.

### 4.5. Aspectos pendientes

Deben confirmarse:

- Esquema definitivo de las tablas de reservas.
- Restricciones y reglas de integridad.
- Estrategia de migraciones de base de datos.
- Límites transaccionales dentro de PostgreSQL.
- Tratamiento de errores y tiempos de espera.
- Mecanismo para detectar y resolver inconsistencias con Redis.

## 5. Consistencia entre Redis y PostgreSQL

### 5.1. Problema de consistencia

La gestión de una reserva involucra dos sistemas diferentes:

- Redis, que administra la disponibilidad operativa.
- PostgreSQL, que persiste las reservas.

Una operación que modifica ambos sistemas puede quedar incompleta si uno de ellos falla. No existe una transacción atómica común entre ambos por el simple hecho de utilizar los dos durante el mismo caso de uso.

### 5.2. Principio de diseño

La coordinación entre Redis y PostgreSQL debe seguir la decisión registrada en el ADR-007, que aborda la doble escritura y la compensación.

El diseño debe considerar, como mínimo:

- Fallos al actualizar Redis.
- Fallos al persistir una reserva.
- Fallos durante una compensación.
- Solicitudes concurrentes sobre la misma disponibilidad.
- Reintentos de una solicitud cuyo resultado anterior no se conoce con certeza.
- Posibles discrepancias entre la disponibilidad operativa y las reservas persistidas.

### 5.3. Compensación

Se ha considerado el uso de `INCRBY` para compensar una disminución de disponibilidad cuando corresponda.

Esta operación no debe presentarse como una garantía absoluta de recuperación: también puede fallar y, por tanto, no asegura por sí sola la consistencia definitiva.

Las condiciones exactas de compensación, el orden de las operaciones y la recuperación ante una compensación fallida deben ajustarse al ADR-007.

### 5.4. Recuperación y reconciliación

La detección y reparación de discrepancias es un aspecto importante para mantener la consistencia entre los sistemas.

Sin embargo, el mecanismo concreto de reconciliación, su frecuencia, su nivel de automatización y las herramientas que utilizaría deben definirse explícitamente antes de considerarlos parte de la solución implementada.

## 6. Integración con Kafka

### 6.1. Propósito

Kafka se contempla como mecanismo de mensajería para publicar eventos relacionados con las reservas de Inventory.

La publicación de eventos permite comunicar hechos de negocio a los consumidores que correspondan, sin introducir dependencias directas entre la lógica de dominio y la tecnología de mensajería.

### 6.2. Arquitectura de integración

Desde la perspectiva de la arquitectura hexagonal, la publicación de eventos debe realizarse mediante un puerto de salida de la capa de aplicación y un adaptador de infraestructura encargado de la integración con Kafka.

El nombre definitivo del puerto y del adaptador depende de la estructura aprobada del código.

La capa de dominio no debe depender directamente de clientes Kafka ni de clases propias de Spring Kafka.

### 6.3. Topic y contrato de eventos

Se han considerado los siguientes nombres como propuestas:

- Topic: `inventory.reservations.confirmed`.
- Tipo de evento: `RESERVATION_CONFIRMED`.

Estos nombres no deben considerarse contratos definitivos hasta que exista una decisión aprobada que los establezca.

También deben definirse la estructura del mensaje, sus identificadores, los datos de negocio incluidos y las reglas de compatibilidad entre versiones.

### 6.4. Momento de publicación

El momento exacto en que Inventory debe publicar un evento depende del estado de la reserva y de las reglas de negocio aprobadas.

No debe publicarse un evento que represente una reserva confirmada únicamente porque se recibió una solicitud o se creó una reserva en estado `PENDING`.

Las condiciones que permiten considerar una reserva confirmada y emitir el evento correspondiente deben quedar establecidas en el contrato de negocio.

### 6.5. Fallos de publicación

La publicación en Kafka puede fallar aunque la información de la reserva ya se encuentre persistida.

Por tanto, el diseño debe definir cómo se evita que un fallo de publicación produzca una pérdida silenciosa de eventos requeridos.

La utilización de Outbox u otro mecanismo de publicación fiable puede evaluarse si las garantías de entrega lo requieren, pero no se considera una decisión aprobada ni una funcionalidad implementada por el hecho de mencionarla en este documento.

### 6.6. Aspectos pendientes

Deben definirse:

- Eventos que Inventory debe publicar.
- Topics definitivos.
- Contratos y versiones de los mensajes.
- Condiciones y momento de publicación.
- Garantías de entrega requeridas.
- Tratamiento de reintentos y errores.
- Estrategia para coordinar la persistencia de reservas con la publicación de eventos.
- Necesidad de mecanismos de deduplicación en los consumidores, si corresponde al contrato de integración.

## 7. Integración reactiva

Inventory utiliza Spring WebFlux y contempla Spring Data R2DBC para el acceso a PostgreSQL.

Las integraciones deben respetar el modelo reactivo del servicio y evitar bloquear innecesariamente los hilos utilizados para procesar solicitudes.

El diseño de los adaptadores debe considerar:

- Propagación adecuada de errores.
- Composición de operaciones asíncronas.
- Gestión de tiempos de espera.
- Control de concurrencia.
- Liberación adecuada de recursos.

El uso de una biblioteca o de un cliente reactivo específico para Redis y Kafka debe corresponder a las dependencias aprobadas del proyecto. No se deben añadir dependencias sin autorización.

## 8. Seguridad de las integraciones

Las conexiones con Redis, PostgreSQL y Kafka deben utilizar los mecanismos de autenticación, autorización y protección de comunicaciones que se definan para el entorno de ejecución.

Las credenciales y demás secretos deben gestionarse mediante configuración externa y no deben incluirse directamente en el código fuente ni en los archivos de documentación con valores reales.

La validación local de JWT forma parte del alcance de seguridad considerado para Inventory. Los detalles de las credenciales, las claves de validación, los claims y las reglas de autorización deben ajustarse al contrato de seguridad aprobado.

Este documento no establece roles, permisos o claims específicos que no hayan sido confirmados.

## 9. Observabilidad de las integraciones

Las integraciones deben poder diagnosticarse mediante los mecanismos de observabilidad contemplados para Inventory.

Spring Boot Actuator y Micrometer se consideran las tecnologías previstas para las capacidades de salud y métricas.

La observabilidad debe permitir evaluar, según la configuración aprobada:

- Estado del servicio.
- Errores en las operaciones de Redis.
- Errores en la persistencia de PostgreSQL.
- Fallos en la publicación de eventos.
- Latencia de las operaciones.
- Indicadores relacionados con la disponibilidad y las reservas.

Los nombres definitivos de las métricas, sus etiquetas, umbrales y alertas deben establecerse en la configuración de observabilidad correspondiente. No se consideran implementados únicamente por aparecer en este documento.

## 10. Decisiones y documentos relacionados

El diseño de estas integraciones debe mantenerse alineado con los siguientes documentos:

- ADR-003: decisión relacionada con Redis y la disponibilidad.
- ADR-004: decisión relacionada con PostgreSQL y la persistencia.
- ADR-007: decisión relacionada con la doble escritura, la consistencia y la compensación entre Redis y PostgreSQL.
- `01_arquitectura_hexagonal.md`: estructura arquitectónica del microservicio.
- `02_componentes_y_responsabilidades.md`: responsabilidades propuestas para los componentes.
- `03_flujos_de_negocio.md`: flujos de disponibilidad y reservas.

Los detalles de integración que no estén resueltos en estos documentos deben mantenerse como pendientes hasta que exista una decisión explícita.

## 11. Resumen de decisiones pendientes

Antes de considerar completa la definición de las integraciones, deben resolverse los siguientes puntos:

1. Mecanismo definitivo de actualización atómica de disponibilidad en Redis.
2. Esquema y restricciones definitivas de las reservas en PostgreSQL.
3. Secuencia de operaciones y recuperación ante fallos parciales según el ADR-007.
4. Contratos definitivos de eventos Kafka.
5. Garantías de publicación y coordinación entre persistencia y mensajería.
6. Clientes y dependencias concretas aprobadas para las integraciones reactivas.
7. Configuración de seguridad para conexiones y validación de JWT.
8. Métricas, comprobaciones de salud y alertas que se implementarán.

Estas decisiones deben quedar registradas en los requisitos, los ADR o los documentos de configuración correspondientes antes de presentarlas como funcionalidades aprobadas o implementadas.
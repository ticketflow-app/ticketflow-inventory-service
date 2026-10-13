# Estrategia de pruebas de integración

## 1. Propósito

Definir la estrategia para verificar la comunicación y el comportamiento conjunto de los componentes del microservicio Inventory y sus dependencias externas.

Las pruebas de integración buscan detectar errores en los adaptadores de infraestructura, la persistencia, la comunicación con servicios externos, el intercambio de datos y el cumplimiento de los contratos definidos.

Estas pruebas complementan las pruebas unitarias y permiten verificar comportamientos que no pueden validarse completamente mediante dependencias simuladas.

## 2. Alcance

Se contemplan las siguientes integraciones:

- Redis para consultar y modificar la disponibilidad de los eventos.
- PostgreSQL para persistir y consultar las reservas.
- Kafka para la publicación y el consumo de eventos, según las responsabilidades que finalmente se definan.
- API REST para recibir solicitudes y devolver respuestas HTTP.
- Spring Data R2DBC para la interacción reactiva con PostgreSQL.
- Validación de JWT y mecanismos de autorización dentro del flujo de solicitudes, de acuerdo con los requisitos de seguridad documentados.

Las pruebas deben respetar la arquitectura hexagonal. La lógica de negocio se mantiene en el dominio y la aplicación, mientras que los adaptadores de infraestructura se encargan de comunicarse con las tecnologías externas.

## 3. Principios de las pruebas de integración

Las pruebas deben seguir estos principios:

1. **Entorno controlado:** las dependencias deben configurarse de manera que sus resultados puedan reproducirse.
2. **Datos aislados:** cada escenario debe utilizar datos identificables y evitar interferir con otras pruebas.
3. **Contratos verificables:** las solicitudes, respuestas, estructuras de datos y eventos deben corresponder con los contratos definidos.
4. **Validación de errores:** se deben probar tanto las operaciones exitosas como los fallos de comunicación y persistencia.
5. **Consistencia:** se deben verificar las invariantes que involucren varias dependencias.
6. **Trazabilidad:** cada escenario debe vincularse con requisitos, atributos de calidad o decisiones arquitectónicas.
7. **Limpieza:** las pruebas deben restaurar o eliminar los datos que hayan creado, cuando corresponda.

## 4. Integración con Redis

### 4.1. Objetivo

Verificar que el adaptador de Redis gestione correctamente la disponibilidad y ejecute las operaciones definidas para mantener el contador de unidades disponibles.

Redis es la fuente de verdad operativa para la disponibilidad en tiempo real, según la arquitectura documentada.

La clave de disponibilidad establecida sigue el formato:

`availability:{event_id}:{section_id}`

Entre las operaciones contempladas se encuentran `GET`, `SET`, `DECRBY` e `INCRBY`.

### 4.2. Casos de prueba

Se deben contemplar los siguientes escenarios:

**Consulta de disponibilidad**

- Consultar una clave que contiene una disponibilidad registrada.
- Consultar una clave inexistente.
- Verificar que el valor recuperado se interprete correctamente.
- Comprobar el manejo de respuestas o valores que no cumplan el formato esperado, si ese escenario es posible según el adaptador.

**Actualización de disponibilidad**

- Inicializar el contador de una sección.
- Reducir la disponibilidad mediante una operación de decremento.
- Incrementar la disponibilidad mediante una operación de compensación.
- Verificar que los cambios se reflejen en consultas posteriores.
- Comprobar que las operaciones respeten las restricciones de disponibilidad establecidas por el diseño.

**Errores de comunicación**

- Intentar una operación cuando Redis no está disponible.
- Verificar el manejo de errores de conexión y de ejecución.
- Comprobar el comportamiento del adaptador ante tiempos de espera agotados.
- Verificar que un error no se interprete como una operación exitosa.

### 4.3. Concurrencia y atomicidad

Las pruebas deben comprobar que las operaciones de actualización del contador utilicen los mecanismos atómicos definidos para Redis.

Se deben ejecutar operaciones concurrentes sobre una misma clave y verificar que el resultado final sea coherente con las operaciones aceptadas.

En particular, se debe comprobar que el mecanismo utilizado para reservar disponibilidad impida asignar más unidades de las disponibles.

La prueba debe considerar el flujo real implementado: la atomicidad de una operación de Redis no garantiza por sí sola que una secuencia de operaciones que también incluye PostgreSQL sea atómica.

### 4.4. Criterios de aceptación

- Las operaciones devuelven resultados coherentes con los valores almacenados.
- Los cambios realizados pueden verificarse mediante consultas posteriores.
- Los errores de comunicación se detectan y manejan explícitamente.
- La concurrencia no permite violar las restricciones de disponibilidad.
- La aplicación no informa una operación como exitosa cuando la actualización necesaria ha fallado.

## 5. Integración con PostgreSQL

### 5.1. Objetivo

Verificar que el adaptador de persistencia guarde y recupere correctamente las reservas mediante Spring Data R2DBC.

PostgreSQL se utiliza para conservar la información persistente de las reservas, de acuerdo con la arquitectura documentada.

### 5.2. Casos de prueba

**Persistencia**

- Guardar una reserva válida.
- Recuperar una reserva previamente guardada.
- Verificar que los atributos persistidos correspondan con los enviados.
- Consultar las reservas asociadas a un usuario.
- Actualizar la información o el estado de una reserva cuando la operación esté definida.
- Verificar el comportamiento ante identificadores inexistentes.

**Validaciones y restricciones**

- Comprobar que se respeten las restricciones de integridad definidas en el esquema.
- Verificar el comportamiento ante datos que no cumplan las restricciones de la base de datos.
- Comprobar que los errores de persistencia se propaguen o traduzcan de acuerdo con el contrato de la aplicación.

**Comportamiento reactivo**

- Verificar que las operaciones reactivas produzcan los resultados esperados al suscribirse y completarse.
- Comprobar el manejo de errores emitidos por las operaciones de persistencia.
- Evitar que las pruebas confundan la creación de un flujo reactivo con la ejecución y finalización efectiva de la operación.

### 5.3. Criterios de aceptación

- Las reservas se almacenan y recuperan correctamente.
- Los datos persistidos corresponden con el modelo definido.
- Las restricciones de integridad se respetan.
- Los errores de base de datos no se presentan como operaciones exitosas.
- Las operaciones reactivas se completan de acuerdo con el resultado esperado.

## 6. Integración entre Redis y PostgreSQL

### 6.1. Objetivo

Evaluar el comportamiento de los casos de uso que involucran tanto la disponibilidad en Redis como la persistencia de las reservas en PostgreSQL.

Esta integración requiere especial atención porque ambos sistemas no comparten automáticamente una transacción atómica.

### 6.2. Casos de prueba

Se deben considerar, de acuerdo con el flujo de negocio aprobado:

- La disponibilidad se actualiza y la reserva se persiste correctamente.
- Redis rechaza o no puede completar la actualización de disponibilidad.
- La actualización de disponibilidad se completa, pero falla la persistencia en PostgreSQL.
- La persistencia responde con un error después de una modificación de disponibilidad.
- Se produce un fallo durante el mecanismo de compensación, si este está implementado.
- Se repite una solicitud debido a un error de comunicación o a un reintento.
- Se detecta una discrepancia entre la disponibilidad operativa y las reservas persistidas.

### 6.3. Verificaciones

Para cada escenario se debe determinar:

- Qué operaciones se completaron realmente.
- Qué información quedó persistida.
- Qué valor tiene la disponibilidad después de la operación.
- Qué resultado recibe el caso de uso.
- Si se ejecutó algún mecanismo de compensación.
- Si el sistema detecta y registra una posible inconsistencia.

El orden definitivo de las operaciones, las garantías de recuperación y el mecanismo de compensación deben corresponder con los ADR y la implementación vigente.

Una compensación mediante `INCRBY` puede fallar; por tanto, las pruebas no deben asumir que una compensación intentada siempre restablece el estado anterior.

### 6.4. Criterios de aceptación

- Las operaciones exitosas producen el estado esperado en ambas dependencias.
- Los fallos parciales se detectan y se manejan de acuerdo con el diseño aprobado.
- Las discrepancias no se ocultan ni se presentan como consistencia garantizada.
- Los mecanismos de compensación, cuando estén implementados, se verifican tanto en sus casos exitosos como en sus fallos.
- Los mecanismos de detección y reparación de inconsistencias se prueban cuando estén definidos.

## 7. Integración con Kafka

### 7.1. Objetivo

Verificar la comunicación con Kafka para el intercambio de eventos relacionados con las reservas, según el mecanismo de mensajería que se apruebe para el microservicio.

La publicación fiable de eventos y su relación con la persistencia son aspectos que deben evaluarse conforme al diseño definitivo.

### 7.2. Casos de prueba

Cuando la integración correspondiente esté implementada, se deben contemplar:

- Publicación de un evento con la estructura y los datos esperados.
- Verificación de que el evento se envíe al destino configurado.
- Manejo de errores al intentar publicar un evento.
- Comportamiento cuando Kafka no está disponible.
- Manejo de tiempos de espera agotados.
- Comportamiento ante reintentos, si están configurados.
- Verificación de duplicados y del procesamiento repetido cuando corresponda.
- Validación de los campos obligatorios y del formato del evento.
- Comprobación del comportamiento del consumidor, si Inventory tiene esa responsabilidad.

El topic `inventory.reservations.confirmed` y el evento `RESERVATION_CONFIRMED` son nombres propuestos en la documentación y deben confirmarse antes de tratarlos como contratos definitivos.

### 7.3. Criterios de aceptación

- Los eventos publicados respetan el contrato definido.
- Los errores de publicación se detectan y registran.
- El resultado comunicado por la aplicación corresponde con las garantías reales de publicación.
- Los reintentos y el procesamiento de duplicados se comportan de acuerdo con la configuración implementada.
- No se asume entrega fiable de eventos si el mecanismo necesario todavía no está definido o implementado.

## 8. Integración de la API REST

### 8.1. Objetivo

Verificar que los endpoints REST conecten correctamente las solicitudes HTTP con los casos de uso y conviertan sus resultados en respuestas apropiadas.

### 8.2. Endpoints incluidos

**Consultar disponibilidad**

`GET /api/v1/availability/{eventId}`

Verificar la consulta de disponibilidad, el formato de la respuesta y el manejo de errores.

**Crear una reserva**

`POST /api/v1/reservations`

Verificar la validación del cuerpo de la solicitud, el manejo de la cabecera `Idempotency-Key` y la respuesta correspondiente al resultado del caso de uso.

**Consultar reservas de un usuario**

`GET /api/v1/reservations/user/{userId}`

Verificar la recuperación de reservas, la estructura de la respuesta y el manejo de solicitudes inválidas o no autorizadas.

**Cancelar una reserva**

`DELETE /api/v1/reservations/{reservationId}`

Verificar el procesamiento de la solicitud, el resultado de la cancelación y el manejo de errores.

### 8.3. Casos de prueba generales

Para cada endpoint se deben considerar:

- Solicitud válida.
- Parámetros o cuerpo inválidos.
- Campos obligatorios ausentes.
- Identificadores que no correspondan con un recurso existente.
- Errores provenientes del caso de uso o de la infraestructura.
- Código HTTP y estructura de respuesta esperados.
- Manejo consistente de los errores.
- Validación de autenticación y autorización según el contrato de seguridad definido.

Los códigos HTTP concretos deben contrastarse con los requisitos y el contrato de la API. No deben suponerse únicamente a partir del tipo de error interno.

### 8.4. Criterios de aceptación

- Cada endpoint invoca el caso de uso correspondiente.
- Las solicitudes válidas producen respuestas compatibles con el contrato.
- Las solicitudes inválidas se rechazan correctamente.
- Los errores internos se manejan sin exponer información sensible.
- Los códigos HTTP y las estructuras de respuesta corresponden con los requisitos documentados.

## 9. Integración de seguridad

Las pruebas de integración deben verificar la seguridad en el contexto real de las solicitudes.

Se deben considerar los siguientes escenarios, de acuerdo con el documento de seguridad:

- Solicitud con un token JWT válido.
- Token inválido, expirado o con firma incorrecta.
- Solicitud sin token en un endpoint que requiera autenticación.
- Acceso a una operación sin los permisos requeridos.
- Intento de consultar o cancelar reservas pertenecientes a otro usuario.
- Solicitudes con datos inválidos.
- Verificación de que las respuestas no expongan secretos ni detalles internos innecesarios.

Los resultados esperados deben alinearse con las reglas de autenticación y autorización definidas en `../atributos_calidad/04_seguridad.md`.

Estas pruebas verifican el comportamiento requerido; su inclusión en la estrategia no implica que los controles ya estén implementados.

## 10. Preparación del entorno

Antes de ejecutar las pruebas de integración se debe verificar que:

1. Las dependencias necesarias estén disponibles y configuradas.
2. Las variables de entorno requeridas estén definidas.
3. Las credenciales de prueba tengan permisos limitados.
4. Las bases de datos y las claves de Redis estén aisladas de datos ajenos a las pruebas.
5. Los topics y consumidores de Kafka estén configurados cuando el escenario los requiera.
6. Los datos iniciales puedan prepararse de forma reproducible.
7. Exista un mecanismo para limpiar o restablecer el estado después de las pruebas.

La herramienta utilizada para levantar las dependencias y la configuración específica del entorno se definirán en `05_datos_y_entorno_de_pruebas.md`.

## 11. Evidencias y registro de resultados

Cada ejecución debe registrar, según corresponda:

- Identificador del escenario.
- Componente o integración evaluada.
- Condiciones iniciales y datos utilizados.
- Resultado esperado y resultado observado.
- Estado de la prueba: satisfactoria, fallida, bloqueada o no ejecutada.
- Mensajes de error y registros relevantes.
- Defectos detectados y referencias relacionadas.

Las evidencias deben permitir reproducir el problema sin exponer tokens, contraseñas, credenciales u otros datos sensibles.

## 12. Aspectos pendientes de definición

Los siguientes aspectos deben confirmarse cuando no exista una decisión vigente:

- Herramientas y versiones para ejecutar las pruebas.
- Estrategia para levantar y aislar Redis, PostgreSQL y Kafka.
- Orden definitivo de las operaciones entre Redis y PostgreSQL.
- Garantías de recuperación y compensación ante fallos parciales.
- Mecanismo de publicación fiable de eventos en Kafka.
- Contrato definitivo de los eventos y sus topics.
- Códigos HTTP y estructuras de error para cada endpoint.
- Criterios cuantitativos para pruebas de concurrencia.
- Automatización de la preparación y limpieza de datos.

Estos puntos deben resolverse mediante los requisitos, las decisiones arquitectónicas o los cambios aprobados, no mediante suposiciones dentro de las pruebas.

## 13. Resultado esperado

La aplicación de esta estrategia debe proporcionar evidencia de que las integraciones del microservicio Inventory funcionan conforme a sus contratos, que los errores se manejan de manera controlada y que los fallos parciales pueden detectarse.

Los resultados deben ayudar a validar el funcionamiento conjunto de la aplicación y servir como base para las pruebas especializadas de calidad, seguridad, concurrencia, rendimiento y resiliencia.
# Disponibilidad y Resiliencia - Microservicio Inventory

## 1. Propósito

Este documento define los requisitos de disponibilidad y resiliencia del microservicio Inventory, considerando escenarios de alta demanda, saturación de recursos y fallos en sus dependencias externas.

Estos atributos orientarán las decisiones arquitectónicas mediante Attribute-Driven Design (ADD) y serán evaluados mediante pruebas de carga, fallos y recuperación.

## 2. Disponibilidad

### 2.1. Definición

Capacidad del microservicio para responder correctamente a las solicitudes durante su operación, incluso cuando se presentan condiciones de carga elevada o interrupciones temporales.

### 2.2. Importancia

Durante una venta de entradas, una interrupción puede impedir que los usuarios consulten la disponibilidad o realicen reservas.

Por esta razón, Inventory debe mantener su capacidad de respuesta mientras sus recursos y dependencias funcionen correctamente, y debe informar adecuadamente cuando no pueda atender una solicitud.

### 2.3. Indicadores

- **Porcentaje de solicitudes exitosas:** proporción de solicitudes que terminan con una respuesta satisfactoria.
- **Tasa de errores:** proporción de solicitudes que terminan con errores.
- **Tiempo fuera de servicio:** periodo durante el cual el microservicio no puede atender las solicitudes previstas.
- **Tiempo de recuperación:** tiempo necesario para volver a un estado operativo después de una interrupción.
- **Disponibilidad de dependencias:** estado operativo de Redis, PostgreSQL y Kafka cuando corresponda.

## 3. Resiliencia

### 3.1. Definición

Capacidad del sistema para detectar, gestionar y recuperarse de fallos sin provocar consecuencias incontroladas sobre el inventario o las reservas.

### 3.2. Importancia

Inventory depende de varios componentes tecnológicos. Un fallo en Redis puede impedir la gestión segura de la disponibilidad, mientras que un fallo en PostgreSQL puede impedir registrar de forma persistente una reserva.

La resiliencia debe evitar que los errores de una dependencia se propaguen innecesariamente y comprometan el funcionamiento del resto del sistema.

### 3.3. Estrategias arquitectónicas

#### Circuit Breaker

Se utilizará Resilience4j para proteger las operaciones externas seleccionadas frente a fallos repetidos.

Cuando se supere el umbral configurado, el circuito podrá abrirse temporalmente para evitar llamadas que probablemente fallen. Después de un periodo de espera, permitirá comprobar si el componente se ha recuperado.

#### Reintentos controlados

Se podrán aplicar reintentos no bloqueantes sobre operaciones que fallen de forma transitoria.

Los reintentos deberán tener límites y tiempos de espera definidos. No se aplicarán indiscriminadamente a operaciones de reserva que puedan ejecutarse más de una vez y alterar el inventario.

#### Manejo de tiempos de espera

Las operaciones hacia Redis, PostgreSQL y Kafka deberán contar con límites de espera adecuados para evitar que solicitudes pendientes consuman recursos indefinidamente.

#### Compensación de operaciones

Si Redis descuenta entradas correctamente, pero PostgreSQL falla al registrar la reserva, el caso de uso intentará restaurar la disponibilidad mediante una operación compensatoria en Redis.

La compensación también puede fallar. Por ello, este mecanismo deberá generar métricas y registros que permitan detectar posibles inconsistencias y facilitar su recuperación.

#### Aislamiento de fallos

Los errores de una dependencia deberán gestionarse de forma explícita, evitando que se conviertan en fallos no controlados dentro de los casos de uso o de los controladores.

## 4. Escenarios de Calidad

### 4.1. Escenario de fallo de PostgreSQL

- **Atributo:** resiliencia y consistencia.
- **Estímulo:** PostgreSQL deja de estar disponible cuando una solicitud intenta registrar una reserva.
- **Entorno:** Inventory se encuentra operativo y Redis permite ejecutar operaciones atómicas.
- **Respuesta esperada:** el sistema no debe confirmar una reserva que no haya quedado registrada correctamente. Debe intentar compensar el descuento de disponibilidad y reportar el fallo.
- **Medición:** solicitudes fallidas, errores de persistencia, resultado de las compensaciones y diferencias detectadas en el inventario.

### 4.2. Escenario de fallo de Redis

- **Atributo:** disponibilidad y resiliencia.
- **Estímulo:** Redis deja de responder durante una consulta o una operación de reserva.
- **Entorno:** Inventory continúa ejecutándose, pero no puede verificar o actualizar la disponibilidad mediante Redis.
- **Respuesta esperada:** el sistema debe rechazar o interrumpir de forma controlada las operaciones que requieran acceso a Redis, sin asumir que existe disponibilidad suficiente ni confirmar reservas inseguras.
- **Medición:** tasa de errores, latencia, tiempo de detección y comportamiento de los endpoints afectados.

### 4.3. Escenario de fallo de Kafka

- **Atributo:** resiliencia.
- **Estímulo:** Kafka no está disponible cuando Inventory intenta publicar un evento de reserva confirmada.
- **Entorno:** la reserva ya ha sido confirmada y registrada.
- **Respuesta esperada:** el fallo de publicación debe gestionarse de acuerdo con la política de eventos definida, sin revertir automáticamente una reserva confirmada si la publicación puede recuperarse de manera independiente.
- **Medición:** errores de publicación, eventos pendientes, tiempo de recuperación y eventos publicados correctamente después de restablecerse el servicio.

### 4.4. Escenario de saturación

- **Atributo:** disponibilidad y resiliencia.
- **Estímulo:** el volumen de solicitudes supera la capacidad habitual de procesamiento.
- **Entorno:** Inventory recibe una carga elevada durante un periodo limitado.
- **Respuesta esperada:** el sistema debe gestionar los errores y rechazos de forma controlada, evitar operaciones indefinidamente pendientes y recuperar su comportamiento habitual después de reducirse la carga.
- **Medición:** tasa de errores, latencia, uso de recursos y tiempo de recuperación.

### 4.5. Escenario de recuperación de dependencias

- **Atributo:** disponibilidad y resiliencia.
- **Estímulo:** Redis o PostgreSQL vuelve a estar disponible después de una interrupción.
- **Entorno:** Inventory ha detectado previamente el fallo de la dependencia.
- **Respuesta esperada:** el sistema debe volver a utilizar la dependencia cuando se encuentre operativa, respetando las políticas de reintentos y recuperación configuradas.
- **Medición:** tiempo de recuperación, tasa de errores posterior y resultados de las operaciones realizadas tras la recuperación.

## 5. Observabilidad de Fallos

El sistema deberá registrar y exponer información suficiente para identificar interrupciones y evaluar su impacto.

Se utilizarán los siguientes mecanismos:

- **Spring Boot Actuator:** consulta del estado de salud del microservicio y de las dependencias configuradas.
- **Micrometer:** medición de errores, latencia, fallos de persistencia y resultados de las operaciones compensatorias.
- **Registros de aplicación:** identificación de errores, excepciones y fallos de comunicación relevantes.
- **Métricas de recuperación:** seguimiento del tiempo necesario para recuperar la operación normal.

La información registrada no deberá incluir tokens JWT, secretos ni otros datos sensibles.

## 6. Criterios de Aceptación

El microservicio deberá cumplir los siguientes criterios:

- Los fallos de Redis y PostgreSQL deben gestionarse de manera controlada.
- El sistema no debe confirmar reservas que no hayan cumplido las condiciones de persistencia y confirmación definidas.
- Los errores de persistencia posteriores a un descuento de disponibilidad deben activar el mecanismo de compensación previsto.
- Los fallos de compensación deben quedar registrados y generar información observable para su seguimiento.
- Los reintentos deben estar limitados y evitar la ejecución duplicada de operaciones no idempotentes.
- La recuperación de las dependencias debe poder evaluarse mediante pruebas y métricas.
- Los fallos de publicación en Kafka deben gestionarse según una política explícita que permita determinar si los eventos se recuperaron o quedaron pendientes.

Los objetivos numéricos de disponibilidad y tiempo máximo de recuperación se definirán antes de las pruebas formales, de acuerdo con el alcance y los recursos del prototipo.

## 7. Relación con Otros Atributos de Calidad

- **Consistencia:** los mecanismos de recuperación no deben producir sobreventas ni confirmar reservas incorrectamente.
- **Concurrencia:** las operaciones compensatorias deben considerar las solicitudes concurrentes para evitar alterar reservas válidas.
- **Rendimiento:** los tiempos de espera, reintentos y circuit breakers deben limitar el impacto de los fallos sobre la latencia.
- **Observabilidad:** las métricas y los registros permiten detectar interrupciones y verificar la recuperación.
- **Mantenibilidad:** la separación de responsabilidades facilita modificar las políticas de resiliencia sin incorporar esta lógica directamente en los controladores.
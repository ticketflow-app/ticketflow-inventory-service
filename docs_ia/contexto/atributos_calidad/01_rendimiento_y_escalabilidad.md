# Rendimiento y Escalabilidad - Microservicio Inventory

## 1. Propósito

Este documento define los requisitos de rendimiento y escalabilidad del microservicio Inventory, considerando el escenario de venta de entradas con alta demanda y múltiples solicitudes concurrentes.

Estos atributos orientarán las decisiones arquitectónicas mediante Attribute-Driven Design (ADD) y serán evaluados mediante pruebas de carga y rendimiento.

## 2. Rendimiento

### 2.1. Definición

Capacidad del microservicio para procesar solicitudes de manera eficiente, manteniendo tiempos de respuesta adecuados y un consumo controlado de recursos computacionales.

### 2.2. Importancia

El rendimiento es fundamental durante los periodos de alta demanda, cuando numerosos usuarios consultan la disponibilidad o intentan reservar entradas simultáneamente.

El sistema debe aprovechar las operaciones atómicas de Redis y el procesamiento reactivo de Spring WebFlux para gestionar las solicitudes sin introducir bloqueos innecesarios.

### 2.3. Indicadores

- **Throughput:** cantidad de solicitudes procesadas por segundo.
- **Latencia:** tiempo que transcurre desde la recepción de una solicitud hasta la entrega de su respuesta.
- **Percentil P95:** tiempo de respuesta por debajo del cual se completa el 95 % de las solicitudes.
- **Percentil P99:** tiempo de respuesta por debajo del cual se completa el 99 % de las solicitudes.
- **Tasa de errores:** proporción de solicitudes que terminan con errores.
- **Uso de CPU:** porcentaje de capacidad de procesamiento consumida.
- **Uso de memoria:** cantidad de memoria utilizada durante las pruebas.

### 2.4. Operaciones prioritarias

Las pruebas de rendimiento se concentrarán en los siguientes endpoints:

- `GET /api/v1/availability/{eventId}`: consulta de disponibilidad de entradas.
- `POST /api/v1/reservations`: creación de reservas mediante operaciones atómicas sobre Redis y persistencia en PostgreSQL.

La consulta de disponibilidad permitirá evaluar el rendimiento de las lecturas, mientras que la creación de reservas permitirá analizar el costo adicional de controlar la concurrencia y registrar las operaciones.

## 3. Escalabilidad

### 3.1. Definición

Capacidad del microservicio para soportar incrementos en el número de usuarios y solicitudes sin experimentar una degradación inaceptable de su rendimiento.

### 3.2. Importancia

La venta de entradas puede producir incrementos repentinos del tráfico en periodos cortos. Por esta razón, Inventory debe poder gestionar diferentes niveles de demanda y permitir evaluar cómo cambia su comportamiento al aumentar la carga.

### 3.3. Estrategia arquitectónica

La arquitectura del microservicio considera los siguientes mecanismos:

- **Procesamiento reactivo:** Spring WebFlux permite gestionar operaciones de entrada y salida sin bloquear innecesariamente los hilos durante las esperas de E/S.
- **Operaciones atómicas en Redis:** permiten controlar la disponibilidad compartida entre solicitudes concurrentes.
- **Separación de responsabilidades:** la arquitectura hexagonal facilita identificar y evaluar el comportamiento de los componentes de aplicación e infraestructura.
- **Escalabilidad horizontal:** la ejecución de varias instancias de Inventory podrá evaluarse para determinar si permite atender una mayor demanda sin comprometer la consistencia del inventario.

La escalabilidad horizontal dependerá también de la capacidad de Redis, PostgreSQL y los demás recursos compartidos. Aumentar las instancias de Inventory no garantiza por sí solo un incremento proporcional del rendimiento.

## 4. Escenarios de Calidad

### 4.1. Escenario de carga sostenida

- **Atributo:** rendimiento.
- **Estímulo:** un generador de carga envía solicitudes de forma constante durante un periodo determinado.
- **Entorno:** microservicio desplegado en contenedores, con Redis y PostgreSQL disponibles.
- **Respuesta esperada:** Inventory procesa las solicitudes y mantiene un comportamiento estable durante la prueba.
- **Medición:** throughput, latencia P95 y P99, tasa de errores, CPU y memoria.

### 4.2. Escenario de incremento repentino

- **Atributo:** escalabilidad.
- **Estímulo:** el generador de carga incrementa rápidamente el número de solicitudes concurrentes.
- **Entorno:** microservicio sometido a una carga creciente, con sus dependencias operativas.
- **Respuesta esperada:** Inventory continúa atendiendo solicitudes dentro de los límites de calidad que se definan para el experimento, sin producir sobreventas.
- **Medición:** throughput, latencia P95 y P99, tasa de errores y consumo de recursos.

### 4.3. Escenario de pico de tráfico

- **Atributo:** rendimiento y escalabilidad.
- **Estímulo:** una gran cantidad de solicitudes llega en un intervalo corto, simulando la apertura de una venta.
- **Entorno:** sistema sometido a una carga superior a la habitual.
- **Respuesta esperada:** Inventory procesa las solicitudes que puede atender y gestiona adecuadamente los rechazos y errores, sin comprometer la consistencia del inventario.
- **Medición:** throughput, latencia, tasa de errores, uso de recursos y tiempo necesario para recuperar el comportamiento habitual después del pico.

## 5. Estrategia de Evaluación

La evaluación se realizará mediante pruebas de carga y rendimiento que permitan comparar el comportamiento del microservicio bajo diferentes niveles de demanda.

Las pruebas deberán contemplar:

1. Una carga inicial para establecer una línea base de rendimiento.
2. Una carga sostenida para observar la estabilidad del sistema.
3. Un incremento progresivo de solicitudes para identificar el punto en que comienza la degradación.
4. Un pico repentino de tráfico para evaluar la respuesta ante una demanda elevada.
5. Una comparación entre diferentes niveles de carga y, cuando corresponda, distintas cantidades de instancias del microservicio.

Durante las pruebas se registrarán las métricas definidas en este documento, manteniendo condiciones de ejecución comparables.

## 6. Criterios de Aceptación

El microservicio deberá cumplir los siguientes criterios:

- Las pruebas deben producir mediciones de throughput, latencia P95, latencia P99, tasa de errores y consumo de recursos.
- El comportamiento del sistema debe poder compararse entre diferentes niveles de carga.
- Deben identificarse los niveles de demanda a partir de los cuales la latencia o la tasa de errores presentan una degradación significativa.
- El incremento de instancias debe evaluarse para determinar su efecto real sobre la capacidad de procesamiento.
- Las operaciones de reserva deben mantener la consistencia del inventario, independientemente de la carga aplicada.

Los umbrales numéricos de latencia, throughput, tasa de errores y consumo de recursos se definirán antes de ejecutar las pruebas formales, según la capacidad disponible y los objetivos experimentales del proyecto.

## 7. Relación con Otros Atributos de Calidad

El rendimiento y la escalabilidad se relacionan directamente con los siguientes atributos:

- **Concurrencia:** el sistema debe gestionar solicitudes simultáneas sin generar conflictos en las reservas.
- **Consistencia:** el aumento de carga no debe permitir que se reserve una cantidad de entradas superior a la disponibilidad.
- **Disponibilidad:** la saturación de recursos puede incrementar los errores y reducir la capacidad de respuesta.
- **Observabilidad:** las métricas permiten identificar degradaciones y comparar los resultados de las pruebas.

La evaluación de estos atributos se complementará con los documentos específicos de concurrencia, consistencia, disponibilidad y observabilidad.
# Observabilidad y Mantenibilidad - Microservicio Inventory

## 1. Propósito

Este documento define los requisitos de observabilidad y mantenibilidad del microservicio Inventory.

La observabilidad permitirá conocer el estado del servicio, identificar errores y analizar su comportamiento bajo diferentes condiciones de carga.

La mantenibilidad permitirá modificar, probar y extender el microservicio sin introducir cambios innecesarios en sus reglas de negocio ni comprometer su arquitectura.

Estos requisitos orientarán las decisiones arquitectónicas y servirán como referencia para las pruebas de calidad.

## 2. Observabilidad

### 2.1. Definición

La observabilidad es la capacidad de comprender el comportamiento interno del sistema mediante la información que expone durante su ejecución.

Para Inventory, esta capacidad se apoyará principalmente en métricas, registros y verificaciones de salud.

### 2.2. Métricas principales

El microservicio utilizará Micrometer y Spring Boot Actuator para recopilar y exponer las métricas compatibles con su configuración.

Las métricas prioritarias serán:

| Categoría | Indicadores |
|---|---|
| Rendimiento | Latencia promedio, P95, P99 y solicitudes por segundo. |
| Errores | Cantidad y proporción de solicitudes fallidas. |
| Recursos | Uso de CPU y memoria. |
| Disponibilidad | Estado de salud del servicio y sus dependencias. |
| Concurrencia | Solicitudes simultáneas y resultados de las operaciones de reserva. |
| Inventario | Cantidad disponible, reservas procesadas y operaciones rechazadas por falta de disponibilidad. |
| Resiliencia | Activaciones del Circuit Breaker, reintentos y fallos de dependencias. |

Las métricas deberán permitir relacionar los problemas observados con las operaciones del microservicio, especialmente la consulta de disponibilidad, la creación de reservas y su cancelación.

### 2.3. Endpoints de salud

Spring Boot Actuator permitirá comprobar el estado general del servicio y, según la configuración, el estado de sus dependencias.

El endpoint de salud deberá utilizarse para distinguir entre:

- **Servicio operativo:** puede atender las solicitudes previstas.
- **Dependencia degradada:** una dependencia presenta problemas que afectan determinadas operaciones.
- **Servicio no disponible:** existe una condición que impide atender las operaciones esenciales.

La inclusión de Redis, PostgreSQL y Kafka en las verificaciones de salud deberá configurarse de acuerdo con su importancia para cada operación. Un fallo de Kafka, por ejemplo, no necesariamente debe impedir consultar la disponibilidad si esa operación puede seguir funcionando de manera segura.

Los endpoints de diagnóstico no deberán exponer información sensible ni habilitar operaciones administrativas a usuarios no autorizados.

### 2.4. Registros de aplicación

Los registros deberán facilitar la investigación de errores y la reconstrucción del flujo de una solicitud.

Se recomienda registrar:

- Identificador de correlación de la solicitud.
- Operación ejecutada.
- Resultado de la operación.
- Duración de la solicitud.
- Errores de Redis, PostgreSQL y Kafka.
- Activación de mecanismos de resiliencia.
- Rechazos por falta de disponibilidad.
- Fallos durante la creación, persistencia o cancelación de reservas.

Los registros no deberán incluir tokens JWT completos, credenciales ni otros datos sensibles.

Cuando sea posible, los registros relacionados con una misma operación deberán compartir un identificador de correlación. La clave de idempotencia podrá utilizarse para investigar una operación, pero no deberá tratarse como un secreto ni registrarse innecesariamente.

### 2.5. Alertas y detección de problemas

Las métricas recopiladas deberán permitir detectar condiciones como:

- Incremento sostenido de la latencia.
- Aumento de la tasa de errores.
- Agotamiento de recursos.
- Fallos repetidos en Redis o PostgreSQL.
- Activaciones frecuentes del Circuit Breaker.
- Diferencias entre la disponibilidad registrada en Redis y las reservas persistidas.
- Acumulación de reservas en estado `PENDING`.

Los umbrales de alertas deberán establecerse durante la configuración de las pruebas de calidad y del entorno de ejecución. No se fijarán valores arbitrarios sin resultados de referencia.

## 3. Mantenibilidad

### 3.1. Definición

La mantenibilidad es la capacidad de comprender, modificar, probar y extender el software con un esfuerzo controlado y sin introducir regresiones innecesarias.

En Inventory, la mantenibilidad se apoyará en la arquitectura hexagonal, la separación de responsabilidades, las pruebas automatizadas y la documentación de las decisiones arquitectónicas.

### 3.2. Arquitectura hexagonal

El microservicio mantendrá la separación entre dominio, aplicación e infraestructura.

- **Domain:** contiene las entidades, reglas de negocio y conceptos propios del inventario.
- **Application:** coordina los casos de uso y define los puertos necesarios para interactuar con otras capas.
- **Infrastructure:** implementa los adaptadores para la API REST, Redis, PostgreSQL, Kafka, seguridad y observabilidad.

Las dependencias deberán dirigirse hacia el dominio. Las reglas de negocio no deberán depender directamente de Spring, Redis, PostgreSQL ni Kafka.

Esta separación permitirá modificar una tecnología de infraestructura sin exigir cambios generalizados en las reglas de negocio.

### 3.3. Responsabilidades y límites

Cada componente deberá tener una responsabilidad claramente definida.

Los casos de uso coordinarán las operaciones de negocio, mientras que los adaptadores se encargarán de los detalles de comunicación con sistemas externos.

Por ejemplo, el caso de uso de creación de una reserva no deberá contener detalles específicos del cliente Redis ni de la configuración del productor Kafka. Esos detalles deberán mantenerse en los adaptadores correspondientes.

La lógica de concurrencia, idempotencia y compensación deberá estar documentada y contar con pruebas específicas, ya que involucra varias dependencias y puede producir inconsistencias si se modifica incorrectamente.

### 3.4. Pruebas automatizadas

La mantenibilidad dependerá de una estrategia de pruebas que permita detectar regresiones durante los cambios.

Se consideran los siguientes niveles:

- **Pruebas unitarias:** verifican las reglas de dominio y los casos de uso utilizando dependencias controladas.
- **Pruebas de integración:** verifican los adaptadores y la comunicación con Redis, PostgreSQL y Kafka.
- **Pruebas de API:** comprueban los contratos HTTP, la validación de solicitudes y las respuestas.
- **Pruebas de concurrencia:** verifican que las operaciones simultáneas no provoquen sobreventa.
- **Pruebas de resiliencia:** evalúan el comportamiento frente a fallos de dependencias y errores de persistencia.

Los cambios en reglas de negocio deberán acompañarse de las pruebas correspondientes. Las pruebas de integración no deberán sustituirse únicamente por pruebas unitarias cuando sea necesario validar el comportamiento real de las dependencias.

### 3.5. Documentación y decisiones arquitectónicas

Las decisiones arquitectónicas relevantes deberán registrarse en documentos ADR (Architecture Decision Records).

La documentación deberá explicar:

- El problema que motivó la decisión.
- Las alternativas consideradas.
- La decisión seleccionada.
- Las consecuencias y limitaciones conocidas.
- Los atributos de calidad afectados.

Las modificaciones de arquitectura, dependencias o contratos deberán reflejarse en la documentación correspondiente para mantenerla alineada con la implementación.

### 3.6. Gestión de dependencias y configuración

Las versiones de Java, Spring Boot y las dependencias deberán mantenerse bajo control mediante los archivos de configuración del proyecto.

Los cambios de dependencias deberán justificarse y validarse mediante compilación y pruebas automatizadas.

La configuración específica del entorno deberá mantenerse separada del código fuente. Las credenciales y secretos no deberán almacenarse directamente en el código.

## 4. Escenarios de Calidad

### 4.1. Escenario de detección de degradación

- **Atributo:** observabilidad.
- **Estímulo:** la latencia de las solicitudes aumenta durante una prueba de carga.
- **Entorno:** Inventory procesa solicitudes concurrentes.
- **Respuesta esperada:** las métricas permiten identificar el incremento de latencia, la tasa de errores y el consumo de recursos.
- **Medición:** latencia promedio, P95, P99, solicitudes por segundo, errores, CPU y memoria.

### 4.2. Escenario de fallo de una dependencia

- **Atributo:** observabilidad y resiliencia.
- **Estímulo:** Redis, PostgreSQL o Kafka presenta errores de comunicación.
- **Entorno:** Inventory se encuentra atendiendo solicitudes.
- **Respuesta esperada:** el sistema registra el fallo, expone el estado correspondiente y permite identificar qué operaciones están afectadas.
- **Medición:** errores de dependencia, estado de salud, activaciones del Circuit Breaker y duración de la degradación.

### 4.3. Escenario de modificación de infraestructura

- **Atributo:** mantenibilidad.
- **Estímulo:** se requiere cambiar la implementación de un adaptador de infraestructura.
- **Entorno:** el dominio y los casos de uso permanecen sin cambios funcionales.
- **Respuesta esperada:** la modificación se concentra en los componentes afectados, siempre que se mantengan los contratos definidos.
- **Medición:** cantidad de componentes modificados, pruebas superadas y regresiones detectadas.

### 4.4. Escenario de modificación de reglas de negocio

- **Atributo:** mantenibilidad.
- **Estímulo:** se modifica una regla relacionada con la creación o cancelación de reservas.
- **Entorno:** el proyecto dispone de pruebas automatizadas y documentación de los casos de uso.
- **Respuesta esperada:** el cambio puede implementarse y validarse sin introducir dependencias indebidas entre el dominio y la infraestructura.
- **Medición:** pruebas superadas, defectos introducidos y componentes afectados.

### 4.5. Escenario de investigación de una reserva fallida

- **Atributo:** observabilidad.
- **Estímulo:** una solicitud de reserva falla durante la ejecución.
- **Entorno:** se dispone de registros y métricas de la aplicación.
- **Respuesta esperada:** el equipo puede identificar la operación, consultar los errores relacionados y determinar si el fallo ocurrió durante la actualización de Redis, la persistencia en PostgreSQL o la publicación en Kafka.
- **Medición:** disponibilidad de información relevante y tiempo requerido para identificar la causa.

## 5. Estrategia de Evaluación

La observabilidad se evaluará mediante pruebas de carga, inyección controlada de fallos y revisión de métricas y registros.

La mantenibilidad se evaluará mediante revisión de la estructura del código, pruebas automatizadas y análisis del impacto de cambios representativos.

Las verificaciones incluirán:

1. Comprobar que las métricas principales están disponibles.
2. Verificar que los errores de las dependencias pueden identificarse.
3. Comprobar que los registros permiten correlacionar solicitudes relacionadas.
4. Verificar que los registros no exponen información sensible.
5. Revisar que el dominio no depende directamente de tecnologías de infraestructura.
6. Comprobar que los casos de uso pueden probarse de forma aislada.
7. Ejecutar las pruebas automatizadas después de cambios representativos.
8. Verificar que las decisiones arquitectónicas y la documentación reflejan el diseño vigente.

## 6. Criterios de Aceptación

El microservicio deberá cumplir los siguientes criterios:

- Las métricas definidas como prioritarias deben poder consultarse durante las pruebas.
- Los errores y fallos de dependencias deben producir información útil para el diagnóstico.
- Los endpoints de salud deben reflejar el estado configurado del servicio sin exponer información sensible.
- Los registros deben facilitar la investigación de solicitudes fallidas.
- La arquitectura deberá mantener las responsabilidades separadas entre dominio, aplicación e infraestructura.
- Las reglas de negocio deberán poder probarse sin depender obligatoriamente de servicios externos reales.
- Los cambios relevantes deberán validarse mediante pruebas automatizadas.
- Las decisiones arquitectónicas y la documentación deberán mantenerse actualizadas.

## 7. Relación con Otros Atributos de Calidad

- **Rendimiento y escalabilidad:** las métricas permiten identificar cuellos de botella y evaluar el comportamiento bajo carga.
- **Disponibilidad y resiliencia:** los indicadores de salud y los registros ayudan a detectar fallos y evaluar la recuperación.
- **Consistencia y concurrencia:** la observabilidad facilita identificar reservas pendientes y posibles discrepancias entre Redis y PostgreSQL.
- **Seguridad:** los registros y endpoints de diagnóstico deben proteger la información sensible.
- **Mantenibilidad:** la arquitectura hexagonal, las pruebas automatizadas y los ADR facilitan la evolución del sistema.

## 8. Consideraciones de Implementación

Este documento define requisitos de calidad y no implica que todas las métricas, alertas o mecanismos de diagnóstico estén implementados actualmente.

La instrumentación concreta, el formato de los registros, los umbrales de alertas y las herramientas de visualización deberán definirse y validarse durante la implementación y las pruebas del microservicio.

Las métricas deberán utilizar etiquetas controladas para evitar cardinalidad excesiva. Por ejemplo, no se recomienda utilizar identificadores únicos de usuarios o reservas como etiquetas de métricas.

La detección automática de discrepancias entre Redis y PostgreSQL podrá requerir procesos adicionales de reconciliación, según las decisiones de arquitectura que se adopten.
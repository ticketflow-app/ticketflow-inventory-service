# Configuración técnica del microservicio Inventory

## 1. Propósito

Este documento define los criterios para organizar y administrar la configuración técnica del microservicio Inventory.

Su objetivo es separar los parámetros de ejecución de la lógica de negocio, facilitar la adaptación a diferentes entornos y establecer cómo se configuran las conexiones con las dependencias externas.

El documento describe los criterios de configuración y distingue los parámetros pendientes de aquellos que ya están definidos. No implica que todas las configuraciones descritas estén implementadas.

## 2. Alcance

La configuración técnica de Inventory comprende:

- Configuración general de Spring Boot.
- Parámetros de ejecución de Java y de la aplicación.
- Conexión con Redis.
- Conexión con PostgreSQL mediante Spring Data R2DBC.
- Configuración de Kafka.
- Gestión externa de parámetros y credenciales.
- Configuración de Spring Boot Actuator y Micrometer.
- Organización de perfiles y valores específicos de cada entorno.

Los requisitos de autenticación, autorización y protección de información sensible se encuentran documentados en `../atributos_calidad/04_seguridad.md`. Este documento no los redefine.

## 3. Configuración general de la aplicación

### 3.1. Tecnologías definidas

La configuración del servicio debe ser compatible con las siguientes tecnologías:

- Java 21.
- Spring Boot 4.1.1.
- Maven.
- Spring WebFlux para el procesamiento reactivo de solicitudes HTTP.
- Spring Data R2DBC para el acceso reactivo a PostgreSQL.
- Redis para gestionar la disponibilidad operativa.
- PostgreSQL para persistir las reservas.
- Kafka para la publicación de eventos contemplados en la arquitectura.
- Spring Boot Actuator y Micrometer para salud y métricas.

Las versiones y dependencias adicionales deben corresponder a las definidas y aprobadas para el proyecto.

### 3.2. Archivo de configuración

La configuración propia de Spring Boot puede mantenerse en `application.properties` o en el formato que se haya establecido para el proyecto.

El archivo debe contener los parámetros generales de la aplicación y las referencias a los valores específicos del entorno.

No deben incorporarse directamente valores de configuración que pertenezcan a otros entornos ni credenciales reales que deban mantenerse protegidas.

### 3.3. Puerto HTTP

El puerto HTTP debe configurarse de acuerdo con el entorno de ejecución.

El valor definitivo y la forma de proporcionarlo deben verificarse en la configuración del proyecto. No se establece un puerto específico en este documento mientras no esté confirmado.

## 4. Configuración por entorno

### 4.1. Separación de parámetros

La aplicación debe permitir diferenciar los parámetros generales de aquellos que dependen del entorno en el que se ejecuta.

Entre los parámetros que pueden depender del entorno se encuentran:

- Direcciones de los servicios externos.
- Puertos de conexión.
- Nombres de bases de datos.
- Parámetros de conexión.
- Credenciales proporcionadas externamente.
- Configuración de observabilidad.

La lista identifica categorías de configuración, no nombres definitivos de variables de entorno.

### 4.2. Perfiles de Spring Boot

Los perfiles de Spring Boot pueden utilizarse cuando sea necesario mantener configuraciones diferentes para distintos entornos.

La definición de perfiles concretos, sus nombres y sus archivos asociados debe ajustarse a las necesidades reales del proyecto.

No se presupone que existan perfiles como `dev`, `test` o `prod` hasta verificar que estén definidos.

### 4.3. Valores por defecto

Los valores por defecto deben utilizarse únicamente cuando sean seguros y apropiados para el parámetro correspondiente.

Las configuraciones obligatorias para el funcionamiento de Inventory no deben sustituirse silenciosamente por valores que puedan ocasionar conexiones incorrectas o comportamientos inesperados.

La aplicación debe detectar los parámetros obligatorios ausentes durante el proceso de inicialización, cuando corresponda.

## 5. Configuración de Redis

### 5.1. Propósito

Redis es la fuente de verdad operativa para la disponibilidad en tiempo real de las entradas.

Inventory debe disponer de los parámetros necesarios para conectarse a Redis y ejecutar las operaciones requeridas por los casos de uso de disponibilidad y reservas.

### 5.2. Parámetros de configuración

La configuración de Redis debe contemplar los parámetros que requiera el cliente aprobado, entre ellos:

- Dirección del servicio.
- Puerto de conexión.
- Credenciales, si son necesarias.
- Parámetros de conexión segura, cuando correspondan.
- Tiempos de espera.
- Configuración de conexión y recuperación ante fallos.

Los nombres de las propiedades, sus valores y la configuración concreta del cliente deben verificarse en el proyecto antes de documentarse como definitivos.

### 5.3. Configuración de disponibilidad

La estructura de las claves de disponibilidad definida para Inventory es:

`availability:{event_id}:{section_id}`

La configuración técnica debe ser compatible con esta convención y con las operaciones de Redis establecidas para gestionar la disponibilidad.

Las reglas de inicialización, los límites de las cantidades y el mecanismo de actualización atómica corresponden al diseño de disponibilidad y a los ADR aplicables, no a una variable de configuración inventada en este documento.

## 6. Configuración de PostgreSQL

### 6.1. Propósito

PostgreSQL almacena la información persistente de las reservas de Inventory.

El acceso se contempla mediante Spring Data R2DBC, manteniendo la compatibilidad con el enfoque reactivo de la aplicación.

### 6.2. Parámetros de conexión

La configuración debe proporcionar los datos que requiera el cliente R2DBC para establecer la conexión con PostgreSQL.

Según el entorno, pueden ser necesarios:

- Dirección del servidor.
- Puerto.
- Nombre de la base de datos.
- Usuario de conexión.
- Credencial de acceso.
- Parámetros de conexión segura.
- Tiempos de espera y límites de conexiones.

Estos elementos representan parámetros posibles. Los nombres concretos de las propiedades y las opciones efectivamente utilizadas deben verificarse en la configuración aprobada.

### 6.3. Persistencia y esquema

La configuración de PostgreSQL debe mantenerse alineada con el esquema de datos aprobado para las reservas.

La estrategia de creación y actualización del esquema, las herramientas de migración y sus parámetros de ejecución deben documentarse cuando estén definidos.

No se establece en este documento una herramienta específica de migración ni una estrategia de generación del esquema que no haya sido confirmada.

### 6.4. Conexiones y recursos

La configuración del acceso a PostgreSQL debe considerar la disponibilidad de conexiones y el consumo de recursos.

Los límites del conjunto de conexiones, los tiempos de espera y los demás parámetros de rendimiento deben establecerse a partir de los requisitos y las pruebas del servicio, en lugar de asumir valores arbitrarios.

## 7. Configuración de Kafka

### 7.1. Propósito

Kafka se contempla para publicar eventos relacionados con las reservas de Inventory.

Su configuración debe permitir que el adaptador de mensajería utilice el cliente aprobado y los parámetros requeridos por el entorno.

### 7.2. Parámetros de conexión

Según el cliente y la infraestructura utilizados, pueden requerirse parámetros como:

- Dirección de los brokers.
- Configuración de autenticación.
- Parámetros de protección de las comunicaciones.
- Tiempos de espera.
- Opciones de reintento.
- Configuración de publicación y confirmación de mensajes.

Los parámetros concretos deben definirse de acuerdo con las dependencias y el contrato de integración aprobados.

### 7.3. Topics y contratos

Los nombres de topics y los contratos de eventos deben definirse en la documentación de integración y mensajería.

Los nombres propuestos en `04_integracion_y_mensajeria.md` no deben considerarse valores definitivos hasta que se aprueben.

La configuración de Kafka no determina por sí sola cuándo una reserva está confirmada ni garantiza la entrega fiable de un evento. Esos comportamientos deben resolverse en el diseño de integración.

## 8. Configuración de observabilidad

### 8.1. Tecnologías contempladas

Inventory contempla:

- Spring Boot Actuator para capacidades de salud y administración.
- Micrometer para instrumentación y exposición de métricas.

La configuración debe ajustarse a las dependencias y capacidades efectivamente habilitadas en el proyecto.

### 8.2. Comprobaciones de salud

La aplicación debe permitir verificar su estado mediante los mecanismos de salud que se configuren.

La comprobación del estado de la aplicación y la comprobación de las dependencias externas son aspectos diferentes. La inclusión de verificaciones de Redis, PostgreSQL o Kafka debe responder a las necesidades operativas definidas para Inventory.

No se presupone que todas las dependencias deban bloquear la comprobación de salud ni que todas las comprobaciones estén implementadas.

### 8.3. Exposición de endpoints de administración

Los endpoints de Actuator que se habiliten deben corresponder a las necesidades de observabilidad del servicio.

Los endpoints expuestos, los datos disponibles y las restricciones de acceso deben establecerse en la configuración aprobada.

Los requisitos de seguridad aplicables se encuentran en `../atributos_calidad/04_seguridad.md`.

### 8.4. Métricas

La configuración de Micrometer debe permitir instrumentar las métricas que se definan para evaluar el funcionamiento de Inventory.

Los nombres de las métricas, sus etiquetas, los umbrales y los mecanismos de alerta deben especificarse en los documentos de observabilidad correspondientes.

No se consideran implementados por el hecho de estar enumerados en un documento de diseño.

## 9. Gestión de parámetros sensibles

Los parámetros sensibles necesarios para conectarse a los servicios externos deben suministrarse mediante mecanismos externos a la lógica de negocio.

La configuración debe evitar que los valores reales de credenciales y otros secretos queden incorporados directamente en el código fuente o expuestos en archivos versionados.

La forma concreta de suministrar estos valores dependerá del entorno de ejecución y de los mecanismos aprobados para el proyecto.

Este documento no prescribe una herramienta específica de gestión de secretos.

Los requisitos de protección de credenciales y comunicaciones se definen en `../atributos_calidad/04_seguridad.md`.

## 10. Configuración y arquitectura hexagonal

La configuración técnica debe respetar la separación de responsabilidades de la arquitectura hexagonal.

- **Dominio:** no debe depender de variables de entorno, archivos de configuración ni clientes de infraestructura.
- **Aplicación:** coordina los casos de uso mediante los puertos definidos.
- **Infraestructura:** configura los adaptadores y las conexiones con Redis, PostgreSQL y Kafka.
- **Configuración de Spring:** proporciona los parámetros y construye los componentes necesarios para ejecutar la aplicación.

La ubicación exacta de las clases de configuración y la organización de los paquetes deben ajustarse a la estructura aprobada del código.

## 11. Verificación de la configuración

La configuración debe verificarse antes de considerar que el microservicio está preparado para ejecutarse en un entorno determinado.

La verificación debe contemplar:

1. Compatibilidad con Java 21 y Spring Boot 4.1.1.
2. Disponibilidad de los parámetros obligatorios.
3. Inicialización correcta de la aplicación.
4. Conectividad con Redis cuando la operación requiera esta dependencia.
5. Conectividad con PostgreSQL cuando la operación requiera persistencia.
6. Configuración de Kafka cuando se habilite la publicación de eventos.
7. Funcionamiento de los mecanismos de salud y métricas que estén habilitados.
8. Ausencia de credenciales reales incorporadas directamente en el código fuente.
9. Comportamiento definido cuando una configuración obligatoria es inválida o falta.

Las verificaciones deben convertirse en pruebas o procedimientos reproducibles cuando formen parte de los criterios de aceptación del proyecto.

## 12. Decisiones pendientes

Los siguientes aspectos deben confirmarse a partir de la configuración real y las decisiones aprobadas:

1. Puerto HTTP definitivo.
2. Archivos y perfiles de Spring Boot que se utilizarán.
3. Nombres definitivos de las propiedades y variables de entorno.
4. Cliente y parámetros concretos de conexión a Redis.
5. Parámetros R2DBC y estrategia de migración del esquema PostgreSQL.
6. Cliente, brokers y parámetros concretos de Kafka.
7. Topics y configuración de publicación correspondientes al contrato de mensajería.
8. Endpoints de Actuator que estarán habilitados.
9. Métricas que se expondrán y mecanismos de recolección.
10. Procedimiento para proporcionar los parámetros sensibles en cada entorno.

Estos aspectos deben documentarse cuando estén confirmados. No deben presentarse como implementados ni aprobados mientras sigan pendientes.

## 13. Documentos relacionados

Este documento debe mantenerse alineado con:

- `01_arquitectura_hexagonal.md`
- `02_componentes_y_responsabilidades.md`
- `03_flujos_de_negocio.md`
- `04_integracion_y_mensajeria.md`
- `../atributos_calidad/04_seguridad.md`
- `../atributos_calidad/05_observabilidad_y_mantenibilidad.md`

Las decisiones relativas a Redis, PostgreSQL y la consistencia entre ambos sistemas deben respetar los ADR correspondientes, especialmente los ADR-003, ADR-004 y ADR-007.
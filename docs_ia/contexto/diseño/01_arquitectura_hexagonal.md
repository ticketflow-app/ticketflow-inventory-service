# Arquitectura Hexagonal - Microservicio Inventory

## 1. Propósito

Este documento define la estructura arquitectónica del microservicio Inventory mediante el enfoque de arquitectura hexagonal, también conocido como Ports and Adapters.

El objetivo es separar las reglas de negocio de los detalles técnicos de infraestructura, facilitando las pruebas, el mantenimiento y la evolución del sistema.

La arquitectura deberá permitir modificar los mecanismos de persistencia, comunicación y exposición de servicios sin introducir dependencias innecesarias en el dominio.

## 2. Estilo Arquitectónico

El microservicio utilizará una arquitectura hexagonal organizada en tres capas principales:

- **Domain:** contiene las entidades, los conceptos y las reglas fundamentales del negocio.
- **Application:** coordina los casos de uso y define los contratos necesarios para interactuar con el exterior.
- **Infrastructure:** implementa los mecanismos técnicos para exponer la API REST y comunicarse con Redis, PostgreSQL y Kafka.

La separación entre estas capas deberá mantenerse durante la implementación.

## 3. Estructura de Capas

### 3.1. Domain

La capa de dominio representa las reglas de negocio relacionadas con la disponibilidad del inventario y la gestión de reservas.

Sus responsabilidades incluyen:

- Definir las entidades y los objetos de dominio.
- Representar los estados válidos de una reserva.
- Establecer las reglas de negocio relacionadas con las cantidades y la disponibilidad.
- Evitar que se creen reservas con cantidades inválidas.
- Definir las condiciones de negocio para cancelar una reserva.

Esta capa no deberá depender directamente de Spring Boot, Redis, PostgreSQL, Kafka ni de otros detalles de infraestructura.

Las entidades del dominio no deberán incluir anotaciones de persistencia o de exposición HTTP.

### 3.2. Application

La capa de aplicación coordina los casos de uso del microservicio y establece la interacción entre el dominio y los sistemas externos.

Sus responsabilidades incluyen:

- Coordinar la creación de reservas.
- Consultar la disponibilidad del inventario.
- Consultar las reservas de un usuario.
- Coordinar la cancelación de reservas.
- Definir los puertos necesarios para acceder a la persistencia y a los servicios externos.
- Coordinar las operaciones de idempotencia, concurrencia y compensación según las decisiones arquitectónicas aprobadas.

La capa de aplicación no deberá depender de implementaciones concretas de Redis, PostgreSQL o Kafka.

Cuando un caso de uso necesite una operación externa, deberá utilizar un puerto cuya implementación corresponda a la capa de infraestructura.

### 3.3. Infrastructure

La capa de infraestructura contiene las implementaciones técnicas necesarias para conectar Inventory con sus clientes y dependencias externas.

Sus responsabilidades incluyen:

- Exponer los endpoints REST mediante Spring WebFlux.
- Validar y transformar las solicitudes HTTP en datos apropiados para los casos de uso.
- Implementar los adaptadores para Redis.
- Implementar los adaptadores de persistencia mediante Spring Data R2DBC y PostgreSQL.
- Implementar la integración con Kafka.
- Configurar la validación local de JWT.
- Configurar las métricas, los registros y los indicadores de salud.
- Administrar la configuración técnica de las dependencias.

Los controladores REST y los adaptadores de persistencia no deberán contener las reglas principales del negocio.

## 4. Reglas de Dependencia

La arquitectura deberá respetar las siguientes reglas:

1. El dominio no dependerá de las capas de aplicación o infraestructura.
2. La aplicación podrá depender del dominio y de las abstracciones que necesite para ejecutar sus casos de uso.
3. La infraestructura podrá depender de la aplicación y del dominio para implementar los adaptadores correspondientes.
4. Las interfaces de los puertos deberán ubicarse en la capa que define la necesidad de la operación, siguiendo el criterio arquitectónico adoptado para el proyecto.
5. Los casos de uso no deberán utilizar directamente clientes concretos de Redis, repositorios específicos de R2DBC ni productores de Kafka.
6. Las dependencias técnicas deberán configurarse fuera de las reglas de negocio.

La dirección general de las dependencias será hacia el dominio, no desde el dominio hacia las tecnologías externas.

## 5. Puertos y Adaptadores

### 5.1. Puertos

Los puertos representan contratos mediante los cuales la aplicación define cómo se ejecutan sus casos de uso o cómo accede a capacidades externas.

Se distinguen dos categorías:

- **Puertos de entrada:** representan las operaciones que pueden invocar los adaptadores de entrada, como la creación de una reserva o la consulta de disponibilidad.
- **Puertos de salida:** representan las capacidades externas que necesita la aplicación, como consultar o actualizar la disponibilidad, guardar reservas y publicar eventos.

Los nombres definitivos de las interfaces y su ubicación en los paquetes deberán establecerse durante el diseño detallado.

### 5.2. Adaptadores de entrada

Los adaptadores de entrada reciben solicitudes externas y las traducen al contrato correspondiente de la aplicación.

El adaptador REST deberá:

- Recibir las solicitudes HTTP.
- Validar su estructura y los datos de entrada.
- Invocar el caso de uso correspondiente.
- Transformar el resultado en una respuesta HTTP.
- Gestionar los errores mediante respuestas coherentes con el contrato de la API.

La autenticación y la autorización deberán integrarse mediante los mecanismos de seguridad de la infraestructura, sin trasladar detalles de Spring Security al dominio.

### 5.3. Adaptadores de salida

Los adaptadores de salida implementan los puertos que permiten a la aplicación comunicarse con sistemas externos.

Se contemplan los siguientes:

- **Redis:** gestionar las operaciones atómicas relacionadas con la disponibilidad.
- **PostgreSQL:** persistir y consultar las reservas.
- **Kafka:** publicar eventos de negocio cuando corresponda.
- **Observabilidad:** proporcionar métricas, registros y verificaciones de salud mediante los mecanismos técnicos configurados.

La lógica de cada adaptador deberá respetar el contrato definido por su puerto y evitar introducir decisiones de negocio que correspondan al dominio o a la aplicación.

## 6. Organización Lógica de Paquetes

La organización propuesta es la siguiente:

```text
com.inventory.service
├── domain
│   ├── model
│   ├── exception
│   └── rule
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   └── usecase
└── infrastructure
    ├── adapter
    │   ├── in
    │   │   └── web
    │   └── out
    │       ├── redis
    │       ├── persistence
    │       └── messaging
    └── config
```

Esta estructura es una propuesta de organización lógica, no una afirmación de que todos los paquetes o componentes ya existan.

Los paquetes podrán ajustarse durante el diseño detallado, siempre que se conserven las responsabilidades y las reglas de dependencia establecidas.

## 7. Integración con las Tecnologías

La arquitectura deberá separar las decisiones de negocio de los mecanismos técnicos utilizados por cada dependencia.

| Tecnología | Responsabilidad arquitectónica |
|---|---|
| Spring Boot y Spring WebFlux | Exposición reactiva de la API REST y configuración de la aplicación. |
| Redis | Gestión atómica de la disponibilidad del inventario. |
| PostgreSQL y Spring Data R2DBC | Persistencia reactiva de las reservas. |
| Kafka | Publicación de eventos relacionados con las reservas. |
| Spring Security y JWT | Autenticación y autorización en la infraestructura. |
| Micrometer y Spring Boot Actuator | Métricas, salud y observabilidad. |
| Resilience4j | Mecanismos de resiliencia configurados para las operaciones que lo requieran. |

El uso de estas tecnologías no deberá alterar las reglas de dependencia entre las capas.

## 8. Consideraciones para la Programación Reactiva

El microservicio utilizará Spring WebFlux y programación reactiva para atender las solicitudes de la API.

Las operaciones de entrada y salida que utilicen las bibliotecas reactivas deberán conservar la composición reactiva de sus flujos, evitando bloqueos innecesarios.

Se deberán evaluar especialmente:

- El uso de operaciones reactivas para acceder a PostgreSQL.
- La compatibilidad de los clientes utilizados para Redis y Kafka con el flujo de ejecución.
- La propagación de errores en las cadenas reactivas.
- La coordinación de operaciones que afectan simultáneamente Redis y PostgreSQL.
- La ejecución de tareas que no dispongan de una API reactiva.

La programación reactiva no garantiza por sí sola la consistencia entre Redis y PostgreSQL. Las operaciones que involucren ambas dependencias deberán respetar los mecanismos de compensación y recuperación definidos por el diseño.

## 9. Estrategia de Pruebas por Capa

La separación arquitectónica deberá facilitar pruebas específicas para cada nivel.

### 9.1. Dominio

Las pruebas unitarias verificarán las reglas de negocio, los estados de las reservas y las restricciones sobre las cantidades.

### 9.2. Aplicación

Las pruebas de los casos de uso utilizarán implementaciones controladas de los puertos para verificar la coordinación de operaciones, los resultados y el manejo de errores.

### 9.3. Infraestructura

Las pruebas de integración verificarán los adaptadores REST, Redis, PostgreSQL y Kafka, así como los mecanismos de seguridad y observabilidad que se implementen.

### 9.4. Integración completa

Las pruebas de extremo a extremo comprobarán los flujos principales y su comportamiento cuando ocurran fallos o solicitudes concurrentes.

## 10. Restricciones Arquitectónicas

Durante la implementación deberán respetarse las siguientes restricciones:

- No incorporar anotaciones de Spring o de persistencia en las entidades del dominio.
- No ejecutar consultas a Redis o PostgreSQL directamente desde los controladores REST.
- No incorporar detalles HTTP en las reglas de negocio.
- No acceder directamente a clientes de Kafka desde los casos de uso.
- No permitir que los casos de uso dependan de implementaciones concretas de infraestructura.
- No introducir nuevas dependencias o tecnologías sin la autorización correspondiente.
- Documentar mediante ADR las decisiones arquitectónicas relevantes que modifiquen las definiciones aprobadas.

## 11. Decisiones Pendientes

Los siguientes aspectos deberán concretarse antes o durante la implementación detallada:

- Los nombres definitivos de los casos de uso, puertos y adaptadores.
- La estrategia exacta de coordinación y compensación entre Redis y PostgreSQL.
- El mecanismo de publicación fiable de eventos en Kafka, si los requisitos exigen garantizar su entrega.
- Los clientes concretos que se utilizarán para Redis y Kafka y su integración con el flujo reactivo.
- La estrategia de pruebas de arquitectura que verificará las reglas de dependencia.

Estas decisiones deberán ser coherentes con los ADR existentes y con los requisitos de calidad definidos para Inventory.

## 12. Criterios de Aceptación

La arquitectura se considerará conforme con este documento cuando:

- Las capas y sus responsabilidades estén claramente separadas.
- Las dependencias respeten la dirección hacia el dominio.
- Los casos de uso puedan probarse sin depender obligatoriamente de Redis, PostgreSQL o Kafka reales.
- Los adaptadores implementen los contratos definidos por la aplicación.
- Las reglas de negocio no dependan de detalles de Spring ni de persistencia.
- La estructura del proyecto permita identificar claramente los componentes de cada capa.
- Las decisiones pendientes se resuelvan sin contradecir los requisitos y ADR aprobados.
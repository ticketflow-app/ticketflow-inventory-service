# Arquitectura y configuración del microservicio Inventory

## 1. Identificación del proyecto

El microservicio se denomina:

```text
inventory
```

El proyecto fue creado utilizando:

* **Build tool:** Maven
* **Language:** Java
* **Java version:** 21
* **Spring Boot:** 4.1.1
* **Packaging:** JAR
* **Configuration format:** Properties
* **Group:** `com.inventory`
* **Artifact:** `service`

El archivo principal de configuración será:

```text
src/main/resources/application.properties
```

---

# 2. Arquitectura

El microservicio utiliza **arquitectura hexagonal (Ports and Adapters)**.

La arquitectura debe separar:

```text
Domain
   ↓
Application
   ↓
Infrastructure
```

Las dependencias deben apuntar hacia el dominio y la lógica de aplicación, evitando que el dominio dependa directamente de tecnologías externas.

---

# 3. Estructura de carpetas

La estructura inicial propuesta es:

```text
inventory/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── inventory/
    │   │           └── service/
    │   │               ├── application/
    │   │               │   ├── port/
    │   │               │   │   ├── in/
    │   │               │   │   └── out/
    │   │               │   └── usecase/
    │   │               │
    │   │               ├── domain/
    │   │               │   ├── model/
    │   │               │   ├── exception/
    │   │               │   └── event/
    │   │               │
    │   │               └── infrastructure/
    │   │                   ├── adapter/
    │   │                   │   ├── in/
    │   │                   │   │   └── controller/
    │   │                   │   │
    │   │                   │   └── out/
    │   │                   │       ├── redis/
    │   │                   │       ├── persistence/
    │   │                   │       └── messaging/
    │   │                   │
    │   │                   ├── config/
    │   │                   ├── security/
    │   │                   └── handler/
    │   │
    │   └── resources/
    │       └── application.properties/
    │
    └── test/
```

> Nota: `application.properties` es un archivo, no una carpeta.

---

# 4. Responsabilidad de cada capa

## 4.1 Application

Contiene los casos de uso y las interfaces que permiten comunicarse con el exterior.

### `application/port/in`

Define las operaciones que puede solicitar el exterior.

Ejemplos:

* `GetAvailabilityUseCase`
* `ReserveTicketsUseCase`
* `ConfirmReservationUseCase`
* `ReleaseReservationUseCase`

### `application/port/out`

Define las dependencias externas que necesita la aplicación.

Ejemplos:

* Puerto para consultar/modificar disponibilidad en Redis.
* Puerto para persistir reservas.
* Puerto para publicar eventos.

### `application/usecase`

Contiene las implementaciones de los casos de uso.

---

# 5. Domain

Es el núcleo del negocio.

No debe depender directamente de:

* Spring WebFlux
* Redis
* PostgreSQL
* Kafka
* HTTP
* Spring Security

## `domain/model`

Contiene los conceptos principales del dominio.

Ejemplos:

* `Inventory`
* `Reservation`
* `TicketAvailability`

## `domain/exception`

Excepciones relacionadas con reglas del dominio.

Ejemplos:

* `InsufficientAvailabilityException`
* `ReservationNotFoundException`

## `domain/event`

Eventos relevantes del dominio.

Ejemplo:

* `ReservationConfirmedEvent`

No se deben crear clases adicionales en `domain` sin una necesidad real del dominio.

---

# 6. Infrastructure

Contiene las implementaciones tecnológicas.

## `adapter/in/controller`

Recibe las solicitudes HTTP mediante Spring WebFlux.

Flujo:

```text
HTTP Request
     ↓
Controller
     ↓
Input Port
     ↓
Use Case
```

## `adapter/out/redis`

Implementa la comunicación con Redis.

Redis será utilizado para:

* Consultar disponibilidad.
* Mantener contadores de tickets.
* Ejecutar operaciones atómicas.
* Manejar alta concurrencia.

La comunicación debe ser reactiva.

## `adapter/out/persistence`

Implementa la persistencia de las reservas en PostgreSQL mediante R2DBC.

PostgreSQL será utilizado como almacenamiento persistente.

## `adapter/out/messaging`

Implementa la comunicación mediante Kafka para publicar eventos de reservas confirmadas.

## `config`

Contiene configuraciones técnicas de Spring Boot y las tecnologías utilizadas.

## `security`

Contiene la configuración de Spring Security y la validación de JWT.

## `handler`

Contiene el manejo global de errores y excepciones HTTP.

---

# 7. Tecnologías

| Tecnología                     | ¿Para qué?                                              | Prioridad |
| ------------------------------ | ------------------------------------------------------- | --------: |
| **Spring Boot**                | Base para construir y ejecutar el microservicio         |     ⭐⭐⭐⭐⭐ |
| **Spring WebFlux**             | Manejar alta concurrencia de forma no bloqueante        |     ⭐⭐⭐⭐⭐ |
| **Redis**                      | Disponibilidad y operaciones atómicas                   |     ⭐⭐⭐⭐⭐ |
| **Spring Data Redis Reactive** | Conectar WebFlux con Redis de forma reactiva            |     ⭐⭐⭐⭐⭐ |
| **PostgreSQL**                 | Persistir reservas                                      |     ⭐⭐⭐⭐⭐ |
| **Spring Data R2DBC**          | Acceso reactivo a PostgreSQL                            |      ⭐⭐⭐⭐ |
| **JWT / Spring Security**      | Autenticación y autorización                            |      ⭐⭐⭐⭐ |
| **Resilience4j**               | Reintentos y tolerancia a fallos                        |      ⭐⭐⭐⭐ |
| **Kafka**                      | Eventos de reservas confirmadas                         |       ⭐⭐⭐ |
| **Actuator + Micrometer**      | Métricas y observabilidad                               |      ⭐⭐⭐⭐ |
| **Docker**                     | Ejecución reproducible del microservicio y dependencias |      ⭐⭐⭐⭐ |

---

# 8. Dependencias principales

Las siguientes dependencias deben evaluarse y utilizarse cuando correspondan a la implementación definida:

| Dependencia                               | Propósito                                        | Prioridad |
| ----------------------------------------- | ------------------------------------------------ | --------: |
| `spring-boot-starter-webflux`             | API reactiva y no bloqueante                     |     ⭐⭐⭐⭐⭐ |
| `spring-boot-starter-data-redis-reactive` | Integración reactiva con Redis                   |     ⭐⭐⭐⭐⭐ |
| `spring-boot-starter-data-r2dbc`          | Persistencia reactiva                            |     ⭐⭐⭐⭐⭐ |
| `r2dbc-postgresql`                        | Driver reactivo de PostgreSQL                    |     ⭐⭐⭐⭐⭐ |
| `spring-boot-starter-security`            | Seguridad                                        |      ⭐⭐⭐⭐ |
| `spring-boot-starter-validation`          | Validación de requests                           |      ⭐⭐⭐⭐ |
| `spring-boot-starter-actuator`            | Health checks y métricas                         |      ⭐⭐⭐⭐ |
| `spring-boot-starter-test`                | Pruebas                                          |     ⭐⭐⭐⭐⭐ |
| `spring-boot-starter-kafka`               | Comunicación mediante Kafka                      |       ⭐⭐⭐ |
| `resilience4j-spring-boot3`               | Integración de Resilience4j con Spring Boot      |      ⭐⭐⭐⭐ |
| `resilience4j-reactor`                    | Operaciones de Resilience4j sobre flujos Reactor |      ⭐⭐⭐⭐ |

Las versiones de las dependencias deben ser gestionadas por Spring Boot cuando sea posible, evitando declarar versiones manualmente innecesarias.

---

# 9. Dependencias potenciales

Estas dependencias no deben agregarse automáticamente. Solo deben incorporarse si una necesidad concreta del proyecto las justifica.

### OpenAPI

Puede utilizarse para documentar los endpoints de la API.

Posible tecnología:

```text
springdoc-openapi
```

### Testcontainers

Puede utilizarse para pruebas de integración utilizando instancias reales de:

* PostgreSQL
* Redis
* Kafka

Es especialmente útil para evitar que las pruebas dependan de servicios instalados manualmente.

### Lombok

Puede reducir código repetitivo, pero su utilización es opcional y debe evaluarse frente a mantener el código explícito.

---

# 10. Principios de implementación

## Reactividad

El flujo crítico de Inventory debe mantenerse no bloqueante:

```text
WebFlux
   ↓
Reactive Redis
   ↓
R2DBC
   ↓
PostgreSQL
```

No introducir operaciones bloqueantes dentro del flujo reactivo sin una justificación técnica.

## Redis

Redis es el componente utilizado para controlar la disponibilidad durante operaciones concurrentes.

La disponibilidad debe manejarse mediante operaciones atómicas para evitar overselling.

## PostgreSQL

PostgreSQL mantiene la persistencia definitiva de las reservas.

## Kafka

Kafka permite publicar eventos relacionados con reservas confirmadas.

## Seguridad

Los endpoints protegidos deben validar JWT mediante Spring Security.

## Resiliencia

Las operaciones susceptibles a fallos de infraestructura deben utilizar mecanismos de resiliencia cuando estén justificados.

---

# 11. Observabilidad

El microservicio debe exponer información mediante:

* Spring Boot Actuator.
* Micrometer.
* Métricas de latencia.
* Métricas de solicitudes.
* Tasa de errores.
* Uso de recursos.
* Health checks.

Estas métricas serán utilizadas posteriormente para evaluar los atributos de calidad definidos para el proyecto.

---

# 12. Docker

El microservicio debe poder ejecutarse mediante Docker junto con las dependencias necesarias para desarrollo local.

Inicialmente se consideran:

```text
Inventory
Redis
PostgreSQL
Kafka
```

La configuración concreta de Docker debe mantenerse separada del dominio y de los casos de uso.

---
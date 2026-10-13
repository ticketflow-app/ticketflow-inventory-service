# Vista General de Arquitectura - Microservicio Inventory

## 1. Identificación y Configuración del Proyecto

- **Nombre del Microservicio:** `inventory`
- **Group:** `com.inventory`
- **Artifact:** `service`
- **Packaging:** JAR
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 4.1.1 (Reactivo)
- **Gestor de Dependencias:** Maven
- **Formato de Configuración:** `src/main/resources/application.properties`

---

## 2. Estilo Arquitectónico y Principios

El microservicio implementa una **Arquitectura Hexagonal (Ports and Adapters)** en combinación con un paradigma **Reactivo No Bloqueante (Non-blocking I/O)**.

### Regla de Dependencia

```text
Infrastructure  ──>  Application  ──>  Domain
```

- **Domain (Núcleo):** Aislado por completo. No depende de Spring, WebFlux, R2DBC, Redis, Kafka, JPA ni de ninguna tecnología de infraestructura.
- **Application:** Contiene los casos de uso y define las interfaces (puertos) de entrada y salida.
- **Infrastructure:** Contiene los adaptadores tecnológicos (WebFlux controllers, clientes reactivos de Redis/R2DBC, productores Kafka, seguridad).

## 3. Estructura de Carpetas del Proyecto

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
    │   │               │   │   ├── in/        # Interfaces de casos de uso (Ej: ReserveTicketsUseCase)
    │   │               │   │   └── out/       # Interfaces hacia infra (Ej: InventoryRepositoryPort)
    │   │               │   └── usecase/       # Implementaciones de la lógica de aplicación
    │   │               │
    │   │               ├── domain/
    │   │               │   ├── model/         # Agregados y entidades (Inventory, Reservation)
    │   │               │   ├── exception/     # Excepciones de negocio en inglés
    │   │               │   └── event/         # Eventos de dominio (ReservationConfirmedEvent)
    │   │               │
    │   │               └── infrastructure/
    │   │                   ├── adapter/
    │   │                   │   ├── in/
    │   │                   │   │   └── controller/ # Controladores Spring WebFlux
    │   │                   │   └── out/
    │   │                   │       ├── redis/       # Adaptador reactivo Redis
    │   │                   │       ├── persistence/ # Adaptador R2DBC PostgreSQL
    │   │                   │       └── messaging/   # Adaptador Kafka
    │   │                   ├── config/        # Configuraciones de Spring/Beans
    │   │                   ├── security/      # Spring Security & JWT
    │   │                   └── handler/       # Global Error Handler
    │   └── resources/
    │       └── application.properties
    └── test/
```

## 4. Responsabilidades por Capa

### 4.1 Capa de Aplicación (`application`)

- **`port/in`**: Define los contratos que el exterior puede invocar:
  - `GetAvailabilityUseCase`
  - `ReserveTicketsUseCase`
  - `ConfirmReservationUseCase`
  - `ReleaseReservationUseCase`

- **`port/out`**: Interfaces para comunicarse con componentes externos:
  - Puerto de caché/operaciones atómicas de disponibilidad (Redis).
  - Puerto de persistencia histórica de reservas (PostgreSQL R2DBC).
  - Puerto de publicación de eventos de dominio (Kafka).

- **`usecase`**: Contiene la orquestación de la lógica sin acoplamiento a tecnologías específicas.

### 4.2 Capa de Dominio (`domain`)

- **`model`**: Modelos puramente Java representando las invariantes del inventario y reservas (`Inventory`, `Reservation`, `TicketAvailability`).
- **`exception`**: Excepciones explícitas de negocio (`InsufficientAvailabilityException`, `ReservationNotFoundException`).
- **`event`**: Eventos significativos del dominio (`ReservationConfirmedEvent`).

### 4.3 Capa de Infraestructura (`infrastructure`)

- **`adapter/in/controller`**: Expone los endpoints REST reactivos mediante WebFlux.
- **`adapter/out/redis`**: Conector reactivo para operaciones atómicas sobre contadores de stock.
- **`adapter/out/persistence`**: Persistencia asíncrona no bloqueante con Spring Data R2DBC hacia PostgreSQL.
- **`adapter/out/messaging`**: Productor asíncrono para publicar eventos en Apache Kafka.
- **`security`**: Validación descentralizada de tokens JWT.
- **`handler`**: Traducción de excepciones de dominio a respuestas HTTP estructuradas (RFC 7807).

## 5. Reglas de Gobierno Arquitectónico (Inviolables)

1. **Propiedad de los Datos:** El microservicio de Inventario es el único dueño y responsable de la disponibilidad y reservas. Ningún otro servicio modifica su base de datos ni su instancia de Redis.
2. **Independencia del Dominio:** La lógica del negocio (`domain/`) no implementa anotaciones de Spring, JPA, WebFlux ni referencias a bases de datos o Kafka.
3. **No Lógica en Controladores:** La lógica de negocio reside únicamente en los casos de uso; los controladores WebFlux solo adaptan la petición HTTP hacia un puerto de entrada.
4. **Acceso por Puertos:** Toda persistencia y comunicación externa se hace invocando un `port/out`.
5. **Autenticación Descentralizada:** Se verifica la firma del JWT enviado por el cliente localmente mediante la clave pública/secreta compartida sin consultar al servicio de Usuarios.
6. **Métricas en Operaciones Críticas:** Las reservas y consultas de disponibilidad exponen métricas operativas hacia Micrometer para evaluar rendimiento.

## 6. Decisiones Arquitectónicas Aplicables (ADRs)

- **ADR-001 (Microservicios Independientes):** Inventario opera como un microservicio desacoplado capaz de escalar de forma independiente según la carga.
- **ADR-002 (Arquitectura Hexagonal):** Aísla el dominio core de las tecnologías y frameworks externos.
- **ADR-005 (Autenticación mediante JWT):** Evita el acoplamiento síncrono con el servicio de Usuarios durante la creación de reservas.
- **ADR-006 (Separación Catálogo/Inventario):** Catálogo gestiona datos estáticos (eventos, precios), mientras que Inventario maneja la alta volatilidad de la disponibilidad en tiempo real.

## 7. Tecnologías Fuera del Alcance Inicial (Fase 1)

Para mantener el foco del desarrollo y no sobrecargar el alcance del prototipo, se excluyen de esta fase:

- Kubernetes y Service Mesh.
- Integración real con pasarelas de pago y facturación.
- Notificaciones (Email/SMS) y aplicaciones móviles.
- Búsqueda compleja con Elasticsearch.
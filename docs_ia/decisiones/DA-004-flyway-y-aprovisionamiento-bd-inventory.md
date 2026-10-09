# DA-004

## ID

DA-004

## Título

Gestión del esquema de PostgreSQL y aprovisionamiento de la base de datos `inventory` (Flyway dentro del microservicio)

## Contexto

Información definida en el proyecto (`docs_ia/contexto`):

- `arquitectura/arquitectura_mircroservicio_inventario.md` §12: el microservicio debe poder ejecutarse con Docker junto con Redis, PostgreSQL y Kafka.
- §7–§8: tecnologías/dependencias principales incluyen Spring Data R2DBC y PostgreSQL. §10: el flujo crítico debe mantenerse no bloqueante.
- El contexto **no define** ninguna herramienta de migración de esquema (Flyway, Liquibase) ni auto‑DDL.

Estado verificado del repositorio (no definido en el contexto):

- `pom.xml`: **no** incluye Flyway ni starter JDBC; sí `r2dbc-postgresql` y `org.postgresql:postgresql` (JDBC, scope `runtime`).
- `application.properties`: `spring.r2dbc.url=r2dbc:postgresql://localhost:5432/inventory` (base de datos `inventory`).
- `ReservationPersistenceAdapter` ejecuta SQL nativo `INSERT INTO reservations (event_id, locality, quantity, status)`; **no existe ningún DDL** que cree la tabla `reservations`.
- No existe `schema.sql`, ni `src/main/resources/db/migration`, ni configuración de Flyway.

Estado verificado de otros repositorios:

- `user-service` **no usa Flyway**: usa `spring.jpa.hibernate.ddl-auto=update` (auto‑DDL de Hibernate) y **no** tiene migraciones (`AGENTS.md`: "Flyway está planeado, no existe").
- El `docker-compose.yml` del gateway (cambio no aprobado en el contexto) crea la BD `inventory` con `postgres-init/01-create-inventory-db.sql`, un archivo alojado en el **repo del gateway**.

## Problema

1. R2DBC no crea el esquema: sin DDL, la tabla `reservations` no existe y `POST /reservations` falla en runtime.
2. La base de datos `inventory` debe existir **antes** de que R2DBC/Flyway se conecten. **Flyway crea esquemas y tablas, no bases de datos**, por lo que Flyway por sí solo no elimina la necesidad de aprovisionar la BD.
3. El aprovisionamiento actual vive en el repo del gateway (contexto ajeno); el usuario lo rechaza.
4. "Tal cual como en user-service" no es literalmente posible: user-service usa el auto‑DDL de Hibernate (JPA), tecnología ausente en un servicio WebFlux/R2DBC. El equivalente funcional es una herramienta de migración gestionada desde el propio servicio.

## Alternativas consideradas

### Alternativa A — Flyway en `inventory-service` + servicio `postgres` propio para inventario (recomendada)

- Flyway (`flyway-core` + `flyway-database-postgresql`) dentro del microservicio, con migraciones en `src/main/resources/db/migration/`.
- Un servicio `postgres` dedicado a inventario en el compose, con `POSTGRES_DB: inventory` (igual que user-service obtiene `TicketFlow` de su `POSTGRES_DB`).
- Se elimina `postgres-init/` del gateway.

Ventajas: cada servicio tiene su BD creada por `POSTGRES_DB` (mismo patrón que user-service); sin scripts de init; aislamiento total entre bounded contexts.
Desventajas: un segundo contenedor PostgreSQL (más recursos en local).

### Alternativa B — Flyway + reutilizar la base de datos `TicketFlow`

- `spring.r2dbc.url` apunta a la BD `TicketFlow` ya existente; Flyway crea las tablas en el esquema `public`.
- No se crea BD nueva ni init.

Ventajas: sin contenedor extra; mínima infraestructura.
Desventajas: dos microservicios comparten una misma base de datos (mezcla de contextos); contradice `application.properties`/`AGENTS.md` (BD `inventory`); user-service gobernaría el mismo esquema con Hibernate.

### Alternativa C — Flyway + un solo postgres, con el script de init alojado en el repo de `inventory-service`

- El compose referencia `../ticketflow-inventory-service/docker/postgres/01-create-inventory-db.sql`, de modo que el aprovisionamiento vive en el repo de inventario, no en el gateway.
- Flyway crea las tablas.

Ventajas: un solo contenedor PostgreSQL; el script deja de estar en el gateway.
Desventajas: sigue existiendo un paso de init no gestionado por Flyway; el compose del gateway conoce una ruta del repo de inventario.

### Alternativa D — Sin dependencias nuevas: `schema.sql` de Spring Boot para R2DBC (en lugar de Flyway)

- Spring Boot soporta scripts SQL de inicialización para R2DBC (`spring.sql.init.*` + `schema.sql`), sin agregar dependencias.
- Requeriría igualmente resolver el aprovisionamiento de la BD (A/B/C).

Ventajas: cero dependencias nuevas.
Desventajas: sin versionado de migraciones ni historial; no es lo solicitado por el usuario.

## Recomendación

**Adoptar la Alternativa A.** Cambios propuestos (**aún no aplicados**):

**`pom.xml`**

- Dependencia `org.flywaydb:flyway-core` (versión gestionada por Spring Boot).
- Dependencia `org.flywaydb:flyway-database-postgresql` (soporte de PostgreSQL, requerido por Flyway 10+; versión gestionada por Spring Boot).
- No se agrega driver JDBC adicional: `org.postgresql:postgresql` ya está presente (scope `runtime`).

**Configuración (infraestructura)**

- Nueva clase de configuración (p. ej. `infrastructure/config/FlywayConfig.java`) que construye y ejecuta Flyway al arrancar (`Flyway.configure().dataSource(url, user, password)...load()` con `initMethod = "migrate"`), ya que un servicio R2DBC no expone un `DataSource` de Spring. Propiedades `spring.flyway.url` / `spring.flyway.user` / `spring.flyway.password` (JDBC) configurables por entorno.

**Migraciones**

- `src/main/resources/db/migration/V1__create_reservations.sql` con la tabla `reservations` (`id BIGSERIAL PK`, `event_id BIGINT`, `locality VARCHAR`, `quantity INTEGER`, `status VARCHAR`) según el SQL nativo existente.

**`docker-compose.yml` (gateway) — requiere autorización**

- Agregar servicio `postgres-inventory` con `POSTGRES_DB: inventory` (imagen `postgres:18-alpine`).
- Quitar el montaje `./postgres-init` y el directorio `postgres-init/`.
- Apuntar `inventory-service` a `postgres-inventory:5432` y a `spring.flyway.url=jdbc:postgresql://postgres-inventory:5432/inventory`.

**Actualización documental** (requiere autorización aparte)

- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §8: añadir Flyway como dependencia de migración.

## Ventajas

- El esquema y sus cambios quedan versionados y definidos **dentro del microservicio**, como pidió el usuario.
- Elimina el script de init del repo del gateway.
- Mantiene R2DBC como tecnología de acceso en runtime (Flyway solo se usa puntualmente al arrancar).

## Desventajas

- **Nuevas dependencias** y una clase de configuración técnica → requiere aprobación humana explícita (`.clinerules` §6).
- Flyway es **bloqueante**; se ejecuta una vez al arranque, fuera del flujo reactivo de las peticiones (impacto acotado).
- Un contenedor PostgreSQL adicional (Alternativa A).
- La autoconfiguración de Flyway de Spring Boot puede no activarse sin un `DataSource`; por eso se propone un bean explícito (verificar en la implementación).

## Impacto

- `pom.xml` (2 dependencias), `src/main/resources/application.properties` (propiedades Flyway) y nueva clase de config; nueva migración.
- `docker-compose.yml` + `postgres-init/` (gateway): creación de un postgres dedicado y retiro del init.
- No afecta al dominio ni a la capa de aplicación.
- No modifica decisiones previas (ADR-001..007, DA-001, DA-002, DA-003).

## Estado

APROBADA (2026-10-09)

> Aprobada explícitamente por el usuario, que eligió la **Alternativa A** (servicio `postgres` dedicado a inventario con `POSTGRES_DB: inventory`). Trazabilidad de la implementación en `docs_ia/prompts/P-008-implementacion-flyway.md`.

## Implementación (2026-10-09)

Cambios aplicados tras la aprobación:

- `pom.xml`: dependencias `org.flywaydb:flyway-core` y `org.flywaydb:flyway-database-postgresql` (versión `12.4.0`, gestionada por Spring Boot). No se usó `spring-boot-starter-flyway` porque arrastra `spring-boot-starter-jdbc` (un `DataSource` bloqueante que rompería el arranque reactivo sin `spring.datasource.url`).
- `src/main/resources/application.properties`: propiedades `spring.flyway.url/user/password` (JDBC, parametrizadas por entorno).
- `src/main/java/com/inventory/service/infrastructure/config/FlywayConfig.java`: bean `Flyway` con `initMethod = "migrate"` (conexión JDBC propia, porque Flyway no soporta R2DBC), condicionado a `spring.flyway.enabled` (activo por defecto).
- `src/main/resources/db/migration/V1__create_reservations.sql`: crea la tabla `reservations`.
- `src/test/java/com/inventory/service/ServiceApplicationTests.java`: `@TestPropertySource(properties = "spring.flyway.enabled=false")` para que el smoke test `contextLoads` no requiera una BD viva.
- `ticketflow-api-gateway/docker-compose.yml`: nuevo servicio `postgres-inventory` (`POSTGRES_DB: inventory`); `inventory-service` apunta a `postgres-inventory` (R2DBC y Flyway); **se eliminó** `postgres-init/`.

Verificado:

- `mvnw compile` y `mvnw test`: BUILD SUCCESS (1 test, 0 fallos).
- `dependency:tree`: `flyway-core:12.4.0` + `flyway-database-postgresql:12.4.0`; **sin** `HikariCP` ni `spring-boot-starter-jdbc`.
- `docker compose config`: válido.
- Migración aplicada contra un PostgreSQL 18 efímero: crea `reservations` (`id BIGSERIAL PK`, `event_id BIGINT`, `locality VARCHAR(255)`, `quantity INTEGER`, `status VARCHAR(50)`).

Pendiente (no autorizado en esta decisión):

- Actualizar `docs_ia/contexto` (§8) para incluir la dependencia de Flyway.

# P-010

## Identificador del prompt

P-010

## Fecha

2026-10-13

## Objetivo

Arreglar el fallo de arranque de `inventory-service` dentro del stack de Docker Compose: el contenedor no conectaba a PostgreSQL (`Connection to localhost:5432 refused`) y, tras corregir la conexión, fallaba la validación de esquema (`Schema validation: missing table [reservations]`) porque **Flyway no se ejecutaba**.

## Prompt completo utilizado

```text
"arregla este error del inventory service"

2026-10-13T23:51:15.963Z  WARN 1 --- [ticketflow-inventory-service] [           main] org.hibernate.orm.jdbc.error             : Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
... org.hibernate.exception.JDBCConnectionException: Unable to obtain isolated JDBC connection [Connection to localhost:5432 refused...]
```

## Archivos de contexto consultados

- `AGENTS.md` (raíz y del servicio) — gobernanza §4, §6, §7, §8, §10.
- `docs_ia/decisiones/DA-004` (Flyway y aprovisionamiento de la BD `inventory`).
- `docs_ia/decisiones/DA-005` (migración a MVC/JPA/Virtual Threads).
- `docs_ia/prompts/P-009` (migración aplicada, pendientes).

## Archivos del proyecto analizados

- `src/main/resources/application.properties` (datasource / JPA / virtual threads).
- `src/test/resources/application.properties`.
- `pom.xml` (dependencias Flyway).
- `target/…` y logs del contenedor `ticketflow-inventory-service` (`docker logs`).

## Archivos de otros repositorios analizados

- `ticketflow-api-gateway/docker-compose.yml` (variables de entorno de `inventory-service`).

## Resumen de la respuesta generada

Se diagnosticaron y corrigieron **dos** problemas encadenados:

1. **Conexión a PostgreSQL (`localhost:5432 refused`)**. El `docker-compose.yml` del gateway todavía inyectaba `SPRING_R2DBC_*` y `SPRING_FLYWAY_*` en `inventory-service`. Tras DA-005 el servicio ya no lee esas propiedades, así que caía al default `localhost:5432` (dentro del contenedor → refused) y, con `ddl-auto=validate`, no arrancaba. **Corrección:** reemplazar por `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` apuntando a `jdbc:postgresql://postgres-inventory:5432/inventory`.

2. **`Schema validation: missing table [reservations]`**. Con el `DataSource` ya correcto, el servicio conectaba pero **Flyway no se ejecutaba** (no aparecía ninguna línea de Flyway en los logs). Causa: en **Spring Boot 4 la autoconfiguración está modularizada**; `flyway-core` por sí solo no activa la autoconfiguración de Flyway. En DA-004 no se pudo agregar el starter porque el servicio era R2DBC y no tenía `DataSource`; **ahora sí lo tiene** (DA-005), por lo que la solución correcta es `spring-boot-starter-flyway`. **Corrección:** en `pom.xml`, `org.flywaydb:flyway-core` → `org.springframework.boot:spring-boot-starter-flyway` (se conserva `flyway-database-postgresql`). Flyway se autoconfigura desde el `DataSource` y se ejecuta antes de la validación de Hibernate (vía `FlywayJpaDependencyConfiguration`).

Verificado end-to-end: `inventory-service` arranca, Flyway aplica `V1`, se registra en Eureka y responde por el gateway con JWT válido.

## Propuestas realizadas por la IA

- Cambio de variables de entorno en `ticketflow-api-gateway/docker-compose.yml` (autorización aparte pendiente de DA-005).
- Sustituir `flyway-core` por `spring-boot-starter-flyway` en `pom.xml` (cambio de dependencia; se documenta en la sección de implementación de DA-005).

## Decisiones pendientes

- Autorizar la actualización de `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §7/§8/§10.
- Autorizar la actualización del `AGENTS.md` del servicio (hoy describe WebFlux y el `FlywayConfig` eliminado).

## Autorización humana

El usuario autorizó explícitamente con: *"arregla este error del inventory service"* (corrección del fallo de arranque en Docker). Los dos cambios aplicados son la corrección directa de ese fallo.

## Cambios aplicados

- `ticketflow-api-gateway/docker-compose.yml`: en `inventory-service`, `SPRING_R2DBC_URL/USERNAME/PASSWORD` + `SPRING_FLYWAY_URL/USER/PASSWORD` → `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`.
- `ticketflow-inventory-service/pom.xml`: `org.flywaydb:flyway-core` → `org.springframework.boot:spring-boot-starter-flyway`.

## Comandos ejecutados

- `docker compose config --quiet`
- `docker compose up -d --build inventory-service`
- `docker ps`, `docker logs ticketflow-inventory-service`
- `.\mvnw.cmd -q test`
- Pruebas HTTP contra `http://localhost:8080` (register/login/availability) y `http://localhost:8082` (availability directo).

## Pruebas realizadas y resultado

- **Docker/arranque:** `ticketflow-inventory-service` en `Up`; logs muestran Flyway ejecutándose, `Started ServiceApplication` y `Registering application TICKETFLOW-INVENTORY-SERVICE ... status UP`.
- **`.\mvnw.cmd -q test`:** `Tests run: 1, Failures: 0, Errors: 0` (EXIT=0). El test desactiva Flyway (`spring.flyway.enabled=false`), por lo que el nuevo starter no afecta al smoke test.
- **API directa (8082):** `GET /events/1/availability` → `200` `{"eventId":1,"localities":[{"locality":"VIP","availableTickets":0},{"locality":"General","availableTickets":0}]}`.
- **API vía gateway (8080):** `POST /register` → `201` (con `role:"comprador"`); `POST /login` → `200` con JWT; `GET /events/1/availability` con `Authorization: Bearer <token>` → `200`. Sin token → `401` (correcto: la ruta no es pública).
- **Nota:** `POST /register` con `role:"BUYER"` (nombre del enum) devuelve `400`; el enum `UserRole` se deserializa por su `@JsonValue` (`"comprador"`, `"organizador"`, `"staff_checkin"`, `"admin"`).

## Pendientes

- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §7/§8/§10 (WebFlux/R2DBC → MVC/JPA/Virtual Threads).
- `AGENTS.md` del servicio (reactivo/WebFlux; `FlywayConfig` eliminado).
- Tests de integración/concurrencia con Testcontainers (decisión aparte).

## Estado

IMPLEMENTADO

# P-008

## Identificador del prompt

P-008

## Fecha

2026-10-09

## Objetivo

Registrar la autorización humana y la implementación de **DA-004** (Flyway dentro de `inventory-service` y aprovisionamiento de la BD `inventory` con un `postgres` dedicado).

## Prompt completo utilizado

```text
Selección del usuario en la consulta de DA-004:
"Como aprovisiono la base de datos `inventory`?" → "Postgres dedicado a inventario (Recomendado)".
```

## Archivos de contexto consultados

- `.clinerules` (§1, §2, §5, §6, §8, §9, §10).
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§7, §8, §12).
- `docs_ia/contexto/pruebas/estrategia_pruebas.md` (§2: las pruebas unitarias no deben depender de servicios externos reales).

## Archivos del proyecto modificados

- `pom.xml` (`flyway-core`, `flyway-database-postgresql`).
- `src/main/resources/application.properties` (`spring.flyway.url/user/password`).
- `src/main/java/com/inventory/service/infrastructure/config/FlywayConfig.java` (nuevo).
- `src/main/resources/db/migration/V1__create_reservations.sql` (nuevo).
- `src/test/java/com/inventory/service/ServiceApplicationTests.java` (`@TestPropertySource` para desactivar Flyway en el smoke test).

## Archivos de otros repositorios modificados (autorizado explícitamente por el usuario)

- `ticketflow-api-gateway/docker-compose.yml` (nuevo `postgres-inventory`, inventario apunta a esta BD, eliminación de `postgres-init`).
- Se eliminó el directorio `ticketflow-api-gateway/postgres-init/`.

## Comandos ejecutados

- `.\mvnw.cmd -DskipTests compile`.
- `.\mvnw.cmd dependency:tree "-Dincludes=org.flywaydb:*,org.postgresql:*,com.zaxxer:HikariCP,org.springframework:spring-jdbc,org.springframework.boot:spring-boot-starter-jdbc"`.
- `.\mvnw.cmd test`.
- `docker compose -f docker-compose.yml config --quiet` (api-gateway).
- Verificación del DDL contra `postgres:18-alpine` efímero (contenedor temporal).

## Pruebas realizadas y resultado

- **compile:** BUILD SUCCESS.
- **dependency:tree:** `flyway-core:12.4.0` + `flyway-database-postgresql:12.4.0`; **sin** `HikariCP` ni `spring-boot-starter-jdbc` (no se agrega `DataSource` bloqueante).
- **test:** `Tests run: 1, Failures: 0, Errors: 0`; BUILD SUCCESS (exit 0).
- **docker compose config:** válido.
- **Migración:** aplicada contra PostgreSQL 18 efímero; crea `reservations` correctamente.

## Observaciones

- La primera ejecución de `test` falló porque el bean de Flyway conecta al arrancar y el PostgreSQL local usa otra contraseña (`admin`, del compose) que la de `application.properties` (`postgres`). Se resolvió desactivando Flyway en el smoke test (`spring.flyway.enabled=false`).
- No se usó `spring-boot-starter-flyway` (Boot 4) porque arrastra `spring-boot-starter-jdbc`/`DataSource` bloqueante, incompatible con un arranque WebFlux/R2DBC sin `spring.datasource.url`.

## Decisiones pendientes

- Actualizar `docs_ia/contexto` (§8) con la dependencia de Flyway (no autorizado).
- Aprobar formalmente el cambio de configuración del smoke test (`@TestPropertySource`) si se considera parte de la estrategia de pruebas.

## Estado

IMPLEMENTADO

# P-007

## Identificador del prompt

P-007

## Fecha

2026-10-09

## Objetivo

Analizar la solicitud del usuario de eliminar el script de init de la base de datos del `api-gateway` y gestionar el esquema de `inventory-service` "como en user-service" (Flyway dentro del microservicio). Producir la propuesta formal (`DA-004`).

## Prompt completo utilizado

```text
"pero por que hay un init en el api gateway? quiero que sea como en user-service, es decir, usar flyway y que se defina ahi mismo en el contenedor de inventory service, ademas, tal cual como esta en user-service"
```

## Archivos de contexto consultados

- `.clinerules` (§1 contexto, §2 decisiones, §6 dependencias, §8 autorización, §9 comunicación, §10 trazabilidad).
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§7 tecnologías, §8 dependencias, §12 Docker).

## Archivos del proyecto analizados

- `pom.xml` (sin Flyway; `org.postgresql:postgresql` ya presente en scope `runtime`).
- `src/main/resources/application.properties` (BD `inventory`).
- `src/main/java/.../infrastructure/adapter/out/persistence/ReservationPersistenceAdapter.java` (SQL nativo sobre `reservations`).
- `src/main/java/.../domain/model/Reservation.java`.

## Archivos de otros repositorios analizados

- `ticketflow-user-service/pom.xml`, `src/main/resources/application.properties` (no contiene Flyway; usa `spring.jpa.hibernate.ddl-auto=update`).
- `ticketflow-api-gateway/docker-compose.yml` y `postgres-init/01-create-inventory-db.sql`.

## Resumen de la respuesta generada

- Se aclaró por qué existe el init en el gateway: el servicio `postgres` del compose solo crea la BD `TicketFlow` (`POSTGRES_DB`) y `inventory-service` usa otra BD (`inventory`); **Flyway no crea bases de datos**, solo esquemas/tablas.
- Se corrigió una premisa: `user-service` **no usa Flyway**; usa auto‑DDL de Hibernate. Por ser un servicio WebFlux/R2DBC, el equivalente en inventario es una herramienta de migración (Flyway).
- Se propuso `DA-004` con alternativas para (a) la herramienta de migración y (b) el aprovisionamiento de la BD.

## Propuestas realizadas por la IA

- `DA-004` (`PROPUESTA`): Flyway dentro de `inventory-service` (migraciones en `src/main/resources/db/migration/`) y tres alternativas de aprovisionamiento de la BD (postgres dedicado —recomendada—, reutilizar `TicketFlow`, o script de init en el repo de inventario). Se incluyó también la alternativa sin dependencias nuevas (`schema.sql`).

## Decisiones pendientes

- Aprobar/rechazar `DA-004` y elegir la alternativa de aprovisionamiento de la BD.
- Autorizar los cambios en `pom.xml` (dependencias Flyway) y en el `docker-compose.yml`/`postgres-init` del gateway.

## Estado

PROPUESTA

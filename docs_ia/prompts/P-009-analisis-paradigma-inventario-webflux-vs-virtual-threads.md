# P-009

## Identificador del prompt

P-009

## Fecha

2026-10-10

## Objetivo

Registrar el análisis y la decisión sobre el paradigma de `inventory-service` (WebFlux/R2DBC reactivo vs. Web MVC bloqueante con Virtual Threads + JDBC/JPA) y proponer `DA-005`. Incluye la actualización de la documentación operativa (`AGENTS.md`) con la estrategia de pruebas objetivo.

## Prompt completo utilizado

```text
"en los test, si quiero que los cambies. con respecto a si usar webflux o virtual threads, no quiero lo más fácil sino lo más eficiente en cuanto a un sistema de gran escala"

Selección del usuario en la consulta sobre el paradigma de inventory-service:
"¿Qué hago con el paradigma de inventory-service?" → "Virtual Threads + JDBC/JPA".
```

## Archivos de contexto consultados

- `AGENTS.md` (raíz y del servicio) — gobernanza §1, §2, §5, §6, §8, §9, §10.
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§7 Tecnologías, §8 Dependencias, §10 Reactividad, §12 Docker).
- `docs_ia/contexto/arquitectura/arquitectura.md` (§10, §24.3).
- `docs_ia/contexto/arquitectura/desiciones_arquitectonicas.md` (verificación: sin mención al modelo de hilos).
- `docs_ia/contexto/pruebas/estrategia_pruebas.md` y `pruebas_cargas.md` (§6 Concurrencia).
- `docs_ia/decisiones/DA-001`, `DA-002`, `DA-004`.

## Archivos del proyecto analizados

- `pom.xml`.
- `src/main/resources/application.properties`.
- `src/main/java/com/inventory/service/application/{port,in,out,usecase}` (puertos y casos de uso).
- `src/main/java/com/inventory/service/domain/model/*`.
- `src/main/java/com/inventory/service/infrastructure/adapter/{in,out}/*` y `infrastructure/config/*`.
- `src/test/java/com/inventory/service/ServiceApplicationTests.java`.

## Archivos de otros repositorios analizados

- `ticketflow-user-service/pom.xml` (nombres de starters de Spring Boot 4: `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`).
- `ticketflow-api-gateway/docker-compose.yml` (env vars `SPRING_R2DBC_*` + `SPRING_FLYWAY_*` de `inventory-service`).

## Resumen de la respuesta generada

- Se explicó que, a gran escala, el cuello de botella es el datastore y el escalado horizontal, no el modelo de hilos, y se comparó WebFlux (reactivo) frente a Virtual Threads (bloqueante).
- Se ofreció la elección al usuario; eligió **Virtual Threads + JDBC/JPA**.
- Se redactó `DA-005` (PROPUESTA), que supersede §7/§8/§10 de `arquitectura_mircroservicio_inventario.md`.
- Se actualizaron los `AGENTS.md` (raíz y servicios) para reflejar la **estrategia de pruebas objetivo** (Testcontainers con Postgres/Redis/Kafka reales, prueba de concurrencia, sin H2), marcando "estado actual" vs. "objetivo".

## Propuestas realizadas por la IA

- `DA-005` (`PROPUESTA`): migrar `inventory-service` a Web MVC + Virtual Threads + JDBC (Spring Data JPA).
- Estrategia de pruebas objetivo documentada en los `AGENTS.md`.

## Decisiones pendientes

- Aprobar/rechazar `DA-005` y autorizar los cambios en `pom.xml`, configuración y código.
- Autorizar el cambio de variables de entorno en `ticketflow-api-gateway/docker-compose.yml`.
- Autorizar la actualización de `docs_ia/contexto` (§7/§8/§10) y del `AGENTS.md` del servicio.
- Confirmar si la persistencia se implementa con **Spring Data JPA** (recomendado) o con `JdbcClient`.
- Definir si se implementan los tests reales con Testcontainers (toca `pom` → autorización aparte).

## Autorización humana

El usuario aprobó `DA-005` eligiendo la opción "Virtual Threads + JDBC/JPA" → "Sí, con Spring Data JPA (Recomendado)".

## Cambios aplicados

Los detallados en `DA-005` → sección "Implementación (2026-10-10)".

## Comandos ejecutados

- `.\mvnw.cmd -DskipTests compile`.
- `.\mvnw.cmd test`.
- `.\mvnw.cmd dependency:tree`.

## Pruebas realizadas y resultado

- **test:** `Tests run: 1, Failures: 0, Errors: 0`; BUILD SUCCESS (sin infraestructura levantada).
- **dependency:tree:** sin `r2dbc-*` ni `webflux`; con `webmvc`, `data-jpa`, `data-redis`, HikariCP, Hibernate, Flyway y Kafka.

## Pendientes

- `docker-compose.yml` del gateway (`SPRING_R2DBC_*`/`SPRING_FLYWAY_*` → `SPRING_DATASOURCE_*`).
- `docs_ia/contexto` §7/§8/§10.
- `AGENTS.md` del servicio.
- Tests de integración/concurrencia con Testcontainers (decisión aparte).

## Estado

IMPLEMENTADO

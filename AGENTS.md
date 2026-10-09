# AGENTS.md — ticketflow-inventory-service

Inventario/reservas de TicketFlow (reactivo, WebFlux). Servicio **gobernado por `.clinerules`**: léelo completo antes de actuar. Las reglas completas viven ahí; esto es el resumen.

## Servicio

| Campo | Valor |
|---|---|
| Rol | Inventario / reservas (WebFlux) |
| Eureka name | `ticketflow-inventory-service` |
| Puerto | 8082 (`server.port=${SERVER_PORT:8082}`) |
| Depende de | PostgreSQL (`inventory`), Redis, Kafka, Eureka |
| Stack | Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3 · Resilience4j `2.4.0` (`resilience4j-spring-boot4`) |

## Comandos

```powershell
.\mvnw.cmd test              # solo contextLoads; Flyway se desactiva en el test (no requiere BD)
.\mvnw.cmd spring-boot:run
```

## Gotchas

- **Integrado con Eureka** (DA-003): se registra como `ticketflow-inventory-service` en el puerto **8082** y el gateway lo enruta con `lb://ticketflow-inventory-service`. Sin Eureka en local: `EUREKA_ENABLED=false`.
- **Auth efectivamente abierta**: incluye `spring-boot-starter-oauth2-resource-server`, pero `SecurityConfig` hace `anyExchange().permitAll()` y no hay issuer configurado → los JWT aún no se validan.
- **Dockerfile** en minúscula (`dockerfile`), multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-jammy`), `EXPOSE 8082`. Se levanta como `inventory-service` en el `docker-compose.yml` del gateway, junto con `kafka` y la BD `inventory`.
- **Esquema con Flyway** (DA-004): `flyway-core` + `flyway-database-postgresql`, migraciones en `src/main/resources/db/migration/` y `FlywayConfig` (conexión JDBC propia, porque Flyway no soporta R2DBC). La BD `inventory` la provee el postgres dedicado `postgres-inventory` del compose del gateway.
- Solo tiene el test `contextLoads` (sin `src/test/resources/application.properties`); ahí **Flyway se desactiva** vía `@TestPropertySource(properties = "spring.flyway.enabled=false")`.

## Convenciones

- Hexagonal + Clean + DDD: `com.inventory.service` con `domain/model`, `application/{port,usecase}`, `infrastructure/adapter/{in,out}`. Regla de dependencia: `infrastructure → application → domain` (dominio en Java puro).
- Código, identificadores, comentarios y documentación técnica **en inglés**; la documentación de proyecto en español.

## Gobernanza (`.clinerules` + `docs_ia`)

Las reglas completas están en **`.clinerules`** (léelo antes de tareas relevantes). Resumen:

- `docs_ia/contexto/` es la **única fuente autorizada** de requisitos, arquitectura, calidad y pruebas. No asumas lo que no esté ahí.
- Decisiones arquitectónicas: proponer en `docs_ia/decisiones/DA-xxx-*.md` (estado `PROPUESTA`) y **esperar aprobación humana** antes de implementar. No sobrescribir decisiones previas; si algo cambia, crear una nueva decisión.
- Trazabilidad: registrar toda interacción relevante en `docs_ia/prompts/P-xxx-*.md` (incluso análisis sin cambios).
- Dependencias: **no agregar/quitar/actualizar sin autorización humana explícita**; justificar cada una (propósito, alternativas, impacto, compatibilidad).
- Pruebas: no modificar la estrategia definida en `docs_ia/contexto/` sin proponerlo antes.

## Grafo (codebase-memory)

- Proyecto: `C-Users-Juan-Diego-Duque-Documents-Programacion-ticketflow-ticketflow-inventory-service`.
- Este repo es git → `detect_changes --base-branch main` funciona.
- Arquitectura entre servicios → grafo padre `C-Users-Juan-Diego-Duque-Documents-Programacion-ticketflow`.
- **Cambiar de rama NO refresca este grafo automáticamente**: tras `git checkout`, fuerza re-index con `index_repository --repo-path . --mode full`.

## No tocar

- `.mvn/`, `.idea/`, `target/` = build/IDE output.

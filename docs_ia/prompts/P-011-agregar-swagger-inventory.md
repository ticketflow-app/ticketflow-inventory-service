# P-011

## Identificador del prompt

P-011

## Fecha

2026-10-13

## Objetivo

Analizar y proponer la incorporación de **Swagger / OpenAPI** (`springdoc-openapi`) al microservicio `inventory-service`, en cumplimiento de la gobernanza (§2 propuesta de decisión, §6 gestión de dependencias).

## Prompt completo utilizado

```text
"agregar swagger al microservicio de inventory-service"
```

## Archivos de contexto consultados

- `AGENTS.md` (raíz y del servicio) — gobernanza §1, §2, §6, §8, §9, §10.
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§8 Dependencias, §9 Dependencias potenciales → OpenAPI).
- `docs_ia/contexto/arquitectura/desiciones_arquitectonicas.md` (verificación: sin conflicto).
- `docs_ia/prompts/dependencias/P-001-analisis-dependencias-inventario.md` (clasificación previa de `springdoc` como OPCIONAL).

## Archivos del proyecto analizados

- `pom.xml` (artefacto `com.inventory:service`, parent Boot 4.1.1; sin springdoc).
- `src/main/java/com/inventory/service/infrastructure/adapter/in/controller/{AvailabilityController,ReservationController,HealthController}.java`.
- `src/main/java/com/inventory/service/infrastructure/config/SecurityConfig.java`.
- `src/main/resources/application.properties`.

## Archivos de otros repositorios analizados

- `ticketflow-user-service/pom.xml` (`springdoc-openapi-starter-webmvc-ui:3.0.3`).
- `ticketflow-user-service/src/main/java/com/ticketflow/user/common/infrastructure/config/OpenApiConfig.java` (patrón `@OpenAPIDefinition` + `bearerAuth`).
- `ticketflow-user-service/src/main/resources/application.properties` (`springdoc.swagger-ui.path=/swagger-ui.html`).
- `ticketflow-user-service/docs/guia-microservicio-clean-code.md` (guía de patrón autoritativa).

## Verificación externa realizada (solo lectura, Maven Central)

- `org.springdoc:springdoc-openapi-starter-webmvc-ui`: 3.x es la línea para Spring Boot 4; última **3.1.1**; `user-service` usa **3.0.3** (verificado funcionando en el stack).

## Resumen de la respuesta generada

- El contexto (§9) **contempla** `springdoc-openapi` como dependencia potencial condicionada a necesidad concreta; el usuario aportó esa necesidad.
- Tras DA-005 el servicio es Web MVC → el artefacto correcto es `springdoc-openapi-starter-webmvc-ui` (no `webflux`).
- Se confirmó que `SecurityConfig` (`permitAll()`) ya deja libres `/swagger-ui/**` y `/v3/api-docs/**` → no requiere cambios de seguridad.
- Se redactó **`DA-006` (PROPUESTA)**: agregar `springdoc-openapi-starter-webmvc-ui:3.0.3` (consistente con `user-service`), `springdoc.swagger-ui.path=/swagger-ui.html`, un `OpenApiConfig` y anotaciones `@Tag`/`@Operation`.

## Propuestas realizadas por la IA

- `DA-006` (`PROPUESTA`): adopción de `springdoc-openapi` en `inventory-service`.

## Decisiones pendientes

- Aprobar/rechazar `DA-006` y autorizar los cambios en `pom.xml`, `application.properties` y código.
- Confirmar la **versión** de `springdoc-openapi-starter-webmvc-ui`: `3.0.3` (recomendada, = `user-service`) vs `3.1.1` (última 3.x).

## Autorización humana

El usuario solicitó explícitamente: *"agregar swagger al microservicio de inventory-service"*. Se ofreció una consulta de refinamiento (versión/alcance) que quedó sin responder; se procedió con la opción recomendada (`3.0.3` + implementación completa).

## Cambios aplicados

- `pom.xml`: `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3`.
- `src/main/resources/application.properties`: `springdoc.swagger-ui.path=/swagger-ui.html`.
- `infrastructure/config/OpenApiConfig.java` (nuevo).
- `@Tag`/`@Operation` en `AvailabilityController`, `ReservationController`, `HealthController`.
- `SecurityConfig`: sin cambios.

## Comandos ejecutados

- Lectura de `pom.xml` y verificación de `maven-metadata.xml` de `springdoc-openapi-starter-webmvc-ui` (Maven Central).
- `grep` de `springdoc|openapi|swagger` en el workspace.
- `.\mvnw.cmd -q clean test`.
- `docker compose up -d --build inventory-service`; `docker logs`; `curl` a `/v3/api-docs`, `/swagger-ui.html`, `/swagger-ui/index.html`.

## Pruebas realizadas y resultado

- **`.\mvnw.cmd -q clean test`:** `EXIT=0`; springdoc inicializa el contexto.
- **Docker:** servicio `Up`; `Started ServiceApplication in 34.031 seconds`; Eureka `registration status: 204`.
- **`GET /v3/api-docs`** → `200`; `info = {"title":"TicketFlow Inventory API","version":"v1"}`; paths `/reservations`, `/health`, `/events/{eventId}/availability`.
- **`GET /swagger-ui.html`** → `302` → `/swagger-ui/index.html` → `200`.

## Pendientes

- Opcional (autorización aparte): exponer `/swagger-ui/**` y `/v3/api-docs/**` a través del API Gateway.
- Opcional: revisar `springdoc.api-docs.enabled=false` / `springdoc.swagger-ui.enabled=false` en producción.

## Estado

IMPLEMENTADO

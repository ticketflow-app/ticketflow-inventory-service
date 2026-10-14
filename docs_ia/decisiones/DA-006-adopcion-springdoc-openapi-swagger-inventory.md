# DA-006

## ID

DA-006

## Título

Adopción de `springdoc-openapi` (Swagger UI / OpenAPI 3) en `inventory-service` para documentar la API

## Contexto

Información definida en el proyecto (`docs_ia/contexto`):

- `arquitectura/arquitectura_mircroservicio_inventario.md` §9 "Dependencias potenciales → OpenAPI": *"Puede utilizarse para documentar los endpoints de la API. Posible tecnología: `springdoc-openapi`."* — el contexto **contempla explícitamente** esta dependencia, pero la condiciona a una **necesidad concreta** (no debe agregarse automáticamente).
- §8 "Dependencias principales": *"Las versiones de las dependencias deben ser gestionadas por Spring Boot cuando sea posible, evitando declarar versiones manualmente innecesarias."* — `springdoc` **no** está en el BOM de Spring Boot, por lo que requiere versión explícita.
- `arquitectura/desiciones_arquitectonicas.md`: ningún ADR se opone; la documentación de API es transversal.

Estado verificado del repositorio (no definido en el contexto):

- **DA-005** migró `inventory-service` a Web MVC bloqueante (`spring-boot-starter-webmvc`) → corresponde el artefacto **`webmvc`** (no `webflux`).
- `ticketflow-user-service` ya usa `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` + `common/infrastructure/config/OpenApiConfig.java` (`@OpenAPIDefinition` con `bearerAuth`) y `springdoc.swagger-ui.path=/swagger-ui.html`. `docs/guia-microservicio-clean-code.md` es la guía de patrón autoritativa para servicios nuevos.
- Verificación externa (Maven Central): `springdoc-openapi-starter-webmvc-ui` tiene línea **3.x** para Spring Boot 4 (la 2.x es para Boot 3); última `3.1.1`, y `user-service` usa `3.0.3`.
- `infrastructure/config/SecurityConfig.java` hace `anyRequest().permitAll()` → Swagger UI quedaría **accesible sin cambios** de seguridad.
- Controladores actuales: `AvailabilityController` (`GET /events/{eventId}/availability`), `ReservationController` (`POST /reservations`), `HealthController` (`GET /health`).
- `docs_ia/prompts/dependencias/P-001-...` ya clasificó `springdoc` como dependencia **OPCIONAL** ("no agregar hasta que exista necesidad concreta").

## Problema

El servicio no expone documentación de su API (OpenAPI/Swagger). El usuario solicita agregarla. La necesidad concreta que el contexto §9 exige ya existe (documentar y probar los endpoints de Inventario), por lo que se debe decidir qué artefacto y versión usar y cómo configurarlo, sin romper la coherencia con el resto del sistema.

## Alternativas consideradas

### Alternativa A — `springdoc-openapi-starter-webmvc-ui` (recomendada)

Agregar `org.springdoc:springdoc-openapi-starter-webmvc-ui` (línea 3.x, para Spring Boot 4), con `OpenApiConfig` (`@OpenAPIDefinition` + `bearerAuth`) y anotaciones `@Tag`/`@Operation` en los controladores. Swagger UI en `/swagger-ui.html` y el documento OpenAPI en `/v3/api-docs`.

### Alternativa B — `springdoc-openapi-starter-webflux-ui`

Descartada: el servicio **ya no es reactivo** (DA-005); el starter WebFlux no corresponde al stack actual.

### Alternativa C — `springfox`

Descartada: proyecto sin mantenimiento activo y sin soporte para versiones modernas de Spring Boot.

### Alternativa D — No agregar dependencia; mantener un `openapi.yaml` a mano

Descartada: más esfuerzo de mantenimiento, riesgo de desincronización con el código y sin UI interactiva.

## Recomendación

**Adoptar la Alternativa A.** Cambios propuestos (**aún no aplicados**):

**`pom.xml`**

- **Agregar**: `org.springdoc:springdoc-openapi-starter-webmvc-ui` versión explícita **`3.0.3`** (consistencia con `user-service`, que ya lo usa y funciona con Boot 4.1.1). Alternativa disponible: `3.1.1` (última 3.x).

**Configuración (`application.properties`)**

- `springdoc.swagger-ui.path=/swagger-ui.html` (coherente con `user-service`).

**Código**

- Nuevo `infrastructure/config/OpenApiConfig.java`: `@OpenAPIDefinition(info = @Info(title = "TicketFlow Inventory API", version = "v1"), security = @SecurityRequirement(name = "bearerAuth"))` + `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = bearer, bearerFormat = JWT)` — replica el patrón de `user-service` (el gateway protege la API con JWT).
- Controladores: añadir `@Tag` (clase) y `@Operation` (métodos) para descripciones legibles.
- `SecurityConfig`: **sin cambios** (`anyRequest().permitAll()` ya deja libres `/swagger-ui/**`, `/swagger-ui.html` y `/v3/api-docs/**`).

## Justificación

1. El contexto (§9) contempla `springdoc-openapi` explícitamente; la necesidad concreta está presente (solicitud del usuario).
2. **Consistencia** de sistema: mismo artefacto, versión y patrón que `user-service`, alineado con la guía de patrón autoritativa.
3. Artefacto correcto para el stack actual tras DA-005 (Web MVC), no WebFlux.
4. Bajo riesgo: no altera endpoints, contrato HTTP, dominio ni seguridad; Swagger queda accesible como el resto de rutas (auth abierta ya existente).

## Ventajas

- Documentación interactiva y generada automáticamente desde el código.
- Facilita probar `GET /events/{eventId}/availability` y `POST /reservations` desde el navegador.
- Coherente con `user-service`.

## Desventajas / costos

- Dependencia de terceros fuera del BOM → versión explícita a mantener y actualizar manualmente.
- Añade superficie de exposición de la API. En este servicio no incrementa el riesgo real (ya hace `permitAll()` y JWT no se valida); en producción debe reevaluarse restringir `/swagger-ui/**` y `/v3/api-docs/**`.
- Suma a la ruta de la UI (`/swagger-ui.html`) una ruta que puede colisionar con controladores futuros.

## Impacto

- `pom.xml` (1 dependencia).
- `src/main/resources/application.properties` (1 propiedad).
- `infrastructure/config/OpenApiConfig.java` (nuevo) y anotaciones en los controladores.
- **No** cambia: endpoints, contrato HTTP, modelo de dominio, seguridad, ni la estrategia de pruebas.

## Estado

APROBADA (2026-10-13)

> Autorizada por la solicitud explícita del usuario: *"agregar swagger al microservicio de inventory-service"*. Se adoptó la opción recomendada (**`3.0.3`**, consistente con `user-service`) tras no obtener respuesta a la consulta de refinamiento de versión/alcance. Trazabilidad en `docs_ia/prompts/P-011-agregar-swagger-inventory.md`.

## Implementación (2026-10-13)

Cambios aplicados:

- `pom.xml`: agregada `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3` (versión explícita, fuera del BOM de Spring Boot).
- `src/main/resources/application.properties`: `springdoc.swagger-ui.path=/swagger-ui.html`.
- `infrastructure/config/OpenApiConfig.java` (nuevo): `@OpenAPIDefinition` (`TicketFlow Inventory API` v1) con esquema de seguridad `bearerAuth` (JWT).
- Anotaciones `@Tag` en clase y `@Operation` en método de `AvailabilityController`, `ReservationController` y `HealthController`.
- `SecurityConfig`: sin cambios (`anyRequest().permitAll()` ya expone `/swagger-ui/**`, `/swagger-ui.html` y `/v3/api-docs/**`).

Verificado (2026-10-13):

- `.\mvnw.cmd -q clean test`: `EXIT=0`; springdoc inicializa en el contexto.
- Docker (`docker compose up -d --build inventory-service`): servicio `Up`, `Started ServiceApplication in 34.031 seconds`, Eureka `UP`.
- `GET http://localhost:8082/v3/api-docs` → `200` (`{"title":"TicketFlow Inventory API","version":"v1"}`; paths `/reservations`, `/health`, `/events/{eventId}/availability`).
- `GET http://localhost:8082/swagger-ui.html` → `302` → `/swagger-ui/index.html` → `200`.

Nota: Swagger UI queda accesible **directamente en el puerto 8082**. El gateway no enruta `/swagger-ui/**` ni `/v3/api-docs/**` (fuera del alcance de esta decisión; requeriría autorización aparte si se desea exponerlos por el borde).

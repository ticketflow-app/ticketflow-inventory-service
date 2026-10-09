# DA-003

## ID

DA-003

## Título

Integrar `inventory-service` con el descubrimiento de servicios (Eureka) para registrarse junto al resto del ecosistema TicketFlow

## Contexto

Información definida en el proyecto (`docs_ia/contexto`):

- `arquitectura/arquitectura_mircroservicio_inventario.md` §1 define el microservicio: Maven, Java 21, Spring Boot 4.1.1, empaquetado JAR, formato Properties, group `com.inventory`, artifact `service`.
- §2–§3: arquitectura hexagonal (Ports and Adapters). §7–§8 listan tecnologías y dependencias principales (WebFlux, Redis reactivo, R2DBC/PostgreSQL, Security, Actuator, Kafka, Resilience4j). §12 menciona Docker.
- `arquitectura/arquitectura.md` describe los microservicios (Users, Catalog, Inventory), un **API Gateway con Spring Cloud Gateway** y, en las tablas de infraestructura, **"Balanceo | Application Load Balancer"**.
- El contexto **no menciona** Eureka, Consul ni ningún registro/descubrimiento de servicios. La única referencia a balanceo es el Application Load Balancer.

Estado verificado del repositorio (no especificado en el contexto):

- `pom.xml`: **no** incluye Spring Cloud ni cliente Eureka; no existe `dependencyManagement` del BOM de Spring Cloud.
- `application.properties`: `spring.application.name=service`; **no** define `server.port` (queda por defecto en 8080).
- El resto del ecosistema TicketFlow ya usa Eureka: `eureka-server` (8761), `ticketflow-user-service` (8081) y `api-gateway` (8080), todos con **Spring Cloud 2025.1.3**; el gateway enruta con `lb://` (resolución por Eureka).

## Problema

`inventory-service` queda **aislado** del resto del ecosistema:

1. No se registra en Eureka, por lo que ningún consumidor puede resolverlo por nombre (el gateway usa `lb://`).
2. No define `server.port`, por lo que arranca en **8080** y **colisiona con el api-gateway**.
3. `spring.application.name=service` es genérico y no permite una ruta/cliente estable.
4. El descubrimiento de servicios es una **decisión no definida** en `docs_ia/contexto`; conforme a `.clinerules` (§7 y §8), la implementación debe detenerse y presentarse la decisión.

## Alternativas consideradas

### Alternativa A — Integrar con Eureka (Spring Cloud 2025.1.3 + cliente Eureka) — recomendada

Añadir el BOM de Spring Cloud 2025.1.3 y `spring-cloud-starter-netflix-eureka-client`, registrar el servicio con un nombre estable y un puerto propio.

### Alternativa B — Enrutamiento por URL directa (sin Eureka)

El gateway apunta a `http://localhost:<puerto>` (como la variable `TICKET_SERVICE_URI`), sin registro.

Rechazada como solución principal: rompe la consistencia con el resto del stack y no aporta descubrimiento. Puede mantenerse como *fallback* de desarrollo.

### Alternativa C — Otro registro de servicios (p. ej. Consul)

Rechazada: el resto del ecosistema ya adoptó Eureka; introducir otro registro duplicaría infraestructura sin justificación en el contexto.

### Alternativa D — Mantener el servicio aislado

Rechazada: el objetivo solicitado es que todos los servicios "queden juntos".

## Recomendación

Adoptar la **Alternativa A**. Cambios propuestos (**aún no aplicados**):

**`pom.xml`**

- Property `spring-cloud.version=2025.1.3` (la misma que usan user-service y el gateway).
- `dependencyManagement` importando `org.springframework.cloud:spring-cloud-dependencies:${spring-cloud.version}` (`type` pom, `scope` import).
- Dependencia `org.springframework.cloud:spring-cloud-starter-netflix-eureka-client`.

**`application.properties`**

- `spring.application.name=ticketflow-inventory-service` (a confirmar; alternativa: `inventory-service`).
- `server.port=${SERVER_PORT:8082}` (8082 para no colisionar con gateway 8080 ni user 8081).
- `eureka.client.enabled=${EUREKA_ENABLED:true}`.
- `eureka.client.service-url.defaultZone=${EUREKA_DEFAULT_ZONE:http://localhost:8761/eureka}`.
- `eureka.instance.prefer-ip-address=true` (como user-service).

**`src/main/java/com/inventory/service/ServiceApplication.java`**

- Opcional: `@EnableDiscoveryClient` (explícito; en Spring Cloud moderno suele bastar la autoconfiguración).

**Actualización documental** (requiere autorización aparte)

- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §8: añadir `spring-cloud-starter-netflix-eureka-client`, y documentar el descubrimiento de servicios donde corresponda.

**Consumidores** (opcional, fuera del alcance de esta decisión)

- Añadir en `api-gateway` una ruta `lb://ticketflow-inventory-service`.

## Ventajas

- Integra Inventory en el ecosistema: se resuelve por nombre y puede enrutarse vía gateway (`lb://`).
- Consistencia de versiones (Spring Cloud 2025.1.3) con user-service y el gateway.
- Evita la colisión de puertos (8082) y otorga un nombre estable al servicio.

## Desventajas

- Dependencia nueva y no prevista en el contexto → exige una actualización documental del contexto.
- **WebFlux + cliente Eureka**: verificar con `mvn dependency:tree` que no arrastre el stack servlet; validar en arranque.
- **Resilience4j**: el BOM de Spring Cloud también gestiona Resilience4j; el proyecto fija `2.4.0` explícito. Verificar que no exista conflicto de versiones.
- **Pruebas**: `ServiceApplicationTests.contextLoads` no tiene `src/test/resources/application.properties`; con Eureka habilitado intentará registrar. Se propone `eureka.client.enabled=false` en pruebas (cambio a aprobar por separado).
- El registro requiere el `eureka-server` levantado; en local sin Eureka puede usarse `EUREKA_ENABLED=false`.

## Impacto

- `pom.xml` (BOM + 1 dependencia), `application.properties` (nombre, puerto, Eureka) y, opcionalmente, `ServiceApplication.java`.
- No afecta al dominio ni a la capa de aplicación (la configuración vive en infraestructura).
- No modifica decisiones aprobadas (ADR-001 a ADR-007, DA-001, DA-002).
- Habilita un cambio posterior en el gateway (ruta `lb://ticketflow-inventory-service`).

## Estado

APROBADA (2026-10-09)

> Aprobada explícitamente por el usuario, que autorizó aplicar la Alternativa A tal como se propone (nombre `ticketflow-inventory-service`, puerto 8082) y, como alcance adicional, añadir la ruta en `api-gateway`. Trazabilidad de la implementación en `docs_ia/prompts/P-005-implementacion-integracion-eureka.md`.

## Implementación (2026-10-09)

Cambios aplicados tras la aprobación:

- `pom.xml`: property `spring-cloud.version=2025.1.3`, `dependencyManagement` importando `spring-cloud-dependencies` y dependencia `spring-cloud-starter-netflix-eureka-client`.
- `src/main/resources/application.properties`: `spring.application.name=ticketflow-inventory-service`, `server.port=${SERVER_PORT:8082}` y propiedades de Eureka (`eureka.client.enabled`, `eureka.client.service-url.defaultZone`, `eureka.instance.prefer-ip-address`).
- `ticketflow-api-gateway`: nueva ruta `inventory-service` (`lb://ticketflow-inventory-service`), su circuit breaker (`inventoryCircuitBreaker`) y fallback `/fallback/inventory`.

Verificado: `mvnw compile` (inventario) y `mvnw test` (gateway) en verde; el árbol de dependencias confirma `spring-cloud-starter-netflix-eureka-client:5.0.2` + `spring-cloud-starter-loadbalancer:5.0.3`, sin arrastrar el stack servlet (sigue WebFlux).

Pendiente (no autorizado en esta decisión):

- Actualizar `docs_ia/contexto` (§8) para incluir la dependencia de Eureka y el descubrimiento de servicios.
- Deshabilitar Eureka en las pruebas (`ServiceApplicationTests`) o añadir `src/test/resources/application.properties`.

# DA-005

## ID

DA-005

## Título

Migración de `inventory-service` a Web MVC bloqueante con acceso a datos JDBC (Spring Data JPA) y Virtual Threads — supersede la reactividad WebFlux/R2DBC definida en el contexto

## Contexto

Información definida en el proyecto (`docs_ia/contexto`):

- `arquitectura/arquitectura_mircroservicio_inventario.md`:
  - §7 "Tecnologías": `Spring WebFlux` ⭐⭐⭐⭐⭐ ("Manejar alta concurrencia de forma no bloqueante"), `Spring Data Redis Reactive` ⭐⭐⭐⭐⭐ y `Spring Data R2DBC` ⭐⭐⭐⭐.
  - §8 "Dependencias principales": `spring-boot-starter-webflux`, `spring-boot-starter-data-redis-reactive`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql` y `resilience4j-reactor`.
  - §10 "Principios de implementación → Reactividad": *"El flujo crítico de Inventory debe mantenerse no bloqueante: WebFlux → Reactive Redis → R2DBC → PostgreSQL. No introducir operaciones bloqueantes dentro del flujo reactivo sin una justificación técnica."*
- `arquitectura/arquitectura.md`: §10 y §24.3 confirman que Inventario es el componente crítico para el control de concurrencia y de sobreventa; Redis mantiene los contadores atómicos y PostgreSQL el registro histórico. La concurrencia se resuelve con operaciones atómicas en Redis y con escalado horizontal de instancias (§ "Redis permite que diferentes instancias de Inventario trabajen sobre los mismos contadores"). Este documento **no** fija el modelo de hilos del framework web.
- `arquitectura/desiciones_arquitectonicas.md`: los ADR relevantes (ADR-003 Redis contadores, ADR-004 PostgreSQL registro, ADR-007 ventana de consistencia) son ortogonales al modelo de hilos; **no** lo definen (verificado: no hay ninguna mención a WebFlux/R2DBC/hilos).
- Reglas de negocio RB-08 (la disponibilidad nunca debe ser negativa), RB-11 (no sobreventa), RB-13 (persistencia de reservas) — `contexto_negocio.md` §12.
- Restricciones: equipo de 3 personas, ~6 semanas, prototipo académico; priorizar soluciones simples y apropiadas (`contexto_negocio.md` §17, §21.5).
- Stack: Java 21, Spring Boot 4.1.1, Spring Cloud 2025.1.3.

Estado verificado del repositorio (no definido en el contexto):

- `pom.xml`: `spring-boot-starter-webflux`, `spring-boot-starter-data-redis-reactive`, `spring-boot-starter-data-r2dbc`, `r2dbc-postgresql`, `resilience4j-reactor`. Además `org.postgresql:postgresql` (JDBC, `runtime`), hoy usado **solo para Flyway**.
- Los controllers devuelven `Mono`; los puertos `in`/`out` y los casos de uso devuelven `Mono`; los adaptadores usan `ReactiveStringRedisTemplate` y `io.r2dbc.spi.ConnectionFactory`.
- `KafkaReservationEventPublisher` usa el `KafkaTemplate` **bloqueante** envuelto en `Mono.fromFuture`: el servicio **ya es un híbrido** reactivo/bloqueante (contradice §10).
- `FlywayConfig` usa conexión JDBC propia porque "no hay `DataSource`" en un servicio R2DBC.
- `ticketflow-api-gateway/docker-compose.yml` inyecta `SPRING_R2DBC_*` + `SPRING_FLYWAY_*` en `inventory-service`.
- **El flujo crítico de sobreventa (`DECRBY` + compensación) todavía no está implementado**: `CreateReservationService` solo persiste y publica, y `RedisAvailabilityAdapter` solo lee. Esa lógica pertenece a DA-001 (estado `PROPUESTA`) y es ajena a esta decisión.

> Esta decisión **supersede** la parte de §7, §8 y §10 de `arquitectura_mircroservicio_inventario.md` relativa a WebFlux/R2DBC/reactividad. Queda condicionada a la aprobación humana y a una autorización **separada** para actualizar `docs_ia/contexto`.

## Problema

La decisión de adoptar WebFlux + R2DBC + Redis reactivo impone el modelo reactivo a todo el servicio, con costos concretos:

1. **Ecosistema limitado**: R2DBC no tiene ORM; obliga a SQL manual y mapping ad-hoc (como hoy en `ReservationPersistenceAdapter`).
2. **Complejidad y superficie de error**: operadores `Mono`/`Flux`, riesgo de bloquear el event loop, debugging difícil.
3. **Mezcla incoherente**: el productor Kafka ya es bloqueante; "completar" el reactivo exigiría migrar a `reactor-kafka` (dependencia adicional) y disciplina estricta.
4. **El modelo de hilos no está fijado por el negocio ni por los ADR**, y §10 solo prohíbe bloquear *dentro* del flujo reactivo; no obliga a mantener reactividad per se.

Se necesita decidir si Inventory se mantiene reactivo (WebFlux/R2DBC) o migra a un modelo bloqueante con Virtual Threads (Java 21), y formalizarlo.

## Alternativas consideradas

### Alternativa A — Web MVC + Virtual Threads + JDBC (Spring Data JPA) — seleccionada por el usuario

Migrar a `spring-boot-starter-webmvc` (Tomcat) con **Virtual Threads** (`spring.threads.virtual.enabled=true`), Redis bloqueante (`StringRedisTemplate`), persistencia JDBC con **Spring Data JPA**, y Kafka bloqueante (ya lo es). Eliminar WebFlux/R2DBC/Reactor.

### Alternativa B — Mantener y completar el reactivo (WebFlux + R2DBC + `reactor-kafka`)

Quedarse en el diseño actual y cerrar la inconsistencia reemplazando `KafkaTemplate` por un productor reactivo (`reactor-kafka`). Máxima eficiencia por nodo bajo concurrencia extrema y streaming; máximo costo de complejidad.

### Alternativa C — Híbrido (WebFlux + JPA bloqueante, o MVC + R2DBC)

Rechazada: mezcla modelos dentro del mismo servicio (el peor escenario: bloqueo del event loop o doble modelo de concurrencia). Es justamente lo que §10 prohíbe.

## Recomendación

**Adoptar la Alternativa A.** Cambios propuestos (**aún no aplicados**):

**`pom.xml`**

- **Quitar**: `spring-boot-starter-webflux`, `spring-boot-starter-data-redis-reactive`, `spring-boot-starter-data-r2dbc`, `org.postgresql:r2dbc-postgresql`, `io.github.resilience4j:resilience4j-reactor`.
- **Agregar**: `spring-boot-starter-webmvc` (artefacto de Spring Boot 4; `user-service` ya lo usa), `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis` (Lettuce bloqueante).
- **Mantener**: `flyway-core` + `flyway-database-postgresql`, `spring-kafka`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-oauth2-resource-server`, `io.github.resilience4j:resilience4j-spring-boot4:2.4.0`, `org.postgresql:postgresql` (`runtime`), `jackson-databind`, `spring-boot-starter-test`.
- (Opcional, pruebas) `spring-boot-starter-webmvc-test` para MockMvc.

**Configuración (`application.properties`)**

- Reemplazar `spring.r2dbc.*` por `spring.datasource.url/username/password` (JDBC `jdbc:postgresql://localhost:5432/inventory`).
- `spring.jpa.hibernate.ddl-auto=validate` (el esquema lo gobierna Flyway; distinto de `user-service`, que usa `update`).
- `spring.jpa.open-in-view=false`.
- `spring.threads.virtual.enabled=true`.
- Conservar Redis/Kafka/Eureka. `spring.flyway.*` puede simplificarse (Spring Boot autoconfigura Flyway a partir del `DataSource`).

**Código (infraestructura → aplicación → puertos)**

- `infrastructure/config/FlywayConfig.java`: **eliminar** y usar la autoconfiguración de Flyway (ya existe `DataSource`). Evita doble ejecución.
- `infrastructure/config/SecurityConfig.java`: pasar de `ServerHttpSecurity`/`SecurityWebFilterChain` a `HttpSecurity`/`SecurityFilterChain` (servlet). Se conserva `permitAll()` (habilitar JWT es otra decisión).
- `infrastructure/adapter/out/persistence/ReservationPersistenceAdapter.java`: de `ConnectionFactory` (R2DBC) a **Spring Data JPA** (nueva entidad `ReservationJpaEntity` en `infrastructure` + repositorio `JpaRepository`), preservando el `INSERT ... RETURNING id`. Alternativa más liviana: `JdbcClient` (spring-jdbc) sin entidad, si se desea conservar el SQL explícito.
- `infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java`: `ReactiveStringRedisTemplate` → `StringRedisTemplate`; `getAvailability` devuelve `Availability` (bloqueante).
- `infrastructure/adapter/out/messaging/KafkaReservationEventPublisher.java`: `publish` sin `Mono` (bloqueante/`CompletableFuture`).
- Puertos `in`/`out` y casos de uso: quitar `Mono` (firmas bloqueantes).
- `infrastructure/adapter/in/controller/*`: devolver el objeto directo (sin `Mono`).

**Cross-repo (`ticketflow-api-gateway/docker-compose.yml`) — requiere autorización aparte**

- Reemplazar `SPRING_R2DBC_*` + `SPRING_FLYWAY_*` por `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` en `inventory-service`.

**Documental (autorización aparte)**

- Actualizar `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §7, §8, §10 (sustituir WebFlux/R2DBC/reactivo por MVC/JPA/Virtual Threads).
- Actualizar `AGENTS.md` del servicio (hoy dice "reactivo, WebFlux").

## Justificación

1. **El cuello de botella es el datastore, no el modelo de hilos.** La concurrencia crítica se resuelve con `DECRBY`/`INCRBY` atómicos en Redis (§10, ADR-003) y con instancias horizontales. Redis es la ruta caliente (O(1)); PostgreSQL es registro. Ambos modelos escalan horizontalmente.
2. **Virtual Threads dan concurrencia de I/O comparable sin la complejidad reactiva**, con todo el ecosistema JDBC/JPA disponible.
3. **Coherencia con el stack elegido para los servicios base** (Virtual Threads) y con `user-service` (MVC + JPA), evitando que Inventory sea la excepción reactiva.
4. **Elimina la mezcla actual**: hoy ya hay un productor Kafka bloqueante dentro de un flujo "reactivo" (§10 ya violado).
5. **Simplicidad**: alineado con el principio de soluciones simples del contexto (§21.5) y con un equipo de 3 personas.

## Ventajas

- Sin R2DBC ni Reactor: menos superficie de error, debugging estándar, ORM/JPA y transacciones disponibles.
- Coherente con `user-service` y con el modelo de Virtual Threads del resto de servicios base.
- Flyway, JPA y el `DataSource` se unifican (desaparece la conexión JDBC "especial" de `FlywayConfig`).

## Desventajas / costos

- Menor eficiencia por nodo que el reactivo bajo concurrencia extrema o streaming; se compensa con escalado horizontal (ya previsto en `arquitectura.md`).
- **Java 21 y *pinning* de Virtual Threads**: los bloques `synchronized` pueden fijar el carrier thread (resuelto en Java 24 con JEP 491). Requiere evitar `synchronized` en la ruta caliente y verificar las dependencias (p. ej. Lettuce).
- **Contradice** `arquitectura_mircroservicio_inventario.md` §7/§8/§10 → exige actualización documental del contexto (autorización separada).
- Cambia dependencias y configuración → requiere autorización humana explícita (§6) y, en el flujo de reserva, revertir adaptadores ya escritos en R2DBC/Reactivo.

## Impacto

- `pom.xml` (quitar 5 / agregar 3; mantener el resto).
- `src/main/resources/application.properties` (datasource / JPA / virtual threads).
- `infrastructure/config/{FlywayConfig,SecurityConfig}.java`, adaptadores `out/{persistence,availability,messaging}`, `adapter/in/controller/*`, puertos `in`/`out` y casos de uso.
- `ticketflow-api-gateway/docker-compose.yml` (env vars de inventario) — autorización aparte.
- `docs_ia/contexto/...` y `AGENTS.md` — autorización aparte.
- **No** cambia: endpoints, contrato HTTP, topic/estructura de los eventos Kafka, modelo de dominio, ni las decisiones de consistencia (DA-001) ni los ADR.

## Estado

APROBADA (2026-10-10)

> Aprobada explícitamente por el usuario, que eligió la opción **"Virtual Threads + JDBC/JPA"** (persistencia con Spring Data JPA). Trazabilidad en `docs_ia/prompts/P-009-analisis-paradigma-inventario-webflux-vs-virtual-threads.md`.

## Implementación (2026-10-10)

Cambios aplicados tras la aprobación:

- `pom.xml`: eliminadas `spring-boot-starter-webflux`, `spring-boot-starter-data-redis-reactive`, `spring-boot-starter-data-r2dbc`, `org.postgresql:r2dbc-postgresql` y `io.github.resilience4j:resilience4j-reactor`. Agregadas `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa` y `spring-boot-starter-data-redis`. El resto sin cambios.
- `src/main/resources/application.properties`: `spring.r2dbc.*` → `spring.datasource.*`; `spring.jpa.hibernate.ddl-auto=validate`; `spring.jpa.open-in-view=false`; `spring.threads.virtual.enabled=true`. Se retiraron `spring.flyway.url/user/password` (Flyway se autoconfigura desde el `DataSource`).
- `infrastructure/config/FlywayConfig.java`: **eliminado** (ya no es necesario: existe `DataSource` y Flyway se autoconfigura).
- `infrastructure/config/SecurityConfig.java`: `ServerHttpSecurity`/`SecurityWebFilterChain` → `HttpSecurity`/`SecurityFilterChain` (servlet), conservando `permitAll()`.
- `infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java`: `ReactiveStringRedisTemplate` → `StringRedisTemplate` (bloqueante); `getAvailability` devuelve `Availability`.
- `infrastructure/adapter/out/messaging/KafkaReservationEventPublisher.java`: `publish` devuelve `void` (sin `Mono`; espera el envío con `join()`).
- Persistencia: nuevos `ReservationJpaEntity` (`@Entity`, `GenerationType.IDENTITY`) + `ReservationJpaRepository` (`JpaRepository`); `ReservationPersistenceAdapter` usa el repositorio JPA y mapea dominio ↔ entidad.
- Puertos `in`/`out`, casos de uso y controllers: firmas sin `Mono`/`Flux`.
- `src/test/resources/application.properties` (nuevo) + `ServiceApplicationTests`: el smoke test `contextLoads` sigue sin requerir infraestructura (dialecto explícito + `hibernate.boot.allow_jdbc_metadata_access=false` + `ddl-auto=none` + Hikari `initialization-fail-timeout=-1`).

Verificado (2026-10-10):

- `.\mvnw.cmd test`: `Tests run: 1, Failures: 0, Errors: 0`; BUILD SUCCESS (sin levantar Redis/PostgreSQL/Kafka).
- `dependency:tree`: sin `r2dbc-*` ni `spring-boot-starter-webflux`; con `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis`, `HikariCP`, `hibernate-core`, Flyway y Kafka.

### Corrección posterior (2026-10-13) — arranque en Docker

Al levantar `inventory-service` en el stack de Docker Compose se detectaron dos fallos encadenados (trazabilidad en `P-010`):

1. **Conexión a PostgreSQL**: el `docker-compose.yml` del gateway seguía inyectando `SPRING_R2DBC_*` + `SPRING_FLYWAY_*`, propiedades que el servicio ya no lee → caía a `localhost:5432` y no arrancaba. Se reemplazaron por `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (pendiente de DA-005 completado).
2. **Flyway no se ejecutaba** (`Schema validation: missing table [reservations]`): en Spring Boot 4 la autoconfiguración de Flyway está modularizada y `flyway-core` por sí solo no la activa. En DA-004 no se pudo usar el starter porque el servicio era R2DBC (sin `DataSource`); ahora sí existe `DataSource`, así que se sustituyó `org.flywaydb:flyway-core` por **`org.springframework.boot:spring-boot-starter-flyway`** (se conserva `flyway-database-postgresql`). Flyway se autoconfigura desde el `DataSource` y corre antes de la validación de Hibernate.

Verificado end-to-end (2026-10-13): contenedor `Up`, Flyway aplica `V1`, registro en Eureka `UP`, `.\mvnw.cmd -q test` verde y `GET /events/1/availability` vía gateway con JWT → `200`.

Pendiente (autorización aparte):

- Actualizar `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` §7/§8/§10.
- Actualizar `AGENTS.md` del servicio (hoy dice "reactivo, WebFlux" y describe el `FlywayConfig` eliminado).

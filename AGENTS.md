# AGENTS.md — ticketflow-inventory-service

Inventario/reservas de TicketFlow (reactivo, WebFlux). Gobernado por este `AGENTS.md` + `docs_ia` (el repo conserva también `.clinerules`, la versión equivalente para Cline).

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
.\mvnw.cmd test              # (actual) solo contextLoads; objetivo: Testcontainers + Postgres/Redis/Kafka
.\mvnw.cmd spring-boot:run
```

## Gotchas

- **Integrado con Eureka** (DA-003): se registra como `ticketflow-inventory-service` en el puerto **8082** y el gateway lo enruta con `lb://ticketflow-inventory-service`. Sin Eureka en local: `EUREKA_ENABLED=false`.
- **Auth efectivamente abierta**: incluye `spring-boot-starter-oauth2-resource-server`, pero `SecurityConfig` hace `anyExchange().permitAll()` y no hay issuer configurado → los JWT aún no se validan.
- **Dockerfile** en minúscula (`dockerfile`), multi-stage (`maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-jammy`), `EXPOSE 8082`. Se levanta como `inventory-service` en el `docker-compose.yml` del gateway, junto con `kafka` y la BD `inventory`.
- **Esquema con Flyway** (DA-004): `flyway-core` + `flyway-database-postgresql`, migraciones en `src/main/resources/db/migration/` y `FlywayConfig` (conexión JDBC propia, porque Flyway no soporta R2DBC). La BD `inventory` la provee el postgres dedicado `postgres-inventory` del compose del gateway.
- **Tests (actual → objetivo)**: hoy solo tiene `contextLoads` (sin `src/test/resources/application.properties`) y ahí **Flyway se desactiva** (`@TestPropertySource(properties = "spring.flyway.enabled=false")`). Objetivo: **Testcontainers** con Postgres real (Flyway activo) + Redis + Kafka, más la prueba de **concurrencia** (sin sobreventa).

## Convenciones

- Hexagonal + Clean + DDD: `com.inventory.service` con `domain/model`, `application/{port,usecase}`, `infrastructure/adapter/{in,out}`. Regla de dependencia: `infrastructure → application → domain` (dominio en Java puro).
- Código, identificadores, comentarios y documentación técnica **en inglés**; la documentación de proyecto en español.

## Gobernanza (para OpenCode)

> Versión para **OpenCode** de la gobernanza de TicketFlow (este repo también conserva `.clinerules`, la versión para Cline). Aplica a todo el trabajo en este repo; su fuente de verdad es `/docs_ia/contexto`.

### 1. Fuente de contexto

- `/docs_ia/contexto` es la **única fuente autorizada** para: requisitos, atributos de calidad, arquitectura, diseño, estrategia de pruebas y restricciones del proyecto.
- No asumir información que no esté definida en `/docs_ia/contexto`.
- Antes de una tarea relevante, revisar los archivos de `/docs_ia/contexto` necesarios para esa tarea.

### 2. Decisiones arquitectónicas

- No tomar decisiones arquitectónicas importantes de forma autónoma.
- Cuando haga falta decidir, analizar alternativas y proponer una recomendación.
- Crear la propuesta en `/docs_ia/decisiones` en Markdown.
- Esperar aprobación humana antes de implementar una decisión propuesta.
- Una decisión aprobada queda documentada en `/docs_ia/decisiones`.

### 3. Código

- Puede leer y modificar el código cuando la tarea haya sido aprobada.
- No hacer cambios importantes basados en suposiciones.
- Antes de cambios arquitectónicos, explicar brevemente qué se modificará y por qué.
- Mantener coherencia con las decisiones aprobadas.
- Nomenclatura **en inglés**: variables, métodos, clases, interfaces, paquetes, endpoints, DTOs, entidades, tablas, columnas y archivos de código.
- Comentarios y documentación técnica dentro del código, **en inglés**.
- No usar nombres en español en el código, salvo nombres externos obligados por una API, librería o requisito existente.
- Nomenclatura consistente y descriptiva según el lenguaje y framework.

### 4. Documentación

- Toda decisión arquitectónica o técnica importante se documenta en Markdown.
- Identificadores consecutivos: `DA-001`, `DA-002`, `DA-003`, ...
- No sobrescribir decisiones anteriores; si una decisión cambia, crear una nueva que explique el cambio.
- Cada decisión debe incluir: contexto, problema, alternativas consideradas, recomendación, ventajas, desventajas, impacto y estado.
- Estado `PROPUESTA` hasta recibir aprobación humana explícita; solo entonces `APROBADA` y aplicable.

### 5. Pruebas

- No modificar la estrategia de pruebas definida en `/docs_ia/contexto` sin proponerlo antes.
- Cuando corresponda, crear o modificar pruebas junto con el código.
- No introducir nuevas herramientas o frameworks de pruebas sin analizar antes su necesidad y compatibilidad.

### 6. Gestión de dependencias

- Antes de agregar, eliminar, actualizar o reemplazar una dependencia, revisar los archivos relevantes de `/docs_ia/contexto`.
- Revisar siempre el manifiesto de dependencias actual (`pom.xml`) antes de proponer cambios.
- Clasificar las dependencias como: indispensables, recomendadas, opcionales o no justificadas actualmente.
- Para cada dependencia propuesta indicar: propósito, funcionalidad, relación con la arquitectura, requisito que satisface, alternativas, impacto técnico y compatibilidad con las versiones actuales.
- Verificar compatibilidad con: versión de Java, versión de Spring Boot, versión de Maven y arquitectura definida.
- No agregar dependencias por conveniencia, costumbre o sugerencia automática de una herramienta.
- Principio de mínima dependencia: solo las justificadas por requisitos y arquitectura aprobada.
- **No agregar, eliminar, actualizar ni modificar dependencias sin autorización humana explícita.**
- Antes de modificar el manifiesto, presentar análisis y propuesta; tras la autorización, aplicar solo lo aprobado.
- Toda modificación importante de dependencias se documenta en `/docs_ia/decisiones`.

### 7. Uso de herramientas

- Puede leer archivos; crear y modificar cuando la tarea esté aprobada; ejecutar comandos necesarios para la tarea aprobada.
- No ejecutar comandos que modifiquen el proyecto sin autorización cuando los cambios no estén aprobados.
- Pedir aprobación antes de comandos destructivos o peligrosos y antes de instalar paquetes, dependencias, herramientas o software.
- No eliminar archivos, dependencias, configuraciones o código sin autorización explícita.
- Si una herramienta propone cambios automáticos, analizar primero y pedir autorización antes de aplicarlos.

### 8. Autorización de cambios

- Toda tarea que modifique código, configuración, arquitectura, dependencias o infraestructura requiere autorización humana si no estaba previamente aprobada.
- Analizar primero, modificar después.
- Una autorización específica no autoriza cambios adicionales no relacionados.
- No interpretar expresiones ambiguas como autorización para cambios importantes.
- Si la tarea requiere una decisión no definida en `/docs_ia/contexto`, detener la implementación y presentar la decisión.

### 9. Comunicación

- Explicar las decisiones de forma breve y técnica.
- Si falta información en `/docs_ia/contexto`, indicarlo explícitamente.
- Diferenciar siempre entre: información definida, suposición, propuesta y decisión aprobada.
- Presentar las alternativas técnicamente válidas antes de elegir una.
- No presentar una propuesta como si fuera una decisión aprobada.
- Informar qué archivos se modificarán antes de cambios importantes.
- Tras cambios autorizados, informar: archivos modificados, cambios realizados, comandos ejecutados, pruebas realizadas, resultado y pendientes.

### 10. Trazabilidad del uso de IA

- Toda interacción relevante debe quedar registrada, incluso si es solo análisis sin cambios de código.
- Se registra desde que se ejecuta el prompt y no requiere autorización.
- Ubicación: `/docs_ia/prompts`, un archivo Markdown independiente por interacción, con identificadores consecutivos `P-001`, `P-002`, ...
- Contenido mínimo: identificador, fecha, objetivo, prompt completo, archivos de contexto consultados, archivos analizados, resumen de la respuesta, propuestas, decisiones pendientes y estado.
- Estados permitidos: `ANALISIS` | `PROPUESTA` | `APROBADO` | `RECHAZADO` | `IMPLEMENTADO`.
- Si hay propuesta arquitectónica o técnica, crear también el documento correspondiente en `/docs_ia/decisiones`.
- El registro de trazabilidad y la propuesta documental **no** autorizan a modificar código, configuración, dependencias o infraestructura.
- Ningún archivo (salvo trazabilidad y propuestas) se modifica sin autorización humana explícita.
- Si solo se pide análisis, ejecutarlo y documentarlo sin tocar código.
- Si luego se autoriza, actualizar el registro con autorización, cambios, archivos, comandos, pruebas, resultado y estado final.
- Nunca sobrescribir registros históricos; si algo cambia, crear un nuevo registro relacionado.
- La trazabilidad debe permitir reconstruir: Contexto → Prompt → Respuesta IA → Propuesta → Autorización humana → Implementación → Pruebas → Resultado.

## Grafo (codebase-memory)

- Proyecto de grafo: `inventory-service`.
- Este repo es git → `detect_changes --base-branch main` funciona contra este proyecto.
- Arquitectura entre servicios → consulta cada proyecto por separado y correlaciona tú; **no hay proyecto padre**.
- **Cambiar de rama NO refresca el grafo automáticamente** (el watcher sigue la raíz, que no es git). Tras `git checkout` o editar, fuerza re-index: `index_repository(repo_path="ticketflow-inventory-service", mode="full", name="inventory-service")`. Reindexar descarta las aristas `CROSS_*` y reinicia su estado a `never_run`.
- Sincronización global: `pwsh -File .\sync-graph.ps1` desde la raíz re-indexa los 4 servicios y re-ejecuta el enlazado cross-repo.

## No tocar

- `.mvn/`, `.idea/`, `target/` = build/IDE output.

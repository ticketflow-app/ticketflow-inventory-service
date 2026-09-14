# P-001

## Identificador del prompt

P-001

## Fecha

2026-09-14

## Objetivo

Analizar las dependencias del microservicio Inventory comparando el `pom.xml` actual con las necesidades definidas en `/docs_ia/contexto` (archivo principal: arquitectura del microservicio de inventario), clasificándolas en INDISPENSABLES / RECOMENDADAS / OPCIONALES / NO JUSTIFICADAS ACTUALMENTE, verificando la compatibilidad con Java 21, Spring Boot 4.1.1 y Maven (con especial atención a Resilience4j), y recomendando la configuración mínima necesaria. La interacción fue estrictamente de análisis: sin modificar archivos, sin instalar dependencias y sin ejecutar comandos que alteren el proyecto.

## Prompt completo utilizado

```text
Quiero analizar las dependencias del microservicio Inventory.

IMPORTANTE:
NO puedes modificar ningún archivo.
NO puedes instalar ninguna dependencia.
NO puedes ejecutar comandos que modifiquen el proyecto.
NO puedes crear ni modificar decisiones.
Solo debes leer, analizar y presentar una propuesta.

Primero:

1. Lee los archivos relevantes de:
   /docs_ia/contexto
Archivo principal: /docs_ia/contexto/arquitectura_mircroservicio_inventario

2. Identifica específicamente toda la información relacionada con:
   - tecnologías
   - arquitectura
   - Spring Boot
   - Java
   - Maven
   - Redis
   - PostgreSQL
   - R2DBC
   - WebFlux
   - Kafka
   - seguridad
   - JWT
   - resiliencia
   - observabilidad
   - testing
   - Docker

3. Revisa el pom.xml actual del microservicio Inventory.

4. Compara las dependencias actuales del pom.xml con las necesidades definidas en /docs_ia/contexto.

5. Clasifica las dependencias en:
   - INDISPENSABLES
   - RECOMENDADAS
   - OPCIONALES
   - NO JUSTIFICADAS ACTUALMENTE

6. Para cada dependencia indica:
   - nombre
   - propósito
   - por qué se necesita o no
   - qué parte de la arquitectura soporta
   - si ya está presente en pom.xml
   - si falta
   - posibles alternativas
   - posibles problemas de compatibilidad

7. Verifica especialmente la compatibilidad con:
   - Java 21
   - Spring Boot 4.1.1
   - Maven

8. Presta especial atención a Resilience4j y cualquier dependencia cuya compatibilidad con Spring Boot 4.1.1 pueda requerir verificación.

9. Recomienda la configuración mínima necesaria para Inventory, evitando agregar dependencias innecesarias.

10. NO realices ningún cambio.

Al final entrega un informe breve con esta estructura:

## Dependencias encontradas en el contexto
## Dependencias actuales en pom.xml
## Indispensables
## Recomendadas
## Opcionales
## No justificadas actualmente
## Problemas o incompatibilidades detectadas
## Recomendación final
## Cambios que propondrías realizar

Recuerda:
NO ejecutes los cambios.
NO instales dependencias.
NO modifiques pom.xml.

Espera mi autorización explícita antes de hacer cualquier modificación.
```

Nota: la ruta indicada en el prompt (`/docs_ia/contexto/arquitectura_mircroservicio_inventario`) se resolvió a la ruta real `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.rm`.

## Archivos de contexto consultados

- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.rm` (principal: identificación §1, arquitectura hexagonal §2, estructura §3, capas §4-§6, tecnologías §7, dependencias principales §8, dependencias potenciales §9, reactividad/resiliencia/seguridad §10, observabilidad §11, Docker §12)
- `docs_ia/contexto/arquitectura/arquitectura.rm` (stack tecnológico §4, microservicio Inventario §5.3, hexagonal §6, orden de construcción §24, estado actual §27)
- `docs_ia/contexto/arquitectura/desiciones_arquitectonicas.rm` (ADR-001 a ADR-007, en particular ADR-003 Redis, ADR-004 PostgreSQL, ADR-005 JWT, ADR-007 riesgo Redis/PostgreSQL)
- `docs_ia/contexto/calidad/atributos_calidad.rm` (escalabilidad, disponibilidad, latencia, rendimiento, concurrencia, consistencia, observabilidad, mantenibilidad)
- `docs_ia/contexto/pruebas/estrategia_pruebas.rm` (niveles de prueba unitarias/integración/API/concurrencia/carga; base para evaluar dependencias de testing como Testcontainers)

## Archivos del proyecto analizados

- `pom.xml` (módulo único `com.inventory:service`; parent `spring-boot-starter-parent:4.1.1`; `java.version` 21; dependencias presentes: `spring-boot-starter` y `spring-boot-starter-test` (scope test); plugin `spring-boot-maven-plugin`)
- `src/main/java/com/inventory/service/ServiceApplication.java` (esqueleto inicial, sin lógica)
- `src/main/resources/application.properties` (solo `spring.application.name=service`)
- `src/test/java/com/inventory/service/ServiceApplicationTests.java` (prueba `contextLoads`)
- `.mvn/wrapper/maven-wrapper.properties` (wrapper 3.3.4; distribución Maven 3.9.16)

## Verificación externa realizada (solo lectura, Maven Central)

- `spring-boot-starter-parent`: la versión 4.1.1 existe ✓
- Starters con versión 4.1.1 verificada en su `maven-metadata.xml`: `webflux`, `data-redis-reactive`, `data-r2dbc`, `security`, `validation`, `actuator`, `test`, `oauth2-resource-server`, `kafka` (starter nuevo desde 4.0.0-M1; en Boot 3 no existía y se usaba `spring-kafka`)
- `io.github.resilience4j:resilience4j-spring-boot4`: única versión publicada **2.4.0** (2026-03-14), destinada a Spring Boot 4
- `io.github.resilience4j:resilience4j-reactor`: última versión **2.4.0**
- `org.springdoc:springdoc-openapi-starter-webflux-ui`: línea 3.x (última 3.1.1) para Spring Boot 4; la 2.x es para Boot 3

## Resumen de la respuesta generada

Se entregó el informe solicitado con la estructura indicada:

- INDISPENSABLES: `spring-boot-starter-webflux`, `spring-boot-starter-data-redis-reactive`, `spring-boot-starter-data-r2dbc`, `io.r2dbc:r2dbc-postgresql` (todas faltantes, versión 4.1.1 gestionada por el BOM) y `spring-boot-starter-test` (ya presente).
- RECOMENDADAS: `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server` (JWT, ADR-005), `spring-boot-starter-validation`, `spring-boot-starter-actuator` (con Micrometer), `resilience4j-spring-boot4:2.4.0` + `resilience4j-reactor:2.4.0` (versiones explícitas) y `spring-boot-starter-kafka` (diferido a la etapa de messaging, prioridad ⭐⭐⭐ en el contexto).
- OPCIONALES: `springdoc-openapi-starter-webflux-ui` 3.1.1, Testcontainers, `micrometer-registry-prometheus`, `micrometer-tracing`, Lombok (no recomendado; código explícito).
- NO JUSTIFICADAS ACTUALMENTE: `resilience4j-spring-boot3` (incompatible con Boot 4.1.1), starter núcleo redundante al agregar WebFlux, persistencia bloqueante (JPA/JDBC), `spring-boot-starter-data-redis` no reactivo.
- Problemas detectados: incompatibilidad Resilience4j (hallazgo principal), uso correcto de `spring-boot-starter-kafka` en Boot 4, springdoc debe ser 3.x, decisión pendiente sobre mecanismo JWT, compatibilidad Java 21 + Boot 4.1.1 + Maven 3.9.16 confirmada.
- Recomendación final: configuración mínima con solo las 4 dependencias indispensables (sin versiones explícitas) y diferir el resto por etapas.

## Propuestas realizadas por la IA

1. Etapa 1 (`pom.xml`): agregar `webflux`, `data-redis-reactive`, `data-r2dbc`, `r2dbc-postgresql`; mantener `starter-test`; opcionalmente eliminar el starter núcleo redundante.
2. Etapa 2: `security` + `oauth2-resource-server`, `validation`, `actuator`.
3. Etapa 3: `resilience4j-spring-boot4:2.4.0` + `resilience4j-reactor:2.4.0`.
4. Etapa 4: `spring-boot-starter-kafka`.
5. Actualización documental del contexto §8: sustituir `resilience4j-spring-boot3` por `resilience4j-spring-boot4:2.4.0` (formalizada luego como propuesta DA-002).
6. No agregar springdoc, Testcontainers, Lombok ni métricas extra hasta que exista necesidad concreta.

## Decisiones pendientes

- DA-002 (reemplazo de `resilience4j-spring-boot3` por `resilience4j-spring-boot4:2.4.0`): PROPUESTA, pendiente de aprobación humana.
- Mecanismo de validación JWT en Inventory (HS256 con secreto compartido vs RS256/JWKS vía resource server): no definido en el contexto.
- Backend de métricas y trazabilidad distribuida (Prometheus; `micrometer-tracing` + bridge OTel): no definido.
- Momento de agregación de `spring-boot-starter-kafka` (al implementar `adapter/out/messaging`).
- Eliminación del starter núcleo redundante (solo con autorización explícita).

## Estado

PROPUESTA

> Interacción de análisis ejecutada y documentada sin ninguna modificación al proyecto. Las propuestas quedan pendientes de autorización humana explícita. El hallazgo de incompatibilidad de Resilience4j se formalizó como la propuesta de decisión DA-002 (ver `/docs_ia/decisiones`).

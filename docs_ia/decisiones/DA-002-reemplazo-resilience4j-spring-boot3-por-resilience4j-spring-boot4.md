# DA-002

## ID

DA-002

## Título

Reemplazar `resilience4j-spring-boot3` por `resilience4j-spring-boot4:2.4.0` en el microservicio Inventory (compatibilidad con Spring Boot 4.1.1)

## Contexto

Información definida en el proyecto:

- El microservicio Inventory se define con Java 21, Spring Boot 4.1.1, Maven, packaging JAR (`docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.rm` §1).
- La sección 8 del mismo documento ("Dependencias principales") lista `resilience4j-spring-boot3` (prioridad ⭐⭐⭐⭐) y `resilience4j-reactor` (⭐⭐⭐⭐) para "Reintentos y tolerancia a fallos", e indica que "las versiones de las dependencias deben ser gestionadas por Spring Boot cuando sea posible".
- La sección 10 (Principios de implementación → Resiliencia) establece que "las operaciones susceptibles a fallos de infraestructura deben utilizar mecanismos de resiliencia cuando estén justificados".
- El `pom.xml` actual (verificado en P-001) contiene únicamente `spring-boot-starter` y `spring-boot-starter-test`; aún no incluye Resilience4j.

Verificación externa realizada en Maven Central (2026-09-14, solo lectura):

- `io.github.resilience4j:resilience4j-spring-boot4` existe con una única versión publicada: **2.4.0** (publicada el 2026-03-14). Es el módulo de integración de Resilience4j destinado a la línea Spring Boot 4.
- `io.github.resilience4j:resilience4j-reactor` tiene como última versión **2.4.0** (misma línea de release), necesaria para operadores de resiliencia sobre flujos `Mono`/`Flux` (Reactor), base del flujo reactivo definido en §10.
- Resilience4j **no** es gestionado por el BOM `spring-boot-starter-parent:4.1.1`, por lo que su versión debe declararse explícitamente en `pom.xml`.
- Todos los starters requeridos por Inventory (`webflux`, `data-redis-reactive`, `data-r2dbc`, `security`, `oauth2-resource-server`, `validation`, `actuator`, `kafka`) existen con versión 4.1.1.

## Problema

El documento de contexto §8 especifica `resilience4j-spring-boot3`, un artefacto cuya auto-configuración fue compilada para la línea Spring Boot 3.x. Con el stack definido (Spring Boot 4.1.1 / Spring Framework 7):

1. Existe un riesgo alto de incompatibilidad en compilación o en tiempo de arranque (auto-configuración y APIs de Boot 3 frente a Boot 4), sin garantía de soporte.
2. De agregarse tal como está documentado, la aplicación podría fallar al iniciar o perder la integración de Resilience4j con Spring Boot (anotaciones `@Retry`/`@CircuitBreaker`, propiedades de configuración y métricas).
3. El contexto no es aplicable tal cual: requiere corrección documental y una versión explícita (`2.4.0`), porque el BOM de Boot no gestiona Resilience4j.

## Alternativas consideradas

### Alternativa A — `resilience4j-spring-boot4:2.4.0` + `resilience4j-reactor:2.4.0`

Usar el módulo oficial de Resilience4j para Spring Boot 4 y el módulo Reactor para operadores sobre flujos `Mono`/`Flux`. Versiones explícitas e idénticas (2.4.0) en ambos módulos.

### Alternativa B — Mantener `resilience4j-spring-boot3` (según el contexto actual)

Rechazada: artefacto dirigido a Boot 3.x, incompatible con Boot 4.1.1; contradice el stack definido en el propio contexto.

### Alternativa C — No usar Resilience4j; reintentos nativos de Reactor (`retryWhen`)

Rechazada por ahora: perdería la integración con Spring Boot (anotaciones, configuración y métricas Micrometer de los circuit breakers) que el contexto asigna con prioridad ⭐⭐⭐⭐. Podría reevaluarse si las pruebas demuestran que la necesidad real de resiliencia no justifica la dependencia (principio de mínima dependencia).

### Alternativa D — Retroceder el proyecto a Spring Boot 3.5.x

Rechazada: contradice la versión definida en el contexto (4.1.1) y obligaría a migrar el resto del stack sin justificación alguna en los documentos.

## Recomendación

Adoptar la Alternativa A: incorporar Resilience4j mediante `io.github.resilience4j:resilience4j-spring-boot4:2.4.0` y `io.github.resilience4j:resilience4j-reactor:2.4.0`, con versiones declaradas explícitamente en `pom.xml`, e actualizar en consecuencia el documento de contexto (sección 8) para sustituir `resilience4j-spring-boot3` por `resilience4j-spring-boot4:2.4.0`. La incorporación al `pom.xml` corresponde a la Etapa 3 propuesta en P-001 (al implementar los mecanismos de resiliencia).

## Ventajas

- Compatibilidad real con Spring Boot 4.1.1: uso del módulo oficial de Resilience4j para la línea Boot 4.
- Conserva la integración definida en la arquitectura (anotaciones, configuración y métricas de resiliencia, prioridad ⭐⭐⭐⭐) y soporta los operadores Reactor del flujo reactivo.
- Misma versión (2.4.0) en ambos módulos, evitando desalineaciones entre `spring-boot4` y `reactor`.

## Desventajas

- Requiere declarar versiones manualmente (el BOM de Boot no gestiona Resilience4j), con mantenimiento manual ante futuras versiones.
- `resilience4j-spring-boot4` tiene un historial corto: una única versión publicada (2.4.0, 2026-03-14).
- Exige una actualización adicional del documento de contexto (cambio documental que debe aprobarse aparte).

## Impacto

- `pom.xml`: dos dependencias nuevas con versión explícita 2.4.0, en la etapa de implementación de resiliencia (Etapa 3 de P-001). No afecta al dominio ni a la capa de aplicación (la resiliencia vive en infraestructura, coherente con la arquitectura hexagonal).
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.rm` §8: sustitución de `resilience4j-spring-boot3` por `resilience4j-spring-boot4:2.4.0` (cambio documental pendiente de autorización; **no realizado**).
- Sin impacto sobre las decisiones aprobadas (ADR-001 a ADR-007 del contexto y DA-001).

## Estado

PROPUESTA

> Pendiente de aprobación humana. No se ha modificado `pom.xml` ni ningún archivo de contexto existente. Solo tras la aprobación explícita esta decisión pasará a APROBADA y se ejecutarán los cambios correspondientes (edición de `pom.xml` y actualización documental del contexto).

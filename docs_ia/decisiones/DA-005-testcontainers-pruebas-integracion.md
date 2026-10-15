# DA-005

## ID

DA-005

## Título

Testcontainers (PostgreSQL y Redis, alcance `test`) para las pruebas de integración del microservicio Inventory

## Contexto

- T-004 verificó la capacidad atómica de Redis con pruebas de integración (`RedisAvailabilityAdapterIT`) ejecutadas manualmente contra el contenedor Docker `inventory-redis` en `localhost:6379` (P-010 §9.1, P-011, P-012).
- Esa estrategia exige contenedores manuales en ejecución, acopla las IT a un entorno compartido (p. ej. claves ajenas presentes) y no es reproducible en limpio (CI u otra máquina).
- La orden humana de resolución de hallazgos T-004 (Fases 3 y 4) solicita incorporar Testcontainers para PostgreSQL y Redis con alcance `<scope>test</scope>` y migrar las IT a contenedores efímeros.
- `.clinerules` §6 exige justificar toda dependencia nueva (propósito, alternativas, impacto, compatibilidad) con autorización humana previa.

## Problema

Las IT actuales dependen de un contenedor manual persistente: no son reproducibles en limpio, no aíslan el estado entre ejecuciones y no permiten simular PostgreSQL para la prueba de compensación de DA-001 (Fase 4) sin otro contenedor manual.

## Alternativas consideradas

### Alternativa A — Testcontainers (`postgresql` y `redis`, alcance test)

Contenedores efímeros autogestionados por la JVM de prueba; las IT dejan de depender de contenedores manuales. Versiones gestionadas vía `org.testcontainers:testcontainers-bom`.

### Alternativa B — Mantener contenedores manuales (status quo)

Sin dependencia nueva, pero no reproducible en limpio, con acoplamiento al entorno y sin forma práctica de simular el fallo de PostgreSQL exigido por DA-001/Fase 4.

### Alternativa C — Redis/PostgreSQL embebidos en JVM

Rechazado: sin soporte maduro que replique el scripting Lua de Redis ni PostgreSQL+R2DBC de este stack; contradice la exigencia de "Redis real" ya demostrada en T-004.

## Recomendación

Adoptar la Alternativa A: `org.testcontainers:postgresql` y `org.testcontainers:redis` (con el BOM de Testcontainers), exclusivamente con alcance `test`, declarados en `pom.xml` **solo tras aprobación humana**. Migración de `RedisAvailabilityAdapterIT` y de la futura IT de compensación a contenedores efímeros (Fase 4).

## Ventajas

- Reproducibilidad en limpio (local y CI) sin contenedores manuales.
- Aislamiento real entre ejecuciones; independencia del estado de `inventory-redis`.
- Habilita la prueba de fallo de PostgreSQL de DA-001 con un efímero dedicado.

## Desventajas

- Arranque más lento de las IT y descarga inicial de imágenes.
- Dos dependencias de prueba adicionales (alcance `test`: no viajan en el artefacto final).

## Impacto

- `pom.xml`: 2 dependencias + BOM, alcance `test` únicamente (modificación sujeta a autorización).
- `src/test/.../RedisAvailabilityAdapterIT`: refactor de conexión (contenedor efímero vs. `localhost:6379` fijo).
- Sin impacto en producción ni en el dominio.

## Estado

APROBADA (2026-10-14)

> Aprobación humana explícita el 2026-10-14 (registro P-013), con autorización de modificación de `pom.xml` para añadir las dependencias de Testcontainers con alcance `test`.
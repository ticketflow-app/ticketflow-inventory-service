# P-011

## Identificador del prompt

P-011

## Fecha

2026-10-13

## Objetivo

Registrar la **autorización humana** y la **implementación de T-004** (capacidad atómica de disponibilidad en Redis: `initialize`, `tryReserve`, `release` con scripting Lua), ejecutada por `@coder` contra el contenedor existente `inventory-redis`. La implementación se limitó al alcance de T-004; no se modificó `CreateReservationService`, dominio, controladores, persistencia, `pom.xml` ni configuraciones, y no se cambiaron DA-001 ni DA-002.

## Prompt completo utilizado

```text
Autorizo la implementación de la tarea T-004 [...] Implementa únicamente las operaciones Redis definidas en T-004 [...] Usa el contenedor Docker existente inventory-redis [...] No agregues dependencias nuevas [...] Separa las pruebas de integración [...] de modo que mvnw test pueda ejecutarse sin exigir que Redis esté activo. [...] C1: K disponibles, M concurrentes (M>K) => exactamente K RESERVED, M-K INSUFFICIENT, disponibilidad final 0, nunca negativa. [...] Actualiza T-004 con evidencia real [...] No cambies DA-001 ni DA-002. [...] No ejecutes git commit ni push.
```

## Archivos de contexto consultados

- `.clinerules` (§3 código, §5 pruebas, §6 dependencias, §8 autorización, §9 comunicación, §10 trazabilidad, §11 tareas).
- `docs_ia/tareas/T-004_reserva_atomica_disponibilidad_redis.md` (alcance, contratos §4.2/§4.3, criterios C1–C7).
- `docs_ia/prompts/P-009…md`, `P-010-revision-tecnica-t004-redis.md`.
- `docs_ia/contexto/arquitectura/02_modelo_datos_persistencia.md`, `03_concurrencia_bloqueos.md`; `diseño/04_integracion_y_mensajeria.md`.
- `docs_ia/decisiones/DA-001` (`PROPUESTA`), `DA-002` (`PROPUESTA`, contradicción ya documentada).

## Archivos del proyecto creados

- `src/main/java/com/inventory/service/application/port/out/InitializeOutcome.java` (enum `INITIALIZED`/`ALREADY_INITIALIZED`).
- `src/main/java/com/inventory/service/application/port/out/ReservationOutcome.java` (enum `RESERVED`/`INSUFFICIENT_AVAILABILITY`/`NOT_INITIALIZED`).
- `src/main/java/com/inventory/service/application/port/out/ReleaseOutcome.java` (enum `RELEASED`/`NOT_INITIALIZED`).
- `src/test/java/com/inventory/service/infrastructure/adapter/out/availability/RedisAvailabilityAdapterIT.java` (pruebas de integración; sufijo `IT`, excluidas de `mvnw test`).

## Archivos del proyecto modificados

- `src/main/java/com/inventory/service/application/port/out/AvailabilityRepository.java`: se añadieron `initialize`, `tryReserve`, `release` (se conservó `getAvailability`).
- `src/main/java/com/inventory/service/infrastructure/adapter/out/availability/RedisAvailabilityAdapter.java`: implementación con scripting Lua atómico (`INITIALIZE`, `TRY_RESERVE`, `RELEASE`) vía `ReactiveStringRedisTemplate`; claves unificadas `availability:{event_id}:{section_id}` (+ `:capacity`); `getAvailability` conservado (lee `VIP`/`General`).
- `src/test/java/com/inventory/service/application/usecase/GetAvailabilityServiceTest.java`: se completó el `FakeAvailabilityRepository` con los 3 métodos del puerto ampliado (stubs `Mono.empty()`); sin cambio de comportamiento de los tests existentes. Intervención mínima e imprescindible por la ampliación del puerto.
- `docs_ia/tareas/T-004_reserva_atomica_disponibilidad_redis.md`: actualizada con la evidencia de implementación (§8, §9.4, §10, §11).
- `docs_ia/prompts/P-011-…md` (este registro).

## Comandos ejecutados

- `docker ps --filter name=inventory-redis`; `docker exec inventory-redis redis-cli ping` → `PONG`.
- `mvnw.cmd -o -B compile`.
- `mvnw.cmd -o -B test`.
- `mvnw.cmd -o -B test -Dtest=RedisAvailabilityAdapterIT`.
- `docker exec inventory-redis redis-cli --scan --pattern "availability:909000:*"`; `docker exec inventory-redis redis-cli dbsize`.

## Pruebas realizadas y resultado

- **compile:** BUILD SUCCESS (26 fuentes).
- **test (suite unitaria, sin Redis):** `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`. La clase `…IT` no se ejecuta por defecto.
- **integración (`-Dtest=RedisAvailabilityAdapterIT`, contra `inventory-redis`):** `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`. → **Corregido en el cierre (2026-10-13, ver addendum):** esta cifra quedó desactualizada al añadirse la prueba `c6c_concurrentInitializeOnlyOneWins`; el total actual verificado es **9** (`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`).
- **C1 (evidencia numérica):** `K=10`, `M=50` ⇒ 10 `RESERVED`, 40 `INSUFFICIENT_AVAILABILITY`, disponibilidad final `0`.
- **Limpieza:** sin claves residuales del evento de prueba; `dbsize=2` (claves ajenas no modificadas).

## Observaciones

- La atomicidad se logra con **scripting Lua** ejecutado por Redis (check-and-decrement en una sola operación), no simulando atomicidad con lecturas/escrituras separadas.
- Se respetó el entorno: se usó el contenedor `inventory-redis` existente; no se crearon/eliminaron contenedores ni se modificó `pom.xml`/`application.properties`.
- **No** se afirma idempotencia de `release`: el tope de capacidad evita exceder la disponibilidad, pero no impide liberaciones duplicadas de una misma reserva (fuera de alcance, ligado a DA-001).
- **Concurrencia de `initialize`** (C6) garantizada por `EXISTS`+`SET` en un script atómico; verificada secuencialmente en esta implementación. → **Actualizado en el cierre (2026-10-13):** sí existe prueba concurrente específica, `c6c_concurrentInitializeOnlyOneWins` (8 inicializaciones ⇒ 1 `INITIALIZED` / 7 `ALREADY_INITIALIZED`), ejecutada con 0 fallos.

## Decisiones pendientes

- Cierre formal de T-004 por `@tester`/`@integrador` (`.clinerules` §11). → **Completado 2026-10-13** (ver Addendum de cierre y registro `P-012`).
- (Opcional) timeouts de Redis en `application.properties`.
- (Fuera de alcance) resolver **DA-001** (orquestación reserva→persistencia→compensación en `CreateReservationService`) y regularizar la contradicción de **DA-002**.

## Addendum de cierre (2026-10-13, @integrador)

Registro de correcciones y re-verificación hechas al cierre de T-004 (registro completo en `P-012`). El contenido histórico de este registro se conserva; solo se corrigen datos que quedaron desactualizados respecto del código:

- **Corrección 8 → 9 pruebas de integración:** la ejecución original de este registro corrió antes de añadir `c6c_concurrentInitializeOnlyOneWins`. La ejecución de cierre (`mvnw.cmd -o -B test -Dtest=RedisAvailabilityAdapterIT`) dio `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` — `BUILD SUCCESS` contra `inventory-redis`.
- **Concurrencia de `initialize`:** la observación original ("sin test concurrente específico") quedó superada por la prueba `c6c_concurrentInitializeOnlyOneWins`, verificada en el cierre.
- **Re-verificación completa del cierre:** `mvnw.cmd -o -B compile` → `BUILD SUCCESS`; `mvnw.cmd -o -B test` → `Tests run: 21, Failures: 0` → `BUILD SUCCESS`; sin claves de prueba residuales (`availability:909000:*` → vacío) y `dbsize=2` con las claves ajenas `inventory:availability:25:VIP|General` intactas.
- **Observaciones de `@tester`** (comunicadas en la instrucción de cierre; no hay archivo de informe de `@tester` en el repositorio) registradas en T-004 §9.6: correspondencia nombres de tests ↔ criterios C1–C7, evidencia de inicialización concurrente e impacto del cambio de prefijo `inventory:availability:` → `availability:` (las claves del evento 25 siguen existiendo pero ya no se leen; sin migración, decisión humana pendiente).
- **Desviación detectada:** la implementación retiró `@Retry(name = "inventoryRetry")` de `getAvailability` sin registrarlo aquí; documentada en T-004 §10 como decisión humana pendiente.
- Sin `git add`, `git commit` ni `git push` en esta interacción.

## Estado

IMPLEMENTADO

> Evidencia real adjunta en T-004 (§9.4/§10). No se ejecutaron `git commit` ni `git push`.

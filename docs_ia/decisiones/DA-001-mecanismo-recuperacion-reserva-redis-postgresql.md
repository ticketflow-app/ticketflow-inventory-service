# DA-001

## ID

DA-001

## Título

Mecanismo de recuperación ante fallo de persistencia en la operación de reserva (consistencia Redis → PostgreSQL)

## Contexto

La arquitectura aprobada para la operación crítica de reserva define que:

- Redis mantiene los contadores atómicos de disponibilidad y la decisión de disponibilidad se ejecuta en tiempo real mediante `DECRBY` (ADR-003, `docs_ia/contexto/arquitectura/desiciones_arquitectonicas.rm`).
- PostgreSQL mantiene el registro persistente, durable y auditable de las reservas (ADR-004).
- El flujo de reserva es: `DECRBY` sobre el contador → si el resultado es ≥ 0, registrar la reserva en PostgreSQL (`docs_ia/contexto/arquitectura/arquitectura.rm` §10.4).

El documento de decisión ADR-007 aceptó esta combinación *"con riesgo conocido"*: puede existir una ventana de inconsistencia si la modificación de Redis es exitosa pero la persistencia en PostgreSQL falla. Su acción futura indica que *"se deberá evaluar un mecanismo de recuperación adecuado y realizar pruebas específicas de fallo"*.

Adicionalmente:

- El caso de uso CU-06 (Crear reserva), flujo alternativo FA-03 *"Error durante persistencia"*, exige: *"El sistema debe aplicar el mecanismo de recuperación definido por la arquitectura. El incidente debe quedar registrado para su análisis."* (`docs_ia/contexto/requisitos/casos_uso.rm`).
- La sección 10.6 de `docs_ia/contexto/arquitectura/arquitectura.rm` indica que este escenario *"deberá ser evaluado durante las pruebas de consistencia y resiliencia"*.
- Reglas de negocio aplicables (`docs_ia/contexto/contexto_negocio.rm` §12): RB-08 (la disponibilidad nunca debe ser negativa tras una operación exitosa), RB-11 (no sobreventa — regla fundamental del negocio), RB-13 (persistencia de reservas para trazabilidad).
- La arquitectura ya incorpora primitivas reutilizables (`docs_ia/contexto/arquitectura/arquitectura.rm` §14-§15): clave de idempotencia en `POST /reservations`, dead-letter topic de Kafka, Resilience4j, métricas Actuator/Micrometer.
- Restricciones del proyecto: equipo de 3 personas, ~6 semanas, carácter académico, priorizar *"soluciones simples y apropiadas para un prototipo académico"* (`docs_ia/contexto/contexto_negocio.rm` §17 y §21.5).

Toda la información anterior es **información definida en los documentos del proyecto**. La recomendación de este documento es una **propuesta** que requiere aprobación humana.

## Problema

En el flujo actual, si el `DECRBY` en Redis tiene éxito pero la escritura de la reserva en PostgreSQL falla, ocurre que:

1. La disponibilidad queda disminuida en Redis sin que exista un registro de reserva en PostgreSQL (**disponibilidad fantasma**): entradas que ya no pueden venderse y que no corresponden a ninguna reserva real.
2. No existe un mecanismo definido para detectar, registrar y reparar esta inconsistencia.
3. El requisito CU-06 FA-03 (aplicar mecanismo de recuperación y registrar el incidente) quedaría incumplido.

Nota: el fallo inverso (persistencia en PostgreSQL sin decremento en Redis) no puede ocurrir con el orden actual Redis-primero, pero sí podría ocurrir si se invirtiera el orden de las operaciones (ver alternativa B).

Se necesita decidir y formalizar el mecanismo de recuperación, sin modificar la decisión central ya aceptada en ADR-007 y protegiendo en todo caso la regla fundamental RB-11 (no sobreventa).

## Alternativas

### Alternativa A — Compensación inmediata + registro de incidente + reconciliación periódica

Mantener el orden actual (Redis primero). Ante fallo de la persistencia:

1. Ejecutar compensación con `INCRBY` sobre el contador para restaurar la disponibilidad, con reintentos (Resilience4j, ya definido en la arquitectura).
2. Persistir el incidente de forma durable (registro de incidente en PostgreSQL) para su análisis, cumpliendo CU-06 FA-03.
3. Ejecutar un trabajo de reconciliación periódico que compare los contadores de Redis contra la suma de reservas CONFIRMED en PostgreSQL por sección y corrija Redis tomando PostgreSQL como fuente de verdad de las reservas (RB-13).

En el peor caso (compensación también falla, p. ej. caída simultánea de Redis), la inconsistencia queda acotada, registrada en el incidente y detectada por la reconciliación. El fallo produce **venta perdida**, nunca **sobreventa**.

### Alternativa B — Persistir primero en PostgreSQL (estado PENDING) y luego decrementar Redis

La reserva se registra en PostgreSQL con estado `PENDING`, luego se decrementa Redis; si el decremento falla, la reserva se marca como cancelada/rechazada.

Problema: entre la escritura en PostgreSQL y el decremento en Redis, el contador todavía refleja disponibilidad; solicitudes concurrentes adicionales podrían confirmarse superando la capacidad real, abriendo una **ventana de sobreventa** hasta su compensación. Esto viola la regla fundamental RB-11 durante la ventana de fallo. Dirección de fallo incorrecta: en lugar de perder ventas, se arriesga sobreventa.

### Alternativa C — Outbox transaccional + reconstrucción de contadores desde eventos

Escribir la reserva y un evento en un outbox transaccional en PostgreSQL; un proceso publica a Kafka (`reservas.confirmadas`) y los contadores de Redis se reconstruyen/des-duplican a partir del log de eventos.

Problema: exige implementar outbox, relay/consumidor y lógica de reconstrucción de contadores; complejidad operativa alta no justificada para un equipo de 3 personas y ~6 semanas (contexto_negocio §17, §21.5). El propio documento de arquitectura sitúa la evolución basada en eventos como extensión futura (§26), no como requisito de la primera fase.

### Alternativa D — Transacción distribuida (2PC / XA)

Coordinar Redis y PostgreSQL bajo una transacción distribuida.

Problema: introduce latencia y complejidad operativa significativa, no es apropiada para el prototipo y no existe en el contexto del proyecto ninguna justificación que la requiera.

## Recomendación

**Adoptar la Alternativa A**: mantener el orden Redis-primero ya aceptado, con compensación inmediata (`INCRBY`) ante fallo de persistencia, registro durable del incidente y trabajo de reconciliación periódica que corrige Redis a partir de PostgreSQL como fuente de verdad de las reservas.

De aprobarse, la propuesta se complementaría con pruebas específicas de fallo (fallo simulado de PostgreSQL tras un `DECRBY` exitoso), tal como exige la acción futura de ADR-007 y la estrategia de pruebas de consistencia y resiliencia.

## Justificación

Basada únicamente en el contexto definido:

1. **Protege la regla fundamental (RB-11):** el orden Redis-primero con compensación garantiza que cualquier fallo derive en venta perdida (conservadora), nunca en sobreventa. La Alternativa B invierte la dirección de fallo y abre ventana de sobreventa, inaceptable según el contexto de negocio (§10 y RB-11).
2. **Completa ADR-007 sin contradecirla:** mantiene las decisiones centrales ya aceptadas (ADR-003, ADR-004, ADR-007) y formaliza su acción futura pendiente.
3. **Cumple CU-06 FA-03:** incluye el registro durable del incidente *"para su análisis"*, requisito explícito del caso de uso.
4. **Reutiliza lo ya definido:** no introduce tecnologías nuevas — usa `INCRBY` (§10.4), reintentos con Resilience4j, clave de idempotencia (evita duplicados al reintentar la reserva, §15), dead-letter topic e incidentes registrados (§14). Coherente con el principio de soluciones simples (contexto_negocio §21.5).
5. **PostgreSQL como referencia de reparación:** la reconciliación usa el registro persistente de reservas (RB-13) para corregir Redis, sin convertir la reparación en la ruta crítica de la reserva.
6. **Acotación del residuo:** el único caso no cubierto en caliente (fallo simultáneo de la compensación) queda registrado y es detectado por la reconciliación, con costo de implementación acotado y verificable mediante las pruebas de fallo exigidas por la estrategia de pruebas y ADR-007.

## Consecuencias

**Positivas:**

- Cierra formalmente la ventana de riesgo reconocida en ADR-007 con un mecanismo definido y verificable.
- La sobreventa sigue siendo imposible incluso bajo fallos de persistencia (RB-11 protegida).
- Cumple el flujo alternativo CU-06 FA-03 (recuperación + incidente registrado).
- No agrega tecnologías nuevas; usa las primitivas ya definidas en la arquitectura (§14-§15).
- Habilita las pruebas específicas de fallo y de consistencia previstas (ADR-007, estrategia de pruebas, escenario de calidad de disponibilidad limitada).

**Negativas / costos:**

- Persiste un residuo teórico: si la compensación falla también, la disponibilidad queda subestimada hasta la reconciliación (venta perdida temporal, nunca sobreventa).
- Requiere implementar dos componentes adicionales en Inventory: persistencia de incidentes y trabajo de reconciliación periódica (esfuerzo acotado, dentro del alcance del prototipo).
- La periodicidad de la reconciliación define el tiempo máximo de corrección de una inconsistencia no compensada; su valor deberá definirse y justificarse en la implementación (valor no definido en el contexto).
- En la ruta de fallo, el cliente recibe un error transitorio; la idempotencia (§15) evita reservas duplicadas ante reintentos del cliente.

## Estado

PROPUESTA

> Pendiente de aprobación humana. No implementar hasta contar con la aprobación explícita. Si se aprueba, esta decisión complementa (no reemplaza) a ADR-003, ADR-004 y ADR-007.

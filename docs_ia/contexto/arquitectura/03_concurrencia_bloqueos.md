# Control de Concurrencia, Resiliencia e Idempotencia - Microservicio Inventory

## 1. Control de Disponibilidad mediante Operaciones Atómicas

Para garantizar **consistencia inmediata** y evitar la sobreventa (overselling) bajo escenarios de alta demanda, la toma de decisiones de inventario se ejecuta de forma atómica sobre Redis antes de tocar la base de datos.

### Algoritmo de Reserva Atómica (Decrements)

```text
Cliente (WebFlux)
    │
    ▼
1. Ejecuta DECRBY availability:{event_id}:{section_id} N
    │
    ├─► Si resultado >= 0 ──► 2. Registrar reserva PENDING en PostgreSQL (R2DBC)
    │                            │
    │                            ├─► Éxito: Confirmar reserva y retornar 201 Created
    │                            └─► Falla R2DBC: Compensación (INCRBY Redis) + Error 500
    │
    └─► Si resultado < 0  ──► 3. Ejecutar INCRBY Redis (Compensar / Rollback en memoria)
                                 │
                                 └─► Retornar 400 Bad Request ("Insufficient availability")
```

## 2. Manejo de Riesgo de Inconsistencia (ADR-007)

### Escenario de Falla (Doble Escritura)

Si la operación en Redis es exitosa (`DECRBY` decrementa el stock), pero la escritura posterior en PostgreSQL falla por pérdida de conexión o tiempo de espera:

1. **Mecanismo de Compensación Inmediato:** El caso de uso captura la falla de R2DBC y ejecuta de forma asíncrona un `INCRBY` en Redis por el mismo monto para liberar el stock tomado en memoria.
2. **Alertas y Métricas:** Toda falla de persistencia tras afectar la caché genera un evento de métrica en Micrometer para monitoreo en tiempo real.

## 3. Idempotencia en la Creación de Reservas

Para evitar reservas duplicadas provocadas por reintentos de red del cliente cuando la solicitud original sí fue procesada:

- El cliente envía el header HTTP obligatorio: `Idempotency-Key: <UUID>`.
- El servicio consulta la tabla `reservations` (o un registro temporal en Redis) utilizando la `idempotency_key`.
- Si la clave ya existe, el microservicio retorna la respuesta almacenada anteriormente sin volver a ejecutar el `DECRBY` ni registrar otra reserva.

## 4. Estrategia de Resiliencia (Resilience4j)

- **Circuit Breaker:** Protege las llamadas externas (como la publicación en Kafka) abriendo el circuito si la tasa de errores supera el umbral configurado.
- **Rate Limiting:** Se configura a nivel de API Gateway/WebFlux para prevenir ataques de denegación de servicio o abusos de botnets en la apertura de ventas.
- **Retry con Exponential Backoff:** Aplica reintentos no bloqueantes sobre operaciones de lectura esporádicamente fallidas.
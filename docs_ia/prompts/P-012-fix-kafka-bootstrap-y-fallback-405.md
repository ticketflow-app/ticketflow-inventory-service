# P-012

## Identificador del prompt

P-012

## Fecha

2026-10-14

## Objetivo

Diagnosticar el error **405 Method Not Allowed** al crear una reserva por `POST http://localhost:8080/reservations` y la avalancha de warnings de Kafka del `inventory-service`, y proponer las correcciones (`DA-007`).

## Prompt completo utilizado

```text
"me sale todo esto en el microservicio de inventorty, estoy intentando acceder al metodo de crear una reservations con la url http://localhost:8080/reservations pero me sale error 405 method not allowed:"
```

(seguido de logs de `org.apache.kafka.clients.*` mostrando `Rebootstrapping with [localhost/127.0.0.1:9092]` y `Connection to node -1 ... could not be established`)

## Archivos de contexto consultados

- `AGENTS.md` (raíz y del servicio) — gobernanza §1, §2, §3, §8, §10.
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md`, `arquitectura.md` (mensajería Kafka).
- `docs_ia/decisiones/DA-005` (migración a MVC/JPA; `publish` con `.join()`).

## Archivos del proyecto analizados

- `src/main/java/com/inventory/service/infrastructure/config/KafkaConfig.java`.
- `src/main/java/com/inventory/service/infrastructure/adapter/out/messaging/KafkaReservationEventPublisher.java`.
- `src/main/resources/application.properties`.
- Logs del contenedor `ticketflow-inventory-service`.

## Archivos de otros repositorios analizados

- `ticketflow-api-gateway/src/main/resources/application.yml` (ruta y circuit breaker de inventory).
- `ticketflow-api-gateway/src/main/java/ticketflow/api_gateway/fallback/FallbackController.java` (fallback solo GET).
- `ticketflow-api-gateway/src/main/java/ticketflow/api_gateway/filter/JwtAuthFilter.java`.
- `ticketflow-api-gateway/docker-compose.yml` (Kafka y envs de inventory).

## Resumen de la respuesta generada

Se identificaron **dos** causas que producen 405:

1. **`POST /reservations` → 405 tras ~3.04 s**: el circuit breaker del gateway (TimeLimiter `3s`) se dispara y reenvía a `forward:/fallback/inventory`, que solo acepta **GET** (`@GetMapping`) → 405. El corte ocurre porque `KafkaConfig` fija `bootstrap.servers` a **`localhost:9092`** (hardcodeado), ignorando `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092` del compose; el productor no conecta y `KafkaReservationEventPublisher.publish(...).join()` bloquea la petición.
2. **`GET /reservations` → 405 inmediato**: el endpoint es **POST-only**; GET no está permitido.

Se propuso `DA-007` (inventory: leer el bootstrap desde `spring.kafka.bootstrap-servers`) y se detectó la necesidad de la corrección de fallback en el gateway (`DA-001` del repo `api-gateway`).

## Propuestas realizadas por la IA

- `DA-007` (`PROPUESTA`): `KafkaConfig` debe tomar el bootstrap desde la configuración (Alternativa A con `@Value`).
- Nota cruzada: `api-gateway` requiere `DA-001` (fallback multi-método).

## Decisiones pendientes

- Aprobar `DA-007` y autorizar el cambio en `KafkaConfig.java`.
- Aprobar `DA-001` del gateway y autorizar el cambio en `FallbackController.java`.

## Autorización humana

El usuario autorizó explícitamente: **"Sí, ambos"** (aplicar `DA-007` y `DA-001`).

## Cambios aplicados

- `infrastructure/config/KafkaConfig.java`: bootstrap desde `spring.kafka.bootstrap-servers` (vía `@Value` con default `localhost:9092`).
- (Cross-repo) `ticketflow-api-gateway/.../FallbackController.java`: `@GetMapping` → `@RequestMapping` (`DA-001` del gateway).

## Comandos ejecutados

- `docker compose up -d --build inventory-service api-gateway`.
- `docker logs ticketflow-inventory-service`.
- `curl` a `POST http://localhost:8080/reservations` y `POST http://localhost:8080/fallback/inventory` (con JWT).
- `docker exec ticketflow-kafka /opt/kafka/bin/kafka-topics.sh ... --list`.
- `docker exec ticketflow-kafka .../kafka-console-consumer.sh --topic reservation-created --from-beginning`.
- `docker exec ticketflow-inventory-db psql -U postgres -d inventory -c "select ... from reservations;"`.

## Pruebas realizadas y resultado

- **`POST /reservations` (con token):** `HTTP 200` en **0.075 s**; `{"id":7,"eventId":1,"locality":"VIP","quantity":2,"status":"CONFIRMED"}` (antes: `405` en 3.04 s).
- **Kafka:** productor conectado (`Cluster ID: 5L6g3nShT-eMCtK--X86sw`); topic `reservation-created` creado; mensajes ids 6 y 7 publicados; sin `Rebootstrapping with [localhost:9092]`.
- **BD:** filas visibles en `reservations` (id 7 = VIP/CONFIRMED).
- **Fallback (cross-repo):** `POST /fallback/inventory` (con token) → `503` (antes `405`).

## Pendientes

- `DA-001` (DA-001) del gateway: cierre de su documentación en el repo `api-gateway`.

## Estado

IMPLEMENTADO

# DA-007

## ID

DA-007

## Título

Configurar el bootstrap de Kafka del productor desde `spring.kafka.bootstrap-servers` (eliminar el `localhost:9092` hardcodeado)

## Contexto

- `docs_ia/contexto/arquitectura/arquitectura.yml` / `arquitectura_mircroservicio_inventario.md`: Inventario publica el evento de reserva confirmada a Kafka. `arquitectura.md` define la mensajería asíncrona con Kafka (KRaft).
- Gobernanza: la configuración debe provenir de `application.properties`/entorno, no de literales en el código.
- Estado verificado del repositorio:
  - `infrastructure/config/KafkaConfig.java` crea un `ProducerFactory` con `bootstrap.servers` **hardcodeado** a `"localhost:9092"`, ignorando `spring.kafka.bootstrap-servers`.
  - `application.properties` ya define `spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}` (más serializadores), pero no se usa por el punto anterior.
  - `ticketflow-api-gateway/docker-compose.yml` inyecta `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092` en `inventory-service`.
  - `KafkaReservationEventPublisher.publish(...)` hace `kafkaTemplate.send(...).join()` (bloqueante; introducido en DA-005).
- **Síntoma observado (2026-10-14):** el contenedor `inventory-service` intentaba conectarse a `localhost:9092` y el productor reintentaba indefinidamente (`Rebootstrapping with [localhost/127.0.0.1:9092] ... Connection to node -1 could not be established`). Como `publish` bloquea con `.join()`, la petición `POST /reservations` no terminaba; el circuit breaker del gateway (TimeLimiter 3 s) cortaba y reenviaba al fallback, que devolvía **405** (ver DA-001 del gateway).

## Problema

El productor Kafka no puede conectar dentro de Docker porque el bootstrap está fijado a `localhost:9092` (que dentro del contenedor es el propio contenedor). Además, esto acopla el servicio a un valor no configurable, incumpliendo la práctica de externalizar configuración.

## Alternativas consideradas

### Alternativa A — Leer `spring.kafka.bootstrap-servers` con `@Value` (recomendada)

En `KafkaConfig.producerFactory(...)`, inyectar `@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers` y usarlo en `ProducerConfig.BOOTSTRAP_SERVERS_CONFIG`. Cambio mínimo; conserva el `ProducerFactory`/`KafkaTemplate<String, Reservation>` tipados.

### Alternativa B — Usar `KafkaProperties` de Spring Boot

Construir el `ProducerFactory` desde `KafkaProperties.buildProducerProperties(...)`. Más idiomático, pero en Boot 4 la firma requiere `SslBundles` y añade complejidad; innecesario para este caso.

### Alternativa C — Eliminar `KafkaConfig` y usar la autoconfiguración

Spring Boot crearía `KafkaTemplate<Object, Object>`; el `@Component` que pide `KafkaTemplate<String, Reservation>` cambiaría de tipos y se pierde el tipado fuerte. Más invasivo.

### Alternativa D — Cambiar el compose para exponer Kafka en `localhost`

No aplica: `localhost` dentro del contenedor no es el host; además Kafka **no** publica puertos al host en el compose.

## Recomendación

**Adoptar la Alternativa A.** Cambios propuestos (**aún no aplicados**):

- `infrastructure/config/KafkaConfig.java`: inyectar `@Value("${spring.kafka.bootstrap-servers:localhost:9092}")` y usar ESE valor en `BOOTSTRAP_SERVERS_CONFIG` en lugar del literal `"localhost:9092"`.

## Justificación

1. Es la causa raíz del bloqueo de `POST /reservations` en Docker (el productor nunca conectaba).
2. Externaliza la configuración (permite `kafka:9092` en Docker y `localhost:9092` en local).
3. Cambio mínimo y sin impacto en contrato, dominio ni endpoints.
4. Elimina el acoplamiento a un literal, alineado con la gobernanza.

## Ventajas

- El productor usa la misma configuración que el resto de propiedades Kafka.
- Funciona en Docker (`kafka:9092`) y en local (`localhost:9092`) sin tocar código.

## Desventajas / costos

- Cambio de código en `infrastructure/config` (requiere autorización).
- No resuelve por sí solo el 405 del fallback del gateway (se aborda en el repo del gateway).

## Impacto

- `infrastructure/config/KafkaConfig.java` (1 archivo).
- **No** cambia: endpoints, contrato HTTP, dominio, topic (`reservation-created`), ni el modo bloqueante de `publish` (`.join()`), que con un broker sano se resuelve en milisegundos.

## Estado

APROBADA (2026-10-14)

> Autorizada explícitamente por el usuario ("Sí, ambos"). Trazabilidad en `docs_ia/prompts/P-012-fix-kafka-bootstrap-y-fallback-405.md`.

## Implementación (2026-10-14)

- `infrastructure/config/KafkaConfig.java`: `producerFactory(...)` recibe `@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers` y lo usa en `ProducerConfig.BOOTSTRAP_SERVERS_CONFIG` (reemplaza el literal `"localhost:9092"`).

Verificado (2026-10-14):

- Contenedores reconstruidos (`docker compose up -d --build inventory-service api-gateway`): `Started ServiceApplication in 36.577 seconds`.
- `POST http://localhost:8080/reservations` (con JWT) → **HTTP 200** en **0.075 s** → `{"id":7,"eventId":1,"locality":"VIP","quantity":2,"status":"CONFIRMED"}`.
- El productor conecta al clúster (`Cluster ID: 5L6g3nShT-eMCtK--X86sw`); ya **no** aparece `Rebootstrapping with [localhost/127.0.0.1:9092]`.
- Topic `reservation-created` creado; mensajes publicados (ids 6 y 7); filas presentes en `reservations`.

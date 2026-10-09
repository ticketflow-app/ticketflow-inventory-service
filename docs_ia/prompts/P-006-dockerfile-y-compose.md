# P-006

## Identificador del prompt

P-006

## Fecha

2026-10-09

## Objetivo

Registrar la autorización humana y la implementación del **Dockerfile** de `inventory-service` y su incorporación al `docker-compose.yml` del `api-gateway` (stack local completo: servicio + sus dependencias).

## Prompt completo utilizado

```text
"Ahora quiero que crees el dockerfile y lo agregues al docker-compose"
```

## Archivos de contexto consultados

- `.clinerules` (§2 decisiones, §4 documentación, §7 herramientas, §8 autorización, §10 trazabilidad).
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§12 Docker: el microservicio debe poder ejecutarse con Docker junto con Redis, PostgreSQL y Kafka).
- `dockerfile` de `ticketflow-eureka-server`, `ticketflow-user-service` y `ticketflow-api-gateway` (patrón a replicar).

## Archivos del proyecto analizados

- `pom.xml` (verificación de `spring-boot-maven-plugin` para generar el jar ejecutable).
- `src/main/resources/application.properties` (propiedades `server.port`, `spring.r2dbc.*`, `spring.data.redis.*`, `spring.kafka.*`, `eureka.*`).

## Resumen de la respuesta generada

- Se creó `dockerfile` (build multi-stage: `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-jammy`), `EXPOSE 8082`, replicando el patrón de los demás servicios.
- Se añadió el servicio `inventory-service` al `docker-compose.yml` del gateway, con variables de entorno que sobreescriben las propiedades para el entorno Docker (`SPRING_R2DBC_URL` a la BD `inventory`, `SPRING_DATA_REDIS_HOST`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `EUREKA_DEFAULT_ZONE`), puerto `8082` y `depends_on` (eureka-server, postgres saludable, redis, kafka).

## Autorización recibida

- El usuario solicitó explícitamente crear el Dockerfile y agregarlo al docker-compose. El `.clinerules` §8 exige autorización humana para cambios de infraestructura; dicha solicitud constituye la autorización.

## Archivos de otros repositorios modificados (autorizado explícitamente por el usuario)

- `ticketflow-api-gateway/docker-compose.yml`:
  - se agregó el servicio `inventory-service`;
  - se agregó un servicio `kafka` (`apache/kafka:3.9.1`, modo KRaft single-node) porque el compose no lo tenía y es una dependencia declarada (§12);
  - se montó `./postgres-init` en el servicio `postgres` para crear la BD `inventory`.
- `ticketflow-api-gateway/postgres-init/01-create-inventory-db.sql` (nuevo).

## Comandos ejecutados

- `docker compose -f docker-compose.yml config --quiet` (en `ticketflow-api-gateway`).

## Pruebas realizadas y resultado

- **`docker compose config`:** válido (sin errores de sintaxis ni de esquema).
- **No** se construyó la imagen ni se levantó el stack end-to-end en esta interacción.

## Propuestas realizadas por la IA

- Opción de alcance (Kafka + BD `inventory`, solo BD, o solo el servicio). El usuario no respondió a la pregunta; se implementó la opción recomendada (Kafka + BD `inventory`) por ser la que deja el stack funcional.

## Observaciones

- El script `postgres-init/01-create-inventory-db.sql` solo se ejecuta si el volumen `postgres_data` está **vacío** (primera inicialización). Si el volumen ya existe, la BD `inventory` no se crea automáticamente: recrear el volumen (`docker compose down -v`) o crearla a mano (`docker exec ticketflow-user-db psql -U postgres -c "CREATE DATABASE inventory;"`).
- No se define DDL para las tablas de `inventory` (R2DBC no las crea). Es una condición **pre-existente**, ajena a este cambio.
- Kafka se configura con listener interno (`kafka:9092`); no se publica puerto al host (los servicios del compose lo consumen por nombre de servicio).

## Decisiones pendientes

- Ninguna.

## Estado

IMPLEMENTADO

# DA-010 — Métricas Prometheus en inventario para el informe de carga

## Estado

**APROBADA** (autorización humana del 2026-11-21: "Fase A + B"). Implementada el
2026-11-21 en el working tree, sin commit ni push; registrada en `P-022`.

## Contexto

El plan de pruebas de carga (`ticketflow-load-test/docs_ia/contexto/plan_de_pruebas_carga_local.md`,
R5 y §5) marca como no medibles la CPU/RAM detallada por servicio, el *cache hit* y el *consumer
lag*. El informe del 15-Nov mide CPU y RAM por contenedor con `docker stats`, latencia y acierto
con k6, y el invariante anti-sobreventa, pero no puede explicar **dentro** de inventario de dónde
vienen los picos (GC, Redis, Kafka, pool R2DBC). Tras DA-009 apareció además un consumidor Kafka
y un job de limpieza que conviene observar.

## Problema

- `/actuator/prometheus` no existe en inventario: Actuator está en el `pom.xml`, pero falta el
  registry de Prometheus.
- Prometheus solo se auto-scrapea (`prometheus.yml` del gateway), así que no hay series de los
  servicios.
- Sin series de servicio, el informe solo puede inferir la causa de los picos de latencia.

## Alternativas consideradas

1. **Solo `docker stats` + k6 (lo actual).** Sin dependencias nuevas, suficiente para acierto,
   RAM, CPU y no sobreventa. No permite atribuir latencia interna ni ver lag del consumidor.
2. **`micrometer-registry-prometheus` + scrape de inventario (seleccionada).** Añade una
   dependencia ya gestionada por Spring Boot, expone métricas JVM/HTTP/Redis/Kafka y contadores
   propios. Coste: una dependencia Maven y una sección en `prometheus.yml`.
3. **cAdvisor + dashboard Grafana.** Da CPU/RAM por proceso, pero es más infraestructura y queda
   fuera del alcance aprobado (Fase C).

## Recomendación

Aplicar la alternativa 2. No se instala nada fuera de Maven: la versión la resuelve Spring Boot
4.1.1. Catálogo ya expone `/actuator/prometheus`, así que su scrape job se añade sin tocar ese
servicio.

## Ventajas

- El siguiente informe podrá contrastar `http.server.requests` de catálogo antes/después de DA-009.
- `jvm_gc_pause`, heap y hilos permiten confirmar o descartar la hipótesis de GC en los picos de
  hasta 2 914 ms que el informe anterior solo infería.
- Contadores propios: `catalog.events.published.processed`, `catalog.events.published.duplicates`,
  `catalog.events.cancelled.processed`, `catalog.events.cancelled.duplicates`,
  `catalog.events.cleanup.runs` y `catalog.events.cleanup.events.removed`.

## Desventajas / riesgos

- Nueva dependencia Maven (siempre con autorización humana, ya concedida).
- Más series en Prometheus; en el entorno local con 5 s de intervalo el impacto es despreciable.
- No mejora retroactivamente el informe del 15-Nov: solo prepara el siguiente.

## Impacto

- `pom.xml`: `io.micrometer:micrometer-registry-prometheus`.
- `application.properties`: exposición de `health,info,prometheus`, exportación activada y tag
  `application=ticketflow-inventory-service`.
- `CatalogEventConsumer` y `ExpiredEventCleanup`: inyección de `MeterRegistry` y contadores.
- `ticketflow-api-gateway/prometheus.yml`: jobs `inventory-service` y `catalog-service` (ver
  `DA-003` de ese repo y `P-009`).

Sin cambios en el contrato HTTP ni en el comportamiento de DA-009.

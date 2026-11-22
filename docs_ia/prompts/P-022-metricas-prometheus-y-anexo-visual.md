# P-022 — Métricas Prometheus y anexo visual del informe de carga

- **Fecha:** 2026-11-21
- **Estado:** IMPLEMENTADO
- **Relacionado:** `DA-010` (APROBADA), `DA-009`, `P-021`, `ticketflow-load-test` (`P-001`),
  `ticketflow-api-gateway` (`DA-003`, `P-009`)

## Objetivo

Hacer el informe de pruebas de carga más completo y más fácil de exponer: métricas reales de
servicio, mejor explicación de los datos y tres formatos visuales (Mermaid, SVG e HTML).

## Prompt del usuario

> "si quiero que trabajes en eso, pero solo con la intención de que el informe de las pruebas sea
> aun mas completo y además mejora la forma en que se explican los datos en los informes, y ademas
> quiero poder ver de forma visual mas facil las pruebas, no se si mediante un diagrama de barras o
> en una imagen que explique a que microservio y cuantos paquetes por segundo se envian y durante
> cuantos segundos, o alguna otra forma que me propongas, la idea es qeu sea facil de exponer"

Autorización inicial: **Fase A + B** y **las tres formas visuales**, sin ejecutar pruebas de carga
y sin commit/push. Autorización posterior del usuario ("sigue con eso que falta"): ejecutar la
corrida de verificación que completó la tabla pendiente.

## Documentos consultados

- `AGENTS.md` de la raíz, de inventory y del gateway; `.clinerules` de inventory.
- `docs_ia/decisiones/DA-009`, `DA-010` y sus README.
- `ticketflow-load-test/docs_ia/contexto/plan_de_pruebas_carga_local.md` (R5, líneas 271 y 285).
- `ticketflow-load-test/docs_ia/informes/informe-pruebas-carga-2026-11-15.md`.
- `ticketflow-api-gateway/prometheus.yml` y `docker-compose.yml`.

## Archivos analizados

- `pom.xml` de inventory: Actuator presente, `micrometer-registry-prometheus` ausente.
- `CatalogEventConsumer.java`, `ExpiredEventCleanup.java`, `application.properties`.
- 27 archivos `results/sessions/**/run.json` del repo de carga.

## Cambios aplicados en este repo

- `pom.xml`: nueva dependencia `io.micrometer:micrometer-registry-prometheus` (versión gestionada
  por Spring Boot). Justificación y alternativas en `DA-010`.
- `application.properties`: `management.endpoints.web.exposure.include=health,info,prometheus`,
  exportación Prometheus activada y tag de aplicación.
- `CatalogEventConsumer`: `MeterRegistry` inyectado y contadores de publicados, cancelados y
  duplicados.
- `ExpiredEventCleanup`: contadores de ejecuciones y eventos eliminados.
- Documentos: `DA-010`, este `P-022` y los README de decisiones y prompts.

## Cambios fuera de este repo (autorizados)

- `ticketflow-api-gateway/prometheus.yml`: jobs de scrape para `inventory-service` y
  `catalog-service`; documentado en `DA-003` y `P-009` de ese repo.
- `ticketflow-api-gateway/docker-compose.yml`: se actualizó el comentario obsoleto sobre la URL
  de catálogo (ahora llega por Kafka).
- `ticketflow-load-test`: generador `scripts/generate-visual-report.js`, anexo Markdown con
  Mermaid, `visual/resumen-carga.svg`, `visual/resumen-carga.html` y `P-001`.

## Comandos ejecutados

```powershell
docker compose up -d --build --force-recreate inventory-service prometheus
node scripts/generate-visual-report.js   # desde ticketflow-load-test
[xml](Get-Content docs_ia/informes/visual/resumen-carga.svg -Raw)
.\scripts\run-all.ps1 -Scenarios i1,i2,i3 -Profile local -Label post-da009   # desde ticketflow-load-test
```

## Pruebas y resultados

- Build de inventory: **BUILD SUCCESS** con la nueva dependencia (Java 25 en Docker).
- `GET http://localhost:8082/actuator/prometheus`: expone `catalog_events_published_processed_total`,
  `catalog_events_cancelled_processed_total`, `catalog_events_cleanup_runs_total`,
  `http_server_requests_seconds_count` y `jvm_gc_pause_seconds_count`.
- Smoke Kafka con un evento sintético publicado y cancelado: contadores `1.0` y `1.0`.
- Prometheus API `/api/v1/targets?state=active`: `prometheus`, `catalog-service` e
  `inventory-service` en estado **`up`**.
- Generador visual: 30 ejecuciones en 6 sesiones; sesiones destacadas
  `20261121-215331-post-da009` (inventario directo) y `20261115-093525` (gateway).
- SVG validado como XML; HTML abierto en el panel de revisión.
- Corrida de verificación autorizada después (`sigue con eso que falta`): I1, I2 e I3, **3/3 PASS**.
  - CPU media de catálogo en I1: 209,54 % → 1,21 % (9 muestras y menor caudal: 3.317,5 → 2.098,6
    req/s); señal direccional, no benchmark a igual carga.
  - `http.server.requests` de catálogo: **0 GET** a `/localidades` en 321.362 lecturas de
    disponibilidad de inventario.
  - p95 I1/I2: 18,7/3,9 → 29,3/10,2 ms; **no hubo mejora de latencia**.
  - Invariante I3: PASS, 100 reservas y stock 0; 0,00 % de peticiones fallidas.

## Pendientes

- Fase C (dashboard Grafana/cAdvisor) no autorizada; queda fuera.
- Arreglo de la suite heredada de tests y `DA-008` siguen pendientes de autorización separada.

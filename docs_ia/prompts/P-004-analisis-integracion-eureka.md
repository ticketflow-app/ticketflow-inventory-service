# P-004

## Identificador del prompt

P-004

## Fecha

2026-10-09

## Objetivo

Analizar y proponer la integración de `inventory-service` con el descubrimiento de servicios (Eureka), para que se registre junto al resto del ecosistema TicketFlow. La interacción se limitó a análisis y propuesta documental: no se modificó `pom.xml`, `application.properties` ni código, porque el descubrimiento de servicios no está definido en `docs_ia/contexto` y `.clinerules` (§7, §8) exige detener la implementación y presentar la decisión.

## Prompt completo utilizado

```text
en ticketflow, como necesito que hagas que inventoory service se conecte a eureka para que ya queden todos juntos
```

## Archivos de contexto consultados

- `.clinerules` (§1 fuente de contexto, §2 decisiones, §3 código, §4 documentación, §6 dependencias, §7 herramientas, §8 autorización, §9 comunicación, §10 trazabilidad).
- `docs_ia/contexto/arquitectura/arquitectura_mircroservicio_inventario.md` (§1–§12).
- `docs_ia/contexto/arquitectura/arquitectura.md` (búsqueda de descubrimiento de servicios, balanceo y API Gateway).
- `docs_ia/contexto/arquitectura/desiciones_arquitectonicas.md` (ADR-001 a ADR-007).
- `docs_ia/decisiones/DA-002-...md` (formato de decisión y estilo de análisis de dependencias).

## Archivos del proyecto analizados

- `pom.xml`
- `src/main/resources/application.properties`
- `src/main/java/com/inventory/service/ServiceApplication.java`
- `src/main/java/com/inventory/service/infrastructure/config/SecurityConfig.java`
- `src/test/java/com/inventory/service/ServiceApplicationTests.java`

Referencias externas al repositorio (solo lectura, para comparar la convención):

- `ticketflow-user-service/pom.xml` (BOM de Spring Cloud 2025.1.3 y `spring-cloud-starter-netflix-eureka-client`).
- `ticketflow-user-service/src/main/resources/application.properties` (propiedades de Eureka).
- `ticketflow-api-gateway/src/main/resources/application.yml` (bloque `eureka` y rutas `lb://`).

## Resumen de la respuesta generada

Se determinó que `inventory-service` no incluye Spring Cloud/Eureka, no define `server.port` (por defecto 8080, en conflicto con el gateway) y usa `spring.application.name=service`. El contexto autorizado no define descubrimiento de servicios, por lo que se detuvo la implementación y se elaboró la propuesta **DA-003**, que recomienda la Alternativa A: integrar el servicio con Eureka mediante Spring Cloud 2025.1.3, con nombre estable (`ticketflow-inventory-service`) y puerto propio (8082). No se aplicó ningún cambio de código, configuración ni dependencias.

## Propuestas realizadas por la IA

- **DA-003** (estado PROPUESTA): integración de `inventory-service` con Eureka (BOM Spring Cloud 2025.1.3 + `spring-cloud-starter-netflix-eureka-client`, propiedades de Eureka, nombre y puerto), pendiente de aprobación humana.
- Ajuste propuesto para las pruebas (`eureka.client.enabled=false`) y actualización documental del contexto, ambos a aprobar por separado.

## Decisiones pendientes

- Aprobar o rechazar DA-003.
- Confirmar el nombre de servicio en Eureka (`ticketflow-inventory-service` vs. `inventory-service`).
- Confirmar el puerto (8082 propuesto).
- Decidir si se añade la ruta `lb://...` en el `api-gateway` (fuera del alcance de DA-003).
- Decidir el tratamiento de `eureka.client.enabled` en las pruebas (`contextLoads`).

## Estado

PROPUESTA

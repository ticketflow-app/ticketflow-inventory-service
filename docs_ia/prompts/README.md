# Trazabilidad de uso de IA

Formato: `P-xxx-<titulo>.md`, consecutivo. Registrar toda interacciÃ³n relevante (incluso anÃ¡lisis sin cambios de cÃ³digo).

Cada registro incluye: identificador, fecha, objetivo, prompt completo, archivos de contexto consultados, archivos analizados, resumen de la respuesta, propuestas, decisiones pendientes y estado.

Estados: `ANALISIS` | `PROPUESTA` | `APROBADO` | `RECHAZADO` | `IMPLEMENTADO`.

| ID | Título | Estado |
|---|---|---|
| P-016 | Análisis del acoplamiento inventario → catálogo y de un CDN local | PROPUESTA |
| P-017 | Aclaración: qué información se toma del catálogo y qué aporta Kafka | PROPUESTA |
| P-018 | Aclaración: dónde viven las localidades y cómo funcionaría Kafka | PROPUESTA |
| P-019 | Aclaración: momento de publicación vs. momento de consulta | PROPUESTA |
| P-020 | Crecimiento de claves en Redis con el enfoque Kafka | PROPUESTA |
| P-021 | Implementación de DA-009: proyección local por Kafka y ciclo Redis | IMPLEMENTADO |
| P-022 | Métricas Prometheus y anexo visual del informe de carga | IMPLEMENTADO |

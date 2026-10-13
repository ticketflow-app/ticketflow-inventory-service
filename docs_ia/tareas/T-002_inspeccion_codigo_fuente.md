# T-002 — Inspección del código fuente

## 1. Información general

- **ID:** T-002
- **Estado:** PENDIENTE
- **Prioridad:** ALTA
- **Agente:** @reporter
- **Tipo:** ANÁLISIS
- **Tarea relacionada:** T-001

## 2. Objetivo

Inspeccionar el código fuente actual del microservicio Inventory para identificar qué está implementado, qué funciona y qué falta por desarrollar, utilizando evidencias verificables.

## 3. Contexto específico

La auditoría T-001 no pudo verificar el código fuente ni ejecutar las pruebas, por lo que sus conclusiones son incompletas.

Es necesario realizar una inspección directa antes de definir tareas de implementación.

- **Referencias relevantes:** `T-001_auditoria_estado_actual.md`, `../contexto/`, `pom.xml`, `src/main/` y `src/test/`.
- **Situación actual:** Por verificar mediante inspección directa.

## 4. Alcance

**Incluye:**
- Inspeccionar los archivos de código fuente, pruebas y configuración existentes.
- Identificar componentes implementados y sus responsabilidades.
- Revisar las dependencias declaradas y su uso aparente en el código.
- Ejecutar las comprobaciones de compilación y pruebas que sean posibles.
- Identificar funcionalidades pendientes, inconsistencias y errores verificables.
- Registrar evidencias que permitan proponer tareas posteriores.

**No incluye:**
- Modificar código, pruebas o configuraciones.
- Instalar dependencias o cambiar versiones.
- Modificar los documentos de contexto o las decisiones arquitectónicas.
- Implementar soluciones ni corregir errores.
- Crear tareas de implementación basadas en suposiciones.

**Archivos previsiblemente afectados:**
- `docs_ia/prompts/P-XXX_*.md`, para registrar la interacción.
- `docs_ia/reportes/`, únicamente si se requiere un informe adicional autorizado.

## 5. Criterios de aceptación

- [ ] Se inspeccionan directamente los archivos relevantes del proyecto.
- [ ] Se identifican los componentes encontrados y se indican sus rutas.
- [ ] Se distingue entre código existente, comportamiento verificado y aspectos pendientes de verificar.
- [ ] Se ejecutan las comprobaciones posibles y se registran sus resultados exactos.
- [ ] Se documentan los bloqueos sin asumir que un archivo o componente no existe por un fallo de acceso.
- [ ] Se presentan hallazgos concretos para revisión humana, sin implementar cambios.

## 6. Verificación

- **Comprobaciones o pruebas específicas:** Lectura de archivos, revisión de estructura y ejecución de comandos de compilación o pruebas cuando sea posible.
- **Evidencia esperada:** Rutas inspeccionadas, fragmentos relevantes, comandos ejecutados, resultados obtenidos y errores exactos.

## 7. Resultado

- **Resultado obtenido:** Pendiente.
- **Archivos afectados:** Pendiente.
- **Verificaciones realizadas:** Pendiente.
- **Bloqueos o hallazgos adicionales:** Pendiente.
- **Estado final:** PENDIENTE.
- **Siguiente paso:** Revisar los hallazgos y autorizar las tareas posteriores que correspondan.
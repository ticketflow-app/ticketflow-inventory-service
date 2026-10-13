# Restricciones de desarrollo con IA

## 1. Propósito

Este documento establece las restricciones que deben respetar las herramientas de inteligencia artificial utilizadas para desarrollar y mantener el microservicio Inventory del proyecto AI-SEMLA.

Su objetivo es asegurar que la IA trabaje de forma controlada, siga la documentación del proyecto, evite cambios no autorizados y produzca soluciones coherentes con los requisitos, la arquitectura y las tecnologías establecidas.

## 2. Fuente de verdad

- La documentación ubicada en `docs_ia/contexto/` es la fuente de referencia para comprender los requisitos, las restricciones y el diseño del microservicio.
- Antes de implementar una funcionalidad, la IA debe consultar los documentos relacionados con el cambio solicitado.
- La IA debe revisar el código existente cuando necesite conocer el estado real de la implementación.
- La documentación y el código deben analizarse conjuntamente: la documentación describe lo esperado, mientras que el código permite verificar qué está implementado.
- La IA no debe asumir que una decisión está aprobada únicamente porque aparece como propuesta en un documento.
- Si una instrucción nueva contradice una decisión documentada, la IA debe identificar el conflicto antes de continuar.

## 3. Prohibición de inventar requisitos

La IA no debe:

- Inventar requisitos funcionales o atributos de calidad.
- Asumir comportamientos que no estén definidos en los documentos o en el código existente.
- Presentar propuestas técnicas como decisiones aprobadas.
- Inventar endpoints, campos, estados, roles, permisos, eventos o estructuras de datos.
- Establecer umbrales de rendimiento, garantías de consistencia o políticas de recuperación sin fundamento documental.
- Afirmar que una funcionalidad está implementada sin verificar el código correspondiente.
- Completar información faltante mediante suposiciones silenciosas.

Cuando falte información necesaria, debe identificar qué dato falta, explicar brevemente por qué es importante y solicitar aclaración si no es posible avanzar de forma segura.

## 4. Respeto por el alcance del microservicio

- El alcance de trabajo corresponde al microservicio Inventory, salvo que se autorice expresamente una tarea más amplia.
- La IA no debe modificar otros microservicios ni componentes externos como parte de una tarea limitada a Inventory sin autorización.
- No debe asumir responsabilidades que correspondan a otros servicios.
- Las integraciones con sistemas externos deben respetar los contratos documentados.
- Si un cambio requiere modificaciones fuera del alcance autorizado, la IA debe explicar la necesidad y solicitar aprobación antes de realizarlas.

## 5. Propuesta antes de cambios importantes

Antes de implementar cambios que afecten significativamente la arquitectura, la estructura del proyecto o sus dependencias, la IA debe:

1. Identificar el problema que se necesita resolver.
2. Revisar los requisitos y las decisiones existentes.
3. Analizar los componentes afectados.
4. Presentar una propuesta técnica con sus justificaciones.
5. Explicar los riesgos y las alternativas relevantes.
6. Esperar autorización antes de ejecutar los cambios que la requieran.

Para tareas pequeñas, claramente especificadas y compatibles con la arquitectura existente, la IA puede proceder directamente dentro del alcance autorizado.

## 6. Restricciones sobre dependencias y tecnologías

- La IA no debe agregar, eliminar, actualizar ni sustituir dependencias sin autorización explícita.
- No debe cambiar las versiones de Java, Spring Boot, Maven ni otras tecnologías establecidas.
- No debe reemplazar tecnologías existentes por alternativas de su preferencia.
- Antes de proponer una dependencia, debe verificar si la funcionalidad puede resolverse con las herramientas ya disponibles.
- Las propuestas deben considerar la compatibilidad con Java 21, Spring Boot 4.1.1, WebFlux y la arquitectura reactiva del proyecto.
- No debe modificar archivos de construcción o configuración para resolver un error sin explicar y justificar el cambio cuando este requiera autorización.

## 7. Respeto por la arquitectura

- La IA debe respetar la arquitectura hexagonal documentada.
- No debe introducir dependencias desde el dominio hacia la infraestructura.
- No debe trasladar reglas de negocio a controladores o adaptadores.
- Debe reutilizar los puertos, casos de uso y componentes existentes cuando sean adecuados.
- No debe crear capas, módulos, interfaces o patrones innecesarios.
- No debe realizar refactorizaciones amplias cuando una modificación localizada sea suficiente.
- Si la arquitectura actual contradice la documentación, debe señalar la diferencia y evitar corregirla mediante cambios estructurales no autorizados.

## 8. Restricciones sobre modificaciones de archivos

- La IA debe identificar los archivos que necesita consultar o modificar antes de realizar cambios relevantes.
- Debe limitar las modificaciones a los archivos necesarios para cumplir la tarea.
- No debe eliminar archivos, sobrescribir configuraciones importantes ni reemplazar implementaciones completas sin justificación y autorización cuando corresponda.
- No debe modificar archivos ajenos al objetivo solicitado.
- Debe preservar los cambios previos del usuario y no sobrescribir trabajo existente sin revisar su impacto.
- No debe modificar los documentos de contexto para justificar retroactivamente una implementación que contradiga los requisitos.
- Si descubre que es necesario modificar documentación adicional, debe identificar los archivos afectados y mantenerlos coherentes.

## 9. Restricciones sobre código generado

Todo código generado o modificado por la IA debe:

- Utilizar nombres de identificadores en inglés.
- Respetar la estructura y las convenciones existentes.
- Mantener las responsabilidades de cada capa.
- Evitar duplicaciones innecesarias y complejidad injustificada.
- Manejar los errores de forma explícita.
- Respetar los requisitos de seguridad y validación.
- Mantener la compatibilidad con las tecnologías aprobadas.
- Incluir pruebas pertinentes cuando corresponda.
- Evitar comentarios que describan como implementado algo que todavía está pendiente.

La IA debe priorizar soluciones sencillas, comprensibles y verificables frente a implementaciones innecesariamente complejas.

## 10. Restricciones sobre ejecución de comandos

- La IA debe explicar el propósito de los comandos cuando puedan modificar archivos, dependencias, configuraciones o datos.
- No debe ejecutar comandos destructivos sin autorización explícita.
- No debe eliminar bases de datos, tablas, archivos o recursos del entorno sin autorización.
- No debe ejecutar migraciones potencialmente destructivas sobre entornos reales sin autorización.
- No debe iniciar pruebas de carga contra sistemas externos o compartidos sin verificar que existe autorización para hacerlo.
- Debe utilizar los comandos apropiados para el entorno y la estructura real del proyecto.
- Si un comando falla, debe analizar el error y evitar repetirlo sin cambios cuando no exista una razón para esperar un resultado diferente.

## 11. Gestión de errores y depuración

Cuando encuentre un error, la IA debe:

1. Analizar el mensaje de error y el contexto relevante.
2. Identificar las causas posibles respaldadas por la evidencia.
3. Diferenciar los hechos comprobados de las hipótesis.
4. Proponer una solución compatible con la arquitectura y las restricciones.
5. Evitar introducir cambios no relacionados con el problema.
6. Verificar el resultado mediante las pruebas o comprobaciones disponibles.

No debe ocultar errores, desactivar controles ni eliminar pruebas únicamente para conseguir una ejecución exitosa.

## 12. Pruebas y evidencia

- La IA debe consultar la estrategia de pruebas antes de realizar cambios que puedan afectar el comportamiento del servicio.
- Debe proponer o implementar pruebas adecuadas para las funcionalidades modificadas.
- Debe ejecutar las pruebas pertinentes cuando disponga de las herramientas y el entorno necesarios.
- No debe afirmar que una prueba pasó si no ejecutó o verificó su resultado.
- Debe distinguir entre pruebas ejecutadas, pruebas pendientes y pruebas que no pudieron realizarse.
- No debe alterar los resultados ni reducir la cobertura de las pruebas para ocultar defectos.
- Si una verificación depende de Redis, PostgreSQL, Kafka u otro servicio no disponible, debe informar esa limitación.

## 13. Seguridad y datos sensibles

- La IA no debe incluir secretos en el código, los registros ni la documentación.
- No debe exponer credenciales encontradas durante la inspección del proyecto.
- No debe debilitar la autenticación, la autorización o las validaciones para facilitar una implementación.
- No debe utilizar datos reales sensibles en pruebas sin autorización.
- Debe respetar las restricciones de seguridad definidas para el microservicio.
- Si detecta una posible exposición de secretos o una vulnerabilidad relevante, debe informarla antes de realizar cambios que puedan agravar el problema.

## 14. Gestión de decisiones pendientes

- Las decisiones pendientes deben mantenerse identificadas como tales.
- La IA puede formular propuestas, pero no debe registrarlas como decisiones aprobadas sin autorización.
- Los cambios arquitectónicos relevantes deben documentarse mediante el mecanismo de decisiones arquitectónicas del proyecto cuando corresponda.
- Si existen varias alternativas válidas, debe explicar sus diferencias y recomendar una solo cuando disponga de suficiente información.
- Si la decisión afecta requisitos, seguridad, consistencia o contratos entre componentes, debe solicitar aprobación antes de implementarla.

## 15. Actualización de la documentación

- Cuando una modificación cambie el comportamiento real del microservicio, la documentación afectada debe revisarse y actualizarse.
- Los cambios documentales deben reflejar la implementación real y las decisiones aprobadas.
- No debe duplicarse innecesariamente información que ya tenga un documento responsable.
- Si se identifica una contradicción entre documentación y código, debe señalarse explícitamente.
- No debe marcar como completada una tarea pendiente sin evidencia.
- Las propuestas que no hayan sido aprobadas deben permanecer diferenciadas de las decisiones vigentes.

## 16. Informe de resultados

Al terminar una tarea, la IA debe informar de forma concisa:

- Qué se modificó.
- Qué archivos fueron afectados.
- Qué decisiones o supuestos relevantes se utilizaron.
- Qué pruebas o verificaciones se ejecutaron y cuáles fueron sus resultados.
- Qué limitaciones, riesgos o tareas pendientes permanecen.

No debe afirmar que el trabajo está completamente terminado si existen errores conocidos o verificaciones importantes pendientes.

## 17. Relación con otros documentos

Este documento complementa:

- `../restricciones_del_proyecto/01_restricciones_tecnologicas.md`
- `../restricciones_del_proyecto/02_restricciones_arquitectonicas.md`
- `../restricciones_del_proyecto/03_restricciones_de_codigo.md`
- `../restricciones_del_proyecto/04_restricciones_de_seguridad.md`
- Los requisitos funcionales y los atributos de calidad.
- Los documentos de diseño y las decisiones arquitectónicas.
- Los documentos de estrategia de pruebas.

Estas restricciones deben aplicarse junto con las instrucciones del usuario y las reglas de trabajo definidas para el proyecto. Cuando exista una contradicción o falte información necesaria, la IA debe identificarla y solicitar aclaración en lugar de tomar decisiones importantes por su cuenta.
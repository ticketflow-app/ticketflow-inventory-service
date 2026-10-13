# T-001 — Auditoría del estado actual del proyecto

## 1. Información general

- **ID:** T-001
- **Estado:** PENDIENTE
- **Prioridad:** ALTA
- **Tipo:** AUDITORÍA
- **Agente responsable:** @reporter
- **Tarea relacionada:** No aplica
- **Registro de prompt:** P-XXX (asignar al ejecutar la tarea)

## 2. Objetivo

Inspeccionar el estado actual del microservicio Inventory y compararlo con la documentación autorizada del proyecto para determinar:

- Qué componentes y funcionalidades están implementados.
- Qué partes parecen correctas y cumplen con las especificaciones.
- Qué errores, incumplimientos o riesgos técnicos existen.
- Qué requisitos y componentes están pendientes.
- Qué pruebas existen y qué aspectos se han verificado realmente.
- En qué orden conviene continuar el desarrollo mediante microtareas pequeñas y revisables.

El resultado debe permitir seleccionar la siguiente tarea de desarrollo con base en evidencia, sin asumir que el proyecto está completo o incompleto antes de inspeccionarlo.

## 3. Contexto y referencias

- **Documentos de contexto aplicables:** Documentación existente en `docs_ia/contexto/`.
- **Decisiones arquitectónicas relacionadas:** Archivos existentes en `docs_ia/decisiones/`, si los hay.
- **Archivos existentes relevantes:** Código fuente, `pom.xml`, configuración, pruebas y demás archivos del proyecto Inventory.
- **Requisitos relacionados:** Requisitos funcionales, atributos de calidad, diseño, arquitectura, estrategia de pruebas y restricciones documentadas.

Antes de iniciar, el agente debe leer `.clinerules`, esta tarea y las instrucciones aplicables a `@reporter`.

Debe inspeccionar los documentos disponibles en lugar de asumir que todos los componentes descritos están implementados.

## 4. Alcance

### Incluye

- Inspeccionar la estructura real del proyecto.
- Revisar el código fuente y la configuración existente.
- Comparar la implementación con los documentos de contexto y las decisiones aprobadas.
- Revisar las dependencias declaradas en Maven y su uso aparente.
- Identificar funcionalidades implementadas, parciales, pendientes o no verificables.
- Examinar las pruebas existentes y la evidencia disponible.
- Identificar problemas técnicos y riesgos, indicando su gravedad y evidencia.
- Recomendar un orden de desarrollo mediante microtareas independientes.
- Proponer una primera microtarea de desarrollo o corrección.
- Registrar la interacción en `docs_ia/prompts/` conforme a las reglas del proyecto.

### No incluye

- Corregir errores encontrados.
- Implementar funcionalidades.
- Refactorizar código.
- Modificar o crear pruebas.
- Modificar `pom.xml`, dependencias o configuración.
- Cambiar la arquitectura.
- Modificar documentos de contexto o decisiones existentes.
- Instalar herramientas o dependencias.
- Ejecutar operaciones destructivas.
- Aprobar decisiones técnicas o autorizar tareas futuras.

La auditoría debe limitarse a inspeccionar, analizar y documentar. Si para verificar algún aspecto fuera necesario modificar archivos o realizar una operación que requiera autorización, debe indicarlo y esperar autorización.

## 5. Archivos y recursos

- **Archivos que se pueden consultar:** `.clinerules`, `docs_ia/contexto/`, `docs_ia/decisiones/`, `docs_ia/tareas/`, `docs_ia/prompts/`, `pom.xml`, código fuente, archivos de configuración y pruebas existentes.
- **Archivos que se pueden crear o modificar:** Únicamente el nuevo registro de interacción en `docs_ia/prompts/`, de acuerdo con las reglas de trazabilidad. El informe de auditoría se debe presentar al usuario; no se debe guardar en otra ubicación sin autorización.
- **Archivos que no se pueden modificar:** Código de producción, pruebas, `pom.xml`, dependencias, configuración, infraestructura, documentos de contexto, decisiones existentes, reglas de Cline y archivos ajenos al registro de trazabilidad autorizado.
- **Dependencias, herramientas o servicios involucrados:** Maven, Java y las tecnologías que ya estén declaradas o configuradas en el proyecto. No se deben incorporar herramientas nuevas.

Si no existe alguna carpeta o archivo esperado, debe reportarlo sin crear una estructura adicional, salvo el registro de prompt autorizado.

## 6. Procedimiento

1. Leer `.clinerules`, esta tarea y las instrucciones del agente `@reporter`.

2. Revisar la estructura de `docs_ia/`, la documentación de contexto, las decisiones existentes, el backlog y los registros de prompts disponibles.

3. Inspeccionar la estructura real del microservicio Inventory, identificando módulos, paquetes, clases, interfaces, adaptadores, casos de uso, configuración y pruebas.

4. Revisar `pom.xml` y los archivos de configuración para identificar las tecnologías declaradas, las versiones y las dependencias aparentemente utilizadas.

5. Comparar los componentes encontrados con los requisitos, la arquitectura, los atributos de calidad, las restricciones y las decisiones aprobadas.

6. Clasificar cada hallazgo como:
   - Implementado y aparentemente correcto.
   - Implementado con posibles problemas.
   - Parcialmente implementado.
   - No implementado.
   - No verificable con la evidencia disponible.

7. Para cada problema relevante, indicar su ubicación, evidencia, impacto potencial y prioridad recomendada.

8. Examinar las pruebas existentes. Diferenciar entre pruebas presentes, pruebas ejecutadas y resultados comprobados. No declarar que una funcionalidad funciona únicamente porque exista una clase o una prueba.

9. Identificar contradicciones, documentación ausente, decisiones pendientes y aspectos que requieren aclaración humana.

10. Proponer un backlog ordenado de microtareas. Cada propuesta debe tener un objetivo concreto, alcance limitado, archivos previsiblemente afectados, criterios de aceptación y pruebas esperadas.

11. Recomendar una única primera microtarea posterior a la auditoría y justificar por qué debe realizarse antes que las demás.

12. Registrar el prompt completo y la información de trazabilidad en `docs_ia/prompts/`, respetando la plantilla y la numeración existente.

13. Presentar el informe final sin modificar código ni ejecutar cambios correctivos.

## 7. Restricciones

- `docs_ia/contexto/` es la fuente autorizada para los requisitos y las especificaciones del proyecto.
- Las decisiones arquitectónicas propuestas no se consideran aprobadas.
- No inventar requisitos, resultados de pruebas, archivos, comandos ni comportamientos.
- Diferenciar hechos observados, inferencias, recomendaciones y aspectos no verificados.
- No interpretar la ausencia de evidencia como prueba definitiva de que una funcionalidad no existe.
- No modificar archivos fuera del alcance autorizado.
- No agregar, eliminar ni actualizar dependencias.
- No realizar cambios de arquitectura ni de configuración.
- No ejecutar comandos destructivos ni operaciones que puedan alterar datos.
- No ampliar el alcance para solucionar los problemas encontrados.
- No modificar el historial de prompts ni sobrescribir decisiones anteriores.
- Si existe un conflicto entre documentación y código, registrar ambas evidencias e indicar que requiere revisión.
- Si una actividad exige autorización adicional, detener esa actividad y solicitarla.

## 8. Criterios de aceptación

- [ ] Se inspeccionó la estructura real del proyecto.
- [ ] Se revisó la documentación de contexto disponible.
- [ ] Se revisaron las decisiones existentes y el backlog, cuando estén presentes.
- [ ] Se identificaron los componentes y funcionalidades encontrados, con evidencia.
- [ ] Se comparó el código con los requisitos y decisiones aprobadas.
- [ ] Se documentaron los aspectos correctos y los problemas identificados.
- [ ] Se clasificaron los hallazgos por gravedad o prioridad.
- [ ] Se evaluó la cobertura de las pruebas y se diferenciaron resultados ejecutados de resultados no verificados.
- [ ] Se identificaron las diferencias entre documentación e implementación.
- [ ] Se propuso un backlog de microtareas con alcance limitado.
- [ ] Se recomendó una única primera microtarea posterior a la auditoría.
- [ ] Se registró el prompt completo en `docs_ia/prompts/`.
- [ ] No se modificaron código, pruebas, dependencias ni configuración.

### Criterios específicos

- [ ] El informe permite entender qué está implementado sin confundirlo con lo que solo está documentado.
- [ ] Cada problema relevante incluye evidencia o una explicación de por qué no pudo verificarse.
- [ ] Las microtareas propuestas pueden revisarse y probarse de forma independiente, en la medida de lo posible.
- [ ] La primera microtarea recomendada tiene criterios de aceptación concretos y no presupone autorizaciones que todavía no existen.

## 9. Pruebas y verificación

- **Pruebas requeridas:** Inspección de las pruebas existentes y evaluación de su relación con los requisitos documentados.
- **Comandos de verificación:** Por definir según el entorno encontrado. No ejecutar comandos que requieran instalaciones, cambios de configuración, modificaciones de archivos o acceso a servicios externos sin autorización.
- **Resultado esperado:** Un diagnóstico verificable del estado del proyecto.
- **Resultado obtenido:** Pendiente.
- **Evidencia:** Rutas de archivos, clases, métodos, configuraciones, pruebas y resultados disponibles.
- **Limitaciones de verificación:** Deben documentarse las pruebas que no se hayan ejecutado y las funcionalidades cuyo comportamiento no pueda confirmarse.

No es obligatorio ejecutar toda la batería de pruebas durante esta auditoría. Si una prueba requiere preparar infraestructura o modificar el entorno, debe registrarse como una limitación o una actividad propuesta.

## 10. Resultado de la ejecución

- **Archivos consultados:** Pendiente.
- **Archivos creados:** Pendiente.
- **Archivos modificados:** Ninguno autorizado fuera del registro de prompt.
- **Comandos ejecutados:** Pendiente.
- **Pruebas ejecutadas:** Pendiente.
- **Resultado:** Pendiente.
- **Problemas encontrados:** Pendiente.
- **Decisiones pendientes de autorización:** Pendiente.

El agente debe completar estos campos al finalizar y presentar el informe de auditoría al usuario.

## 11. Cierre

- **Estado final:** PENDIENTE.
- **Criterios de aceptación cumplidos:** Pendiente.
- **Requiere revisión humana:** Sí.
- **Siguiente tarea recomendada:** Pendiente.
- **Observaciones:** La auditoría no autoriza la implementación de las soluciones propuestas.

La tarea solo podrá considerarse completada cuando se presente el informe con evidencia suficiente y se cumplan los criterios aplicables. La aceptación del informe por parte del usuario no implica la aprobación automática de las microtareas propuestas.
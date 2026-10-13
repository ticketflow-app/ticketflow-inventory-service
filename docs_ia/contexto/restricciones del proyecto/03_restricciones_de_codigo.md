# Restricciones de código

## 1. Propósito

Este documento establece las restricciones y convenciones que deben respetarse al escribir, modificar y mantener el código del microservicio Inventory del proyecto AI-SEMLA.

Su objetivo es mantener un código legible, consistente, verificable y alineado con los requisitos, la arquitectura hexagonal y las tecnologías establecidas.

## 2. Convenciones de nomenclatura

- Los nombres de clases, interfaces, métodos, variables, paquetes y demás identificadores del código deben estar en inglés.
- Se deben utilizar nombres descriptivos que representen la responsabilidad o el propósito del elemento.
- Las clases deben seguir la convención PascalCase.
- Los métodos y variables deben seguir la convención camelCase.
- Las constantes deben seguir la convención definida por el proyecto; para constantes estáticas finales, utilizar normalmente UPPER_SNAKE_CASE.
- Los paquetes Java deben utilizar nombres en minúsculas.
- No se deben utilizar nombres ambiguos, abreviaturas innecesarias ni identificadores que dificulten la comprensión del código.
- Los nombres de clases, métodos y variables deben ser coherentes con el dominio de reservas y disponibilidad.

## 3. Organización del código

- El código debe respetar la estructura de paquetes y módulos existente.
- Las nuevas clases deben ubicarse según su responsabilidad y la arquitectura hexagonal.
- No se deben crear paquetes, módulos o capas adicionales sin una necesidad justificada.
- No se deben mover, renombrar ni eliminar clases existentes sin revisar las referencias y el impacto del cambio.
- Antes de crear una nueva clase, se debe comprobar si existe una implementación que ya cumpla esa responsabilidad.
- No se debe duplicar lógica de negocio para resolver un problema que pueda abordarse reutilizando componentes existentes.

## 4. Responsabilidad de las clases

- Cada clase debe tener una responsabilidad clara y coherente.
- Los métodos deben realizar tareas concretas y mantener una complejidad razonable.
- Los controladores no deben concentrar reglas de negocio.
- Los repositorios y adaptadores de infraestructura no deben asumir responsabilidades propias del dominio.
- Las reglas de negocio deben ubicarse en el dominio o en los casos de uso, según su naturaleza.
- No se deben introducir clases genéricas o abstracciones innecesarias que compliquen la solución sin aportar un beneficio claro.

## 5. Independencia del dominio

- Las clases del dominio no deben depender de anotaciones, interfaces o clases específicas de Spring.
- El dominio no debe importar bibliotecas de acceso a Redis, PostgreSQL, R2DBC o Kafka.
- Las entidades y los objetos de valor del dominio deben representar conceptos del negocio, no estructuras de infraestructura.
- Las reglas de negocio deben poder probarse sin levantar el servidor ni conectarse a servicios externos.
- Los modelos de persistencia y los objetos de transferencia de datos deben mantenerse separados del dominio cuando sus responsabilidades o estructuras sean diferentes.

## 6. Manejo de errores y excepciones

- Los errores deben gestionarse explícitamente y de acuerdo con la responsabilidad de cada capa.
- No se deben capturar excepciones para ignorarlas silenciosamente.
- No se deben devolver resultados aparentemente exitosos cuando una operación haya fallado.
- Los mensajes de error deben aportar información útil para diagnosticar el problema sin revelar credenciales, tokens ni otros datos sensibles.
- Los errores de negocio deben distinguirse de los fallos técnicos de infraestructura.
- No se deben utilizar excepciones genéricas como sustituto de un manejo de errores claro cuando sea posible representar mejor la situación.
- Las excepciones deben conservar la información necesaria para el diagnóstico sin duplicar innecesariamente registros de errores.

## 7. Programación reactiva

- El código que utilice WebFlux debe respetar el modelo de programación reactiva del proyecto.
- Las operaciones de entrada y salida deben utilizar mecanismos compatibles con el flujo reactivo cuando corresponda.
- No se deben utilizar llamadas bloqueantes innecesarias dentro de cadenas reactivas.
- No se deben utilizar `block()`, `blockFirst()` o `blockLast()` en los flujos de ejecución normales del servicio sin una justificación técnica aprobada.
- Se debe evitar el uso incorrecto de `subscribe()` dentro de los casos de uso para iniciar operaciones desconectadas del flujo principal.
- Los errores deben propagarse o transformarse mediante mecanismos compatibles con el flujo reactivo.
- No se deben introducir conversiones innecesarias entre programación imperativa y reactiva.

## 8. Validación de entradas

- Las solicitudes deben validarse antes de ejecutar operaciones que puedan afectar la disponibilidad o las reservas.
- Las validaciones deben respetar los requisitos funcionales documentados.
- Los datos inválidos no deben llegar a las operaciones de persistencia o modificación de disponibilidad sin la validación correspondiente.
- Las validaciones de formato de una solicitud deben distinguirse de las reglas de negocio.
- No se deben inventar reglas de validación ni restricciones de negocio que no estén definidas en los requisitos.
- Los valores recibidos del cliente no deben considerarse confiables por defecto.

## 9. Concurrencia e idempotencia

- Las operaciones que modifiquen la disponibilidad deben respetar las reglas de concurrencia definidas para Redis.
- No se debe implementar una operación de lectura y modificación como si fuera atómica cuando no lo sea.
- Las operaciones de reserva deben respetar los requisitos de idempotencia documentados.
- No se debe asumir que una solicitud repetida puede ejecutarse nuevamente sin efectos secundarios.
- Los mecanismos de compensación y reintento deben diseñarse para evitar duplicaciones o inconsistencias.
- Los cambios en estas operaciones deben acompañarse de pruebas que verifiquen los escenarios concurrentes relevantes.

## 10. Seguridad del código

- No se deben incluir contraseñas, claves privadas, tokens ni credenciales directamente en el código fuente.
- No se deben registrar tokens completos, credenciales ni datos sensibles en los logs.
- Las operaciones protegidas deben respetar los mecanismos de autenticación y autorización definidos en los documentos del proyecto.
- No se debe confiar únicamente en los identificadores enviados por el cliente para autorizar el acceso a una reserva.
- Las consultas y operaciones de persistencia deben evitar vulnerabilidades derivadas de entradas no confiables.
- No se deben desactivar validaciones de seguridad para facilitar pruebas o resolver errores de implementación.
- Los cambios de seguridad deben verificarse mediante pruebas apropiadas.

## 11. Comentarios y documentación del código

- Los comentarios deben explicar decisiones, restricciones o comportamientos que no resulten evidentes a partir del código.
- No se deben agregar comentarios que simplemente repitan cada instrucción.
- Los comentarios deben actualizarse cuando cambie el comportamiento que describen.
- Los TODO deben identificar una tarea pendiente concreta y no utilizarse para ocultar funcionalidades incompletas.
- No se debe documentar como implementado un comportamiento que únicamente esté propuesto o pendiente.
- Los contratos públicos y los casos de uso complejos deben contar con documentación suficiente para facilitar su comprensión y mantenimiento.

## 12. Calidad y mantenibilidad

- Se debe priorizar la legibilidad sobre soluciones innecesariamente complejas.
- Se debe evitar la duplicación de código cuando una abstracción sencilla y coherente permita reutilizar la lógica.
- No se deben introducir patrones de diseño únicamente por preferencia personal.
- No se deben dejar bloques de código comentados, importaciones sin uso ni código temporal innecesario.
- No se deben modificar componentes ajenos al objetivo del cambio sin una razón justificada.
- Las modificaciones deben conservar el comportamiento existente, salvo que un requisito aprobado indique lo contrario.
- Las refactorizaciones deben preservar los contratos y contar con pruebas adecuadas cuando corresponda.

## 13. Pruebas del código

- Las nuevas reglas de negocio deben contar con pruebas unitarias pertinentes.
- Los casos de uso deben probarse según sus escenarios normales y de error.
- Las integraciones deben verificarse mediante pruebas de integración apropiadas.
- Los escenarios de concurrencia, idempotencia y fallos parciales deben probarse cuando el cambio pueda afectarlos.
- Las pruebas deben ser reproducibles y no depender de datos externos no controlados.
- No se deben eliminar ni debilitar pruebas existentes únicamente para lograr que la ejecución termine correctamente.
- No se debe afirmar que las pruebas pasan si no se ejecutaron o no se verificaron sus resultados.

## 14. Restricciones sobre cambios de código

Antes de modificar código existente, se debe:

1. Identificar los archivos y componentes involucrados.
2. Consultar los requisitos y documentos de diseño pertinentes.
3. Verificar las convenciones y patrones que ya utiliza el proyecto.
4. Evaluar los posibles efectos sobre otros componentes y pruebas.
5. Realizar el cambio mínimo que resuelva el objetivo autorizado.
6. Ejecutar las verificaciones pertinentes y reportar sus resultados.

No se deben eliminar archivos, reescribir componentes completos ni realizar refactorizaciones amplias sin una necesidad justificada y autorización cuando corresponda.

## 15. Relación con otros documentos

Este documento complementa:

- `../diseño/01_arquitectura_hexagonal.md`
- `../diseño/02_componentes_y_responsabilidades.md`
- `../diseño/03_flujos_de_negocio.md`
- `../atributos_calidad/04_seguridad.md`
- Los documentos de estrategia de pruebas.
- Los requisitos funcionales y las decisiones arquitectónicas aprobadas.

Si una convención aquí descrita entra en conflicto con un requisito o una decisión aprobada, se debe identificar el conflicto y resolverlo antes de implementar el cambio. Las convenciones propuestas no deben tratarse como evidencia de que ya están aplicadas en todo el código existente.
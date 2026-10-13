# Estrategia de pruebas unitarias

## 1. Propósito

Definir cómo se diseñarán y ejecutarán las pruebas unitarias del microservicio Inventory para verificar el comportamiento de sus componentes de dominio y aplicación.

Estas pruebas buscan detectar errores en las reglas de negocio, las validaciones, el manejo de resultados y las condiciones de error antes de realizar pruebas de integración con Redis, PostgreSQL, Kafka u otros componentes externos.

## 2. Alcance

Las pruebas unitarias se concentrarán en los siguientes componentes:

- **Dominio:** entidades, objetos de valor y reglas de negocio que estén definidos en el proyecto.
- **Aplicación:** casos de uso encargados de coordinar las operaciones de consulta de disponibilidad, creación de reservas, consulta de reservas y cancelación.
- **Validaciones:** comprobaciones de los datos recibidos y de las condiciones necesarias para ejecutar una operación.
- **Manejo de errores:** respuestas ante entradas inválidas, resultados inesperados y fallos comunicados por las dependencias.
- **Idempotencia:** comportamiento del caso de uso de creación de reservas frente a solicitudes repetidas con la misma clave, de acuerdo con las reglas que se definan para esta funcionalidad.

Las pruebas unitarias no deben verificar conexiones reales con Redis, PostgreSQL o Kafka. Estas interacciones corresponden principalmente a las pruebas de integración.

## 3. Principios de diseño

Las pruebas unitarias deben cumplir los siguientes principios:

1. **Aislamiento:** cada prueba debe concentrarse en una unidad de comportamiento.
2. **Independencia:** el resultado no debe depender del orden en que se ejecuten las pruebas.
3. **Reproducibilidad:** los mismos datos y condiciones deben producir resultados comparables.
4. **Claridad:** el nombre de cada prueba debe expresar el comportamiento que se verifica.
5. **Cobertura de escenarios:** se deben considerar casos exitosos, entradas inválidas y condiciones límite.
6. **Independencia de infraestructura:** las reglas de negocio deben poder verificarse sin levantar servicios externos.
7. **Verificación del comportamiento:** las pruebas deben comprobar resultados y efectos relevantes, no solamente que un método haya sido ejecutado.
8. **Trazabilidad:** los escenarios deben relacionarse con los requisitos y las decisiones arquitectónicas correspondientes.

## 4. Componentes que se deben probar

### 4.1. Dominio

Se deben verificar las reglas propias del dominio que estén implementadas y documentadas.

Los escenarios pueden incluir:

- Creación válida de las entidades del dominio.
- Rechazo de datos que incumplan las restricciones establecidas.
- Validación de cantidades y otros atributos sujetos a reglas de negocio.
- Aplicación correcta de las operaciones permitidas sobre las entidades.
- Conservación de las invariantes del dominio después de una operación.

No se deben agregar reglas nuevas únicamente para satisfacer una prueba. Cada restricción debe estar respaldada por un requisito o una decisión documentada.

### 4.2. Casos de uso

Los casos de uso deben probarse de forma aislada mediante sus dependencias simuladas o sustituidas por implementaciones de prueba.

Se consideran los siguientes grupos:

- Consulta de disponibilidad.
- Creación de reservas.
- Consulta de reservas asociadas a un usuario.
- Cancelación de reservas.

Para cada caso de uso se deben verificar las entradas, el resultado esperado, los errores contemplados y las interacciones necesarias con los puertos de la arquitectura hexagonal.

### 4.3. Adaptadores simulados

Cuando un caso de uso dependa de un puerto para consultar o modificar información externa, se podrán utilizar dobles de prueba para controlar las respuestas de esa dependencia.

Por ejemplo, para probar la creación de una reserva se pueden simular resultados como:

- Disponibilidad suficiente.
- Disponibilidad insuficiente.
- Error al consultar o modificar la disponibilidad.
- Error al persistir una reserva.
- Resultado repetido de una solicitud idempotente.

Los escenarios exactos deben ajustarse al flujo de negocio y al mecanismo de persistencia y compensación que finalmente se apruebe.

## 5. Casos de prueba previstos

### 5.1. Consulta de disponibilidad

Se deben verificar los siguientes escenarios:

- Consulta con un evento válido y disponibilidad registrada.
- Consulta de un evento o sección sin disponibilidad registrada.
- Identificadores inválidos, cuando existan restricciones definidas para ellos.
- Manejo del resultado cuando la dependencia de disponibilidad comunica un error.
- Devolución del resultado esperado sin modificar innecesariamente el estado.

La forma de representar una disponibilidad inexistente y el comportamiento esperado ante un error de Redis deben corresponder con las decisiones de diseño vigentes.

### 5.2. Creación de reservas

Se deben considerar escenarios como los siguientes:

- Solicitud con identificadores y cantidad válidos.
- Solicitud con cantidad igual a cero o negativa.
- Solicitud con campos obligatorios ausentes.
- Disponibilidad suficiente para la cantidad solicitada.
- Disponibilidad insuficiente.
- Error durante la operación de reserva de disponibilidad.
- Error durante la persistencia.
- Repetición de una solicitud con la misma clave de idempotencia.
- Reutilización de una clave de idempotencia con datos diferentes, si este caso está contemplado en las reglas aprobadas.

En cada caso se deben comprobar el resultado del caso de uso, los errores producidos y las interacciones con los puertos correspondientes.

El estado inicial documentado para la respuesta de creación es `PENDING`. Las transiciones posteriores de estado y las condiciones exactas de confirmación deben verificarse solamente cuando estén definidas en los requisitos o las decisiones arquitectónicas.

### 5.3. Consulta de reservas

Se deben verificar escenarios como:

- Consulta con un identificador de usuario válido.
- Usuario sin reservas registradas.
- Resultado con varias reservas asociadas al usuario.
- Identificador de usuario inválido, cuando corresponda.
- Error comunicado por el puerto de consulta de reservas.

Las reglas de autorización deben evaluarse en las pruebas que correspondan a la capa responsable de aplicarlas. No se debe confundir una validación de negocio con la autorización de una solicitud HTTP.

### 5.4. Cancelación de reservas

Se deben considerar los siguientes escenarios:

- Solicitud de cancelación de una reserva existente.
- Intento de cancelar una reserva inexistente.
- Error durante la actualización del estado de la reserva.
- Error al liberar disponibilidad.
- Solicitud repetida de cancelación, de acuerdo con las reglas que se definan.
- Conservación de las restricciones de negocio después de la cancelación.

Las pruebas deben verificar el comportamiento definido para la cancelación. No se debe asumir que toda cancelación permite liberar disponibilidad inmediatamente o que una operación fallida siempre puede compensarse con éxito.

### 5.5. Errores y excepciones

Se deben comprobar los errores contemplados por cada caso de uso, incluyendo:

- Datos de entrada inválidos.
- Recursos inexistentes.
- Disponibilidad insuficiente.
- Fallos comunicados por los puertos de infraestructura.
- Resultados inesperados que deban manejarse explícitamente.

Los errores deben representarse de forma consistente con el diseño de la aplicación. La conversión final de estos errores a códigos HTTP corresponde principalmente a las pruebas de API e integración.

## 6. Preparación y estructura de las pruebas

Cada prueba debe seguir una estructura clara:

1. **Preparación:** definir los datos de entrada y el estado inicial.
2. **Ejecución:** invocar el método o caso de uso que se quiere verificar.
3. **Verificación:** comprobar el resultado, el error o los efectos esperados.

Se deben utilizar datos de prueba pequeños, comprensibles y específicos para cada escenario.

Cuando se utilicen dependencias simuladas, sus respuestas deben configurarse explícitamente. No se deben introducir comportamientos implícitos que dificulten entender por qué una prueba pasa o falla.

La organización física de las pruebas debe mantener una correspondencia clara con los paquetes de código que verifican, de acuerdo con la estructura del proyecto.

## 7. Verificaciones y criterios de aceptación

Una prueba unitaria se considera satisfactoria cuando:

- El comportamiento observado coincide con el resultado esperado.
- Las reglas de negocio evaluadas se cumplen.
- Los errores se manejan de acuerdo con el contrato del componente.
- No se producen efectos adicionales contrarios al comportamiento esperado.
- Las dependencias simuladas reciben las interacciones que requiere el caso de uso.
- La prueba puede ejecutarse repetidamente sin depender de datos externos ni de otras pruebas.

La aprobación de una prueba individual no demuestra por sí sola que el componente esté completamente validado. Debe considerarse el conjunto de escenarios planificados y su relación con los requisitos.

## 8. Cobertura y regresión

La cobertura de código puede utilizarse como indicador complementario para identificar partes del código que no se están ejecutando durante las pruebas.

Sin embargo, una cobertura elevada no garantiza que las reglas de negocio estén verificadas correctamente. Por ello, la estrategia debe priorizar la cobertura de comportamientos y escenarios relevantes.

El umbral mínimo de cobertura y la herramienta definitiva para medirla quedan pendientes de confirmación si todavía no están establecidos en el proyecto.

Cuando se corrija un defecto o se modifique una regla de negocio, se deben actualizar o agregar las pruebas correspondientes y volver a ejecutar los escenarios afectados.

## 9. Aspectos que no corresponden a las pruebas unitarias

Las siguientes verificaciones deben abordarse principalmente en otros niveles de prueba:

- Conectividad real con Redis y PostgreSQL.
- Persistencia efectiva de datos.
- Comunicación real con Kafka.
- Códigos HTTP y serialización de las respuestas REST.
- Validación completa de tokens JWT en el contexto de las solicitudes HTTP.
- Concurrencia real entre múltiples solicitudes.
- Rendimiento bajo carga.
- Recuperación integral ante la caída de una dependencia.

Las pruebas unitarias pueden simular errores relacionados con estos componentes para verificar cómo reaccionan los casos de uso, pero no sustituyen las pruebas de integración, seguridad, concurrencia o rendimiento.

## 10. Herramientas

La herramienta y las versiones para ejecutar las pruebas deben confirmarse según la configuración real del proyecto.

Se podrá utilizar el framework de pruebas compatible con la versión de Java y Spring Boot adoptada, junto con mecanismos de simulación de dependencias cuando sean necesarios.

No se debe asumir una biblioteca de simulación, un umbral de cobertura o una configuración de ejecución específicos hasta que estén definidos y documentados.

## 11. Resultado esperado

La aplicación de esta estrategia debe proporcionar evidencia de que las reglas de negocio y los casos de uso del microservicio Inventory se comportan de acuerdo con los requisitos definidos.

Los resultados deben facilitar la detección temprana de defectos, la prevención de regresiones y la preparación de los componentes para las pruebas de integración y de calidad.

Los casos de prueba detallados deberán mantenerse alineados con los requisitos, los documentos de diseño, los atributos de calidad y los ADR vigentes.
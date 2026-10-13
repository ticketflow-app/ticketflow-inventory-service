# Estrategia general de pruebas

## 1. Propósito

Establecer la estrategia general para verificar la funcionalidad, la calidad y la confiabilidad del microservicio Inventory, comprobando que su comportamiento cumpla los requisitos definidos y las decisiones arquitectónicas documentadas.

La estrategia busca detectar defectos de manera temprana, validar las reglas de negocio y comprobar el comportamiento del servicio ante condiciones normales, solicitudes inválidas, concurrencia y fallos de sus dependencias.

Este documento define las pruebas previstas y sus criterios generales. No implica que dichas pruebas estén implementadas o ejecutadas.

## 2. Alcance

La estrategia cubre exclusivamente el microservicio Inventory y sus responsabilidades documentadas.

Se consideran los siguientes componentes e interacciones:

- **Dominio:** reglas de negocio y restricciones relacionadas con las reservas y la disponibilidad.
- **Aplicación:** coordinación de los casos de uso y aplicación de las reglas del negocio.
- **Infraestructura:** adaptadores para Redis, PostgreSQL, Kafka y la API REST.
- **Seguridad:** validación local de JWT, autorización y validación de solicitudes, según los requisitos de seguridad documentados.
- **Resiliencia:** comportamiento ante errores, indisponibilidad de dependencias y problemas de consistencia.
- **Rendimiento y concurrencia:** capacidad para atender solicitudes simultáneas y proteger la disponibilidad de los eventos.

Las pruebas deben respetar la arquitectura hexagonal. Las reglas del dominio deben poder verificarse sin depender de servicios externos, mientras que las integraciones deben comprobarse mediante pruebas específicas.

## 3. Objetivos de las pruebas

Los objetivos principales son:

1. Verificar que las reglas de negocio se cumplan en los diferentes casos de uso.
2. Comprobar que las solicitudes válidas produzcan los resultados esperados y que las inválidas sean rechazadas correctamente.
3. Validar la interacción entre los casos de uso y los adaptadores de infraestructura.
4. Comprobar que las operaciones concurrentes no permitan asignar una cantidad de reservas superior a la disponibilidad.
5. Evaluar la consistencia entre Redis y PostgreSQL durante las operaciones de reserva y ante fallos.
6. Verificar la autenticación y la autorización según los requisitos de seguridad definidos.
7. Comprobar el comportamiento del servicio cuando Redis, PostgreSQL o Kafka presentan errores.
8. Evaluar el rendimiento, la disponibilidad y la observabilidad mediante las métricas definidas para el proyecto.
9. Detectar regresiones cuando se realicen cambios en el código.

## 4. Niveles de prueba

### 4.1. Pruebas unitarias

Verifican unidades pequeñas de código de forma aislada, principalmente componentes del dominio y casos de uso.

Se enfocan en:

- Reglas de negocio.
- Validación de datos de entrada.
- Manejo de resultados y errores.
- Restricciones sobre cantidades y disponibilidad.
- Comportamiento esperado de los casos de uso.

Estas pruebas deben evitar depender de conexiones reales a Redis, PostgreSQL o Kafka.

### 4.2. Pruebas de integración

Verifican que los componentes funcionen correctamente al interactuar entre sí y con sus dependencias.

Se enfocan en:

- Comunicación con Redis para consultar y modificar la disponibilidad.
- Persistencia y consulta de reservas en PostgreSQL mediante R2DBC.
- Integración de los adaptadores con los casos de uso.
- Publicación y manejo de eventos mediante Kafka, de acuerdo con el mecanismo de mensajería que se defina.
- Exposición y comportamiento de los endpoints REST.
- Validación de la configuración necesaria para conectar los componentes.

### 4.3. Pruebas funcionales de API

Comprueban el comportamiento del microservicio desde la perspectiva de un consumidor de su API.

Los endpoints documentados que deben considerarse son:

- `GET /api/v1/availability/{eventId}`
- `POST /api/v1/reservations`
- `GET /api/v1/reservations/user/{userId}`
- `DELETE /api/v1/reservations/{reservationId}`

Se deben verificar los códigos HTTP, las respuestas, las validaciones, los errores y las restricciones de acceso correspondientes a cada operación.

### 4.4. Pruebas de calidad y resiliencia

Evalúan atributos que no se limitan a la funcionalidad básica.

Incluyen:

- Rendimiento y tiempos de respuesta.
- Concurrencia y protección de la disponibilidad.
- Recuperación ante fallos de dependencias.
- Consistencia entre Redis y PostgreSQL.
- Seguridad y control de acceso.
- Disponibilidad y observabilidad del servicio.

Los escenarios específicos se desarrollarán en los documentos especializados de esta carpeta.

## 5. Principios de diseño de las pruebas

Las pruebas deben seguir estos principios:

- **Trazabilidad:** cada prueba debe relacionarse con un requisito, una regla de negocio, un escenario de calidad o una decisión arquitectónica.
- **Independencia:** las pruebas deben evitar depender innecesariamente del resultado o del orden de ejecución de otras pruebas.
- **Reproducibilidad:** una prueba debe poder ejecutarse nuevamente bajo condiciones controladas y producir resultados comparables.
- **Aislamiento:** las pruebas unitarias deben mantenerse separadas de las dependencias externas.
- **Validación de errores:** no basta con comprobar los casos exitosos; también deben verificarse las entradas inválidas y los fallos previstos.
- **Protección de invariantes:** las pruebas deben comprobar que las operaciones no violen las reglas fundamentales del dominio.
- **Detección de regresiones:** los cambios no deben alterar comportamientos previamente verificados sin una justificación y actualización de los requisitos.
- **Evidencia:** los resultados relevantes deben registrarse para facilitar el análisis de defectos y la evaluación de calidad.

## 6. Criterios generales de entrada

Antes de ejecutar un conjunto de pruebas, se debe comprobar que:

1. Los requisitos y el comportamiento esperado del componente estén suficientemente definidos.
2. El código que se desea verificar esté disponible en una versión identificable.
3. Las dependencias requeridas por el tipo de prueba estén configuradas.
4. Existan datos de prueba adecuados y controlados.
5. Los resultados esperados y las condiciones de aceptación estén definidos.
6. El entorno permita ejecutar las pruebas sin afectar datos o servicios ajenos a ellas.

Si una decisión arquitectónica necesaria para una prueba sigue pendiente, el escenario debe documentarse como pendiente de definición, sin asumir una implementación específica.

## 7. Criterios generales de salida

Un conjunto de pruebas podrá considerarse finalizado cuando:

1. Se hayan ejecutado las pruebas planificadas para su alcance.
2. Los resultados estén registrados y puedan relacionarse con la versión evaluada.
3. Los defectos encontrados estén documentados y clasificados.
4. Los fallos críticos para los requisitos evaluados se hayan corregido y verificado nuevamente, o exista una aceptación explícita del riesgo correspondiente.
5. Se hayan documentado las pruebas bloqueadas, no ejecutadas o con resultados inconclusos.
6. La evidencia permita determinar si se cumplen los criterios de aceptación definidos.

La finalización de las pruebas no implica automáticamente que todo el sistema esté libre de defectos. Las conclusiones deben limitarse al alcance efectivamente evaluado.

## 8. Gestión de defectos

Cuando una prueba falle, se debe registrar como mínimo:

- Identificador o referencia del defecto.
- Componente afectado.
- Descripción del comportamiento observado.
- Resultado esperado.
- Pasos para reproducir el problema.
- Evidencia disponible, como mensajes de error, registros o resultados de ejecución.
- Severidad o prioridad asignada según el impacto.
- Estado de la corrección y resultado de la nueva ejecución.

Después de corregir un defecto, se deben repetir las pruebas relacionadas y, cuando exista riesgo de afectar otras funcionalidades, las pruebas de regresión pertinentes.

## 9. Datos y ambientes de prueba

Los datos utilizados deben ser controlados y adecuados para cada escenario.

Se debe evitar que una prueba dependa accidentalmente del estado dejado por una ejecución anterior. Cuando se utilicen Redis o PostgreSQL, es necesario establecer cómo se inicializan, aíslan y limpian los datos de prueba.

Las pruebas de integración y de calidad deben ejecutarse en entornos donde sea posible controlar las dependencias y observar los resultados. No deben realizarse pruebas destructivas sobre datos reales.

Las herramientas concretas, la configuración de los ambientes y los mecanismos de preparación y limpieza se especificarán en `05_datos_y_entorno_de_pruebas.md`.

## 10. Herramientas y automatización

La selección definitiva de herramientas debe corresponder con las tecnologías utilizadas por el proyecto y con los requisitos de cada nivel de prueba.

Se podrán evaluar herramientas para:

- Ejecutar pruebas unitarias y de integración.
- Verificar endpoints HTTP.
- Medir cobertura de código.
- Simular concurrencia y carga.
- Analizar registros y métricas.
- Automatizar la ejecución repetible de las pruebas.

Las herramientas, versiones, umbrales de cobertura y mecanismos de automatización específicos quedan pendientes de confirmación cuando no estén definidos en la documentación del proyecto.

## 11. Organización de la estrategia

La estrategia se desarrolla mediante los siguientes documentos complementarios:

- `01_estrategia_general.md`: objetivos, alcance, niveles y criterios generales.
- `02_pruebas_unitarias.md`: reglas de negocio, casos de uso y validaciones aisladas.
- `03_pruebas_integracion.md`: integración con Redis, PostgreSQL, Kafka y la API REST.
- `04_pruebas_calidad_y_resiliencia.md`: seguridad, concurrencia, rendimiento, consistencia y fallos de dependencias.
- `05_datos_y_entorno_de_pruebas.md`: datos, ambientes, herramientas, aislamiento y reproducibilidad.

Estos documentos deben mantenerse alineados con los requisitos, los atributos de calidad, el diseño arquitectónico y las decisiones registradas en los ADR.

## 12. Aspectos pendientes de definición

Antes de considerar completa la estrategia de pruebas, deben confirmarse los aspectos que aún no estén establecidos en la documentación:

- Herramientas y versiones definitivas para cada nivel de prueba.
- Umbral mínimo de cobertura de código, si se exige.
- Criterios cuantitativos de rendimiento y carga.
- Configuración y disponibilidad de los ambientes de integración.
- Mecanismo definitivo de publicación fiable de eventos en Kafka.
- Comportamiento esperado ante fallos parciales entre Redis y PostgreSQL.
- Criterios de aceptación para la recuperación y reconciliación de datos.
- Grado de automatización requerido para la ejecución de pruebas.

Estos puntos no deben darse por implementados ni aprobados hasta contar con la correspondiente decisión o evidencia del proyecto.

## 13. Resultado esperado

La aplicación de esta estrategia debe permitir evaluar de manera organizada y trazable si el microservicio Inventory cumple sus requisitos funcionales y de calidad, detectar defectos antes de su entrega y aportar evidencia verificable sobre su comportamiento en condiciones normales, concurrentes y de fallo.
# Restricciones arquitectónicas

## 1. Propósito

Este documento define las restricciones arquitectónicas que deben respetarse durante el desarrollo del microservicio Inventory del proyecto AI-SEMLA.

Su objetivo es preservar la separación de responsabilidades, mantener la independencia del dominio y evitar modificaciones que contradigan la arquitectura definida.

## 2. Arquitectura hexagonal

- El microservicio debe respetar la arquitectura hexagonal documentada en el proyecto.
- La solución debe separar el dominio, la aplicación y la infraestructura.
- Cada capa debe tener responsabilidades claramente delimitadas.
- Las nuevas funcionalidades deben integrarse en la arquitectura existente, sin introducir estructuras alternativas que generen inconsistencias.
- No se debe cambiar el estilo arquitectónico sin autorización explícita.

## 3. Separación de capas

### 3.1. Dominio

- Contiene las reglas de negocio y los conceptos centrales del microservicio.
- No debe depender de Spring Boot, WebFlux, Redis, PostgreSQL, R2DBC, Kafka ni otras tecnologías de infraestructura.
- No debe incluir anotaciones específicas de persistencia o del framework.
- Las reglas de negocio deben permanecer independientes de los mecanismos utilizados para ejecutarlas.

### 3.2. Aplicación

- Coordina los casos de uso del microservicio.
- Define o utiliza los puertos necesarios para interactuar con servicios externos.
- No debe acoplar los casos de uso directamente a implementaciones concretas de infraestructura.
- Debe respetar las reglas del dominio y delegar las operaciones externas mediante las abstracciones correspondientes.

### 3.3. Infraestructura

- Contiene las implementaciones técnicas necesarias para integrar el microservicio con tecnologías externas.
- Implementa los adaptadores para Redis, PostgreSQL, Kafka y las interfaces de entrada, según corresponda.
- Es responsable de los detalles específicos de comunicación, persistencia y configuración tecnológica.
- No debe trasladar las reglas centrales del negocio a controladores, repositorios o adaptadores.

## 4. Dirección de dependencias

- Las dependencias deben orientarse hacia las reglas centrales del negocio.
- El dominio no debe importar clases de infraestructura.
- Los casos de uso no deben depender directamente de implementaciones concretas de Redis, PostgreSQL o Kafka.
- Los detalles tecnológicos deben conectarse mediante puertos, interfaces o abstracciones coherentes con la arquitectura.
- No se deben introducir dependencias circulares entre capas o módulos.

## 5. Separación entre negocio y tecnología

- Las reglas de disponibilidad y reservas deben mantenerse separadas de los detalles de acceso a Redis y PostgreSQL.
- Los casos de uso no deben contener lógica propia de controladores HTTP, consultas específicas de base de datos o configuración de clientes externos.
- Las decisiones de negocio no deben depender de detalles del protocolo HTTP o de estructuras internas de Kafka.
- Los cambios tecnológicos no deben obligar a reescribir las reglas centrales del dominio cuando puedan resolverse mediante los adaptadores correspondientes.

## 6. Controladores y casos de uso

- Los controladores REST deben encargarse de recibir solicitudes, validar los aspectos correspondientes a la entrada y devolver respuestas HTTP.
- Los controladores no deben concentrar las reglas de negocio ni coordinar directamente toda la lógica de persistencia.
- Los casos de uso deben representar las operaciones de negocio y coordinar las interacciones necesarias.
- Las respuestas y los errores deben adaptarse a los contratos de la API sin exponer innecesariamente detalles internos.

## 7. Acceso a sistemas externos

- El acceso a Redis debe realizarse mediante los componentes definidos para esa responsabilidad.
- El acceso a PostgreSQL debe respetar la estrategia de persistencia reactiva basada en R2DBC.
- La publicación y el consumo de eventos Kafka deben mantenerse separados de las reglas centrales del dominio.
- No se deben introducir accesos directos a sistemas externos desde clases del dominio.
- Las implementaciones concretas deben respetar los contratos establecidos por los puertos correspondientes.

## 8. Disponibilidad y persistencia

- Redis es la fuente de verdad operativa para la disponibilidad en tiempo real.
- PostgreSQL conserva la información persistente de las reservas.
- La arquitectura debe mantener claras las responsabilidades de ambos sistemas.
- No se debe tratar la actualización de Redis y PostgreSQL como una transacción atómica conjunta.
- Los mecanismos de compensación, recuperación y reconciliación deben respetar las decisiones documentadas.
- No se deben introducir mecanismos de consistencia nuevos sin evaluar su impacto y obtener la autorización correspondiente.

## 9. Programación reactiva

- La infraestructura y los casos de uso que formen parte de los flujos reactivos deben respetar el modelo de ejecución definido por el proyecto.
- Las operaciones de entrada y salida deben utilizar mecanismos compatibles con la arquitectura reactiva cuando corresponda.
- No se deben introducir bloqueos innecesarios en los flujos de WebFlux.
- No se deben convertir tipos reactivos en resultados bloqueantes sin una justificación técnica aprobada.
- El dominio debe conservar su independencia del modelo reactivo de Spring cuando no necesite conocerlo para expresar sus reglas de negocio.

## 10. Contratos e integración

- Las interfaces entre capas deben ser claras y coherentes con las responsabilidades de cada componente.
- Los contratos REST y los contratos de eventos deben respetarse.
- Los cambios incompatibles en interfaces, solicitudes, respuestas o eventos deben identificarse antes de implementarse.
- No se deben inventar contratos ni asumir que una propuesta de diseño ya fue aprobada.
- Cuando una modificación afecte a varios componentes, se deben identificar y actualizar las partes de la solución que correspondan.

## 11. Manejo de errores

- Los errores técnicos deben gestionarse en la capa que corresponda a su responsabilidad.
- Los errores de negocio deben distinguirse de los errores de infraestructura.
- Los controladores deben traducir los errores pertinentes a respuestas HTTP coherentes con el contrato de la API.
- No se deben ocultar fallos de Redis, PostgreSQL o Kafka como si las operaciones hubieran finalizado correctamente.
- Los mecanismos de reintento y compensación no deben duplicar operaciones ni comprometer la consistencia de las reservas.

## 12. Pruebas y verificabilidad

- La separación de capas debe facilitar las pruebas unitarias de las reglas de negocio y los casos de uso.
- Las integraciones con Redis, PostgreSQL, Kafka y la API deben verificarse mediante las pruebas correspondientes.
- Las pruebas no deben depender innecesariamente de implementaciones concretas cuando puedan utilizarse abstracciones adecuadas.
- Las modificaciones arquitectónicas deben acompañarse de pruebas que permitan verificar que se conservan las responsabilidades y los comportamientos esperados.
- No se debe considerar que la arquitectura cumple estas restricciones únicamente porque el código compila.

## 13. Cambios arquitectónicos

Se requiere autorización explícita antes de:

- Cambiar la arquitectura hexagonal por otro estilo arquitectónico.
- Alterar la separación o las responsabilidades de las capas.
- Introducir dependencias que inviertan o rompan la dirección establecida.
- Incorporar acceso directo a infraestructura desde el dominio.
- Cambiar las responsabilidades de Redis o PostgreSQL.
- Introducir nuevos patrones arquitectónicos que modifiquen sustancialmente el diseño.
- Cambiar los contratos de integración de forma incompatible.

Las propuestas deben documentar el problema que resuelven, las alternativas consideradas, sus consecuencias y los componentes afectados. Cuando corresponda, deben registrarse como decisiones arquitectónicas (ADR).

## 14. Gestión de información pendiente

- Si no está definida la ubicación de una responsabilidad, no se debe asignar arbitrariamente.
- Si existe una contradicción entre la arquitectura documentada y el código actual, se debe identificar y analizar antes de modificar componentes.
- Si una decisión arquitectónica está pendiente, debe tratarse como propuesta y no como una regla aprobada.
- Cuando la documentación no permita determinar la solución correcta, se debe solicitar aclaración antes de realizar un cambio estructural.

## 15. Relación con otros documentos

Este documento complementa los siguientes archivos:

- `../diseño/01_arquitectura_hexagonal.md`
- `../diseño/02_componentes_y_responsabilidades.md`
- `../diseño/03_flujos_de_negocio.md`
- `../diseño/04_integracion_y_mensajeria.md`
- `../diseño/05_configuracion.md`
- Los documentos de estrategia de pruebas y atributos de calidad.

Ante una contradicción, se debe identificar la diferencia y resolverla explícitamente. Este documento no sustituye los requisitos, las decisiones arquitectónicas aprobadas ni los contratos definidos para el microservicio.
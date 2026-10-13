# Restricciones tecnológicas

## 1. Propósito

Este documento establece las restricciones tecnológicas que deben respetarse durante el desarrollo del microservicio Inventory del proyecto AI-SEMLA.

Su objetivo es mantener la compatibilidad entre las tecnologías utilizadas, evitar cambios no autorizados y garantizar que las decisiones técnicas sean coherentes con la arquitectura y los requisitos documentados.

## 2. Lenguaje y versión de Java

- El lenguaje de programación del microservicio es Java.
- La versión establecida es Java 21.
- No se debe cambiar la versión de Java sin autorización explícita.
- Las implementaciones deben ser compatibles con la versión de Java configurada en el proyecto.

## 3. Framework y programación reactiva

- El framework principal es Spring Boot 4.1.1.
- La aplicación utiliza Spring WebFlux para el desarrollo de servicios reactivos.
- Los controladores y componentes que formen parte de los flujos reactivos deben respetar el modelo de programación reactiva.
- No se debe incorporar Spring MVC ni sustituir WebFlux sin autorización.
- No se deben introducir operaciones bloqueantes en los flujos reactivos sin una justificación técnica y una estrategia adecuada para evitar afectar su rendimiento.

## 4. Gestión del proyecto y dependencias

- Maven es la herramienta de gestión de dependencias y construcción del proyecto.
- Se debe respetar la estructura y configuración Maven existente.
- No se deben agregar, eliminar, actualizar ni sustituir dependencias sin autorización explícita.
- Antes de proponer una dependencia nueva, se debe verificar si las funcionalidades necesarias pueden resolverse con las tecnologías existentes.
- Las nuevas dependencias propuestas deben ser compatibles con Java 21, Spring Boot 4.1.1 y el modelo reactivo cuando corresponda.
- No se deben cambiar versiones para resolver un problema sin analizar primero las causas y los posibles efectos secundarios.

## 5. Almacenamiento y persistencia

### 5.1. Redis

- Redis se utiliza como fuente de verdad operativa para la disponibilidad de entradas en tiempo real.
- La disponibilidad se gestiona mediante claves con el patrón `availability:{event_id}:{section_id}`.
- Las operaciones sobre la disponibilidad deben respetar las reglas de concurrencia y consistencia definidas en los documentos de diseño.
- No se debe sustituir Redis por otra tecnología ni cambiar su responsabilidad sin autorización.

### 5.2. PostgreSQL

- PostgreSQL se utiliza para la persistencia duradera de las reservas.
- El acceso a PostgreSQL se realiza mediante Spring Data R2DBC, respetando el enfoque reactivo del servicio.
- No se debe introducir JPA o Hibernate como mecanismo alternativo de persistencia sin autorización.
- Los cambios en el esquema de la base de datos deben ser compatibles con el modelo de persistencia y las migraciones definidas para el proyecto.

### 5.3. Consistencia entre sistemas

- Las operaciones que involucran Redis y PostgreSQL deben respetar el flujo de negocio y las estrategias de recuperación documentadas.
- No se debe asumir que una operación sobre ambos sistemas es atómica.
- No se deben presentar mecanismos de compensación, recuperación o consistencia como garantías absolutas si no están implementados y verificados.

## 6. Mensajería

- Apache Kafka se contempla para la publicación y el consumo de eventos relacionados con las reservas.
- La integración debe respetar los contratos de eventos y las decisiones documentadas.
- No se deben inventar nombres de tópicos, estructuras de eventos, políticas de reintentos o garantías de entrega como si fueran decisiones aprobadas.
- Cualquier cambio en la tecnología de mensajería o en sus responsabilidades requiere autorización.

## 7. Observabilidad

- Micrometer se utiliza para la instrumentación de métricas.
- Spring Boot Actuator se utiliza para exponer información de salud y observabilidad según la configuración del servicio.
- Las métricas y los indicadores implementados deben corresponder con los requisitos de calidad documentados.
- No se deben exponer endpoints de administración o información operativa sensible sin considerar los requisitos de seguridad.

## 8. Configuración y secretos

- La configuración debe mantenerse separada del código fuente cuando corresponda.
- Las credenciales, contraseñas, tokens y otros secretos no deben incluirse directamente en el código ni en los documentos versionados.
- Se deben respetar los mecanismos de configuración existentes en el proyecto.
- No se deben modificar direcciones de servicios, variables de entorno o configuraciones de conexión sin verificar su impacto.
- Los valores concretos de configuración deben obtenerse de los archivos y documentos existentes; no deben inventarse.

## 9. Compatibilidad entre tecnologías

Antes de incorporar o modificar una tecnología, se debe verificar su compatibilidad con:

- Java 21.
- Spring Boot 4.1.1.
- Spring WebFlux y el modelo reactivo.
- Maven y la estructura actual del proyecto.
- Las tecnologías de persistencia, almacenamiento y mensajería utilizadas.

La compatibilidad no debe darse por sentada únicamente porque una biblioteca funciona con versiones anteriores de Spring Boot.

## 10. Cambios tecnológicos no autorizados

Se requiere autorización explícita antes de:

- Cambiar las versiones de Java o Spring Boot.
- Incorporar, eliminar o actualizar dependencias.
- Sustituir Redis, PostgreSQL, R2DBC, Kafka, WebFlux, Micrometer o Actuator.
- Introducir un framework o mecanismo de persistencia alternativo.
- Modificar la configuración tecnológica general del microservicio.
- Cambiar la herramienta de construcción o la estructura base del proyecto.

Cuando se identifique una mejora tecnológica, primero se debe documentar la necesidad, los beneficios, los riesgos y las alternativas. La propuesta no debe implementarse hasta recibir autorización.

## 11. Verificación de las restricciones

Antes de dar por terminado un cambio tecnológico, se debe comprobar que:

- La aplicación mantiene la compatibilidad con las versiones establecidas.
- La construcción Maven finaliza correctamente, cuando sea posible ejecutarla.
- Las pruebas pertinentes se ejecutan y sus resultados se informan con precisión.
- No se han introducido dependencias o tecnologías no autorizadas.
- La documentación afectada se encuentra actualizada.

No se debe afirmar que una tecnología está correctamente configurada o que una prueba fue superada sin evidencia suficiente.

## 12. Relación con otros documentos

Este documento complementa, pero no sustituye, los siguientes documentos:

- `../diseño/01_arquitectura_hexagonal.md`
- `../diseño/02_componentes_y_responsabilidades.md`
- `../diseño/04_integracion_y_mensajeria.md`
- `../diseño/05_configuracion.md`
- Los documentos correspondientes a los atributos de calidad y las decisiones arquitectónicas (ADR).

Si se encuentra una contradicción, se debe identificar y resolver explícitamente antes de implementar un cambio. No se debe elegir arbitrariamente una interpretación ni asumir que una propuesta pendiente ya fue aprobada.
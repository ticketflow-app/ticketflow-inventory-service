# Estrategia de pruebas de calidad y resiliencia

## 1. Propósito

Definir la estrategia para evaluar los atributos de calidad del microservicio Inventory, con énfasis en rendimiento, escalabilidad, disponibilidad, resiliencia, consistencia, concurrencia y seguridad.

Estas pruebas buscan determinar si el servicio cumple los requisitos establecidos tanto en condiciones normales como cuando aumenta la demanda o se presentan fallos en sus dependencias.

Los resultados deben aportar evidencia para identificar limitaciones, validar las decisiones arquitectónicas y orientar las mejoras necesarias.

Este documento establece las pruebas previstas. No implica que todas estén implementadas o ejecutadas.

## 2. Alcance

Las pruebas se centran en los siguientes aspectos:

- Rendimiento y tiempos de respuesta.
- Comportamiento bajo carga y concurrencia.
- Protección de la disponibilidad de los eventos.
- Manejo de fallos de Redis, PostgreSQL y Kafka.
- Consistencia entre la disponibilidad operativa y las reservas persistidas.
- Recuperación ante errores y detección de inconsistencias.
- Autenticación, autorización y validación de solicitudes.
- Observabilidad y capacidad para diagnosticar fallos.

Los escenarios deben alinearse con los requisitos, los documentos de atributos de calidad, los documentos de diseño y los ADR del microservicio Inventory.

## 3. Pruebas de rendimiento y escalabilidad

### 3.1. Objetivo

Evaluar el comportamiento del servicio cuando recibe diferentes cantidades de solicitudes, identificando cómo cambia el tiempo de respuesta y el consumo de recursos.

Las pruebas deben ayudar a determinar la capacidad del microservicio para atender la demanda esperada y reconocer los puntos en los que su rendimiento comienza a degradarse.

### 3.2. Escenarios

Se deben considerar los siguientes escenarios:

- **Carga normal:** ejecución de las operaciones con un volumen representativo de solicitudes.
- **Carga creciente:** aumento gradual de solicitudes para observar la evolución del rendimiento.
- **Carga elevada:** evaluación del comportamiento cerca de los límites operativos del entorno.
- **Carga sostenida:** ejecución durante un periodo definido para identificar degradaciones persistentes.
- **Recuperación de carga:** observación del comportamiento después de reducir la demanda.

Las pruebas deben incluir, cuando corresponda, consultas de disponibilidad, creación de reservas, consulta de reservas y cancelación.

### 3.3. Métricas

Se deben recopilar, según las capacidades de observabilidad disponibles:

- Tiempo de respuesta promedio.
- Percentiles de latencia, como p95 y p99.
- Número de solicitudes procesadas por unidad de tiempo.
- Tasa de errores.
- Disponibilidad del servicio durante la prueba.
- Uso de CPU y memoria.
- Consumo de conexiones y recursos de Redis y PostgreSQL.
- Comportamiento de la comunicación con Kafka, cuando participe en el escenario.

Las métricas deben interpretarse en conjunto. Un aumento de solicitudes atendidas no necesariamente indica una mejora si también aumenta significativamente la latencia o la tasa de errores.

### 3.4. Criterios de aceptación

Los criterios cuantitativos deben derivarse de los requisitos de rendimiento y de las condiciones del entorno de pruebas.

Antes de evaluar formalmente el resultado, se deben definir:

- Volumen objetivo de solicitudes.
- Número de usuarios o solicitudes concurrentes.
- Duración de la prueba.
- Límites aceptables de latencia.
- Tasa máxima de errores permitida.
- Recursos disponibles para ejecutar el escenario.

Cuando estos valores no estén definidos, la prueba podrá utilizarse de forma exploratoria para caracterizar el sistema, pero no deberá declararse formalmente aprobada frente a umbrales inexistentes.

## 4. Pruebas de concurrencia

### 4.1. Objetivo

Comprobar que las solicitudes simultáneas no provoquen asignaciones superiores a la disponibilidad real ni violen las restricciones del dominio.

La disponibilidad operativa se administra mediante contadores en Redis. Por ello, la concurrencia debe evaluarse sobre las operaciones reales utilizadas por el microservicio.

### 4.2. Escenarios

**Reservas simultáneas con disponibilidad suficiente**

Ejecutar varias solicitudes concurrentes para una misma sección y verificar que las operaciones aceptadas correspondan con las reglas definidas.

**Reservas simultáneas con disponibilidad limitada**

Configurar una disponibilidad inferior a la demanda concurrente y comprobar que no se asignen más unidades de las disponibles.

**Competencia por la última unidad disponible**

Ejecutar varias solicitudes cuando queda una sola unidad y verificar que el resultado respete las restricciones de disponibilidad.

**Solicitudes repetidas**

Repetir solicitudes con la misma clave de idempotencia para evaluar el comportamiento definido para las operaciones duplicadas.

**Operaciones concurrentes de cancelación y reserva**

Cuando el comportamiento de negocio esté definido, evaluar las reservas y cancelaciones que ocurren simultáneamente sobre la misma sección.

### 4.3. Verificaciones

Se debe comprobar:

- Que el número de unidades asignadas no supere la disponibilidad inicial.
- Que el resultado final del contador sea coherente con las operaciones aceptadas.
- Que las solicitudes rechazadas no produzcan asignaciones indebidas.
- Que las solicitudes repetidas se manejen de acuerdo con las reglas de idempotencia.
- Que no se produzcan inconsistencias silenciosas entre los resultados informados y las operaciones efectivamente realizadas.

La atomicidad de Redis para una operación individual no garantiza que toda la secuencia que involucra Redis y PostgreSQL sea atómica.

### 4.4. Criterios de aceptación

Las pruebas deben demostrar que se respetan las restricciones de disponibilidad bajo concurrencia. Los resultados concretos deben compararse con los requisitos y las decisiones arquitectónicas vigentes.

## 5. Pruebas de disponibilidad y resiliencia

### 5.1. Objetivo

Evaluar cómo responde el microservicio cuando una dependencia externa deja de estar disponible, presenta errores o responde fuera del tiempo esperado.

### 5.2. Fallos de Redis

Se deben considerar escenarios como:

- Redis no está disponible al iniciar una operación.
- Se pierde la conexión durante una solicitud.
- Una operación de lectura o actualización falla.
- Una solicitud excede el tiempo de espera configurado.
- Redis vuelve a estar disponible después de una interrupción.

Se debe verificar que el servicio no informe como exitosa una reserva cuya actualización de disponibilidad no se haya confirmado.

La estrategia no debe asumir que siempre es seguro continuar utilizando valores de disponibilidad almacenados en memoria o en caché cuando Redis no está disponible. Ese comportamiento debe estar definido explícitamente antes de probarlo como una garantía.

### 5.3. Fallos de PostgreSQL

Se deben considerar:

- PostgreSQL no está disponible durante una operación.
- Se pierde una conexión durante la persistencia.
- Una operación de escritura falla.
- Se produce un tiempo de espera agotado.
- La conexión se restablece después de una interrupción.

Las pruebas deben verificar que los errores de persistencia se comuniquen correctamente y que se manejen las consecuencias sobre la disponibilidad cuando una operación haya modificado previamente Redis.

### 5.4. Fallos de Kafka

Cuando la integración esté implementada, se deben evaluar:

- Kafka no está disponible al intentar publicar un evento.
- Se produce un error durante la publicación.
- La publicación tarda más de lo esperado.
- Kafka vuelve a estar disponible después de una interrupción.
- Se producen reintentos o publicaciones duplicadas, cuando correspondan.

Se debe comprobar el comportamiento real de publicación y el resultado comunicado por la aplicación. No se debe considerar garantizada la entrega de eventos si el mecanismo de publicación fiable todavía no está definido o implementado.

### 5.5. Criterios de aceptación

- Los fallos de las dependencias se detectan y registran.
- Las operaciones afectadas no se presentan como exitosas sin las garantías requeridas.
- Los errores se comunican de acuerdo con los contratos definidos.
- La recuperación de una dependencia permite retomar las operaciones conforme al comportamiento implementado.
- Las limitaciones de recuperación que no estén resueltas quedan documentadas.

## 6. Pruebas de consistencia y recuperación

### 6.1. Objetivo

Verificar el comportamiento de Inventory cuando una operación afecta a más de una dependencia y no todas las acciones pueden completarse.

Redis mantiene la disponibilidad operativa, mientras que PostgreSQL conserva la información persistente de las reservas. La coordinación entre ambos sistemas requiere pruebas específicas porque no comparten automáticamente una transacción atómica.

### 6.2. Escenarios

Se deben contemplar los siguientes casos:

- La modificación de disponibilidad y la persistencia se completan correctamente.
- La disponibilidad se modifica, pero falla la persistencia.
- Se produce un error antes de modificar la disponibilidad.
- Se intenta compensar una modificación y la compensación falla.
- Una solicitud se repite después de una respuesta ambigua o un error de comunicación.
- La información persistida no coincide con el estado de disponibilidad.
- Se requiere detectar o reparar una inconsistencia después de un fallo.

### 6.3. Verificaciones

Para cada escenario se debe comparar:

- La disponibilidad inicial.
- Las operaciones que realmente se completaron.
- El estado final de Redis.
- El estado persistido en PostgreSQL.
- El resultado informado al consumidor.
- Los registros y las métricas generados.
- La ejecución y el resultado de cualquier mecanismo de recuperación.

La compensación mediante `INCRBY` es una posibilidad documentada, pero puede fallar. Las pruebas deben verificar tanto su funcionamiento como las consecuencias de un fallo.

Los mecanismos de reconciliación, reparación automática, reintentos o patrones adicionales de persistencia solo deben probarse como funcionalidades existentes cuando estén definidos e implementados.

### 6.4. Criterios de aceptación

- Los estados finales se pueden verificar en ambas dependencias.
- Los fallos parciales se detectan y no se ocultan.
- Los mecanismos de compensación cumplen el comportamiento definido cuando funcionan correctamente.
- Los fallos de compensación generan evidencia suficiente para diagnosticar la inconsistencia.
- La recuperación y la reconciliación cumplen los criterios aprobados, cuando esos mecanismos estén disponibles.

## 7. Pruebas de seguridad

### 7.1. Objetivo

Verificar que el microservicio proteja sus operaciones y datos de acuerdo con los requisitos de seguridad documentados.

### 7.2. Autenticación

Se deben evaluar escenarios como:

- Solicitud con un JWT válido.
- Solicitud sin token cuando se requiere autenticación.
- Token expirado.
- Token con firma inválida.
- Token con campos obligatorios ausentes o inválidos.
- Token que no cumple las condiciones de validación establecidas.

### 7.3. Autorización

Se deben comprobar las reglas documentadas para el acceso a las operaciones, incluyendo:

- Acceso a operaciones que requieran permisos específicos.
- Intento de consultar reservas de otro usuario.
- Intento de cancelar una reserva que pertenece a otro usuario.
- Solicitudes con roles o permisos insuficientes.
- Acceso a endpoints definidos como públicos.

Los roles y permisos que se evalúen deben corresponder con los requisitos aprobados. El documento de seguridad contempla los roles `BUYER` y `ADMIN`, pero las pruebas deben mantenerse alineadas con la definición vigente de dichos permisos.

### 7.4. Validación y protección de información

Se debe comprobar:

- El rechazo de solicitudes malformadas.
- La validación de campos y parámetros.
- El manejo seguro de errores.
- La ausencia de secretos en las respuestas y los registros.
- La protección de las credenciales utilizadas para acceder a las dependencias.
- El uso de los mecanismos de transporte seguro exigidos por el entorno.

### 7.5. Criterios de aceptación

Las solicitudes deben cumplir las reglas de autenticación, autorización y validación documentadas. Los códigos HTTP esperados y las condiciones específicas de acceso deben verificarse contra el contrato de seguridad y de la API.

## 8. Pruebas de observabilidad

### 8.1. Objetivo

Comprobar que el servicio proporciona información suficiente para identificar problemas de funcionamiento y analizar las operaciones.

La arquitectura contempla Micrometer y Spring Boot Actuator para apoyar la observabilidad.

### 8.2. Escenarios

Se deben verificar, según la configuración implementada:

- Disponibilidad de los indicadores de salud.
- Registro de errores relevantes en las operaciones.
- Captura de métricas de solicitudes y tiempos de respuesta.
- Identificación de errores de comunicación con las dependencias.
- Capacidad de relacionar una ejecución fallida con sus registros.
- Ausencia de tokens, contraseñas y otros secretos en la información de diagnóstico.

Las métricas y los endpoints de Actuator que se evalúen deben corresponder con la configuración real del servicio.

### 8.3. Criterios de aceptación

- Los fallos relevantes generan evidencia que permite investigarlos.
- Las métricas disponibles reflejan el comportamiento evaluado.
- Los indicadores de salud corresponden con los componentes que realmente supervisan.
- La información de observabilidad no expone datos sensibles.

## 9. Preparación de las pruebas

Antes de ejecutar estas pruebas se deben establecer:

1. El escenario que se desea evaluar y el requisito relacionado.
2. El estado inicial de Redis y PostgreSQL.
3. La configuración de las dependencias necesarias.
4. El volumen de solicitudes y la duración, para las pruebas de rendimiento.
5. Los fallos que se simularán y las condiciones para restablecer el entorno.
6. Las métricas y evidencias que se recopilarán.
7. Los criterios con los que se interpretarán los resultados.

Las pruebas de concurrencia, rendimiento y fallos deben ejecutarse en entornos controlados, sin afectar datos reales ni servicios ajenos al proyecto.

## 10. Registro de resultados

Cada prueba debe documentar:

- Identificador y objetivo.
- Atributo de calidad evaluado.
- Requisito o decisión relacionada.
- Configuración y condiciones iniciales.
- Carga o tipo de fallo aplicado.
- Resultado esperado y resultado observado.
- Métricas, registros y evidencias.
- Defectos identificados.
- Limitaciones de la ejecución.

Cuando se comparen resultados de rendimiento, también se deben registrar las características relevantes del entorno para evitar conclusiones basadas en condiciones no equivalentes.

## 11. Aspectos pendientes de definición

Los siguientes puntos deben confirmarse si todavía no están establecidos:

- Objetivos cuantitativos de latencia y capacidad.
- Volumen de solicitudes y concurrencia que debe soportar el servicio.
- Tasa de errores aceptable bajo carga.
- Tiempo esperado de recuperación después de un fallo.
- Comportamiento requerido cuando Redis o PostgreSQL no están disponibles.
- Garantías de consistencia y compensación entre Redis y PostgreSQL.
- Mecanismo definitivo de publicación fiable de eventos en Kafka.
- Política de reintentos y circuit breaker, si corresponde.
- Criterios específicos de recuperación y reconciliación.
- Métricas y umbrales de salud que deben supervisarse.
- Herramientas y configuración para pruebas de carga y seguridad.

Estos aspectos no deben convertirse en criterios obligatorios sin una decisión o requisito que los respalde.

## 12. Resultado esperado

La aplicación de esta estrategia debe permitir evaluar la capacidad de Inventory para mantener un comportamiento correcto ante carga, concurrencia y fallos, además de identificar limitaciones de rendimiento y resiliencia.

Los resultados deben aportar evidencia verificable sobre los atributos de calidad y servir para priorizar mejoras antes de considerar el microservicio preparado para su entrega.
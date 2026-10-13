# Datos y entorno de pruebas

## 1. Propósito

Definir los criterios para preparar los datos, configurar los entornos y gestionar los recursos necesarios para ejecutar las pruebas del microservicio Inventory.

El objetivo es garantizar que las pruebas se puedan ejecutar de manera controlada, reproducible y aislada, evitando interferencias entre escenarios y protegiendo los datos ajenos al entorno de pruebas.

Este documento establece las condiciones previstas para la ejecución de las pruebas. No implica que todos los entornos, herramientas o mecanismos descritos estén configurados actualmente.

## 2. Alcance

Este documento cubre:

- Preparación y administración de datos de prueba.
- Configuración de Redis y PostgreSQL.
- Configuración de Kafka cuando la prueba lo requiera.
- Configuración del microservicio y sus variables de entorno.
- Aislamiento y limpieza de los datos.
- Gestión de credenciales de prueba.
- Reproducibilidad de las ejecuciones.
- Registro de la configuración y los resultados.

La estrategia aplica a las pruebas unitarias, de integración, funcionales y de calidad definidas en los documentos anteriores.

## 3. Tipos de entorno de pruebas

### 3.1. Entorno unitario

Se utiliza para verificar las reglas de negocio y los casos de uso de forma aislada.

Características:

- No requiere conexiones reales con Redis, PostgreSQL o Kafka.
- Las dependencias externas se sustituyen por dobles de prueba cuando sea necesario.
- Los datos se preparan dentro de cada escenario.
- Las pruebas deben ejecutarse sin depender de servicios externos.

### 3.2. Entorno de integración

Se utiliza para verificar la comunicación entre el microservicio y sus dependencias.

Puede incluir:

- Redis para la disponibilidad.
- PostgreSQL para la persistencia de reservas.
- Kafka para los escenarios de mensajería que estén implementados.
- El microservicio Inventory y sus adaptadores.
- Herramientas para enviar solicitudes HTTP y verificar respuestas.

Las dependencias necesarias deben estar disponibles y configuradas antes de ejecutar los escenarios correspondientes.

### 3.3. Entorno de calidad y resiliencia

Se utiliza para evaluar rendimiento, concurrencia, seguridad y comportamiento ante fallos.

Debe permitir controlar la carga aplicada, observar las métricas y registrar los resultados.

Las pruebas que interrumpan servicios o modifiquen intensivamente los datos deben ejecutarse en un entorno aislado, separado de cualquier ambiente con datos reales.

## 4. Datos de prueba

### 4.1. Principios generales

Los datos de prueba deben cumplir las siguientes condiciones:

- Ser identificables y comprensibles.
- Estar relacionados con el escenario que se desea verificar.
- Evitar información personal o sensible real.
- Poder crearse y limpiarse de manera controlada.
- Mantener coherencia entre Redis y PostgreSQL cuando el escenario involucre ambos sistemas.
- Permitir reproducir los casos exitosos, los errores y las condiciones límite.

Cada conjunto de datos debe contar con un estado inicial conocido para que el resultado de la prueba pueda compararse con lo esperado.

### 4.2. Datos para disponibilidad

Para probar Redis se deben preparar claves con diferentes condiciones iniciales.

Por ejemplo:

- Una sección con disponibilidad positiva.
- Una sección con disponibilidad igual a cero.
- Una sección con una cantidad pequeña de unidades disponibles.
- Una clave inexistente.
- Una clave con un valor no válido, cuando sea posible y relevante para el adaptador.

Las claves deben respetar el formato definido para el proyecto:

`availability:{event_id}:{section_id}`

Los identificadores utilizados deben corresponder con los datos del escenario y no entrar en conflicto con otros escenarios de prueba.

### 4.3. Datos para reservas

Para probar PostgreSQL y los casos de uso se deben preparar reservas que representen distintas situaciones:

- Reserva válida.
- Usuario sin reservas.
- Usuario con varias reservas.
- Identificador de reserva inexistente.
- Reserva en el estado inicial definido por el contrato.
- Datos que permitan evaluar la cancelación, cuando esta operación esté implementada.

Los estados y las transiciones utilizados deben corresponder con las reglas documentadas. No se deben inventar estados adicionales ni asumir transiciones que todavía no estén aprobadas.

### 4.4. Datos para seguridad

Las pruebas de seguridad pueden utilizar identidades y credenciales ficticias preparadas exclusivamente para el entorno de pruebas.

Se deben contemplar los datos necesarios para verificar:

- Solicitudes autenticadas.
- Tokens inválidos o expirados.
- Diferentes permisos de acceso.
- Intentos de acceder a reservas de otros usuarios.

Los tokens, las claves de firma y las credenciales deben ser de prueba. Nunca deben incluirse secretos reales en el código de las pruebas, los archivos versionados o las evidencias compartidas.

### 4.5. Datos para concurrencia

Las pruebas de concurrencia deben comenzar con una disponibilidad inicial conocida y utilizar un conjunto controlado de solicitudes.

Los datos deben permitir comprobar que la cantidad asignada no supere las unidades disponibles y que el resultado final corresponda con las operaciones aceptadas.

Se deben evitar interferencias con otras ejecuciones que utilicen las mismas claves o reservas.

## 5. Configuración de Redis

Redis debe configurarse para que las pruebas puedan inicializar, consultar y modificar las claves de disponibilidad de forma controlada.

Antes de ejecutar las pruebas se debe verificar:

- Que la conexión esté disponible.
- Que la aplicación utilice la configuración esperada.
- Que las claves de prueba puedan identificarse.
- Que los valores iniciales correspondan con el escenario.
- Que sea posible consultar el estado final después de una operación.

Al finalizar, se deben eliminar o restablecer las claves creadas por la prueba, siempre que hacerlo no afecte datos ajenos a ella.

Para las pruebas de concurrencia y recuperación, se deben registrar los valores iniciales y finales para poder verificar los resultados.

La configuración de Redis, incluidas sus credenciales y dirección de conexión, debe administrarse mediante los mecanismos de configuración del entorno y no mediante secretos escritos directamente en el código.

## 6. Configuración de PostgreSQL

PostgreSQL debe contener el esquema y las restricciones necesarias para ejecutar las pruebas de persistencia.

Antes de las ejecuciones se debe verificar:

- Que la conexión esté disponible.
- Que el esquema corresponda con la versión de la aplicación.
- Que las migraciones requeridas se hayan aplicado.
- Que el usuario de conexión tenga únicamente los permisos necesarios.
- Que los datos de prueba puedan identificarse y limpiarse.

La configuración de persistencia debe ser compatible con el uso de Spring Data R2DBC definido para el microservicio.

Las pruebas deben comprobar tanto la información almacenada como el resultado que devuelve la aplicación.

La limpieza de datos debe realizarse de forma controlada, evitando eliminar registros que pertenezcan a otros escenarios o entornos.

## 7. Configuración de Kafka

Kafka se utilizará en los entornos de prueba cuando los escenarios involucren la integración de mensajería implementada.

Antes de ejecutar esas pruebas se debe verificar:

- Que el servicio esté disponible.
- Que la aplicación tenga configurada la conexión correspondiente.
- Que los topics necesarios existan o puedan crearse mediante el mecanismo aprobado.
- Que los consumidores requeridos estén configurados cuando corresponda.
- Que los mensajes de prueba puedan distinguirse de otros mensajes.
- Que sea posible observar los resultados de publicación y procesamiento.

Los nombres definitivos de los topics, los contratos de los eventos y las garantías de publicación deben corresponder con las decisiones vigentes del proyecto.

Si la integración o el mecanismo de publicación fiable todavía no está implementado, los escenarios correspondientes deben registrarse como pendientes, sin simular que la funcionalidad ya existe.

## 8. Configuración del microservicio

El entorno debe proporcionar la configuración necesaria para ejecutar Inventory y conectarlo con las dependencias correspondientes.

Se deben verificar los siguientes aspectos:

- Puerto y configuración de la API REST.
- Dirección y credenciales de Redis.
- Parámetros de conexión a PostgreSQL.
- Configuración de Kafka, cuando corresponda.
- Parámetros de seguridad necesarios para validar JWT.
- Configuración de registros, métricas y salud.
- Valores de tiempo de espera y resiliencia que estén definidos para la aplicación.

Las variables de entorno y los nombres concretos de las propiedades deben coincidir con la configuración real del proyecto.

No se deben asumir valores por defecto, perfiles de Spring, herramientas de contenedorización o mecanismos de despliegue que no estén documentados.

## 9. Aislamiento de las pruebas

Cada prueba debe operar sobre datos que pueda identificar y controlar.

Para lograrlo se deben aplicar las siguientes medidas:

- Utilizar identificadores específicos para los escenarios.
- Evitar que varias pruebas modifiquen simultáneamente los mismos datos, salvo cuando se esté evaluando concurrencia.
- Restablecer el estado inicial cuando sea necesario.
- No ejecutar pruebas destructivas sobre ambientes con datos reales.
- Evitar que una prueba dependa del resultado dejado por otra.
- Separar los datos de pruebas funcionales de los utilizados en pruebas de carga.

En pruebas que involucren Redis y PostgreSQL, el aislamiento debe considerar ambos sistemas para evitar que los datos queden en estados incompatibles entre ejecuciones.

## 10. Limpieza y restablecimiento

La estrategia de limpieza debe definirse según el tipo de recurso utilizado.

### 10.1. Redis

Eliminar o restablecer las claves creadas por el escenario, verificando que no pertenezcan a otras pruebas.

### 10.2. PostgreSQL

Eliminar o restablecer los registros creados durante la prueba, respetando las restricciones de integridad y el aislamiento del entorno.

### 10.3. Kafka

Cuando sea necesario, utilizar topics o datos de prueba aislados y mecanismos controlados para distinguir los mensajes de cada ejecución.

### 10.4. Aplicación

Detener o restablecer los procesos utilizados cuando sea necesario para evitar que conexiones, tareas pendientes o estados residuales interfieran con las siguientes pruebas.

La limpieza debe ejecutarse de forma segura incluso cuando una prueba falle. Los procedimientos concretos dependerán de las herramientas y del entorno finalmente seleccionados.

## 11. Credenciales y secretos

Las credenciales utilizadas durante las pruebas deben administrarse de forma segura.

Se deben seguir estas reglas:

- No escribir contraseñas, claves de firma o tokens reales directamente en el código.
- No versionar archivos que contengan secretos reales.
- Utilizar variables de entorno o el mecanismo de gestión de secretos definido para el proyecto.
- Limitar los permisos de las cuentas utilizadas para las pruebas.
- Evitar registrar credenciales en mensajes de error o evidencias.
- Rotar o invalidar las credenciales de prueba cuando sea necesario.

Los archivos de configuración de ejemplo deben utilizar valores ficticios claramente identificados.

## 12. Reproducibilidad de las pruebas

Una ejecución debe poder repetirse bajo condiciones equivalentes.

Para facilitarlo, se debe registrar:

- Versión del código evaluado.
- Configuración relevante del entorno.
- Versiones de las dependencias utilizadas cuando estén disponibles.
- Datos iniciales.
- Escenario ejecutado.
- Parámetros de carga o condiciones de fallo.
- Resultado observado.
- Evidencias y errores encontrados.

En las pruebas de concurrencia y rendimiento también se deben registrar las condiciones de ejecución que puedan afectar las mediciones.

Cuando un resultado dependa de una condición no controlada, esta limitación debe documentarse para evitar conclusiones incorrectas.

## 13. Herramientas y automatización

Las herramientas concretas deben seleccionarse según las necesidades del proyecto y la configuración existente.

Se pueden evaluar herramientas para:

- Ejecutar las pruebas automatizadas.
- Preparar y limpiar datos.
- Gestionar las dependencias de integración.
- Enviar solicitudes HTTP.
- Simular carga y concurrencia.
- Recopilar registros y métricas.
- Conservar evidencias de las ejecuciones.

La elección definitiva de herramientas, versiones, comandos y mecanismos de automatización queda pendiente si todavía no se ha aprobado.

## 14. Criterios de preparación del entorno

Antes de iniciar una ejecución, se debe verificar que:

1. El entorno corresponde con el tipo de prueba.
2. La versión del código está identificada.
3. Las dependencias requeridas están disponibles.
4. Las variables de configuración necesarias están definidas.
5. Los datos iniciales están preparados.
6. Las credenciales son apropiadas para el entorno.
7. Las pruebas no afectarán datos o servicios ajenos.
8. Los resultados esperados están definidos.
9. Existe una forma de registrar los resultados y limpiar los recursos utilizados.

Si alguna condición necesaria no se cumple, la prueba debe considerarse bloqueada o no ejecutada, en lugar de interpretarse como aprobada.

## 15. Aspectos pendientes de definición

Se deben confirmar los siguientes puntos cuando todavía no estén definidos:

- Herramientas para preparar y administrar el entorno.
- Mecanismo de ejecución de Redis, PostgreSQL y Kafka durante las pruebas.
- Estrategia de aislamiento entre ejecuciones.
- Procedimientos automatizados de limpieza.
- Variables de entorno definitivas.
- Configuración de credenciales de prueba.
- Versiones de las dependencias utilizadas.
- Herramientas para pruebas de carga, concurrencia y seguridad.
- Conservación y ubicación de las evidencias.
- Automatización de la preparación y restauración de datos.

Las decisiones deben documentarse y mantenerse alineadas con la configuración real del microservicio.

## 16. Resultado esperado

La aplicación de estas pautas debe permitir ejecutar las pruebas de Inventory en condiciones controladas y reproducibles, con datos aislados, dependencias correctamente configuradas y evidencias suficientes para analizar los resultados.

El entorno debe facilitar la detección de defectos, reducir las interferencias entre pruebas y permitir verificar de forma confiable el comportamiento del microservicio.
# Seguridad - Microservicio Inventory

## 1. Propósito

Este documento define los requisitos de seguridad del microservicio Inventory para proteger las operaciones de consulta, creación y cancelación de reservas.

Los requisitos orientarán las decisiones arquitectónicas mediante Attribute-Driven Design (ADD) y serán evaluados mediante pruebas de autenticación, autorización, validación de entradas y acceso a los endpoints.

## 2. Autenticación

### 2.1. Definición

La autenticación es el proceso mediante el cual el sistema verifica la identidad de un usuario antes de permitirle acceder a operaciones protegidas.

### 2.2. Estrategia arquitectónica

Inventory utilizará JSON Web Tokens (JWT) para verificar la identidad y las credenciales representadas en el token, sin consultar al microservicio de Usuarios en cada solicitud.

El cliente enviará el token mediante el encabezado HTTP:

`Authorization: Bearer <JWT>`

El componente de seguridad deberá validar:

- La firma del token mediante la clave o el mecanismo criptográfico configurado.
- La fecha de expiración.
- Los campos requeridos para identificar al usuario.
- Los roles o permisos incluidos en los claims autorizados.
- El algoritmo de firma esperado y los demás requisitos definidos en la configuración de seguridad.

Los tokens inválidos, expirados o que no cumplan los requisitos establecidos deberán rechazarse.

### 2.3. Independencia del servicio de Usuarios

La validación del JWT se realizará localmente mediante Spring Security y el mecanismo criptográfico configurado.

Esta estrategia reduce la dependencia síncrona del microservicio de Usuarios y evita realizar una consulta remota por cada operación protegida.

La verificación local no sustituye la validación criptográfica ni la necesidad de mantener una configuración segura de las claves.

## 3. Autorización

### 3.1. Definición

La autorización determina qué operaciones puede realizar un usuario autenticado según sus roles y permisos.

### 3.2. Roles

Los roles considerados para la API son:

- **`BUYER`:** puede consultar la disponibilidad y gestionar sus propias reservas.
- **`ADMIN`:** puede acceder a las operaciones administrativas expresamente autorizadas.
- **Público:** puede acceder únicamente a los endpoints definidos como públicos.

### 3.3. Reglas de acceso

| Operación | Acceso requerido | Restricción principal |
|---|---|---|
| Consultar disponibilidad | Público o `BUYER` | Solo expone información permitida por la API. |
| Crear una reserva | `BUYER` | El usuario debe estar autenticado y autorizado. |
| Consultar reservas de un usuario | `BUYER` o `ADMIN` | Un comprador solo puede consultar sus propias reservas. |
| Cancelar una reserva | `BUYER` | El comprador solo puede cancelar sus propias reservas, según las reglas del negocio. |

Las solicitudes de un usuario autenticado no deben considerarse autorizadas únicamente por incluir un `userId` o un `reservationId` en la ruta.

La aplicación debe comprobar la relación entre el usuario autenticado y el recurso solicitado. Los permisos administrativos deberán limitarse a las operaciones expresamente definidas.

## 4. Protección de la API REST

### 4.1. Endpoints protegidos

Las operaciones de creación, consulta de historial y cancelación de reservas requieren controles de acceso apropiados.

Los endpoints públicos deberán limitarse a las operaciones de consulta expresamente autorizadas.

### 4.2. Validación de solicitudes

El sistema validará los datos recibidos antes de ejecutar las operaciones de negocio.

Entre las validaciones se incluyen:

- Presencia de los campos obligatorios.
- Formato válido de los identificadores.
- Cantidad de entradas mayor que cero.
- Longitud y formato adecuados de los encabezados.
- Presencia y validez de la clave de idempotencia en la creación de reservas.
- Rechazo de valores que no cumplan las restricciones definidas por el dominio.

Las validaciones de entrada no sustituyen las comprobaciones de autorización ni las reglas de consistencia del inventario.

### 4.3. Manejo de errores

Las respuestas de error deberán informar del problema sin revelar detalles internos de la infraestructura.

El sistema no debe exponer:

- Claves criptográficas ni secretos.
- Tokens JWT completos.
- Contraseñas o credenciales.
- Consultas internas de base de datos.
- Detalles de configuración que faciliten ataques.
- Trazas internas de excepciones en las respuestas destinadas al cliente.

Los errores deberán registrarse de manera que permitan el diagnóstico sin almacenar información sensible innecesaria.

## 5. Protección de Datos y Credenciales

### 5.1. Gestión de secretos

Las claves utilizadas para verificar los JWT y las credenciales de Redis, PostgreSQL y Kafka deberán configurarse mediante mecanismos externos a la lógica de negocio.

Los secretos no deben incluirse directamente en el código fuente ni almacenarse en archivos versionados que puedan quedar expuestos.

### 5.2. Comunicación segura

Cuando el sistema se despliegue en un entorno accesible por red, las comunicaciones deberán protegerse mediante los mecanismos de transporte seguro apropiados, como HTTPS y TLS para las conexiones a las dependencias que lo requieran.

La configuración concreta dependerá del entorno de despliegue y de las capacidades de los servicios utilizados.

### 5.3. Principio de mínimo privilegio

Cada componente y usuario deberá disponer únicamente de los permisos necesarios para realizar sus operaciones.

En particular:

- El acceso a PostgreSQL deberá limitarse a las operaciones requeridas por Inventory.
- El acceso a Redis deberá restringirse a las claves y operaciones necesarias para gestionar la disponibilidad.
- Las credenciales de Kafka deberán limitarse a los permisos de publicación que correspondan.
- Las operaciones administrativas no deberán habilitarse automáticamente para todos los usuarios autenticados.

## 6. Escenarios de Calidad

### 6.1. Escenario de token inválido

- **Atributo:** seguridad.
- **Estímulo:** un cliente envía una solicitud protegida con un JWT inválido, expirado o cuya firma no puede verificarse.
- **Entorno:** Inventory se encuentra operativo.
- **Respuesta esperada:** el sistema rechaza la solicitud antes de ejecutar la operación protegida y devuelve una respuesta HTTP `401 Unauthorized`.
- **Medición:** solicitudes rechazadas, operaciones de negocio ejecutadas y registros de autenticación.

### 6.2. Escenario de acceso no autorizado

- **Atributo:** seguridad.
- **Estímulo:** un usuario autenticado intenta ejecutar una operación para la cual no tiene permisos.
- **Entorno:** el token es válido, pero los roles o la relación con el recurso no permiten la operación.
- **Respuesta esperada:** el sistema rechaza el acceso y devuelve `403 Forbidden`, cuando corresponda.
- **Medición:** accesos denegados, operaciones ejecutadas y registros de autorización.

### 6.3. Escenario de acceso a reservas ajenas

- **Atributo:** seguridad.
- **Estímulo:** un comprador intenta consultar o cancelar una reserva perteneciente a otro usuario.
- **Entorno:** ambos usuarios tienen tokens válidos.
- **Respuesta esperada:** Inventory impide la operación si el comprador no tiene autorización sobre la reserva.
- **Medición:** solicitudes rechazadas y ausencia de cambios no autorizados en las reservas.

### 6.4. Escenario de solicitud malformada

- **Atributo:** seguridad y robustez.
- **Estímulo:** un cliente envía una solicitud con campos obligatorios ausentes, cantidades inválidas o identificadores con formatos no admitidos.
- **Entorno:** Inventory recibe la solicitud a través de la API REST.
- **Respuesta esperada:** el sistema rechaza la solicitud mediante una respuesta de validación apropiada, sin modificar la disponibilidad ni crear una reserva.
- **Medición:** solicitudes inválidas rechazadas, cambios en Redis y registros en PostgreSQL.

### 6.5. Escenario de exposición de información sensible

- **Atributo:** seguridad.
- **Estímulo:** una operación provoca un error de autenticación, persistencia o comunicación con una dependencia.
- **Entorno:** Inventory gestiona la excepción y construye la respuesta HTTP.
- **Respuesta esperada:** la respuesta no revela secretos, tokens completos, credenciales ni detalles internos innecesarios.
- **Medición:** revisión de las respuestas HTTP y de los registros generados.

## 7. Estrategia de Evaluación

La seguridad se evaluará mediante pruebas automatizadas de integración y pruebas específicas de acceso a los recursos.

Las pruebas deberán verificar:

1. El rechazo de tokens inválidos o expirados.
2. La autorización según los roles definidos.
3. La imposibilidad de consultar o modificar reservas de otros compradores sin permiso.
4. El rechazo de solicitudes que no cumplan las validaciones de entrada.
5. La ausencia de cambios en el inventario cuando una solicitud es rechazada por seguridad.
6. La ausencia de información sensible en las respuestas de error.
7. La correcta gestión de las credenciales y configuraciones de seguridad.

## 8. Criterios de Aceptación

El microservicio deberá cumplir los siguientes criterios:

- Los endpoints protegidos deben validar la autenticidad del JWT.
- Los permisos deben verificarse en función del rol y del recurso solicitado.
- Un comprador no puede acceder a las reservas de otros compradores sin autorización.
- Las solicitudes inválidas deben rechazarse antes de ejecutar operaciones de negocio.
- Los errores no deben revelar información sensible.
- Las credenciales y claves criptográficas no deben estar incorporadas directamente en el código fuente.
- Las pruebas deben demostrar que las solicitudes no autorizadas no modifican la disponibilidad ni las reservas.

## 9. Relación con Otros Atributos de Calidad

- **Consistencia:** las solicitudes no autorizadas no deben modificar el inventario.
- **Disponibilidad y resiliencia:** los errores de autenticación y autorización deben gestionarse sin interrumpir innecesariamente el servicio.
- **Rendimiento:** la validación local del JWT reduce la necesidad de consultas síncronas al microservicio de Usuarios.
- **Observabilidad:** los registros permiten identificar intentos de acceso no autorizado y errores de seguridad.
- **Mantenibilidad:** la separación de la seguridad en la infraestructura permite modificar los mecanismos de autenticación sin introducir dependencias en el dominio.
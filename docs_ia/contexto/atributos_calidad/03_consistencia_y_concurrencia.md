# Consistencia y Concurrencia - Microservicio Inventory

## 1. Propósito

Este documento define los requisitos de consistencia y concurrencia del microservicio Inventory, considerando la gestión de disponibilidad y reservas durante escenarios de alta demanda.

Estos atributos orientarán las decisiones arquitectónicas mediante Attribute-Driven Design (ADD) y serán evaluados mediante pruebas de concurrencia, consistencia e idempotencia.

## 2. Concurrencia

### 2.1. Definición

Capacidad del sistema para procesar múltiples solicitudes simultáneamente sin producir conflictos, resultados incorrectos ni modificaciones incompatibles del inventario.

### 2.2. Importancia

Durante una venta de entradas, varios usuarios pueden intentar reservar entradas de la misma localidad al mismo tiempo.

Si las operaciones no se coordinan correctamente, el sistema podría permitir que diferentes solicitudes consuman las mismas entradas y generar una sobreventa (*overselling*).

### 2.3. Estrategia arquitectónica

La disponibilidad se gestionará mediante operaciones atómicas sobre contadores de Redis.

Cada contador representa la cantidad de entradas disponibles para un evento y una sección determinados.

La clave seguirá la estructura:

`availability:{event_id}:{section_id}`

Para solicitar una reserva de una cantidad determinada de entradas, el sistema utilizará una operación atómica de decremento mediante `DECRBY`.

El resultado de la operación permitirá determinar si la disponibilidad continúa siendo válida:

- Si el resultado es mayor o igual a cero, la operación puede continuar con el registro de la reserva.
- Si el resultado es negativo, la operación debe compensarse mediante un incremento equivalente y la solicitud debe rechazarse.

La lógica de aplicación coordinará la operación sobre Redis con el registro de la reserva en PostgreSQL mediante R2DBC.

### 2.4. Consideraciones

- Las operaciones de actualización de disponibilidad deben ser atómicas.
- No se debe utilizar una secuencia independiente de lectura y escritura para decidir si existe disponibilidad suficiente.
- Las solicitudes concurrentes deben operar sobre el mismo contador compartido para cada evento y sección.
- Las operaciones de compensación deben ejecutarse de manera controlada.
- El sistema debe contemplar fallos durante el registro de la reserva y durante la compensación.

## 3. Consistencia

### 3.1. Definición

Capacidad del sistema para mantener coherentes la disponibilidad de entradas y los registros de reservas, respetando las reglas de negocio incluso ante solicitudes concurrentes o fallos de infraestructura.

### 3.2. Regla principal

La cantidad total de entradas reservadas no puede superar la capacidad disponible del evento o sección.

Para una sección determinada, el sistema debe mantener la siguiente invariante:

`Entradas reservadas + Entradas disponibles = Capacidad total`

Esta relación debe evaluarse teniendo en cuenta el estado de las reservas y las operaciones que todavía están en proceso.

### 3.3. Responsabilidad de los componentes

- **Redis:** administra los contadores de disponibilidad utilizados para controlar las reservas en tiempo real.
- **PostgreSQL:** conserva el registro persistente de las reservas y sus estados.
- **Capa de aplicación:** coordina las operaciones y aplica las reglas de negocio.
- **Capa de dominio:** representa las entidades y reglas que deben respetarse durante el ciclo de vida de una reserva.

### 3.4. Consistencia entre Redis y PostgreSQL

Redis y PostgreSQL no comparten una transacción atómica entre ambos sistemas. Por ello, una operación que modifica Redis y posteriormente registra una reserva en PostgreSQL puede fallar parcialmente.

La estrategia inicial será:

1. Descontar atómicamente la cantidad solicitada en Redis.
2. Verificar que el resultado no sea negativo.
3. Registrar la reserva en PostgreSQL con estado `PENDING`.
4. Si el registro tiene éxito, continuar con el flujo definido para la reserva.
5. Si el registro falla, intentar restaurar la disponibilidad en Redis.
6. Registrar y supervisar cualquier fallo de compensación.

La compensación reduce el riesgo de inconsistencias, pero no garantiza por sí sola que ambos sistemas permanezcan sincronizados en todos los escenarios. El sistema deberá poder detectar y recuperar discrepancias cuando una compensación falle.

## 4. Idempotencia

### 4.1. Definición

Propiedad que permite reconocer solicitudes repetidas y evitar que una misma operación lógica produzca múltiples reservas o descuentos de disponibilidad.

### 4.2. Importancia

Un cliente puede reenviar una solicitud cuando no recibe respuesta a tiempo, aunque la operación original haya sido procesada.

Sin idempotencia, cada reintento podría descontar nuevamente las entradas y crear una reserva adicional.

### 4.3. Estrategia

La creación de reservas utilizará el encabezado HTTP obligatorio:

`Idempotency-Key: <UUID>`

El sistema deberá:

- Asociar la clave de idempotencia con la operación solicitada.
- Detectar solicitudes que reutilicen una clave ya procesada.
- Evitar ejecutar nuevamente el descuento de disponibilidad cuando la solicitud corresponda a una operación ya registrada.
- Devolver el resultado previamente asociado a la solicitud cuando corresponda.
- Garantizar la unicidad de la clave mediante una restricción persistente en PostgreSQL o un mecanismo equivalente correctamente coordinado.

La comprobación de la clave y la creación de la reserva deberán coordinarse para evitar que dos solicitudes simultáneas con la misma clave generen reservas duplicadas.

Una clave reutilizada con un contenido diferente deberá gestionarse según una política explícita de validación.

## 5. Ciclo de Vida de las Reservas

Las reservas utilizarán los siguientes estados:

- **`PENDING`:** reserva registrada que todavía no ha completado el proceso de confirmación.
- **`CONFIRMED`:** reserva confirmada según las reglas del sistema.
- **`CANCELLED`:** reserva cancelada que ya no debe consumir disponibilidad.

Las transiciones deberán validarse en la capa de dominio y coordinarse con las operaciones de persistencia y disponibilidad.

### 5.1. Confirmación

Una reserva solo podrá pasar a `CONFIRMED` si cumple las condiciones definidas para su confirmación.

La transición no debe ejecutarse más de una vez ni provocar publicaciones duplicadas de eventos de negocio.

### 5.2. Cancelación

La cancelación de una reserva activa debe actualizar su estado y liberar la cantidad correspondiente de entradas.

La liberación debe ejecutarse de forma controlada para impedir que una misma reserva devuelva entradas varias veces debido a solicitudes concurrentes o reintentos.

Las reservas confirmadas o canceladas deberán respetar las reglas de transición establecidas por el negocio.

## 6. Escenarios de Calidad

### 6.1. Escenario de alta concurrencia

- **Atributo:** concurrencia.
- **Estímulo:** múltiples usuarios intentan reservar simultáneamente entradas de la misma sección.
- **Entorno:** Inventory opera con Redis y PostgreSQL disponibles.
- **Respuesta esperada:** las operaciones se procesan sin permitir que la cantidad reservada supere la capacidad disponible.
- **Medición:** reservas exitosas, reservas rechazadas, disponibilidad final, errores y cantidad total reservada.

### 6.2. Escenario de disponibilidad limitada

- **Atributo:** consistencia.
- **Estímulo:** quedan pocas entradas disponibles y varias solicitudes intentan reservar una cantidad superior a la disponibilidad restante.
- **Entorno:** el sistema recibe solicitudes concurrentes sobre la misma sección.
- **Respuesta esperada:** únicamente se aceptan las reservas que pueden satisfacerse sin superar la capacidad disponible.
- **Medición:** capacidad inicial, cantidad reservada, disponibilidad final y diferencias entre Redis y PostgreSQL.

### 6.3. Escenario de solicitud duplicada

- **Atributo:** idempotencia.
- **Estímulo:** el cliente envía varias veces la misma solicitud utilizando la misma clave de idempotencia.
- **Entorno:** la solicitud original puede haber sido procesada total o parcialmente.
- **Respuesta esperada:** los reintentos no deben generar reservas adicionales ni repetir el descuento de disponibilidad para una operación ya registrada.
- **Medición:** cantidad de reservas creadas, número de descuentos ejecutados y respuestas devueltas.

### 6.4. Escenario de cancelaciones concurrentes

- **Atributo:** concurrencia y consistencia.
- **Estímulo:** varias solicitudes intentan cancelar la misma reserva simultáneamente.
- **Entorno:** la reserva existe y se encuentra en un estado que permite su cancelación.
- **Respuesta esperada:** la reserva se cancela una sola vez y la disponibilidad se incrementa únicamente por la cantidad correspondiente.
- **Medición:** estado final de la reserva, número de operaciones de liberación y disponibilidad final.

### 6.5. Escenario de fallo durante la persistencia

- **Atributo:** consistencia y resiliencia.
- **Estímulo:** Redis descuenta entradas correctamente, pero PostgreSQL falla al registrar la reserva.
- **Entorno:** el sistema está operativo, salvo por el fallo de persistencia.
- **Respuesta esperada:** Inventory intenta compensar el descuento y registra cualquier fallo de compensación para su detección y recuperación.
- **Medición:** registros persistidos, operaciones compensatorias, disponibilidad final y discrepancias detectadas.

## 7. Estrategia de Evaluación

La evaluación se realizará mediante pruebas automatizadas y pruebas de carga que simulen solicitudes simultáneas sobre las mismas secciones.

Las pruebas deberán verificar:

1. Que las operaciones atómicas impidan asignar las mismas entradas a varias reservas.
2. Que la cantidad reservada no supere la capacidad definida.
3. Que los reintentos con una misma clave de idempotencia no creen reservas duplicadas.
4. Que las cancelaciones concurrentes no liberen entradas más de una vez.
5. Que los fallos de persistencia y compensación puedan detectarse.
6. Que el estado final del inventario sea coherente con las reservas registradas y sus estados.

Las pruebas deberán registrar tanto las respuestas HTTP como los resultados persistidos y los contadores finales de Redis.

## 8. Criterios de Aceptación

El microservicio deberá cumplir los siguientes criterios:

- No se permite que la cantidad reservada supere la capacidad disponible.
- Las modificaciones de disponibilidad deben utilizar operaciones atómicas.
- Las solicitudes duplicadas con una misma clave de idempotencia no deben generar reservas adicionales.
- La cancelación de una reserva debe liberar su disponibilidad una sola vez.
- Los cambios de estado deben respetar las transiciones permitidas.
- Los fallos de persistencia y compensación deben poder identificarse mediante registros y métricas.
- Las pruebas deben permitir comparar la disponibilidad en Redis con los registros de reservas en PostgreSQL.

## 9. Relación con Otros Atributos de Calidad

- **Rendimiento:** las operaciones atómicas deben mantener tiempos de respuesta adecuados bajo carga concurrente.
- **Escalabilidad:** las instancias de Inventory deben coordinarse mediante recursos compartidos para preservar la consistencia.
- **Disponibilidad y resiliencia:** los fallos de infraestructura deben gestionarse sin confirmar operaciones incorrectas.
- **Observabilidad:** las métricas y los registros permiten detectar duplicados, errores de compensación y discrepancias.
- **Mantenibilidad:** la separación entre dominio, aplicación e infraestructura permite modificar los mecanismos de almacenamiento sin trasladar sus detalles a las reglas de negocio.
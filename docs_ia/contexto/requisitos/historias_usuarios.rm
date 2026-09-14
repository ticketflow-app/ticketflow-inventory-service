# Historias de Usuario

## 1. Convención

Las historias de usuario utilizan la siguiente estructura:

Como [actor]

Quiero [funcionalidad]

Para [beneficio]

Los criterios de aceptación utilizan sintaxis Gherkin:

Given / Dado
When / Cuando
Then / Entonces

---

# 2. Gestión de usuarios

## HU-01 - Registrar usuario

**Como** comprador u organizador

**Quiero** registrarme en el sistema

**Para** poder utilizar las funcionalidades correspondientes a mi rol.

### Criterios de aceptación

#### Escenario 1 - Registro exitoso

Given que el usuario no está registrado

When proporciona nombre, correo y contraseña válidos

Then el sistema debe crear el usuario.

#### Escenario 2 - Correo existente

Given que existe un usuario con el correo proporcionado

When otro usuario intenta registrarse con el mismo correo

Then el sistema debe rechazar el registro.

#### Escenario 3 - Datos inválidos

Given que el usuario proporciona información inválida

When intenta registrarse

Then el sistema debe informar que los datos no cumplen las reglas requeridas.

---

## HU-02 - Iniciar sesión

**Como** usuario registrado

**Quiero** iniciar sesión

**Para** acceder a las funcionalidades correspondientes a mi rol.

### Criterios de aceptación

#### Escenario 1 - Credenciales válidas

Given que el usuario está registrado

When proporciona credenciales válidas

Then el sistema debe autenticarlo y generar un token de acceso.

#### Escenario 2 - Credenciales inválidas

Given que el usuario proporciona credenciales incorrectas

When intenta iniciar sesión

Then el sistema debe rechazar la autenticación.

---

## HU-03 - Consultar perfil

**Como** usuario autenticado

**Quiero** consultar mi perfil

**Para** conocer la información asociada a mi cuenta.

### Criterios de aceptación

#### Escenario 1

Given que el usuario está autenticado

When solicita su perfil

Then el sistema debe retornar su información básica.

---

# 3. Gestión de eventos

## HU-04 - Crear evento

**Como** organizador

**Quiero** crear un evento

**Para** publicar una actividad que pueda ser consultada por los compradores.

### Criterios de aceptación

#### Escenario 1

Given que el organizador está autenticado

When proporciona los datos requeridos del evento

Then el sistema debe crear el evento.

#### Escenario 2

Given que faltan datos obligatorios

When el organizador intenta crear el evento

Then el sistema debe rechazar la operación.

---

## HU-05 - Consultar eventos

**Como** comprador

**Quiero** consultar los eventos disponibles

**Para** seleccionar un evento de mi interés.

### Criterios de aceptación

#### Escenario 1

Given que existen eventos registrados

When el comprador consulta los eventos

Then el sistema debe retornar los eventos disponibles.

---

## HU-06 - Buscar y filtrar eventos

**Como** comprador

**Quiero** buscar y filtrar eventos

**Para** encontrar rápidamente los eventos de mi interés.

### Criterios de aceptación

#### Escenario 1

Given que existen múltiples eventos

When el comprador aplica un criterio de búsqueda

Then el sistema debe retornar los eventos que coincidan.

#### Escenario 2

Given que existen eventos con diferentes categorías o fechas

When el comprador aplica filtros

Then el sistema debe retornar únicamente los eventos que cumplan los filtros.

---

## HU-07 - Consultar detalle del evento

**Como** comprador

**Quiero** consultar el detalle de un evento

**Para** conocer sus características antes de reservar.

### Criterios de aceptación

#### Escenario 1

Given que el evento existe

When el comprador solicita su detalle

Then el sistema debe retornar la información del evento.

---

## HU-08 - Modificar evento

**Como** organizador

**Quiero** modificar un evento

**Para** mantener actualizada su información.

### Criterios de aceptación

#### Escenario 1

Given que el organizador tiene permisos

When modifica información válida del evento

Then el sistema debe actualizar la información.

---

# 4. Gestión de inventario

## HU-09 - Consultar disponibilidad

**Como** comprador

**Quiero** consultar la disponibilidad de entradas

**Para** conocer cuántas entradas puedo reservar.

### Criterios de aceptación

#### Escenario 1

Given que existe un evento con localidades configuradas

When el comprador consulta la disponibilidad

Then el sistema debe mostrar la disponibilidad actual.

---

## HU-10 - Configurar localidades

**Como** organizador

**Quiero** configurar las localidades de un evento

**Para** definir las opciones disponibles para los compradores.

### Criterios de aceptación

#### Escenario 1

Given que el organizador está creando un evento

When define una localidad válida

Then el sistema debe registrar la localidad.

---

## HU-11 - Configurar capacidad

**Como** organizador

**Quiero** definir la capacidad de una localidad

**Para** establecer la cantidad máxima de entradas disponibles.

### Criterios de aceptación

#### Escenario 1

Given que existe una localidad

When el organizador establece una capacidad válida

Then el sistema debe registrar la capacidad total.

---

# 5. Gestión de reservas

## HU-12 - Crear reserva

**Como** comprador

**Quiero** reservar entradas

**Para** asegurar mi participación en un evento.

### Criterios de aceptación

#### Escenario 1 - Reserva exitosa

Given que existe disponibilidad suficiente

When el comprador solicita una cantidad válida de entradas

Then el sistema debe crear la reserva

And debe disminuir la disponibilidad correspondiente.

#### Escenario 2 - Sin disponibilidad

Given que no existe disponibilidad suficiente

When el comprador intenta realizar una reserva

Then el sistema debe rechazar la reserva.

---

## HU-13 - Evitar sobreventa

**Como** sistema

**Quiero** controlar las operaciones concurrentes

**Para** evitar asignar más entradas de las disponibles.

### Criterios de aceptación

#### Escenario 1 - Una entrada disponible

Given que existe una sola entrada disponible

And existen dos solicitudes concurrentes

When ambas intentan reservar la entrada

Then únicamente una solicitud debe ser aceptada

And la otra debe ser rechazada

And la disponibilidad final debe ser cero.

#### Escenario 2 - Múltiples reservas simultáneas

Given que existe una cantidad limitada de entradas

When múltiples usuarios realizan reservas simultáneas

Then el sistema no debe superar la capacidad disponible.

---

## HU-14 - Consultar disponibilidad actualizada

**Como** comprador

**Quiero** consultar la disponibilidad actual

**Para** conocer el inventario antes de reservar.

### Criterios de aceptación

#### Escenario 1

Given que existen cambios en el inventario

When el comprador consulta la disponibilidad

Then el sistema debe retornar el estado actualizado.

---

## HU-15 - Consultar mis reservas

**Como** comprador

**Quiero** consultar mis reservas

**Para** conocer las reservas realizadas.

### Criterios de aceptación

#### Escenario 1

Given que el comprador tiene reservas

When consulta su historial

Then el sistema debe retornar sus reservas.

---

## HU-16 - Consultar detalle de reserva

**Como** comprador

**Quiero** consultar el detalle de una reserva

**Para** conocer la información asociada.

### Criterios de aceptación

#### Escenario 1

Given que la reserva pertenece al comprador

When solicita el detalle

Then el sistema debe retornar la información de la reserva.

---

## HU-17 - Cancelar reserva

**Como** comprador

**Quiero** cancelar una reserva

**Para** liberar las entradas cuando corresponda.

### Criterios de aceptación

#### Escenario 1

Given que la reserva puede ser cancelada

When el comprador solicita la cancelación

Then el sistema debe cambiar el estado de la reserva

And debe liberar las entradas correspondientes.

---

# 6. Evaluación de rendimiento

## HU-18 - Ejecutar prueba de carga

**Como** investigador del proyecto

**Quiero** ejecutar escenarios de carga

**Para** evaluar el comportamiento del sistema.

### Criterios de aceptación

#### Escenario 1

Given que el sistema está disponible

When se ejecuta un escenario de carga

Then deben registrarse las métricas de la prueba.

---

## HU-19 - Simular pico de tráfico

**Como** investigador del proyecto

**Quiero** simular incrementos repentinos de usuarios

**Para** evaluar el comportamiento del sistema ante alta demanda.

### Criterios de aceptación

#### Escenario 1

Given que el sistema está funcionando con carga normal

When se incrementa significativamente el número de solicitudes

Then el sistema debe continuar procesando solicitudes dentro de los límites establecidos para el experimento.

---

## HU-20 - Medir rendimiento

**Como** investigador del proyecto

**Quiero** medir el rendimiento del sistema

**Para** comparar su comportamiento bajo diferentes escenarios de carga.

### Criterios de aceptación

#### Escenario 1

Given que se ejecuta una prueba de carga

When finaliza la prueba

Then deben estar disponibles métricas de latencia, throughput y errores.

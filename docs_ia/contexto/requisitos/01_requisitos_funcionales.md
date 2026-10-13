# Requisitos Funcionales - Microservicio de Inventario (Inventory)

## 1. Alcance del Microservicio
Este microservicio es responsable de gestionar las localidades, controlar la capacidad máxima, administrar la disponibilidad en tiempo real y ejecutar operaciones atómicas de reserva y cancelación de entradas garantizando la prevención de sobreventa.

---

## 2. Mapeo de Épicas y Features

### Épica E3: Gestión de Localidades e Inventario
- **F3.1 Crear localidad:** Registrar las áreas o secciones asignadas a un evento.
- **F3.2 Definir precio:** Asignar el valor económico por tipo de entrada/localidad.
- **F3.3 Definir capacidad:** Establecer el límite máximo de entradas disponibles por localidad.
- **F3.4 Consultar disponibilidad general:** Permitir la lectura del stock disponible por evento.
- **F3.5 Consultar disponibilidad por localidad:** Obtener el detalle de inventario de una sección específica.
- **F3.6 Actualizar disponibilidad:** Incrementar o decrementar el stock disponible según operaciones de reserva o cancelación.

### Épica E4: Gestión de Reservas (Core de Inventario)
- **F4.1 Seleccionar localidad:** Identificar la sección destino para la reserva.
- **F4.2 Seleccionar cantidad:** Validar límites de cantidad por solicitud.
- **F4.3 Validar disponibilidad:** Verificar stock en tiempo real antes de procesar.
- **F4.4 Crear reserva:** Procesar la solicitud de reserva disminuyendo el stock.
- **F4.5 Registrar reserva:** Persistir el comprobante de la reserva.
- **F4.6 Controlar concurrencia:** Prevenir la asignación simultánea de más entradas de las existentes.
- **F4.7 Consultar reserva:** Obtener detalle de una reserva activa.
- **F4.8 Consultar historial de reservas:** Obtener reservas realizadas por comprador.
- **F4.9 Cancelar reserva:** Cambiar estado de la reserva y liberar el stock asignado.
- **F4.10 Liberar entradas:** Reintegrar la cantidad de entradas al inventario disponible.

### Épica E5: Control de Concurrencia y Consistencia
- **F5.1 Operación atómica de reserva:** Garantizar que la reducción de stock y la creación de la reserva se realicen como una unidad indivisible.
- **F5.2 Validación de disponibilidad:** Bloqueo o control optimista/pesimista sobre el stock.
- **F5.3 Prevención de sobreventa:** Evitar que el stock disminuya de cero (0).
- **F5.4 Control de operaciones concurrentes:** Garantizar atomicidad en picos de alta demanda.
- **F5.5 Recuperación ante errores:** Mecanismos de reintento o rollback si falla la persistencia tras afectar inventario.
- **F5.6 Consistencia del inventario:** Mantenimiento de la integridad de datos entre stock total, reservado y disponible.

---

## 3. Historias de Usuario

### HU-09 - Consultar disponibilidad
**Como** comprador  
**Quiero** consultar la disponibilidad de entradas  
**Para** conocer cuántas entradas puedo reservar.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que existe un evento con localidades configuradas
  - **When** el comprador consulta la disponibilidad
  - **Then** el sistema debe mostrar la disponibilidad actual por localidad.

---

### HU-10 - Configurar localidades
**Como** organizador  
**Quiero** configurar las localidades de un evento  
**Para** definir las opciones disponibles para los compradores.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que el organizador está creando un evento
  - **When** define una localidad válida
  - **Then** el sistema debe registrar la localidad.

---

### HU-11 - Configurar capacidad
**Como** organizador  
**Quiero** definir la capacidad de una localidad  
**Para** establecer la cantidad máxima de entradas disponibles.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que existe una localidad
  - **When** el organizador establece una capacidad válida
  - **Then** el sistema debe registrar la capacidad total.

---

### HU-12 - Crear reserva
**Como** comprador  
**Quiero** reservar entradas  
**Para** asegurar mi participación en un evento.

#### Criterios de aceptación
- **Escenario 1 - Reserva exitosa:**
  - **Given** que existe disponibilidad suficiente
  - **When** el comprador solicita una cantidad válida de entradas
  - **Then** el sistema debe crear la reserva
  - **And** debe disminuir la disponibilidad correspondiente.
- **Escenario 2 - Sin disponibilidad:**
  - **Given** que no existe disponibilidad suficiente
  - **When** el comprador intenta realizar una reserva
  - **Then** el sistema debe rechazar la reserva.

---

### HU-13 - Evitar sobreventa (Control de Concurrencia)
**Como** sistema  
**Quiero** controlar las operaciones concurrentes  
**Para** evitar asignar más entradas de las disponibles.

#### Criterios de aceptación
- **Escenario 1 - Una entrada disponible:**
  - **Given** que existe una sola entrada disponible
  - **And** existen dos solicitudes concurrentes
  - **When** ambas intentan reservar la entrada
  - **Then** únicamente una solicitud debe ser aceptada
  - **And** la otra debe ser rechazada
  - **And** la disponibilidad final debe ser cero.
- **Escenario 2 - Múltiples reservas simultáneas:**
  - **Given** que existe una cantidad limitada de entradas
  - **When** múltiples usuarios realizan reservas simultáneas
  - **Then** el sistema no debe superar la capacidad disponible.

---

### HU-14 - Consultar disponibilidad actualizada
**Como** comprador  
**Quiero** consultar la disponibilidad actual  
**Para** conocer el inventario antes de reservar.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que existen cambios en el inventario
  - **When** el comprador consulta la disponibilidad
  - **Then** el sistema debe retornar el estado actualizado.

---

### HU-15 - Consultar mis reservas
**Como** comprador  
**Quiero** consultar mis reservas  
**Para** conocer las reservas realizadas.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que el comprador tiene reservas
  - **When** consulta su historial
  - **Then** el sistema debe retornar sus reservas.

---

### HU-16 - Consultar detalle de reserva
**Como** comprador  
**Quiero** consultar el detalle de una reserva  
**Para** conocer la información asociada.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que la reserva pertenece al comprador
  - **When** solicita el detalle
  - **Then** el sistema debe retornar la información de la reserva.

---

### HU-17 - Cancelar reserva
**Como** comprador  
**Quiero** cancelar una reserva  
**Para** liberar las entradas cuando corresponda.

#### Criterios de aceptación
- **Escenario 1:**
  - **Given** que la reserva puede ser cancelada
  - **When** el comprador solicita la cancelación
  - **Then** el sistema debe cambiar el estado de la reserva
  - **And** debe liberar las entradas correspondientes en el inventario.

---

## 4. Casos de Uso Específicos del Microservicio

### CU-05 - Consultar disponibilidad
- **Actor principal:** Comprador / Sistema consumidor.
- **Objetivo:** Conocer las entradas disponibles.
- **Precondiciones:** El evento y localidad deben existir.
- **Flujo Principal:**
  1. El usuario/sistema solicita la disponibilidad indicando el ID del evento/localidad.
  2. Inventory identifica la localidad y consulta el stock en tiempo real.
  3. Inventory retorna la cantidad disponible por localidad.

---

### CU-06 - Crear reserva
- **Actor principal:** Comprador.
- **Objetivo:** Reservar una cantidad determinada de entradas de forma segura.
- **Precondiciones:**
  - Usuario autenticado.
  - Evento y localidad existentes.
  - Cantidad solicitada mayor a cero.
- **Flujo Principal:**
  1. El comprador solicita reservar $N$ entradas para una localidad.
  2. Inventory recibe la solicitud.
  3. Inventory ejecuta una operación atómica sobre la disponibilidad.
  4. El sistema determina si existe disponibilidad suficiente ($Stock \ge N$).
  5. Si hay disponibilidad, decrementa el stock en $N$ y genera el registro de reserva en estado `RESERVADO`.
  6. El sistema retorna los detalles de la reserva realizada.
- **Flujos Alternativos:**
  - **FA-01 - Disponibilidad insuficiente:**
    1. Inventory detecta $Stock < N$.
    2. La operación es rechazada y no se altera el inventario.
    3. Retorna un mensaje notificando la falta de disponibilidad.
  - **FA-02 - Solicitudes concurrentes:**
    1. Múltiples solicitudes intentan reservar simultáneamente sobre el mismo stock.
    2. Inventory procesa las operaciones de manera atómica (mediante bloqueos o transacciones aisladas).
    3. Solo se aceptan aquellas que no sobrepasen la capacidad máxima.
  - **FA-03 - Error durante la persistencia:**
    1. Si falla la persistencia tras afectar el inventario, se ejecuta un rollback/compensación.
- **Postcondición:**
  - Reserva creada y asociada al usuario.
  - Disponibilidad decrementada correctamente.
  - El stock acumulado nunca es menor a cero.

---

### CU-07 - Cancelar reserva
- **Actor principal:** Comprador.
- **Objetivo:** Cancelar una reserva previa y reintegrar las entradas al inventario.
- **Precondiciones:**
  - La reserva debe existir y pertenecer al usuario.
  - Debe estar en estado que permita cancelación (ej: `RESERVADO` / `PENDIENTE_PAGO`).
- **Flujo Principal:**
  1. El comprador solicita cancelar la reserva mediante el ID.
  2. El sistema valida la propiedad y el estado de la reserva.
  3. El sistema cambia el estado de la reserva a `CANCELADA`.
  4. Inventory incrementa el stock disponible en la cantidad correspondiente.
  5. El sistema confirma la liberación del inventario.
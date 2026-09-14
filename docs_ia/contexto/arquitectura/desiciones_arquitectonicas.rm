# Decisiones Arquitectónicas

## ADR-001 - Utilizar arquitectura de microservicios

### Estado

Aceptada.

### Contexto

El proyecto busca estudiar aplicaciones de gran escala y atributos como escalabilidad, disponibilidad y concurrencia.

### Decisión

Se utilizarán microservicios independientes.

Los servicios iniciales serán:

- Users.
- Catalog.
- Inventory.

### Justificación

La separación permite analizar el comportamiento de diferentes componentes de forma independiente.

También facilita estudiar escalabilidad individual.

### Consecuencia

Aumenta la complejidad respecto a un monolito.

Por esta razón, el número de microservicios se mantiene reducido.

---

# ADR-002 - Utilizar arquitectura hexagonal

### Estado

Aceptada.

### Contexto

Se necesita separar las reglas de negocio de las tecnologías utilizadas.

### Decisión

Cada microservicio utilizará arquitectura hexagonal.

### Justificación

Permite separar:

- Dominio.
- Casos de uso.
- Interfaces.
- Persistencia.
- APIs.
- Tecnologías externas.

### Consecuencia

El proyecto tendrá una estructura interna más organizada, aunque inicialmente requiera más clases e interfaces.

---

# ADR-003 - Utilizar Redis para disponibilidad

### Estado

Aceptada.

### Contexto

La disponibilidad será consultada y modificada frecuentemente.

### Decisión

Redis será utilizado para almacenar contadores de disponibilidad.

### Justificación

Las operaciones atómicas sobre contadores permiten manejar concurrencia de manera eficiente.

### Consecuencia

Debe considerarse la sincronización entre Redis y la persistencia de reservas.

---

# ADR-004 - Utilizar PostgreSQL para persistencia

### Estado

Aceptada.

### Contexto

Las reservas deben mantenerse de forma persistente.

### Decisión

Se utilizará PostgreSQL para los datos persistentes.

### Justificación

Proporciona persistencia, consistencia y capacidades relacionales adecuadas para el dominio.

---

# ADR-005 - Utilizar JWT

### Estado

Aceptada.

### Contexto

Los microservicios necesitan identificar y autorizar usuarios.

### Decisión

Se utilizarán tokens JWT.

### Justificación

Los servicios pueden validar el token sin consultar Users para cada solicitud.

Esto reduce dependencia entre microservicios.

### Consecuencia

Debe garantizarse la protección adecuada de la clave utilizada para firmar los tokens.

---

# ADR-006 - Separar Catalog e Inventory

### Estado

Aceptada.

### Contexto

La información del evento cambia con poca frecuencia, mientras que la disponibilidad puede cambiar constantemente.

### Decisión

Catalog manejará la configuración del evento.

Inventory manejará la disponibilidad actual.

### Justificación

La separación permite optimizar las consultas y operaciones críticas de inventario.

---

# ADR-007 - Redis como mecanismo de disponibilidad y PostgreSQL como persistencia

### Estado

Aceptada con riesgo conocido.

### Contexto

Una operación de reserva puede modificar Redis y posteriormente persistir la reserva en PostgreSQL.

### Decisión

Redis manejará el contador de disponibilidad y PostgreSQL almacenará la reserva.

### Riesgo

Puede existir una ventana de inconsistencia si la modificación de Redis es exitosa pero la persistencia en PostgreSQL falla.

### Acción futura

Se deberá evaluar un mecanismo de recuperación adecuado y realizar pruebas específicas de fallo.

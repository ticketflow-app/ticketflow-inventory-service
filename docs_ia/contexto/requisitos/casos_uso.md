# Casos de Uso

## 1. Propósito

Este documento describe el comportamiento esperado del sistema desde la perspectiva de los actores.

Los casos de uso complementan las historias de usuario y permiten describir:

- Actores.
- Precondiciones.
- Flujo principal.
- Flujos alternativos.
- Postcondiciones.

---

# CU-01 - Registrar usuario

## Actor principal

Comprador u organizador.

## Objetivo

Crear una cuenta en el sistema.

## Precondiciones

- El usuario no debe estar registrado con el mismo correo.

## Flujo principal

1. El usuario proporciona su nombre.
2. El usuario proporciona su correo.
3. El usuario proporciona su contraseña.
4. El sistema valida los datos.
5. El sistema verifica que el correo no exista.
6. El sistema almacena el usuario.
7. El sistema informa que el registro fue exitoso.

## Flujos alternativos

### FA-01 - Correo existente

1. El sistema detecta que el correo ya está registrado.
2. El sistema rechaza la operación.
3. El sistema informa el motivo.

### FA-02 - Datos inválidos

1. El sistema detecta información inválida.
2. El sistema rechaza la operación.
3. El sistema informa los datos que deben corregirse.

## Postcondición

El usuario queda registrado.

---

# CU-02 - Iniciar sesión

## Actor principal

Usuario.

## Objetivo

Autenticar al usuario.

## Precondiciones

- El usuario debe estar registrado.

## Flujo principal

1. El usuario proporciona correo y contraseña.
2. El sistema valida las credenciales.
3. El sistema genera un JWT.
4. El sistema retorna el token.

## Flujos alternativos

### FA-01 - Credenciales inválidas

1. El sistema detecta credenciales incorrectas.
2. El sistema rechaza la autenticación.

## Postcondición

El usuario obtiene una sesión autenticada mediante JWT.

---

# CU-03 - Crear evento

## Actor principal

Organizador.

## Objetivo

Registrar un nuevo evento.

## Precondiciones

- El usuario debe estar autenticado.
- El usuario debe tener rol de organizador.

## Flujo principal

1. El organizador proporciona los datos del evento.
2. El sistema valida los datos.
3. El sistema registra el evento.
4. El sistema registra sus localidades.
5. El sistema registra precios y capacidades.
6. El sistema confirma la creación.

## Postcondición

El evento queda registrado en Catalog.

---

# CU-04 - Consultar eventos

## Actor principal

Comprador.

## Objetivo

Obtener eventos disponibles.

## Precondiciones

- No requiere autenticación para consulta pública.

## Flujo principal

1. El comprador solicita los eventos.
2. Catalog procesa la consulta.
3. El sistema obtiene los eventos.
4. El sistema retorna los resultados.

---

# CU-05 - Consultar disponibilidad

## Actor principal

Comprador.

## Objetivo

Conocer las entradas disponibles.

## Precondiciones

- El evento debe existir.

## Flujo principal

1. El comprador solicita la disponibilidad.
2. Inventory identifica el evento.
3. Inventory consulta la disponibilidad actual.
4. El sistema retorna la disponibilidad por localidad.

## Postcondición

El comprador obtiene la disponibilidad actual.

---

# CU-06 - Crear reserva

## Actor principal

Comprador.

## Objetivo

Reservar una cantidad determinada de entradas.

## Precondiciones

- El comprador debe estar autenticado.
- El evento debe existir.
- La localidad debe existir.
- La cantidad solicitada debe ser válida.

## Flujo principal

1. El comprador solicita una cantidad de entradas.
2. Inventory recibe la solicitud.
3. Inventory ejecuta una operación atómica sobre la disponibilidad.
4. El sistema determina si existe disponibilidad.
5. Si existe disponibilidad, la operación es aceptada.
6. El sistema registra la reserva.
7. El sistema retorna la reserva.

## Flujos alternativos

### FA-01 - Disponibilidad insuficiente

1. Inventory detecta que la cantidad solicitada no está disponible.
2. La operación es rechazada.
3. La disponibilidad no debe quedar afectada por una reserva no válida.
4. El sistema informa que no existe disponibilidad suficiente.

### FA-02 - Solicitudes concurrentes

1. Dos o más usuarios realizan reservas simultáneamente.
2. Inventory procesa las operaciones de manera atómica.
3. Solo las solicitudes con disponibilidad suficiente son aceptadas.
4. El inventario no debe permitir sobreventa.

### FA-03 - Error durante persistencia

1. La operación de inventario puede completarse antes de persistir la reserva.
2. El sistema debe aplicar el mecanismo de recuperación definido por la arquitectura.
3. El incidente debe quedar registrado para su análisis.

## Postcondición

Si la reserva es exitosa:

- Existe un registro de reserva.
- La disponibilidad disminuye.
- La cantidad reservada no supera la disponibilidad existente.

---

# CU-07 - Cancelar reserva

## Actor principal

Comprador.

## Objetivo

Cancelar una reserva y liberar sus entradas.

## Precondiciones

- La reserva debe existir.
- La reserva debe pertenecer al comprador.
- La reserva debe estar en un estado que permita cancelación.

## Flujo principal

1. El comprador solicita cancelar la reserva.
2. El sistema valida la propiedad de la reserva.
3. El sistema cambia el estado de la reserva.
4. El sistema libera las entradas.
5. El sistema confirma la cancelación.

---

# CU-08 - Ejecutar prueba de carga

## Actor principal

Investigador.

## Objetivo

Evaluar el comportamiento del sistema.

## Precondiciones

- El sistema debe estar desplegado.
- Deben existir datos de prueba.
- El escenario de carga debe estar definido.

## Flujo principal

1. El investigador configura el número de usuarios.
2. Configura la duración.
3. Configura el patrón de tráfico.
4. Ejecuta la prueba.
5. El generador envía solicitudes.
6. El sistema procesa las solicitudes.
7. Se recopilan métricas.
8. Se almacenan los resultados.

## Postcondición

Se dispone de resultados de rendimiento.

---

# CU-09 - Detectar anomalías

## Actor principal

Componente de Inteligencia Artificial.

## Objetivo

Identificar comportamientos anormales en las métricas.

## Precondiciones

- Deben existir métricas operativas.

## Flujo principal

1. El sistema obtiene métricas.
2. La IA analiza los datos.
3. Identifica patrones relevantes.
4. Determina si existe una anomalía.
5. Clasifica el comportamiento.
6. Genera una recomendación cuando corresponda.

---

# CU-10 - Analizar resultados

## Actor principal

Investigador.

## Objetivo

Comparar el comportamiento del sistema bajo diferentes escenarios.

## Flujo principal

1. El investigador ejecuta escenarios.
2. Obtiene las métricas.
3. Compara resultados.
4. Identifica diferencias.
5. Analiza los atributos de calidad.
6. Documenta las conclusiones.

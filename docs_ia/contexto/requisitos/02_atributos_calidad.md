# Atributos de Calidad - Microservicio de Inventario (Inventory)

## 1. Rendimiento y Latencia (Performance)

- **RNF-01 - Tiempo de respuesta en lecturas:**
  - La consulta de disponibilidad de stock (`/api/v1/inventory/availability`) debe responder en **menos de 100 ms** bajo condiciones normales de tráfico.
- **RNF-02 - Tiempo de respuesta en escrituras/reservas:**
  - La operación de reserva atómica de entradas debe procesarse en **menos de 250 ms** en el percentil 95 (P95).
- **RNF-03 - Rendimiento sostenido (Throughput):**
  - El servicio debe mantener una capacidad mínima de procesamiento de transacciones por segundo (TPS) sin degrada la latencia base durante las operaciones de reserva.

---

## 2. Concurrencia y Consistencia de Datos (Consistency)

- **RNF-04 - Consistencia Estricta de Inventario:**
  - El control de inventario requiere **consistencia inmediata (Strong Consistency)**. Bajo ningún concepto se permite consistencia eventual en el cálculo del stock disponible.
- **RNF-05 - Control de Concurrencia y Prevención de Sobreventa:**
  - Ante solicitudes simultáneas sobre la misma localidad/entrada, el servicio debe garantizar que el stock disponible nunca sea inferior a cero (`Stock >= 0`).
  - La estrategia de bloqueo (bloqueo pesimista `SELECT FOR UPDATE` o bloqueo optimista con campos de versión `@Version`) debe garantizar atomicidad a nivel de base de datos.

---

## 3. Escalabilidad y Disponibilidad (Scalability & Availability)

- **RNF-06 - Manejo de Picos de Trafico (Spike Traffic):**
  - El microservicio debe estar preparado para absorber incrementos repentinos de tráfico (picos de alta demanda al inicio de venta de eventos populares) sin colapsar la capa de persistencia.
- **RNF-07 - Aislamiento de Fallas (Fault Tolerance):**
  - Si la operación de registro de reserva falla por tiempo de espera (timeout) o error de red, el sistema debe revertir (rollback) de forma inmediata cualquier descuento realizado en el inventario.

---

## 4. Seguridad (Security)

- **RNF-08 - Autenticación y Autorización:**
  - Todas las operaciones de modificación de inventario (creación de localidades, configuración de capacidad, reserva y cancelación) deben exigir autenticación mediante **JWT**.
  - Las operaciones de configuración de capacidad y localidades están restringidas exclusivamente al rol `ORGANIZER` o `ADMIN`.
  - La consulta pública de disponibilidad permite acceso anónimo o autenticado (`BUYER`).

---

## 5. Mantenibilidad y Estándares de Código (Maintainability)

- **RNF-09 - Nomenclatura e Idioma:**
  - Todo el código fuente, clases, métodos, entidades, tablas de base de datos, DTOs y comentarios internos del proyecto DEBEN escribirse estrictamente en **inglés** (cumpliendo la Regla 3 del proyecto).
- **RNF-10 - Cobertura de Pruebas Unitarias e Integración:**
  - Los componentes críticos de reserva y control de concurrencia deben contar con pruebas automatizadas que simulen múltiples hilos concurrentes tratando de reservar el mismo stock disponible.
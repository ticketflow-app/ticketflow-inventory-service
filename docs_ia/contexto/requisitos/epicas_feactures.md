# Épicas y Features

## 1. Propósito

Este documento define la estructura funcional del sistema mediante épicas y features.

Las épicas representan grandes capacidades funcionales del sistema.

Las features representan funcionalidades específicas que forman parte de cada épica.

Este documento sirve como puente entre el contexto de negocio y las historias de usuario.

---

# 2. Épicas

## E1. Gestión de usuarios y sesiones

Permite registrar usuarios, autenticarlos y gestionar su información básica y permisos.

### Features

- F1.1 Registro de usuario
- F1.2 Inicio de sesión
- F1.3 Consulta de perfil
- F1.4 Gestión de roles
- F1.5 Gestión de sesión

---

## E2. Gestión de eventos

Permite a los organizadores crear y administrar eventos.

### Features

- F2.1 Crear evento
- F2.2 Modificar evento
- F2.3 Consultar eventos
- F2.4 Buscar eventos
- F2.5 Filtrar eventos
- F2.6 Consultar detalle del evento
- F2.7 Gestionar estado del evento

---

## E3. Gestión de localidades e inventario

Permite configurar las localidades y controlar la disponibilidad de entradas.

### Features

- F3.1 Crear localidad
- F3.2 Definir precio
- F3.3 Definir capacidad
- F3.4 Consultar disponibilidad
- F3.5 Consultar disponibilidad por localidad
- F3.6 Actualizar disponibilidad

---

## E4. Gestión de reservas

Permite a los compradores realizar y consultar reservas.

### Features

- F4.1 Seleccionar localidad
- F4.2 Seleccionar cantidad
- F4.3 Validar disponibilidad
- F4.4 Crear reserva
- F4.5 Registrar reserva
- F4.6 Controlar concurrencia
- F4.7 Consultar reserva
- F4.8 Consultar historial de reservas
- F4.9 Cancelar reserva
- F4.10 Liberar entradas

---

## E5. Control de concurrencia y consistencia

Permite garantizar que las operaciones críticas mantengan la integridad del inventario.

### Features

- F5.1 Operación atómica de reserva
- F5.2 Validación de disponibilidad
- F5.3 Prevención de sobreventa
- F5.4 Control de operaciones concurrentes
- F5.5 Recuperación ante errores
- F5.6 Consistencia del inventario

---

## E6. Evaluación de alta demanda

Permite evaluar el comportamiento del sistema bajo diferentes niveles de carga.

### Features

- F6.1 Simular usuarios concurrentes
- F6.2 Simular carga sostenida
- F6.3 Simular incremento repentino
- F6.4 Simular pico de tráfico
- F6.5 Medir latencia
- F6.6 Medir throughput
- F6.7 Medir errores
- F6.8 Medir utilización de recursos
- F6.9 Evaluar comportamiento de concurrencia

---

## E7. Observabilidad y análisis mediante IA

Permite recopilar métricas y utilizar técnicas de Inteligencia Artificial para identificar patrones operativos.

### Features

- F7.1 Recopilar métricas
- F7.2 Registrar eventos operativos
- F7.3 Detectar anomalías
- F7.4 Identificar patrones de sobrecarga
- F7.5 Generar recomendaciones
- F7.6 Analizar comportamiento del sistema

---

# 3. Priorización

## Alta prioridad - MVP

Las siguientes funcionalidades son necesarias para construir el prototipo mínimo:

- F1.1 Registro de usuario
- F1.2 Inicio de sesión
- F1.3 Consulta de perfil
- F2.1 Crear evento
- F2.3 Consultar eventos
- F2.4 Buscar eventos
- F2.6 Consultar detalle
- F3.1 Crear localidad
- F3.2 Definir precio
- F3.3 Definir capacidad
- F3.4 Consultar disponibilidad
- F4.1 Seleccionar localidad
- F4.2 Seleccionar cantidad
- F4.3 Validar disponibilidad
- F4.4 Crear reserva
- F5.1 Operación atómica de reserva
- F5.3 Prevención de sobreventa
- F5.4 Control de concurrencia
- F6.1 Simular usuarios concurrentes
- F6.3 Simular incrementos repentinos
- F6.5 Medir latencia
- F6.6 Medir throughput
- F6.7 Medir errores

---

# 4. Prioridad media

- Modificar eventos.
- Gestionar estados.
- Consultar detalle de reservas.
- Cancelar reservas.
- Liberar entradas.
- Consultar reservas del evento.
- Observabilidad avanzada.
- Detección de anomalías.
- Recomendaciones mediante IA.

---

# 5. Fuera del MVP

- Pagos reales.
- Facturación.
- Notificaciones.
- Aplicación móvil.
- Integraciones externas.
- Elasticsearch.
- Kafka.
- Kubernetes.
- Recomendaciones comerciales.
- Sistema completo de check-in.

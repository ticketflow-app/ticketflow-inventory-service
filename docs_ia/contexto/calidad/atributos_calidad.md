# Atributos de Calidad

## 1. Propósito

Este documento define los atributos de calidad prioritarios para el proyecto.

Estos atributos orientarán las decisiones arquitectónicas mediante ADD y serán evaluados mediante pruebas.

---

# 2. Escalabilidad

## Definición

Capacidad del sistema para soportar incrementos en la cantidad de usuarios y solicitudes sin degradarse de manera inaceptable.

## Importancia

Es uno de los atributos principales debido al escenario de alta demanda.

## Indicadores

- Throughput.
- Uso de CPU.
- Uso de memoria.
- Tiempo de respuesta.
- Número de solicitudes concurrentes.
- Tasa de errores.

---

# 3. Disponibilidad

## Definición

Capacidad del sistema para continuar respondiendo solicitudes durante periodos de carga.

## Indicadores

- Porcentaje de solicitudes exitosas.
- Tiempo fuera de servicio.
- Tasa de errores.
- Capacidad de recuperación.

---

# 4. Latencia

## Definición

Tiempo transcurrido desde que el sistema recibe una solicitud hasta que entrega una respuesta.

## Indicadores

- Promedio.
- Mediana.
- P95.
- P99.
- Máximo.

Los percentiles serán importantes porque el promedio puede ocultar solicitudes extremadamente lentas.

---

# 5. Rendimiento

## Definición

Capacidad del sistema para procesar solicitudes eficientemente.

## Indicadores

- Requests por segundo.
- Tiempo de respuesta.
- Tasa de errores.
- Consumo de CPU.
- Consumo de memoria.

---

# 6. Concurrencia

## Definición

Capacidad del sistema para procesar múltiples solicitudes simultáneamente sin producir resultados incorrectos.

## Principal preocupación

La reserva concurrente de entradas.

El sistema debe impedir que varias solicitudes consuman las mismas entradas.

---

# 7. Consistencia

## Definición

Capacidad del sistema para mantener el inventario y las reservas coherentes.

## Regla principal

La cantidad reservada no puede superar la capacidad disponible.

---

# 8. Observabilidad

## Definición

Capacidad para conocer el estado interno del sistema mediante métricas y registros.

## Métricas

- CPU.
- Memoria.
- Latencia.
- Throughput.
- Errores.
- Número de solicitudes.
- Disponibilidad.
- Estado del inventario.

---

# 9. Mantenibilidad

Aunque no es el principal foco experimental, la arquitectura debe facilitar modificaciones.

La arquitectura hexagonal permite modificar adaptadores sin modificar directamente la lógica de negocio.

---

# 10. Priorización

Prioridad alta:

- Escalabilidad.
- Concurrencia.
- Consistencia.
- Rendimiento.
- Latencia.

Prioridad media:

- Disponibilidad.
- Observabilidad.

Prioridad complementaria:

- Mantenibilidad.

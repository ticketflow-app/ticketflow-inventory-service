# Escenarios de Calidad

## 1. Propósito

Este documento define escenarios concretos para evaluar los atributos de calidad.

---

# 2. Escenario de alta concurrencia

## Atributo

Concurrencia.

## Estímulo

Múltiples usuarios intentan reservar simultáneamente entradas de la misma localidad.

## Entorno

Sistema desplegado en contenedores.

## Respuesta esperada

Las operaciones deben procesarse sin sobreventa.

## Medición

- Reservas exitosas.
- Reservas rechazadas.
- Disponibilidad final.
- Errores.

---

# 3. Escenario de disponibilidad limitada

## Atributo

Consistencia.

## Estímulo

Existe una cantidad pequeña de entradas disponibles y múltiples usuarios realizan reservas.

## Respuesta esperada

El sistema debe permitir reservas únicamente hasta agotar la disponibilidad.

## Medición

La suma de entradas reservadas no debe superar la capacidad inicial.

---

# 4. Escenario de carga sostenida

## Atributo

Rendimiento.

## Estímulo

Número constante de solicitudes durante un periodo determinado.

## Respuesta esperada

El sistema debe mantener un comportamiento estable.

## Medición

- Latencia.
- Throughput.
- Errores.
- CPU.
- Memoria.

---

# 5. Escenario de incremento repentino

## Atributo

Escalabilidad.

## Estímulo

Incremento rápido del número de usuarios.

## Respuesta esperada

El sistema debe continuar respondiendo dentro de los límites definidos.

## Medición

- P95.
- P99.
- Throughput.
- Errores.
- Recursos.

---

# 6. Escenario de pico de tráfico

## Atributo

Disponibilidad y escalabilidad.

## Estímulo

Una gran cantidad de solicitudes durante un intervalo corto.

## Respuesta esperada

El sistema debe continuar procesando solicitudes y recuperarse después del pico.

## Medición

- Tasa de errores.
- Latencia.
- Throughput.
- Tiempo de recuperación.

---

# 7. Escenario de recuperación

## Atributo

Disponibilidad.

## Estímulo

Reducción de la disponibilidad de recursos o finalización de un periodo de saturación.

## Respuesta esperada

El sistema debe retornar progresivamente a condiciones normales.

## Medición

- Tiempo de recuperación.
- Latencia posterior.
- Tasa de errores.

---

# 8. Escenario de consulta de disponibilidad

## Atributo

Rendimiento.

## Estímulo

Muchos usuarios consultan simultáneamente la disponibilidad.

## Respuesta esperada

Inventory debe responder de manera eficiente.

## Medición

- Latencia.
- Requests por segundo.
- Errores.

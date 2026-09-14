# Pruebas de Carga y Concurrencia

## 1. Propósito

Este documento define los escenarios utilizados para evaluar el comportamiento del sistema ante diferentes niveles de demanda.

El objetivo principal es analizar:

- Escalabilidad.
- Rendimiento.
- Latencia.
- Concurrencia.
- Consistencia.
- Disponibilidad.

---

# 2. Herramienta de generación de carga

Se utilizará una herramienta de generación de carga que permita simular múltiples usuarios concurrentes.

La herramienta debe permitir:

- Configurar usuarios virtuales.
- Definir solicitudes.
- Configurar duración.
- Configurar incrementos de carga.
- Registrar latencias.
- Registrar errores.
- Registrar throughput.

La herramienta específica podrá seleccionarse durante la implementación.

---

# 3. Escenario 1 - Carga baja

## Objetivo

Establecer una línea base.

## Características

Pocos usuarios concurrentes.

## Operaciones

- Consulta de eventos.
- Consulta de disponibilidad.
- Creación de reservas.

## Métricas

- Latencia.
- Throughput.
- Errores.

---

# 4. Escenario 2 - Carga media

## Objetivo

Evaluar el comportamiento con un nivel mayor de concurrencia.

## Características

Incremento controlado de usuarios.

## Métricas

- P50.
- P95.
- P99.
- Throughput.
- Errores.
- CPU.
- Memoria.

---

# 5. Escenario 3 - Carga alta

## Objetivo

Determinar el comportamiento del sistema cuando aumenta considerablemente el número de solicitudes.

## Características

Mayor número de usuarios concurrentes.

## Métricas

- Latencia.
- Throughput.
- Tasa de errores.
- CPU.
- Memoria.
- Disponibilidad.

---

# 6. Escenario 4 - Pico repentino

## Objetivo

Simular el inicio de venta de un evento altamente demandado.

## Comportamiento

El sistema comienza con carga normal.

Posteriormente, el número de solicitudes aumenta rápidamente.

## Métricas

- Latencia antes del pico.
- Latencia durante el pico.
- Latencia después del pico.
- Throughput.
- Errores.
- Recursos.

---

# 7. Escenario 5 - Concurrencia de reservas

## Objetivo

Evaluar el control de concurrencia.

## Preparación

Configurar una localidad con una cantidad limitada de entradas.

Ejemplo:

```text
Disponibilidad inicial: 100
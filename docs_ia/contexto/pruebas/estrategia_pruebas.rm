# Estrategia de Pruebas

## 1. Propósito

La estrategia de pruebas tiene como objetivo verificar que el sistema cumple los requisitos funcionales y los atributos de calidad definidos.

Se utilizarán diferentes niveles de prueba.

---

# 2. Pruebas unitarias

Validarán componentes individuales.

Se probarán principalmente:

- Entidades.
- Reglas de negocio.
- Casos de uso.
- Validaciones.
- Manejo de errores.

Las pruebas unitarias no deben depender de servicios externos reales.

---

# 3. Pruebas de integración

Validarán la interacción entre componentes.

Se probarán:

- PostgreSQL.
- Redis.
- Repositorios.
- APIs.
- Comunicación entre componentes.

---

# 4. Pruebas de API

Validarán los endpoints REST.

Se comprobarán:

- Códigos HTTP.
- Estructuras de respuesta.
- Validaciones.
- Autenticación.
- Autorización.
- Errores.

---

# 5. Pruebas funcionales

Validarán las historias de usuario.

Cada historia de usuario priorizada debe tener casos de prueba asociados.

---

# 6. Pruebas de concurrencia

Se utilizarán para verificar la operación crítica de reserva.

El objetivo principal es comprobar que:

- No existe sobreventa.
- No existen reservas duplicadas incorrectamente.
- La disponibilidad final es correcta.
- El sistema soporta múltiples solicitudes simultáneas.

---

# 7. Pruebas de carga

Permitirán evaluar el comportamiento bajo diferentes niveles de solicitudes.

Se considerarán:

- Carga normal.
- Carga sostenida.
- Incremento progresivo.
- Incremento repentino.
- Pico de tráfico.

---

# 8. Métricas

Las pruebas recopilarán:

- Latencia promedio.
- P50.
- P95.
- P99.
- Throughput.
- Tasa de errores.
- CPU.
- Memoria.
- Tiempo de recuperación.

---

# 9. Criterio de éxito

Los criterios definitivos serán definidos antes de ejecutar las pruebas finales.

No se deben inventar umbrales de rendimiento sin justificarlos experimentalmente o académicamente.

---

# 10. Repetibilidad

Cada escenario importante debe poder ejecutarse más de una vez bajo condiciones similares.

Se deben documentar:

- Configuración.
- Número de usuarios.
- Duración.
- Tipo de tráfico.
- Datos utilizados.
- Recursos disponibles.
- Resultados.

---

# 11. Comparación

Los resultados deben permitir comparar el comportamiento del sistema ante diferentes niveles de carga.

Por ejemplo:

```text
Carga baja
Carga media
Carga alta
Pico
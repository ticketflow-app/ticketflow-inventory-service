# Contexto de Negocio

## 1. Información general del proyecto

### Nombre del proyecto

Metodología de Ingeniería de Software con Inteligencia Artificial para Aplicaciones de Gran Escala

### Tipo de proyecto

Proyecto académico de desarrollo de software y evaluación de una metodología de ingeniería de software asistida por Inteligencia Artificial.

### Equipo

El proyecto será desarrollado por un equipo de tres estudiantes.

### Duración

El proyecto se desarrolla durante el semestre académico 2026-2.

El prototipo funcional tiene una restricción aproximada de seis semanas para su implementación principal.

---

# 2. Contexto del problema

Las aplicaciones que manejan eventos de alta demanda pueden experimentar incrementos repentinos y significativos en el número de usuarios cuando se habilita la venta o reserva de entradas para eventos populares.

Un ejemplo representativo ocurre cuando comienza la venta de entradas para un evento con alta demanda. En ese momento, una gran cantidad de usuarios puede intentar simultáneamente:

- Consultar los eventos disponibles.
- Consultar información de un evento.
- Consultar las localidades disponibles.
- Consultar la cantidad de entradas restantes.
- Seleccionar una localidad.
- Seleccionar una cantidad de entradas.
- Crear una reserva.

Este comportamiento genera una alta concentración de solicitudes en periodos muy cortos.

Una implementación que no considere adecuadamente la concurrencia puede producir problemas como:

- Sobreventa de entradas.
- Reservas duplicadas.
- Cantidades de disponibilidad incorrectas.
- Condiciones de carrera.
- Tiempos de respuesta elevados.
- Errores bajo carga.
- Saturación de recursos.
- Caídas del sistema.
- Dificultad para recuperar el sistema después de un pico de tráfico.

Por esta razón, el proyecto busca utilizar una metodología de ingeniería de software que considere desde las primeras etapas los atributos de calidad asociados con aplicaciones de gran escala.

---

# 3. Problema principal

El problema central consiste en diseñar y construir un prototipo de aplicación para reserva de entradas que permita estudiar cómo una arquitectura y una metodología de ingeniería de software pueden responder ante escenarios de alta concurrencia y picos repentinos de demanda.

El proyecto no busca construir una plataforma comercial de venta de entradas.

El objetivo es construir un prototipo controlado que permita experimentar y evaluar decisiones de arquitectura relacionadas con:

- Escalabilidad.
- Disponibilidad.
- Rendimiento.
- Baja latencia.
- Concurrencia.
- Consistencia.
- Observabilidad.
- Manejo de picos de tráfico.

---

# 4. Objetivo general

Diseñar y evaluar una metodología de ingeniería de software asistida por Inteligencia Artificial para el desarrollo de aplicaciones de gran escala, utilizando como caso de estudio un sistema de reserva de entradas para eventos con alta demanda.

---

# 5. Objetivos específicos

1. Identificar los requisitos funcionales y atributos de calidad relevantes para una aplicación de reserva de entradas sometida a alta concurrencia.

2. Utilizar técnicas de diseño de software como Attribute-Driven Design (ADD) para orientar las decisiones arquitectónicas a partir de los atributos de calidad.

3. Aplicar Spec-Driven Development (SDD) para definir de manera estructurada los requisitos, contratos y comportamiento esperado del sistema.

4. Diseñar una arquitectura basada en microservicios y arquitectura hexagonal.

5. Implementar un prototipo funcional compuesto inicialmente por los microservicios de usuarios, catálogo e inventario.

6. Implementar mecanismos que permitan controlar la concurrencia durante la reserva de entradas.

7. Diseñar escenarios de carga que permitan simular diferentes niveles de demanda.

8. Evaluar el comportamiento del sistema bajo carga sostenida y aumentos repentinos de tráfico.

9. Medir métricas como latencia, disponibilidad, tasa de errores, capacidad de procesamiento y utilización de recursos.

---

# 6. Caso de estudio

El caso de estudio consiste en una aplicación para la consulta y reserva de entradas para eventos.

Los usuarios podrán consultar eventos disponibles, revisar sus características, consultar las localidades y cantidades disponibles y realizar reservas.

Los organizadores podrán crear y administrar eventos.

---

# 7. Alcance funcional

El prototipo contempla inicialmente tres microservicios principales:

1. Users.
2. Catalog.
3. Inventory.

## 7.1 Users

El microservicio Users será responsable de la gestión de usuarios y autenticación.

Permitirá:

- Registrar usuarios.
- Iniciar sesión.
- Consultar el perfil del usuario autenticado.
- Gestionar roles.

Los roles considerados son:

- comprador.
- organizador.

---

## 7.2 Catalogo

El microservicio Catalog será responsable de la información estática relacionada con los eventos.

Permitirá:

- Crear eventos.
- Consultar eventos.
- Buscar eventos.
- Filtrar eventos.
- Consultar el detalle de un evento.
- Modificar eventos.
- Definir localidades.
- Definir precios.
- Definir la capacidad total de cada localidad.

Catalogo almacenará información como:

- Nombre del evento.
- Descripción.
- Fecha.
- Lugar.
- Categoría.
- Organizador.
- Localidades.
- Precio.
- Capacidad total.

Catalog no será responsable de determinar la disponibilidad actual de entradas.

---

## 7.3 Inventory

El microservicio Inventory será responsable de controlar la disponibilidad de entradas y las reservas.

Permitirá:

- Consultar disponibilidad.
- Crear reservas.
- Consultar las reservas de un usuario.
- Consultar el estado de una reserva.
- Cancelar reservas cuando esta funcionalidad sea implementada.
- Liberar disponibilidad cuando corresponda.

Inventory será el responsable de aplicar las reglas críticas relacionadas con la concurrencia.

---

# 8. Actores del sistema

## 8.1 Comprador

Usuario que desea consultar eventos y realizar reservas.

Puede:

- Registrarse.
- Iniciar sesión.
- Consultar eventos.
- Consultar detalles.
- Consultar disponibilidad.
- Seleccionar una localidad.
- Seleccionar cantidad de entradas.
- Crear una reserva.
- Consultar sus reservas.

---

## 8.2 Organizador

Usuario responsable de administrar eventos.

Puede:

- Crear eventos.
- Modificar eventos.
- Definir localidades.
- Definir precios.
- Definir capacidades.

---

## 8.3 Administrador

Rol utilizado para operaciones administrativas del sistema.

Puede realizar operaciones relacionadas con la gestión de usuarios y roles.

---

## 8.4 Sistema de pruebas de carga

Componente externo utilizado para simular múltiples usuarios concurrentes.

Su propósito es generar solicitudes controladas para evaluar:

- Latencia.
- Throughput.
- Tasa de errores.
- Capacidad.
- Comportamiento ante concurrencia.
- Comportamiento ante picos de tráfico.

---

# 9. Flujo general del negocio

El flujo principal del sistema es el siguiente:

Un organizador crea un evento y configura sus localidades, precios y capacidades.

Los compradores pueden consultar los eventos disponibles y seleccionar uno.

Al consultar el detalle del evento, el usuario puede conocer sus características y las localidades disponibles.

Cuando el usuario desea reservar entradas, el sistema consulta la disponibilidad actual.

La operación de reserva debe verificar y modificar la disponibilidad de manera atómica para evitar que dos o más usuarios obtengan las mismas entradas.

Si existe disponibilidad suficiente, la reserva es aceptada y registrada.

Si no existe disponibilidad suficiente, la reserva es rechazada.

La información de disponibilidad debe reflejar el estado actual del inventario.

---

# 10. Regla fundamental del negocio

El sistema nunca debe permitir una cantidad de reservas confirmadas superior a la capacidad disponible de una localidad.

Por ejemplo:

Si una localidad tiene una capacidad de 100 entradas y existen 98 entradas disponibles, dos usuarios pueden realizar reservas simultáneas.

Si un usuario intenta reservar 2 entradas y otro usuario intenta reservar 3 entradas al mismo tiempo, el sistema debe garantizar que las operaciones se procesen correctamente y que nunca se asignen más de las 100 entradas disponibles.

La lógica de concurrencia debe garantizar que no se produzca sobreventa.

---

# 11. Separación entre catálogo e inventario

Una decisión fundamental del negocio es separar la información estática del evento de la disponibilidad dinámica.

Catalog contiene:

- Nombre.
- Descripción.
- Fecha.
- Lugar.
- Categoría.
- Localidades.
- Precio.
- Capacidad total.

Inventory contiene:

- Disponibilidad actual.
- Reservas.
- Estado de las reservas.

La capacidad total representa una configuración del evento.

La disponibilidad representa el estado dinámico del inventario.

Por lo tanto:


La capacidad total cambia principalmente durante la configuración del evento.

La disponibilidad cambia constantemente a medida que los usuarios realizan reservas o se liberan entradas.

---

# 12. Reglas de negocio

## RB-01. Identificación de usuarios

Cada usuario debe tener un identificador único.

## RB-02. Correo único

No deben existir dos usuarios con el mismo correo electrónico.

## RB-03. Contraseñas

Las contraseñas no deben almacenarse en texto plano.

## RB-04. Autenticación

Las operaciones protegidas requieren autenticación.

## RB-05. Roles

Las operaciones administrativas deben depender del rol del usuario.

## RB-06. Organización de eventos

Un organizador puede crear y modificar eventos de acuerdo con los permisos definidos.

## RB-07. Capacidad

Cada localidad debe tener una capacidad total definida.

## RB-08. Disponibilidad

La disponibilidad nunca debe ser negativa al finalizar una operación exitosa.

## RB-09. Reserva

Una reserva solamente puede ser creada cuando existe disponibilidad suficiente.

## RB-10. Concurrencia

Dos operaciones concurrentes no pueden consumir las mismas entradas.

## RB-11. No sobreventa

El total de entradas reservadas no puede superar la capacidad disponible.

## RB-12. Inventario como fuente de verdad

Inventory es responsable de determinar la disponibilidad actual.

## RB-13. Persistencia

Las reservas deben mantenerse en una base de datos para proporcionar persistencia y trazabilidad.

## RB-14. Disponibilidad rápida

La consulta de disponibilidad debe estar optimizada para soportar una gran cantidad de consultas concurrentes.

## RB-15. Independencia de servicios

Cada microservicio debe ser responsable de su propio dominio y datos.

---

# 13. Escenario crítico de negocio

El escenario crítico del proyecto ocurre cuando inicia la venta o reserva de entradas de un evento altamente demandado.

En este momento puede ocurrir que cientos o miles de solicitudes intenten realizar operaciones similares en un periodo corto.

El sistema debe mantener la integridad del inventario.

Ejemplo:

Una localidad tiene una sola entrada disponible.

Dos compradores realizan una solicitud de reserva prácticamente al mismo tiempo.

El sistema debe garantizar que:

- Solo una reserva sea aceptada.
- La otra reserva sea rechazada.
- La disponibilidad final sea cero.
- No exista sobreventa.
- El estado persistido de las reservas sea coherente con el resultado de la operación.

Este escenario será uno de los principales casos utilizados para evaluar la arquitectura.

---

# 14. Atributos de calidad prioritarios

El proyecto considera los siguientes atributos de calidad.

## 14.1 Escalabilidad

El sistema debe poder soportar incrementos en el número de solicitudes.

## 14.2 Disponibilidad

El sistema debe continuar respondiendo durante incrementos de demanda dentro de los límites definidos para el experimento.

## 14.3 Baja latencia

Las operaciones críticas deben responder en tiempos adecuados incluso bajo carga.

## 14.4 Rendimiento

El sistema debe procesar una cantidad significativa de solicitudes por unidad de tiempo.

## 14.5 Concurrencia

El sistema debe manejar correctamente múltiples operaciones simultáneas.

## 14.6 Consistencia

El inventario debe mantener información coherente y evitar la sobreventa.

## 14.7 Observabilidad

El sistema debe proporcionar métricas suficientes para analizar su comportamiento.

---

# 15. Arquitectura general

El sistema utilizará una arquitectura basada en microservicios combinada con arquitectura hexagonal.

Los microservicios iniciales serán:

- Users.
- Catalog.
- Inventory.

Cada microservicio tendrá su propio dominio y será responsable de sus operaciones.

La arquitectura hexagonal se utilizará para separar:

- Lógica de negocio.
- Casos de uso.
- Interfaces de entrada.
- Interfaces de salida.
- Tecnologías externas.

Las tecnologías concretas utilizadas por el prototipo podrán cambiar durante el proceso de desarrollo sin modificar directamente las reglas centrales del negocio.

Los detalles técnicos completos de la arquitectura se encuentran en:

`docs/arquitectura/arquitectura.md`

---

# 16. Tecnologías conceptuales

El proyecto contempla inicialmente:

- Microservicios.
- Arquitectura hexagonal.
- API REST.
- PostgreSQL.
- Redis.
- JWT.
- Contenedores.
- Herramientas de pruebas de carga.
- Herramientas de monitoreo.
- Inteligencia Artificial.

La elección definitiva de tecnologías debe justificarse de acuerdo con los requisitos y atributos de calidad.

---

# 17. Restricciones del proyecto

El proyecto tiene las siguientes restricciones:

1. El equipo está compuesto por tres personas.

2. El prototipo tiene aproximadamente seis semanas para su implementación principal.

3. El proyecto tiene carácter académico.

4. El sistema debe ser suficientemente pequeño para poder ser implementado y evaluado dentro del tiempo disponible.

5. No se busca construir una plataforma comercial completa.

6. No se implementarán integraciones financieras reales.

7. Las pruebas de alta demanda se realizarán mediante escenarios controlados.

8. La Inteligencia Artificial será utilizada como apoyo al proceso de ingeniería de software y análisis del comportamiento del sistema.

---

# 18. Funcionalidades fuera del alcance

Las siguientes funcionalidades no forman parte del alcance principal del prototipo:

- Pagos reales.
- Integración con bancos.
- Facturación.
- Procesamiento de tarjetas.
- Aplicación móvil.
- Sistema real de emisión de entradas.
- Códigos QR reales.
- Notificaciones por SMS.
- Notificaciones por correo.
- Integración con plataformas externas de eventos.
- Recomendaciones personalizadas.
- Elasticsearch.
- Kafka.
- Kubernetes.
- Arquitecturas excesivamente complejas que no sean necesarias para el experimento.

Estas funcionalidades podrían considerarse como trabajo futuro.

---

# 19. Propósito de la Inteligencia Artificial

La Inteligencia Artificial no constituye únicamente una funcionalidad del sistema.

Su propósito principal dentro del proyecto es apoyar el proceso de ingeniería de software y el análisis operacional.

La IA podrá utilizarse para:

- Apoyar el análisis de requisitos.
- Apoyar el diseño arquitectónico.
- Analizar atributos de calidad.
- Identificar posibles riesgos.
- Analizar métricas de operación.
- Detectar patrones de sobrecarga.
- Identificar anomalías.
- Generar recomendaciones de optimización.
- Apoyar el análisis de resultados de pruebas.

La IA no debe inventar requisitos o funcionalidades que no hayan sido definidos en los documentos del proyecto.

---

# 20. Metodología del proyecto

La metodología combina diferentes prácticas de ingeniería de software.

## Attribute-Driven Design (ADD)

ADD se utilizará para diseñar la arquitectura a partir de los atributos de calidad prioritarios.

Los principales atributos considerados son:

- Escalabilidad.
- Disponibilidad.
- Rendimiento.
- Latencia.
- Concurrencia.
- Consistencia.
- Observabilidad.

## Spec-Driven Development (SDD)

SDD se utilizará para definir explícitamente:

- Requisitos.
- Contratos.
- Comportamientos esperados.
- Criterios de aceptación.
- Interfaces.

## AIOps

Se utilizará un enfoque inspirado en AIOps para analizar métricas y eventos operativos.

El propósito será identificar patrones y apoyar decisiones relacionadas con:

- Escalabilidad.
- Optimización.
- Detección de anomalías.
- Capacidad.

---

# 21. Principio para la IA asistente

Los documentos de este proyecto deben ser considerados como la fuente de contexto del sistema.

La IA debe:

1. Respetar el alcance definido.

2. No inventar funcionalidades.

3. No agregar microservicios que no hayan sido aprobados.

4. No introducir tecnologías innecesarias.

5. Priorizar soluciones simples y apropiadas para un prototipo académico.

6. Mantener la separación entre los microservicios.

7. Mantener la separación entre dominio y tecnología establecida por la arquitectura hexagonal.

8. Considerar los atributos de calidad como parte fundamental del diseño.

9. Mantener la regla de no sobreventa.

10. Explicar las decisiones técnicas relacionándolas con los requisitos y atributos de calidad.

---

# 22. Fuente de verdad

En caso de conflicto entre propuestas generadas por una IA y los documentos del proyecto, deben priorizarse los requisitos y decisiones explícitamente establecidos en los documentos.

La IA puede proponer mejoras, pero debe identificarlas como propuestas y no asumir que forman parte del sistema.

---

# 23. Estado actual del proyecto

El proyecto se encuentra en etapa de diseño y planificación del prototipo.

La arquitectura inicial está definida como:

- Microservicios.
- Arquitectura hexagonal.
- Users.
- Catalog.
- Inventory.

La implementación y las decisiones técnicas detalladas se documentarán progresivamente.
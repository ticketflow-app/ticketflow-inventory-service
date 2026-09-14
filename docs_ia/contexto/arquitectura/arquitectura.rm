# Documentación de arquitectura

## Plataforma de venta de boletos para eventos de alta demanda

**Fase 1 — Microservicios: Usuarios, Catálogo y búsqueda, Inventario**

---

# 1. Visión general

Esta fase inicial del sistema cubre tres microservicios base:

- Usuarios
- Catálogo y búsqueda
- Inventario

Estos servicios constituyen la base sobre la cual podrán construirse funcionalidades adicionales en fases posteriores, como notificaciones, check-in y analítica.

Los tres microservicios comparten un entorno de ejecución basado en contenedores y se comunican mediante APIs. El acceso externo al sistema se centraliza mediante un API Gateway, responsable del enrutamiento de las solicitudes hacia los servicios correspondientes.

El diseño separa deliberadamente las responsabilidades según el comportamiento de los datos:

- **Usuarios** administra identidad, autenticación y roles.
- **Catálogo y búsqueda** administra la información estática de los eventos y sus secciones.
- **Inventario** administra la disponibilidad de boletos y las reservas, siendo el componente que requiere mayor consistencia y control de concurrencia.

Catálogo utiliza PostgreSQL como almacenamiento persistente y Redis como caché de lectura. Inventario utiliza Redis para mantener los contadores de disponibilidad y realizar operaciones atómicas, mientras PostgreSQL conserva el registro histórico y auditable de las reservas.

El objetivo principal de esta arquitectura es permitir evaluar el comportamiento del sistema ante escenarios de alta concurrencia, especialmente cuando una gran cantidad de usuarios intenta reservar boletos simultáneamente para un evento de alta demanda.

---

# 2. Objetivos arquitectónicos

La arquitectura busca cumplir los siguientes objetivos:

- Separar las responsabilidades del sistema mediante microservicios.
- Permitir escalar independientemente los servicios que reciban mayor carga.
- Mantener una alta disponibilidad durante incrementos repentinos de tráfico.
- Reducir la latencia en operaciones de lectura frecuentes.
- Evitar la sobreventa de boletos mediante control de concurrencia.
- Mantener una fuente de verdad clara para la disponibilidad.
- Aislar la lógica de negocio de las tecnologías de infraestructura.
- Facilitar las pruebas unitarias y de carga.
- Permitir incorporar nuevas funcionalidades sin modificar innecesariamente los servicios existentes.
- Proporcionar observabilidad suficiente para analizar el comportamiento del sistema bajo carga.

---

# 3. Estilo arquitectónico

La arquitectura general utiliza una combinación de:

- Arquitectura de microservicios.
- Arquitectura hexagonal dentro de cada microservicio.
- Comunicación REST síncrona.
- Comunicación asíncrona mediante eventos para funcionalidades que lo requieran.
- Redis para caché y operaciones atómicas.
- PostgreSQL para almacenamiento persistente.
- Docker para contenerización.
- API Gateway para enrutamiento y control de acceso.
- JWT para autenticación distribuida.

Cada microservicio posee una responsabilidad de negocio claramente delimitada y es responsable de sus propios datos.

Ningún microservicio debe modificar directamente las tablas pertenecientes a otro microservicio. La comunicación entre servicios se realiza mediante APIs o eventos.

---

# 4. Stack tecnológico

| Componente | Tecnología | Justificación |
| --- | --- | --- |
| Lenguaje / framework | Java + Spring Boot | Ecosistema maduro para desarrollo de microservicios y disponibilidad de herramientas para seguridad, persistencia y comunicación |
| Estilo arquitectónico interno | Arquitectura hexagonal | Aísla el dominio de Spring, JPA, Redis y otras tecnologías de infraestructura |
| Base de datos relacional | PostgreSQL | Proporciona persistencia y consistencia transaccional |
| Caché y contadores atómicos | Redis | Permite operaciones rápidas y atómicas para caché y control de disponibilidad |
| Mensajería asíncrona | Apache Kafka | Permite publicar eventos que pueden ser consumidos independientemente por diferentes componentes |
| Autenticación | JWT + Spring Security | Permite validar la identidad y los roles sin consultar al servicio de Usuarios en cada petición |
| API Gateway | Spring Cloud Gateway | Centraliza el enrutamiento y puede incorporar mecanismos como rate limiting |
| CDN y DNS | Cloudflare | Permite distribuir contenido estático y gestionar DNS |
| Contenedores | Docker + Docker Compose | Facilita la ejecución reproducible del sistema en desarrollo y pruebas |
| Persistencia en nube | Amazon RDS PostgreSQL | Servicio administrado para las bases de datos |
| Redis en nube | Amazon ElastiCache | Servicio administrado para Redis |
| Cómputo en nube | Amazon ECS Fargate | Permite ejecutar contenedores sin administrar directamente servidores |
| Balanceo | Application Load Balancer | Distribuye el tráfico entre las instancias de los servicios |
| Almacenamiento de archivos | Amazon S3 | Permite almacenar contenido estático |
| Gestión de secretos | AWS Systems Manager Parameter Store | Centraliza configuraciones sensibles |
| CI/CD | GitHub Actions + Amazon ECR + ECS | Automatiza construcción, almacenamiento y despliegue de contenedores |

## 4.1 Orquestador

Para desarrollo y pruebas se utilizará Docker Compose.

El uso de un orquestador de mayor complejidad, como Kubernetes, no forma parte de la primera fase debido al alcance del proyecto y al tamaño del equipo.

En el despliegue en AWS se utilizará Amazon ECS Fargate para administrar la ejecución de los contenedores.

---

# 5. Microservicios

La primera fase está compuesta por tres microservicios:

## 5.1 Usuarios

Responsable de:

- Registro de usuarios.
- Inicio de sesión.
- Autenticación.
- Gestión de roles.
- Consulta del perfil.

## 5.2 Catálogo y búsqueda

Responsable de:

- Creación de eventos.
- Modificación de eventos.
- Consulta de eventos.
- Búsqueda y filtrado.
- Consulta de información de las secciones.
- Administración de precios y capacidades configuradas.

Este servicio contiene información estática del evento.

La disponibilidad en tiempo real de los boletos pertenece al microservicio de Inventario.

## 5.3 Inventario

Responsable de:

- Disponibilidad de boletos.
- Reservas.
- Validación de disponibilidad.
- Actualización de disponibilidad.
- Control de concurrencia.
- Historial de reservas.

Es el microservicio más crítico desde el punto de vista de consistencia y concurrencia.

---

# 6. Arquitectura hexagonal por microservicio

Cada microservicio utiliza arquitectura hexagonal o arquitectura de puertos y adaptadores.

La arquitectura separa:

- Dominio.
- Aplicación.
- Puertos de entrada.
- Puertos de salida.
- Adaptadores.

El dominio contiene las entidades y reglas de negocio y no debe depender de:

- Spring.
- JPA.
- PostgreSQL.
- Redis.
- Kafka.
- HTTP.
- Frameworks de infraestructura.

La aplicación implementa los casos de uso y utiliza los puertos definidos para comunicarse con la infraestructura.

Los adaptadores implementan las interfaces necesarias para conectar el sistema con tecnologías externas.

---

# 7. Estructura interna de referencia

Ejemplo para el microservicio de Inventario:

```text
inventario-service/
├── domain/
│   ├── Reserva.java
│   └── EstadoReserva.java
│
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   └── ReservarBoletosUseCase.java
│   │   │
│   │   └── out/
│   │       ├── ReservaRepositoryPort.java
│   │       ├── DisponibilidadPort.java
│   │       └── EventoPublisherPort.java
│   │
│   └── service/
│       └── ReservarBoletosService.java
│
└── infrastructure/
    └── adapter/
        ├── in/
        │   └── web/
        │       └── ReservaController.java
        │
        └── out/
            ├── persistence/
            │   └── ReservaJpaAdapter.java
            ├── cache/
            │   └── RedisDisponibilidadAdapter.java
            └── messaging/
                └── KafkaEventoPublisherAdapter.java

# 8. Microservicio de Usuarios

## 8.1 Responsabilidad

El microservicio de Usuarios administra la identidad y autenticación de los actores del sistema.

Los roles considerados son:

- comprador
- organizador
- staff_checkin

El rol `staff_checkin` se contempla para futuras funcionalidades y no constituye una prioridad de la primera fase.

## 8.2 Modelo de datos

Usuario:

- id
- nombre
- email
- password_hash
- rol
- creado_en

## 8.3 Endpoints principales

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | /register | Registra un nuevo usuario |
| POST | /login | Valida credenciales y genera un JWT |
| GET | /me | Obtiene los datos del usuario autenticado |
| PATCH | /users/:id/rol | Actualiza el rol de un usuario |

Las contraseñas deben almacenarse utilizando un algoritmo de hash seguro como bcrypt o Argon2.

Nunca se almacenan contraseñas en texto plano.

## 8.4 Autenticación mediante JWT

El servicio utiliza JWT para representar la identidad autenticada.

El token contiene como mínimo:

- `sub`: identificador del usuario.
- `role`: rol del usuario.

El token es firmado digitalmente y posteriormente validado por los demás microservicios.

De esta manera, Catálogo e Inventario pueden validar la identidad y los permisos localmente sin consultar al microservicio de Usuarios en cada solicitud.

Esto evita que Usuarios se convierta en un cuello de botella o punto único de falla para las operaciones del sistema.

# 9. Microservicio de Catálogo y búsqueda

## 9.1 Responsabilidad

Catálogo administra la información estática relacionada con los eventos.

Permite a los organizadores crear y administrar eventos y a los compradores consultarlos, buscarlos y filtrarlos.

La disponibilidad de boletos en tiempo real no pertenece a este servicio.

## 9.2 Modelo de datos

### Evento

- id
- organizador_id
- nombre
- descripción
- fecha
- lugar
- categoría

### Sección

- id
- evento_id
- nombre
- precio
- capacidad_total

## 9.3 Endpoints principales

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | /events | Crea un evento |
| GET | /events?fecha=&ciudad=&categoria= | Consulta y filtra eventos |
| GET | /events/:id | Consulta el detalle de un evento |
| PATCH | /events/:id | Modifica un evento |

## 9.4 Estrategia de caché

Se utiliza el patrón `cache-aside`.

Para una operación de lectura:

1. El servicio consulta Redis.
2. Si existe el dato, devuelve la información almacenada.
3. Si no existe, consulta PostgreSQL.
4. El resultado se almacena en Redis.
5. Se asigna un TTL para evitar que la información permanezca indefinidamente.

Cuando se modifica un evento, se invalida la entrada correspondiente de Redis.

Esta estrategia es adecuada porque las consultas del catálogo serán mucho más frecuentes que las modificaciones y unos segundos de desactualización en información estática no afectan la disponibilidad de boletos.

# 10. Microservicio de Inventario

## 10.1 Responsabilidad

Inventario administra la disponibilidad de boletos y las reservas.

Es el componente que requiere mayor control de consistencia y concurrencia.

La disponibilidad se representa mediante contadores atómicos en Redis.

## 10.2 Modelo de datos

Redis mantiene la disponibilidad actual utilizando una clave con la siguiente estructura:

`availability:{evento_id}:{seccion_id}`

El valor almacenado corresponde a la cantidad de boletos disponibles.

PostgreSQL mantiene el registro persistente de las reservas.

### Reserva

- id
- usuario_id
- evento_id
- seccion_id
- cantidad
- estado
- creado_en

Los estados de una reserva pueden ser:

- PENDING
- CONFIRMED
- CANCELLED

## 10.3 Endpoints principales

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | /availability/:evento_id | Consulta la disponibilidad actual por sección |
| POST | /reservations | Crea una reserva |
| GET | /reservations/user/:id | Consulta las reservas de un usuario |
| DELETE | /reservations/:id | Cancela una reserva, si la funcionalidad está habilitada |

## 10.4 Control de disponibilidad

La disponibilidad se controla mediante operaciones atómicas de Redis.

Para reservar N boletos:

1. El cliente solicita N boletos.
2. Inventario ejecuta `DECRBY` sobre el contador correspondiente.
3. Si el resultado es mayor o igual a cero, la operación puede continuar.
4. Se registra la reserva en PostgreSQL.
5. Si el resultado es negativo, se ejecuta `INCRBY` para restaurar el contador.
6. Se informa al cliente que no existe disponibilidad suficiente.

## 10.5 Control de concurrencia

El uso de operaciones atómicas permite que varias solicitudes concurrentes modifiquen el mismo contador sin producir una condición de carrera.

Por ejemplo, si existe una disponibilidad inicial de un boleto y dos usuarios intentan reservarlo simultáneamente, solo una de las operaciones podrá obtener correctamente el último boleto.

El resultado esperado es:

- Usuario A: reserva aceptada.
- Usuario B: reserva rechazada.
- Disponibilidad final: 0.

Esta estrategia evita la sobreventa producida por múltiples solicitudes que consultan simultáneamente una misma cantidad disponible.

## 10.6 Redis y PostgreSQL

Redis es responsable de la decisión de disponibilidad en tiempo real.

PostgreSQL mantiene el registro durable y auditable de las reservas.

Esta separación permite que las operaciones críticas de disponibilidad sean rápidas, mientras que la base de datos conserva la información necesaria para consultar el historial.

La implementación debe considerar el escenario en el que la operación en Redis sea exitosa pero la escritura posterior en PostgreSQL falle. Este caso deberá ser evaluado durante las pruebas de consistencia y resiliencia.

# 11. Comunicación entre servicios

La comunicación entre los microservicios utiliza principalmente REST para las operaciones síncronas.

## 11.1 Usuarios

Los demás servicios validan localmente el JWT generado por Usuarios.

No se realiza una llamada a Usuarios en cada solicitud autenticada.

## 11.2 Catálogo → Inventario

Cuando se crea o configura un evento con sus secciones, Inventario debe disponer de los contadores correspondientes.

Durante esta fase, la inicialización o actualización de los contadores se realiza mediante comunicación REST síncrona.

Esta comunicación puede evolucionar posteriormente hacia un mecanismo basado en eventos.

## 11.3 Inventario → eventos

Cuando una reserva es confirmada, Inventario puede publicar un evento en Kafka.

El evento contiene la información necesaria para que otros componentes puedan reaccionar sin acoplarse directamente al servicio de Inventario.

# 12. Mensajería asíncrona con Kafka

Apache Kafka se utiliza para comunicación basada en eventos cuando múltiples consumidores necesitan reaccionar de manera independiente ante una misma operación.

Kafka se eligió sobre RabbitMQ debido principalmente al patrón de consumo esperado.

| Criterio | Kafka | RabbitMQ |
| --- | --- | --- |
| Modelo | Log distribuido | Cola de mensajes |
| Múltiples consumidores independientes | Sí | Sí, mediante exchanges |
| Replay de eventos | Sí | Limitado |
| Consumer groups | Sí | No utiliza el mismo modelo |
| Complejidad operativa | Mayor | Menor |
| Adecuado para eventos históricos | Sí | Menos orientado a este escenario |

El caso de uso principal consiste en publicar un evento cuando Inventario confirma una reserva.

Posteriormente, diferentes componentes podrán consumir ese evento de manera independiente.

## 12.1 Topic principal

`reservas.confirmadas`

Productor:

`Inventario`

Consumidores futuros:

- Notificaciones
- Analítica

Las funcionalidades de Notificaciones y Analítica no forman parte de los tres microservicios principales de esta fase.

# 13. Estrategia de caché

| Operación | Estrategia | Justificación |
| --- | --- | --- |
| Explorar catálogo | Redis + TTL | Alta proporción de lecturas y tolerancia a pequeños periodos de desactualización |
| Consultar detalle del evento | Redis + TTL | Reduce consultas repetitivas a PostgreSQL |
| Comprar boleto | Sin caché tradicional | Requiere consistencia inmediata |
| Consultar disponibilidad | Redis | El contador representa la disponibilidad actual |

Redis cumple dos funciones diferentes:

1. Caché para información del catálogo.
2. Contador atómico para disponibilidad.

La disponibilidad no debe tratarse como una caché tradicional porque constituye el estado utilizado para tomar la decisión de reserva.

# 14. Resiliencia y observabilidad

La arquitectura incorpora mecanismos para observar y controlar el comportamiento del sistema bajo carga.

| Necesidad | Herramienta | Propósito |
| --- | --- | --- |
| Circuit breaker y reintentos | Resilience4j | Evitar propagación de fallos entre servicios |
| Métricas | Spring Boot Actuator + Micrometer | Exponer métricas de operación |
| Trazabilidad distribuida | Micrometer Tracing + Zipkin | Seguir solicitudes entre servicios |
| Rate limiting | Spring Cloud Gateway + Redis | Controlar tráfico excesivo |
| Idempotencia | Clave de idempotencia en POST /reservations | Evitar reservas duplicadas por reintentos |
| Mensajes fallidos | Dead-letter topic de Kafka | Gestionar eventos que no pudieron procesarse |

Estas herramientas permiten obtener información para posteriormente analizar:

- Latencia.
- Throughput.
- Errores.
- Uso de CPU.
- Uso de memoria.
- Saturación.
- Cantidad de solicitudes.
- Comportamiento ante incrementos de tráfico.

# 15. Idempotencia de reservas

La operación de creación de reservas debe considerar los reintentos de red.

Un cliente puede enviar nuevamente una solicitud porque no recibió la respuesta original, aunque la reserva ya haya sido procesada.

Para evitar reservas duplicadas se utilizará una clave de idempotencia asociada a la solicitud.

Conceptualmente, una solicitud de reserva puede incluir:

`Idempotency-Key: identificador-unico`

Si el mismo identificador se recibe nuevamente, el servicio debe reconocer que la operación ya fue procesada y evitar crear una segunda reserva.

# 16. API Gateway

El sistema utiliza Spring Cloud Gateway como punto de entrada lógico hacia los microservicios.

Sus responsabilidades incluyen:

- Enrutamiento.
- Control de acceso.
- Rate limiting.
- Manejo de solicitudes comunes.
- Integración con Redis para mecanismos de limitación de tráfico.

El Gateway no contiene lógica de negocio.

La lógica de negocio permanece dentro de los microservicios correspondientes.

En el entorno de AWS, el Application Load Balancer se utiliza para distribuir el tráfico hacia las instancias desplegadas.

# 17. Despliegue en AWS

El despliegue se centraliza en AWS para evitar la complejidad de administrar múltiples proveedores de nube.

Cloudflare constituye una excepción y se utiliza únicamente para CDN y DNS.

## 17.1 Componentes

| Capa | Servicio | Propósito |
| --- | --- | --- |
| Cómputo | ECS Fargate | Ejecutar contenedores |
| Balanceo | Application Load Balancer | Distribuir tráfico |
| Base de datos | RDS PostgreSQL | Persistencia |
| Caché | ElastiCache Redis | Caché y disponibilidad |
| Mensajería | Kafka en Docker/EC2 | Comunicación basada en eventos |
| Almacenamiento | Amazon S3 | Archivos y contenido estático |
| CDN / DNS | Cloudflare | Distribución y resolución de contenido |
| Secretos | Systems Manager Parameter Store | Configuración sensible |
| Registro de imágenes | Amazon ECR | Almacenamiento de imágenes Docker |
| CI/CD | GitHub Actions | Automatización del despliegue |

## 17.2 PostgreSQL

Se utilizará PostgreSQL como sistema de persistencia.

Cada microservicio debe ser propietario de sus datos.

La separación lógica puede realizarse mediante esquemas independientes dentro de una misma instancia durante el prototipo.

Pueden existir los siguientes esquemas:

- `usuarios`
- `catalogo`
- `inventario`

Esta configuración permite reducir costos y complejidad durante el desarrollo.

En una arquitectura de mayor escala, cada servicio podría evolucionar hacia una instancia de base de datos independiente.

## 17.3 Kafka

Durante el desarrollo académico Kafka puede ejecutarse mediante Docker.

Para un despliegue productivo se podría utilizar Amazon MSK, pero no constituye una prioridad para el prototipo debido a su costo y complejidad adicional.

# 18. Contenedores y entorno de desarrollo

Durante el desarrollo y las pruebas de carga se utilizará Docker Compose.

El entorno puede contener:

- Usuarios.
- Catálogo.
- Inventario.
- PostgreSQL.
- Redis.
- Kafka.
- API Gateway.
- Herramientas de observabilidad.

Docker Compose permite reproducir el entorno de pruebas y facilita la ejecución de escenarios de alta concurrencia.

El uso de Kubernetes no forma parte del alcance inicial.

# 19. Escenario crítico de arquitectura

El escenario principal utilizado para validar la arquitectura es la apertura de venta de un evento de alta demanda.

En este escenario:

1. Un evento se encuentra configurado en Catálogo.
2. Inventario mantiene los contadores de disponibilidad.
3. La venta se habilita.
4. Una gran cantidad de usuarios consulta simultáneamente el evento.
5. Los usuarios seleccionan una sección.
6. Los usuarios solicitan reservar boletos.
7. Inventario ejecuta operaciones atómicas sobre Redis.
8. Las solicitudes que obtienen disponibilidad continúan con la creación de la reserva.
9. Las solicitudes que no obtienen disponibilidad son rechazadas.
10. El sistema registra las reservas en PostgreSQL.
11. Las reservas confirmadas pueden generar eventos para consumidores posteriores.

El escenario permite evaluar principalmente:

- Concurrencia.
- Latencia.
- Throughput.
- Disponibilidad.
- Tasa de errores.
- Consistencia.
- Uso de recursos.
- Comportamiento ante picos de tráfico.

# 20. Escalabilidad

Los microservicios están diseñados para poder escalar independientemente.

Por ejemplo:

- Catálogo puede aumentar sus instancias cuando exista una gran cantidad de consultas.
- Inventario puede aumentar sus instancias durante una apertura de venta.
- Usuarios puede escalar de acuerdo con las necesidades de autenticación.

El estado crítico de disponibilidad no se mantiene únicamente en memoria local de una instancia.

Redis permite que diferentes instancias de Inventario trabajen sobre los mismos contadores de disponibilidad.

Esto evita que cada instancia tenga una visión diferente de los boletos restantes.

# 21. Principios de consistencia

La arquitectura distingue entre información que puede tolerar cierta desactualización e información que requiere consistencia inmediata.

## 21.1 Información con tolerancia a desactualización

Ejemplos:

- Nombre del evento.
- Descripción.
- Lugar.
- Categoría.
- Información general de las secciones.

Esta información puede utilizar caché.

## 21.2 Información que requiere consistencia inmediata

El principal ejemplo es:

- Cantidad de boletos disponibles.

Esta información se mantiene mediante contadores atómicos en Redis.

La separación permite utilizar diferentes estrategias de almacenamiento según los requisitos de cada operación.

# 22. Reglas arquitectónicas

Las siguientes reglas deben mantenerse durante el desarrollo:

1. Cada microservicio debe tener una responsabilidad de negocio clara.
2. Cada servicio debe ser propietario de sus propios datos.
3. Ningún servicio debe modificar directamente la base de datos de otro servicio.
4. El dominio no debe depender de tecnologías de infraestructura.
5. Redis debe utilizarse de acuerdo con la responsabilidad definida para cada operación.
6. Las operaciones de disponibilidad deben ser atómicas.
7. La lógica de negocio no debe implementarse en los controladores REST.
8. Los controladores deben comunicarse con casos de uso mediante puertos de entrada.
9. La persistencia debe accederse mediante puertos de salida.
10. La autenticación entre servicios debe utilizar JWT.
11. Las reservas deben contemplar idempotencia.
12. Las operaciones críticas deben generar métricas suficientes para su evaluación.
13. Las decisiones de escalabilidad deben estar respaldadas por métricas obtenidas durante las pruebas.

# 23. Tecnologías fuera del alcance inicial

Para mantener el alcance adecuado al tamaño del equipo y al tiempo disponible, las siguientes tecnologías o funcionalidades no forman parte de la primera fase:

- Kubernetes.
- Elasticsearch.
- Service Mesh.
- Integración con sistemas externos de pago.
- Procesamiento real de pagos.
- Facturación.
- Notificaciones reales.
- Aplicación móvil.
- Sistemas reales de check-in.
- Integraciones externas.
- Sistemas de recomendación.

Estas funcionalidades pueden considerarse para fases posteriores si los resultados del prototipo justifican su incorporación.

# 24. Orden de construcción

El desarrollo de los microservicios seguirá el siguiente orden:

## 24.1 Usuarios

Se construirá primero porque el resto del sistema necesita autenticación y autorización.

Incluye:

- Registro.
- Inicio de sesión.
- JWT.
- Roles.
- Perfil.

## 24.2 Catálogo y búsqueda

Se construirá después porque permite crear los eventos que posteriormente serán utilizados por Inventario.

Incluye:

- Creación de eventos.
- Consulta de eventos.
- Búsqueda.
- Filtrado.
- Consulta de detalle.
- Configuración de secciones.
- Precios.
- Capacidades.

## 24.3 Inventario

Se construirá posteriormente debido a que contiene la lógica más crítica.

Incluye:

- Inicialización de disponibilidad.
- Consulta de disponibilidad.
- Reservas.
- Actualización de disponibilidad.
- Control de concurrencia.
- Idempotencia.
- Historial de reservas.

# 25. Relación con las pruebas de carga

La arquitectura está diseñada para permitir pruebas de carga y concurrencia.

Los escenarios principales serán:

- Carga sostenida.
- Incremento repentino de usuarios.
- Pico de tráfico durante la apertura de un evento.
- Múltiples usuarios intentando reservar los últimos boletos.
- Consultas simultáneas del catálogo.
- Solicitudes concurrentes de disponibilidad.
- Reservas simultáneas sobre una misma sección.

Las pruebas permitirán medir:

- Tiempo de respuesta.
- Throughput.
- Porcentaje de errores.
- Disponibilidad.
- Consumo de CPU.
- Consumo de memoria.
- Capacidad de procesamiento.
- Comportamiento del sistema ante incrementos de carga.
- Recuperación después de una situación de sobrecarga.

# 26. Evolución futura

La arquitectura está diseñada para permitir incorporar nuevos componentes sin modificar directamente la lógica de los servicios existentes.

Entre las posibles extensiones se encuentran:

- Notificaciones.
- Check-in.
- Analítica.
- Monitoreo predictivo.
- Recomendaciones de escalabilidad.
- Automatización de decisiones operativas.

Estas extensiones podrán aprovechar los eventos publicados por Inventario mediante Kafka.

La incorporación de nuevas tecnologías deberá justificarse según las necesidades observadas durante las pruebas y no únicamente por motivos tecnológicos.

# 27. Estado actual de la arquitectura

La arquitectura definida para esta fase está compuesta por:

- 3 microservicios:
  - Usuarios.
  - Catálogo y búsqueda.
  - Inventario.
- Arquitectura hexagonal dentro de cada microservicio.
- PostgreSQL para persistencia.
- Redis para caché y contadores atómicos.
- JWT para autenticación.
- REST para comunicación síncrona.
- Kafka para comunicación basada en eventos.
- Spring Cloud Gateway como API Gateway.
- Docker Compose para desarrollo y pruebas.
- AWS ECS Fargate para despliegue en nube.
- Cloudflare para CDN y DNS.
- Herramientas de observabilidad mediante Actuator, Micrometer y trazabilidad distribuida.

El componente central para el escenario de alta demanda es Inventario, debido a que debe garantizar que múltiples solicitudes concurrentes no produzcan sobreventa.

La arquitectura prioriza simplicidad, separación de responsabilidades, capacidad de escalamiento y control de concurrencia, manteniendo el alcance adecuado para un prototipo académico desarrollado por un equipo pequeño.
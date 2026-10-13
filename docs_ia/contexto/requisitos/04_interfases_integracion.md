@architect Lee todos los archivos existentes en /docs_ia/contexto/ (01_requisitos_funcionales.md, 02_atributos_calidad.md y 03_reglas_negocio.md). 

Basándote en esos requisitos, diseña la propuesta de arquitectura técnica para el Microservicio de Inventario considerando:
1. Stack Tecnológico: Java 17/21, Spring Boot 3, Spring Data JPA, PostgreSQL y Flyway para migraciones.
2. Arquitectura en capas: Arquitectura Hexagonal (Ports & Adapters) para desacoplar el dominio.
3. Estrategia de Concurrencia: Bloqueo Pesimista (Pessimistic Locking con `SELECT FOR UPDATE`) para las consultas y reservas de stock críticas, garantizando prevención absoluta de sobreventa.
4. Definición de la Base de Datos: Tablas `events`, `locations`, `inventory` y `reservations`.
5. API REST: Endpoints principales con nomenclatura estricta en inglés y manejo global de excepciones.

Genera el documento DA-001-arquitectura-tecnica-inventario.md en /docs_ia/decisiones/ con estado PROPUESTA, registra la trazabilidad P-001 en /docs_ia/prompts/ y preséntame el resumen en el chat para mi aprobación.
# T-003 — Verificación y completado de puertos de salida

## 1. Información general

- ID: T-003
- Estado: PENDIENTE
- Prioridad: ALTA
- Tipo: IMPLEMENTACIÓN
- Responsable: @coder
- Revisores: @architect y @tester

## 2. Objetivo

Crear los tres puertos de salida que utilizan los casos de uso del microservicio Inventory y que actualmente no se encontraron en el código fuente.

El objetivo es completar los contratos necesarios para permitir la compilación, sin implementar adaptadores ni lógica de negocio adicional.

## 3. Alcance

Crear únicamente las siguientes interfaces:

1. `AvailabilityRepository.java`
2. `ReservationRepository.java`
3. `ReservationEventPublisher.java`

Ubicación: `src/main/java/com/inventory/service/application/port/out/`

Firmas propuestas:

- `AvailabilityRepository`: `Mono<Availability> getAvailability(Long eventId);`
- `ReservationRepository`: `Mono<Reservation> save(Reservation reservation);`
- `ReservationEventPublisher`: `Mono<Void> publish(Reservation reservation);`

Los paquetes e importaciones deben corresponder con los modelos de dominio y los tipos utilizados por los casos de uso existentes.

## 4. Restricciones

- No modificar archivos existentes.
- No modificar `pom.xml` ni la configuración.
- No implementar adaptadores de infraestructura.
- No agregar lógica de Redis, PostgreSQL o Kafka.
- No introducir nuevas dependencias.
- Respetar la arquitectura hexagonal y las reglas de `.clinerules`.
- Si se encuentra una incompatibilidad con el código existente, detenerse y consultar antes de ampliar el alcance.

## 5. Criterios de aceptación

- Existen las tres interfaces en el paquete indicado.
- Las firmas coinciden con los usos de los casos de uso.
- Las interfaces no dependen de componentes de infraestructura.
- La compilación se ejecuta y su resultado real queda documentado.
- No se modifican archivos fuera del alcance autorizado.

## 6. Responsabilidades

- `@coder`: implementar las tres interfaces y reportar los cambios.
- `@tester`: revisar los contratos y verificar la compilación si sus permisos lo permiten.
- `@architect`: revisar la coherencia de los contratos con la arquitectura hexagonal.

## 7. Estado de verificación

- Implementación: PENDIENTE.
- Compilación: PENDIENTE.
- Revisión: PENDIENTE.
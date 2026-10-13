# Restricciones de seguridad

## 1. Propósito

Este documento establece las restricciones de seguridad que deben respetarse durante el desarrollo, configuración, ejecución y mantenimiento del microservicio Inventory del proyecto AI-SEMLA.

Su objetivo es prevenir la exposición de información sensible, proteger las operaciones de reservas y disponibilidad, y garantizar que las modificaciones respeten los requisitos de seguridad documentados.

## 2. Cumplimiento de los requisitos de seguridad

- Toda implementación debe respetar los requisitos definidos en `../atributos_calidad/04_seguridad.md`.
- No se deben eliminar, debilitar ni omitir controles de seguridad para simplificar el desarrollo o resolver errores.
- No se deben asumir mecanismos de autenticación, autorización o validación que no estén definidos en los documentos aprobados.
- Las decisiones de seguridad pendientes deben identificarse explícitamente antes de implementar mecanismos que dependan de ellas.
- Las pruebas deben verificar los controles de seguridad pertinentes para cada modificación.

## 3. Protección de credenciales y secretos

- No se deben incluir contraseñas, claves privadas, tokens, credenciales de bases de datos ni secretos de infraestructura directamente en el código fuente.
- Los secretos no deben almacenarse en archivos versionados sin los mecanismos de protección establecidos para el proyecto.
- Se deben respetar los mecanismos de configuración existentes para proporcionar credenciales y secretos durante la ejecución.
- No se deben publicar secretos en registros, mensajes de error, documentación, capturas de pantalla o resultados de pruebas.
- Si se detecta un secreto expuesto, se debe informar y recomendar su revocación o rotación según corresponda.
- No se deben inventar credenciales reales ni utilizar valores de producción en pruebas sin autorización.

## 4. Autenticación y validación de JWT

- La autenticación debe respetar el mecanismo JWT definido en los documentos de seguridad del proyecto.
- La validación de tokens debe realizarse localmente, de acuerdo con la estrategia establecida.
- No se deben aceptar tokens cuya firma no pueda validarse correctamente.
- Se deben respetar las validaciones de expiración y demás comprobaciones definidas para los tokens.
- No se deben confiar en los datos contenidos en un token únicamente porque este tenga una estructura JWT válida.
- No se deben aceptar algoritmos, claves o mecanismos de validación alternativos sin una decisión de seguridad aprobada.
- No se deben registrar ni exponer tokens completos.
- No se debe implementar autenticación propia adicional que contradiga el mecanismo establecido.

Los detalles exactos de firma, algoritmo, claims y configuración deben obtenerse de la documentación aprobada y de la configuración real del proyecto; no deben suponerse.

## 5. Autorización y control de acceso

- Las operaciones protegidas deben aplicar los controles de autorización definidos en los requisitos.
- La identidad del usuario debe verificarse antes de permitir el acceso a recursos privados.
- No se debe confiar únicamente en un `userId`, `reservationId` u otro identificador enviado por el cliente para autorizar una operación.
- Se debe impedir que un usuario acceda o modifique reservas ajenas sin autorización.
- Los permisos administrativos deben aplicarse únicamente cuando estén definidos y aprobados para la operación correspondiente.
- No se deben omitir controles de autorización en controladores, casos de uso o adaptadores.
- Las respuestas deben distinguir entre errores de autenticación y de autorización según el contrato de la API.

Los nombres definitivos de roles y sus permisos deben corresponder con los requisitos aprobados; no se deben inventar roles ni asignar privilegios por conveniencia.

## 6. Protección de la disponibilidad y las reservas

- Las solicitudes deben validarse antes de ejecutar operaciones que modifiquen la disponibilidad.
- No se deben permitir reservas que incumplan las reglas de negocio aprobadas.
- Las operaciones sobre Redis deben respetar los mecanismos de concurrencia establecidos para evitar asignaciones por encima de la disponibilidad.
- La idempotencia debe respetarse en las operaciones para las que esté definida.
- Los errores de infraestructura no deben interpretarse automáticamente como operaciones exitosas.
- Los mecanismos de compensación y recuperación deben evitar, en la medida definida por el diseño, duplicaciones o inconsistencias en las reservas.
- No se deben exponer datos de reservas a usuarios que no tengan autorización para consultarlos.

## 7. Validación de solicitudes y datos

- Todos los datos recibidos del cliente deben tratarse como entradas no confiables.
- Se deben validar los campos obligatorios, los tipos de datos, los formatos y las restricciones establecidas en los requisitos.
- Los errores de validación no deben producir modificaciones parciales no controladas en la disponibilidad o las reservas.
- Las operaciones de persistencia deben utilizar mecanismos seguros de acceso a datos.
- No se deben construir consultas inseguras mediante concatenación de entradas no confiables.
- Los mensajes de error no deben revelar consultas internas, estructura de infraestructura, credenciales ni detalles sensibles.
- No se deben agregar restricciones funcionales nuevas sin verificar que estén respaldadas por los requisitos.

## 8. Protección de datos y registros

- Los registros de aplicación deben contener únicamente la información necesaria para diagnosticar y supervisar el servicio.
- No se deben registrar contraseñas, tokens completos, claves privadas ni credenciales.
- Los datos personales o sensibles deben tratarse conforme a los requisitos aplicables y a las políticas del proyecto.
- Los identificadores y datos de reservas no deben exponerse innecesariamente en mensajes, métricas o registros.
- Las excepciones no deben devolver trazas internas ni detalles de implementación a los clientes.
- Las métricas no deben incluir información sensible como etiquetas o dimensiones.
- La información de diagnóstico debe permitir investigar fallos sin comprometer la confidencialidad.

## 9. Configuración y exposición de servicios

- Los servicios externos y las conexiones deben utilizar la configuración establecida para cada entorno.
- No se deben incorporar credenciales directamente en archivos de configuración versionados.
- Los endpoints de administración y diagnóstico deben exponerse únicamente conforme a la política de seguridad definida.
- No se deben desactivar controles de acceso para facilitar pruebas locales.
- No se deben habilitar servicios, puertos o interfaces innecesarios sin justificación.
- Los cambios de configuración que afecten la autenticación, la autorización, las conexiones o la exposición de información deben revisarse antes de aplicarse.

## 10. Seguridad en Kafka y sistemas externos

- La comunicación con sistemas externos debe respetar la configuración de seguridad definida para cada entorno.
- No se deben exponer credenciales de Kafka, Redis o PostgreSQL en el código o los registros.
- Los mensajes recibidos desde sistemas externos deben validarse antes de procesarse.
- No se debe asumir que un evento es confiable únicamente porque provenga de un sistema integrado.
- Los contratos de eventos deben respetarse y los cambios incompatibles deben revisarse.
- No se deben inventar mecanismos de cifrado, autenticación entre servicios o permisos de infraestructura como si ya estuvieran configurados.

## 11. Restricciones durante las pruebas

- Las pruebas deben utilizar datos ficticios o expresamente autorizados.
- No se deben utilizar credenciales reales en el código de las pruebas ni publicarlas en los resultados.
- No se deben desactivar controles de autenticación o autorización para conseguir que una prueba pase.
- Las pruebas negativas deben verificar que las solicitudes no autorizadas sean rechazadas.
- Se deben probar, cuando corresponda, tokens inválidos o expirados, accesos no autorizados y solicitudes malformadas.
- Las pruebas de seguridad deben realizarse en entornos controlados.
- No se deben ejecutar pruebas destructivas contra servicios o datos reales sin autorización explícita.

## 12. Cambios que requieren autorización

Se requiere autorización explícita antes de:

- Cambiar el mecanismo de autenticación.
- Modificar la estrategia de validación de JWT.
- Alterar las reglas de autorización o los permisos.
- Desactivar controles de seguridad.
- Cambiar la gestión o ubicación de secretos.
- Exponer nuevos endpoints de administración o información operativa.
- Modificar la política de acceso a reservas y datos de usuarios.
- Cambiar la configuración de seguridad de Redis, PostgreSQL o Kafka.
- Incorporar dependencias que afecten la autenticación, la autorización o la protección de datos.

Las propuestas deben incluir la justificación, los riesgos, los componentes afectados y las pruebas necesarias.

## 13. Verificación y reporte

Antes de considerar terminado un cambio relacionado con seguridad, se debe:

1. Identificar los requisitos de seguridad afectados.
2. Verificar que los controles existentes se mantienen.
3. Ejecutar las pruebas pertinentes cuando sea posible.
4. Revisar que no se hayan expuesto secretos o datos sensibles.
5. Informar los resultados reales de las verificaciones.
6. Documentar las limitaciones o decisiones pendientes.

No se debe afirmar que el microservicio es seguro únicamente porque compila o porque una prueba básica finaliza correctamente.

## 14. Relación con otros documentos

Este documento complementa:

- `../atributos_calidad/04_seguridad.md`
- `../diseño/02_componentes_y_responsabilidades.md`
- `../diseño/03_flujos_de_negocio.md`
- `../diseño/04_integracion_y_mensajeria.md`
- `../diseño/05_configuracion.md`
- Los requisitos funcionales, los contratos de API y las decisiones arquitectónicas aprobadas.

Si se identifica una contradicción entre documentos, se debe registrar y resolver antes de realizar cambios que puedan afectar la seguridad. Las medidas propuestas o pendientes no deben presentarse como mecanismos ya implementados.
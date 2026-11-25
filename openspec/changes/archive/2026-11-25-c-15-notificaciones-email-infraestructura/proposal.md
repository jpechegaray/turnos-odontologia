# Proposal

## Why

El consultorio necesita avisar por email la creación, cancelación, reprogramación y recordatorio de turnos, y los avisos de huecos de la lista de espera, sin acoplar los módulos de dominio a un servidor SMTP. C-15 entrega la infraestructura común de notificaciones —puerto, adaptador SMTP, registro idempotente y reintentos— para que C-25, C-36, C-37, C-41 y C-44 la consuman sin duplicar lógica (RN-30, RN-31, DD-07).

## What Changes

- Crear el puerto `NotificacionPort` y sus adaptadores: `SmtpNotificacionAdapter` (Spring Mail, credenciales solo por entorno) e `InMemoryNotificacionAdapter` (tests, sin SMTP real).
- Persistir cada envío en `NotificacionEnviada` (tipo, canal `EMAIL`, destinatario, `inicio_referencia`, estado `PENDIENTE|ENVIADA|FALLIDA`, intentos, último error, programación y fecha de envío), con referencias por id a turno, oferta de hueco y paciente.
- Garantizar idempotencia con `UNIQUE (turno_id, tipo, inicio_referencia)` y `UNIQUE (oferta_hueco_id, tipo)`.
- Implementar el servicio de envío con hasta 3 intentos y espera creciente (RN-30), más un planificador `@Scheduled` que retoma pendientes y reintenta fallidas sin perder el turno.
- Armar la plantilla de email base con `consultorio.nombre` de C-08, fecha y hora en Buenos Aires, sin datos de otros pacientes (RN-31, RN-35).
- Completar solo el bloque de mail en `application.yml` (`app.notificaciones` / `spring.mail`) y documentar las variables en `.env.example`.
- No se implementa todavía qué evento dispara cada tipo de email: C-15 expone la infraestructura y los consumidores (C-25, C-36, C-37, C-41, C-44) definen el contenido y el momento. No se agrega WhatsApp ni una cola externa.

## Capabilities

### New Capabilities

- `notificaciones-email`: envío de emails transaccionales por un puerto con adaptador SMTP y adaptador en memoria, registro idempotente en `NotificacionEnviada`, reintentos con espera creciente, planificador de pendientes y fallidas, y plantilla base sin datos de otros pacientes.

### Modified Capabilities

Ninguna. Se reutilizan `configuracion-parametros` (`consultorio.nombre`), `reloj-del-sistema` (`Clock`) y `errores-api` sin cambiar sus requisitos.

## Impact

- Nuevo paquete `backend/src/main/java/ar/edu/utn/turnos/shared/notificacion/` con `domain`, `application` e `infrastructure`; tabla `notificacion_enviada`; pruebas unitarias e integración con Testcontainers.
- `backend/src/main/resources/application.yml`: se completa solo el bloque `app.notificaciones`; `spring-boot-starter-mail` ya está declarado en `backend/pom.xml`.
- `backend/src/main/resources/schema-constraints.sql`: se agregan las dos restricciones únicas de `NotificacionEnviada` y el índice del planificador si Hibernate no los genera.
- `.env.example` y `.env` (no versionado): variables `SMTP_*`; ninguna credencial en el repositorio.
- No cambia el contrato HTTP de módulos existentes ni el login.

## Governance

**Governance: HIGH.** El equipo revisa y aprueba esta propuesta, el diseño y las specs antes de implementar. Este change no escribe código de producción ni tests en la fase de propuesta.

## Risks / Preconditions

- **RM-02 (`application.yml`)**: el bloque de mail está comentado y reservado a C-15; se completa sin tocar los bloques de otros módulos.
- **Precondiciones de roadmap**: C-01, C-07 y C-08 ya están mergeados y archivados; el change depende de `Clock` (C-01) y de `consultorio.nombre` (C-08).
- **Fallo de SMTP**: el turno nunca se pierde; el envío queda `FALLIDA` con `ultimo_error` y se reintenta hasta 3 veces.
- **Duplicados**: dos inserciones iguales deben violar la restricción única; la garantía final es de PostgreSQL, no solo de Java.
- **Secretos**: un test o revisión debe confirmar que ninguna credencial SMTP quedó versionada.

# recordatorio-turno Specification

## Purpose

Enviar recordatorios por email para turnos próximos con un enlace de gestión del turno.

## Requirements

### Requirement: Planificación idempotente por horario

El sistema SHALL consultar cada cinco minutos los turnos PROGRAMADO o CONFIRMADO con email y un inicio futuro dentro de la anticipación configurable. SHALL registrar un pendiente como máximo por turno e inicio de referencia.

#### Scenario: Dos ciclos no duplican el recordatorio

- **WHEN** el planificador procesa dos veces el mismo turno elegible con el mismo inicio
- **THEN** solo hay un registro y un envío exitoso

#### Scenario: Dos ciclos concurrentes encolan y envían un solo recordatorio

- **WHEN** dos ciclos detectan el mismo turno y compiten por registrarlo y enviarlo
- **THEN** queda una sola fila y el correo se intenta una sola vez

#### Scenario: La ventana configurable incluye exactamente su límite superior

- **WHEN** hay un turno cuya anticipación coincide con el valor configurado y otro fuera de la ventana
- **THEN** solo se encola el turno que coincide con el límite

#### Scenario: Un turno creado con poca anticipación entra en el siguiente ciclo

- **WHEN** se crea un turno dentro de la ventana de recordatorios
- **THEN** el próximo ciclo registra y procesa su recordatorio

#### Scenario: Un paciente sin email se omite

- **WHEN** el turno elegible pertenece a un paciente sin email
- **THEN** no se registra ni se envía el recordatorio

### Requirement: Envío vigente con enlace y reintentos

Cada intento SHALL usar el estado e inicio actuales del turno, respetar los reintentos de C-15 y adjuntar el enlace sin persistir ni registrar el token en claro.

#### Scenario: Un turno reprogramado recibe recordatorio para su nuevo inicio

- **WHEN** se cambia el inicio de un turno con recordatorio previo
- **THEN** se descarta cualquier envío obsoleto y el nuevo inicio puede recibir un recordatorio

#### Scenario: Un turno cancelado o iniciado no recibe un pendiente

- **WHEN** se reclama un recordatorio cuyo turno se canceló o ya empezó
- **THEN** se descarta sin enviar email

#### Scenario: El recordatorio de un turno interno incluye un enlace válido

- **WHEN** se envía el recordatorio de un turno futuro creado por recepción
- **THEN** se incluye un enlace que identifica únicamente a ese turno y el secreto solo se guarda como hash

#### Scenario: Un fallo de correo se reintenta hasta el tope

- **WHEN** el envío falla y el turno sigue elegible
- **THEN** el recordatorio se vuelve a intentar con espera creciente, hasta el máximo de C-15

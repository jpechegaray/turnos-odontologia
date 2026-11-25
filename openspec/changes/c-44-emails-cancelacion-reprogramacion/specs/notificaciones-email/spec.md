## ADDED Requirements

### Requirement: Avisos al confirmar cambios de turno

Tras confirmar una cancelación por el personal o una reprogramación, el sistema SHALL registrar y enviar el aviso por C-15 si el paciente tiene email (RN-31). El aviso SHALL indicar la fecha y hora del turno cancelado o el nuevo inicio para una reprogramación.

#### Scenario: La cancelación del personal envía un solo correo
- **WHEN** el personal cancela un turno de un paciente con email y se procesa dos veces el mismo evento confirmado
- **THEN** hay un registro `CANCELACION` y se envía un correo con el horario cancelado

#### Scenario: La reprogramación avisa el nuevo horario y descarta el anterior
- **WHEN** se confirma un cambio de inicio de un turno que tiene un recordatorio para el horario anterior
- **THEN** se registra y envía un correo `REPROGRAMACION` con el nuevo inicio y el recordatorio anterior queda descartado

#### Scenario: Un cambio sin email descarta el recordatorio sin enviar avisos
- **WHEN** se confirma la cancelación o el cambio de inicio de un turno de un paciente sin email
- **THEN** la operación termina correctamente, se descarta el recordatorio obsoleto y no se registra ni envía el aviso

#### Scenario: Una cancelación por token descarta el recordatorio sin enviar aviso
- **WHEN** el paciente cancela su turno por token
- **THEN** se descarta el recordatorio y no se registra ni envía el aviso de cancelación

#### Scenario: Una operación revertida no envía avisos ni descarta recordatorios
- **WHEN** la transacción de agenda publica el evento y después se revierte
- **THEN** no se registra ni envía el aviso y el recordatorio conserva su programación

#### Scenario: Un aviso fallido se reintenta con la misma fecha
- **WHEN** falla el primer envío y vence el plazo de reintento
- **THEN** la notificación se reintenta con la misma fecha de referencia y se registra el resultado

### Requirement: Vigencia del recordatorio tras cambiar un turno

El sistema SHALL descartar los recordatorios programados que pierdan vigencia por cancelación o cambio de inicio, conservar los ya enviados y permitir que el planificador genere el recordatorio del nuevo inicio (RN-30).

#### Scenario: Una reprogramación con el mismo inicio conserva su recordatorio
- **WHEN** se reprograma un turno sin modificar su inicio
- **THEN** el recordatorio para ese inicio conserva su programación

#### Scenario: Volver al inicio anterior permite retomar su recordatorio descartado
- **WHEN** el turno vuelve al inicio original y su recordatorio de ese inicio había sido descartado antes de enviarse
- **THEN** el próximo ciclo puede enviar el recordatorio conservando el mismo registro

# Spec Delta

## ADDED Requirements

### Requirement: Rechazo de bloqueo superpuesto con turnos activos

`POST /api/profesionales/{id}/bloqueos` MUST rechazar un bloqueo cuyo intervalo `[inicio, fin)` se superponga con turnos `PROGRAMADO` o `CONFIRMADO` del profesional (RN-17, RN-02), respondiendo `409` en Problem Details con `codigo` `RN-17` y `turnos` ordenados por `inicio` e `id` (`id`, `inicio`, `fin`, `estado` en UTC, sin datos del paciente, RN-35) sin persistir el bloqueo. Un turno contiguo o en estado `CANCELADO`, `ATENDIDO` o `AUSENTE` MUST NOT impedir el alta.

#### Scenario: Crear un bloqueo superpuesto con turnos activos devuelve 409 con la lista de conflictos
- **GIVEN** un profesional con un turno `PROGRAMADO` de 10:00 a 10:30 y otro `CONFIRMADO` de 10:30 a 11:00
- **WHEN** `ADMIN` o `RECEPCION` intenta crear un bloqueo para ese profesional de 10:15 a 10:45 con CSRF válido
- **THEN** recibe `409` en Problem Details con `codigo` igual a `RN-17` y `turnos` con ambos turnos ordenados por `inicio` e `id`
- **AND** el bloqueo no queda registrado

#### Scenario: Crear un bloqueo contiguo a turnos activos se acepta
- **GIVEN** un profesional con un turno `PROGRAMADO` de 09:30 a 10:00 y otro `CONFIRMADO` de 11:00 a 11:30
- **WHEN** `ADMIN` o `RECEPCION` crea un bloqueo para ese profesional de 10:00 a 11:00 con CSRF válido
- **THEN** recibe `201` con el bloqueo creado

#### Scenario: Cancelar el turno activo permite crear el bloqueo antes rechazado
- **GIVEN** un profesional con un turno `PROGRAMADO` de 10:00 a 10:30 que impidió crear un bloqueo en ese horario
- **WHEN** el turno se cancela y `ADMIN` o `RECEPCION` vuelve a enviar el alta del bloqueo de 10:00 a 10:30
- **THEN** el alta del bloqueo responde `201`

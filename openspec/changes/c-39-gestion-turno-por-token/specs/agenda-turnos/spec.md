# Spec Delta

## MODIFIED Requirements

### Requirement: Solo se cancelan turnos vigentes que no empezaron

En la cancelación por el personal, el sistema MUST rechazar un turno en estado final (`CANCELADO`, `ATENDIDO` o `AUSENTE`) con `409 TURNO_TRANSICION_INVALIDA` y un turno cuyo `inicio` es igual o anterior al momento actual con `422 RN-05`, con prioridad para el rechazo de estado final. En la cancelación pública, el sistema MUST aplicar primero la vigencia de `token-turno`; un token no válido responde `404`, y una transición inválida con token vigente responde `409 TURNO_TRANSICION_INVALIDA`. En todos los rechazos el turno no cambia.

#### Scenario: Un turno ya cancelado no se vuelve a cancelar
- **GIVEN** un turno `CANCELADO`
- **WHEN** recepción lo cancela
- **THEN** la respuesta es `409` con el código `TURNO_TRANSICION_INVALIDA` y los datos de la primera cancelación no cambian

#### Scenario: Un turno atendido no se puede cancelar
- **GIVEN** un turno `ATENDIDO`
- **WHEN** recepción lo cancela
- **THEN** la respuesta es `409` con el código `TURNO_TRANSICION_INVALIDA`

#### Scenario: Un turno que ya empezó no se puede cancelar
- **GIVEN** un turno `PROGRAMADO` cuyo inicio es exactamente el momento actual
- **WHEN** recepción lo cancela
- **THEN** la respuesta es `422` con el código `RN-05` y el turno sigue `PROGRAMADO`

### Requirement: Cancelaciones simultáneas del mismo turno

Si dos cancelaciones del mismo turno llegan a la vez, el sistema MUST aplicar solo una y el hueco liberado MUST publicarse una sola vez. La segunda solicitud MUST recibir `409` si llega por `POST /api/turnos/{id}/cancelar` y supera sus controles de acceso; si llega por la ruta pública, MUST recibir el `404` uniforme porque el token ya no es válido. Un conflicto de versión al modificar un turno MUST responder `409`, nunca `500`.

#### Scenario: Dos cancelaciones simultáneas del mismo turno
- **GIVEN** un turno `PROGRAMADO`
- **WHEN** dos usuarios de recepción lo cancelan al mismo tiempo
- **THEN** una respuesta es `200`, la otra es `409` y se publica un solo aviso de hueco liberado

### Requirement: Solo el personal autorizado cancela turnos

En `POST /api/turnos/{id}/cancelar`, `ODONTOLOGO` MUST recibir `403`, sin validar el cuerpo ni modificar el turno (RN-20, RN-33). Sin sesión MUST responder `401`. Sin un token CSRF válido MUST responder `403`. Un token de turno MUST NOT autorizar el acceso a esta ruta interna; el paciente utiliza únicamente la operación pública definida en `token-turno`.

#### Scenario: El odontólogo no puede cancelar turnos
- **GIVEN** un usuario `ODONTOLOGO` con sesión y un turno `PROGRAMADO`
- **WHEN** intenta cancelarlo por la ruta interna
- **THEN** la respuesta es `403` y el turno sigue `PROGRAMADO`

#### Scenario: Sin sesión no se puede cancelar un turno
- **WHEN** se cancela un turno por la ruta interna sin sesión, incluso presentando un token de turno
- **THEN** la respuesta es `401`

#### Scenario: Sin CSRF válido no se puede cancelar un turno
- **GIVEN** un usuario de recepción con sesión
- **WHEN** cancela un turno por la ruta interna con un token CSRF incorrecto
- **THEN** la respuesta es `403` y el turno sigue `PROGRAMADO`

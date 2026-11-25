## ADDED Requirements

### Requirement: Cancelación de un turno por el personal

`ADMIN` y `RECEPCION` SHALL poder cancelar un turno `PROGRAMADO` o `CONFIRMADO` con `POST /api/turnos/{id}/cancelar`, enviando un `motivo` opcional de hasta 255 caracteres. El turno MUST quedar `CANCELADO` y la respuesta MUST ser `200` con el turno cancelado, incluidos los datos de la cancelación.

#### Scenario: Recepción cancela un turno programado
- **GIVEN** un turno `PROGRAMADO` que empieza dentro de 72 horas
- **WHEN** recepción lo cancela con el motivo "El paciente avisó que no puede venir"
- **THEN** la respuesta es `200`, el turno queda `CANCELADO` con ese motivo y la base guarda el mismo estado

#### Scenario: Se puede cancelar un turno confirmado
- **GIVEN** un turno `CONFIRMADO` que todavía no empezó
- **WHEN** el administrador lo cancela sin enviar cuerpo
- **THEN** la respuesta es `200`, el turno queda `CANCELADO` y el motivo queda vacío

#### Scenario: Un motivo demasiado largo se rechaza
- **GIVEN** un turno `PROGRAMADO`
- **WHEN** recepción lo cancela con un motivo de 256 caracteres
- **THEN** la respuesta es `400` con el campo `motivo` y el turno sigue `PROGRAMADO`

#### Scenario: Un turno inexistente no se puede cancelar
- **WHEN** recepción cancela un `id` que no corresponde a ningún turno
- **THEN** la respuesta es `404` con el código `TURNO_INEXISTENTE`

### Requirement: Solo se cancelan turnos vigentes que no empezaron

El sistema MUST rechazar la cancelación de un turno en estado final (`CANCELADO`, `ATENDIDO` o `AUSENTE`) con `409 TURNO_TRANSICION_INVALIDA`. MUST rechazar la cancelación de un turno cuyo `inicio` es igual o anterior al momento actual con `422 RN-05`. En ambos casos el turno no cambia.

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

### Requirement: Cancelación tardía según el plazo configurado

El sistema SHALL marcar la cancelación como tardía cuando el tiempo que falta hasta el `inicio` es menor que el plazo `cancelacion.plazo_horas`, calculado con el reloj del sistema en `America/Argentina/Buenos_Aires`. El límite MUST ser inclusivo: a plazo exacto no es tardía. La cancelación tardía MUST permitirse y solo registrarse.

#### Scenario: Cancelar a 24 horas exactas no es tardío
- **GIVEN** un plazo de 24 horas y un turno el miércoles a las 10:00
- **WHEN** se cancela el martes a las 10:00
- **THEN** la cancelación no queda marcada como tardía

#### Scenario: Cancelar un minuto después del plazo es tardío
- **GIVEN** un plazo de 24 horas y un turno el miércoles a las 10:00
- **WHEN** se cancela el martes a las 10:01
- **THEN** la cancelación queda marcada como tardía y el turno queda `CANCELADO` igual

#### Scenario: Un cambio del plazo no altera cancelaciones anteriores
- **GIVEN** una cancelación registrada como no tardía con un plazo de 24 horas
- **WHEN** el administrador cambia el plazo a 72 horas
- **THEN** esa cancelación sigue registrada como no tardía

### Requirement: Registro de quién y cuándo cancela

Toda cancelación MUST registrar el momento de la cancelación según el reloj del sistema, el tipo de actor (`USUARIO` o `PACIENTE_TOKEN`), el usuario del personal que canceló cuando el actor es `USUARIO`, el motivo opcional y la marca de tardía (RN-19).

#### Scenario: La cancelación guarda quién la hizo y cuándo
- **GIVEN** un turno `PROGRAMADO` y un usuario de recepción con sesión
- **WHEN** ese usuario lo cancela
- **THEN** el turno guarda el momento actual como fecha de cancelación, el actor `USUARIO` y el id de ese usuario

### Requirement: Cancelar libera el intervalo

Un turno `CANCELADO` MUST dejar de ocupar su intervalo para el profesional y para el sillón (RN-06, RN-10), sea o no tardía la cancelación. Inmediatamente después de cancelar, el sistema SHALL permitir crear otro turno en el mismo intervalo.

#### Scenario: Tras cancelar se puede dar otro turno en el mismo horario
- **GIVEN** un turno `PROGRAMADO` del profesional A en el sillón 1 de 10:00 a 10:30
- **WHEN** recepción lo cancela y después crea otro turno para el profesional A en el sillón 1 de 10:00 a 10:30
- **THEN** la cancelación responde `200` y la creación responde `201`

#### Scenario: Una cancelación tardía también libera el horario
- **GIVEN** un turno `PROGRAMADO` que empieza dentro de 2 horas, con un plazo de 24 horas
- **WHEN** recepción lo cancela y después crea otro turno en el mismo intervalo
- **THEN** la cancelación queda tardía y la creación responde `201`

### Requirement: Aviso de hueco liberado

Cada cancelación exitosa MUST publicar exactamente un aviso de turno cancelado y exactamente un aviso de hueco liberado, con el profesional, el sillón, el intervalo y el turno de origen, para que la lista de espera pueda ofrecer el hueco (RN-06, RN-23). Una cancelación rechazada MUST NOT publicar avisos.

#### Scenario: Una cancelación publica un único hueco liberado
- **GIVEN** un turno `PROGRAMADO`
- **WHEN** recepción lo cancela
- **THEN** se publica un aviso de turno cancelado y un aviso de hueco liberado con el profesional, el sillón y el intervalo del turno

#### Scenario: Una cancelación rechazada no publica avisos
- **GIVEN** un turno `CANCELADO`
- **WHEN** recepción intenta cancelarlo otra vez
- **THEN** la respuesta es `409` y no se publica ningún aviso

### Requirement: Cancelaciones simultáneas del mismo turno

Si dos cancelaciones del mismo turno llegan a la vez, el sistema MUST aplicar solo una. La otra MUST responder `409` y el hueco liberado MUST publicarse una sola vez. Un conflicto de versión al modificar un turno MUST responder `409`, nunca `500`.

#### Scenario: Dos cancelaciones simultáneas del mismo turno
- **GIVEN** un turno `PROGRAMADO`
- **WHEN** dos usuarios de recepción lo cancelan al mismo tiempo
- **THEN** una respuesta es `200`, la otra es `409` y se publica un solo aviso de hueco liberado

### Requirement: Solo el personal autorizado cancela turnos

`ODONTOLOGO` MUST recibir `403` al cancelar un turno, sin validar el cuerpo ni modificar el turno (RN-20, RN-33). Sin sesión MUST responder `401`. Sin un token CSRF válido MUST responder `403`.

#### Scenario: El odontólogo no puede cancelar turnos
- **GIVEN** un usuario `ODONTOLOGO` con sesión y un turno `PROGRAMADO`
- **WHEN** intenta cancelarlo
- **THEN** la respuesta es `403` y el turno sigue `PROGRAMADO`

#### Scenario: Sin sesión no se puede cancelar un turno
- **WHEN** se cancela un turno sin sesión
- **THEN** la respuesta es `401`

#### Scenario: Sin CSRF válido no se puede cancelar un turno
- **GIVEN** un usuario de recepción con sesión
- **WHEN** cancela un turno con un token CSRF incorrecto
- **THEN** la respuesta es `403` y el turno sigue `PROGRAMADO`

## ADDED Requirements

### Requirement: Registro de asistencia o ausencia por el personal

`ADMIN` y `RECEPCION` SHALL poder marcar un turno como `ATENDIDO` o `AUSENTE` con `POST /api/turnos/{id}/asistencia`, enviando `{ "estado": "ATENDIDO" | "AUSENTE" }`. La marca MUST aceptarse solo sobre un turno `PROGRAMADO` o `CONFIRMADO` y solo cuando el momento actual es igual o posterior al `inicio` del turno (RN-20). La respuesta MUST ser `200` con el turno actualizado. Un estado destino ausente o distinto de `ATENDIDO` y `AUSENTE` MUST rechazarse con `400`.

#### Scenario: Recepción marca un turno como atendido
- **GIVEN** un turno `PROGRAMADO` cuyo inicio fue hace una hora
- **WHEN** recepción envía el estado `ATENDIDO`
- **THEN** la respuesta es `200`, el turno queda `ATENDIDO` y la base guarda el mismo estado

#### Scenario: Recepción marca la ausencia de un turno confirmado
- **GIVEN** un turno `CONFIRMADO` cuyo inicio fue hace una hora
- **WHEN** recepción envía el estado `AUSENTE`
- **THEN** la respuesta es `200` y el turno queda `AUSENTE`

#### Scenario: A la hora exacta de inicio ya se puede marcar
- **GIVEN** un turno `PROGRAMADO` cuyo inicio es exactamente el momento actual
- **WHEN** recepción envía el estado `ATENDIDO`
- **THEN** la respuesta es `200` y el turno queda `ATENDIDO`

#### Scenario: Antes del inicio no se puede marcar
- **GIVEN** un turno `PROGRAMADO` que empieza dentro de un minuto
- **WHEN** recepción envía el estado `AUSENTE`
- **THEN** la respuesta es `422` con el código `RN-20` y el turno sigue `PROGRAMADO`

#### Scenario: Un estado destino inválido se rechaza
- **GIVEN** un turno `PROGRAMADO` que ya empezó
- **WHEN** recepción envía el estado `CANCELADO`, o no envía estado
- **THEN** la respuesta es `400` con el campo `estado` y el turno sigue `PROGRAMADO`

#### Scenario: Un turno inexistente no admite asistencia
- **WHEN** recepción marca la asistencia de un `id` que no corresponde a ningún turno
- **THEN** la respuesta es `404` con el código `TURNO_INEXISTENTE`

### Requirement: Un turno cancelado no admite asistencia

El sistema MUST rechazar con `409 TURNO_TRANSICION_INVALIDA` cualquier marca de asistencia sobre un turno `CANCELADO`, sin importar el rol ni si el turno ya empezó. El turno no cambia.

#### Scenario: Un turno cancelado no se marca
- **GIVEN** un turno `CANCELADO` cuyo inicio ya pasó
- **WHEN** el administrador envía el estado `ATENDIDO`
- **THEN** la respuesta es `409` con el código `TURNO_TRANSICION_INVALIDA` y el turno sigue `CANCELADO`

### Requirement: Corrección de la asistencia solo por el administrador

Sobre un turno ya `ATENDIDO` o `AUSENTE`, solo `ADMIN` SHALL poder corregir la marca, pasándolo al otro estado con el mismo endpoint. `RECEPCION` MUST recibir `403` y el turno no cambia. Marcar el mismo estado que el turno ya tiene MUST rechazarse con `409 TURNO_TRANSICION_INVALIDA`. La corrección no exige que el turno haya empezado, porque una marca previa ya lo implica.

#### Scenario: El administrador corrige una marca
- **GIVEN** un turno `AUSENTE` marcado por error
- **WHEN** el administrador envía el estado `ATENDIDO`
- **THEN** la respuesta es `200` y el turno queda `ATENDIDO`

#### Scenario: Recepción no puede corregir una marca
- **GIVEN** un turno `ATENDIDO`
- **WHEN** recepción envía el estado `AUSENTE`
- **THEN** la respuesta es `403` y el turno sigue `ATENDIDO`

#### Scenario: Repetir la misma marca se rechaza
- **GIVEN** un turno `ATENDIDO`
- **WHEN** el administrador envía el estado `ATENDIDO`
- **THEN** la respuesta es `409` con el código `TURNO_TRANSICION_INVALIDA`

#### Scenario: La corrección no cambia lo demás
- **GIVEN** un turno `AUSENTE` con notas y recursos
- **WHEN** el administrador lo corrige a `ATENDIDO`
- **THEN** el intervalo, el paciente, el profesional, el sillón, la prestación y las notas quedan iguales

### Requirement: Solo el personal autorizado marca asistencia

`ODONTOLOGO` MUST recibir `403` al intentar marcar o corregir asistencia, y una petición sin sesión o con una cabecera CSRF que no coincide con la cookie MUST rechazarse; en todos los casos antes de validar el cuerpo y sin cambiar el turno (RN-20, RN-33).

#### Scenario: El odontólogo no puede marcar asistencia
- **GIVEN** un turno `PROGRAMADO` que ya empezó
- **WHEN** un usuario `ODONTOLOGO` envía el estado `ATENDIDO`
- **THEN** la respuesta es `403` y el turno sigue `PROGRAMADO`

#### Scenario: Sin sesión no se puede marcar asistencia
- **WHEN** una petición sin sesión envía `POST /api/turnos/{id}/asistencia`
- **THEN** la respuesta es `401`

#### Scenario: Sin CSRF válido no se puede marcar asistencia
- **WHEN** `RECEPCION` envía `POST /api/turnos/{id}/asistencia` con una cabecera CSRF que no coincide con la cookie
- **THEN** la respuesta es `403`

## MODIFIED Requirements

### Requirement: Estados del turno y sus transiciones

Un turno MUST estar en uno de estos estados: `PROGRAMADO`, `CONFIRMADO`, `ATENDIDO`, `AUSENTE` o `CANCELADO`. Las únicas transiciones permitidas MUST ser `PROGRAMADO → CONFIRMADO` y `PROGRAMADO|CONFIRMADO → CANCELADO|ATENDIDO|AUSENTE`. `CANCELADO`, `ATENDIDO` y `AUSENTE` MUST ser finales. Una transición no permitida MUST rechazarse con `409` y código `TURNO_TRANSICION_INVALIDA` (RN-12). La única excepción es la corrección de la asistencia por `ADMIN` (`ATENDIDO ↔ AUSENTE`), que se hace solo con la operación de asistencia y no habilita ningún otro cambio desde un estado final.

#### Scenario: Transiciones permitidas desde un turno programado
- **WHEN** un turno `PROGRAMADO` pasa a `CONFIRMADO`, `CANCELADO`, `ATENDIDO` o `AUSENTE`
- **THEN** la transición se acepta

#### Scenario: Un estado final no cambia
- **WHEN** un turno `CANCELADO`, `ATENDIDO` o `AUSENTE` intenta pasar a cualquier otro estado
- **THEN** la transición se rechaza con código `TURNO_TRANSICION_INVALIDA`

#### Scenario: La corrección de asistencia no abre otras transiciones
- **GIVEN** un turno `ATENDIDO` corregido a `AUSENTE` por el administrador
- **WHEN** alguien intenta cancelarlo o volverlo a `CONFIRMADO`
- **THEN** la operación se rechaza con `409` y código `TURNO_TRANSICION_INVALIDA`

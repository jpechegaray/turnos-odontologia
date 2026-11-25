# agenda-turnos Specification

## Purpose
Permitir que el personal cree turnos sin sobreturnos: ningún profesional ni sillón puede tener dos turnos superpuestos, ni siquiera con pedidos simultáneos. Además, cada turno respeta los recursos activos, el horario de atención, los bloqueos y la granularidad de la agenda (US-010).

## Requirements

### Requirement: Alta de un turno por el personal

`ADMIN` y `RECEPCION` SHALL poder crear un turno con `POST /api/turnos`, enviando `pacienteId`, `profesionalId`, `sillonId`, `prestacionId` e `inicio` (instante ISO-8601 con zona). Las `notas` son opcionales, con un máximo de 1000 caracteres. El turno nuevo MUST quedar `PROGRAMADO`, con canal `INTERNO` y el usuario que lo creó registrado. La respuesta MUST ser `201` con la representación del turno.

#### Scenario: Recepción crea un turno válido
- **WHEN** `RECEPCION` crea un turno el lunes de 10:00 a 10:30, hora de Buenos Aires, para un profesional que atiende de 09:00 a 13:00, con recursos activos y sin otros turnos
- **THEN** la respuesta es `201` con estado `PROGRAMADO`, canal `INTERNO`, `fin` a las 10:30 y `duracionMin` 30
- **AND** el turno queda guardado con el usuario que lo creó

#### Scenario: Datos obligatorios o con formato inválido
- **WHEN** falta alguno de `pacienteId`, `profesionalId`, `sillonId`, `prestacionId` o `inicio`, el `inicio` no tiene un formato válido o las `notas` superan los 1000 caracteres
- **THEN** la respuesta es `400` con el campo inválido
- **AND** no se guarda ningún turno

### Requirement: Duración del turno

La duración del turno SHALL ser `duracionMin` si se envía y, si no, la duración por defecto de la prestación (RN-03). El `fin` del turno MUST ser `inicio + duración`, y la duración guardada MUST ser la efectiva (RN-09).

#### Scenario: Sin duración se usa la de la prestación
- **WHEN** se crea un turno sin `duracionMin` para una prestación de 45 minutos por defecto
- **THEN** el turno se crea con `duracionMin` 45 y `fin` 45 minutos después del inicio

#### Scenario: La duración se puede ajustar
- **WHEN** se crea un turno con `duracionMin` 60 para una prestación de 30 minutos por defecto
- **THEN** el turno se crea con `duracionMin` 60 y `fin` 60 minutos después del inicio

### Requirement: Recursos existentes y activos

Paciente, profesional, sillón y prestación MUST existir y estar activos (RN-11). Un recurso inexistente MUST responder `422` con `PACIENTE_INEXISTENTE`, `PROFESIONAL_INEXISTENTE`, `SILLON_INEXISTENTE` o `PRESTACION_INEXISTENTE`. Uno inactivo MUST responder `422` con `PACIENTE_INACTIVO`, `PROFESIONAL_INACTIVO`, `SILLON_INACTIVO` o `PRESTACION_INACTIVA`.

#### Scenario: Un recurso inexistente se rechaza
- **WHEN** se crea un turno con un `sillonId` que no existe
- **THEN** la respuesta es `422` con código `SILLON_INEXISTENTE`
- **AND** no se guarda ningún turno

#### Scenario: Un recurso inactivo se rechaza
- **WHEN** se crea un turno para un profesional inactivo
- **THEN** la respuesta es `422` con código `PROFESIONAL_INACTIVO`

### Requirement: El profesional realiza la prestación

El profesional MUST realizar la prestación del turno (RN-11). Si no la realiza, la respuesta MUST ser `422` con código `RN-11`. Ninguna prestación exige un tipo de sillón: sirve cualquier sillón activo.

#### Scenario: El profesional no realiza la prestación
- **WHEN** se crea un turno para una prestación que el profesional no tiene asignada
- **THEN** la respuesta es `422` con código `RN-11`

### Requirement: Inicio futuro y granularidad de cinco minutos

El `inicio` MUST ser posterior al momento actual (RN-13). El `inicio` MUST caer en un minuto múltiplo de 5, con segundos en cero. La duración efectiva MUST ser un múltiplo de 5 entre 5 y 480 minutos (RN-09). Si no se cumple, la respuesta MUST ser `422` con el código `RN-13` o `RN-09`, según corresponda.

#### Scenario: Un turno en el pasado se rechaza
- **WHEN** se crea un turno cuyo `inicio` es anterior o igual al momento actual
- **THEN** la respuesta es `422` con código `RN-13`

#### Scenario: Un inicio fuera de la grilla de cinco minutos se rechaza
- **WHEN** se crea un turno que empieza a las 10:03
- **THEN** la respuesta es `422` con código `RN-09`

#### Scenario: Una duración fuera de rango se rechaza
- **WHEN** se crea un turno con `duracionMin` 7 o con `duracionMin` 485
- **THEN** la respuesta es `422` con código `RN-09`

### Requirement: Dentro del horario de atención y fuera de bloqueos

El turno MUST quedar completo dentro de un único tramo del horario de atención del profesional, evaluado en hora de Buenos Aires (RN-04, RN-14, RN-16). El turno MUST NOT superponerse con un bloqueo del profesional; un bloqueo que termina justo cuando empieza el turno no lo afecta (RN-02).

Si cae fuera de horario, la respuesta MUST ser `422` con código `RN-04` y `motivo` `FUERA_DE_HORARIO`. Si cae en un bloqueo, MUST ser `422` con código `RN-04` y `motivo` `BLOQUEO`.

#### Scenario: Un turno que sale del horario se rechaza
- **WHEN** el profesional atiende el lunes de 09:00 a 13:00 y se crea un turno ese lunes de 12:45 a 13:15
- **THEN** la respuesta es `422` con código `RN-04` y `motivo` `FUERA_DE_HORARIO`

#### Scenario: Un turno que termina justo al cierre del tramo se acepta
- **WHEN** el profesional atiende el lunes de 09:00 a 13:00 y se crea un turno ese lunes de 12:30 a 13:00
- **THEN** la respuesta es `201`

#### Scenario: Un turno que cruza el corte entre dos tramos se rechaza
- **WHEN** el profesional atiende el lunes de 09:00 a 12:00 y de 14:00 a 18:00, y se crea un turno ese lunes de 11:30 a 14:30
- **THEN** la respuesta es `422` con código `RN-04` y `motivo` `FUERA_DE_HORARIO`

#### Scenario: Un turno dentro de un bloqueo se rechaza
- **WHEN** el profesional tiene un bloqueo de 10:00 a 11:00 y se crea un turno de 10:30 a 11:30
- **THEN** la respuesta es `422` con código `RN-04` y `motivo` `BLOQUEO`

#### Scenario: Un turno que empieza cuando termina un bloqueo se acepta
- **WHEN** el profesional tiene un bloqueo de 10:00 a 11:00 y se crea un turno de 11:00 a 11:30
- **THEN** la respuesta es `201`

### Requirement: Sin solapamiento por profesional ni por sillón

Un turno MUST NOT superponerse con otro turno del mismo profesional ni del mismo sillón (RN-01). Los intervalos son `[inicio, fin)`: empezar justo cuando termina otro no es solapamiento (RN-02). Un solapamiento MUST responder `409` con código `RN-01` y `recurso` `PROFESIONAL` o `SILLON`; si chocan los dos ejes, se informa `PROFESIONAL`.

#### Scenario: Un turno solapado por profesional se rechaza
- **WHEN** un profesional tiene un turno de 10:00 a 10:30 en el sillón 1 y se crea otro para el mismo profesional de 10:15 a 10:45 en el sillón 2
- **THEN** la respuesta es `409` con código `RN-01` y `recurso` `PROFESIONAL`
- **AND** no se guarda el segundo turno

#### Scenario: Un turno solapado por sillón se rechaza
- **WHEN** el sillón 1 tiene un turno de 10:00 a 10:30 y se crea otro en el sillón 1 de 10:15 a 10:45 con otro profesional
- **THEN** la respuesta es `409` con código `RN-01` y `recurso` `SILLON`

#### Scenario: Un turno que empieza justo cuando termina otro no se solapa
- **WHEN** existe un turno de 10:00 a 10:30 y se crea otro para el mismo profesional y el mismo sillón de 10:30 a 11:00
- **THEN** la respuesta es `201`

#### Scenario: Un turno que empieza un minuto antes del fin de otro se solapa
- **WHEN** existe un turno de 10:00 a 10:30 y se crea otro para el mismo profesional que empieza a las 10:25
- **THEN** la respuesta es `409` con código `RN-01`

### Requirement: Qué turnos ocupan lugar

Los turnos `CANCELADO` MUST NOT ocupar lugar en la agenda. Los turnos `PROGRAMADO`, `CONFIRMADO`, `ATENDIDO` y `AUSENTE` MUST ocuparlo (RN-10).

#### Scenario: Un turno atendido sigue ocupando lugar
- **WHEN** existe un turno `ATENDIDO` de 10:00 a 10:30 y se crea otro para el mismo profesional de 10:00 a 10:30
- **THEN** la respuesta es `409` con código `RN-01`

#### Scenario: Un turno cancelado no ocupa lugar
- **WHEN** existe un turno `CANCELADO` de 10:00 a 10:30 y se crea otro para el mismo profesional y el mismo sillón de 10:00 a 10:30
- **THEN** la respuesta es `201`

### Requirement: Garantía de no solapamiento ante pedidos simultáneos

La base de datos MUST rechazar un turno no cancelado que se superponga con otro del mismo profesional o del mismo sillón, aunque las validaciones previas lo hayan aceptado (RN-15). Ese rechazo MUST responder el mismo `409` con código `RN-01` y el `recurso` correspondiente.

#### Scenario: La base rechaza un solapamiento que esquivó la validación
- **WHEN** se intenta guardar directamente un turno que se superpone con otro no cancelado del mismo sillón, sin pasar por la validación previa
- **THEN** la base rechaza la operación
- **AND** la API traduce el rechazo a `409` con código `RN-01` y `recurso` `SILLON`

#### Scenario: Dos creaciones simultáneas sobre el mismo hueco
- **WHEN** dos pedidos crean a la vez un turno para el mismo profesional y el mismo horario
- **THEN** uno recibe `201` y el otro `409` con código `RN-01`
- **AND** en la base queda un solo turno para ese horario

### Requirement: Restricciones de exclusión en la base

La tabla `turno` MUST tener las restricciones de exclusión `turno_sin_solape_profesional` y `turno_sin_solape_sillon`, sobre intervalos `[inicio, fin)` y sin incluir los turnos `CANCELADO`. Crearlas MUST ser idempotente.

#### Scenario: Las restricciones de exclusión existen en la base
- **WHEN** arranca la aplicación sobre PostgreSQL
- **THEN** la tabla `turno` tiene las restricciones `turno_sin_solape_profesional` y `turno_sin_solape_sillon`
- **AND** volver a ejecutar el script de restricciones no falla ni las duplica

### Requirement: Solo el personal autorizado crea turnos

Solo `ADMIN` y `RECEPCION` MUST poder crear turnos. `ODONTOLOGO` MUST recibir `403` (RN-20, RN-33). Sin sesión, la respuesta MUST ser `401`. Sin un token CSRF válido, MUST ser `403`. En ningún caso se guarda un turno.

#### Scenario: El odontólogo no puede crear turnos
- **WHEN** un `ODONTOLOGO` envía `POST /api/turnos` con datos válidos
- **THEN** la respuesta es `403`
- **AND** no se guarda ningún turno

#### Scenario: Sin sesión no se puede crear un turno
- **WHEN** se envía `POST /api/turnos` sin sesión iniciada
- **THEN** la respuesta es `401`

#### Scenario: Sin CSRF válido no se puede crear un turno
- **WHEN** `RECEPCION` envía `POST /api/turnos` con una cabecera CSRF que no coincide con la cookie
- **THEN** la respuesta es `403`

### Requirement: Estados del turno y sus transiciones

Un turno MUST estar en uno de estos estados: `PROGRAMADO`, `CONFIRMADO`, `ATENDIDO`, `AUSENTE` o `CANCELADO`. Las únicas transiciones permitidas MUST ser `PROGRAMADO → CONFIRMADO` y `PROGRAMADO|CONFIRMADO → CANCELADO|ATENDIDO|AUSENTE`. `CANCELADO`, `ATENDIDO` y `AUSENTE` MUST ser finales. Una transición no permitida MUST rechazarse con `409` y código `TURNO_TRANSICION_INVALIDA` (RN-12).

#### Scenario: Transiciones permitidas desde un turno programado
- **WHEN** un turno `PROGRAMADO` pasa a `CONFIRMADO`, `CANCELADO`, `ATENDIDO` o `AUSENTE`
- **THEN** la transición se acepta

#### Scenario: Un estado final no cambia
- **WHEN** un turno `CANCELADO`, `ATENDIDO` o `AUSENTE` intenta pasar a cualquier otro estado
- **THEN** la transición se rechaza con código `TURNO_TRANSICION_INVALIDA`

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

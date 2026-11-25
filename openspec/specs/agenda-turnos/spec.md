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

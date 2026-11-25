## ADDED Requirements

### Requirement: Reprogramación de un turno por el personal

`ADMIN` y `RECEPCION` SHALL poder reprogramar un turno con `POST /api/turnos/{id}/reprogramar`. El cuerpo lleva `inicio` (ISO-8601 con zona) y, como opcionales, `profesionalId`, `sillonId` y `duracionMin`. Si faltan, se conservan los del turno. La respuesta SHALL ser `200` con el **mismo** turno (mismo `id`, paciente, prestación, estado, notas y canal) en su horario nuevo (RN-18).

#### Scenario: Recepción mueve un turno a otro horario
- **GIVEN** un turno `PROGRAMADO` de 30 minutos el lunes a las 10:00, a más de 24 horas
- **WHEN** `RECEPCION` lo reprograma al lunes a las 11:00 con CSRF válido
- **THEN** recibe `200` con el mismo `id`, `inicio` a las 11:00, `fin` a las 11:30, estado `PROGRAMADO` y `reprogramadoTarde` falso

#### Scenario: Se puede cambiar de profesional, de sillón y de duración
- **GIVEN** un turno y otro profesional activo que realiza la prestación, con horario y un sillón libre
- **WHEN** `ADMIN` lo reprograma con el otro profesional, el otro sillón y 45 minutos
- **THEN** recibe `200` con los recursos y la duración nuevos

#### Scenario: Un turno confirmado sigue confirmado
- **GIVEN** un turno `CONFIRMADO`
- **WHEN** recepción lo reprograma
- **THEN** recibe `200` y el turno sigue `CONFIRMADO`

#### Scenario: Reprogramar al mismo horario no cambia nada
- **WHEN** recepción reprograma un turno al mismo inicio, profesional, sillón y duración
- **THEN** recibe `200` con el turno sin cambios, la versión no cambia y no se publica ningún evento

#### Scenario: Un pedido sin inicio se rechaza
- **WHEN** recepción envía un cuerpo sin `inicio`, con una fecha mal formada o con JSON roto
- **THEN** recibe `400` en Problem Details y el turno no cambia

#### Scenario: Un turno inexistente no se puede reprogramar
- **WHEN** recepción reprograma un `id` que no existe
- **THEN** recibe `404` en Problem Details con `TURNO_INEXISTENTE`

### Requirement: Solo se reprograman turnos vigentes que no empezaron

La reprogramación MUST rechazar con `409 TURNO_TRANSICION_INVALIDA` un turno `CANCELADO`, `ATENDIDO` o `AUSENTE` y, en segundo lugar, con `422 RN-05` un turno cuyo inicio original es igual al momento actual o anterior. El turno no cambia.

#### Scenario: Un turno cancelado no se puede reprogramar
- **WHEN** recepción reprograma un turno `CANCELADO`, `ATENDIDO` o `AUSENTE`
- **THEN** recibe `409` en Problem Details con `TURNO_TRANSICION_INVALIDA` y el turno no cambia

#### Scenario: Un turno que ya empezó no se puede reprogramar
- **GIVEN** un turno `PROGRAMADO` cuyo inicio es igual al momento actual
- **WHEN** recepción lo reprograma a un horario futuro libre
- **THEN** recibe `422` en Problem Details con `RN-05` y el turno no cambia

### Requirement: El horario nuevo cumple las reglas del alta excluyendo al propio turno

El horario nuevo SHALL validarse con las mismas reglas que el alta (RN-01 a RN-04, RN-09, RN-11, RN-13), con los mismos códigos y estados, pero **excluyendo al propio turno** de la búsqueda de solapamientos (RN-18). La restricción de exclusión de la base MUST seguir siendo la garantía final.

#### Scenario: Un turno se mueve quince minutos dentro de su propio intervalo
- **GIVEN** un turno de 10:00 a 10:30
- **WHEN** recepción lo reprograma a las 10:15
- **THEN** recibe `200` con el turno de 10:15 a 10:45

#### Scenario: Un horario ocupado por otro turno responde conflicto
- **GIVEN** otro turno del mismo profesional, o del mismo sillón, de 11:00 a 11:30
- **WHEN** recepción reprograma el turno a las 11:15
- **THEN** recibe `409` en Problem Details con `RN-01` y el turno no cambia

#### Scenario: Un horario fuera de atención se rechaza
- **WHEN** recepción reprograma el turno a un horario fuera de la atención del profesional o dentro de un bloqueo
- **THEN** recibe `422` en Problem Details con `RN-04` y el turno no cambia

#### Scenario: El horario nuevo respeta el inicio futuro, la grilla y los recursos activos
- **WHEN** recepción reprograma a un inicio pasado, a un minuto fuera de la grilla de cinco, a un sillón inactivo o a un profesional que no realiza la prestación
- **THEN** recibe `422` en Problem Details con el código de la regla (`RN-13`, `RN-09` o `RN-11`) y el turno no cambia

### Requirement: Reprogramación tardía según el plazo configurado

Si entre el momento actual y el **inicio original** queda menos que `cancelacion.plazo_horas`, la reprogramación SHALL permitirse y marcar `reprogramadoTarde` verdadero (RN-05, SU-07, PA-06). A plazo exacto no es tardía. Una vez marcada, la marca MUST conservarse en las reprogramaciones siguientes.

#### Scenario: Reprogramar dentro del plazo queda marcado como tardío
- **GIVEN** un plazo de 24 horas y un turno que empieza en 10 horas
- **WHEN** recepción lo reprograma a otro día
- **THEN** recibe `200` con `reprogramadoTarde` verdadero

#### Scenario: Reprogramar a 24 horas exactas no es tardío
- **GIVEN** un plazo de 24 horas y un turno que empieza en exactamente 24 horas
- **WHEN** recepción lo reprograma
- **THEN** recibe `200` con `reprogramadoTarde` falso

#### Scenario: La marca de tardía se conserva
- **GIVEN** un turno ya reprogramado tarde
- **WHEN** recepción lo reprograma de nuevo con más de 24 horas de anticipación
- **THEN** `reprogramadoTarde` sigue verdadero

### Requirement: Reprogramar libera el horario anterior

Al reprogramar, la parte del intervalo anterior que el turno deja de ocupar SHALL quedar libre para otro turno. El sistema SHALL publicar un `TurnoReprogramado`, con los recursos y los intervalos anterior y nuevo, y un `HuecoLiberado` por cada tramo del intervalo anterior que quedó libre:

- con el mismo profesional, la diferencia entre los dos intervalos;
- con otro profesional, el intervalo anterior completo.

Una reprogramación rechazada o sin cambios MUST NOT publicar eventos.

#### Scenario: El horario anterior se puede volver a dar
- **GIVEN** un turno de 10:00 a 10:30 reprogramado a las 12:00
- **WHEN** recepción da otro turno al mismo profesional y sillón de 10:00 a 10:30
- **THEN** recibe `201`

#### Scenario: Reprogramar publica el turno reprogramado y el hueco anterior
- **WHEN** recepción reprograma un turno de 10:00 a 10:30 a las 12:00
- **THEN** se publica un `TurnoReprogramado` con los dos intervalos y la marca de tardía, y un `HuecoLiberado` de 10:00 a 10:30 con el profesional y el sillón anteriores

#### Scenario: Moverse dentro del propio intervalo libera solo lo que dejó
- **WHEN** recepción reprograma un turno de 10:00 a 10:30 a las 10:15
- **THEN** se publica un solo `HuecoLiberado`, de 10:00 a 10:15

#### Scenario: Cambiar de profesional libera el intervalo completo
- **WHEN** recepción reprograma un turno de 10:00 a 10:30 al mismo horario con otro profesional
- **THEN** se publica un `HuecoLiberado` de 10:00 a 10:30 del profesional anterior

#### Scenario: Una reprogramación rechazada no publica eventos
- **WHEN** una reprogramación responde `404`, `409` o `422`
- **THEN** no se publica `TurnoReprogramado` ni `HuecoLiberado`

### Requirement: Reprogramaciones simultáneas sobre el mismo horario

Si una reprogramación y un alta, o dos reprogramaciones, apuntan a la vez al mismo horario libre, SHALL ganar una y la otra MUST recibir `409 RN-01` (RN-15).

#### Scenario: Una reprogramación y un alta compiten por el mismo horario
- **WHEN** a la vez recepción reprograma un turno a las 12:00 y otra recepción da un turno nuevo al mismo profesional y sillón a las 12:00
- **THEN** una responde con éxito y la otra `409` con `RN-01`, y en la base queda un solo turno a esa hora

### Requirement: Solo el personal autorizado reprograma turnos

La ruta SHALL requerir sesión del personal y CSRF válido. `ODONTOLOGO` MUST recibir `403`, aun con un cuerpo inválido (RN-20, RN-33). El paciente no reprograma con su token (RN-18).

#### Scenario: El odontólogo no puede reprogramar turnos
- **WHEN** un odontólogo reprograma un turno con CSRF válido, con un cuerpo válido o inválido
- **THEN** recibe `403` en Problem Details y el turno no cambia

#### Scenario: Sin sesión no se puede reprogramar un turno
- **WHEN** un cliente sin sesión reprograma un turno
- **THEN** recibe `401` en Problem Details y el turno no cambia

#### Scenario: Sin CSRF válido no se puede reprogramar un turno
- **WHEN** recepción reprograma un turno sin CSRF válido
- **THEN** recibe `403` en Problem Details y el turno no cambia

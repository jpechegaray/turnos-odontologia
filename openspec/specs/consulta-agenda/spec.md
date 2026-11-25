# consulta-agenda Specification

## Purpose
Permite que el personal consulte la agenda diaria y semanal del consultorio, con turnos, horarios de atención y bloqueos en hora de Buenos Aires, filtrada por profesional y por sillón, mostrando los datos del paciente según el rol (US-013, RN-14, RN-33).

## Requirements

### Requirement: Consulta de la agenda por día o por semana

`ADMIN`, `RECEPCION` y `ODONTOLOGO` SHALL consultar la agenda con `GET /api/agenda`. Reciben `vista` (`dia` o `semana`, por defecto `dia`) y `fecha` (`AAAA-MM-DD`, por defecto hoy en Buenos Aires). La respuesta MUST incluir `vista`, `desde` y `hasta` (fechas locales inclusive), y las listas `turnos`, `horarios` y `bloqueos` del período.

#### Scenario: La vista diaria devuelve turnos, horarios y bloqueos del día
- **GIVEN** un profesional con un tramo de atención los lunes de 09:00 a 13:00, un turno el lunes 2026-12-07 a las 10:00 y un bloqueo ese lunes de 12:00 a 13:00 en Buenos Aires
- **WHEN** `ADMIN` o `RECEPCION` consulta `GET /api/agenda?vista=dia&fecha=2026-12-07`
- **THEN** recibe `200` con `desde` y `hasta` 2026-12-07, el turno con su estado, el horario de 09:00 a 13:00 de esa fecha y el bloqueo

#### Scenario: La vista semanal va de lunes a domingo
- **GIVEN** turnos el lunes 2026-12-07, el domingo 2026-12-13 y el lunes 2026-12-14 en Buenos Aires
- **WHEN** un usuario autorizado consulta `GET /api/agenda?vista=semana&fecha=2026-12-10`
- **THEN** `desde` es 2026-12-07, `hasta` es 2026-12-13, y la respuesta incluye los turnos del lunes 7 y el domingo 13 pero no el del lunes 14

#### Scenario: Sin parámetros se consulta el día de hoy
- **GIVEN** que hoy es 2026-12-01 en Buenos Aires y hay un turno ese día
- **WHEN** un usuario autorizado consulta `GET /api/agenda` sin parámetros
- **THEN** recibe la vista `dia` con `desde` y `hasta` 2026-12-01 y ese turno

#### Scenario: Parámetros inválidos responden 400
- **WHEN** un usuario autorizado consulta con `vista=mes` o con una `fecha` que no respeta `AAAA-MM-DD`
- **THEN** recibe `400` en Problem Details

### Requirement: Hora de Buenos Aires y bordes del período

El período SHALL calcularse en `America/Argentina/Buenos_Aires`: un día abarca `[00:00, 24:00)` hora local. Los instantes de turnos, horarios y bloqueos MUST expresarse en ISO-8601 con el desfase de Buenos Aires (RN-14). Un turno o bloqueo MUST aparecer si comparte algún instante con el período.

#### Scenario: Un turno a las 23:30 hora local pertenece a ese día
- **GIVEN** un turno el 2026-12-07 de 23:30 a 23:55 en Buenos Aires (02:30 UTC del 2026-12-08)
- **WHEN** un usuario autorizado consulta la vista diaria del 2026-12-07 y la del 2026-12-08
- **THEN** el turno aparece en la del 7, con `inicio` `2026-12-07T23:30:00-03:00`, y no aparece en la del 8

#### Scenario: Un turno a las 00:00 hora local pertenece al día que empieza
- **GIVEN** un turno el 2026-12-08 de 00:00 a 00:30 en Buenos Aires
- **WHEN** un usuario autorizado consulta la vista diaria del 2026-12-07 y la del 2026-12-08
- **THEN** el turno aparece solo en la del 8

#### Scenario: Un bloqueo de varios días aparece en cada día que toca
- **GIVEN** un bloqueo del 2026-12-07 a las 15:00 al 2026-12-09 a las 10:00 en Buenos Aires
- **WHEN** un usuario autorizado consulta la vista diaria del 2026-12-08
- **THEN** el bloqueo aparece con su `inicio` y `fin` completos

### Requirement: Contenido de turnos, horarios y bloqueos

Cada turno SHALL incluir `id`, `inicio`, `fin`, `duracionMin`, `estado`, `profesionalId`, `sillonId`, `prestacionId`, `pacienteId`, `paciente` (nombre y apellido, o nulo) y `notas`. Se incluyen turnos de cualquier estado, ordenados por `inicio` y luego por `id`. Cada horario SHALL incluir `profesionalId`, `fecha`, `inicio` y `fin` de cada fecha del período en que el profesional atiende. Cada bloqueo SHALL incluir `id`, `profesionalId`, `inicio`, `fin` y `motivo` (o nulo).

#### Scenario: Los turnos de cualquier estado se muestran con su estado
- **GIVEN** un turno `PROGRAMADO` y otro `CANCELADO` el mismo día
- **WHEN** un usuario autorizado consulta la vista diaria
- **THEN** los dos aparecen, cada uno con su `estado`, ordenados por `inicio`

#### Scenario: Un horario semanal se proyecta en cada fecha del período
- **GIVEN** un profesional que atiende los lunes y miércoles de 09:00 a 13:00
- **WHEN** un usuario autorizado consulta la vista semanal del 2026-12-07
- **THEN** `horarios` incluye ese tramo para el 2026-12-07 y el 2026-12-09, con `inicio` `2026-12-07T09:00:00-03:00` y `fin` `2026-12-07T13:00:00-03:00` en el primero

### Requirement: Filtros por profesional y por sillón

La agenda SHALL aceptar los filtros opcionales y combinables `profesionalId` y `sillonId`. `profesionalId` limita turnos, horarios y bloqueos a ese profesional. `sillonId` limita solo los turnos. Sin filtros, se incluyen todos los profesionales. Un id sin datos MUST devolver listas vacías.

#### Scenario: Filtrar por profesional muestra solo su agenda
- **GIVEN** turnos, horarios y bloqueos de dos profesionales el mismo día
- **WHEN** un usuario autorizado consulta con el `profesionalId` del primero
- **THEN** turnos, horarios y bloqueos son solo del primer profesional

#### Scenario: Filtrar por sillón muestra solo los turnos de ese sillón
- **GIVEN** turnos del mismo día en dos sillones
- **WHEN** un usuario autorizado consulta con el `sillonId` del primero
- **THEN** `turnos` incluye solo los del primer sillón

#### Scenario: Los filtros de profesional y sillón se combinan
- **GIVEN** un profesional con turnos en dos sillones y otro profesional con un turno en el primer sillón
- **WHEN** un usuario autorizado consulta con el `profesionalId` del primero y el `sillonId` del primer sillón
- **THEN** `turnos` incluye solo el turno de ese profesional en ese sillón

#### Scenario: Un profesional sin datos devuelve listas vacías
- **WHEN** un usuario autorizado consulta con un `profesionalId` que no tiene turnos, horarios ni bloqueos
- **THEN** recibe `200` con `turnos`, `horarios` y `bloqueos` vacíos

### Requirement: Datos del paciente y de bloqueos según el rol

`ADMIN` y `RECEPCION` SHALL ver `paciente`, `notas` y `motivo` en todos los turnos y bloqueos. `ODONTOLOGO` SHALL verlos solo en los turnos y bloqueos de su propio profesional: en los de otros profesionales, `paciente`, `notas` y `motivo` MUST ser nulos, aunque conserve `pacienteId` y los horarios. Un `ODONTOLOGO` sin profesional vinculado MUST recibir `403`.

#### Scenario: Recepción ve el paciente de todos los turnos
- **GIVEN** turnos de dos profesionales con pacientes y notas, y un bloqueo con motivo
- **WHEN** `ADMIN` o `RECEPCION` consulta la vista diaria
- **THEN** cada turno trae `paciente` con nombre y apellido y sus `notas`, y el bloqueo trae su `motivo`

#### Scenario: El odontólogo ve el paciente solo en sus turnos
- **GIVEN** un `ODONTOLOGO` vinculado al primer profesional, un turno de cada profesional y un bloqueo con motivo de cada uno
- **WHEN** consulta la vista diaria sin filtros
- **THEN** su turno trae `paciente` y `notas` y su bloqueo trae `motivo`, y en los del otro profesional `paciente`, `notas` y `motivo` son nulos, con `pacienteId` presente y los horarios de ambos

#### Scenario: Un odontólogo sin profesional vinculado recibe 403
- **WHEN** un `ODONTOLOGO` sin profesional vinculado consulta `GET /api/agenda`
- **THEN** recibe `403` en Problem Details

#### Scenario: Sin sesión la agenda responde 401
- **WHEN** un cliente sin sesión consulta `GET /api/agenda`
- **THEN** recibe `401` en Problem Details

### Requirement: El odontólogo no escribe en ningún endpoint

Toda operación de escritura (`POST`, `PUT`, `PATCH` o `DELETE`) de la API, salvo login, logout y la zona pública, MUST responder `403` a un `ODONTOLOGO` autenticado, aun con un token CSRF válido (RN-33).

#### Scenario: Toda escritura de un odontólogo responde 403
- **GIVEN** un `ODONTOLOGO` autenticado con un token CSRF válido
- **WHEN** invoca cada endpoint de escritura registrado en la API, salvo login, logout y `/api/public/**`
- **THEN** cada uno responde `403` y no se modifica ningún dato

## ADDED Requirements

### Requirement: Alta interna de un turno online

La agenda SHALL ofrecer a los flujos públicos una operación interna de alta, sin endpoint ni rol de personal, que crea un turno `PROGRAMADO` con canal `ONLINE`, sin usuario creador y con la duración por defecto de la prestación (RN-03). MUST aplicar las mismas reglas, serialización y traducción a `409 RN-01` que el alta del personal (RN-01 a RN-04, RN-09, RN-11, RN-13, RN-15).

#### Scenario: Alta online solapada con otro turno del profesional
- **GIVEN** un turno no cancelado del profesional de 10:00 a 10:30
- **WHEN** se pide el alta online del mismo profesional para las 10:15
- **THEN** se rechaza con `409` código `RN-01` y recurso `PROFESIONAL`

### Requirement: Sillón asignado por el sistema en el alta online

El alta online SHALL usar el sillón preferido si está activo y libre en el intervalo; si no, MUST usar el primer sillón activo libre por orden de id. Si ningún sillón activo queda libre, MUST rechazar con `409` código `RN-01` y recurso `SILLON`.

#### Scenario: Alta online en el sillón preferido
- **GIVEN** un profesional que atiende el lunes de 09:00 a 13:00, una prestación de 30 minutos que realiza y el sillón 2 activo y libre
- **WHEN** se pide el alta online para el lunes a las 10:00 con sillón preferido 2
- **THEN** se guarda un turno `PROGRAMADO` de 10:00 a 10:30 en el sillón 2, con canal `ONLINE` y sin usuario creador

#### Scenario: Alta online sin sillón preferido libre
- **GIVEN** los sillones activos 1, 2 y 3, con el 1 y el 2 ocupados de 10:00 a 10:30
- **WHEN** se pide el alta online para las 10:00 con sillón preferido 2
- **THEN** el turno se guarda en el sillón 3

#### Scenario: Alta online sin ningún sillón libre
- **GIVEN** todos los sillones activos ocupados de 10:00 a 10:30 por turnos de otros profesionales
- **WHEN** se pide el alta online para las 10:00
- **THEN** se rechaza con `409` código `RN-01` y recurso `SILLON`
- **AND** no se guarda ningún turno

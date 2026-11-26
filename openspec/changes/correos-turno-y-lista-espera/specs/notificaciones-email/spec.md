## ADDED Requirements

### Requirement: Correos al agendar y al utilizar la lista de espera

El sistema SHALL usar un formato común para los correos de turno. El ingreso a la lista MUST indicar la prestación y que el consultorio avisará por correo cuando haya un horario compatible. La confirmación posterior a la espera MUST informar su finalización. Un ofrecimiento de hueco conserva su condición de oferta hasta agendar el turno.

#### Scenario: El correo de turno detalla fecha profesional y prestación
- **WHEN** se arma la confirmación de un turno
- **THEN** comunica el agendamiento y muestra fecha, profesional y prestación

#### Scenario: El correo de ingreso avisa que la espera sigue vigente
- **WHEN** se arma la confirmación del ingreso a la lista
- **THEN** comunica el registro de la solicitud y el futuro aviso por correo

#### Scenario: El correo de asignación desde lista informa que terminó la espera
- **WHEN** se arma la confirmación de un turno desde lista de espera
- **THEN** comunica el fin de la espera y muestra los mismos datos de un turno directo

#### Scenario: El primer registro de una entrada envía su confirmación una sola vez
- **WHEN** se confirma un alta de lista de espera
- **THEN** se registra el aviso con el identificador de la entrada y se envía una vez

# Spec Delta

## ADDED Requirements

### Requirement: Alta de turno desde la agenda

La pantalla SHALL ofrecer a ADMIN y RECEPCION el alta desde Agenda. El formulario MUST permitir buscar y seleccionar un paciente, elegir profesional y prestación asignada, y ajustar la duración de la prestación dentro de la regla RN-09. El odontólogo conserva su agenda de solo lectura.

#### Scenario: Recepción crea el turno y actualiza la agenda

- **GIVEN** un paciente que devuelve la búsqueda y una profesional con una prestación asignada
- **WHEN** recepción elige una sugerencia, ajusta el sillón y confirma
- **THEN** se envía el alta con la sesión y protección CSRF, se confirma y la agenda vuelve a consultar el día del turno

#### Scenario: Administración puede abrir el formulario de alta

- **WHEN** un ADMIN abre la agenda y elige Nuevo turno
- **THEN** se muestra el formulario de alta

#### Scenario: El odontólogo no ve el alta de turno

- **WHEN** un ODONTOLOGO abre la agenda
- **THEN** no aparece Nuevo turno ni se consulta la disponibilidad de alta

### Requirement: Horarios sugeridos en Buenos Aires

El formulario SHALL consultar horarios de un día civil en `America/Argentina/Buenos_Aires` con la duración elegida. MUST mostrar la hora local y proponer el sillón de la respuesta. El usuario MUST poder elegir otro sillón activo. Cambiar los criterios SHALL invalidar la selección y las respuestas anteriores.

#### Scenario: La duración ajustada vuelve a consultar el día de Buenos Aires

- **GIVEN** una sugerencia seleccionada con la duración de la prestación
- **WHEN** se cambia la duración y se vuelve a buscar
- **THEN** la selección anterior deja de ser válida, se consulta con la nueva duración y con las medianoches locales, y la sugerencia nueva se muestra en hora de Buenos Aires

#### Scenario: Un cambio de fecha descarta la disponibilidad anterior

- **GIVEN** una consulta de horarios pendiente
- **WHEN** se elige otra fecha y termina la consulta anterior
- **THEN** no se muestran sugerencias de la fecha anterior

### Requirement: Recuperación ante rechazos del alta

Ante un 409, la pantalla MUST distinguir profesional o sillón, conservar los datos y pedir alternativas con los mismos criterios. Ante un 422, MUST indicar el ajuste correspondiente según el código y el motivo. El formulario MUST permanecer disponible para corregirlo.

#### Scenario: Un conflicto del profesional actualiza las alternativas

- **WHEN** el servidor responde 409 indicando el profesional
- **THEN** se explica que el profesional está ocupado y se muestran alternativas actualizadas sin perder el paciente

#### Scenario: Un conflicto del sillón permite elegir otro sillón

- **WHEN** el servidor responde 409 indicando el sillón
- **THEN** se explica que el sillón está ocupado y se puede elegir una nueva sugerencia con otro sillón

#### Scenario: Un bloqueo del profesional indica cambiar el horario

- **WHEN** el alta responde 422 con RN-04 y motivo BLOQUEO
- **THEN** se muestra cómo corregir el horario y se conserva el formulario

#### Scenario: Los códigos de regla indican cómo corregir el turno

- **WHEN** el servidor rechaza un turno fuera de atención, en el pasado, con duración inválida o con recursos inválidos
- **THEN** el mensaje indica el dato que debe cambiarse

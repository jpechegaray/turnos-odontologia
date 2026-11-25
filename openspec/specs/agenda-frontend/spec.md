# agenda-frontend Specification

## Purpose
Permitir al personal consultar la agenda diaria o semanal en hora de Buenos Aires. Recepción y administración también pueden crear turnos desde la agenda con horarios sugeridos.

## Requirements

### Requirement: Pantalla de agenda para el personal

La SPA SHALL ofrecer la pantalla `/agenda`, con la entrada de menú "Agenda", a `ADMIN`, `RECEPCION` y `ODONTOLOGO`. Al entrar MUST pedir `GET /api/agenda` sin `fecha`, para que el día de hoy lo decida el servidor en Buenos Aires. Mientras carga MUST mostrar un estado de carga; si la consulta falla MUST mostrar un error con la opción de reintentar.

#### Scenario: Al entrar se muestra la agenda de hoy
- **GIVEN** una sesión activa y una API que responde la vista diaria del 2026-12-07 con un turno
- **WHEN** el usuario abre `/agenda`
- **THEN** se pide `GET /api/agenda?vista=dia` sin `fecha` y se muestra el lunes 07/12/2026 con ese turno

#### Scenario: Un fallo al cargar la agenda permite reintentar
- **GIVEN** que la primera consulta de la agenda responde `500` y la segunda responde la vista del día
- **WHEN** el usuario elige "Reintentar"
- **THEN** desaparece el error y se muestra la agenda

### Requirement: Vista diaria por profesional o por sillón

La vista diaria SHALL mostrar una grilla de filas de 15 minutos con una columna por profesional o por sillón (por defecto, por profesional). Cada turno MUST ocupar las filas de `[inicio, fin)` y mostrar paciente, prestación, horario y estado. Por profesional, cada columna MUST mostrar sus horarios de atención y bloqueos. La grilla va de la primera a la última hora con datos, o de 08:00 a 20:00 sin datos.

#### Scenario: La vista diaria ubica cada turno en la columna de su profesional
- **GIVEN** un turno de 10:00 a 10:30 de la primera profesional y otro de 11:00 a 11:45 del segundo profesional
- **WHEN** se muestra la vista diaria por profesional
- **THEN** cada turno aparece en la columna de su profesional, con su paciente, su prestación y su horario

#### Scenario: La vista diaria por sillón agrupa los turnos por sillón
- **GIVEN** dos turnos del mismo profesional en dos sillones
- **WHEN** el usuario elige agrupar por sillón
- **THEN** la grilla tiene una columna por sillón y cada turno aparece en la de su sillón

#### Scenario: Se muestran los horarios de atención y los bloqueos del profesional
- **GIVEN** una profesional que atiende de 09:00 a 13:00 y tiene un bloqueo de 12:00 a 13:00 con motivo "Capacitación"
- **WHEN** se muestra la vista diaria por profesional
- **THEN** su columna indica que atiende de 09:00 a 13:00 y muestra el bloqueo con su motivo

### Requirement: Vista semanal de lunes a domingo

La vista semanal SHALL mostrar siete columnas, de lunes a domingo, con los horarios de atención, los bloqueos y los turnos de cada día ordenados por hora. Un bloqueo de varios días MUST aparecer en cada día que toca.

#### Scenario: La vista semanal muestra cada turno en su día
- **GIVEN** una semana del 2026-12-07 al 2026-12-13 con un turno el lunes y otro el domingo
- **WHEN** el usuario elige la vista semanal
- **THEN** se pide `vista=semana`, hay una columna por día y cada turno aparece en la columna de su día

#### Scenario: Un bloqueo de varios días aparece en cada día que toca
- **GIVEN** un bloqueo del miércoles 2026-12-09 a las 15:00 al viernes 2026-12-11 a las 10:00
- **WHEN** se muestra la vista semanal
- **THEN** el bloqueo aparece el miércoles, el jueves y el viernes, y no aparece los otros días

### Requirement: Navegación y filtros

La pantalla SHALL permitir ir al período anterior y al siguiente (un día o una semana según la vista), volver a hoy y elegir una fecha. SHALL permitir filtrar por profesional y por sillón con los catálogos de `GET /api/profesionales` y `GET /api/sillones`, y enviar los filtros como `profesionalId` y `sillonId`. Cambiar la vista MUST conservar la fecha consultada.

#### Scenario: Ir al día siguiente pide la fecha siguiente
- **GIVEN** la vista diaria del 2026-12-07
- **WHEN** el usuario elige "Siguiente"
- **THEN** se pide la agenda con `fecha=2026-12-08`

#### Scenario: En la vista semanal se avanza una semana
- **GIVEN** la vista semanal del 2026-12-07 al 2026-12-13
- **WHEN** el usuario elige "Anterior"
- **THEN** se pide la agenda con `vista=semana` y `fecha=2026-11-30`

#### Scenario: Filtrar por profesional y por sillón envía los dos filtros
- **GIVEN** la agenda del día cargada
- **WHEN** el usuario elige una profesional y un sillón en los filtros
- **THEN** se pide la agenda con ese `profesionalId` y ese `sillonId`

### Requirement: Estados del design system

Cada turno SHALL mostrar su estado con el badge del design system, que combina color, ícono y texto: `PROGRAMADO` como "Reservado", `CONFIRMADO`, `ATENDIDO`, `AUSENTE` y `CANCELADO`. Un turno cancelado MUST verse tachado. Cada bloqueo MUST mostrarse con la variante "Bloqueo". La pantalla MUST mostrar la leyenda de estados.

#### Scenario: Cada turno muestra el badge de su estado
- **GIVEN** un turno de cada estado: `PROGRAMADO`, `CONFIRMADO`, `ATENDIDO`, `AUSENTE` y `CANCELADO`
- **WHEN** se muestra la vista diaria
- **THEN** cada turno lleva el texto de su estado ("Reservado", "Confirmado", "Atendido", "Ausente" y "Cancelado") con la clase de estado del design system

### Requirement: Hora de Buenos Aires

Toda fecha y hora de la agenda SHALL mostrarse en `America/Argentina/Buenos_Aires` (RN-14), cualquiera sea la zona del navegador y el desfase con que llegue el instante. Un turno MUST ubicarse en la fila de su hora local.

#### Scenario: Un instante en UTC se muestra en hora de Buenos Aires
- **GIVEN** un turno con `inicio` `2026-12-08T02:30:00Z` y `fin` `2026-12-08T02:55:00Z` en la vista diaria del 2026-12-07
- **WHEN** se muestra la vista diaria
- **THEN** el turno se muestra de 23:30 a 23:55 y ocupa la fila de las 23:30

### Requirement: Ficha básica del paciente

Desde un turno con paciente visible, la pantalla SHALL abrir en solo lectura la ficha básica con `GET /api/pacientes/{id}`: nombre, apellido, DNI, teléfono, email, obra social y número de afiliado. Si la API devuelve el paciente nulo, el turno MUST mostrar "Paciente de otro profesional" y no MUST ofrecer la ficha. Si la ficha no se puede cargar, MUST mostrarse un error.

#### Scenario: Se abre la ficha básica del paciente de un turno
- **GIVEN** un turno de la paciente Ana Prueba
- **WHEN** el usuario elige ver su ficha
- **THEN** se pide `GET /api/pacientes/{id}` y se muestran sus datos de contacto y obra social sin campos editables

#### Scenario: Un turno de otro profesional no ofrece la ficha
- **GIVEN** un `ODONTOLOGO` y un turno de otro profesional, que la API devuelve con `paciente` nulo
- **WHEN** se muestra la vista diaria
- **THEN** el turno dice "Paciente de otro profesional" y no ofrece abrir la ficha

### Requirement: Agenda en solo lectura para el odontólogo

Para `ODONTOLOGO` la pantalla SHALL ser de solo lectura (RN-33): MUST NOT mostrar acciones de alta, reprogramación, cancelación, asistencia, horarios ni bloqueos, ni huecos libres para crear turnos, y MUST NOT enviar pedidos de escritura.

#### Scenario: El odontólogo ve su agenda sin acciones de escritura
- **GIVEN** un `ODONTOLOGO` con una agenda que tiene turnos, horarios y bloqueos
- **WHEN** recorre la vista diaria y la semanal y abre una ficha
- **THEN** no hay botones de alta, reprogramación, cancelación, asistencia, horarios ni bloqueos, y no se envía ningún `POST`, `PUT`, `PATCH` ni `DELETE`

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

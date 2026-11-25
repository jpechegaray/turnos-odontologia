# bloqueos-frontend Specification

## Purpose

Define cómo el personal consulta, crea y elimina desde la SPA los bloqueos de agenda de cada profesional (vacaciones, congresos, trámites), con fechas en hora de Buenos Aires y escritura limitada a `ADMIN` y `RECEPCION` (US-008, RN-33).

## Requirements

### Requirement: Consulta de los bloqueos de un profesional
En `/profesionales`, cada fila SHALL ofrecer a los tres roles la acción «Bloqueos», que abre un panel del profesional. El panel consulta `GET /api/profesionales/{id}/bloqueos` y MUST mostrar cada bloqueo en el orden de la API, con fecha y hora de inicio y de fin en hora de Buenos Aires (`dd/mm/aaaa HH:mm`) y su motivo. MUST informar cuando no hay bloqueos. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El panel muestra los bloqueos en hora de Buenos Aires
- **GIVEN** una sesión activa y un profesional con un bloqueo de `2026-12-01T13:00:00Z` a `2026-12-01T21:00:00Z` con motivo "Congreso"
- **WHEN** la persona abre su panel de bloqueos
- **THEN** la SPA consulta `GET /api/profesionales/{id}/bloqueos` y el panel muestra el bloqueo del 01/12/2026 10:00 al 01/12/2026 18:00 con motivo "Congreso"

#### Scenario: Un profesional sin bloqueos muestra un mensaje
- **GIVEN** un profesional cuyo listado de bloqueos es vacío
- **WHEN** la persona abre su panel de bloqueos
- **THEN** el panel muestra un mensaje de que el profesional no tiene bloqueos

#### Scenario: Un fallo al consultar los bloqueos permite reintentar
- **GIVEN** una API que falla al listar los bloqueos del profesional
- **WHEN** la persona abre su panel de bloqueos
- **THEN** el panel muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar los bloqueos

### Requirement: Validación local de un bloqueo antes de enviar
El formulario de bloqueo SHALL exigir fecha y hora de inicio, fecha y hora de fin, con el fin estrictamente posterior al inicio, y un motivo no blanco de hasta 255 caracteres. Un bloqueo inválido MUST mostrar el motivo del rechazo junto al campo y MUST NOT llamar a la API.

#### Scenario: Un fin igual o anterior al inicio bloquea el envío sin llamar a la API
- **WHEN** quien edita ingresa un bloqueo del 01/12/2026 18:00 al 01/12/2026 10:00 con motivo "Congreso" y lo envía
- **THEN** el formulario indica que el fin debe ser posterior al inicio y no se emite ninguna escritura a la API

#### Scenario: Un motivo vacío o demasiado largo bloquea el envío sin llamar a la API
- **WHEN** quien edita ingresa un bloqueo válido en fechas con el motivo en blanco o con 256 caracteres y lo envía
- **THEN** el formulario indica el problema del motivo y no se emite ninguna escritura a la API

### Requirement: Alta y eliminación de bloqueos
Para crear, la SPA SHALL enviar `POST /api/profesionales/{id}/bloqueos` con `inicio` y `fin` como instantes ISO 8601 en UTC, calculados a partir de la fecha y hora ingresadas en hora de Buenos Aires, y el `motivo` sin espacios al borde. Para eliminar, pide confirmación y envía `DELETE /api/profesionales/{id}/bloqueos/{bloqueoId}`. Tras cada respuesta exitosa, el panel MUST volver a consultar los bloqueos y anunciar el éxito. Cancelar MUST descartar sin escribir.

#### Scenario: Agregar un bloqueo envía los instantes en UTC y lo muestra
- **GIVEN** una sesión `RECEPCION` y un profesional sin bloqueos
- **WHEN** agrega un bloqueo del 01/12/2026 10:00 al 01/12/2026 18:00 con motivo " Congreso "
- **THEN** la SPA emite `POST /api/profesionales/{id}/bloqueos` con `inicio` `2026-12-01T13:00:00.000Z`, `fin` `2026-12-01T21:00:00.000Z` y `motivo` "Congreso", vuelve a consultar los bloqueos, anuncia el éxito y el panel muestra el bloqueo

#### Scenario: Eliminar un bloqueo pide confirmación y lo retira
- **GIVEN** una sesión `ADMIN` y un profesional con un bloqueo
- **WHEN** elige eliminarlo y confirma
- **THEN** la SPA emite `DELETE /api/profesionales/{id}/bloqueos/{bloqueoId}` y el panel indica que el profesional no tiene bloqueos

#### Scenario: Cancelar la eliminación de un bloqueo no escribe
- **WHEN** quien edita elige eliminar un bloqueo y cancela la confirmación
- **THEN** no se emite ninguna escritura a la API y el bloqueo sigue visible

### Requirement: Errores de la API al escribir bloqueos
El panel SHALL traducir los errores por código, sin mostrar el `detalle` del servidor. `404 BLOQUEO_NO_ENCONTRADO` avisa que el bloqueo ya no existe y vuelve a consultar. `404 PROFESIONAL_NO_ENCONTRADO` hace que la página refresque la lista de profesionales. Un `409` o `422` muestra que el bloqueo no se puede crear en ese intervalo y conserva lo ingresado. `403` muestra permiso insuficiente sin cerrar la sesión. Otro error invita a reintentar.

#### Scenario: Un bloqueo que ya no existe avisa y recarga
- **WHEN** quien edita elimina un bloqueo y la API responde `404` con código `BLOQUEO_NO_ENCONTRADO`
- **THEN** el panel muestra que el bloqueo ya no existe y vuelve a consultar los bloqueos

#### Scenario: Un profesional que ya no existe refresca la lista al escribir un bloqueo
- **WHEN** quien edita crea un bloqueo y la API responde `404` con código `PROFESIONAL_NO_ENCONTRADO`
- **THEN** la página muestra que el profesional ya no existe y vuelve a consultar `GET /api/profesionales`

#### Scenario: Un conflicto del servidor conserva lo ingresado
- **WHEN** quien edita crea un bloqueo y la API responde `409`
- **THEN** el panel muestra que el bloqueo no se puede crear en ese intervalo, sin el `detalle` del servidor, y el formulario conserva las fechas y el motivo ingresados

#### Scenario: Un 403 al escribir un bloqueo muestra permiso insuficiente sin cerrar la sesión
- **WHEN** quien edita crea o elimina un bloqueo y la API responde `403`
- **THEN** el panel muestra una alerta de permiso insuficiente, sin el `detalle` del servidor, y la sesión sigue activa

### Requirement: Escritura de bloqueos solo para ADMIN y RECEPCION
`ADMIN` y `RECEPCION` SHALL ver en el panel de bloqueos el formulario de alta y la acción de eliminar para cualquier profesional. `ODONTOLOGO` MUST ver los bloqueos de su propio profesional, el único que le devuelve el listado, sin controles de escritura y sin emitir escrituras (RN-33).

#### Scenario: Recepción ve los controles de bloqueos
- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre el panel de bloqueos de un profesional
- **THEN** el panel ofrece el formulario de alta y la acción de eliminar en cada bloqueo

#### Scenario: El odontólogo ve sus bloqueos sin controles de edición
- **GIVEN** una sesión activa con rol `ODONTOLOGO` cuyo profesional tiene un bloqueo con motivo "Vacaciones"
- **WHEN** abre el panel de bloqueos de su registro
- **THEN** el panel muestra el bloqueo "Vacaciones" sin el formulario ni la acción de eliminar, y no se emite ninguna escritura a la API

## ADDED Requirements

### Requirement: Selección y lectura de notificaciones

La bandeja SHALL mostrar «Leída» o «No leída» por aviso y ofrecer casillas individuales y una casilla para seleccionar todos los avisos de la página actual. El personal SHALL poder marcar la selección como leída o no leída. La SPA SHALL recordar la lectura por cuenta en el navegador y guardar solo los identificadores de los avisos leídos. La lectura indica una acción del personal dentro de la bandeja.

#### Scenario: Se pueden seleccionar varios avisos y cambiar su lectura
- **WHEN** se marcan avisos individualmente o se selecciona toda la página y se aplica una acción de lectura
- **THEN** los avisos elegidos pasan a «Leída» o «No leída» según la acción y se limpia la selección

#### Scenario: La lectura se conserva al volver a abrir la bandeja
- **GIVEN** que el personal marcó un aviso como leído
- **WHEN** vuelve a abrir la bandeja con la misma cuenta en el mismo navegador
- **THEN** el aviso sigue marcado como «Leída»

#### Scenario: La bandeja oculta el estado de envío y el error técnico
- **GIVEN** un aviso con un error de envío en el registro
- **WHEN** se muestra la bandeja
- **THEN** la fila muestra la lectura y la bandeja omite el estado, el filtro de estado y el error de envío

### Requirement: Detalle común de notificaciones

La SPA SHALL abrir el detalle desde el tipo de una fila o desde «Abrir notificación» cuando haya exactamente un aviso seleccionado. El detalle SHALL mostrar el tipo, el destinatario, el turno o la oferta y la fecha de envío con la misma estructura para todos los tipos. Abrirlo SHALL marcar el aviso como leído. Al volver a la bandeja, la selección SHALL conservarse y el foco MUST volver al control de apertura.

#### Scenario: Una sola notificación seleccionada abre el detalle común
- **GIVEN** un recordatorio y un aviso de hueco en la bandeja
- **WHEN** se abre un único aviso seleccionado y luego se abre el otro desde su fila
- **THEN** ambos detalles muestran el mismo conjunto de datos con los valores del aviso correspondiente

### Requirement: Filtro por número de turno en la bandeja

La SPA SHALL ofrecer una búsqueda por número de turno y consultar desde la primera página con `turnoId` solo si se indicó un valor. Un número que no sea un entero positivo MUST mostrar un error en el campo y MUST NOT emitir la consulta.

#### Scenario: Filtrar por turno envía solo el número de turno
- **WHEN** recepción escribe « 42 » y busca
- **THEN** se consulta `GET /api/notificaciones?turnoId=42&pagina=0&tamanio=20`

#### Scenario: Un número de turno inválido no consulta
- **WHEN** recepción escribe el turno «abc» o «0» y busca
- **THEN** el campo muestra «Ingresá un número de turno válido.» y se conserva el listado

#### Scenario: Buscar vuelve a la primera página
- **GIVEN** la pantalla en la segunda página
- **WHEN** recepción busca un turno
- **THEN** la consulta se emite con `pagina=0`

#### Scenario: Una búsqueda sin coincidencias muestra un mensaje
- **WHEN** la API devuelve una página vacía para un turno
- **THEN** la tabla muestra «No hay envíos que coincidan con los filtros.»

#### Scenario: Sin envíos registrados se muestra un mensaje
- **WHEN** la API devuelve una página vacía sin filtro
- **THEN** la tabla muestra «No hay envíos registrados.»

## MODIFIED Requirements

### Requirement: Listado del registro de notificaciones

Al abrir `/notificaciones` la SPA SHALL consultar `GET /api/notificaciones?pagina=0&tamanio=20` y mostrar los avisos en el orden del backend, con tipo, destinatario, turno y fecha de envío en Buenos Aires. Un aviso sin turno pero con oferta de hueco MUST mostrarse como «Aviso de hueco» en la columna de turno. Un dato ausente MUST mostrarse con una marca de vacío.

#### Scenario: Al abrir la pantalla se lista la primera página del registro
- **GIVEN** que la API devuelve tres avisos
- **WHEN** recepción abre `/notificaciones`
- **THEN** se consulta `GET /api/notificaciones?pagina=0&tamanio=20` y la tabla muestra los tres en el orden recibido

#### Scenario: Los datos del envío se muestran en español y en hora de Buenos Aires
- **GIVEN** un recordatorio del turno 42 con `enviadaEn` `2026-12-03T13:05:00Z`
- **WHEN** se muestra el listado
- **THEN** la fila muestra «Recordatorio», «#42» y «03/12/2026 10:05»

#### Scenario: Un aviso de hueco sin turno se identifica como tal
- **GIVEN** una oferta de hueco con `turnoId` nulo
- **WHEN** se muestra el listado
- **THEN** la fila muestra «Aviso de hueco» como tipo y en la columna de turno

#### Scenario: Un envío pendiente muestra la marca de vacío en la fecha de envío
- **WHEN** se muestra un aviso con `enviadaEn` nulo
- **THEN** su fecha de envío muestra una marca de vacío identificada como «Sin fecha de envío»

### Requirement: Paginación del registro

La SPA SHALL mostrar «Página X de Y · N envíos» y los botones «Anterior» y «Siguiente», que consultan la página contigua con el turno vigente. Los botones MUST deshabilitarse en los extremos. La paginación MUST NOT mostrarse sin resultados.

#### Scenario: Siguiente consulta la página contigua con los filtros vigentes
- **GIVEN** la primera de dos páginas con 25 envíos para el turno 42
- **WHEN** recepción pulsa «Siguiente»
- **THEN** se consulta `GET /api/notificaciones?turnoId=42&pagina=1&tamanio=20` y se muestra «Página 2 de 2 · 25 envíos»

#### Scenario: Los botones se deshabilitan en los extremos
- **GIVEN** una única página con tres avisos
- **WHEN** se muestra la paginación
- **THEN** «Anterior» y «Siguiente» están deshabilitados y se muestra «Página 1 de 1 · 3 envíos»

## REMOVED Requirements

### Requirement: Último error de los envíos fallidos

**Reason**: La bandeja muestra el seguimiento de lectura del personal. El diagnóstico técnico permanece en el registro de la API.
**Migration**: No requiere migración de datos.

### Requirement: Filtros por estado y por turno

**Reason**: La bandeja ofrece la búsqueda solo por turno.
**Migration**: Usar el filtro de turno de la bandeja. La API conserva el filtro de estado.

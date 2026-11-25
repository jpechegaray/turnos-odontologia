## ADDED Requirements

### Requirement: Acceso a la pantalla de notificaciones según rol

La pantalla `/notificaciones` SHALL estar disponible y aparecer en el menú como "Notificaciones" para `ADMIN` y `RECEPCION`. `ODONTOLOGO` MUST NOT verla en el menú y, si navega a `/notificaciones`, la SPA MUST mostrar el aviso de falta de permiso sin consultar `/api/notificaciones`.

#### Scenario: La pantalla de notificaciones aparece en el menú para administración y recepción
- **GIVEN** una sesión activa con rol `ADMIN` o `RECEPCION`
- **WHEN** se muestra el menú principal
- **THEN** la entrada "Notificaciones" está presente y abre `/notificaciones`

#### Scenario: El odontólogo no ve la pantalla de notificaciones
- **GIVEN** una sesión activa con rol `ODONTOLOGO`
- **WHEN** se muestra el menú principal y navega a `/notificaciones`
- **THEN** el menú no tiene la entrada "Notificaciones", la pantalla muestra el aviso de falta de permiso y no se emite ninguna consulta a `/api/notificaciones`

### Requirement: Listado del registro de notificaciones

Al abrir `/notificaciones` la SPA SHALL consultar `GET /api/notificaciones?pagina=0&tamanio=20` y mostrar los envíos en el orden del backend, con tipo y estado en español, destinatario, turno, fecha y hora de referencia en Buenos Aires, intentos y fecha de envío. Un envío sin turno pero con oferta de hueco MUST mostrarse como "Aviso de hueco" en la columna de turno. Un dato ausente MUST mostrarse con una marca de vacío.

#### Scenario: Al abrir la pantalla se lista la primera página del registro
- **GIVEN** que la API devuelve tres envíos, uno `PENDIENTE`, uno `ENVIADA` y uno `FALLIDA`
- **WHEN** recepción abre `/notificaciones`
- **THEN** se consulta `GET /api/notificaciones?pagina=0&tamanio=20` y la tabla muestra los tres en el orden recibido, con los estados "Pendiente", "Enviada" y "Fallida"

#### Scenario: Los datos del envío se muestran en español y en hora de Buenos Aires
- **GIVEN** un envío `RECORDATORIO` del turno 42 con `inicioReferencia` `2026-12-04T13:00:00Z`, 1 intento y `enviadaEn` `2026-12-03T13:05:00Z`
- **WHEN** se muestra el listado
- **THEN** la fila muestra "Recordatorio", "#42", "04/12/2026 10:00", "1" y "03/12/2026 10:05"

#### Scenario: Un aviso de hueco sin turno se identifica como tal
- **GIVEN** un envío `OFERTA_HUECO` con `turnoId` nulo y `ofertaHuecoId` 7
- **WHEN** se muestra el listado
- **THEN** la fila muestra el tipo "Aviso de hueco" y, en la columna de turno, "Aviso de hueco"

#### Scenario: Un envío pendiente muestra la marca de vacío en la fecha de envío
- **GIVEN** un envío `PENDIENTE` con `enviadaEn` nulo
- **WHEN** se muestra el listado
- **THEN** la fecha de envío de esa fila muestra la marca de vacío

### Requirement: Último error de los envíos fallidos

La SPA SHALL mostrar el `ultimoError` de cada envío `FALLIDA`. Para los envíos `ENVIADA` y `PENDIENTE` la columna de error MUST mostrar la marca de vacío.

#### Scenario: Un envío fallido muestra su último error
- **GIVEN** un envío `FALLIDA` con 3 intentos y `ultimoError` «No se pudo enviar la notificación por email»
- **WHEN** se muestra el listado
- **THEN** la fila muestra "Fallida", "3" y «No se pudo enviar la notificación por email»

#### Scenario: Un envío enviado no muestra error
- **GIVEN** un envío `ENVIADA`
- **WHEN** se muestra el listado
- **THEN** la columna de error de esa fila muestra la marca de vacío

### Requirement: Filtros por estado y por turno

La SPA SHALL permitir filtrar por estado (Todos, Pendiente, Enviada o Fallida) y por número de turno, combinables. Al buscar, MUST consultar desde la primera página y enviar solo los filtros elegidos (`estado`, `turnoId`). Un número de turno que no sea un entero positivo MUST mostrar un error en el campo y MUST NOT emitir la consulta.

#### Scenario: Filtrar por estado envía solo ese filtro
- **GIVEN** la pantalla abierta
- **WHEN** recepción elige el estado "Fallida" y busca
- **THEN** se consulta `GET /api/notificaciones?estado=FALLIDA&pagina=0&tamanio=20`

#### Scenario: Los filtros de estado y turno se combinan
- **GIVEN** la pantalla abierta
- **WHEN** recepción elige "Enviada", escribe el turno " 42 " y busca
- **THEN** se consulta `GET /api/notificaciones?estado=ENVIADA&turnoId=42&pagina=0&tamanio=20`

#### Scenario: Un número de turno inválido no consulta
- **GIVEN** la pantalla abierta
- **WHEN** recepción escribe el turno "abc" o "0" y busca
- **THEN** el campo muestra "Ingresá un número de turno válido." y no se emite una nueva consulta

#### Scenario: Buscar vuelve a la primera página
- **GIVEN** la pantalla en la segunda página
- **WHEN** recepción cambia el estado y busca
- **THEN** la consulta se emite con `pagina=0`

#### Scenario: Una búsqueda sin coincidencias muestra un mensaje
- **GIVEN** que la API devuelve una página vacía para los filtros elegidos
- **WHEN** se muestra el listado
- **THEN** la tabla muestra "No hay envíos que coincidan con los filtros."

#### Scenario: Sin envíos registrados se muestra un mensaje
- **GIVEN** que la API devuelve una página vacía sin filtros
- **WHEN** se abre la pantalla
- **THEN** la tabla muestra "No hay envíos registrados."

### Requirement: Paginación del registro

La SPA SHALL mostrar "Página X de Y · N envíos" según la respuesta y los botones "Anterior" y "Siguiente", que consultan la página contigua con los filtros vigentes. "Anterior" MUST estar deshabilitado en la primera página y "Siguiente" en la última. Sin resultados, la paginación MUST NOT mostrarse.

#### Scenario: Siguiente consulta la página contigua con los filtros vigentes
- **GIVEN** una respuesta con `pagina` 0, `totalPaginas` 2 y `totalElementos` 25, con el filtro de estado "Fallida" aplicado
- **WHEN** recepción pulsa "Siguiente"
- **THEN** se consulta `GET /api/notificaciones?estado=FALLIDA&pagina=1&tamanio=20` y se muestra "Página 2 de 2 · 25 envíos"

#### Scenario: Los botones se deshabilitan en los extremos
- **GIVEN** una respuesta con `pagina` 0 y `totalPaginas` 1
- **WHEN** se muestra la paginación
- **THEN** "Anterior" y "Siguiente" están deshabilitados y se muestra "Página 1 de 1 · 3 envíos"

### Requirement: Errores al cargar el registro

Si la consulta falla, la SPA SHALL mostrar una alerta y MUST NOT mostrar datos viejos. Ante `403` MUST mostrar el aviso de falta de permiso. Ante cualquier otro error MUST mostrar "No pudimos cargar el registro de notificaciones." con un botón "Reintentar" que repite la última consulta.

#### Scenario: Un error de carga ofrece reintentar
- **GIVEN** que la API responde `500` la primera vez y una página válida la segunda
- **WHEN** recepción abre la pantalla y pulsa "Reintentar"
- **THEN** primero ve "No pudimos cargar el registro de notificaciones." y, al reintentar, se repite la misma consulta y se muestra el listado

#### Scenario: Un 403 de la API muestra el aviso de permiso
- **GIVEN** que la API responde `403`
- **WHEN** recepción abre la pantalla
- **THEN** se muestra "No tenés permiso para ver el registro de notificaciones." y no hay tabla

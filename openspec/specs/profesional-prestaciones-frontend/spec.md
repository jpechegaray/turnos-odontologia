# profesional-prestaciones-frontend Specification

## Purpose

Define cómo el personal consulta desde la SPA las prestaciones que realiza cada profesional y cómo el rol `ADMIN` reemplaza ese conjunto, sin ofrecer prestaciones inactivas nuevas ni perder las inactivas ya asociadas (US-005, RN-11).

## Requirements

### Requirement: Consulta de las prestaciones de un profesional en la SPA

En `/profesionales`, cada fila SHALL ofrecer a los tres roles una acción «Prestaciones». Esa acción abre un panel del profesional que consulta `GET /api/profesionales/{id}/prestaciones` y muestra cada prestación asociada con su nombre, su duración en minutos y la marca «inactiva» cuando corresponde. MUST informar cuando no tiene prestaciones. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El panel muestra las prestaciones asociadas con su duración y la marca de inactiva

- **GIVEN** una sesión activa y un profesional asociado a "Consulta" activa y a "Blanqueamiento" inactiva
- **WHEN** la persona usa la acción «Prestaciones» de ese profesional
- **THEN** la SPA consulta `GET /api/profesionales/{id}/prestaciones` y el panel muestra ambas con su duración en minutos, con "Blanqueamiento" marcada como inactiva

#### Scenario: Un profesional sin prestaciones muestra un mensaje en el panel

- **GIVEN** una API que devuelve una lista vacía de prestaciones para el profesional
- **WHEN** la persona abre su panel de prestaciones
- **THEN** el panel muestra un mensaje de que el profesional no tiene prestaciones asociadas

#### Scenario: Un fallo al consultar las prestaciones del profesional permite reintentar

- **GIVEN** una API que responde `500` al consultar las prestaciones del profesional
- **WHEN** la persona abre su panel de prestaciones
- **THEN** el panel muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar las prestaciones del profesional

### Requirement: Opciones ofrecidas para asociar prestaciones

Para `ADMIN`, el panel SHALL ofrecer una casilla por opción tomada de `GET /api/prestaciones?soloActivas=false`. Las opciones son las prestaciones activas del catálogo más las ya asociadas aunque estén inactivas. Estas últimas se marcan como inactivas y se pueden conservar o quitar. Las prestaciones inactivas no asociadas MUST NOT ofrecerse. Las asociadas actuales MUST aparecer preseleccionadas.

#### Scenario: El administrador ve preseleccionadas las asociadas y solo las activas como nuevas opciones

- **GIVEN** una sesión `ADMIN`, un catálogo con "Consulta" y "Limpieza" activas y "Blanqueamiento" y "Ortodoncia" inactivas, y un profesional asociado a "Consulta" y "Blanqueamiento"
- **WHEN** abre el panel de prestaciones del profesional
- **THEN** ve casillas para "Consulta" y "Blanqueamiento" marcadas, "Limpieza" sin marcar, "Blanqueamiento" señalada como inactiva y ninguna casilla para "Ortodoncia"

### Requirement: Reemplazo del conjunto de prestaciones desde la SPA

Al guardar, la SPA SHALL enviar `PUT /api/profesionales/{id}/prestaciones` con `{"prestacionIds": [...]}`. El cuerpo lleva el conjunto completo seleccionado, sin ids repetidos. Con un `200` el panel MUST mostrar la lista devuelta por la API y anunciar el éxito. Cancelar MUST descartar los cambios sin emitir escrituras.

#### Scenario: El administrador guarda un nuevo conjunto de prestaciones

- **GIVEN** un profesional asociado a "Consulta" y la prestación activa "Limpieza" sin asociar
- **WHEN** el administrador marca "Limpieza", guarda y la API responde `200` con "Consulta" y "Limpieza"
- **THEN** la SPA emite un único `PUT` con los ids de "Consulta" y "Limpieza" sin repetidos, el panel muestra la lista devuelta y se anuncia el éxito

#### Scenario: Quitar todas las prestaciones envía una lista vacía

- **GIVEN** un profesional asociado solo a "Consulta"
- **WHEN** el administrador desmarca "Consulta", guarda y la API responde `200` con una lista vacía
- **THEN** la SPA emite el `PUT` con `prestacionIds` vacío y el panel indica que el profesional no tiene prestaciones asociadas

#### Scenario: Cancelar la edición de prestaciones descarta los cambios sin escrituras

- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** el administrador marca otra prestación y luego cancela
- **THEN** el panel vuelve a mostrar solo "Consulta" como asociada y no se emite ninguna escritura a la API

#### Scenario: Guardar las prestaciones dos veces seguidas emite un solo reemplazo

- **GIVEN** una API que tarda en responder el `PUT` de prestaciones
- **WHEN** el administrador pulsa guardar dos veces mientras el pedido está pendiente
- **THEN** el botón de guardar queda deshabilitado durante el pedido y la SPA emite un solo `PUT`

### Requirement: Errores al reemplazar las prestaciones de un profesional

Los errores del reemplazo SHALL mostrarse por código y MUST NOT mostrar el `detalle` del servidor. Ante `422 PRESTACION_INEXISTENTE` o `422 PRESTACION_INACTIVA` se muestra una alerta, se vuelven a consultar el catálogo y las asociadas y se conserva la selección. Ante `404 PROFESIONAL_NO_ENCONTRADO`, una alerta y el refresco de la lista de profesionales. Ante `409 PROFESIONAL_PRESTACIONES_CONFLICTO`, una alerta de reintento. Ante `403`, permiso insuficiente sin cerrar la sesión.

#### Scenario: Una prestación dada de baja mientras se editaba se muestra como alerta

- **WHEN** la API responde `422` con código `PRESTACION_INACTIVA` al guardar las prestaciones
- **THEN** el panel muestra una alerta de prestación inactiva, vuelve a consultar el catálogo y las prestaciones del profesional y conserva la selección hecha

#### Scenario: Una prestación inexistente al guardar se muestra como alerta

- **WHEN** la API responde `422` con código `PRESTACION_INEXISTENTE` al guardar las prestaciones
- **THEN** el panel muestra una alerta de prestación inexistente, vuelve a consultar el catálogo y las prestaciones del profesional y conserva la selección hecha

#### Scenario: Guardar prestaciones de un profesional inexistente refresca la lista de profesionales

- **WHEN** la API responde `404` con código `PROFESIONAL_NO_ENCONTRADO` al guardar las prestaciones
- **THEN** la pantalla muestra una alerta de profesional inexistente y vuelve a consultar `GET /api/profesionales`

#### Scenario: Un conflicto al guardar prestaciones pide reintentar

- **WHEN** la API responde `409` con código `PROFESIONAL_PRESTACIONES_CONFLICTO` al guardar las prestaciones
- **THEN** el panel muestra una alerta que invita a intentar de nuevo y conserva la selección hecha

#### Scenario: Un 403 al guardar prestaciones se muestra como permiso insuficiente

- **WHEN** la API responde `403` al guardar las prestaciones de un profesional
- **THEN** el panel muestra una alerta de permiso insuficiente, sin el `detalle` del servidor, y la sesión sigue activa

### Requirement: Prestaciones de un profesional en solo lectura según rol

Solo `ADMIN` SHALL ver las casillas y el botón de guardar del panel. `RECEPCION` y `ODONTOLOGO` MUST ver la lista de prestaciones asociadas sin controles de edición y sin emitir escrituras. El odontólogo solo accede al panel de su propio registro, el único que le devuelve el listado.

#### Scenario: Recepción consulta las prestaciones de un profesional sin controles de edición

- **GIVEN** una sesión activa con rol `RECEPCION` y un profesional asociado a "Consulta"
- **WHEN** abre el panel de prestaciones de ese profesional
- **THEN** ve "Consulta" en la lista sin casillas ni botón de guardar, y no se emite ninguna escritura

#### Scenario: El odontólogo consulta sus propias prestaciones sin controles de edición

- **GIVEN** una sesión activa con rol `ODONTOLOGO` cuyo único profesional visible está asociado a "Consulta"
- **WHEN** abre el panel de prestaciones de su registro
- **THEN** ve "Consulta" en la lista sin casillas ni botón de guardar, y no se emite ninguna escritura

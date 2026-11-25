# horarios-frontend Specification

## Purpose

Define cómo el personal consulta y edita desde la SPA los tramos de atención semanales de cada profesional, con validación local de superposición (RN-16) y escritura limitada a `ADMIN` y `RECEPCION` (US-007, RN-33).

## Requirements

### Requirement: Consulta de los tramos de un profesional por día de la semana
En `/profesionales`, cada fila SHALL ofrecer a los tres roles la acción «Horarios», que abre un panel del profesional. El panel consulta `GET /api/profesionales/{id}/horarios` y MUST mostrar los días de lunes a domingo en ese orden. Cada día lista sus tramos ordenados por hora de inicio en formato `HH:mm` o, si no tiene tramos, indica que no atiende. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El panel agrupa los tramos por día y los ordena por hora de inicio
- **GIVEN** una sesión activa y un profesional con tramos el lunes de 13:00 a 17:00 y de 08:00 a 12:00 y el miércoles de 09:00 a 13:00
- **WHEN** la persona abre su panel de horarios
- **THEN** la SPA consulta `GET /api/profesionales/{id}/horarios` y el panel muestra el lunes con 08:00 a 12:00 antes de 13:00 a 17:00, el miércoles con 09:00 a 13:00 y los demás días sin atención

#### Scenario: Un profesional sin tramos muestra todos los días sin atención
- **GIVEN** un profesional cuyo listado de horarios es vacío
- **WHEN** la persona abre su panel de horarios
- **THEN** el panel muestra los siete días, cada uno con la indicación de que no atiende

#### Scenario: Un fallo al consultar los horarios permite reintentar
- **GIVEN** una API que falla al listar los horarios del profesional
- **WHEN** la persona abre su panel de horarios
- **THEN** el panel muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar los horarios

### Requirement: Validación local de un tramo antes de enviar
El formulario de tramo SHALL exigir un día de la semana y horas de inicio y fin en `HH:mm`, con el fin posterior al inicio. También MUST rechazar un tramo que se superponga con otro del mismo día, salvo con el tramo que se está modificando. Dos tramos `[inicio, fin)` se superponen solo si `inicioA < finB` y `finA > inicioB`. Un tramo inválido MUST mostrar el motivo junto al formulario y MUST NOT llamar a la API.

#### Scenario: Un fin igual o anterior al inicio bloquea el envío sin llamar a la API
- **WHEN** quien edita ingresa un tramo del lunes de 12:00 a 12:00 o de 12:00 a 09:00 y lo envía
- **THEN** el formulario indica que el fin debe ser posterior al inicio y no se emite ninguna escritura a la API

#### Scenario: Un tramo superpuesto con otro del mismo día bloquea el envío sin llamar a la API
- **GIVEN** un profesional con el tramo del lunes de 09:00 a 13:00
- **WHEN** quien edita ingresa un tramo del lunes de 12:00 a 14:00 y lo envía
- **THEN** el formulario indica que el tramo se superpone con el de 09:00 a 13:00 y no se emite ninguna escritura a la API

#### Scenario: Un tramo contiguo o de otro día se acepta
- **GIVEN** un profesional con el tramo del lunes de 09:00 a 13:00
- **WHEN** quien edita agrega el lunes de 13:00 a 17:00 o el martes de 09:00 a 13:00
- **THEN** la SPA emite el `POST` correspondiente sin mostrar error de superposición

### Requirement: Alta, modificación y eliminación de tramos
Para crear, la SPA SHALL enviar `POST /api/profesionales/{id}/horarios` con `diaSemana` de 1 (lunes) a 7 (domingo), `horaInicio` y `horaFin` en `HH:mm`. Para modificar, envía `PUT /api/profesionales/{id}/horarios/{horarioId}` con el mismo cuerpo. Para eliminar, pide confirmación y envía `DELETE` al mismo recurso. Tras cada respuesta exitosa, el panel MUST volver a consultar los tramos y anunciar el éxito. Cancelar MUST descartar sin escribir.

#### Scenario: Agregar un tramo lo envía con el día ISO y lo muestra en su día
- **GIVEN** una sesión `RECEPCION` y un profesional sin tramos
- **WHEN** agrega el miércoles de 09:00 a 13:00
- **THEN** la SPA emite `POST /api/profesionales/{id}/horarios` con `{"diaSemana":3,"horaInicio":"09:00","horaFin":"13:00"}`, vuelve a consultar los horarios, anuncia el éxito y el miércoles muestra el tramo

#### Scenario: Modificar un tramo envía PUT y refleja los nuevos valores
- **GIVEN** una sesión `ADMIN` y un profesional con el tramo del lunes de 09:00 a 13:00
- **WHEN** lo modifica a lunes de 10:00 a 14:00
- **THEN** la SPA emite `PUT /api/profesionales/{id}/horarios/{horarioId}` con los nuevos valores y el panel muestra el lunes de 10:00 a 14:00

#### Scenario: Eliminar un tramo pide confirmación y lo retira
- **GIVEN** una sesión `RECEPCION` y un profesional con el tramo del lunes de 09:00 a 13:00
- **WHEN** elige eliminarlo y confirma
- **THEN** la SPA emite `DELETE /api/profesionales/{id}/horarios/{horarioId}` y el lunes queda sin atención

#### Scenario: Cancelar la eliminación no escribe
- **WHEN** quien edita elige eliminar un tramo y cancela la confirmación
- **THEN** no se emite ninguna escritura a la API y el tramo sigue visible

### Requirement: Errores de la API al escribir tramos
El panel SHALL traducir los errores por código, sin mostrar el `detalle` del servidor. `409 HORARIO_SUPERPUESTO` muestra la superposición y vuelve a consultar los tramos. `404 HORARIO_NO_ENCONTRADO` avisa que el tramo ya no existe y vuelve a consultar. `404 PROFESIONAL_NO_ENCONTRADO` hace que la página refresque la lista de profesionales. `403` muestra permiso insuficiente sin cerrar la sesión. Cualquier otro error invita a reintentar.

#### Scenario: Un 409 del servidor muestra la superposición y recarga los tramos
- **GIVEN** que otra persona agregó un tramo superpuesto después de la última consulta
- **WHEN** quien edita envía un alta y la API responde `409` con código `HORARIO_SUPERPUESTO`
- **THEN** el panel muestra una alerta de superposición y vuelve a consultar los horarios

#### Scenario: Un tramo que ya no existe avisa y recarga
- **WHEN** quien edita modifica o elimina un tramo y la API responde `404` con código `HORARIO_NO_ENCONTRADO`
- **THEN** el panel muestra que el tramo ya no existe y vuelve a consultar los horarios

#### Scenario: Un profesional que ya no existe refresca la lista de profesionales
- **WHEN** quien edita escribe un tramo y la API responde `404` con código `PROFESIONAL_NO_ENCONTRADO`
- **THEN** la página muestra que el profesional ya no existe y vuelve a consultar `GET /api/profesionales`

#### Scenario: Un 403 al escribir un tramo muestra permiso insuficiente sin cerrar la sesión
- **WHEN** quien edita escribe un tramo y la API responde `403`
- **THEN** el panel muestra una alerta de permiso insuficiente, sin el `detalle` del servidor, y la sesión sigue activa

### Requirement: Escritura de horarios solo para ADMIN y RECEPCION
`ADMIN` y `RECEPCION` SHALL ver en el panel de horarios los controles para agregar, modificar y eliminar tramos de cualquier profesional. `ODONTOLOGO` MUST ver los tramos de su propio profesional, el único que le devuelve el listado, sin controles de escritura y sin emitir escrituras (RN-33).

#### Scenario: Recepción ve los controles de edición de horarios
- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre el panel de horarios de un profesional
- **THEN** el panel ofrece agregar un tramo y modificar o eliminar cada tramo existente

#### Scenario: El odontólogo ve sus horarios sin controles de edición
- **GIVEN** una sesión activa con rol `ODONTOLOGO` cuyo profesional tiene el tramo del lunes de 09:00 a 13:00
- **WHEN** abre el panel de horarios de su registro
- **THEN** el panel muestra el lunes de 09:00 a 13:00 sin el formulario ni botones para agregar, modificar o eliminar, y no se emite ninguna escritura a la API

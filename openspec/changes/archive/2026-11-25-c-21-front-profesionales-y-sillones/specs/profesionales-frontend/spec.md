# Spec Delta

## Purpose

Define cómo el personal consulta y administra el catálogo de profesionales desde la SPA. Valida los datos en el cliente, limita los controles de escritura al rol `ADMIN` y muestra al odontólogo solo su propio registro (US-005).

## ADDED Requirements

### Requirement: Listado de profesionales en la SPA

La pantalla `/profesionales` SHALL mostrar los profesionales que devuelve `GET /api/profesionales`, activos e inactivos, en el orden de la API. Cada fila MUST mostrar apellido y nombre, matrícula, especialidad (o una marca de vacío si falta) y estado. MUST informar cuando no hay profesionales. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El listado muestra los profesionales con su matrícula y estado

- **GIVEN** una sesión activa con rol `ADMIN` y una API que devuelve un profesional activo con especialidad y otro inactivo sin especialidad
- **WHEN** abre `/profesionales`
- **THEN** la SPA consulta `GET /api/profesionales` y la tabla muestra el apellido y nombre, la matrícula, la especialidad o la marca de vacío y el estado activo o inactivo de cada uno

#### Scenario: El listado vacío de profesionales muestra un mensaje

- **GIVEN** una API que devuelve una lista vacía de profesionales
- **WHEN** la persona abre `/profesionales`
- **THEN** la pantalla muestra un mensaje de listado vacío y ninguna fila de datos

#### Scenario: Un fallo al listar profesionales permite reintentar

- **GIVEN** una API que responde `500` al listar profesionales
- **WHEN** la persona abre `/profesionales`
- **THEN** la pantalla muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar el listado

### Requirement: Validación del formulario de profesional antes de enviar

El formulario de profesional SHALL exigir nombre, apellido y matrícula no blancos y de 255 caracteres como máximo. La especialidad es opcional, con el mismo máximo. Un valor inválido MUST impedir el envío, mostrar el mensaje junto al campo afectado y MUST NOT llamar a la API.

#### Scenario: Una matrícula en blanco bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa nombre y apellido, deja la matrícula con solo espacios y envía el formulario
- **THEN** el formulario indica junto a la matrícula que es obligatoria y no se emite ninguna escritura a la API

#### Scenario: Un nombre o apellido en blanco bloquea el envío sin llamar a la API

- **WHEN** el administrador deja en blanco el nombre y el apellido, ingresa una matrícula y envía el formulario
- **THEN** el formulario marca ambos campos como obligatorios y no se emite ninguna escritura a la API

#### Scenario: Un texto de más de 255 caracteres bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa una especialidad de 256 caracteres con los demás campos válidos y envía el formulario
- **THEN** el formulario indica el máximo de 255 caracteres junto a la especialidad y no se emite ninguna escritura a la API

### Requirement: Alta de profesional desde la SPA

El rol `ADMIN` SHALL poder dar de alta un profesional con `POST /api/profesionales`. La SPA envía nombre, apellido y matrícula recortados, y la especialidad recortada o `null` si queda vacía. Con un `201` la lista MUST refrescarse y el formulario MUST quedar limpio. Un `409` con código `PROFESIONAL_MATRICULA_DUPLICADA` SHALL mostrarse como alerta sin perder lo ingresado.

#### Scenario: El administrador da de alta un profesional y la lista se refresca

- **WHEN** el administrador envía un profesional válido sin especialidad y la API responde `201`
- **THEN** la SPA emite el `POST` con los textos recortados y `especialidad` nula, vuelve a consultar la lista, muestra el profesional nuevo y deja el formulario vacío

#### Scenario: Una matrícula duplicada se muestra como alerta sin perder los datos

- **WHEN** la API responde `409` con código `PROFESIONAL_MATRICULA_DUPLICADA` a un alta
- **THEN** la pantalla muestra una alerta de matrícula duplicada y conserva todos los datos ingresados en el formulario

### Requirement: Modificación de profesional

El rol `ADMIN` SHALL poder editar un profesional con `PUT /api/profesionales/{id}`. El formulario se precarga con los datos actuales y envía el valor actual de `activo`. Con un `200` la lista MUST refrescarse. Un `409 PROFESIONAL_MATRICULA_DUPLICADA` SHALL mostrarse como alerta sin perder lo ingresado. Un `404 PROFESIONAL_NO_ENCONTRADO` SHALL mostrarse como alerta y refrescar la lista.

#### Scenario: El administrador edita un profesional y la lista se refresca

- **WHEN** el administrador abre la edición de un profesional, cambia su especialidad y la API responde `200`
- **THEN** la SPA emite el `PUT` al id correspondiente con los datos nuevos y el `activo` actual, vuelve a consultar la lista y muestra la especialidad nueva

#### Scenario: Editar con una matrícula de otro profesional muestra una alerta

- **WHEN** la API responde `409` con código `PROFESIONAL_MATRICULA_DUPLICADA` a una edición
- **THEN** la pantalla muestra una alerta de matrícula duplicada y el formulario de edición conserva lo ingresado

#### Scenario: Editar un profesional inexistente muestra una alerta y refresca la lista

- **WHEN** la API responde `404` con código `PROFESIONAL_NO_ENCONTRADO` a una edición
- **THEN** la pantalla muestra una alerta de profesional inexistente y vuelve a consultar la lista

### Requirement: Baja lógica y reactivación de profesional

El rol `ADMIN` SHALL poder dar de baja un profesional con `PUT /api/profesionales/{id}` y `activo=false`, después de confirmarlo en la propia fila. También SHALL poder reactivarlo con `activo=true`. En ambos casos se envían los datos actuales. Tras la respuesta, la lista MUST refrescarse y el profesional MUST seguir visible con su nuevo estado. La SPA MUST NOT ofrecer borrado físico. Un error SHALL mostrarse como alerta.

#### Scenario: El administrador da de baja un profesional y sigue visible como inactivo

- **WHEN** el administrador pide la baja de un profesional activo, la confirma y la API responde `200`
- **THEN** la SPA emite el `PUT` con `activo` igual a `false` y los datos actuales, vuelve a consultar la lista y muestra el profesional como inactivo

#### Scenario: Cancelar la confirmación de baja no emite escrituras

- **WHEN** el administrador pide la baja de un profesional y luego cancela la confirmación
- **THEN** la fila vuelve a mostrar sus acciones habituales y no se emite ninguna escritura a la API

#### Scenario: El administrador reactiva un profesional dado de baja

- **WHEN** el administrador usa la acción de reactivar sobre un profesional inactivo y la API responde `200`
- **THEN** la SPA emite el `PUT` con `activo` igual a `true` y la lista refrescada lo muestra activo

#### Scenario: Un error al dar de baja un profesional se muestra como alerta

- **WHEN** la API responde a una baja con un error distinto de `401`
- **THEN** la pantalla muestra una alerta sin el `detalle` del servidor y vuelve a consultar la lista

### Requirement: Controles de escritura de profesionales según rol

Solo el rol `ADMIN` SHALL ver y usar los controles de alta, edición, baja y reactivación de profesionales. `RECEPCION` MUST consultar el listado completo en modo solo lectura. La ruta `/profesionales` SHALL estar disponible y figurar en el menú para los tres roles del personal. Un `403` en una escritura SHALL mostrarse como permiso insuficiente sin cerrar la sesión.

#### Scenario: El administrador ve los controles de escritura de profesionales

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** abre `/profesionales`
- **THEN** ve el formulario de alta y, en cada fila, un control de edición y uno de baja o de reactivación según el estado

#### Scenario: Recepción consulta los profesionales sin controles de escritura

- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre `/profesionales`
- **THEN** ve el listado completo sin formulario ni botones de alta, edición, baja o reactivación, y no se emite ninguna escritura

#### Scenario: La pantalla de profesionales aparece en el menú para los tres roles

- **GIVEN** una sesión activa con cualquiera de los roles `ADMIN`, `RECEPCION` u `ODONTOLOGO`
- **WHEN** se muestra el menú principal
- **THEN** la entrada de profesionales está presente y abre `/profesionales`

#### Scenario: Un 403 al escribir un profesional se muestra como permiso insuficiente

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** la API responde `403` a un alta de profesional
- **THEN** la pantalla muestra una alerta de permiso insuficiente, conserva lo ingresado y la sesión sigue activa

### Requirement: Vista del odontólogo sobre su propio registro

Para `ODONTOLOGO` la pantalla SHALL mostrar, sin controles de escritura, lo que devuelva `GET /api/profesionales`, que es solo su registro vinculado. Si la API responde `403` porque no tiene un profesional vinculado, la pantalla MUST mostrar un aviso informativo de falta de vínculo, distinto del error recuperable, y MUST NOT cerrar la sesión.

#### Scenario: El odontólogo vinculado ve solo su registro sin controles de escritura

- **GIVEN** una sesión activa con rol `ODONTOLOGO` y una API que devuelve un único profesional
- **WHEN** abre `/profesionales`
- **THEN** ve ese profesional sin formulario ni botones de escritura, y no se emite ninguna escritura

#### Scenario: El odontólogo sin vínculo ve un aviso de falta de vínculo

- **GIVEN** una sesión activa con rol `ODONTOLOGO` y una API que responde `403` al listar profesionales
- **WHEN** abre `/profesionales`
- **THEN** la pantalla muestra un aviso de que su usuario no tiene un profesional vinculado, sin botón de reintento ni tabla, y la sesión sigue activa

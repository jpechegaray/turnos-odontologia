# Spec Delta

## Purpose

Define cómo el personal consulta y administra los sillones o boxes del consultorio desde la SPA, con validación local del nombre y controles de escritura limitados al rol `ADMIN` (US-004).

## ADDED Requirements

### Requirement: Listado de sillones en la SPA

La pantalla `/sillones` SHALL mostrar todos los sillones que devuelve `GET /api/sillones`, activos e inactivos, ordenados por nombre. Cada fila MUST mostrar el nombre, la descripción (o una marca de vacío si falta) y el estado. MUST informar cuando no hay sillones. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El listado muestra los sillones con su descripción y estado

- **GIVEN** una sesión activa y una API que devuelve "Sillón 2" activo con descripción y "Sillón 1" inactivo sin descripción
- **WHEN** abre `/sillones`
- **THEN** la SPA consulta `GET /api/sillones` y la tabla muestra "Sillón 1" antes que "Sillón 2", cada uno con su descripción o la marca de vacío y su estado activo o inactivo

#### Scenario: El listado vacío de sillones muestra un mensaje

- **GIVEN** una API que devuelve una lista vacía de sillones
- **WHEN** la persona abre `/sillones`
- **THEN** la pantalla muestra un mensaje de listado vacío y ninguna fila de datos

#### Scenario: Un fallo al listar sillones permite reintentar

- **GIVEN** una API que responde `500` al listar sillones
- **WHEN** la persona abre `/sillones`
- **THEN** la pantalla muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar el listado

### Requirement: Validación del formulario de sillón antes de enviar

El formulario de sillón SHALL exigir un nombre no blanco y admitir una descripción opcional. Un nombre en blanco MUST impedir el envío, mostrar el mensaje junto al campo y MUST NOT llamar a la API.

#### Scenario: Un nombre de sillón en blanco bloquea el envío sin llamar a la API

- **WHEN** el administrador deja el nombre con solo espacios, ingresa una descripción y envía el formulario
- **THEN** el formulario indica junto al nombre que es obligatorio y no se emite ninguna escritura a la API

### Requirement: Alta de sillón desde la SPA

El rol `ADMIN` SHALL poder dar de alta un sillón con `POST /api/sillones`, enviando el nombre recortado y la descripción recortada, o `null` si queda vacía. Con un `201` la lista MUST refrescarse y el formulario MUST quedar limpio. Un `409` con código `SILLON_NOMBRE_DUPLICADO` SHALL mostrarse como alerta sin perder lo ingresado.

#### Scenario: El administrador da de alta un sillón y la lista se refresca

- **WHEN** el administrador envía el nombre " Box 3 " sin descripción y la API responde `201`
- **THEN** la SPA emite el `POST` con nombre "Box 3" y `descripcion` nula, vuelve a consultar la lista, muestra el sillón nuevo y deja el formulario vacío

#### Scenario: Un nombre de sillón duplicado se muestra como alerta sin perder los datos

- **WHEN** la API responde `409` con código `SILLON_NOMBRE_DUPLICADO` a un alta
- **THEN** la pantalla muestra una alerta de nombre de sillón duplicado y conserva el nombre y la descripción ingresados

### Requirement: Modificación de sillón

El rol `ADMIN` SHALL poder editar el nombre y la descripción de un sillón con `PUT /api/sillones/{id}`. El formulario se precarga con los datos actuales y envía el valor actual de `activo`. Con un `200` la lista MUST refrescarse. Un `409 SILLON_NOMBRE_DUPLICADO` SHALL mostrarse como alerta sin perder lo ingresado. Un `404 SILLON_NO_ENCONTRADO` SHALL mostrarse como alerta y refrescar la lista.

#### Scenario: El administrador edita un sillón y la lista se refresca

- **WHEN** el administrador abre la edición de un sillón, cambia su descripción y la API responde `200`
- **THEN** la SPA emite el `PUT` al id correspondiente con el nombre, la descripción nueva y el `activo` actual, vuelve a consultar la lista y muestra la descripción nueva

#### Scenario: Editar un sillón inexistente muestra una alerta y refresca la lista

- **WHEN** la API responde `404` con código `SILLON_NO_ENCONTRADO` a una edición
- **THEN** la pantalla muestra una alerta de sillón inexistente y vuelve a consultar la lista

### Requirement: Baja lógica y reactivación de sillón

El rol `ADMIN` SHALL poder dar de baja un sillón con `PUT /api/sillones/{id}` y `activo=false`, después de confirmarlo en la propia fila. También SHALL poder reactivarlo con `activo=true`. En ambos casos se envían el nombre y la descripción actuales. Tras la respuesta la lista MUST refrescarse y el sillón MUST seguir visible con su nuevo estado. La SPA MUST NOT ofrecer borrado físico. Un error SHALL mostrarse como alerta.

#### Scenario: El administrador da de baja un sillón y sigue visible como inactivo

- **WHEN** el administrador pide la baja de un sillón activo, la confirma y la API responde `200`
- **THEN** la SPA emite el `PUT` con `activo` igual a `false` y los datos actuales, vuelve a consultar la lista y muestra el sillón como inactivo

#### Scenario: El administrador reactiva un sillón dado de baja

- **WHEN** el administrador usa la acción de reactivar sobre un sillón inactivo y la API responde `200`
- **THEN** la SPA emite el `PUT` con `activo` igual a `true` y la lista refrescada lo muestra activo

#### Scenario: Un error al dar de baja un sillón se muestra como alerta

- **WHEN** la API responde a una baja de sillón con un error distinto de `401`
- **THEN** la pantalla muestra una alerta sin el `detalle` del servidor y vuelve a consultar la lista

### Requirement: Controles de escritura de sillones según rol

Solo el rol `ADMIN` SHALL ver y usar los controles de alta, edición, baja y reactivación de sillones. `RECEPCION` y `ODONTOLOGO` MUST consultar el listado en modo solo lectura. La ruta `/sillones` SHALL estar disponible y figurar en el menú para los tres roles del personal. Un `403` en una escritura SHALL mostrarse como permiso insuficiente sin cerrar la sesión.

#### Scenario: El administrador ve los controles de escritura de sillones

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** abre `/sillones`
- **THEN** ve el formulario de alta y, en cada fila, un control de edición y uno de baja o de reactivación según el estado

#### Scenario: Recepción y odontología consultan los sillones sin controles de escritura

- **GIVEN** una sesión activa con rol `RECEPCION` u `ODONTOLOGO`
- **WHEN** abre `/sillones`
- **THEN** ve el listado sin formulario ni botones de alta, edición, baja o reactivación, y no se emite ninguna escritura

#### Scenario: La pantalla de sillones aparece en el menú para los tres roles

- **GIVEN** una sesión activa con cualquiera de los roles `ADMIN`, `RECEPCION` u `ODONTOLOGO`
- **WHEN** se muestra el menú principal
- **THEN** la entrada de sillones está presente y abre `/sillones`

#### Scenario: Un 403 al escribir un sillón se muestra como permiso insuficiente

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** la API responde `403` a un alta de sillón
- **THEN** la pantalla muestra una alerta de permiso insuficiente, conserva lo ingresado y la sesión sigue activa

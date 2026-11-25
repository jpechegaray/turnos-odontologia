# usuarios-frontend Specification

## Purpose

Define cómo el administrador gestiona desde la SPA las cuentas del personal (alta, edición, rol, vínculo del odontólogo con su profesional, desactivación y reactivación), con validación local y la sección restringida al rol `ADMIN` (US-002, RN-33, RN-34).

## Requirements

### Requirement: Sección de usuarios solo para el administrador

La pantalla `/usuarios` SHALL figurar en el menú solo para el rol `ADMIN`. Si `RECEPCION` u `ODONTOLOGO` entran por URL a `/usuarios`, la SPA MUST mostrar el aviso de permiso insuficiente y MUST NOT consultar `/api/usuarios`.

#### Scenario: La sección de usuarios aparece en el menú solo para el administrador

- **GIVEN** una sesión activa
- **WHEN** la persona tiene rol `ADMIN`, `RECEPCION` u `ODONTOLOGO`
- **THEN** el menú muestra «Usuarios» solo para `ADMIN`

#### Scenario: Recepción o el odontólogo no acceden a usuarios por URL

- **GIVEN** una sesión activa con rol `RECEPCION` u `ODONTOLOGO`
- **WHEN** navega directamente a `/usuarios`
- **THEN** la pantalla muestra el aviso de permiso insuficiente y no se emite ninguna petición a `/api/usuarios`

### Requirement: Listado de usuarios en la SPA

La pantalla `/usuarios` SHALL mostrar los usuarios que devuelve `GET /api/usuarios` en el orden de la API (por email). Cada fila MUST mostrar email, nombre, rol, el profesional vinculado («Apellido, Nombre») o una marca de vacío, y el estado. MUST informar cuando no hay usuarios. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El listado muestra los usuarios con su rol, vínculo y estado

- **GIVEN** una sesión `ADMIN` y una API que devuelve un usuario `ODONTOLOGO` activo vinculado a un profesional y un usuario `RECEPCION` inactivo
- **WHEN** abre `/usuarios`
- **THEN** la SPA consulta `GET /api/usuarios` y `GET /api/profesionales`, y la tabla muestra cada usuario con su email, nombre, rol, el nombre del profesional o la marca de vacío, y su estado activo o inactivo

#### Scenario: El listado vacío de usuarios muestra un mensaje

- **GIVEN** una API que devuelve una lista vacía de usuarios
- **WHEN** el administrador abre `/usuarios`
- **THEN** la pantalla muestra un mensaje de listado vacío y ninguna fila de datos

#### Scenario: Un fallo al listar usuarios permite reintentar

- **GIVEN** una API que responde `500` al listar usuarios
- **WHEN** el administrador abre `/usuarios`
- **THEN** la pantalla muestra una alerta de error y un botón para reintentar, y al usarlo vuelve a consultar el listado

### Requirement: Validación del formulario de usuario antes de enviar

El formulario SHALL exigir un email con formato válido de hasta 254 caracteres, un nombre no blanco de hasta 255 y un rol. En el alta MUST exigir una contraseña de al menos 8 caracteres y como máximo 72 bytes en UTF-8. Con rol `ODONTOLOGO` MUST exigir un profesional. Un dato inválido MUST impedir el envío, mostrar el mensaje junto al campo y MUST NOT llamar a la API.

#### Scenario: Un email inválido o un nombre en blanco bloquean el envío sin llamar a la API

- **WHEN** el administrador ingresa un email sin formato válido o deja el nombre con solo espacios y envía el formulario
- **THEN** el formulario indica el error junto al campo correspondiente y no se emite ninguna escritura a la API

#### Scenario: Una contraseña fuera de largo bloquea el alta sin llamar a la API

- **WHEN** el administrador da de alta un usuario con una contraseña de 7 caracteres o de más de 72 bytes
- **THEN** el formulario indica el error junto a la contraseña y no se emite ninguna escritura a la API

#### Scenario: Un odontólogo sin profesional bloquea el envío sin llamar a la API

- **WHEN** el administrador elige el rol `ODONTOLOGO`, no elige profesional y envía el formulario
- **THEN** el formulario indica junto al profesional que es obligatorio y no se emite ninguna escritura a la API

### Requirement: Selección del profesional vinculado

El selector de profesional SHALL mostrarse solo con el rol `ODONTOLOGO` y ofrecer los profesionales activos sin otro usuario vinculado; al editar MUST incluir también el profesional vinculado al usuario editado. Al cambiar a un rol distinto de `ODONTOLOGO` el vínculo MUST descartarse y enviarse `profesionalId` nulo.

#### Scenario: El selector ofrece solo profesionales activos y libres

- **GIVEN** un profesional activo sin usuario, un profesional activo ya vinculado a otro usuario y un profesional inactivo
- **WHEN** el administrador elige el rol `ODONTOLOGO` en el alta
- **THEN** el selector ofrece solo el profesional activo sin usuario

#### Scenario: Cambiar a un rol que no es odontólogo descarta el vínculo

- **GIVEN** el formulario con rol `ODONTOLOGO` y un profesional elegido
- **WHEN** el administrador cambia el rol a `RECEPCION` y envía
- **THEN** el selector de profesional desaparece y la petición lleva `profesionalId` nulo

### Requirement: Alta de usuario desde la SPA

El rol `ADMIN` SHALL dar de alta un usuario con `POST /api/usuarios`, enviando email y nombre recortados, rol, contraseña tal como se ingresó y `profesionalId`. Con un `201` la lista MUST refrescarse y el formulario MUST quedar limpio, incluida la contraseña. La contraseña MUST NOT mostrarse en la lista, en alertas ni en avisos.

#### Scenario: El administrador da de alta un usuario de recepción y la lista se refresca

- **WHEN** el administrador envía email " recepcion2@consultorio.test ", un nombre, rol `RECEPCION` y una contraseña ficticia válida, y la API responde `201`
- **THEN** la SPA emite el `POST` con el email recortado, `profesionalId` nulo y la contraseña, vuelve a consultar la lista, muestra el usuario nuevo y deja el formulario vacío

#### Scenario: El administrador da de alta un odontólogo vinculado a su profesional

- **WHEN** el administrador elige rol `ODONTOLOGO`, un profesional libre y completa los demás datos, y la API responde `201`
- **THEN** la SPA emite el `POST` con el `profesionalId` elegido y la fila nueva muestra ese profesional

### Requirement: Errores de negocio de usuarios como alertas

La SPA SHALL traducir los errores por su código, sin mostrar el `detalle` ni repetir la contraseña, y conservar lo ingresado salvo la contraseña. Cubre `USUARIO_EMAIL_DUPLICADO`, `USUARIO_VINCULO_INVALIDO`, `USUARIO_PROFESIONAL_YA_VINCULADO`, `USUARIO_ULTIMO_ADMIN`, `USUARIO_NO_ENCONTRADO` (refresca la lista), el `400` de contraseña (marca el campo) y el `403`.

#### Scenario: Un email duplicado se muestra como alerta sin perder los datos

- **WHEN** la API responde `409` con código `USUARIO_EMAIL_DUPLICADO` a un alta
- **THEN** la pantalla muestra una alerta de email ya usado y conserva el email, el nombre, el rol y el profesional ingresados

#### Scenario: Un profesional ya vinculado se muestra como alerta

- **WHEN** la API responde `409` con código `USUARIO_PROFESIONAL_YA_VINCULADO`
- **THEN** la pantalla muestra una alerta de profesional ya vinculado a otro usuario y vuelve a consultar los profesionales

#### Scenario: Un vínculo inválido se muestra como alerta

- **WHEN** la API responde `422` con código `USUARIO_VINCULO_INVALIDO`
- **THEN** la pantalla muestra una alerta de vínculo con profesional inválido sin mostrar el `detalle` del servidor

#### Scenario: Una contraseña rechazada por la API marca el campo

- **WHEN** la API responde `400` en formato Problem Details indicando el campo `password`
- **THEN** el formulario marca el error junto a la contraseña, sin repetir su valor

#### Scenario: Un 403 al escribir un usuario se muestra como permiso insuficiente

- **WHEN** la API responde `403` a una escritura de usuario
- **THEN** la pantalla muestra una alerta de permiso insuficiente, conserva los datos y la sesión sigue activa

### Requirement: Modificación de usuario

El rol `ADMIN` SHALL editar email, nombre, rol y vínculo de un usuario con `PUT /api/usuarios/{id}`, precargando los datos actuales y enviando su `activo` actual. El formulario de edición MUST NOT incluir contraseña. Con un `200` la lista MUST refrescarse. Un `404 USUARIO_NO_ENCONTRADO` SHALL mostrarse como alerta y refrescar la lista.

#### Scenario: El administrador cambia el rol de un usuario

- **GIVEN** un usuario `RECEPCION` activo
- **WHEN** el administrador abre su edición, cambia el rol a `ADMIN` y la API responde `200`
- **THEN** la SPA emite el `PUT` al id correspondiente con rol `ADMIN`, `profesionalId` nulo y `activo` verdadero, sin campo de contraseña, y la lista muestra el rol nuevo

#### Scenario: Editar un usuario inexistente muestra una alerta y refresca la lista

- **WHEN** la API responde `404` con código `USUARIO_NO_ENCONTRADO` a una edición
- **THEN** la pantalla muestra una alerta de usuario inexistente y vuelve a consultar la lista

### Requirement: Desactivación y reactivación de usuario

El rol `ADMIN` SHALL desactivar un usuario con `DELETE /api/usuarios/{id}`, después de confirmarlo en la fila, y reactivarlo con `PUT /api/usuarios/{id}` con sus datos actuales y `activo=true`. Tras la respuesta la lista MUST refrescarse y el usuario MUST seguir visible con su nuevo estado. Un `409 USUARIO_ULTIMO_ADMIN` SHALL mostrarse como alerta y el usuario sigue activo.

#### Scenario: El administrador desactiva un usuario y sigue visible como inactivo

- **WHEN** el administrador confirma la desactivación de un usuario activo y la API responde `204`
- **THEN** la SPA emite `DELETE /api/usuarios/{id}`, vuelve a consultar la lista y el usuario aparece inactivo

#### Scenario: Cancelar la confirmación de desactivación no emite escrituras

- **WHEN** el administrador pide desactivar un usuario y cancela la confirmación
- **THEN** no se emite ninguna escritura a la API y el usuario sigue activo

#### Scenario: El administrador reactiva un usuario desactivado

- **WHEN** el administrador reactiva un usuario inactivo y la API responde `200`
- **THEN** la SPA emite el `PUT` con sus datos actuales y `activo` verdadero, y la lista lo muestra activo

#### Scenario: No se puede desactivar al último administrador activo

- **WHEN** la API responde `409` con código `USUARIO_ULTIMO_ADMIN` a una desactivación o a un cambio de rol
- **THEN** la pantalla muestra una alerta que explica que tiene que quedar al menos un administrador activo y la lista lo sigue mostrando activo con rol `ADMIN`

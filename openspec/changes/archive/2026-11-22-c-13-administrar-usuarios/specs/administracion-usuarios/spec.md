# Spec Delta

## Purpose

Define cómo el administrador del consultorio gestiona las cuentas del personal (alta, modificación, desactivación, rol y vínculo del odontólogo con su profesional) sin exponer credenciales, y cómo los demás módulos obtienen ese vínculo (US-002, RN-33, RN-34).

## ADDED Requirements

### Requirement: Alta de usuario
El administrador SHALL poder dar de alta un usuario con email, nombre, rol (`ODONTOLOGO`, `RECEPCION` o `ADMIN`), contraseña inicial y, si el rol es `ODONTOLOGO`, el profesional vinculado. El email MUST guardarse en minúsculas y sin espacios en los extremos. La contraseña MUST guardarse solo como hash BCrypt. El usuario nuevo queda activo y la respuesta MUST ser 201.

#### Scenario: El administrador da de alta un usuario de recepción
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** envía `POST /api/usuarios` con email "Recepcion2@Consultorio.Test", nombre, rol `RECEPCION` y una contraseña ficticia válida
- **THEN** la respuesta tiene estado 201 y cabecera `Location`
- **AND** el cuerpo incluye `id`, email "recepcion2@consultorio.test", nombre, rol `RECEPCION`, `activo` igual a `true` y `profesionalId` nulo

#### Scenario: El usuario dado de alta puede iniciar sesión con su contraseña
- **GIVEN** un usuario recién dado de alta con una contraseña ficticia
- **WHEN** inicia sesión con ese email y esa contraseña
- **THEN** el login responde 200 con su rol

#### Scenario: La contraseña se guarda solo como hash BCrypt
- **WHEN** el administrador da de alta un usuario
- **THEN** lo almacenado es un hash BCrypt que verifica la contraseña, y nunca la contraseña en claro

#### Scenario: Una contraseña fuera de largo responde 400
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** da de alta un usuario con una contraseña de menos de 8 caracteres o de más de 72 bytes
- **THEN** la respuesta tiene estado 400 en formato Problem Details e indica el campo `password`, sin repetir su valor

### Requirement: Email único
El email SHALL ser único sin distinguir mayúsculas, contando también los usuarios inactivos. Un alta o una modificación con un email ya usado por otro usuario MUST responder 409 con código `USUARIO_EMAIL_DUPLICADO`.

#### Scenario: Un email repetido con otra capitalización responde 409
- **GIVEN** un usuario existente con email "odonto1@consultorio.test"
- **WHEN** el administrador da de alta otro usuario con email "ODONTO1@consultorio.test"
- **THEN** la respuesta tiene estado 409 y `codigo` igual a `USUARIO_EMAIL_DUPLICADO`

### Requirement: Vínculo del odontólogo con su profesional
Un usuario con rol `ODONTOLOGO` SHALL estar vinculado a exactamente un profesional existente y activo, y un profesional MUST tener a lo sumo un usuario vinculado. Los roles `RECEPCION` y `ADMIN` MUST NOT tener vínculo. Un vínculo faltante o sobrante, o un profesional inexistente o inactivo, MUST responder 422 con código `USUARIO_VINCULO_INVALIDO`. Un profesional ya vinculado a otro usuario MUST responder 409 con código `USUARIO_PROFESIONAL_YA_VINCULADO`.

#### Scenario: Un odontólogo se da de alta vinculado a su profesional
- **GIVEN** un profesional activo sin usuario
- **WHEN** el administrador da de alta un usuario `ODONTOLOGO` con ese `profesionalId`
- **THEN** la respuesta tiene estado 201 y el cuerpo incluye ese `profesionalId`

#### Scenario: Un odontólogo sin profesional responde 422
- **WHEN** el administrador da de alta un usuario `ODONTOLOGO` sin `profesionalId`
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `USUARIO_VINCULO_INVALIDO`

#### Scenario: Un rol que no es odontólogo con profesional responde 422
- **WHEN** el administrador da de alta un usuario `RECEPCION` con un `profesionalId`
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `USUARIO_VINCULO_INVALIDO`

#### Scenario: Un profesional inexistente o inactivo responde 422
- **WHEN** el administrador vincula un odontólogo a un profesional que no existe o está inactivo
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `USUARIO_VINCULO_INVALIDO`

#### Scenario: Un profesional ya vinculado responde 409
- **GIVEN** un profesional ya vinculado a un usuario
- **WHEN** el administrador vincula otro usuario `ODONTOLOGO` a ese mismo profesional
- **THEN** la respuesta tiene estado 409 y `codigo` igual a `USUARIO_PROFESIONAL_YA_VINCULADO`

### Requirement: Modificación de usuario
El administrador SHALL poder modificar el email, el nombre, el rol, el vínculo con el profesional y el estado activo de un usuario existente, con las mismas validaciones del alta. La contraseña MUST NOT cambiarse por esta vía. Modificar un usuario inexistente MUST responder 404 con código `USUARIO_NO_ENCONTRADO`.

#### Scenario: El administrador cambia el rol de un usuario
- **GIVEN** un usuario `RECEPCION`
- **WHEN** el administrador envía `PUT /api/usuarios/{id}` con rol `ADMIN`, sin vínculo y `activo` igual a `true`
- **THEN** la respuesta tiene estado 200 con el rol `ADMIN`

#### Scenario: Modificar un usuario inexistente responde 404
- **WHEN** el administrador envía `PUT /api/usuarios/{id}` con un id que no existe
- **THEN** la respuesta tiene estado 404 y `codigo` igual a `USUARIO_NO_ENCONTRADO`

### Requirement: Desactivación de usuario
Desactivar un usuario SHALL marcarlo inactivo sin borrarlo, responder 204 y ser inocuo si se repite. Un usuario inactivo MUST NOT poder iniciar sesión (RN-34). El administrador MUST poder reactivarlo modificándolo con `activo` igual a `true`. Desactivar un usuario inexistente MUST responder 404.

#### Scenario: Un usuario desactivado no puede iniciar sesión
- **GIVEN** un usuario activo con contraseña conocida
- **WHEN** el administrador envía `DELETE /api/usuarios/{id}` y luego el usuario intenta iniciar sesión
- **THEN** la desactivación responde 204 y el login responde 401 con el mensaje genérico de siempre

#### Scenario: Repetir la desactivación responde 204
- **GIVEN** un usuario ya inactivo
- **WHEN** el administrador vuelve a desactivarlo
- **THEN** la respuesta tiene estado 204

#### Scenario: Un usuario reactivado vuelve a iniciar sesión
- **GIVEN** un usuario inactivo
- **WHEN** el administrador lo modifica con `activo` igual a `true` y el usuario inicia sesión
- **THEN** el login responde 200

### Requirement: Siempre queda un administrador activo
El sistema SHALL impedir que quede sin administradores activos: desactivar al último `ADMIN` activo, o cambiarle el rol, MUST responder 409 con código `USUARIO_ULTIMO_ADMIN` sin aplicar cambios.

#### Scenario: No se puede desactivar al último administrador activo
- **GIVEN** un único usuario `ADMIN` activo
- **WHEN** se intenta desactivarlo o cambiarle el rol a `RECEPCION`
- **THEN** la respuesta tiene estado 409, `codigo` igual a `USUARIO_ULTIMO_ADMIN`, y sigue activo con rol `ADMIN`

### Requirement: Consulta de usuarios sin credenciales
El administrador SHALL poder listar los usuarios ordenados por email y consultar uno por id, con id, email, nombre, rol, `profesionalId` y `activo`. Ninguna respuesta de `/api/usuarios` MUST incluir la contraseña, su hash, los intentos fallidos ni el fin del bloqueo. Consultar un id inexistente MUST responder 404.

#### Scenario: Nunca se devuelve el hash de la contraseña
- **GIVEN** usuarios existentes
- **WHEN** el administrador lista los usuarios y consulta uno
- **THEN** ninguna respuesta incluye `password`, `passwordHash`, `intentosFallidos` ni `bloqueadoHasta`

#### Scenario: El listado se ordena por email
- **GIVEN** usuarios "b@consultorio.test" y "a@consultorio.test"
- **WHEN** el administrador lista los usuarios
- **THEN** "a@consultorio.test" aparece antes que "b@consultorio.test"

#### Scenario: Consultar un usuario inexistente responde 404
- **WHEN** el administrador consulta un id que no existe
- **THEN** la respuesta tiene estado 404 y `codigo` igual a `USUARIO_NO_ENCONTRADO`

### Requirement: Solo el administrador gestiona usuarios
Toda operación de `/api/usuarios`, incluidas las lecturas, SHALL estar reservada al rol `ADMIN`. `RECEPCION` y `ODONTOLOGO` MUST recibir 403 en formato Problem Details sin cambios ni datos, y una petición sin sesión MUST recibir 401 (RN-33).

#### Scenario: Recepción no accede a la gestión de usuarios
- **GIVEN** un usuario con rol `RECEPCION`
- **WHEN** lista, consulta, da de alta, modifica o desactiva usuarios
- **THEN** cada respuesta tiene estado 403 en formato Problem Details y no hay cambios

#### Scenario: El odontólogo no accede a la gestión de usuarios
- **GIVEN** un usuario con rol `ODONTOLOGO`
- **WHEN** lista, consulta, da de alta, modifica o desactiva usuarios
- **THEN** cada respuesta tiene estado 403 en formato Problem Details y no hay cambios

#### Scenario: Sin sesión la gestión de usuarios responde 401
- **WHEN** un cliente sin sesión envía `GET /api/usuarios`
- **THEN** la respuesta tiene estado 401

### Requirement: Vínculo disponible para otros módulos
El módulo de seguridad SHALL resolver, a partir de la identidad de la sesión, el profesional vinculado al usuario autenticado, para que otros módulos limiten al odontólogo a sus propios datos. Un usuario sin vínculo, inactivo o inexistente MUST resolverse como sin profesional.

#### Scenario: Un odontólogo vinculado ve solo su propio profesional
- **GIVEN** un usuario `ODONTOLOGO` vinculado a un profesional y con sesión iniciada
- **WHEN** consulta `GET /api/profesionales`
- **THEN** la respuesta tiene estado 200 y solo incluye su profesional

#### Scenario: Una identidad sin vínculo se resuelve sin profesional
- **WHEN** se resuelve el vínculo de un usuario `RECEPCION`, de un usuario inactivo o de una identidad inexistente
- **THEN** el resultado es que no tiene profesional

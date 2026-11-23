# Spec Delta

## Purpose

Define el catálogo de prestaciones del consultorio: qué tratamientos se ofrecen y cuánto dura cada uno por defecto. Es la base para calcular la duración de los turnos (RN-03) y para que agenda, lista de espera y reserva online solo usen prestaciones activas.

## ADDED Requirements

### Requirement: Alta de prestación
El administrador SHALL poder dar de alta una prestación con nombre y duración por defecto en minutos. La prestación nueva MUST quedar activa. La respuesta MUST ser 201 con el id asignado, el nombre, la duración y el estado activo.

#### Scenario: El administrador da de alta una prestación válida
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** envía `POST /api/prestaciones` con nombre "Limpieza" y duración por defecto 45
- **THEN** la respuesta tiene estado 201 y cabecera `Location` hacia la prestación creada
- **AND** el cuerpo incluye un `id`, el nombre "Limpieza", la duración 45 y `activa` igual a `true`

#### Scenario: Un nombre vacío responde 400
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** envía `POST /api/prestaciones` con el nombre vacío o sin duración
- **THEN** la respuesta tiene estado 400 en formato Problem Details y lista los campos inválidos

### Requirement: Duración por defecto válida
La duración por defecto SHALL ser un entero de minutos, múltiplo de 5, entre 5 y 480 inclusive (RN-09). Una duración fuera de regla, al dar de alta o al modificar, MUST responder 422 con código `RN-09` y no guardar cambios.

#### Scenario: Una duración no múltiplo de 5 responde 422
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** da de alta una prestación con duración 7
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `RN-09`
- **AND** la prestación no se crea

#### Scenario: Una duración mayor a 480 responde 422
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** da de alta una prestación con duración 481
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `RN-09`

#### Scenario: Una duración menor a 5 responde 422
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** da de alta una prestación con duración 0
- **THEN** la respuesta tiene estado 422 y `codigo` igual a `RN-09`

#### Scenario: Las duraciones límite 5 y 480 se aceptan
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** da de alta una prestación con duración 5 y otra con duración 480
- **THEN** ambas respuestas tienen estado 201

### Requirement: Nombre de prestación único
El nombre de una prestación SHALL ser único sin distinguir mayúsculas ni espacios al inicio o al final, contando también las prestaciones inactivas. Un alta o modificación con un nombre ya usado por otra prestación MUST responder 409 con código `PRESTACION-DUPLICADA`.

#### Scenario: Un nombre repetido responde 409
- **GIVEN** una prestación existente llamada "Limpieza"
- **WHEN** el administrador da de alta otra prestación llamada " limpieza "
- **THEN** la respuesta tiene estado 409 y `codigo` igual a `PRESTACION-DUPLICADA`

#### Scenario: Modificar una prestación conservando su propio nombre se acepta
- **GIVEN** una prestación existente llamada "Limpieza"
- **WHEN** el administrador la modifica manteniendo el nombre "Limpieza" y cambiando la duración a 60
- **THEN** la respuesta tiene estado 200 y la duración queda en 60

### Requirement: Modificación de prestación
El administrador SHALL poder modificar el nombre, la duración por defecto y el estado activo de una prestación existente, lo que también permite reactivar una prestación dada de baja. Modificar una prestación inexistente MUST responder 404 con código `PRESTACION-NO-ENCONTRADA`.

#### Scenario: El administrador modifica una prestación
- **GIVEN** una prestación "Consulta" de 30 minutos
- **WHEN** el administrador envía `PUT /api/prestaciones/{id}` con nombre "Consulta inicial", duración 40 y `activa` igual a `true`
- **THEN** la respuesta tiene estado 200 con los datos nuevos

#### Scenario: El administrador reactiva una prestación dada de baja
- **GIVEN** una prestación inactiva
- **WHEN** el administrador la modifica con `activa` igual a `true`
- **THEN** la prestación vuelve a aparecer al listar con `soloActivas`

#### Scenario: Modificar una prestación inexistente responde 404
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** envía `PUT /api/prestaciones/{id}` con un id que no existe
- **THEN** la respuesta tiene estado 404 y `codigo` igual a `PRESTACION-NO-ENCONTRADA`

### Requirement: Baja lógica de prestación
Dar de baja una prestación SHALL marcarla como inactiva sin eliminarla, para conservar su historial (RN-37). La respuesta MUST ser 204 y repetir la baja de una prestación ya inactiva MUST ser inocuo. Dar de baja una prestación inexistente MUST responder 404.

#### Scenario: La baja deja la prestación inactiva pero consultable
- **GIVEN** una prestación activa
- **WHEN** el administrador envía `DELETE /api/prestaciones/{id}`
- **THEN** la respuesta tiene estado 204
- **AND** `GET /api/prestaciones/{id}` sigue devolviendo la prestación con `activa` igual a `false`

#### Scenario: Repetir la baja responde 204
- **GIVEN** una prestación ya inactiva
- **WHEN** el administrador vuelve a darla de baja
- **THEN** la respuesta tiene estado 204

#### Scenario: Dar de baja una prestación inexistente responde 404
- **GIVEN** un usuario con rol `ADMIN`
- **WHEN** envía `DELETE /api/prestaciones/{id}` con un id que no existe
- **THEN** la respuesta tiene estado 404 y `codigo` igual a `PRESTACION-NO-ENCONTRADA`

### Requirement: Consulta del catálogo
El personal autenticado SHALL poder listar las prestaciones ordenadas por nombre y consultar una por id. Con el parámetro `soloActivas=true` el listado MUST excluir las inactivas; sin él MUST incluirlas. Consultar un id inexistente MUST responder 404.

#### Scenario: Una prestación inactiva no aparece con soloActivas
- **GIVEN** una prestación activa "Consulta" y una inactiva "Blanqueamiento"
- **WHEN** un usuario autenticado envía `GET /api/prestaciones?soloActivas=true`
- **THEN** la respuesta incluye "Consulta" y no incluye "Blanqueamiento"

#### Scenario: Sin filtro el listado incluye las inactivas ordenadas por nombre
- **GIVEN** una prestación activa "Limpieza" y una inactiva "Blanqueamiento"
- **WHEN** un usuario autenticado envía `GET /api/prestaciones`
- **THEN** la respuesta incluye ambas, con "Blanqueamiento" antes que "Limpieza"

#### Scenario: Consultar una prestación inexistente responde 404
- **GIVEN** un usuario autenticado
- **WHEN** envía `GET /api/prestaciones/{id}` con un id que no existe
- **THEN** la respuesta tiene estado 404 y `codigo` igual a `PRESTACION-NO-ENCONTRADA`

### Requirement: Permisos del catálogo de prestaciones
Solo el rol `ADMIN` SHALL dar de alta, modificar o dar de baja prestaciones. Los roles `RECEPCION` y `ODONTOLOGO` MUST poder leer el catálogo y MUST recibir 403 en formato Problem Details al intentar escribir (RN-33). Sin sesión, toda operación MUST recibir 401.

#### Scenario: Recepción lee el catálogo
- **GIVEN** un usuario con rol `RECEPCION`
- **WHEN** envía `GET /api/prestaciones`
- **THEN** la respuesta tiene estado 200

#### Scenario: El odontólogo lee el catálogo
- **GIVEN** un usuario con rol `ODONTOLOGO`
- **WHEN** envía `GET /api/prestaciones`
- **THEN** la respuesta tiene estado 200

#### Scenario: Recepción no puede escribir prestaciones
- **GIVEN** un usuario con rol `RECEPCION`
- **WHEN** intenta dar de alta, modificar o dar de baja una prestación
- **THEN** cada respuesta tiene estado 403 en formato Problem Details y no hay cambios

#### Scenario: El odontólogo no puede escribir prestaciones
- **GIVEN** un usuario con rol `ODONTOLOGO`
- **WHEN** intenta dar de alta, modificar o dar de baja una prestación
- **THEN** cada respuesta tiene estado 403 en formato Problem Details y no hay cambios

#### Scenario: Sin sesión el catálogo responde 401
- **GIVEN** un cliente sin sesión
- **WHEN** envía `GET /api/prestaciones`
- **THEN** la respuesta tiene estado 401

### Requirement: Consulta de prestaciones para otros módulos
El catálogo SHALL ofrecer a los demás módulos una consulta de solo lectura que, dado un id, devuelva el nombre, la duración por defecto y si la prestación está activa, o informe que no existe. Los demás módulos MUST usar esta consulta en lugar de acceder al almacenamiento del catálogo.

#### Scenario: Otro módulo consulta una prestación existente
- **GIVEN** una prestación inactiva "Blanqueamiento" de 60 minutos
- **WHEN** otro módulo la consulta por id
- **THEN** obtiene el nombre "Blanqueamiento", la duración 60 y que no está activa

#### Scenario: Otro módulo consulta una prestación inexistente
- **WHEN** otro módulo consulta un id que no existe
- **THEN** la consulta informa que la prestación no existe, sin lanzar un error

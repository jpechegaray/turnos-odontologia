# Spec Delta

## Purpose

Define cómo el personal consulta y administra el catálogo de prestaciones desde la SPA, con validación local de la duración (RN-09) y controles de escritura limitados al rol `ADMIN` (US-003).

## ADDED Requirements

### Requirement: Listado del catálogo en la SPA

La pantalla `/prestaciones` SHALL mostrar todas las prestaciones, activas e inactivas, ordenadas por nombre, con nombre, duración por defecto en minutos y estado. MUST informar cuando el catálogo está vacío. Un fallo al consultar SHALL mostrar un error recuperable con reintento.

#### Scenario: El catálogo lista las prestaciones con su duración y estado

- **GIVEN** una persona autenticada con sesión activa
- **WHEN** abre `/prestaciones`
- **THEN** la SPA consulta `GET /api/prestaciones?soloActivas=false` y la tabla muestra cada nombre con su duración en minutos y su estado activa o inactiva

#### Scenario: El catálogo vacío muestra un mensaje

- **GIVEN** una API que devuelve una lista vacía de prestaciones
- **WHEN** la persona abre `/prestaciones`
- **THEN** la pantalla muestra un mensaje de catálogo vacío y ninguna fila de datos

#### Scenario: Un fallo al listar permite reintentar

- **GIVEN** una API que falla al listar prestaciones
- **WHEN** la persona abre `/prestaciones`
- **THEN** la pantalla muestra una alerta de error y un botón para reintentar, y al usarlo se vuelve a consultar el catálogo

### Requirement: Validación de duración antes de enviar

El formulario de prestación SHALL exigir un nombre no vacío y una duración entera, múltiplo de 5, entre 5 y 480 minutos (RN-09). Un valor inválido MUST impedir el envío, mostrar el mensaje de RN-09 junto al campo y MUST NOT llamar a la API.

#### Scenario: Una duración que no es múltiplo de 5 bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa el nombre "Limpieza" y la duración 7 y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-09 y no se emite ninguna escritura a la API

#### Scenario: Una duración menor a 5 bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa el nombre "Limpieza" y la duración 0 y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-09 y no se emite ninguna escritura a la API

#### Scenario: Una duración mayor a 480 bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa el nombre "Limpieza" y la duración 481 y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-09 y no se emite ninguna escritura a la API

#### Scenario: Una duración no entera bloquea el envío sin llamar a la API

- **WHEN** el administrador ingresa el nombre "Limpieza" y la duración 7,5 y envía el formulario
- **THEN** el formulario muestra el mensaje de RN-09 y no se emite ninguna escritura a la API

#### Scenario: Un nombre vacío bloquea el envío sin llamar a la API

- **WHEN** el administrador deja el nombre en blanco, ingresa la duración 30 y envía el formulario
- **THEN** el formulario indica que el nombre es obligatorio y no se emite ninguna escritura a la API

#### Scenario: Las duraciones límite 5 y 480 se aceptan

- **WHEN** el administrador da de alta una prestación con duración 5 y luego otra con duración 480
- **THEN** ambas altas se envían a la API y la lista se refresca con cada resultado exitoso

### Requirement: Alta de prestación desde la SPA

El rol `ADMIN` SHALL poder dar de alta una prestación con nombre y duración válidos mediante `POST /api/prestaciones`. Con un `201` la lista MUST refrescarse y el formulario MUST quedar limpio. Un `409` con código `PRESTACION-DUPLICADA` y un `422` con código `RN-09` SHALL mostrarse como alerta sin perder lo ingresado.

#### Scenario: El administrador da de alta una prestación y la lista se refresca

- **WHEN** el administrador envía una prestación válida y la API responde `201`
- **THEN** la SPA emite el `POST` con nombre y duración, vuelve a consultar la lista y muestra la prestación nueva

#### Scenario: Un nombre duplicado se muestra como alerta sin perder los datos

- **WHEN** la API responde `409` con código `PRESTACION-DUPLICADA`
- **THEN** la pantalla muestra una alerta de nombre duplicado y conserva el nombre y la duración ingresados

#### Scenario: Un 422 de duración del backend se muestra como alerta

- **WHEN** la API responde `422` con código `RN-09`
- **THEN** la pantalla muestra el mensaje de duración inválida como alerta y no refresca la lista

### Requirement: Modificación de prestación

El rol `ADMIN` SHALL poder modificar nombre, duración y estado activo con `PUT /api/prestaciones/{id}`. Con un `200` la lista MUST refrescarse. Un `404` con código `PRESTACION-NO-ENCONTRADA` SHALL mostrarse como alerta y refrescar la lista.

#### Scenario: El administrador edita una prestación y la lista se refresca

- **WHEN** el administrador cambia el nombre y la duración de una prestación y la API responde `200`
- **THEN** la SPA emite el `PUT` al id correspondiente, vuelve a consultar la lista y muestra los datos nuevos

#### Scenario: El administrador reactiva una prestación dada de baja

- **WHEN** el administrador usa la acción de reactivar sobre una prestación inactiva
- **THEN** la SPA emite el `PUT` con `activa` igual a `true` y la lista refrescada la muestra activa

#### Scenario: Editar una prestación inexistente muestra una alerta y refresca la lista

- **WHEN** la API responde `404` con código `PRESTACION-NO-ENCONTRADA`
- **THEN** la pantalla muestra una alerta de prestación inexistente y vuelve a consultar la lista

### Requirement: Baja lógica de prestación

El rol `ADMIN` SHALL poder dar de baja una prestación con `DELETE /api/prestaciones/{id}`. Con un `204` la lista MUST refrescarse y la prestación MUST seguir visible como inactiva (RN-37). Un `404` SHALL mostrarse como alerta.

#### Scenario: El administrador da de baja una prestación y sigue visible como inactiva

- **WHEN** el administrador confirma la baja y la API responde `204`
- **THEN** la SPA vuelve a consultar la lista y muestra la prestación con estado inactivo

#### Scenario: Dar de baja una prestación inexistente muestra una alerta

- **WHEN** la API responde `404` con código `PRESTACION-NO-ENCONTRADA` a una baja
- **THEN** la pantalla muestra una alerta de prestación inexistente y refresca la lista

### Requirement: Controles de escritura según rol

Solo el rol `ADMIN` SHALL ver y usar los controles de alta, edición y baja. `RECEPCION` y `ODONTOLOGO` MUST consultar el catálogo en modo solo lectura, sin formulario ni botones de escritura. La ruta `/prestaciones` SHALL estar disponible para los tres roles del personal.

#### Scenario: El administrador ve los controles de alta, edición y baja

- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** abre `/prestaciones`
- **THEN** ve el botón de alta, un control de edición y uno de baja por cada prestación

#### Scenario: Recepción consulta el catálogo sin controles de escritura

- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre `/prestaciones`
- **THEN** ve la lista en modo consulta, sin formulario ni botones de alta, edición o baja, y no se emite ninguna escritura

#### Scenario: El odontólogo consulta el catálogo sin controles de escritura

- **GIVEN** una sesión activa con rol `ODONTOLOGO`
- **WHEN** abre `/prestaciones`
- **THEN** ve la lista en modo consulta, sin formulario ni botones de alta, edición o baja, y no se emite ninguna escritura

#### Scenario: La pantalla de prestaciones aparece en el menú para los tres roles

- **GIVEN** una sesión activa con cualquiera de los roles `ADMIN`, `RECEPCION` u `ODONTOLOGO`
- **WHEN** se muestra el menú principal
- **THEN** la entrada de prestaciones está presente y abre `/prestaciones`

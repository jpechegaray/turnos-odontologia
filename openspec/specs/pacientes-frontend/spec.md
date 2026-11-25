# pacientes-frontend Specification

## Purpose
Define cómo el personal busca, da de alta, edita y da de baja fichas de paciente desde la SPA (US-009), con la validación de RN-32 antes de enviar, los errores del backend mostrados en el campo que corresponde y los controles que habilita cada rol.

## Requirements

### Requirement: Acceso a la pantalla de pacientes según rol
La pantalla `/pacientes` SHALL estar disponible y aparecer en el menú para los roles `ADMIN` y `RECEPCION`. El rol `ODONTOLOGO` MUST NOT verla en el menú y, si navega a `/pacientes`, la SPA MUST mostrar el aviso de falta de permiso sin consultar `/api/pacientes`.

#### Scenario: La pantalla de pacientes aparece en el menú para administración y recepción
- **GIVEN** una sesión activa con rol `ADMIN` o `RECEPCION`
- **WHEN** se muestra el menú principal
- **THEN** la entrada de pacientes está presente y abre `/pacientes`

#### Scenario: El odontólogo no ve la pantalla de pacientes
- **GIVEN** una sesión activa con rol `ODONTOLOGO`
- **WHEN** se muestra el menú principal y navega a `/pacientes`
- **THEN** el menú no tiene la entrada de pacientes, la pantalla muestra el aviso de falta de permiso y no se emite ninguna consulta a `/api/pacientes`

### Requirement: Listado inicial de fichas activas
Al abrir `/pacientes` la SPA SHALL consultar `GET /api/pacientes` sin filtros y mostrar las fichas activas que devuelve, en el orden del backend (apellido y nombre), con apellido, nombre, DNI, email, teléfono y obra social. Un dato ausente MUST mostrarse con una marca de vacío.

#### Scenario: Al abrir la pantalla se listan las fichas activas
- **GIVEN** una recepcionista con sesión activa y una API que devuelve dos fichas ficticias
- **WHEN** abre `/pacientes`
- **THEN** la SPA consulta `GET /api/pacientes` sin parámetros y la tabla muestra una fila por ficha con apellido, nombre, DNI, email, teléfono y obra social

#### Scenario: Un dato ausente de la ficha se muestra con una marca de vacío
- **GIVEN** una ficha sin DNI ni obra social
- **WHEN** la recepcionista abre `/pacientes`
- **THEN** las celdas de DNI y obra social de esa fila muestran la marca de vacío

### Requirement: Búsqueda por apellido, nombre, DNI o email
La pantalla SHALL ofrecer una búsqueda con los campos apellido, nombre, DNI y email. Al buscar, la SPA MUST enviar a `GET /api/pacientes` solo los filtros no vacíos, recortados y codificados como parámetros de consulta, y MUST reemplazar la tabla con el resultado. Un resultado vacío SHALL mostrar un mensaje.

#### Scenario: La búsqueda por apellido envía solo ese filtro
- **WHEN** la recepcionista escribe "Gómez" en apellido, deja vacíos los otros campos y busca
- **THEN** la SPA consulta `GET /api/pacientes?apellido=G%C3%B3mez` y la tabla muestra las fichas que devuelve la API

#### Scenario: Los filtros de búsqueda se combinan
- **WHEN** la recepcionista escribe "Ana" en nombre y "ana.ficticia@example.test" en email y busca
- **THEN** la SPA consulta `GET /api/pacientes` con los parámetros `nombre` y `email` juntos y sin `apellido` ni `dni`

#### Scenario: La búsqueda por DNI envía el DNI recortado
- **WHEN** la recepcionista escribe " 30111222 " en DNI y busca
- **THEN** la SPA consulta `GET /api/pacientes?dni=30111222`

#### Scenario: Una búsqueda sin coincidencias muestra un mensaje
- **GIVEN** una API que devuelve una lista vacía
- **WHEN** la recepcionista busca por un apellido
- **THEN** la pantalla muestra el mensaje de que no se encontraron pacientes y ninguna fila de datos

### Requirement: Error recuperable al consultar pacientes
Si la consulta de pacientes falla, la pantalla SHALL mostrar una alerta de error con un botón para reintentar. Al reintentar, la SPA MUST repetir la última consulta con los mismos filtros.

#### Scenario: Un fallo al consultar pacientes permite reintentar
- **GIVEN** una API que falla la primera vez que se consultan pacientes
- **WHEN** la recepcionista abre `/pacientes` y usa el botón de reintentar
- **THEN** la pantalla muestra primero la alerta de error y después la tabla con las fichas, tras una segunda consulta a `GET /api/pacientes`

### Requirement: Validación de la ficha antes de enviar
El formulario SHALL exigir nombre y apellido no vacíos y al menos un medio de contacto, email o teléfono (RN-32). Los textos MUST recortarse y un campo opcional vacío MUST enviarse como `null`. Un dato inválido MUST mostrar su mensaje junto al campo y MUST NOT emitir ninguna escritura a la API.

#### Scenario: Una ficha sin nombre o apellido bloquea el envío sin llamar a la API
- **WHEN** la recepcionista deja vacíos nombre y apellido, completa el email y envía el formulario
- **THEN** el formulario muestra el mensaje de dato obligatorio junto a nombre y junto a apellido, y no se emite ninguna escritura a la API

#### Scenario: Una ficha sin email ni teléfono bloquea el envío sin llamar a la API
- **WHEN** la recepcionista completa nombre y apellido, deja vacíos email y teléfono y envía el formulario
- **THEN** el formulario muestra el mensaje de medio de contacto obligatorio junto a email y teléfono, y no se emite ninguna escritura a la API

#### Scenario: Una ficha con solo teléfono se acepta
- **WHEN** la recepcionista completa nombre, apellido y teléfono, deja vacío el email y envía el formulario
- **THEN** la SPA envía la ficha a la API con `email` igual a `null`

### Requirement: Alta de ficha de paciente
Los roles `ADMIN` y `RECEPCION` SHALL poder dar de alta una ficha con `POST /api/pacientes`, enviando nombre, apellido, DNI, email, teléfono, obra social, número de afiliado y notas. Con un `201` la SPA MUST avisar el alta, vaciar el formulario y volver a consultar la lista con los filtros vigentes.

#### Scenario: Recepción da de alta un paciente y la lista se refresca
- **GIVEN** una recepcionista con sesión activa
- **WHEN** completa la ficha de un paciente ficticio, la envía y la API responde `201`
- **THEN** la SPA envía `POST /api/pacientes` con los ocho campos, muestra el aviso de paciente agregado, vacía el formulario y vuelve a consultar `GET /api/pacientes`

#### Scenario: La obra social se envía como texto libre
- **WHEN** la recepcionista escribe "Obra Social Ficticia Plan 210" en obra social y envía una ficha válida
- **THEN** el cuerpo del `POST` lleva `obraSocial` con ese mismo texto, sin validación adicional

### Requirement: Edición de ficha de paciente
Los roles `ADMIN` y `RECEPCION` SHALL poder editar una ficha desde su fila. El formulario MUST precargarse con los datos de la fila y enviar `PUT /api/pacientes/{id}` con los ocho campos. Con un `200` la SPA MUST avisar el cambio, cerrar la edición y volver a consultar la lista con los filtros vigentes.

#### Scenario: Recepción edita una ficha y la lista se refresca
- **GIVEN** una recepcionista y una ficha ficticia en la tabla
- **WHEN** usa el control de edición de esa fila, cambia el teléfono, guarda y la API responde `200`
- **THEN** la SPA envía `PUT /api/pacientes/{id}` con los datos precargados y el teléfono nuevo, muestra el aviso de paciente actualizado y vuelve a consultar la lista

#### Scenario: Cancelar la edición vuelve al formulario de alta
- **WHEN** la recepcionista abre la edición de una ficha y usa el botón de cancelar
- **THEN** el formulario vuelve a estar vacío para un alta y no se emite ninguna escritura a la API

### Requirement: Errores del backend en el campo correspondiente
El formulario SHALL mostrar junto al campo afectado los errores de RN-32 del backend, sin perder los datos ingresados: el `409` con código `RN-32` en DNI, el `422` con código `RN-32` en email y teléfono, y cada campo del `400` en su campo. Un `404` o un `403` MUST mostrarse como alerta. El `detalle` del servidor MUST NOT mostrarse.

#### Scenario: Un DNI duplicado se muestra en el campo DNI sin perder los datos
- **WHEN** la recepcionista envía una ficha válida y la API responde `409` con código `RN-32`
- **THEN** el campo DNI muestra el mensaje de DNI ya registrado, los demás campos conservan lo ingresado y no se muestra el `detalle` del servidor

#### Scenario: Un contacto faltante del backend se muestra en los campos de contacto
- **WHEN** la recepcionista envía una ficha válida y la API responde `422` con código `RN-32`
- **THEN** los campos email y teléfono muestran el mensaje de medio de contacto obligatorio y los datos ingresados se conservan

#### Scenario: Un campo inválido del backend se muestra en su campo
- **WHEN** la API responde `400` con la lista de errores que incluye el campo `notas`
- **THEN** el campo notas muestra un mensaje de dato inválido y los otros campos no muestran error

#### Scenario: Editar una ficha inexistente muestra una alerta y refresca la lista
- **WHEN** la recepcionista guarda una edición y la API responde `404` con código `PACIENTE_NO_ENCONTRADO`
- **THEN** la pantalla muestra una alerta de paciente inexistente y vuelve a consultar la lista

#### Scenario: Un rechazo por permiso se muestra como alerta
- **WHEN** la API responde `403` a un alta
- **THEN** el formulario muestra la alerta de falta de permiso y conserva los datos ingresados

### Requirement: Baja lógica de ficha reservada al administrador
Solo el rol `ADMIN` SHALL ver el control de baja de cada fila. La baja MUST pedir confirmación en la fila y enviar `DELETE /api/pacientes/{id}`. Con un `204` la SPA MUST avisar la baja y volver a consultar la lista, donde la ficha ya no aparece porque la búsqueda solo trae fichas activas. Un `404` SHALL mostrarse como alerta.

#### Scenario: El administrador da de baja una ficha y deja de aparecer en la lista
- **GIVEN** un administrador y una ficha ficticia en la tabla
- **WHEN** usa el control de baja, confirma y la API responde `204`
- **THEN** la SPA envía `DELETE /api/pacientes/{id}`, muestra el aviso de paciente dado de baja y vuelve a consultar la lista, que ya no incluye esa ficha

#### Scenario: Cancelar la confirmación no da de baja la ficha
- **WHEN** el administrador usa el control de baja y después cancela la confirmación
- **THEN** la fila vuelve a mostrar sus acciones y no se emite ninguna escritura a la API

#### Scenario: Dar de baja una ficha inexistente muestra una alerta
- **WHEN** el administrador confirma una baja y la API responde `404` con código `PACIENTE_NO_ENCONTRADO`
- **THEN** la pantalla muestra una alerta de paciente inexistente y vuelve a consultar la lista

### Requirement: Controles de escritura según rol
`ADMIN` SHALL ver el formulario de alta y, por fila, los controles de edición y de baja. `RECEPCION` SHALL ver el formulario de alta y el control de edición, pero MUST NOT ver el control de baja.

#### Scenario: El administrador ve los controles de alta, edición y baja
- **GIVEN** una sesión activa con rol `ADMIN`
- **WHEN** abre `/pacientes`
- **THEN** ve el formulario de alta y, en cada fila, un control de edición y uno de baja

#### Scenario: Recepción ve alta y edición pero no la baja
- **GIVEN** una sesión activa con rol `RECEPCION`
- **WHEN** abre `/pacientes`
- **THEN** ve el formulario de alta y un control de edición por fila, y ninguna fila tiene control de baja

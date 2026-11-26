# lista-espera-frontend Specification

## Purpose

Dar a recepción una pantalla para anotar pacientes en la lista de espera, ver y dar de baja sus entradas, y asignar a mano un hueco liberado a una entrada compatible.

## Requirements

### Requirement: Acceso a la pantalla de lista de espera

La SPA SHALL ofrecer `/lista-espera` con entrada en el menú para `ADMIN` y `RECEPCION`. Para `ODONTOLOGO`, la ruta MUST NOT aparecer en el menú ni mostrar la pantalla.

#### Scenario: Recepción ve la lista de espera en el menú
- **WHEN** una persona con rol `RECEPCION` inicia sesión
- **THEN** el menú muestra «Lista de espera» y la ruta abre la pantalla

#### Scenario: El odontólogo no accede a la lista de espera
- **WHEN** una persona con rol `ODONTOLOGO` navega a `/lista-espera`
- **THEN** el menú no muestra la entrada y la pantalla de lista de espera no se muestra

### Requirement: Alta de una entrada con validaciones

El alta SHALL pedir paciente y prestación como obligatorios, profesional opcional, fecha de inicio opcional, fecha de fin obligatoria y franja opcional. Antes de enviar, la SPA SHALL validar que la fecha de fin no sea anterior a la de inicio y que la franja tenga las dos horas o ninguna, con el fin posterior al inicio.

#### Scenario: Un alta válida agrega la entrada al listado
- **WHEN** recepción elige paciente, prestación y fecha de fin, y confirma el alta
- **THEN** la SPA envía un solo POST, informa que el paciente quedó anotado y el listado de activas muestra la nueva entrada

#### Scenario: El alta sin datos obligatorios no se envía
- **WHEN** recepción confirma el alta sin paciente, sin prestación o sin fecha de fin
- **THEN** la SPA marca los campos que faltan y no envía el pedido

#### Scenario: Un rango de fechas invertido no se envía
- **WHEN** la fecha de fin es anterior a la fecha de inicio
- **THEN** la SPA lo indica junto al campo y no envía el pedido

#### Scenario: Una franja incompleta o invertida no se envía
- **WHEN** la franja tiene una sola hora, o la hora de fin no es posterior a la de inicio
- **THEN** la SPA lo indica junto a la franja y no envía el pedido

### Requirement: Rechazos del alta informados por el servidor

La SPA SHALL mostrar los rechazos del servidor sin perder los datos cargados: entrada activa duplicada (409), paciente sin email, inactivo o datos inexistentes (422) y errores por campo (400).

#### Scenario: Una entrada activa duplicada se informa sin perder el formulario
- **WHEN** la API responde 409 `ENTRADA_ACTIVA_DUPLICADA`
- **THEN** la pantalla informa que el paciente ya está anotado para esa prestación y conserva los datos cargados

#### Scenario: Un paciente sin email no se puede anotar
- **WHEN** la API responde 422 `PACIENTE_SIN_EMAIL`
- **THEN** la pantalla informa que el paciente necesita un email para recibir el aviso y conserva los datos cargados

### Requirement: Listado de entradas por estado

La SPA SHALL consultar las entradas de todos los estados y mostrar los conteos y el filtro de estado. La vista inicial SHALL mostrar las entradas activas. Cada fila SHALL mostrar el nombre del paciente, la prestación, el profesional («Cualquiera» si no hay preferencia), el rango de fechas, la franja y el estado. Si un catálogo no responde, SHALL mostrar un nombre de respaldo con el número de ID.

#### Scenario: El listado muestra las entradas activas con nombres
- **WHEN** recepción abre la pantalla
- **THEN** la SPA consulta las entradas y muestra las activas con paciente, prestación, profesional, rango, franja y estado con nombres legibles

#### Scenario: Cambiar el filtro de estado consulta ese estado
- **WHEN** recepción elige otro estado en el filtro
- **THEN** la SPA muestra solamente las entradas de ese estado

#### Scenario: Un estado sin entradas lo informa
- **WHEN** la vista de un estado no contiene entradas
- **THEN** la pantalla informa que no hay entradas en ese estado

### Requirement: Baja de una entrada activa

La baja SHALL pedir una confirmación en la misma fila antes de enviar `DELETE` y SHALL enviar como máximo un pedido en curso. Si la API responde 409 porque la entrada ya no está activa, la SPA SHALL avisarlo y recargar el listado.

#### Scenario: Dar de baja una entrada la quita de las activas
- **WHEN** recepción pulsa «Dar de baja», confirma y la API responde 204
- **THEN** la SPA envía un solo DELETE y la entrada deja de aparecer entre las activas

#### Scenario: Desistir de la baja no envía el pedido
- **WHEN** recepción pulsa «Dar de baja» y elige volver
- **THEN** la SPA no envía el DELETE y la entrada sigue en el listado

#### Scenario: Dar de baja una entrada que ya no está activa recarga el listado
- **WHEN** la API responde 409 `ENTRADA_NO_ACTIVA` a la baja
- **THEN** la pantalla avisa que la entrada ya no estaba activa y vuelve a consultar el listado

### Requirement: Sugerencias de entradas compatibles para un hueco

La SPA SHALL permitir indicar un hueco con profesional, fecha, hora de inicio y hora de fin, y SHALL consultar las entradas compatibles con instantes ISO con zona en Buenos Aires. Antes de consultar, SHALL validar que la hora de fin sea posterior a la de inicio.

#### Scenario: Buscar un hueco lista las entradas compatibles
- **WHEN** recepción indica profesional, fecha y horario del hueco y busca
- **THEN** la SPA consulta las compatibles de ese intervalo en Buenos Aires y muestra cada entrada con su paciente y prestación

#### Scenario: Un hueco sin compatibles lo informa
- **WHEN** la consulta de compatibles devuelve una lista vacía
- **THEN** la pantalla informa que no hay entradas compatibles con ese hueco

#### Scenario: Un hueco con horario invertido no se consulta
- **WHEN** la hora de fin del hueco no es posterior a la de inicio
- **THEN** la SPA lo indica junto al campo y no consulta las compatibles

### Requirement: Asignación manual del hueco

Asignar SHALL enviar `POST /api/lista-espera/{id}/asignar` con el profesional y el inicio del hueco, con un solo pedido en curso. Ante un 201, la SPA SHALL informar el turno creado y recargar las sugerencias y el listado. Ante un 409 o un 422, SHALL mostrar un mensaje claro y recargar las sugerencias.

#### Scenario: Asignar el hueco crea el turno y cierra la entrada
- **WHEN** recepción pulsa «Asignar» en una sugerencia y la API responde 201
- **THEN** la pantalla informa que se asignó el turno al paciente y la entrada deja de figurar entre las sugerencias y las activas

#### Scenario: Un segundo clic en asignar no repite el pedido
- **WHEN** recepción pulsa «Asignar» otra vez mientras el primer pedido está en curso
- **THEN** la SPA hace un solo POST de asignación

#### Scenario: Un horario ocupado al asignar se informa y recarga las sugerencias
- **WHEN** la API responde 409 porque el profesional quedó ocupado en ese horario
- **THEN** la pantalla informa que el horario ya no está disponible y vuelve a consultar las compatibles

#### Scenario: Asignar una entrada que ya no está activa se informa
- **WHEN** la API responde 409 `ENTRADA_NO_ACTIVA` al asignar
- **THEN** la pantalla informa que la entrada ya no está activa y vuelve a consultar las compatibles

#### Scenario: Una entrada incompatible con el hueco se informa
- **WHEN** la API responde 422 `ENTRADA_INCOMPATIBLE` al asignar
- **THEN** la pantalla informa que la entrada no es compatible con ese hueco y no muestra el turno como asignado

### Requirement: Acciones del encabezado de la lista de espera

La pantalla SHALL mostrar «Anotar paciente» y «Asignar hueco» en el encabezado. Cada botón SHALL alternar la visibilidad de su formulario, indicar su estado con `aria-expanded` y enfocar el panel al abrirlo. La pantalla MUST mostrar como máximo un formulario a la vez y conservar los datos cargados al alternar.

#### Scenario: Los botones de lista de espera abren su formulario
- **GIVEN** los dos formularios están ocultos al entrar
- **WHEN** se selecciona «Anotar paciente» y después «Asignar hueco»
- **THEN** se muestra solamente el formulario de la acción seleccionada y un segundo clic en ella vuelve a ocultarlo

# Spec Delta

## Purpose

Permitir que recepción anote pacientes en la lista de espera con sus preferencias, que las entradas vencidas dejen de estar activas y que el ciclo de estados quede definido para los changes que ofrecerán y asignarán huecos.

## ADDED Requirements

### Requirement: Alta de una entrada en la lista de espera

`ADMIN` y `RECEPCION` SHALL poder crear una entrada con `POST /api/lista-espera`. El alta MUST recibir `pacienteId`, `prestacionId` y `fechaHasta` (`AAAA-MM-DD`). Son opcionales `profesionalId`, `fechaDesde` (por defecto, la fecha de hoy en Buenos Aires) y la franja `franjaDesde` y `franjaHasta` (`HH:mm`, las dos o ninguna). La entrada nueva queda `ACTIVA` y el alta devuelve `201` con su representación.

#### Scenario: Recepción anota a un paciente con todos los datos
- **GIVEN** un paciente activo con email, una prestación activa y un profesional activo
- **WHEN** `ADMIN` o `RECEPCION` envía un alta con paciente, prestación, profesional, rango de fechas y franja, con CSRF válido
- **THEN** recibe `201` con `id`, `pacienteId`, `prestacionId`, `profesionalId`, `fechaDesde`, `fechaHasta`, `franjaDesde`, `franjaHasta`, `estado` `ACTIVA`, `turnoAsignadoId` nulo y `creadoEn`

#### Scenario: Solo con los datos obligatorios la fecha de inicio es hoy
- **GIVEN** que hoy es 2026-12-01 en Buenos Aires
- **WHEN** un usuario autorizado envía un alta solo con paciente, prestación y `fechaHasta` 2026-12-31
- **THEN** recibe `201` con `fechaDesde` 2026-12-01 y profesional y franja nulos

#### Scenario: El alta rechaza datos con formato inválido
- **WHEN** un usuario autorizado envía un alta sin paciente, sin prestación o sin `fechaHasta`, con una fecha que no respeta `AAAA-MM-DD`, con `fechaHasta` anterior a `fechaDesde` o a hoy, con una sola hora de franja, con una hora que no respeta `HH:mm` o con `franjaHasta` no posterior a `franjaDesde`
- **THEN** recibe `400` en Problem Details y no se crea ninguna entrada

### Requirement: Recursos válidos en el alta

El alta MUST rechazar con `422` en Problem Details, sin crear la entrada:

- un paciente inexistente (`PACIENTE_INEXISTENTE`), inactivo (`PACIENTE_INACTIVO`) o sin email (`PACIENTE_SIN_EMAIL`, RN-21 y PA-16);
- una prestación inexistente (`PRESTACION_INEXISTENTE`) o inactiva (`PRESTACION_INACTIVA`);
- un profesional preferido inexistente (`PROFESIONAL_INEXISTENTE`) o inactivo (`PROFESIONAL_INACTIVO`).

#### Scenario: Un paciente sin email no se puede anotar
- **GIVEN** un paciente activo que solo tiene teléfono
- **WHEN** un usuario autorizado intenta anotarlo
- **THEN** recibe `422` con código `PACIENTE_SIN_EMAIL` y no se crea ninguna entrada

#### Scenario: Paciente, prestación o profesional inexistente o inactivo se rechaza
- **WHEN** un usuario autorizado envía un alta con un paciente, una prestación o un profesional inexistente o inactivo
- **THEN** recibe `422` con el código correspondiente y no se crea ninguna entrada

### Requirement: Sin entradas activas duplicadas

Un paciente SHALL NOT tener dos entradas `ACTIVA` para la misma prestación (RN-21). Un alta que lo intente MUST responder `409` con código `ENTRADA_ACTIVA_DUPLICADA`, también si llegan dos altas al mismo tiempo. Las entradas en otro estado, o de otra prestación, no cuentan.

#### Scenario: Una segunda entrada activa para la misma prestación se rechaza
- **GIVEN** un paciente con una entrada `ACTIVA` para una prestación
- **WHEN** un usuario autorizado intenta anotarlo de nuevo para esa prestación
- **THEN** recibe `409` con código `ENTRADA_ACTIVA_DUPLICADA` y el paciente conserva una sola entrada activa

#### Scenario: Otra prestación o una entrada ya cancelada no generan duplicado
- **GIVEN** un paciente con una entrada `ACTIVA` para la prestación A y otra `CANCELADA` para la prestación B
- **WHEN** se lo anota para la prestación B y para una prestación C
- **THEN** ambas altas devuelven `201`

#### Scenario: Dos altas simultáneas duplicadas: una gana y la otra recibe 409
- **WHEN** dos transacciones intentan anotar al mismo tiempo al mismo paciente para la misma prestación
- **THEN** una crea la entrada y la otra recibe el error `ENTRADA_ACTIVA_DUPLICADA`

### Requirement: Consulta de la lista de espera

`ADMIN`, `RECEPCION` y `ODONTOLOGO` SHALL poder listar las entradas con `GET /api/lista-espera` y consultar una con `GET /api/lista-espera/{id}`. El listado acepta los filtros opcionales `estado`, `pacienteId` y `prestacionId`, combinables, y se ordena por `creadoEn` y luego por `id`, de la más antigua a la más nueva. Un filtro de estado desconocido MUST devolver `400`, y un ID inexistente, `404` con código `ENTRADA_NO_ENCONTRADA`.

#### Scenario: El listado filtra y ordena por antigüedad
- **GIVEN** entradas de varios pacientes, prestaciones y estados
- **WHEN** un usuario con cualquiera de los tres roles lista con y sin filtros
- **THEN** recibe `200` con las entradas que cumplen todos los filtros, de la más antigua a la más nueva

#### Scenario: Consultar una entrada inexistente devuelve 404
- **WHEN** un usuario autorizado consulta un ID de entrada inexistente
- **THEN** recibe `404` con código `ENTRADA_NO_ENCONTRADA`

### Requirement: Baja de una entrada

`DELETE /api/lista-espera/{id}` SHALL permitir a `ADMIN` y `RECEPCION` cancelar una entrada `ACTIVA`, que pasa a `CANCELADA` y se conserva. Devuelve `204` sin cuerpo. Si la entrada no existe, responde `404` con código `ENTRADA_NO_ENCONTRADA`. Si la entrada no está `ACTIVA`, responde `409` con código `ENTRADA_NO_ACTIVA` y no cambia.

#### Scenario: Cancelar una entrada activa la conserva como cancelada
- **GIVEN** una entrada `ACTIVA`
- **WHEN** `ADMIN` o `RECEPCION` la da de baja con CSRF válido
- **THEN** recibe `204` sin cuerpo, la entrada sigue existiendo con estado `CANCELADA` y el paciente puede volver a anotarse para esa prestación

#### Scenario: Una entrada que no está activa no se puede cancelar
- **GIVEN** una entrada `VENCIDA`, `CANCELADA` o `ASIGNADA`
- **WHEN** un usuario autorizado intenta darla de baja
- **THEN** recibe `409` con código `ENTRADA_NO_ACTIVA` y el estado no cambia

### Requirement: Ciclo de estados de una entrada

Una entrada SHALL nacer `ACTIVA` y solo desde `ACTIVA` puede pasar a `ASIGNADA` (registrando el turno asignado), a `CANCELADA` o a `VENCIDA`. Los estados `ASIGNADA`, `CANCELADA` y `VENCIDA` son finales: cualquier otra transición MUST rechazarse.

#### Scenario: Desde ACTIVA se puede asignar, cancelar o vencer
- **GIVEN** una entrada `ACTIVA`
- **WHEN** se la asigna a un turno, se la cancela o se la vence
- **THEN** pasa a `ASIGNADA` con el turno registrado, a `CANCELADA` o a `VENCIDA`

#### Scenario: Un estado final no admite otra transición
- **GIVEN** una entrada `ASIGNADA`, `CANCELADA` o `VENCIDA`
- **WHEN** se intenta asignarla, cancelarla o vencerla
- **THEN** la transición se rechaza y la entrada conserva su estado

### Requirement: Vencimiento automático

Un proceso programado SHALL ejecutarse una vez por día y marcar `VENCIDA` toda entrada `ACTIVA` cuya `fechaHasta` sea anterior a la fecha de hoy en `America/Argentina/Buenos_Aires` (RN-14, RN-21). Una entrada cuya `fechaHasta` es hoy sigue `ACTIVA` hasta el ciclo siguiente.

#### Scenario: Una entrada con fecha de fin pasada vence en el siguiente ciclo
- **GIVEN** que hoy es 2026-12-01 en Buenos Aires y hay entradas `ACTIVA` con `fechaHasta` 2026-11-30 y 2026-12-01, y una `CANCELADA` con `fechaHasta` 2026-11-20
- **WHEN** corre el ciclo de vencimiento
- **THEN** solo la entrada con `fechaHasta` 2026-11-30 pasa a `VENCIDA`, y las otras dos conservan su estado

### Requirement: Acceso a la lista de espera según rol

Las rutas SHALL requerir sesión del personal. `ODONTOLOGO` SHALL poder consultar, pero MUST recibir `403` al crear o dar de baja una entrada, aun con CSRF válido y cuerpo malformado (RN-33). Las escrituras de `ADMIN` y `RECEPCION` MUST exigir CSRF válido.

#### Scenario: El odontólogo consulta pero no escribe
- **WHEN** un odontólogo lista o consulta entradas, e intenta crear o dar de baja una con CSRF válido
- **THEN** las consultas devuelven `200` y las escrituras, `403` en Problem Details sin cambiar registros

#### Scenario: Las escrituras requieren CSRF válido
- **WHEN** `ADMIN` o `RECEPCION` intenta crear o dar de baja una entrada sin CSRF válido
- **THEN** recibe `403` en Problem Details y los registros no cambian

#### Scenario: Sin sesión no se accede a la lista de espera
- **WHEN** un cliente sin sesión intenta listar, consultar, crear o dar de baja una entrada
- **THEN** recibe `401` en Problem Details y los registros no cambian

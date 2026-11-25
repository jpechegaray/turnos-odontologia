# lista-espera Specification

## Purpose
Permitir que recepción anote pacientes en la lista de espera con sus preferencias, que las entradas vencidas dejen de estar activas y que el ciclo de estados quede definido para los changes que ofrecerán y asignarán huecos.

## Requirements

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

### Requirement: Compatibilidad entre un hueco y una entrada

Un hueco SHALL ser un profesional P con un intervalo `[inicio, fin)`, `inicio < fin`; el sillón es indistinto. Una entrada SHALL ser compatible solo si está `ACTIVA`, P realiza su prestación activa, el profesional preferido (si hay) es P, la duración por defecto cabe (`duración <= fin − inicio`) y cumple fechas y franja (RN-22). MUST ser una función pura del dominio; no verifica que el hueco esté libre.

#### Scenario: Una prestación de 90 minutos no cabe en un hueco de 30
- **GIVEN** un hueco de 30 minutos de un profesional que realiza "Tratamiento de conducto" (90 minutos) y "Consulta" (30 minutos)
- **WHEN** se evalúan una entrada de cada prestación, sin profesional preferido ni franja y con el hueco dentro de sus fechas
- **THEN** la entrada de "Tratamiento de conducto" no es compatible y la de "Consulta" sí

#### Scenario: Una prestación que dura exactamente el hueco es compatible
- **GIVEN** un hueco de 30 minutos y una entrada de una prestación de 30 minutos
- **WHEN** se evalúa la compatibilidad
- **THEN** la entrada es compatible

#### Scenario: El profesional del hueco debe realizar la prestación
- **GIVEN** un hueco de un profesional que no realiza la prestación de la entrada
- **WHEN** se evalúa la compatibilidad
- **THEN** la entrada no es compatible

#### Scenario: Una prestación inactiva no es compatible
- **GIVEN** una entrada cuya prestación fue desactivada después del alta
- **WHEN** se evalúa con un hueco donde cabe
- **THEN** la entrada no es compatible

#### Scenario: Se respeta el profesional preferido
- **GIVEN** una entrada con profesional preferido A
- **WHEN** se evalúa con un hueco de A y con un hueco de otro profesional B que también realiza la prestación
- **THEN** es compatible con el hueco de A y no con el de B

#### Scenario: Una entrada sin profesional preferido acepta a cualquiera que realice la prestación
- **GIVEN** una entrada sin profesional preferido
- **WHEN** se evalúa con un hueco de un profesional que realiza la prestación
- **THEN** la entrada es compatible

#### Scenario: Una entrada que no está activa no es compatible
- **GIVEN** entradas `ASIGNADA`, `CANCELADA` y `VENCIDA` que cumplirían el resto de las condiciones
- **WHEN** se evalúan con el hueco
- **THEN** ninguna es compatible

### Requirement: Fechas y franja de la entrada en la compatibilidad

La fecha del `inicio` del hueco en `America/Argentina/Buenos_Aires` SHALL estar dentro de `[fechaDesde, fechaHasta]`, con los extremos incluidos. Si la entrada tiene franja, el turno resultante `[inicio, inicio + duración)` en hora de Buenos Aires SHALL empezar en `franjaDesde` o después y terminar en `franjaHasta` o antes, el mismo día (RN-22).

#### Scenario: La fecha del hueco debe estar dentro del rango de la entrada
- **GIVEN** una entrada con `fechaDesde` 2030-06-10 y `fechaHasta` 2030-06-20
- **WHEN** se evalúa con huecos que empiezan el 2030-06-09, el 2030-06-10, el 2030-06-20 y el 2030-06-21 en Buenos Aires
- **THEN** solo son compatibles los del 2030-06-10 y el 2030-06-20

#### Scenario: La fecha se toma en hora de Buenos Aires
- **GIVEN** una entrada con `fechaHasta` 2030-06-12
- **WHEN** se evalúa con un hueco que empieza a las 22:00 del 2030-06-12 en Buenos Aires (01:00 UTC del 2030-06-13)
- **THEN** la entrada es compatible

#### Scenario: El turno resultante debe caer dentro de la franja
- **GIVEN** una entrada de una prestación de 30 minutos con franja de 09:00 a 12:00
- **WHEN** se evalúa con huecos de una hora que empiezan a las 08:30, a las 09:00, a las 11:30 y a las 11:45 en Buenos Aires
- **THEN** son compatibles los de las 09:00 y las 11:30, y no lo son los de las 08:30 y las 11:45

#### Scenario: Una entrada sin franja acepta cualquier hora
- **GIVEN** una entrada sin franja
- **WHEN** se evalúa con un hueco a las 19:00 en Buenos Aires
- **THEN** la entrada es compatible

### Requirement: Selección de las entradas a avisar

Entre las entradas compatibles con un hueco, el sistema SHALL elegir a quién avisar de la siguiente forma (RN-23, SU-14):

- las ordena por antigüedad: `creadoEn` ascendente y, si empatan, `id` ascendente;
- deja una sola entrada por paciente, la más antigua;
- corta la lista en `lista_espera.max_avisos_por_hueco`, contando pacientes distintos.

Si el momento actual es igual al `inicio` del hueco o posterior, la selección MUST ser vacía. La selección MUST ser una función pura del dominio.

#### Scenario: El tope de avisos elige a los más antiguos
- **GIVEN** 12 entradas compatibles de pacientes distintos y un tope de 10
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se eligen las 10 más antiguas, en orden de antigüedad

#### Scenario: Un paciente con dos entradas compatibles recibe un solo aviso
- **GIVEN** un paciente con dos entradas compatibles de prestaciones distintas, creadas en días distintos
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se elige solo la más antigua de ese paciente

#### Scenario: El paciente repetido no ocupa un lugar del tope
- **GIVEN** un tope de 2 y las entradas compatibles, de la más antigua a la más nueva, del paciente A, del paciente A y del paciente B
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se eligen la primera entrada de A y la de B

#### Scenario: No se avisa por un hueco que ya empezó
- **GIVEN** un hueco cuyo inicio es igual al momento actual o anterior
- **WHEN** se seleccionan las entradas a avisar
- **THEN** la selección es vacía

### Requirement: Consulta de entradas compatibles con un hueco

`ADMIN` y `RECEPCION` SHALL poder consultar `GET /api/lista-espera/compatibles?profesionalId=&inicio=&fin=` (ISO-8601 con zona). La respuesta SHALL ser `200` con todas las entradas compatibles (RN-22), ordenadas por antigüedad, con la representación de la consulta de la lista, sin tope ni deduplicación: son sugerencias para recepción (SU-15). Si el hueco ya empezó, la lista MUST ser vacía.

#### Scenario: Recepción ve solo las entradas compatibles
- **GIVEN** un hueco futuro de 30 minutos de un profesional, una entrada activa compatible, una de una prestación de 90 minutos, una con otro profesional preferido y una cancelada
- **WHEN** `RECEPCION` consulta las entradas compatibles con ese hueco
- **THEN** recibe `200` con solo la entrada compatible

#### Scenario: Las sugerencias se ordenan por antigüedad y muestran todas las entradas del paciente
- **GIVEN** tres entradas compatibles, dos del mismo paciente, creadas en momentos distintos
- **WHEN** `ADMIN` consulta las entradas compatibles
- **THEN** recibe las tres, de la más antigua a la más nueva

#### Scenario: Un hueco que ya empezó no tiene sugerencias
- **GIVEN** un hueco cuyo inicio ya pasó y una entrada que cumpliría la compatibilidad
- **WHEN** `RECEPCION` consulta las entradas compatibles
- **THEN** recibe `200` con una lista vacía

### Requirement: Validación y acceso de la consulta de compatibles

La consulta MUST responder en Problem Details `400` si falta un parámetro, si una fecha está mal formada o si `fin` no es posterior a `inicio`, y `422` con `PROFESIONAL_INEXISTENTE` o `PROFESIONAL_INACTIVO` si el profesional no existe o está inactivo. `ODONTOLOGO` MUST recibir `403` y un cliente sin sesión, `401`.

#### Scenario: La consulta rechaza un hueco inválido
- **WHEN** `RECEPCION` consulta sin `profesionalId`, sin `inicio` o sin `fin`, con una fecha mal formada, o con `fin` igual o anterior a `inicio`
- **THEN** recibe `400` en Problem Details

#### Scenario: La consulta rechaza un profesional inexistente o inactivo
- **WHEN** `RECEPCION` consulta un hueco de un profesional inexistente o inactivo
- **THEN** recibe `422` en Problem Details con `PROFESIONAL_INEXISTENTE` o `PROFESIONAL_INACTIVO`

#### Scenario: El odontólogo no consulta las entradas compatibles
- **WHEN** un odontólogo consulta las entradas compatibles con un hueco
- **THEN** recibe `403` en Problem Details

#### Scenario: Sin sesión no se consultan las entradas compatibles
- **WHEN** un cliente sin sesión consulta las entradas compatibles con un hueco
- **THEN** recibe `401` en Problem Details

# registro-notificaciones Specification

## Purpose
Permite que administración y recepción consulten el registro de notificaciones del consultorio, filtrándolo por estado y por turno, para ver qué emails se enviaron, cuáles fallaron y por qué (US-020, RN-30).

## Requirements

### Requirement: Consulta del registro de notificaciones

`ADMIN` y `RECEPCION` SHALL poder consultar el registro con `GET /api/notificaciones`. Cada elemento MUST incluir `id`, `tipo`, `estado`, `canal`, `destinatario`, `turnoId`, `ofertaHuecoId`, `pacienteId`, `inicioReferencia`, `intentos`, `ultimoError`, `programadaPara` y `enviadaEn`, con los instantes en UTC (ISO-8601). Los elementos se ordenan del registro más reciente al más antiguo.

#### Scenario: Recepción lista el registro de notificaciones
- **GIVEN** tres notificaciones registradas, una `PENDIENTE`, una `ENVIADA` y una `FALLIDA`
- **WHEN** `ADMIN` o `RECEPCION` consulta `GET /api/notificaciones` sin filtros
- **THEN** recibe `200` con las tres en `contenido`, de la registrada más recientemente a la más antigua, cada una con todos sus campos

#### Scenario: Sin notificaciones el registro está vacío
- **GIVEN** que no hay notificaciones registradas
- **WHEN** un usuario autorizado consulta `GET /api/notificaciones`
- **THEN** recibe `200` con `contenido` vacío, `totalElementos` 0 y `totalPaginas` 0

### Requirement: Último error de los envíos fallidos

El registro SHALL mostrar en `ultimoError` el último error de cada notificación `FALLIDA`. Las notificaciones `ENVIADA` y `PENDIENTE` MUST devolver `ultimoError` nulo. El error MUST ser el mensaje registrado por el envío, sin trazas ni detalles internos del servidor.

#### Scenario: Una notificación fallida muestra su último error
- **GIVEN** una notificación `FALLIDA` con 2 intentos y el error «No se pudo enviar la notificación por email»
- **WHEN** un usuario autorizado consulta el registro
- **THEN** ese elemento trae `estado` `FALLIDA`, `intentos` 2 y `ultimoError` «No se pudo enviar la notificación por email»

#### Scenario: Una notificación enviada no trae error
- **GIVEN** una notificación `ENVIADA` con su fecha de envío
- **WHEN** un usuario autorizado consulta el registro
- **THEN** ese elemento trae `ultimoError` nulo y `enviadaEn` con la fecha de envío

### Requirement: Filtros por estado y por turno

El registro SHALL aceptar los filtros opcionales `estado` (`PENDIENTE`, `ENVIADA` o `FALLIDA`) y `turnoId`, combinables entre sí. Un `estado` desconocido MUST responder `400` en Problem Details. Un `turnoId` sin notificaciones MUST devolver una página vacía.

#### Scenario: Filtrar por estado devuelve solo las de ese estado
- **GIVEN** notificaciones `PENDIENTE`, `ENVIADA` y `FALLIDA`
- **WHEN** un usuario autorizado consulta `GET /api/notificaciones?estado=FALLIDA`
- **THEN** `contenido` incluye solo las `FALLIDA` y `totalElementos` es su cantidad

#### Scenario: Filtrar por turno devuelve solo las de ese turno
- **GIVEN** notificaciones de dos turnos distintos y un aviso de oferta de hueco sin turno
- **WHEN** un usuario autorizado consulta `GET /api/notificaciones?turnoId=<id del primer turno>`
- **THEN** `contenido` incluye solo las notificaciones de ese turno

#### Scenario: Los filtros de estado y turno se combinan
- **GIVEN** un turno con una notificación `ENVIADA` y otra `FALLIDA`, y otro turno con una `FALLIDA`
- **WHEN** un usuario autorizado consulta con `estado=FALLIDA` y el `turnoId` del primer turno
- **THEN** `contenido` incluye solo la `FALLIDA` del primer turno

#### Scenario: Un turno sin notificaciones devuelve una página vacía
- **WHEN** un usuario autorizado consulta con un `turnoId` que no tiene notificaciones
- **THEN** recibe `200` con `contenido` vacío y `totalElementos` 0

#### Scenario: Un estado desconocido responde 400
- **WHEN** un usuario autorizado consulta `GET /api/notificaciones?estado=PERDIDA`
- **THEN** recibe `400` en formato Problem Details

### Requirement: Paginación del registro

El registro SHALL paginarse con `pagina` (desde 0, por defecto 0) y `tamanio` (por defecto 20, entre 1 y 100). La respuesta MUST incluir `contenido`, `pagina`, `tamanio`, `totalElementos` y `totalPaginas`, y los totales MUST reflejar los filtros aplicados. Un `pagina` negativo o un `tamanio` fuera de rango MUST responder `400` en Problem Details.

#### Scenario: Sin parámetros se devuelve la primera página de 20
- **GIVEN** 25 notificaciones registradas
- **WHEN** un usuario autorizado consulta `GET /api/notificaciones`
- **THEN** recibe 20 elementos con `pagina` 0, `tamanio` 20, `totalElementos` 25 y `totalPaginas` 2

#### Scenario: La segunda página trae el resto en el mismo orden
- **GIVEN** 25 notificaciones registradas
- **WHEN** un usuario autorizado consulta con `pagina=1` y `tamanio=20`
- **THEN** recibe las 5 más antiguas, que no se repiten con las de la primera página

#### Scenario: Una página posterior a la última viene vacía
- **GIVEN** 3 notificaciones registradas
- **WHEN** un usuario autorizado consulta con `pagina=5`
- **THEN** recibe `200` con `contenido` vacío, `totalElementos` 3 y `totalPaginas` 1

#### Scenario: Parámetros de paginación fuera de rango responden 400
- **WHEN** un usuario autorizado consulta con `pagina=-1`, `tamanio=0` o `tamanio=101`
- **THEN** recibe `400` en formato Problem Details

### Requirement: Acceso restringido al registro

Solo `ADMIN` y `RECEPCION` SHALL acceder al registro. `ODONTOLOGO` MUST recibir `403` y un pedido sin sesión, `401`, ambos en Problem Details y sin datos del registro.

#### Scenario: El odontólogo no puede ver el registro
- **WHEN** un usuario `ODONTOLOGO` consulta `GET /api/notificaciones`
- **THEN** recibe `403` y la respuesta no incluye notificaciones

#### Scenario: Sin sesión el registro responde 401
- **WHEN** un cliente sin sesión consulta `GET /api/notificaciones`
- **THEN** recibe `401` y la respuesta no incluye notificaciones

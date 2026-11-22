# bloqueos-de-agenda Specification

## Purpose

Registrar ausencias por profesional y permitir que la agenda determine si un intervalo candidato está bloqueado, sin considerar solapados los intervalos contiguos.

## Requirements

### Requirement: Alta y listado de bloqueos por profesional

`ADMIN` y `RECEPCION` SHALL poder crear con `POST /api/profesionales/{id}/bloqueos` y listar con `GET /api/profesionales/{id}/bloqueos` para cualquier profesional existente. El alta MUST recibir `inicio` y `fin` como fechas ISO 8601 con offset explícito (`Z` incluido) y hasta seis decimales de segundo, `fin` estrictamente posterior a `inicio` y `motivo` no blanco de hasta 255 caracteres. El alta devuelve `201` y la representación creada; el listado devuelve `200` con un arreglo, ordenado por `inicio` ascendente y luego por `id` para desempatar. Cada representación contiene `id`, `profesionalId`, `inicio`, `fin` y `motivo`, con los instantes normalizados a UTC. Las entradas inválidas MUST devolver `400` en Problem Details sin persistir datos.

#### Scenario: Administración y recepción crean y listan bloqueos del profesional solicitado
- **GIVEN** dos profesionales existentes con bloqueos registrados
- **WHEN** `ADMIN` o `RECEPCION` crea un bloqueo válido con offset `-03:00` y CSRF válido, y lista los bloqueos de ese profesional
- **THEN** el alta devuelve `201` con ID generado e instantes equivalentes en UTC y el listado `200` incluye solo bloqueos del profesional, ordenados por `inicio` e `id`

#### Scenario: Un profesional sin bloqueos devuelve un listado vacío
- **WHEN** `ADMIN` o `RECEPCION` lista los bloqueos de un profesional existente sin registros
- **THEN** recibe `200` con `[]`

#### Scenario: El alta rechaza fechas o motivos inválidos
- **WHEN** un usuario autorizado envía un alta con CSRF válido y falta una fecha, una fecha no tiene offset o supera la precisión de microsegundos, el fin es igual o anterior al inicio, o el motivo falta, es blanco o excede 255 caracteres
- **THEN** recibe `400` en Problem Details y no se crea ningún bloqueo

#### Scenario: Gestionar bloqueos de un profesional inexistente devuelve 404
- **WHEN** `ADMIN` o `RECEPCION` consulta el listado, crea o intenta borrar un bloqueo para un profesional inexistente, con solicitudes por lo demás válidas
- **THEN** recibe `404` en Problem Details con código `PROFESIONAL_NO_ENCONTRADO`

### Requirement: Eliminación de un bloqueo identificado por profesional

`DELETE /api/profesionales/{id}/bloqueos/{bloqueoId}` SHALL permitir a `ADMIN` y `RECEPCION` eliminar el bloqueo de ese profesional y devolver `204` sin cuerpo. Si el profesional existe pero el bloqueo no existe o pertenece a otro profesional, MUST responder `404` en Problem Details con código `BLOQUEO_NO_ENCONTRADO` sin eliminar ningún otro registro.

#### Scenario: Eliminar un bloqueo lo retira del listado y de la consulta de solapamiento
- **GIVEN** un único bloqueo de un profesional en un intervalo
- **WHEN** `ADMIN` o `RECEPCION` lo elimina con CSRF válido
- **THEN** recibe `204` sin cuerpo, el registro no aparece en el listado y ya no solapa en la consulta interna

#### Scenario: Un bloqueo ausente o de otro profesional no se puede eliminar
- **GIVEN** un profesional existente
- **WHEN** un usuario autorizado intenta eliminar un ID de bloqueo inexistente o perteneciente a otro profesional
- **THEN** recibe el mismo `404` con código `BLOQUEO_NO_ENCONTRADO` y no se elimina ningún registro

### Requirement: Acceso a bloqueos según rol y titularidad

Las rutas SHALL requerir sesión del personal. `ODONTOLOGO` SHALL poder listar únicamente los bloqueos de su propio profesional, identificado por un vínculo resuelto en el servidor. Sin vínculo o ante un identificador ajeno, MUST recibir `403` antes de comprobar si el profesional solicitado existe. Para `POST` y `DELETE` MUST recibir `403` aun con CSRF válido. Las escrituras de `ADMIN` y `RECEPCION` MUST exigir CSRF válido.

#### Scenario: El odontólogo vinculado lista sus bloqueos pero no los ajenos
- **GIVEN** un odontólogo vinculado y bloqueos de más de un profesional
- **WHEN** consulta su listado, el de un tercero y el de un ID inexistente
- **THEN** recibe `200` con sus bloqueos para su propio ID y `403` en Problem Details para los otros dos

#### Scenario: El odontólogo sin vínculo no puede listar bloqueos
- **WHEN** un odontólogo sin vínculo resuelto solicita el listado de bloqueos
- **THEN** recibe `403` en Problem Details sin datos de bloqueos

#### Scenario: El odontólogo no puede crear ni eliminar bloqueos
- **WHEN** un odontólogo vinculado envía `POST` o `DELETE` a sus bloqueos con CSRF válido
- **THEN** recibe `403` en Problem Details y los registros no cambian

#### Scenario: Las escrituras de bloqueos requieren CSRF válido
- **WHEN** `ADMIN` o `RECEPCION` con sesión intenta crear o eliminar un bloqueo sin CSRF válido
- **THEN** recibe `403` en Problem Details y los registros no cambian

#### Scenario: Consultar o escribir bloqueos sin sesión devuelve 401
- **WHEN** un cliente sin sesión intenta listar, crear o eliminar un bloqueo
- **THEN** recibe `401` en Problem Details y no se modifican registros

### Requirement: Consulta de solapamiento por profesional

La consulta interna de agenda SHALL indicar si existe al menos un bloqueo del profesional solicitado que se solape con el intervalo candidato. Los intervalos SHALL ser semiabiertos `[inicio, fin)`: existe solapamiento únicamente si `inicioBloqueo < finCandidato` y `finBloqueo > inicioCandidato` (RN-02, RN-04).

#### Scenario: El candidato que cruza un bloqueo del mismo profesional solapa
- **GIVEN** un bloqueo del profesional de 10:00 a 11:00
- **WHEN** se consulta por ese profesional el intervalo de 10:30 a 11:30
- **THEN** la consulta indica solapamiento

#### Scenario: El candidato contiguo a cualquiera de los extremos no solapa
- **GIVEN** un bloqueo del profesional de 10:00 a 11:00
- **WHEN** se consulta por ese profesional el intervalo de 09:30 a 10:00 o de 11:00 a 11:30
- **THEN** la consulta no indica solapamiento en ninguno de los casos

#### Scenario: Los bloqueos de otro profesional no afectan la consulta
- **GIVEN** un bloqueo de 10:00 a 11:00 que pertenece solo a otro profesional
- **WHEN** se consulta el intervalo de 10:30 a 11:30 para el profesional solicitado
- **THEN** la consulta no indica solapamiento

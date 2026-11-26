# catalogo-profesionales Specification

## Purpose

Mantener el catálogo interno de profesionales sin perder su historial y ofrecer a los módulos consumidores consultas consistentes por ID y por estado activo.

## Requirements

### Requirement: Alta y actualización de profesionales

`ADMIN` SHALL poder crear con `POST /api/profesionales` y actualizar con `PUT /api/profesionales/{id}`. Nombre, apellido y matrícula MUST ser textos no blancos; especialidad es opcional. La matrícula MUST ser única entre activos e inactivos, comparada después de quitar espacios de los extremos y distinguiendo mayúsculas de minúsculas. El alta deja el registro activo; la actualización requiere `activo` explícito. La representación devuelve `id`, `nombre`, `apellido`, `matricula`, `especialidad` y `activo`.

#### Scenario: El administrador da de alta un profesional activo
- **WHEN** `ADMIN` envía un alta válida sin especialidad
- **THEN** recibe `201`, `Location` con el detalle y un registro activo con ID generado y especialidad nula

#### Scenario: El administrador modifica los datos de un profesional
- **GIVEN** un profesional existente
- **WHEN** `ADMIN` actualiza sus datos válidos con estado explícito
- **THEN** recibe `200` con los nuevos datos y el mismo ID

#### Scenario: El catálogo rechaza datos obligatorios inválidos
- **WHEN** `ADMIN` envía un alta o actualización con nombre, apellido o matrícula en blanco, o una actualización sin `activo`
- **THEN** recibe `400` en formato Problem Details sin cambios persistidos

#### Scenario: Un alta no puede reutilizar la matrícula de un profesional inactivo
- **GIVEN** un profesional inactivo con matrícula `MP-0001`
- **WHEN** `ADMIN` intenta dar de alta otro con matrícula ` MP-0001 `
- **THEN** recibe `409` con código `PROFESIONAL_MATRICULA_DUPLICADA` y no se crea otro registro

#### Scenario: Una actualización no puede tomar la matrícula de otro profesional
- **GIVEN** dos profesionales con matrículas distintas
- **WHEN** `ADMIN` intenta asignar al primero la matrícula del segundo
- **THEN** recibe `409` con código `PROFESIONAL_MATRICULA_DUPLICADA` y ambos quedan sin cambios

### Requirement: Baja lógica y reactivación

`ADMIN` SHALL poder desactivar o reactivar con `PUT` sin cambiar el ID ni eliminar la fila. El catálogo MUST NOT ofrecer borrado físico. Si el profesional está activo y se solicita `activo=false` mientras existen turnos futuros activos (`PROGRAMADO` o `CONFIRMADO` con `fin` posterior al instante actual del reloj) asignados a ese profesional, el sistema MUST rechazar la actualización con `409` en formato Problem Details, `codigo` igual a `RN-37`, `recurso` igual a `PROFESIONAL` y la propiedad `turnos` con la lista de turnos en conflicto (`id`, `inicio`, `fin`, `estado` ordenados por `inicio` e `id`, sin datos del paciente), conservando el profesional activo (RN-37, RN-35).

#### Scenario: El administrador desactiva y reactiva el mismo registro
- **WHEN** `ADMIN` actualiza un profesional a inactivo y luego a activo
- **THEN** ambas actualizaciones devuelven `200`, conservan el mismo ID y el detalle refleja cada estado

#### Scenario: El catálogo no permite borrar físicamente un profesional
- **WHEN** `ADMIN` envía `DELETE /api/profesionales/{id}` para un profesional existente con CSRF válido
- **THEN** la operación no está disponible y el registro queda intacto

#### Scenario: Desactivar un profesional con turnos futuros activos devuelve 409 con la lista de conflictos
- **GIVEN** un profesional activo con un turno futuro en estado `PROGRAMADO` o `CONFIRMADO`
- **WHEN** `ADMIN` envía `PUT /api/profesionales/{id}` con `activo=false` y CSRF válido
- **THEN** recibe `409` en formato Problem Details con `codigo` igual a `RN-37`, `recurso` igual a `PROFESIONAL` y `turnos` con el turno en conflicto
- **AND** el profesional permanece con `activo=true`

#### Scenario: Cancelar los turnos futuros permite desactivar al profesional
- **GIVEN** un profesional activo cuyo único turno futuro activo fue cancelado
- **WHEN** `ADMIN` envía `PUT /api/profesionales/{id}` con `activo=false` y CSRF válido
- **THEN** recibe `200` con `activo=false`

### Requirement: Lectura y escritura según el rol

`GET /api/profesionales` y `GET /api/profesionales/{id}` SHALL requerir autenticación. `ADMIN` y `RECEPCION` SHALL ver ambos estados, con el listado ordenado por ID. `ODONTOLOGO` SHALL ver únicamente su propio registro, si existe un vínculo resuelto del lado servidor; hasta que C-13 provea el vínculo no tiene acceso. El detalle ajeno MUST responder `403` sin revelar si el ID existe. Solo `ADMIN` SHALL poder escribir.

#### Scenario: Administración y recepción consultan activos e inactivos
- **GIVEN** profesionales de ambos estados
- **WHEN** `ADMIN` o `RECEPCION` consulta el listado o un detalle existente
- **THEN** recibe `200`, con ambos estados en el listado ordenado por ID y sin excluir inactivos del detalle

#### Scenario: Consultar o actualizar un profesional inexistente responde 404
- **WHEN** un usuario autorizado para acceder a cualquier registro pide un detalle o `ADMIN` intenta actualizar un ID inexistente
- **THEN** recibe `404` con código `PROFESIONAL_NO_ENCONTRADO`

#### Scenario: Un odontólogo sin vínculo no puede leer el catálogo
- **GIVEN** un `ODONTOLOGO` sin vínculo resuelto
- **WHEN** consulta el listado o un detalle
- **THEN** recibe `403` en formato Problem Details sin datos del catálogo

#### Scenario: Un odontólogo vinculado solo puede consultar su propio registro
- **GIVEN** un `ODONTOLOGO` con un vínculo resuelto por el servidor
- **WHEN** consulta el listado, su detalle o un detalle con otro ID
- **THEN** el listado y el detalle propio solo muestran su registro y cualquier otro ID devuelve `403`, exista o no

#### Scenario: Recepción y odontología no pueden escribir en el catálogo
- **WHEN** `RECEPCION` u `ODONTOLOGO` envía un alta o actualización con CSRF válido
- **THEN** recibe `403` en formato Problem Details y no hay cambios persistidos

#### Scenario: Una consulta anónima del catálogo requiere autenticación
- **WHEN** un cliente sin autenticar solicita el listado o un detalle
- **THEN** recibe `401` en formato Problem Details

### Requirement: Consultas internas de profesionales

El catálogo SHALL ofrecer a los módulos consumidores un listado solo de activos, ordenado por ID, y una búsqueda por ID que distingue activo, inactivo e inexistente.

#### Scenario: La consulta interna de candidatos excluye inactivos
- **GIVEN** profesionales activos e inactivos
- **WHEN** un módulo consulta los candidatos activos
- **THEN** obtiene solo los profesionales activos ordenados por ID

#### Scenario: La consulta interna por ID distingue inactivo de inexistente
- **WHEN** un módulo busca por ID un profesional activo, uno inactivo y uno inexistente
- **THEN** obtiene cada registro existente con su estado y una ausencia explícita para el que no existe

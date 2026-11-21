# Spec Delta

## Purpose

Permite al personal consultar el catálogo de sillones, al administrador mantenerlo sin perder referencias históricas y a la agenda identificar los sillones habilitados para nuevos turnos.

## ADDED Requirements

### Requirement: Consultas del catálogo por el personal
`GET /api/sillones` y `GET /api/sillones/{id}` SHALL responder `200` a `ADMIN`, `RECEPCION` y `ODONTOLOGO` para consultas válidas. El listado SHALL incluir activos e inactivos; cada elemento y el detalle SHALL devolver `id`, `nombre`, `descripcion` y `activo`. Un ID válido pero inexistente SHALL devolver `404` en formato Problem Details.

#### Scenario: Los tres roles consultan sillones activos e inactivos
- **GIVEN** un sillón activo y otro inactivo
- **WHEN** cada rol del personal consulta el listado y el detalle de ambos
- **THEN** recibe `200` con los campos del catálogo y el valor real de `activo` en cada sillón

#### Scenario: Consultar un ID inexistente devuelve 404
- **WHEN** un usuario del personal consulta el detalle de un ID válido que no existe
- **THEN** recibe `404` en formato Problem Details

### Requirement: Alta y edición con nombre único
`POST /api/sillones` SHALL crear un sillón activo y responder `201` con su representación y la ubicación del detalle. `PUT /api/sillones/{id}` SHALL actualizar un sillón existente y responder `200` con su representación; un ID inexistente SHALL devolver `404` en formato Problem Details. El cuerpo de alta SHALL requerir `nombre` no vacío y admitir `descripcion` opcional. El cuerpo de actualización SHALL aceptar esos mismos campos y requerir además `activo` booleano. El nombre, sin espacios iniciales ni finales y distinguiendo mayúsculas y minúsculas, SHALL ser único entre todos los sillones, incluidos los inactivos. Una colisión SHALL devolver `409` en formato Problem Details y una entrada que no cumpla las validaciones de campos SHALL devolver `400` en ese mismo formato.

#### Scenario: El administrador da de alta un sillón activo
- **WHEN** `ADMIN` registra un sillón con nombre disponible y descripción omitida
- **THEN** recibe `201`, la ubicación del detalle y un ID nuevo con `activo=true`

#### Scenario: El administrador edita un sillón sin cambiar su identidad
- **WHEN** `ADMIN` actualiza el nombre o la descripción de un sillón existente con datos válidos
- **THEN** recibe `200` con los datos actualizados y el mismo ID

#### Scenario: El nombre duplicado devuelve 409 incluso si el otro sillón está inactivo
- **GIVEN** un nombre en uso
- **WHEN** `ADMIN` intenta crear un sillón o renombrar otro al mismo nombre, incluso con espacios en los extremos
- **THEN** recibe `409` en formato Problem Details y el catálogo no cambia, tanto si el nombre pertenece a un sillón activo como a uno inactivo

#### Scenario: Dos altas concurrentes del mismo nombre no crean duplicados
- **WHEN** dos administradores registran simultáneamente sillones con el mismo nombre libre
- **THEN** una alta responde `201`, la otra `409` en formato Problem Details y queda un solo registro con ese nombre

#### Scenario: Una escritura con campos inválidos devuelve 400
- **WHEN** `ADMIN` envía un alta o actualización con nombre vacío, o una actualización sin `activo`
- **THEN** recibe `400` en formato Problem Details con los campos inválidos, sin modificar el catálogo

#### Scenario: Actualizar un ID inexistente devuelve 404
- **WHEN** `ADMIN` envía una actualización válida para un ID que no existe
- **THEN** recibe `404` en formato Problem Details y no se crea un sillón

### Requirement: Baja lógica y reactivación
El administrador SHALL poder desactivar o reactivar un sillón mediante `PUT /api/sillones/{id}` con `activo=false` o `activo=true`, respectivamente. Estas transiciones MUST conservar el mismo registro y el mismo ID; no existe una operación de borrado físico.

#### Scenario: Desactivar un sillón conserva su registro
- **WHEN** `ADMIN` actualiza un sillón activo con `activo=false`
- **THEN** recibe `200` y el mismo ID sigue disponible en listado y detalle con `activo=false`

#### Scenario: Reactivar un sillón conserva su registro
- **WHEN** `ADMIN` actualiza un sillón inactivo con `activo=true`
- **THEN** recibe `200` y el mismo ID aparece en listado y detalle con `activo=true`

### Requirement: Acceso protegido al catálogo
Las rutas del catálogo SHALL aplicar el contrato compartido de autenticación y CSRF. Solo `ADMIN` SHALL poder ejecutar `POST` y `PUT`, incluida la desactivación y la reactivación. Los rechazos de acceso SHALL usar el formato Problem Details de `control-acceso`.

#### Scenario: Recepción y odontología no pueden escribir en el catálogo
- **GIVEN** un usuario con rol `RECEPCION` u `ODONTOLOGO` y CSRF válido
- **WHEN** intenta un alta, una edición, una desactivación o una reactivación
- **THEN** recibe `403` en formato Problem Details y el catálogo no cambia

#### Scenario: Un cliente sin sesión no puede acceder al catálogo
- **WHEN** un cliente anónimo consulta o intenta escribir en una ruta del catálogo
- **THEN** recibe `401` en formato Problem Details

#### Scenario: Un administrador sin CSRF válido no puede escribir en el catálogo
- **GIVEN** un administrador autenticado
- **WHEN** intenta una escritura sin token CSRF o con un token inválido
- **THEN** recibe `403` en formato Problem Details y el catálogo no cambia

### Requirement: Consulta interna de sillones para agenda
La consulta interna del catálogo SHALL listar solo sillones activos como candidatos para agenda; estar activo no garantiza que el sillón esté libre. La consulta por ID SHALL devolver un sillón existente con su estado y distinguirlo de la ausencia, sin filtrar los inactivos.

#### Scenario: Agenda deja de ofrecer un sillón desactivado
- **GIVEN** dos sillones activos
- **WHEN** se desactiva uno y agenda consulta los candidatos
- **THEN** el sillón desactivado no aparece y el otro sigue disponible como candidato

#### Scenario: Agenda vuelve a ofrecer un sillón reactivado
- **GIVEN** un sillón inactivo que no aparece entre los candidatos
- **WHEN** se reactiva y agenda consulta los candidatos
- **THEN** el sillón vuelve a aparecer con su ID original

#### Scenario: Agenda distingue un sillón inactivo de uno inexistente
- **WHEN** agenda consulta por ID un sillón activo, uno inactivo y un ID inexistente
- **THEN** obtiene los dos sillones existentes con su estado real y solo el ID inexistente devuelve ausencia

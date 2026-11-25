# Spec Delta

## MODIFIED Requirements

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

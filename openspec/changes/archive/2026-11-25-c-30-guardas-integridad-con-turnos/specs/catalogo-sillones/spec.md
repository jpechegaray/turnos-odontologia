# Spec Delta

## MODIFIED Requirements

### Requirement: Baja lógica y reactivación
El administrador SHALL poder desactivar o reactivar un sillón mediante `PUT /api/sillones/{id}` con `activo=false` o `activo=true`, respectivamente. Estas transiciones MUST conservar el mismo registro y el mismo ID; no existe una operación de borrado físico. Si el sillón está activo y se solicita `activo=false` mientras existen turnos futuros activos (`PROGRAMADO` o `CONFIRMADO` con `fin` posterior al instante actual del reloj) asignados a ese sillón, el sistema MUST rechazar la operación con `409` en formato Problem Details, `codigo` igual a `RN-37`, `recurso` igual a `SILLON` y la propiedad `turnos` con la lista de turnos en conflicto (`id`, `inicio`, `fin`, `estado` ordenados por `inicio` e `id`, sin datos del paciente), conservando el sillón activo (RN-37, RN-35).

#### Scenario: Desactivar un sillón conserva su registro
- **WHEN** `ADMIN` actualiza un sillón activo con `activo=false`
- **THEN** recibe `200` y el mismo ID sigue disponible en listado y detalle con `activo=false`

#### Scenario: Reactivar un sillón conserva su registro
- **WHEN** `ADMIN` actualiza un sillón inactivo con `activo=true`
- **THEN** recibe `200` y el mismo ID aparece en listado y detalle con `activo=true`

#### Scenario: Desactivar un sillón con turnos futuros activos devuelve 409 con la lista de conflictos
- **GIVEN** un sillón activo con un turno futuro en estado `PROGRAMADO` o `CONFIRMADO`
- **WHEN** `ADMIN` envía `PUT /api/sillones/{id}` con `activo=false` y CSRF válido
- **THEN** recibe `409` en formato Problem Details con `codigo` igual a `RN-37`, `recurso` igual a `SILLON` y `turnos` con el turno en conflicto
- **AND** el sillón permanece con `activo=true`

#### Scenario: Cancelar los turnos futuros permite desactivar el sillón
- **GIVEN** un sillón activo cuyo único turno futuro activo fue cancelado
- **WHEN** `ADMIN` envía `PUT /api/sillones/{id}` con `activo=false` y CSRF válido
- **THEN** recibe `200` con `activo=false`

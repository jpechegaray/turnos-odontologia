# Spec Delta

## ADDED Requirements

### Requirement: Guarda contra desactivación de prestación con turnos futuros activos

El sistema MUST rechazar que una prestación activa pase a inactiva (`DELETE /api/prestaciones/{id}` o `PUT /api/prestaciones/{id}` con `activa=false`) si tiene turnos `PROGRAMADO` o `CONFIRMADO` con `fin` posterior al reloj actual (RN-37), respondiendo `409` en Problem Details con `codigo` `RN-37`, `recurso` `PRESTACION` y `turnos` (`id`, `inicio`, `fin`, `estado` ordenados por `inicio` e `id`, sin datos del paciente, RN-35). Si ya estaba inactiva no MUST bloquearse.

#### Scenario: Dar de baja o desactivar una prestación con turnos futuros activos devuelve 409 con la lista de conflictos
- **GIVEN** una prestación activa con un turno futuro en estado `PROGRAMADO` o `CONFIRMADO`
- **WHEN** `ADMIN` intenta desactivarla con `DELETE /api/prestaciones/{id}` o con `PUT /api/prestaciones/{id}` enviando `activa=false`
- **THEN** en ambos casos recibe `409` en formato Problem Details con `codigo` igual a `RN-37`, `recurso` igual a `PRESTACION` y `turnos` con el turno en conflicto
- **AND** la prestación sigue con `activa=true`

#### Scenario: Cancelar los turnos futuros permite dar de baja la prestación
- **GIVEN** una prestación activa cuyo único turno futuro activo fue cancelado
- **WHEN** `ADMIN` envía `DELETE /api/prestaciones/{id}` con CSRF válido
- **THEN** recibe `204` y la prestación queda con `activa=false`

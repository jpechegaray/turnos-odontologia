# Proposal

## Why

Hoy es posible crear un bloqueo de agenda sobre un intervalo que ya tiene turnos activos (`PROGRAMADO` o `CONFIRMADO`), o desactivar un sillón, un profesional o una prestación que todavía tiene turnos futuros activos asignados, dejando la agenda en un estado inconsistente. Con la entidad `Turno` ya implementada (C-19) y la cancelación disponible (C-27), este change cierra las guardas de integridad entre turnos y catálogos/bloqueos exigidas por **RN-17** y **RN-37** (US-003, US-004, US-005 y US-008).

## What Changes

- Se expone en `agenda.application` el puerto de consulta `TurnosActivosConsulta` (junto con el record `TurnoEnConflicto(id, inicio, fin, estado)`) y su adaptador de persistencia en `agenda.infrastructure` en archivos nuevos, sin modificar `TurnoJpaRepository` ni `TurnosPersistencia` (mitigación RM-04).
- Al crear un bloqueo (`POST /api/profesionales/{id}/bloqueos`), si el intervalo `[inicio, fin)` se solapa con turnos activos (`PROGRAMADO` o `CONFIRMADO`) de ese profesional (borde contiguo aceptado según RN-02), se rechaza la operación con `409 Conflict` en formato Problem Details (`codigo: "RN-17"`) incluyendo la lista `turnos` de conflictos ordenada por `inicio ASC, id ASC` y sin datos personales de pacientes (RN-17, RN-35).
- Al desactivar un sillón (`PUT /api/sillones/{id}` pasando de `activo: true -> false`), un profesional (`PUT /api/profesionales/{id}` pasando de `activo: true -> false`) o una prestación (`DELETE /api/prestaciones/{id}` o `PUT /api/prestaciones/{id}` pasando de `activa: true -> false`), si existen turnos activos (`PROGRAMADO` o `CONFIRMADO`) con `fin > ahora` (medido con el `Clock` inyectable), se rechaza con `409 Conflict` en formato Problem Details (`codigo: "RN-37"`, `recurso: "SILLON" | "PROFESIONAL" | "PRESTACION"`) y la lista `turnos` de conflictos (RN-37, RN-35).

## Capabilities

### New Capabilities

### Modified Capabilities
- `agenda-turnos`: expone la consulta interna `TurnosActivosConsulta` para obtener turnos activos (`PROGRAMADO` o `CONFIRMADO`) de un profesional que se solapan con un intervalo `[inicio, fin)` y turnos futuros activos (`fin > ahora`) por profesional, sillón o prestación.
- `bloqueos-de-agenda`: impide dar de alta un bloqueo que se superponga con turnos activos del profesional, respondiendo `409` con código `RN-17` y la lista `turnos` en conflicto, aceptando bordes contiguos.
- `catalogo-sillones`: impide desactivar un sillón que tenga turnos futuros activos, respondiendo `409` con código `RN-37`, `recurso: "SILLON"` y la lista `turnos` en conflicto.
- `catalogo-profesionales`: impide desactivar un profesional que tenga turnos futuros activos, respondiendo `409` con código `RN-37`, `recurso: "PROFESIONAL"` y la lista `turnos` en conflicto.
- `catalogo-prestaciones`: impide desactivar una prestación (tanto por `DELETE` como por `PUT` con transición a `activa=false`) que tenga turnos futuros activos, respondiendo `409` con código `RN-37`, `recurso: "PRESTACION"` y la lista `turnos` en conflicto.

## Impact

- **Módulo `agenda`**: archivos nuevos `TurnosActivosConsulta.java` (en `agenda.application`) y `TurnosActivosPersistencia.java` (en `agenda.infrastructure`).
- **Módulo `profesionales`**: `ServicioBloqueos`, `ServicioSillones` y `ServicioProfesionales` incorporan la verificación contra `TurnosActivosConsulta`.
- **Módulo `prestaciones`**: `CatalogoPrestaciones` incorpora la verificación contra `TurnosActivosConsulta` en `darDeBaja` y en `modificar` cuando transiciona de activa a inactiva.
- **Tests**: tests unitarios y de integración con Testcontainers (PostgreSQL real) verificando el rechazo `409` con el listado de turnos, la aceptación del borde contiguo y el éxito tras cancelar los turnos en conflicto.

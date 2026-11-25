# Proposal

## Why

C-19 ya garantiza la creación sin solapamientos. Recepción necesita encontrar horarios posibles antes de intentar crear un turno; el mismo cálculo servirá para C-38 y C-43.

## What Changes

- Calculador puro de disponibilidad sobre instantáneas de horarios, bloqueos, ocupaciones y sillones activos.
- Consulta `GET /api/agenda/disponibilidad` para recepción y administración, por profesional, prestación y rango de instantes con zona; duración opcional.
- Respuesta ordenada con inicio, fin y un sillón activo libre por inicio, sin datos de pacientes.
- Tests de bordes, permisos y creación efectiva de un turno sugerido.

## Capabilities

### New Capabilities

- `agenda-disponibilidad`: búsqueda de intervalos disponibles para una prestación y profesional.

### Modified Capabilities

Ninguna. La creación sigue revalidando y PostgreSQL conserva la garantía ante concurrencia.

## Impact

Archivos nuevos en agenda y un puerto de lectura con adaptador en profesionales. No se modifica `TurnoJpaRepository`, la entidad Turno, el esquema ni C-27. Sin frontend, reservas públicas, horizonte o anticipación mínima de RN-27. Gobernanza MEDIUM; implementación solicitada por Juan Pablo.

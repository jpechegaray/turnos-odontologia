# Proposal

## Why

Recepción puede dar y cancelar turnos, pero todavía no puede registrar si el paciente vino: un turno que ya transcurrió queda `PROGRAMADO` para siempre y no hay registro de la atención (US-014). C-29 agrega esa marca. Desbloquea C-46 (front de reprogramar, cancelar y asistencia). Su única dependencia, C-19, ya está en `master`.

## What Changes

- Agregar `POST /api/turnos/{id}/asistencia` con un cuerpo `{ "estado": "ATENDIDO" | "AUSENTE" }`. Responde `200` con el turno actualizado.
- **Primera marca** (`RECEPCION` y `ADMIN`): solo sobre un turno `PROGRAMADO` o `CONFIRMADO` y solo desde su `inicio` (RN-20). A la hora exacta de inicio ya se puede marcar; antes responde `422 RN-20`.
- **Corrección** (solo `ADMIN`): sobre un turno ya `ATENDIDO` o `AUSENTE` se puede pasar al otro estado. `RECEPCION` recibe `403`. Marcar el mismo estado que ya tiene el turno responde `409`.
- Un turno `CANCELADO` no admite asistencia: `409 TURNO_TRANSICION_INVALIDA`, el mismo código que usa C-27.
- Orden de los chequeos: el turno existe (`404 TURNO_INEXISTENTE`), después el estado (`409`), después el permiso de corrección (`403`), después el reloj (`422`).
- `ODONTOLOGO` y un CSRF inválido reciben `403`, y la falta de sesión `401`, antes de validar el cuerpo (RN-20, RN-33).
- Un estado destino ausente o distinto de `ATENDIDO` y `AUSENTE` (por ejemplo `CANCELADO` o `CONFIRMADO`) responde `400`.
- **Excepción acotada a RN-12:** `ATENDIDO` y `AUSENTE` siguen siendo estados finales para el resto del sistema (cancelar, reprogramar); la tabla de `EstadoTurno` no cambia. La corrección es una operación propia de asistencia, no una transición general.
- Quedan fuera:
  - la auditoría de la corrección (quién y cuándo corrigió): `Turno` no tiene columnas para eso y la ficha no lo pide;
  - eventos de dominio y emails por asistencia;
  - la política para ausencias reiteradas (PA-13): solo se registra;
  - el frontend (C-46).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `agenda-turnos`: se agrega el registro de asistencia y ausencia (primera marca y corrección por `ADMIN`) y se acota la regla de estados finales con esa corrección.

## Impact

- `agenda.domain`: `Turno` gana `marcarAsistencia(destino, ahora)` y `corregirAsistencia(destino)`. `EstadoTurno` no cambia.
- `agenda.application`: nuevo `MarcarAsistenciaService`.
- `agenda.web`: `POST /api/turnos/{id}/asistencia` con `SolicitudMarcarAsistencia`; `RespuestaTurno` no cambia.
- `SecurityConfig`: matcher de escritura de `POST /api/turnos/{id}/asistencia` para `ADMIN` y `RECEPCION`.
- Sin cambios de esquema ni de `schema-constraints.sql`: solo se modifica la columna `estado` existente.
- Se reutilizan sin cambios `TurnosPersistencia.buscarParaActualizar` y `guardar` (con su traducción a `409 TURNO_MODIFICADO`) de C-27.

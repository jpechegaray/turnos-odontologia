# Proposal

## Why

Hoy recepción solo puede cancelar un turno y dar otro, y eso pierde la identidad del turno: el historial, el token y el aviso al paciente. RN-18 pide reprogramar el **mismo** turno. C-35 desbloquea C-44 (emails de reprogramación) y C-46 (frontend). Sus dependencias, C-19 y C-27, ya están en `master`. El líder resolvió PA-06 el 2026-11-25: la reprogramación tardía se permite y se registra.

## What Changes

- **Endpoint:** `POST /api/turnos/{id}/reprogramar` para `RECEPCION` y `ADMIN`, con:
  - `inicio`, obligatorio;
  - `profesionalId`, `sillonId` y `duracionMin`, opcionales: si faltan, se conservan los del turno;
  - la prestación y el paciente no cambian.

  Responde `200` con el turno reprogramado.
- **Orden de las validaciones:**
  1. el turno existe → si no, `404 TURNO_INEXISTENTE`;
  2. está `PROGRAMADO` o `CONFIRMADO` → si no, `409 TURNO_TRANSICION_INVALIDA`, como la cancelación de C-27;
  3. no empezó → si no, `422 RN-05` (decisión del líder);
  4. el horario nuevo pasa las mismas validaciones que el alta (RN-01 a RN-04, RN-09, RN-11, RN-13), **excluyendo al propio turno** (RN-18). Así un turno puede moverse 15 minutos dentro de su propio intervalo. Un conflicto responde `409 RN-01` y un horario fuera de atención, `422 RN-04`.
- **Reprogramación tardía:** se calcula con `PoliticaCancelacion` de C-27 sobre el **inicio original** y el plazo `cancelacion.plazo_horas`. Se permite y queda `reprogramadoTarde = true` (SU-07, PA-06). La marca no se borra si después se reprograma a tiempo.
- **Lo que se conserva:** el turno guarda su `id`, su estado (`PROGRAMADO` o `CONFIRMADO`), las notas y el canal. Si el pedido no cambia nada, responde `200` sin modificar ni publicar eventos.
- **Eventos:** se publica `TurnoReprogramado` (anterior y nuevo) y un `HuecoLiberado` por cada tramo del intervalo anterior que quedó libre (RN-18, decisión del líder):
  - con el mismo profesional, solo la diferencia entre el intervalo anterior y el nuevo, que puede ser vacía, uno o dos tramos;
  - con otro profesional, el intervalo anterior completo.
- **Concurrencia:** lock de la fila del turno y después los advisory locks del profesional y el sillón nuevos, como el alta. El conflicto de versión responde `409 TURNO_MODIFICADO` (C-27).
- **`RespuestaTurno`** suma `reprogramadoTarde`.
- **Acceso:** `ODONTOLOGO` recibe `403`, y el paciente no reprograma por token (RN-18, IN-02).
- **Quedan fuera:**
  - recalcular la expiración del token (C-32 escucha `TurnoReprogramado`);
  - el email (C-44) y el aviso a la lista de espera (C-36 escucha `HuecoLiberado`);
  - el frontend (C-46).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `agenda-turnos`: se agrega la reprogramación de turnos por el personal, con su validación, la marca de tardía, los eventos y la concurrencia.

## Impact

- **`shared.tiempo.Intervalo`:** nuevo `menos(otro)`, que devuelve los tramos que no se solapan.
- **`agenda.domain`:**
  - `Turno` gana `exigirReprogramable(ahora)` y `reprogramar(...)`;
  - `TurnoReprogramado.de(anterior, nuevo)`;
  - `HuecoLiberado.porReprogramacion(anterior, nuevo)`.
- **`agenda.application`:** nuevo `ReprogramarTurnoService`. Reutiliza `ValidadorTurno` con `turnoExcluidoId` y la consulta de recursos de `CrearTurnoService`, extraída a un componente compartido.
- **`agenda.web`:** `POST /api/turnos/{id}/reprogramar`, `SolicitudReprogramarTurno` y `RespuestaTurno.reprogramadoTarde`.
- **`SecurityConfig`:** matcher de escritura para `POST /api/turnos/{id}/reprogramar`.
- **KB:** PA-06 queda resuelta en `knowledge-base/10_preguntas_abiertas.md`, y se actualiza la nota de `CLAUDE.md` y `AGENTS.md`.
- No hay cambios de esquema: `reprogramado_tarde` ya existe desde C-19.

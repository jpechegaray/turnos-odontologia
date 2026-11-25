# Proposal

## Why

Recepción todavía no puede cancelar un turno, así que un horario dado nunca se recupera. Cancelar y liberar el hueco es el paso siguiente del camino crítico (C-19 → C-27 → C-36 → C-42): sin el evento de hueco liberado no hay aviso a la lista de espera, que es el diferenciador del producto. Además, C-27 desbloquea C-35 (reprogramar), C-36, C-39, C-44 y C-46. Sus dependencias (C-08 y C-19) ya están en `master`.

## What Changes

- Agregar `POST /api/turnos/{id}/cancelar` para que `RECEPCION` y `ADMIN` cancelen un turno, con un motivo opcional (hasta 255 caracteres). Responde `200` con el turno cancelado.
- La cancelación aplica, en este orden:
  1. El turno existe → si no, `404 TURNO_INEXISTENTE`.
  2. El turno está `PROGRAMADO` o `CONFIRMADO` (RN-12) → si no, `409 TURNO_TRANSICION_INVALIDA`.
  3. El turno no empezó: el momento actual es anterior al `inicio` (RN-05) → si no, `422 RN-05`.
- `PoliticaCancelacion`, una regla pura de dominio, decide si la cancelación es tardía: `tardía = (inicio − ahora) < plazo`, con el plazo de `cancelacion.plazo_horas` y el límite inclusivo (a plazo exacto no es tardía). El reloj es el `Clock` inyectable (RN-05, RN-14).
- El turno pasa a `CANCELADO` y registra `cancelado_en`, `cancelado_por` (`USUARIO` o `PACIENTE_TOKEN`), el usuario que canceló, el motivo y `cancelacion_tardia` (RN-19). Se agrega la columna `cancelado_por_usuario_id`.
- Al quedar `CANCELADO`, las restricciones `EXCLUDE` de C-19 ya no lo consideran y el intervalo queda libre: se puede crear otro turno en el mismo horario (RN-06, RN-10).
- Se publican `TurnoCancelado` y `HuecoLiberado`, una sola vez por cancelación. Dos cancelaciones simultáneas del mismo turno se ordenan con un lock de la fila: una responde `200` y la otra `409`.
- Un conflicto de versión (`@Version`) en cualquier modificación de turno responde `409` en lugar de `500`.
- `ODONTOLOGO` recibe `403` al intentar cancelar (RN-20, RN-33).
- Quedan fuera:
  - el aviso a la lista de espera y las entradas compatibles en la respuesta (C-36, que escucha `HuecoLiberado`);
  - la cancelación por token del paciente: el servicio admite el actor `PACIENTE_TOKEN`, pero el endpoint llega con C-32/C-39;
  - el email de cancelación (C-44) y la revocación del token del turno (C-32);
  - el frontend (C-46).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `agenda-turnos`: se agrega la cancelación de turnos por el personal, con el registro de la cancelación, el cálculo de cancelación tardía, la liberación del intervalo y los eventos para la lista de espera.

## Impact

- `agenda.domain`: nuevo `PoliticaCancelacion` y `ActorCancelacion`; `Turno` gana `cancelar(...)` y el campo `canceladoPorUsuarioId`; `TurnoCancelado` y `HuecoLiberado` ganan la fábrica `de(turno)`.
- `agenda.application`: nuevo `CancelarTurnoService`.
- `agenda.infrastructure`: `TurnoJpa` suma `cancelado_por_usuario_id` (FK a `usuario` en `schema-constraints.sql`, idempotente) y el repositorio una lectura con `PESSIMISTIC_WRITE`.
- `agenda.web`: `POST /api/turnos/{id}/cancelar`; `RespuestaTurno` suma los campos de cancelación.
- `shared.web.ManejadorErrores`: traduce el conflicto de bloqueo optimista a `409`.
- `SecurityConfig`: matcher de escritura de `POST /api/turnos/{id}/cancelar`.
- Se consume sin modificarlo `ConfiguracionServicio.plazoCancelacionHoras()`.

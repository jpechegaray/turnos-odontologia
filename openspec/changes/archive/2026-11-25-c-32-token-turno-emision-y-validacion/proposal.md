# C-32 — Emisión y validación del token de turno

## Why

El paciente necesita un enlace que permita gestionar exclusivamente su turno, sin cuenta ni sesión (RN-07, RN-25, US-016). C-14 ya provee la generación segura y C-19 el turno persistido; falta asociar el secreto con ese turno y mantener su vigencia ante cambios de agenda.

## What Changes

- Incorporar `TokenTurno` y `ServicioTokenTurno` en `reservapublica.token`: emitir para un turno existente, devolver el secreto solo al emitir y guardar únicamente su hash junto con los metadatos.
- Validar la pertenencia al turno y la vigencia hasta el inicio, excluido. Usar un único error equivalente a `404` para cualquier rechazo del token, sin revelar la causa ni escribir el secreto en logs.
- Revocar los tokens asociados al recibir `TurnoCancelado` y actualizar el vencimiento al recibir `TurnoReprogramado`, sin cambiar el enlace.
- Quedan fuera los endpoints de gestión del paciente (C-39), los flujos de reserva y toma de hueco (C-41, C-42), los recordatorios (C-37) y la publicación de eventos de cancelación y reprogramación (C-27, C-35). No se emite un token automáticamente al crear un turno interno.

## Capabilities

### New Capabilities

- `token-turno`: emisión, persistencia, validación acotada al turno, revocación y actualización de vigencia.

### Modified Capabilities

Ninguna. Se reutilizan `token-seguro`, `agenda-turnos`, `reloj-del-sistema` y el contrato de `errores-api` sin cambiar sus requisitos.

## Impact

- Backend y PostgreSQL: nueva tabla `token_turno` gestionada con Hibernate, vinculada a `turno`; sin librerías nuevas.
- No cambia la API HTTP ni el frontend. El contrato de rechazo queda disponible para C-39.
- C-14 y C-19 ya están integrados en `master`. Sus tipos de evento están disponibles, pero los productores de cancelación y reprogramación quedan en C-27 y C-35.
- **Gobernanza crítica:** la implementación requiere aprobación explícita de Juan Pablo.

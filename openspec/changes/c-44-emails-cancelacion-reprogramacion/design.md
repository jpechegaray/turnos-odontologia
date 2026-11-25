# Diseño

## Context

C-27 y C-35 publican eventos dentro de la transacción de agenda. C-15 implementa el envío idempotente y los reintentos. El recordatorio ya valida el estado y el inicio del turno al enviarse.

## Goals / Non-Goals

**Objetivos**

- Procesar los eventos confirmados y descartar los recordatorios obsoletos.
- Aplicar RN-31 y el flujo 3, que reserva el correo de cancelación para cancelaciones del personal.

**Fuera de alcance**

- Nuevos endpoints, cambios de esquema y enlaces con token en estos correos.

## Decisions

- Escuchar después del commit en una transacción nueva. El planificador bloquea primero el recordatorio y después el turno. Así el descarte ocurre cuando la operación de agenda ya liberó el bloqueo del turno.
- Incorporar el actor a `TurnoCancelado`. La cancelación por token también descarta el recordatorio.
- Descartar los recordatorios programados de una cancelación y el del inicio anterior de una reprogramación, siempre que ese inicio haya quedado obsoleto. Mantener el historial de envíos y de intentos.
- Si el turno vuelve a un inicio descartado por este cambio, el planificador puede reactivar la misma fila conservando los intentos. La clave de C-15 sigue evitando duplicados.
- Enviar en texto plano mediante C-15, usando tipo, horario y destinatario. Estos datos quedan disponibles para reintentar con la misma plantilla.

## Risks / Trade-offs

- El manejador posterior al commit se ejecuta en el proceso que confirma la operación. Si el proceso se interrumpe antes del registro del aviso, el planificador conserva la comprobación de vigencia del recordatorio, pero no puede reconstruir un aviso que todavía no fue registrado.

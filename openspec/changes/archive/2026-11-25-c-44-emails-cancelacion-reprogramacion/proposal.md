# Propuesta

## Why

El paciente necesita saber si el consultorio canceló o movió su turno. Una vez confirmado el cambio, un recordatorio asociado al horario anterior puede generar confusión.

## What Changes

- Enviar y registrar el correo de cancelación por el personal o de reprogramación cuando el paciente tenga email (RN-31, flujos 2 y 3).
- Descartar los recordatorios pendientes o reintentables que pierdan vigencia, incluso si el paciente no tiene email (RN-30).
- Reutilizar el registro, la plantilla y los reintentos de C-15.

## Capabilities

### New Capabilities

### Modified Capabilities

- `notificaciones-email` incorpora el envío por cambios de turno y el descarte del recordatorio obsoleto.

## Impact

Eventos de agenda y notificaciones del backend. C-15, C-27 y C-35 ya están en `master`.

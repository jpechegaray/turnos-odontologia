# Proposal

## Why

Los turnos próximos todavía no disparan recordatorios. Se necesita automatizar el aviso sin repetirlo cuando se ejecuta varias veces el planificador y sin avisar por un horario cancelado o reemplazado.

## What Changes

- Consultar cada cinco minutos los turnos elegibles en la ventana configurada. PA-05 confirmada: un recordatorio por horario, con 24 horas de anticipación por defecto.
- Registrar el pendiente por turno y horario de referencia, y usar el envío y la política de reintentos de C-15.
- Generar un enlace de gestión con C-32 para cada intento válido, también para turnos internos, conservando los enlaces anteriores.
- Descartar pendientes obsoletos antes de enviar, incluidos los reintentos.

## Capabilities

### New Capabilities
- `recordatorio-turno`: planificación, emisión del enlace y descarte de avisos.

### Modified Capabilities
- Ninguna.

## Impact

Agenda agrega un puerto interno de lectura. Notificaciones reutiliza la infraestructura de C-15 y distingue un recordatorio obsoleto de una notificación que no reconoce. El enlace usa el contrato de gestión por token que expondrán C-39 y C-49.

# Proposal

## Why

Recepción necesita completar la gestión de los turnos desde la agenda diaria y semanal. Los servicios de agenda ya permiten reprogramar, cancelar y registrar asistencia. C-46 integra esas operaciones en la pantalla del personal.

## What Changes

- Agregar un panel de gestión de los turnos visibles con las acciones que permiten el rol, el estado y la hora.
- Avisar antes de reprogramar o cancelar fuera del plazo vigente y mostrar la marca que devuelve el servidor.
- Enviar las operaciones a la API protegida y actualizar la agenda. Mostrar los rechazos de manera que el personal pueda corregir los datos o recargar el turno.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `agenda-frontend`: gestión de reprogramación, cancelación y asistencia desde ambas vistas.

## Impact

Frontend de agenda y pruebas. Se utilizan los contratos de C-27, C-29, C-35 y la consulta de configuración existente.

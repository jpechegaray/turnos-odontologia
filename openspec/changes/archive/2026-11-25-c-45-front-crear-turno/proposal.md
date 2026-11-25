# Proposal

## Why

Recepción todavía consulta la agenda y la disponibilidad por separado. C-45 reúne la búsqueda del paciente, los horarios sugeridos y el alta en una sola pantalla.

## What Changes

- Agregar el panel Nuevo turno a la agenda de administración y recepción.
- Sugerir horarios en hora de Buenos Aires con la duración de la prestación o la que el personal ajuste.
- Explicar conflictos por profesional o sillón y actualizar las alternativas en el mismo formulario.
- Refrescar la agenda al guardar y mostrar los rechazos del servidor con acciones concretas.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `agenda-frontend`: agrega el alta para administración y recepción, junto con la consulta de alternativas y la recuperación ante errores.

## Impact

Frontend de agenda. Reutiliza los contratos de pacientes, prestaciones por profesional, disponibilidad y alta de turno. Sin dependencias ni cambios de API.

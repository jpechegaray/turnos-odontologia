# Design

## Context

US-011, US-012 y US-014 comparten el turno y su estado. La SPA tiene vistas diaria y semanal y el alta de C-45. Los endpoints de escritura mantienen las validaciones y permisos efectivos.

## Goals / Non-Goals

- Gestionar un turno visible con opciones según rol, estado y hora.
- Avisar el plazo real y refrescar la agenda.
- La agenda leída de C-28 no expone el historial de marcas tardías. La interfaz muestra la marca al recibir la respuesta de la operación, sin almacenar turnos en el navegador.

## Decisions

- Un panel en la cabecera permite elegir un turno de la vista actual. Así se conservan las tarjetas compactas de la grilla y las acciones tienen el tamaño táctil establecido. El kit UI y los tokens aprobados se mantienen en modo system-only.
- Los turnos activos futuros admiten reprogramación y cancelación para recepción y administración. Desde el inicio admiten registrar asistencia. Una marca existente solo admite la corrección contraria por ADMIN. El odontólogo conserva la agenda de solo lectura.
- Reprogramar permite editar fecha, hora, duración, profesional o sillón conservando paciente y prestación. Fecha y hora se convierten desde Buenos Aires. El horario se escribe manualmente porque la consulta de disponibilidad incluye el turno original y podría ocultar un movimiento válido dentro de su propio intervalo. El backend valida la operación.
- Cancelar ofrece un motivo opcional de hasta 255 caracteres. Se consulta el plazo vigente al abrir la acción de cambio o cancelación. Con un error de configuración, se muestra que el plazo no pudo consultarse y se puede reintentar o continuar sabiendo que el servidor determinará la marca.
- RN-05 usa un límite inclusivo y el horario original. La respuesta del POST determina las marcas visibles en la confirmación. El POST usa el cliente con CSRF y bloquea duplicados mientras está pendiente.
- Un conflicto de horario conserva los datos para corregirlos. Si el turno cambió, se indica que la agenda debe actualizarse. Tras una operación exitosa se recarga y, si cambió el día, se muestra la nueva fecha en la vista vigente.

## Risks / Trade-offs

La interfaz usa la hora del navegador para anticipar qué opción ofrecer y si hay un aviso de demora. La API sigue siendo la autoridad para el momento y el plazo efectivos.

## Migration Plan

No aplica.

## Open Questions

Ninguna.

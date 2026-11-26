# Proposal

## Why

El backend ya tiene implementados el alta, el listado y la baja de la lista de espera (C-18), las sugerencias de entradas compatibles con un hueco (C-31) y la asignación manual (C-43). Lo que falta es una pantalla: hoy recepción no tiene cómo anotar a un paciente ni asignar un hueco desde la SPA. C-47 cierra US-017 y la parte de US-018 que corresponde a recepción.

## What Changes

- Nueva pantalla del personal en `/lista-espera`, con entrada en el menú, para `ADMIN` y `RECEPCION`.
- Alta de una entrada. Se busca al paciente y se cargan la prestación (obligatoria), el profesional preferido (opcional), el rango de fechas (con fecha de fin obligatoria) y la franja horaria (opcional, con las dos horas o ninguna). La pantalla valida los datos antes de enviarlos. También muestra los rechazos del servidor: entrada activa duplicada (409), paciente sin email o inactivo (422) y errores por campo (400).
- Listado filtrado por estado (`ACTIVA` por defecto) que muestra los nombres de paciente, prestación y profesional en lugar de los IDs, junto con el rango, la franja y el estado.
- Baja de una entrada activa con confirmación previa en la misma fila. Si la entrada ya no estaba activa (409), se avisa y el listado se recarga.
- Sugerencias para un hueco: se indican el profesional, la fecha y las horas de inicio y fin, y se listan las entradas compatibles. Cada sugerencia permite asignar el hueco con un solo pedido. Si la asignación responde 409, porque el horario quedó ocupado o la entrada ya no está activa, se avisa y las sugerencias se recargan.
- Tests de Vitest con los nombres exactos de los escenarios.

## Capabilities

### New Capabilities
- `lista-espera-frontend`: pantalla de recepción para anotar, listar y dar de baja entradas de la lista de espera, y para asignar a mano un hueco a una entrada compatible.

### Modified Capabilities
- (ninguna)

## Impact

- Frontend: código nuevo en `frontend/src/features/lista-espera/` (hoy la carpeta está vacía) con su `routes.ts`. Se reutilizan `SeleccionPaciente` y `mensajeDelTurno` de `features/agenda/crear`, los catálogos (`/profesionales`, `/prestaciones`, `/pacientes`) y el kit de UI.
- API: se consumen sin cambios `/api/lista-espera`, `/api/lista-espera/compatibles` y `/api/lista-espera/{id}/asignar`. No se toca el backend.
- Gobernanza LOW. El design system está en modo `system-only`, así que no hay gate de mockups.

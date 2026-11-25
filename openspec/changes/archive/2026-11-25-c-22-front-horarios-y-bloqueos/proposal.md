# Proposal

## Why

El back ya expone los horarios de atención (C-11) y los bloqueos de agenda (C-12) de cada profesional, pero hoy solo se pueden cargar con llamadas a la API. Recepción necesita definir los tramos semanales y bloquear vacaciones o congresos desde la SPA para que la agenda que se arma en C-19 tenga datos reales (US-007, US-008). El odontólogo tiene que poder consultarlos sin modificarlos (RN-33).

## What Changes

- Se agrega en `/profesionales` la acción de fila **«Horarios»**. Abre un panel del profesional con sus tramos agrupados por día de la semana (lunes a domingo) y permite crearlos, modificarlos y eliminarlos (`/api/profesionales/{id}/horarios`).
- Antes de enviar un alta o una modificación, la SPA valida la superposición con los demás tramos del mismo día (intervalos `[inicio, fin)`, RN-16). Igual informa el `409 HORARIO_SUPERPUESTO` del servidor.
- Se agrega la acción de fila **«Bloqueos»**. Abre un panel con los bloqueos del profesional ordenados por inicio y permite darlos de alta (fecha y hora de inicio y de fin en hora de Buenos Aires, más motivo) y eliminarlos con confirmación (`/api/profesionales/{id}/bloqueos`).
- `ADMIN` y `RECEPCION` ven los controles de escritura en ambos paneles. `ODONTOLOGO` ve los paneles de su propio profesional sin botones. Un `403` se muestra como permiso insuficiente sin cerrar la sesión.
- `PaginaProfesionales` (C-21) cambia su punto de extensión de un único `panel` a una lista `paneles`, para mostrar «Prestaciones», «Horarios» y «Bloqueos» en la misma fila. El cambio es interno a la SPA y no altera lo que ve el usuario en «Prestaciones».
- Se agregan tests de Vitest con el nombre exacto de cada escenario.

## Capabilities

### New Capabilities
- `horarios-frontend`: consulta y edición en la SPA de los tramos de atención semanales de un profesional, con validación de superposición y permisos por rol.
- `bloqueos-frontend`: consulta, alta y baja en la SPA de los bloqueos de agenda de un profesional, con fechas en hora de Buenos Aires y permisos por rol.

### Modified Capabilities
- Ninguna. Las specs de frontend de profesionales de C-21 (`profesionales-frontend`, `profesional-prestaciones-frontend`) todavía no están sincronizadas en `openspec/specs/`, porque C-21 sigue sin archivar. Este change no modifica sus requisitos: la acción «Prestaciones» se comporta igual.

## Impact

- **Frontend**: `frontend/src/features/profesionales/` suma las carpetas `horarios/` y `bloqueos/`. También se tocan `profesionales/PaginaProfesionales.tsx`, `profesionales/routes.ts` y un permiso nuevo en `permisos.ts` (escriben `ADMIN` y `RECEPCION`). Se ajusta el test de C-21 que usa la prop `panel`.
- **Backend y API**: sin cambios. Se consumen los contratos de las specs `horarios-de-atencion` y `bloqueos-de-agenda`.
- **Dependencias**: C-11, C-12 y C-16 (mergeados). Para el panel se reutiliza la pantalla de C-21, que ya está en `master`.
- **Fuera de alcance**: el chequeo de turnos activos superpuestos al crear un bloqueo (RN-17). Necesita turnos (C-19) y lo agrega el back en C-30. La SPA solo muestra el error que llegue.

# Proposal

## Why

C-25 ya expone `GET /api/notificaciones`: el registro paginado de emails, filtrable por estado y turno y con el último error de los fallidos. Pero administración y recepción todavía no lo pueden ver desde la aplicación (US-020, RN-30). Sin esa pantalla, un email que falla (un recordatorio de C-37, un aviso de hueco de C-36 o una cancelación) solo se detecta mirando la base. Sus dependencias, C-16 y C-25, ya están en `master`. Desbloquea C-50.

## What Changes

- **Pantalla nueva `/notificaciones`** en `features/notificaciones`, con entrada "Notificaciones" en el menú, solo para `ADMIN` y `RECEPCION`. El odontólogo no la ve en el menú; si entra por URL, la pantalla queda denegada como el resto de las internas.
- **Listado** del registro en el orden que devuelve la API (más reciente primero), con estas columnas:
  - tipo, en español (Recordatorio, Confirmación de reserva, Cancelación, Reprogramación, Aviso de hueco);
  - estado (Pendiente, Enviada, Fallida);
  - destinatario;
  - turno (`#id`, o "Aviso de hueco" si es de una oferta sin turno);
  - fecha y hora de referencia en Buenos Aires;
  - intentos;
  - fecha de envío;
  - último error, que se muestra solo en las fallidas.
- **Filtros** por estado (Todos, Pendiente, Enviada, Fallida) y por número de turno. Se combinan. Un número de turno que no es un entero positivo muestra el error en el campo y no consulta. Buscar vuelve a la primera página.
- **Paginación** de 20 por página, con "Anterior" y "Siguiente" y el texto "Página X de Y · N envíos". Es el primer listado paginado del front: consume el formato `Pagina` de C-25 (`contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`).
- **Estados de la pantalla:** cargando; vacío ("No hay envíos registrados" sin filtros, "No hay envíos que coincidan con los filtros" con filtros); error de carga con "Reintentar"; `403` con el mensaje de permiso.
- **Tests Vitest** con el nombre exacto de cada escenario.
- **Quedan fuera:** reenviar un email a mano, ver el cuerpo del email (no se guarda) y filtrar por paciente o por fecha. La API no lo ofrece.

## Capabilities

### New Capabilities

- `notificaciones-frontend`: pantalla del personal para consultar el registro de notificaciones: acceso por rol, listado, filtros, paginación y estados de carga y error.

### Modified Capabilities

Ninguna. `registro-notificaciones` (API de C-25) se consume sin cambios.

## Impact

- **Nuevo:** `frontend/src/features/notificaciones/` (`routes.ts`, página, filtros, hook de carga, tipos, armado de la consulta, textos y test). Se borra el `.gitkeep`.
- **Sin cambios** en el backend, en `shared/` ni en `app/`: la ruta se descubre sola por `import.meta.glob` (C-09).
- Estilos: solo tokens de `design-system/tokens.css` y clases del kit (regla dura 13). La política visual del proyecto es `system-only`, así que no lleva mockups.

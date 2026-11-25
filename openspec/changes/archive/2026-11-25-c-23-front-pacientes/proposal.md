# Proposal

## Why

La API de fichas de paciente (`/api/pacientes`, C-07) ya está en `master`, pero recepción todavía no puede usarla desde la SPA. US-009 pide que recepción cree y busque pacientes con sus datos de contacto, obra social y notas para asignarles turnos rápido. Este change es el paso previo a crear turnos desde la UI (C-19 y C-27) y lo pide la ficha `[C-23]` de `CHANGES.md` (Ola 3, S, governance LOW). Sus dependencias, C-07 y C-16, ya están mergeadas.

## What Changes

- Pantalla `/pacientes` en `frontend/src/features/pacientes/`, que se registra con su propio `routes.ts`, como C-20 y C-21. Entra al menú para `ADMIN` y `RECEPCION`.
- Búsqueda por apellido, nombre, DNI o email (US-009, RN-32) contra `GET /api/pacientes`, con los cuatro filtros que ya acepta el backend. Apellido y nombre buscan por coincidencia parcial; DNI y email, por coincidencia exacta. Los filtros se combinan entre sí y el resultado trae solo fichas activas.
- Formulario de alta (`POST /api/pacientes`) y de edición (`PUT /api/pacientes/{id}`) con los campos del DTO: nombre, apellido, DNI, email, teléfono, obra social, número de afiliado y notas. Antes de enviar valida lo mismo que exige RN-32: nombre y apellido obligatorios y al menos un medio de contacto.
- Baja lógica (`DELETE /api/pacientes/{id}`) con confirmación en la fila. Solo la ve `ADMIN`, porque el backend y la matriz de KB 03 reservan la baja a ese rol.
- Los errores del backend se muestran en el campo que corresponde: el DNI duplicado (`409`, `RN-32`) en el campo DNI, el contacto faltante (`422`, `RN-32`) en los campos email y teléfono, y los campos inválidos del `400` en su campo. Una ficha inexistente (`404`) o una falta de permiso (`403`) se muestran como alerta. Nunca se muestra el `detalle` del servidor.
- Tests Vitest + Testing Library con `fetch` simulado (`simularSesionActiva`). Cada `it(...)` lleva el nombre exacto de un escenario de la spec y se usan solo datos ficticios.

## Capabilities

### New Capabilities
- `pacientes-frontend`: cómo el personal busca, da de alta, edita y da de baja fichas de paciente desde la SPA, qué ve cada rol y cómo se muestran los errores de RN-32. El nombre sigue la convención de las capacidades de front que ya existen (`prestaciones-frontend`, `configuracion-frontend`, `sillones-frontend`).

### Modified Capabilities
- Ninguna. `gestion-pacientes` (backend) no cambia: este change solo consume su contrato.

## Impact

- **Código nuevo:** `frontend/src/features/pacientes/` (página, formulario, búsqueda, tipos, validación, errores, permisos, `routes.ts` y tests). Reemplaza el `.gitkeep` de la carpeta, que `estructura.test.ts` exige que exista.
- **Código existente:** no se modifican `shared/`, `app/` ni otras features. Se reutilizan `api` (`shared/api/clienteApi`), `ErrorApi` (`shared/api/errorApi`), el kit `shared/ui` (`Campo`, `Boton`, `Tabla`, `Tarjeta`, `Alerta`, `Toast`), `useSesion`, `RequireRole` y el registro de rutas por `import.meta.glob`.
- **API:** no hay endpoints nuevos ni cambios en el backend.
- **Dependencias:** no se agregan.
- **Desbloquea:** C-50 (identidad visual final).

## Fuera de alcance

- La lectura de fichas por el odontólogo, limitada a los pacientes de sus turnos (KB 03): queda para C-28. Hoy el backend responde `403` a `ODONTOLOGO` en todo `/api/pacientes`, así que la pantalla no se le ofrece.
- Reactivar fichas dadas de baja o listar inactivas: el backend no tiene endpoint para eso (la búsqueda excluye inactivas y un `PUT` sobre una ficha inactiva responde `422`).
- La paginación: el backend no la ofrece.
- La consulta por identificador (`GET /api/pacientes/{id}`): la edición parte de la fila del resultado de búsqueda, que ya trae todos los campos.
- El consentimiento de datos (`consentimiento_datos_en`): lo maneja la reserva pública (C-41) y la API de gestión no lo expone.
- La elección de paciente al crear un turno: es parte de C-27.

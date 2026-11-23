# Proposal

## Why

Los contratos de prestaciones (C-04) y configuración (C-08) ya están implementados y el personal puede autenticarse en la SPA (C-16), pero las carpetas `features/prestaciones` y `features/configuracion` solo contienen `.gitkeep`. Sin estas pantallas, `ADMIN` no puede administrar el catálogo ni los parámetros del consultorio desde la aplicación, y `RECEPCION` no puede consultarlos. C-20 cierra esa brecha de US-003 y US-006 sin tocar el backend.

## What Changes

- Incorporar la pantalla `/prestaciones` con listado, alta, edición y baja lógica. La duración se valida en el cliente con RN-09 (entero, múltiplo de 5, entre 5 y 480) antes de llamar a la API. Solo `ADMIN` ve los controles de escritura; `RECEPCION` y `ODONTOLOGO` consultan.
- Incorporar la pantalla `/configuracion` con el formulario de las siete claves, precargado desde `GET /api/configuracion` y validado por campo con RN-36 antes de enviar el reemplazo completo. `ADMIN` edita y guarda; `RECEPCION` consulta en solo lectura; `ODONTOLOGO` no accede.
- Mapear a alertas los errores de negocio ya definidos: `422 RN-09`, `422 RN-36`, `409 PRESTACION-DUPLICADA`, `404 PRESTACION-NO-ENCONTRADA` y fallos técnicos recuperables.
- Agregar tests Vitest de las validaciones y de los permisos por rol, usando `fetch` stubbeado y los helpers de sesión de C-16.
- No se modifican endpoints, DTOs, reglas ni esquema del backend. No se agregan dependencias ni tokens visuales.

## Capabilities

### New Capabilities

- `prestaciones-frontend`: comportamiento de la SPA para listar, dar de alta, editar y dar de baja lógicamente prestaciones, incluida la validación de duración y la visibilidad de controles por rol.
- `configuracion-frontend`: comportamiento de la SPA para consultar y reemplazar los siete parámetros operativos, incluida la validación RN-36 por campo y los permisos de edición.

### Modified Capabilities

Ninguna. `catalogo-prestaciones` y `configuracion-parametros` describen el contrato de la API y no cambian; estas capacidades nuevas describen la política de la SPA sobre esos contratos, igual que `sesion-frontend-personal` lo hace sobre `control-acceso`.

## Impact

- Frontend: `features/prestaciones` (pantalla, formulario, `routes.ts`, tests), `features/configuracion` (pantalla, formulario, `routes.ts`, tests) y, si hace falta, un módulo compartido de validación por feature. Reutiliza `clienteApi`, el kit UI, `useSesion`, `RequireRole` y los tokens de `design-system`.
- Backend: sin cambios. Se consumen `GET/POST/PUT/DELETE /api/prestaciones` y `GET/PUT /api/configuracion` tal como los entregaron C-04 y C-08.
- Gobernanza LOW: implementar con TDD y mergear si los tests pasan. Sin mockups por change (`design-system` en modo `system-only`).

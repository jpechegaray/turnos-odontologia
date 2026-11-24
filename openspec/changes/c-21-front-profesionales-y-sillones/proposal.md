# Proposal

## Why

Los contratos de sillones (C-05), profesionales (C-06) y prestaciones por profesional (C-10) ya están implementados, y el personal puede autenticarse en la SPA (C-16). Sin embargo, `features/profesionales` solo contiene `.gitkeep`. Sin estas pantallas, `ADMIN` no puede mantener desde la aplicación los profesionales, los sillones ni las prestaciones que realiza cada profesional. Tampoco `RECEPCION` y `ODONTOLOGO` pueden consultarlos. C-21 cubre esa brecha para US-004 y US-005 sin tocar el backend.

## What Changes

- Incorporar la pantalla `/profesionales` con listado, alta, edición, baja lógica y reactivación. La validación local exige nombre, apellido y matrícula no blancos y como máximo 255 caracteres, y especialidad opcional. Solo `ADMIN` ve los controles de escritura. `RECEPCION` consulta todos los profesionales y `ODONTOLOGO` ve solo el registro propio que devuelve la API. Si no tiene vínculo, la pantalla le muestra un aviso claro.
- Incorporar la pantalla `/sillones` con listado, alta, edición, baja lógica y reactivación. El nombre es obligatorio y la descripción, opcional. Solo `ADMIN` escribe; los tres roles consultan.
- Dentro de `/profesionales`, agregar un panel de prestaciones por profesional. Se consulta `GET /api/profesionales/{id}/prestaciones`. `ADMIN` elige el conjunto entre las prestaciones activas del catálogo y las ya asociadas, y lo guarda con `PUT /api/profesionales/{id}/prestaciones`. Los demás roles ven la lista sin controles.
- Mostrar como alertas los errores de negocio ya definidos, usando su código y nunca el `detalle`: `409 PROFESIONAL_MATRICULA_DUPLICADA`, `409 SILLON_NOMBRE_DUPLICADO`, `404 PROFESIONAL_NO_ENCONTRADO`, `404 SILLON_NO_ENCONTRADO`, `422 PRESTACION_INEXISTENTE`, `422 PRESTACION_INACTIVA`, `409 PROFESIONAL_PRESTACIONES_CONFLICTO`, `403` y los fallos técnicos recuperables.
- Agregar tests Vitest de formularios, asignación y permisos por rol. Usan `fetch` simulado y el fixture de sesión de C-16/C-20.
- No se modifican endpoints, DTOs, reglas ni esquema del backend. No se agregan dependencias ni tokens visuales.

**Fuera de alcance:**

- La guarda de RN-37, que impide desactivar con turnos futuros activos. Llega con C-30 y la SPA solo mostrará el error que devuelva el backend.
- La vinculación de un usuario `ODONTOLOGO` con un profesional, que es parte de la administración de usuarios (C-13).
- Los horarios y los bloqueos, que hace C-22.

## Capabilities

### New Capabilities

- `profesionales-frontend`: cómo se comporta la SPA al listar, dar de alta, editar, dar de baja y reactivar profesionales. Incluye la validación local y la visibilidad de los controles y de los datos según el rol, con el caso del odontólogo sin vínculo.
- `sillones-frontend`: cómo se comporta la SPA al listar, dar de alta, editar, dar de baja y reactivar sillones. Incluye la validación local y la visibilidad de los controles según el rol.
- `profesional-prestaciones-frontend`: cómo se comporta la SPA al consultar y reemplazar el conjunto de prestaciones de un profesional. Define qué prestaciones se ofrecen para asociar y cómo se manejan las inactivas ya asociadas.

### Modified Capabilities

Ninguna. `catalogo-profesionales`, `catalogo-sillones` y `profesional-prestaciones` describen el contrato de la API y no cambian. Las capacidades nuevas describen la política de la SPA sobre esos contratos, como hacen `prestaciones-frontend` y `configuracion-frontend` (C-20).

## Impact

- **Frontend:** se crea `features/profesionales/` con `routes.ts`, `profesionales/`, `sillones/` y `prestaciones/`, cada una con su pantalla o panel, formulario, validación, errores y tests. Se reutilizan `clienteApi`, el kit UI, `useSesion`, `RequireRole`, `simularSesionActiva` y los tokens de `design-system`.
- **Coordinación con C-22 (RM-09):** C-22 también trabaja en `features/profesionales`. El `routes.ts` de la feature agrupa las rutas de sus subcarpetas para que C-22 sume las suyas sin editar los archivos de C-21.
- **Backend:** sin cambios. Se consumen `GET/POST/PUT /api/profesionales`, `GET/POST/PUT /api/sillones`, `GET/PUT /api/profesionales/{id}/prestaciones` y `GET /api/prestaciones?soloActivas=false`.
- **Gobernanza LOW:** se implementa con TDD y se mergea si los tests pasan. No hay mockups por change porque el `design-system` está en modo `system-only`.

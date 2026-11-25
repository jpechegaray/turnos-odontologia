# Proposal

## Why

El contrato de `/api/usuarios` (C-13) ya está implementado y el personal puede autenticarse en la SPA (C-16), pero no hay ninguna pantalla para administrar cuentas. Hoy la única forma de crear un usuario `RECEPCION` u `ODONTOLOGO` es llamar a la API a mano, y por eso tampoco se pudo cerrar la prueba manual por rol de C-21 (tarea 13.3). C-24 cubre US-002 desde la UI, sin tocar el backend.

## What Changes

- Incorporar la pantalla `/usuarios`, que solo ve y abre `ADMIN`. Muestra el listado ordenado por email, con email, nombre, rol, profesional vinculado y estado.
- Agregar el alta con email, nombre, rol, contraseña inicial y, si el rol es `ODONTOLOGO`, el profesional vinculado. La validación local cubre email con formato válido y como máximo 254 caracteres, nombre obligatorio de hasta 255, contraseña de al menos 8 caracteres y como máximo 72 bytes en UTF-8, y profesional obligatorio solo para `ODONTOLOGO`. El selector de profesional ofrece los profesionales activos que no tienen otro usuario vinculado.
- Agregar la edición de email, nombre, rol y vínculo, sin campo de contraseña, porque la API no la cambia por esta vía. Al pasar a un rol distinto de `ODONTOLOGO` se descarta el vínculo.
- Agregar la desactivación con confirmación en la fila (`DELETE /api/usuarios/{id}`) y la reactivación (`PUT` con `activo: true`).
- Mostrar como alertas, por código y nunca con el `detalle`, los errores de negocio ya definidos: `409 USUARIO_EMAIL_DUPLICADO`, `422 USUARIO_VINCULO_INVALIDO`, `409 USUARIO_PROFESIONAL_YA_VINCULADO`, `409 USUARIO_ULTIMO_ADMIN`, `404 USUARIO_NO_ENCONTRADO`, el `400` de la contraseña (indicando el campo sin repetir el valor), `403` y los fallos técnicos recuperables.
- Agregar tests Vitest de formulario, permisos por rol y acceso por URL. Usan `fetch` simulado y el fixture de sesión de C-16.
- No se modifican endpoints, DTOs, reglas ni esquema del backend. No se agregan dependencias ni tokens visuales.

**Fuera de alcance:**

- Cambiar o restablecer la contraseña de un usuario existente: la API de C-13 no lo admite. Si hace falta, se desactiva el usuario y se crea otro, o se abre un change de backend aparte.
- Que el propio usuario cambie su contraseña o su perfil.
- Borrar usuarios físicamente: la API solo desactiva.

## Capabilities

### New Capabilities

- `usuarios-frontend`: cómo se comporta la SPA al listar, dar de alta, editar, desactivar y reactivar usuarios del personal. Incluye la validación local, el vínculo del odontólogo con su profesional y la restricción de la sección al rol `ADMIN`, tanto en el menú como al entrar por URL.

### Modified Capabilities

Ninguna. `administracion-usuarios` y `control-acceso` describen el contrato de la API y no cambian. La capacidad nueva describe la política de la SPA sobre ese contrato, como hacen `prestaciones-frontend` (C-20) y `profesionales-frontend` (C-21).

## Impact

- **Frontend:** se crea `features/auth/usuarios/` con `routes.ts`, pantalla, formulario, validación, errores, tipos, hook de carga y tests. `features/auth/routes.ts` pasa a sumar las rutas de `usuarios/` además de `/login`. Se reutilizan `clienteApi`, el kit UI, `useSesion`, `RequireRole`, `simularSesionActiva` y los tokens de `design-system`.
- **Backend:** sin cambios. Se consumen `GET/POST /api/usuarios`, `PUT/DELETE /api/usuarios/{id}` y `GET /api/profesionales`.
- **Desbloquea:** la prueba manual 13.3 de C-21, porque `ADMIN` podrá crear desde la UI las cuentas ficticias de `RECEPCION` y `ODONTOLOGO`. También alimenta C-50.
- **Gobernanza MEDIUM:** se implementa con TDD por pasos y se muestran las decisiones no obvias. No hay mockups por change porque el `design-system` está en modo `system-only`.

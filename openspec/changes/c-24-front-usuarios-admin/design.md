# Design

## Context

- El backend de C-13 expone `GET/POST /api/usuarios`, `GET/PUT/DELETE /api/usuarios/{id}`, solo para `ADMIN`. Devuelve `{ id, email, nombre, rol, profesionalId, activo }` y nunca credenciales. El `PUT` es un reemplazo completo sin contraseña (`email`, `nombre`, `rol`, `profesionalId`, `activo`). El `DELETE` desactiva y es idempotente. La contraseña se valida con `@ClaveValida` (8 caracteres como mínimo, 72 bytes en UTF-8 como máximo) y su error llega como `400` con el campo `password` en `errores`.
- La SPA descubre las features con `import.meta.glob("../../features/*/routes.ts")` en `app/rutas/modulos.ts`, que no se edita. El menú ya se filtra por `roles` (`AppRutas` usa `tieneRol`) y `RequireRole` muestra «No tenés permiso» cuando el rol no está permitido, sin montar la pantalla.
- `features/auth/routes.ts` hoy declara solo `/login`. El roadmap ubica la pantalla en `features/auth/usuarios`.
- C-20 y C-21 dejaron el patrón de pantallas CRUD: hook de carga con contador de solicitudes, formulario con `noValidate` y `validacion.ts`, `errores.ts` que traduce por `codigo`, `AccionesFila` con confirmación en la fila, `Toast` para avisos y tests que montan `AppRutas` con `simularSesionActiva`.
- `ErrorApi` ya expone `codigo`, `estado` y `campos` (los errores de campo del `400`).

## Goals / Non-Goals

**Goals:**

- Que `ADMIN` gestione los usuarios del personal desde la UI con las mismas reglas que valida la API, y que los demás roles no vean ni abran la sección.
- Reutilizar el patrón de C-20 y C-21 sin tocar `shared/`, `app/` ni el backend.
- Dejar al equipo en condiciones de crear cuentas ficticias de `RECEPCION` y `ODONTOLOGO` para las pruebas manuales por rol (C-21 13.3).

**Non-Goals:**

- Cambiar o restablecer contraseñas de usuarios existentes, porque la API no lo admite.
- Mostrar el estado de bloqueo por intentos fallidos, porque la API no lo expone a propósito.
- Paginación o búsqueda: un consultorio de 2 a 5 profesionales tiene pocas cuentas.

## Decisions

### D1. Rutas: `features/auth/routes.ts` agrupa las de `usuarios/`

`features/auth/usuarios/routes.ts` declara `/usuarios` con el menú «Usuarios» (orden 90, al final) y `roles: ["ADMIN"]`. `features/auth/routes.ts` conserva `/login` y concatena las rutas de `./usuarios/routes.ts`. El menú y el guard por URL salen gratis del registro existente.

- *Alternativa descartada:* crear `features/usuarios/` como feature de primer nivel. Contradice la ubicación que fija el roadmap y separa la gestión de cuentas del módulo `seguridad` del backend.
- *Alternativa descartada:* reutilizar `agruparRutas` de `features/profesionales`. Importaría una feature desde otra. Con un solo hijo basta un import directo, sin `import.meta.glob`.

### D2. El profesional vinculado se resuelve en el cliente

La pantalla carga en paralelo `GET /api/usuarios` y `GET /api/profesionales` (`ADMIN` recibe todos, activos e inactivos). Con eso:

- La columna «Profesional» muestra «Apellido, Nombre» buscando por `profesionalId`, o la marca de vacío.
- El selector ofrece los profesionales activos cuyo id no figura como `profesionalId` de otro usuario. En la edición suma el profesional actual del usuario, aunque esté inactivo, para no perder el valor precargado.

Si falla la carga de profesionales, se muestra el error recuperable de la pantalla completa, porque sin ellos no se puede vincular ni mostrar la columna.

- *Alternativa descartada:* un endpoint nuevo de profesionales libres. Exige tocar el backend, y el backend igual revalida (`422` o `409`).

### D3. Un formulario para alta y edición, con la contraseña solo en el alta

`FormularioUsuario` recibe el modo. En el alta muestra el campo «Contraseña inicial» (`type="password"`, `autoComplete="new-password"`). En la edición no hay campo de contraseña y el cuerpo del `PUT` no la incluye. El selector de profesional se renderiza solo con el rol `ODONTOLOGO`. Al cambiar el rol se limpia el profesional elegido.

Después de un alta exitosa o de un error, la contraseña se borra del estado del formulario. No se recorta ni se transforma: se envía tal como se escribió, igual que la valida el backend.

### D4. Validación local espejo de la API

`validacion.ts` replica `UsuarioAlta` y `@ClaveValida`:

- Email recortado, no vacío, con formato `algo@algo.algo` y de hasta 254 caracteres.
- Nombre recortado, no vacío y de hasta 255 caracteres.
- Contraseña de 8 *code points* como mínimo (`[...clave].length`) y de 72 bytes como máximo (`new TextEncoder().encode(clave).length`), igual que `ClaveUsuario`.
- Profesional obligatorio solo con `ODONTOLOGO`.

El backend sigue siendo la autoridad. Un `400` con `campos` se mapea al campo correspondiente (`password` → contraseña, `email` → email, `nombre` → nombre) con un mensaje propio, nunca con el `mensaje` del servidor ni con el valor ingresado.

### D5. Errores por código en `errores.ts`

| Respuesta | Mensaje | Efecto |
|---|---|---|
| `409 USUARIO_EMAIL_DUPLICADO` | Ya hay un usuario con ese email. | Conserva datos |
| `409 USUARIO_PROFESIONAL_YA_VINCULADO` | Ese profesional ya está vinculado a otro usuario. | Recarga usuarios y profesionales |
| `422 USUARIO_VINCULO_INVALIDO` | El profesional elegido no es válido para ese rol. | Conserva datos |
| `409 USUARIO_ULTIMO_ADMIN` | Tiene que quedar al menos un administrador activo. | Recarga la lista |
| `404 USUARIO_NO_ENCONTRADO` | El usuario ya no existe. | Recarga la lista |
| `403` | Mensaje de permiso de la feature | Conserva datos y sesión |
| Otros | Mensaje genérico | Conserva datos |

Los mensajes de permiso y genérico se definen en `features/auth/usuarios/mensajes.ts`, sin importar de `features/profesionales`.

### D6. Desactivar con `DELETE` y reactivar con `PUT`

«Desactivar» pide confirmación en la fila y emite `DELETE /api/usuarios/{id}`. «Reactivar» emite el `PUT` con los datos actuales y `activo: true`. Las dos acciones recargan la lista después de cada respuesta, también ante un error. La SPA no impide desactivarse a uno mismo: lo protege la regla del último administrador del backend, y desactivarse siendo uno de varios `ADMIN` es una acción válida.

## Risks / Trade-offs

- **Carrera al vincular:** dos administradores pueden elegir a la vez el mismo profesional libre. El backend responde `409 USUARIO_PROFESIONAL_YA_VINCULADO` y la SPA recarga profesionales (D5).
- **La contraseña en memoria del navegador:** vive solo en el estado del formulario hasta el envío. No se guarda en `localStorage`, no se loguea y no aparece en `Toast` ni en `Alerta`. Los tests usan contraseñas ficticias.
- **Sin restablecer contraseña:** si un integrante olvida la de su cuenta ficticia, hay que desactivarla y crear otra con otro email (el email inactivo sigue ocupado). Queda anotado como posible change de backend.
- **Validación de email local más laxa que `@Email` de Jakarta:** algún caso raro puede pasar el cliente y volver como `400`, que se mapea al campo (D4).

## UI Mockups

- Design system revision: 1
- Decision: no (project policy)
- Mockups: none
- Approval: not applicable
- Preview: not applicable
- Visual verify: pending (se compara contra los tokens y componentes aprobados en apply/verify)

## Migration Plan

No hay migración de datos, API ni dependencias. Para revertir, se elimina `features/auth/usuarios/` y se vuelve `features/auth/routes.ts` a declarar solo `/login`.

## Open Questions

Ninguna bloqueante. Restablecer contraseñas desde la UI queda fuera de alcance hasta que exista un endpoint para eso.

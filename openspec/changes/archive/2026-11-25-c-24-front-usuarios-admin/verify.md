# Verify — c-24-front-usuarios-admin (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 0 escenarios Parciales; 1 detalle menor a revisar por Juan Pablo en el PR)

> `openspec validate --strict` pasa y las **32 tareas** están marcadas `[x]`. Los **23 escenarios** de la delta spec
> tienen un `it(...)` con el nombre exacto y los 23 quedan **Cubiertos**. Re-verificación acotada: el escenario «No se puede
> desactivar al último administrador activo» era Parcial y ahora cubre `DELETE` y `PUT` (`it.each`). Vitest (219 tests), lint y build terminan en verde. El backend no cambió. No se
> encontraron violaciones de reglas duras. La prueba manual 8.4 es evidencia declarada por el usuario, no verificada
> por este informe.

Verificación independiente. Árbol evaluado: commit base `42222a8` (rama `lionel`) más cambios sin commitear; huella
recalculada `bc61b3007b632117` (coincide con la declarada; el informe anterior se hizo con `7f1e7b4c2a698ccf`, previa al cambio de `Usuarios.test.tsx`).

## Validación estructural

- `openspec validate c-24-front-usuarios-admin --strict`: **válido** (`Change 'c-24-front-usuarios-admin' is valid`).
- Existen `proposal.md`, `design.md`, `tasks.md` y `specs/usuarios-frontend/spec.md`.
- Delta spec `usuarios-frontend`: 8 requirements, 23 escenarios.
- Alcance: `git status --short backend` y `git diff --name-only HEAD -- backend` no devuelven nada; `git diff --name-only master -- backend shared frontend/src/app frontend/src/shared` tampoco. El backend, `shared/` y `app/` no se tocaron (tarea 8.5). Cambios del árbol: `frontend/src/features/auth/routes.ts` (modificado), `frontend/src/features/auth/usuarios/` (nuevo) y `openspec/changes/c-24-front-usuarios-admin/` (nuevo).

## Tareas

**32/32 completas** (1.1–1.3, 2.1–2.4, 3.1–3.4, 4.1–4.3, 5.1–5.6, 6.1–6.4, 7.1–7.3, 8.1–8.5). Sin pendientes.

- **8.4 (prueba manual contra el backend real):** marcada `[x]` con una nota. Es **evidencia declarada por el usuario**
  (2026-11-25): funciona con ADMIN, RECEPCION y ODONTOLOGO (alta, edición, desactivación, reactivación y acceso por
  rol). El aviso del último administrador no se pudo disparar a mano porque la base de desarrollo tiene dos ADMIN
  activos. Esta verificación **no la reprodujo** (no se arrancó servidor ni navegador); la nota de tasks.md no
  incluye credenciales ni cookies.
- 8.2, 8.3 y 8.5 se re-ejecutaron en esta verificación (ver "Reglas duras").
- El "Workflow follow-up" (prueba 13.3 de C-21, PR y archivo) son pasos posteriores, no tareas del change.

## Matriz escenario → test

Todos los tests están en `frontend/src/features/auth/usuarios/Usuarios.test.tsx`. Todos pasan en la corrida de abajo.

| Spec (requirement) | Escenario | Estado | Test (línea) | Nota |
|---|---|---|---|---|
| Sección solo ADMIN | La sección de usuarios aparece en el menú solo para el administrador | Cubierto | :78 (`it.each` por rol) | Afirma el enlace `/usuarios` para ADMIN y su ausencia para RECEPCION y ODONTOLOGO. |
| Sección solo ADMIN | Recepción o el odontólogo no acceden a usuarios por URL | Cubierto | :90 (`it.each`) | Afirma «No tenés permiso» y 0 consultas a `/api/usuarios`. |
| Listado | El listado muestra los usuarios con su rol, vínculo y estado | Cubierto | :99 | Celdas de email, nombre, rol, profesional o «—», estado; orden de la API; consulta ambos GET. |
| Listado | El listado vacío de usuarios muestra un mensaje | Cubierto | :121 | Mensaje y ninguna fila de datos. |
| Listado | Un fallo al listar usuarios permite reintentar | Cubierto | :128 | `500`, alerta, «Reintentar» y segunda consulta. |
| Validación | Un email inválido o un nombre en blanco bloquean el envío sin llamar a la API | Cubierto | :179 | Mensajes junto a los campos, `aria-invalid` y 0 escrituras. |
| Validación | Una contraseña fuera de largo bloquea el alta sin llamar a la API | Cubierto | :192 (`it.each`: 7 caracteres y 37×«ñ» = 74 bytes) | Mensaje junto al campo y 0 escrituras. Los bordes (8, 72, 73 bytes) están en `validacion.test.ts`. |
| Validación | Un odontólogo sin profesional bloquea el envío sin llamar a la API | Cubierto | :206 | Mensaje junto al selector y 0 escrituras. |
| Selector | El selector ofrece solo profesionales activos y libres | Cubierto | :223 | Opciones exactas: «Elegí un profesional» y «Ruiz, Luis». |
| Selector | Cambiar a un rol que no es odontólogo descarta el vínculo | Cubierto | :306 | El selector desaparece y el `POST` lleva `profesionalId: null`. |
| Alta | El administrador da de alta un usuario de recepción y la lista se refresca | Cubierto | :238 | Cuerpo exacto (email y nombre recortados, `profesionalId` nulo, contraseña), segundo GET, fila nueva, formulario y contraseña vacíos. |
| Alta | El administrador da de alta un odontólogo vinculado a su profesional | Cubierto | :288 | `POST` con `profesionalId: 11` y la fila muestra «Ruiz, Luis». |
| Errores | Un email duplicado se muestra como alerta sin perder los datos | Cubierto | :338 | Alerta, sin `detalle`, conserva email, nombre, rol y profesional. La contraseña queda vacía. |
| Errores | Un profesional ya vinculado se muestra como alerta | Cubierto | :363 | Alerta y un `GET /api/profesionales` adicional. |
| Errores | Un vínculo inválido se muestra como alerta | Cubierto | :380 | Alerta, sin `detalle`. |
| Errores | Una contraseña rechazada por la API marca el campo | Cubierto | :393 | Mensaje junto al campo; el valor no aparece en el DOM. |
| Errores | Un 403 al escribir un usuario se muestra como permiso insuficiente | Cubierto | :411 | Alerta, datos conservados y una sola llamada a `/api/auth/me` (la sesión sigue). |
| Modificación | El administrador cambia el rol de un usuario | Cubierto | :428 | `PUT` exacto (sin `password`, `activo: true`, `profesionalId: null`); la lista muestra «Administrador»; no hay campo de contraseña. |
| Modificación | Editar un usuario inexistente muestra una alerta y refresca la lista | Cubierto | :457 | Alerta «El usuario ya no existe.» y recarga. |
| Desactivación | El administrador desactiva un usuario y sigue visible como inactivo | Cubierto | :496 | Confirmación en la fila, un `DELETE`, recarga y la fila queda «Inactivo». |
| Desactivación | Cancelar la confirmación de desactivación no emite escrituras | Cubierto | :513 | 0 escrituras y el usuario sigue activo. |
| Desactivación | El administrador reactiva un usuario desactivado | Cubierto | :526 | `PUT` exacto con `activo: true` y la lista lo muestra activo. |
| Desactivación | No se puede desactivar al último administrador activo | Cubierto | :540 (`it.each`: `DELETE` y `PUT`) | `409 USUARIO_ULTIMO_ADMIN` al confirmar la desactivación y al guardar el cambio de rol a `RECEPCION` desde la edición. Afirma el cuerpo del `PUT` (`rol: RECEPCION`, `activo: true`), la alerta «Tiene que quedar al menos un administrador activo.» sin `detalle`, la recarga (+1 consulta) y que la fila siga «Activo» y «Administrador». |

Complementarios (no corresponden a escenarios): `validacion.test.ts` (8 tests: bordes de 8 caracteres, 72 y 73 bytes, code points, profesional solo para el odontólogo, contraseña ausente en edición, email de más de 254) y `opciones.test.ts` (3 tests: alta, profesional de un usuario inactivo cuenta como ocupado, edición incluye el profesional actual aun inactivo).

## Ejecución de tests

Todos los comandos corrieron en `frontend/`.

- `npx vitest run` → **exit 0**. `Test Files 28 passed (28)`, `Tests 219 passed (219)`, sin fallidos ni saltados (re-ejecutado en esta re-verificación con el mismo resultado). `Usuarios.test.tsx`: 28 tests; `opciones.test.ts`: 3; `validacion.test.ts`: 8 (la salida de cierre, recortada, muestra `✓ src/features/auth/usuarios/Usuarios.test.tsx (28 tests)` y `✓ ... opciones.test.ts (3 tests)`). Las únicas advertencias son los *Future Flag Warning* de React Router, ya presentes en el resto del front.
- `npm run lint` (`eslint .`) → **exit 0**, sin salida de errores.
- `npm run build` → **exit 0**: `✓ 115 modules transformed.` … `dist/assets/index-NZyCkyfi.js 210.85 kB │ gzip: 66.39 kB` … `✓ built in 1.23s`.
- **Prueba de mutación (corroborada):** se copió `errores.ts` al scratchpad, se quitó `"USUARIO_ULTIMO_ADMIN"` de `CODIGOS_CON_RECARGA` y se corrió `Usuarios.test.tsx -t "último administrador"`: **1 falló** (el caso `PUT`) y 1 pasó (el caso `DELETE`, que recarga por otro camino). Archivo restaurado: `sha256sum -c` en OK y huella del árbol de nuevo `bc61b3007b632117`. El caso `PUT` detecta la regresión.
- Backend: **no se ejecutó Maven**, como se indicó. No hay cambios en `backend/` (ver Validación estructural), así que su estado no depende de este change.

## Reglas de negocio y reglas duras

- **RN-33 / RN-34 (US-002):** hay escenario y test para alta, edición, rol, vínculo, desactivación y reactivación. La validación de contraseña (`validacion.ts:51`) cuenta *code points* (`[...password].length >= 8`) y bytes UTF-8 (`TextEncoder` ≤ 72): coincide con `ClaveUsuario.esValida` (`codePointCount >= 8` y `getBytes(UTF_8).length <= 72`). La contraseña no se recorta (se envía tal como se ingresó), igual que en el contrato.
- **Contraseña:** `git grep -i password` en el código fuente (sin tests) muestra uso solo en `FormularioUsuario` (estado y campo `type="password"`, `autoComplete="new-password"`), el tipo `UsuarioAlta`, el armado del `POST` en `PaginaUsuarios.tsx:39`, `validacion.ts` y los mensajes de error fijos de `errores.ts`. No aparece en `Toast` («Usuario agregado.»), `Alerta`, tabla ni logs (`console.` no se usa en la feature). El `PUT` se arma con `cuerpoModificacion`, que no incluye la clave `password` (confirmado por el test de :428 y el de reactivación). El formulario borra la contraseña en el `finally` de cada envío. El `400` de contraseña muestra un mensaje fijo, no el del servidor.
- **`detalle` del servidor:** `errores.ts` traduce por código y por estado; ningún mensaje usa `detalle` ni `errores[].mensaje`. Los tests de alerta afirman `not.toHaveTextContent("detalle interno del servidor")`.
- **Regla dura 1 (datos reales):** emails `*.test` y contraseñas ficticias. Sin violación.
- **Regla dura 2 (secretos):** no hay secretos ni `.env` en el diff.
- **Regla dura 4 (odontólogo solo lectura):** la ruta lleva `roles: ["ADMIN"]` y `RequireRole` bloquea por URL; los tests :78 y :90 lo prueban. La autorización efectiva sigue en la API.
- **Regla dura 11 (nombre exacto del escenario en `it`):** cumplida para los 23 escenarios (los tres `it.each` repiten el nombre exacto del escenario).
- **Regla dura 12 (TypeScript strict, sin `any`, PascalCase):** `grep` de `any` sin resultados en la feature; lint y build pasan; componentes en PascalCase.
- **Regla dura 13 (tokens, nada fijo):** `git grep -nE "#[0-9a-fA-F]{3,8}\b|[0-9]+px|rgb\(|font-family"` sobre `usuarios/` no devuelve coincidencias. Los estilos vienen del kit `shared/ui`.
- **Reglas duras 5–10 (dominio y backend):** no aplican, no hay cambios de backend ni de esquema.
- **Reglas 14–17 (git):** no se hizo commit ni push; el change sigue sin commitear. Un PR por change a `master`, que revisa Juan Pablo.
- **Buenas prácticas del registro de skills:** `useUsuarios` usa `Promise.all` para los dos GET independientes; imports directos, sin barrels propios.

## Diferencias con design.md

- **Bloqueantes:** ninguna.
- **Mayores:** ninguna. D1 (rutas agrupadas en `features/auth/routes.ts`), D2 (`opcionesProfesional` en el cliente), D3 (un formulario con la contraseña solo en el alta), D4 (validación espejo), D5 (tabla de errores y recargas: profesional ya vinculado, último admin y no encontrado) y D6 (`DELETE` para desactivar y `PUT` para reactivar, recarga también ante un error) coinciden con el código.
- **Menores:**
  1. ~~Parcial del último administrador~~: **resuelto** en esta re-verificación (el `it.each` cubre `DELETE` y `PUT`).
  2. **Tras un `404 USUARIO_NO_ENCONTRADO` al editar, el formulario sigue en modo edición** (`PaginaUsuarios.tsx:45-48`, `actualizarEditando(undefined)` solo se ejecuta si el `PUT` tiene éxito) aunque el usuario ya no exista. La alerta y la recarga ocurren como pide el spec, y quien edita puede pulsar «Cancelar». Es de severidad baja y la spec no exige cerrar la edición. Se deja a criterio de Juan Pablo.
  3. `design.md` no registra la decisión de usar `role="status"` del `Toast` para el aviso de éxito; no contradice nada.

## Qué NO se pudo verificar

- **Prueba manual contra el backend real (8.4):** solo hay la declaración del usuario; no se arrancó servidor ni navegador. No se disparó a mano el aviso del último administrador.
- **Backend:** no se corrió `mvn verify`; solo se confirmó que no hay cambios en `backend/`.
- **Contrato C-13 de punta a punta:** la SPA se probó con `fetch` simulado. Los nombres de campos del `400` (`errores[].campo`) y de los códigos de negocio se compararon con el spec y el design, y la ruta de contrato real solo la cubre la prueba manual declarada.
- **Visual:** la política es `system-only` (sin mockups); se comprobó el uso de tokens por búsqueda de texto y no se hizo captura visual.

## Próximo paso

1. Abrir el PR a `master` desde la rama `lionel` (lo revisa Juan Pablo). Pedir el commit por etapa o por change, como indica la regla 16.
2. En el PR, anotar el detalle menor del cierre de la edición tras un `404` para que se decida; no bloquea.
3. Con las cuentas de la tarea 8.4, completar la prueba manual 13.3 de C-21 y archivar C-21.
4. Con el PR mergeado, `/opsx:archive c-24-front-usuarios-admin`.

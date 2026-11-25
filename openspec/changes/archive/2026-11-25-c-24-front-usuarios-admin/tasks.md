# Tasks

> Gobernanza MEDIUM: al cerrar cada grupo, mostrar al usuario las decisiones no obvias antes de seguir. Los nombres de los tests (`it(...)`) son los nombres exactos de los escenarios de `specs/usuarios-frontend/spec.md`. Todas las contraseñas de los tests son ficticias.

## 1. Rutas y acceso solo para ADMIN

- [x] 1.1 RED: crear `frontend/src/features/auth/usuarios/Usuarios.test.tsx`, que monta `AppRutas` con el registro real y usa `simularSesionActiva(rol, responder)` con la cookie `XSRF-TOKEN`. Agregar los tests «La sección de usuarios aparece en el menú solo para el administrador» (un `it.each` por rol) y «Recepción o el odontólogo no acceden a usuarios por URL» (sin peticiones a `/api/usuarios`). Verificar que fallan.
- [x] 1.2 GREEN: crear `features/auth/usuarios/routes.ts` (`/usuarios`, menú «Usuarios» orden 90, `roles: ["ADMIN"]`) con una `PaginaUsuarios` mínima, y hacer que `features/auth/routes.ts` concatene `/login` con esas rutas. Verificar que pasan los tests de 1.1, `Login.test.tsx` y `estructura.test.ts`.
- [x] 1.3 Crear `usuarios/tipos.ts` (`UsuarioRespuesta`, `UsuarioAlta`, `UsuarioModificacion`, `Rol` reutilizado de `app/rutas/tipos`) y `usuarios/mensajes.ts` (permiso y genérico), sin importar de otras features. Verificar con `npm run lint` en `frontend/`.

## 2. Listado y estados de carga

- [x] 2.1 RED: agregar el test «El listado muestra los usuarios con su rol, vínculo y estado» y verificar que falla.
- [x] 2.2 GREEN: implementar `useUsuarios`, que carga en paralelo `GET /api/usuarios` y `GET /api/profesionales` con contador de solicitudes, y la `Tabla` con «Email», «Nombre», «Rol», «Profesional» («Apellido, Nombre» o marca de vacío) y «Estado», en el orden de la API. Verificar que pasa el test de 2.1.
- [x] 2.3 TRIANGULATE: agregar los tests «El listado vacío de usuarios muestra un mensaje» y «Un fallo al listar usuarios permite reintentar» (`500`, `Alerta tipo="peligro"` y «Reintentar» que vuelve a consultar los dos recursos). Verificar los tres tests del listado.
- [x] 2.4 REFACTOR: dejar los estados de carga, error, vacío y datos excluyentes y verificar `npm test` y `npm run lint`.

## 3. Validación local

- [x] 3.1 RED: agregar el test «Un email inválido o un nombre en blanco bloquean el envío sin llamar a la API» y verificar que falla.
- [x] 3.2 GREEN: crear `FormularioUsuario` en modo alta con `Campo` para email, nombre, rol (`<select>`), contraseña inicial (`type="password"`, `autoComplete="new-password"`) y `noValidate`, y `usuarios/validacion.ts` con las reglas de email y nombre (D4). Verificar que pasa el test de 3.1.
- [x] 3.3 TRIANGULATE: agregar los tests «Una contraseña fuera de largo bloquea el alta sin llamar a la API» (7 caracteres y 73 bytes, por ejemplo con caracteres de 2 bytes) y «Un odontólogo sin profesional bloquea el envío sin llamar a la API». Implementar la regla de *code points* y bytes con `TextEncoder`. Verificar que ninguno emite escrituras.
- [x] 3.4 Agregar un test unitario `validacion.test.ts` de los bordes de la contraseña (8 caracteres, 72 bytes, 73 bytes) con nombres descriptivos. Verificar con `npm test`.

## 4. Selector de profesional

- [x] 4.1 RED: agregar el test «El selector ofrece solo profesionales activos y libres» y verificar que falla.
- [x] 4.2 GREEN: mostrar el selector solo con rol `ODONTOLOGO` y calcular las opciones con una función pura `opcionesProfesional(profesionales, usuarios, usuarioEditadoId?)` (D2). Verificar que pasa el test de 4.1.
- [x] 4.3 TRIANGULATE: agregar el test «Cambiar a un rol que no es odontólogo descarta el vínculo» y un test unitario de `opcionesProfesional` que incluya el profesional actual (aun inactivo) en la edición. Verificar los dos.

## 5. Alta y errores de negocio

- [x] 5.1 RED: agregar el test «El administrador da de alta un usuario de recepción y la lista se refresca». Espera el `POST /api/usuarios` con email recortado, `profesionalId: null` y la contraseña, un `GET` posterior, la fila nueva y el formulario vacío (también la contraseña). Verificar que falla.
- [x] 5.2 GREEN: conectar el envío válido al `POST`, recargar, limpiar el formulario y anunciar el alta con `Toast` sin mencionar la contraseña. Verificar que pasa el test de 5.1.
- [x] 5.3 TRIANGULATE: agregar el test «El administrador da de alta un odontólogo vinculado a su profesional». Verificar que pasa.
- [x] 5.4 RED: agregar el test «Un email duplicado se muestra como alerta sin perder los datos» y verificar que falla.
- [x] 5.5 GREEN: crear `usuarios/errores.ts` con la tabla de D5, que traduce por código y nunca muestra `detalle`. Conservar los datos y borrar solo la contraseña. Verificar que pasa el test de 5.4.
- [x] 5.6 TRIANGULATE: agregar los tests «Un profesional ya vinculado se muestra como alerta» (recarga de profesionales), «Un vínculo inválido se muestra como alerta», «Una contraseña rechazada por la API marca el campo» (`400` con `campos` de `password`, sin repetir el valor en el DOM) y «Un 403 al escribir un usuario se muestra como permiso insuficiente» (la sesión sigue activa). Verificar los cuatro y después `npm test` y `npm run lint`.

## 6. Edición

- [x] 6.1 RED: agregar el test «El administrador cambia el rol de un usuario», que espera el `PUT /api/usuarios/{id}` con rol `ADMIN`, `profesionalId: null`, `activo: true` y sin la clave `password`. Verificar que falla.
- [x] 6.2 GREEN: agregar la acción «Editar» por fila, que precarga `FormularioUsuario` en modo edición (sin campo de contraseña) dentro de una `Tarjeta` con «Guardar cambios» y «Cancelar», y envía el `PUT` antes de recargar. Verificar que pasa el test de 6.1.
- [x] 6.3 TRIANGULATE: agregar el test «Editar un usuario inexistente muestra una alerta y refresca la lista» (`404 USUARIO_NO_ENCONTRADO`). Verificar que pasa.
- [x] 6.4 REFACTOR: unificar alta y edición con `key` por id y extraer la construcción del cuerpo del `PUT` a partir de la representación actual. Verificar `npm test` y `npm run lint`.

## 7. Desactivación y reactivación

- [x] 7.1 RED: agregar el test «El administrador desactiva un usuario y sigue visible como inactivo» (confirmación en la fila, `DELETE /api/usuarios/{id}` y `GET` posterior). Verificar que falla.
- [x] 7.2 GREEN: crear `usuarios/AccionesFila.tsx` (Editar, Desactivar o Reactivar según `activo`, con confirmación en la fila) y conectar la desactivación con recarga después de cada respuesta. Verificar que pasa el test de 7.1.
- [x] 7.3 TRIANGULATE: agregar los tests «Cancelar la confirmación de desactivación no emite escrituras», «El administrador reactiva un usuario desactivado» (`PUT` con `activo: true`) y «No se puede desactivar al último administrador activo» (`409 USUARIO_ULTIMO_ADMIN`, alerta y recarga). Verificar los tres.

## 8. Integración y verificación

- [x] 8.1 Ejecutar `npm test`, `npm run lint` y `npm run build` en `frontend/` y verificar que todo termina sin errores.
- [x] 8.2 Revisar con `git grep -nE "#[0-9a-fA-F]{3,8}\b|[0-9]+px|rgb\(|font-family" -- frontend/src/features/auth/usuarios` que no haya colores, espaciados ni fuentes fijos. Si aparece una coincidencia, reemplazarla por el token de `tokens.css`.
- [x] 8.3 Revisar con `git grep -n -i "password" -- frontend/src/features/auth/usuarios` que la contraseña solo aparezca en el formulario de alta, el cuerpo del `POST`, la validación y los tests, nunca en `Toast`, `Alerta`, logs ni en la tabla.
- [x] 8.4 Probar en desarrollo contra el backend real de C-13 con datos ficticios: `ADMIN` crea un usuario `RECEPCION` y un `ODONTOLOGO` vinculado a un profesional, edita un rol, desactiva y reactiva un usuario y comprueba el aviso del último administrador. Después inicia sesión con las cuentas nuevas y comprueba que no ven «Usuarios» ni pueden abrir `/usuarios`. Registrar la evidencia sin credenciales ni cookies.
  - Hecho (2026-11-25): el usuario probó en desarrollo contra el backend real y confirmó que todo funciona como ADMIN, RECEPCION y ODONTOLOGO (alta, edición, desactivación, reactivación y acceso por rol). El aviso del último administrador no se pudo disparar a mano porque la base de desarrollo tiene dos ADMIN activos; lo cubren los tests de Vitest. Evidencia registrada sin credenciales ni cookies.
- [x] 8.5 Confirmar con `git diff --name-only master` que no se modificó ningún archivo de `backend/`, `shared/` ni `app/`. Si aparece un hueco real de API, detenerse y reportarlo en lugar de tocar el backend.

## Workflow follow-up

- Con las cuentas de 8.4, completar la prueba manual 13.3 de C-21 y archivar C-21.
- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Abrir el PR a `master` (lo revisa Juan Pablo) y archivar el change con `openspec-archive-change` cuando esté mergeado.

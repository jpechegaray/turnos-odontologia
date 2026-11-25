# Tasks

## 1. Estructura de la feature y agregador de rutas

- [x] 1.1 RED: crear `frontend/src/features/profesionales/routes.test.ts` con los tests «El agregador expone las rutas de profesionales y sillones sin duplicados» y «El agregador falla si un routes.ts hijo no exporta rutas» (este último prueba la función de agregación con un módulo simulado); verificar que fallan.
- [x] 1.2 GREEN: crear `frontend/src/features/profesionales/routes.ts`, que concatena, ordenadas por archivo, las `rutas` que encuentra `import.meta.glob<ModuloRutas>("./*/routes.ts", { eager: true })` y lanza un error si un hijo no exporta un arreglo. Crear `profesionales/routes.ts` (`/profesionales`, menú «Profesionales» orden 30) y `sillones/routes.ts` (`/sillones`, menú «Sillones» orden 40), las dos con los tres roles y una pantalla mínima. Eliminar `features/profesionales/.gitkeep`. Verificar que pasan los tests de 1.1 y `estructura.test.ts`.
- [x] 1.3 Crear `features/profesionales/permisos.ts` con `useEsAdministrador()`, sin importar nada de `features/prestaciones`. Crear también el `AccionesFila` de la feature (Editar, Dar de baja o Reactivar según `activo`, y la confirmación en la fila). Verificar con `npm test` y `npm run lint` en `frontend/`.

## 2. Profesionales: listado y estados de carga

- [x] 2.1 RED: crear `frontend/src/features/profesionales/profesionales/Profesionales.test.tsx`, que monta `AppRutas` con el registro del `routes.ts` real y usa `simularSesionActiva(rol, responder)` con la cookie `XSRF-TOKEN`. Agregar el test «El listado muestra los profesionales con su matrícula y estado» y verificar que falla.
- [x] 2.2 GREEN: implementar `PaginaProfesionales` y su hook de carga con `GET /api/profesionales` en el orden de la API, contador de solicitudes y `Tabla` con «Apellido y nombre», «Matrícula», «Especialidad» (marca de vacío si falta) y «Estado». Verificar que pasa el test de 2.1.
- [x] 2.3 TRIANGULATE: agregar los tests «El listado vacío de profesionales muestra un mensaje» y «Un fallo al listar profesionales permite reintentar» (`500` con `Alerta tipo="peligro"` y botón «Reintentar» que vuelve a consultar). Verificar los tres tests del listado.
- [x] 2.4 REFACTOR: limpiar la duplicación entre los estados de carga, error y vacío, y verificar `npm test` y `npm run lint`.

## 3. Profesionales: validación local

- [x] 3.1 RED: agregar el test «Una matrícula en blanco bloquea el envío sin llamar a la API» y verificar que falla.
- [x] 3.2 GREEN: crear `FormularioProfesional` con `Campo` para nombre, apellido, matrícula y especialidad, `noValidate` y la validación de obligatorios al enviar, limpiando el error al editar el campo. Verificar que pasa el test de 3.1.
- [x] 3.3 TRIANGULATE: agregar los tests «Un nombre o apellido en blanco bloquea el envío sin llamar a la API» y «Un texto de más de 255 caracteres bloquea el envío sin llamar a la API» (especialidad de 256). Verificar que ninguno emite escrituras.
- [x] 3.4 REFACTOR: extraer `validarProfesional` y los mensajes a `profesionales/validacion.ts` sin cambiar el comportamiento, y verificar que los tests de validación siguen en verde.

## 4. Profesionales: alta y modificación

- [x] 4.1 RED: agregar el test «El administrador da de alta un profesional y la lista se refresca». Espera `POST /api/profesionales` con los textos recortados y `especialidad: null`, un `GET` posterior, la fila nueva y el formulario vacío. Verificar que falla.
- [x] 4.2 GREEN: conectar el envío válido al `POST`, recargar la lista, limpiar el formulario y anunciar el alta con `Toast`. Verificar que pasa el test de 4.1.
- [x] 4.3 TRIANGULATE: agregar el test «Una matrícula duplicada se muestra como alerta sin perder los datos» (`409 PROFESIONAL_MATRICULA_DUPLICADA`) y crear `profesionales/errores.ts`, que traduce por código y nunca muestra `detalle`. Verificar que se conservan los datos ingresados.
- [x] 4.4 RED: agregar el test «El administrador edita un profesional y la lista se refresca», que espera `PUT /api/profesionales/{id}` con los datos nuevos y el `activo` actual, y verificar que falla.
- [x] 4.5 GREEN: implementar la edición que precarga el formulario en una `Tarjeta` (con «Guardar cambios» y «Cancelar») y envía el `PUT` antes de recargar. Verificar que pasa el test de 4.4.
- [x] 4.6 TRIANGULATE: agregar los tests «Editar con una matrícula de otro profesional muestra una alerta» (`409`, se conserva lo ingresado) y «Editar un profesional inexistente muestra una alerta y refresca la lista» (`404 PROFESIONAL_NO_ENCONTRADO`). Verificar los dos.
- [x] 4.7 REFACTOR: unificar el alta y la edición en un solo formulario con `key` por id sin romper los tests de 4.x, y verificar `npm test` y `npm run lint`.

## 5. Profesionales: baja lógica y reactivación

- [x] 5.1 RED: agregar el test «El administrador da de baja un profesional y sigue visible como inactivo», que confirma la baja y espera `PUT` con `activo: false`, los datos actuales y un `GET` posterior. Verificar que falla.
- [x] 5.2 GREEN: conectar `AccionesFila` a la baja con confirmación en la fila y recargar la lista después de cada respuesta. Verificar que pasa el test de 5.1.
- [x] 5.3 TRIANGULATE: agregar los tests «Cancelar la confirmación de baja no emite escrituras», «El administrador reactiva un profesional dado de baja» (`PUT` con `activo: true`) y «Un error al dar de baja un profesional se muestra como alerta» (alerta sin `detalle` y recarga). Verificar los tres.
- [x] 5.4 REFACTOR: extraer a una función la construcción del cuerpo del `PUT` a partir de la representación actual, y verificar `npm test`.

## 6. Profesionales: permisos, menú y vista del odontólogo

- [x] 6.1 RED: agregar el test «El administrador ve los controles de escritura de profesionales» con rol `ADMIN` y verificar que falla si los controles no dependen del rol.
- [x] 6.2 GREEN: renderizar el formulario y la columna de acciones solo cuando `useEsAdministrador()` es verdadero. Verificar que pasa el test de 6.1.
- [x] 6.3 TRIANGULATE: agregar los tests «Recepción consulta los profesionales sin controles de escritura», «La pantalla de profesionales aparece en el menú para los tres roles» y «Un 403 al escribir un profesional se muestra como permiso insuficiente» (alerta de permiso, datos conservados y sesión activa). Verificar que no se emiten escrituras en el rol de consulta.
- [x] 6.4 RED: agregar los tests «El odontólogo vinculado ve solo su registro sin controles de escritura» y «El odontólogo sin vínculo ve un aviso de falta de vínculo» y verificar que el segundo falla.
- [x] 6.5 GREEN: hacer que el hook de carga marque el estado `sinVinculo` ante un `403` del `GET` y mostrar `Alerta tipo="info"` sin tabla ni «Reintentar». Verificar los dos tests de 6.4 y que la sesión sigue activa.
- [x] 6.6 REFACTOR: revisar que los estados de la pantalla (carga, error, vacío, sin vínculo y datos) sean excluyentes, y verificar `npm test` y `npm run lint`.

## 7. Sillones: listado y validación

- [x] 7.1 RED: crear `frontend/src/features/profesionales/sillones/Sillones.test.tsx` con el mismo montaje que en 2.1 y el test «El listado muestra los sillones con su descripción y estado», que exige el orden por nombre. Verificar que falla.
- [x] 7.2 GREEN: implementar `PaginaSillones` y su hook con `GET /api/sillones`, el orden por nombre en el cliente (`localeCompare(..., "es")`), el contador de solicitudes y `Tabla` con «Nombre», «Descripción» (marca de vacío) y «Estado». Verificar que pasa el test de 7.1.
- [x] 7.3 TRIANGULATE: agregar los tests «El listado vacío de sillones muestra un mensaje» y «Un fallo al listar sillones permite reintentar» y verificar los tres tests del listado.
- [x] 7.4 RED: agregar el test «Un nombre de sillón en blanco bloquea el envío sin llamar a la API» y verificar que falla.
- [x] 7.5 GREEN: crear `FormularioSillon` con `noValidate` y `sillones/validacion.ts` (nombre obligatorio, descripción opcional). Verificar que pasa el test de 7.4 y que no se emiten escrituras.

## 8. Sillones: alta, modificación, baja y reactivación

- [x] 8.1 RED: agregar el test «El administrador da de alta un sillón y la lista se refresca», que espera un `POST /api/sillones` con nombre "Box 3" y `descripcion: null` y un `GET` posterior. Verificar que falla.
- [x] 8.2 GREEN: conectar el alta con el recorte, la recarga, la limpieza del formulario y el `Toast`. Verificar que pasa el test de 8.1.
- [x] 8.3 TRIANGULATE: agregar el test «Un nombre de sillón duplicado se muestra como alerta sin perder los datos» (`409 SILLON_NOMBRE_DUPLICADO`) y crear `sillones/errores.ts`, que traduce por código. Verificar que se conservan los datos.
- [x] 8.4 RED: agregar el test «El administrador edita un sillón y la lista se refresca», que espera un `PUT /api/sillones/{id}` con el nombre, la descripción nueva y el `activo` actual. Verificar que falla.
- [x] 8.5 GREEN: implementar la edición precargada y agregar el test «Editar un sillón inexistente muestra una alerta y refresca la lista» (`404 SILLON_NO_ENCONTRADO`). Verificar los dos tests de edición.
- [x] 8.6 RED: agregar el test «El administrador da de baja un sillón y sigue visible como inactivo» (`PUT` con `activo: false` después de confirmar en la fila) y verificar que falla.
- [x] 8.7 GREEN: conectar el `AccionesFila` de la feature a la baja y la reactivación de sillones y agregar los tests «El administrador reactiva un sillón dado de baja» y «Un error al dar de baja un sillón se muestra como alerta». Verificar los tres tests de baja y reactivación, y después `npm test` y `npm run lint`.

## 9. Sillones: permisos y menú

- [x] 9.1 RED: agregar el test «El administrador ve los controles de escritura de sillones» y verificar que falla si los controles no dependen del rol.
- [x] 9.2 GREEN: mostrar el formulario y las acciones solo para `ADMIN` con `useEsAdministrador()`. Verificar que pasa el test de 9.1.
- [x] 9.3 TRIANGULATE: agregar los tests «Recepción y odontología consultan los sillones sin controles de escritura» (un `it.each` por rol con el nombre exacto), «La pantalla de sillones aparece en el menú para los tres roles» y «Un 403 al escribir un sillón se muestra como permiso insuficiente». Verificar que no se emiten escrituras en los roles de consulta.

## 10. Panel de prestaciones: consulta y solo lectura

- [x] 10.1 RED: crear `frontend/src/features/profesionales/prestaciones/PanelPrestaciones.test.tsx`, que monta `/profesionales` y usa la acción «Prestaciones» de una fila. Agregar el test «El panel muestra las prestaciones asociadas con su duración y la marca de inactiva» y verificar que falla.
- [x] 10.2 GREEN: agregar la acción «Prestaciones» por fila, visible para los tres roles. Implementar `PanelPrestaciones` con `key={profesional.id}`, `GET /api/profesionales/{id}/prestaciones` y la lista con la duración y la marca «inactiva». Verificar que pasa el test de 10.1.
- [x] 10.3 TRIANGULATE: agregar los tests «Un profesional sin prestaciones muestra un mensaje en el panel» y «Un fallo al consultar las prestaciones del profesional permite reintentar». Verificar los tres tests de consulta.
- [x] 10.4 TRIANGULATE: agregar los tests «Recepción consulta las prestaciones de un profesional sin controles de edición» y «El odontólogo consulta sus propias prestaciones sin controles de edición». Verificar que no se emiten escrituras ni el `GET` del catálogo para esos roles.

## 11. Panel de prestaciones: opciones y reemplazo

- [x] 11.1 RED: agregar el test «El administrador ve preseleccionadas las asociadas y solo las activas como nuevas opciones» y verificar que falla.
- [x] 11.2 GREEN: para `ADMIN`, pedir en paralelo `GET /api/prestaciones?soloActivas=false` y construir las opciones (activas más las asociadas inactivas) como `<input type="checkbox">` con `<label>` dentro de `<fieldset>`/`<legend>`. Crear `prestaciones/panel.css`, que solo usa variables de `design-system/tokens.css`. Verificar que pasa el test de 11.1.
- [x] 11.3 RED: agregar el test «El administrador guarda un nuevo conjunto de prestaciones», que espera un único `PUT /api/profesionales/{id}/prestaciones` con `prestacionIds` sin repetidos, la lista devuelta y el `Toast`. Verificar que falla.
- [x] 11.4 GREEN: implementar el guardado con un solo `PUT`, adoptar la respuesta `200` y anunciar el éxito. Verificar que pasa el test de 11.3.
- [x] 11.5 TRIANGULATE: agregar los tests «Quitar todas las prestaciones envía una lista vacía», «Cancelar la edición de prestaciones descarta los cambios sin escrituras» y «Guardar las prestaciones dos veces seguidas emite un solo reemplazo» (`PUT` demorado y botón deshabilitado mientras está pendiente). Verificar los tres.
- [x] 11.6 REFACTOR: extraer a una función pura y testeable el cálculo de opciones y de selección inicial, y verificar `npm test` y `npm run lint`.

## 12. Panel de prestaciones: errores del reemplazo

- [x] 12.1 RED: agregar el test «Una prestación dada de baja mientras se editaba se muestra como alerta» (`422 PRESTACION_INACTIVA`), que espera la alerta, la recarga del catálogo y de las asociadas y la selección conservada. Verificar que falla.
- [x] 12.2 GREEN: crear `prestaciones/errores.ts` con el mapeo por código e implementar la recarga ante `422` y `409` sin perder la selección. Verificar que pasa el test de 12.1.
- [x] 12.3 TRIANGULATE: agregar los tests «Una prestación inexistente al guardar se muestra como alerta» (`422 PRESTACION_INEXISTENTE`), «Un conflicto al guardar prestaciones pide reintentar» (`409 PROFESIONAL_PRESTACIONES_CONFLICTO`) y «Un 403 al guardar prestaciones se muestra como permiso insuficiente». Verificar los tres.
- [x] 12.4 RED/GREEN: agregar el test «Guardar prestaciones de un profesional inexistente refresca la lista de profesionales» (`404 PROFESIONAL_NO_ENCONTRADO`) y exponer desde la página un callback que vuelve a consultar `GET /api/profesionales`. Verificar que pasa, junto con `npm test` y `npm run lint`.

## 13. Integración y verificación

- [x] 13.1 Ejecutar `npm test`, `npm run lint` y `npm run build` en `frontend/` y verificar que todo termina sin errores.
- [x] 13.2 Revisar con `git grep -nE "#[0-9a-fA-F]{3,8}\b|[0-9]+px|rgb\(|font-family" -- frontend/src/features/profesionales` que no haya colores, espaciados ni fuentes fijos (solo `var(--…)` de `tokens.css`). Si aparece una coincidencia, reemplazarla por el token correspondiente.
- [x] 13.3 Probar en desarrollo contra el backend real de C-05, C-06 y C-10 con cuentas ficticias. `ADMIN` da de alta, edita, da de baja y reactiva un profesional y un sillón, y asigna y quita prestaciones a un profesional. `RECEPCION` y `ODONTOLOGO` abren `/profesionales`, `/sillones` y el panel sin controles de escritura. Registrar la evidencia sin credenciales ni cookies.
  - Avance (2026-11-23): la prueba manual como `ADMIN` la hizo el usuario y resultó correcta. La de `RECEPCION` y `ODONTOLOGO` se pospone por decisión del equipo hasta que C-24 (front-usuarios-admin) permita crear esas cuentas desde la UI. Mientras tanto, la cobertura de esos roles la dan los tests de Vitest de las tres specs.
  - Hecho (2026-11-25): con cuentas creadas desde C-24, el usuario probó `RECEPCION` y `ODONTOLOGO` contra el backend real y confirmó que abren `/profesionales`, `/sillones` y el panel sin controles de escritura, y que el odontólogo ve solo su profesional. Evidencia registrada sin credenciales ni cookies.
- [x] 13.4 Confirmar con `git diff --name-only master` que no se modificó ningún archivo de `backend/` ni de `features/prestaciones`. Si aparece un hueco real de API, detenerse y reportarlo en lugar de tocar el backend.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Abrir el PR a `master` (lo revisa Juan Pablo) y archivar el change con `openspec-archive-change` cuando esté mergeado.

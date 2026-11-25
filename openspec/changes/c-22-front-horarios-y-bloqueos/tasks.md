# Tasks

## 1. Punto de extensión: varios paneles por fila

- [x] 1.1 RED: en `frontend/src/features/profesionales/profesionales/Profesionales.test.tsx`, agregar el test «Cada fila ofrece Prestaciones, Horarios y Bloqueos y abre un solo panel a la vez». Monta la página con dos paneles de prueba, verifica los botones en orden, que al abrir el segundo se cierra el primero y que el botón activo tiene `aria-pressed="true"`. Verificar que falla.
- [x] 1.2 GREEN: cambiar en `PaginaProfesionales` la prop `panel` por `paneles: readonly PanelDefinicion[]`. El estado guarda `{ etiqueta, profesionalId }` y el panel se monta con `key` `${etiqueta}-${id}`. Hay un botón por panel en «Detalle» (roles de consulta) y en el `extra` de `AccionesFila` (ADMIN). Actualizar `profesionales/routes.ts` para pasar `[PANEL_PRESTACIONES]`. Verificar que pasan el test de 1.1 y todos los tests existentes de `Profesionales.test.tsx` y `PanelPrestaciones.test.tsx`.
- [x] 1.3 Agregar `usePuedeGestionarAgenda()` (ADMIN o RECEPCION) en `features/profesionales/permisos.ts`. Verificar con `npm run lint` y con su uso en los tests de los grupos 3 y 6.

## 2. Horarios: funciones puras de días y validación

- [x] 2.1 RED: crear `horarios/dias.test.ts` con un test que pruebe que `agruparPorDia` devuelve los siete días de lunes a domingo y ordena cada uno por hora de inicio, y otro con una lista vacía. Crear `horarios/validacion.test.ts` con casos de fin igual o anterior al inicio, superposición en el mismo día, tramo contiguo aceptado, otro día aceptado y el tramo editado excluido de la comparación. Verificar que fallan.
- [x] 2.2 GREEN: implementar `horarios/tipos.ts`, `horarios/dias.ts` (`DIAS_SEMANA` ISO 1 a 7 con nombre) y `horarios/validacion.ts` (`validarTramo`, que pasa `HH:mm` a minutos y reutiliza `seSolapan` de `shared/fecha/intervalo`). Verificar que pasan los tests de 2.1.

## 3. Horarios: panel de consulta y solo lectura

- [x] 3.1 RED: crear `horarios/PanelHorarios.test.tsx` (monta `AppRutas` con el registro real, igual que `PanelPrestaciones.test.tsx`) con «El panel agrupa los tramos por día y los ordena por hora de inicio». Verificar que falla.
- [x] 3.2 GREEN: implementar `PanelHorarios` con `GET /api/profesionales/{id}/horarios`, contador de solicitudes, lista por día e indicación de «No atiende». Registrar el panel en `profesionales/routes.ts` como `{ etiqueta: "Horarios", Componente: PanelHorarios }` después de Prestaciones. Verificar que pasa el test de 3.1.
- [x] 3.3 TRIANGULATE: agregar «Un profesional sin tramos muestra todos los días sin atención», «Un fallo al consultar los horarios permite reintentar» y «El odontólogo ve sus horarios sin controles de edición». Verificar los cuatro.

## 4. Horarios: alta, modificación y eliminación

- [x] 4.1 RED: agregar «Recepción ve los controles de edición de horarios» y «Agregar un tramo lo envía con el día ISO y lo muestra en su día» y verificar que fallan.
- [x] 4.2 GREEN: crear `horarios/FormularioTramo.tsx` (`<select>` de día y dos `Campo type="time"`, `noValidate`). Mostrarlo solo con `usePuedeGestionarAgenda()`, enviar el `POST`, volver a consultar y anunciar con `Toast`. Verificar los tests de 4.1.
- [x] 4.3 TRIANGULATE: agregar «Un fin igual o anterior al inicio bloquea el envío sin llamar a la API», «Un tramo superpuesto con otro del mismo día bloquea el envío sin llamar a la API» y «Un tramo contiguo o de otro día se acepta». Verificar que ninguno de los dos primeros emite escrituras.
- [x] 4.4 RED → GREEN: agregar «Modificar un tramo envía PUT y refleja los nuevos valores» e implementar la edición, que precarga el formulario con «Guardar cambios» y «Cancelar». Verificar el test.
- [x] 4.5 RED → GREEN: agregar «Eliminar un tramo pide confirmación y lo retira» y «Cancelar la eliminación no escribe», e implementar la confirmación en línea. Verificar los dos tests.

## 5. Horarios: errores de la API

- [x] 5.1 RED: agregar «Un 409 del servidor muestra la superposición y recarga los tramos», «Un tramo que ya no existe avisa y recarga», «Un profesional que ya no existe refresca la lista de profesionales» y «Un 403 al escribir un tramo muestra permiso insuficiente sin cerrar la sesión». Verificar que fallan.
- [x] 5.2 GREEN: crear `horarios/errores.ts` (`{ mensaje, reaccion }` por código, reutilizando `MENSAJE_PERMISO`, `MENSAJE_GENERICO` y `MENSAJE_NO_ENCONTRADO`, sin `detalle`) y conectarlo al panel. Verificar los cuatro tests.
- [x] 5.3 REFACTOR: limpiar la duplicación entre alta, modificación y eliminación del panel. Verificar `npm test` y `npm run lint` en `frontend/`.

## 6. Bloqueos: validación, consulta y solo lectura

- [x] 6.1 RED: crear `bloqueos/validacion.test.ts` con casos de fin igual o anterior al inicio, motivo en blanco, motivo de 256 caracteres y cuerpo armado en UTC (`01/12/2026 10:00` → `2026-12-01T13:00:00.000Z`, con el motivo recortado). Verificar que fallan.
- [x] 6.2 GREEN: implementar `bloqueos/tipos.ts` y `bloqueos/validacion.ts` (`validarBloqueo` con `aInstanteUtc`). Verificar los tests de 6.1.
- [x] 6.3 RED: crear `bloqueos/PanelBloqueos.test.tsx` con «El panel muestra los bloqueos en hora de Buenos Aires» y verificar que falla.
- [x] 6.4 GREEN: implementar `PanelBloqueos` con `GET /api/profesionales/{id}/bloqueos` y `formatearFecha` y `formatearHora`, y registrarlo en `profesionales/routes.ts` como «Bloqueos» después de Horarios. Verificar el test de 6.3.
- [x] 6.5 TRIANGULATE: agregar «Un profesional sin bloqueos muestra un mensaje», «Un fallo al consultar los bloqueos permite reintentar» y «El odontólogo ve sus bloqueos sin controles de edición». Verificar los cuatro.

## 7. Bloqueos: alta, eliminación y errores

- [x] 7.1 RED: agregar «Recepción ve los controles de bloqueos» y «Agregar un bloqueo envía los instantes en UTC y lo muestra» y verificar que fallan.
- [x] 7.2 GREEN: crear `bloqueos/FormularioBloqueo.tsx` (`Campo type="date"` y `type="time"` para inicio y fin, motivo con `maxLength` 255, `noValidate`), visible solo con `usePuedeGestionarAgenda()`. Conectar el `POST`, volver a consultar y anunciar. Verificar los tests de 7.1.
- [x] 7.3 TRIANGULATE: agregar «Un fin igual o anterior al inicio bloquea el envío sin llamar a la API» y «Un motivo vacío o demasiado largo bloquea el envío sin llamar a la API», en el describe de bloqueos. Verificar que no emiten escrituras.
- [x] 7.4 RED → GREEN: agregar «Eliminar un bloqueo pide confirmación y lo retira» y «Cancelar la eliminación de un bloqueo no escribe», e implementar la confirmación en línea. Verificar los dos tests.
- [x] 7.5 RED → GREEN: agregar «Un bloqueo que ya no existe avisa y recarga», «Un profesional que ya no existe refresca la lista al escribir un bloqueo», «Un conflicto del servidor conserva lo ingresado» y «Un 403 al escribir un bloqueo muestra permiso insuficiente sin cerrar la sesión». Crear `bloqueos/errores.ts` y conectarlo. Verificar los cuatro tests.
- [x] 7.6 REFACTOR: extraer a `panel-agenda.css`, con tokens de `design-system/tokens.css`, los estilos compartidos por los dos paneles. Verificar `npm test` y `npm run lint`.

## 8. Integración y verificación

- [x] 8.1 Ejecutar `npm test`, `npm run lint` y `npm run build` en `frontend/` y verificar que todo termina sin errores.
- [x] 8.2 Revisar con `git grep -nE "#[0-9a-fA-F]{3,8}\b|[0-9]+px|rgb\(|font-family" -- frontend/src/features/profesionales` que no haya colores, espaciados ni fuentes fijos.
- [x] 8.3 Confirmar con `git diff --name-only master` que no se tocó `backend/` y que, fuera de `horarios/` y `bloqueos/`, solo cambiaron `PaginaProfesionales.tsx`, `profesionales/routes.ts`, `permisos.ts` y los tests de C-21 que usan el punto de extensión.
- [x] 8.4 Probar en desarrollo contra el backend real de C-11 y C-12 con cuentas ficticias. `RECEPCION` agrega, modifica y elimina un tramo y crea y elimina un bloqueo. `ODONTOLOGO` abre los dos paneles de su profesional sin controles. Registrar la evidencia sin credenciales ni cookies.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Commit en `valentino-c-22-front-horarios-y-bloqueos` (el agente pregunta antes), push y PR a `master` para la revisión de Juan Pablo.
- Después del merge: `/opsx:archive` para sincronizar `horarios-frontend` y `bloqueos-frontend` en `openspec/specs/`.

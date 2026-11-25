# Verify — c-22-front-horarios-y-bloqueos (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (sin bloqueantes; 2 escenarios parciales y 2 diferencias menores para que Juan Pablo los revise en el PR)

> Primera pasada: NO LISTO por la tarea 8.4. Segunda pasada (el mismo día): se hizo la 8.4 contra el backend real y se corrigieron 2 detalles visuales que aparecieron en esa prueba.

## Validación estructural

`openspec validate c-22-front-horarios-y-bloqueos --strict` → `Change 'c-22-front-horarios-y-bloqueos' is valid`.

## Tareas

31/31 completas.

## Prueba manual contra el backend real (tarea 8.4)

**Entorno:** `docker compose` (PostgreSQL 16 en `127.0.0.1:5433`), el back con `mvnw spring-boot:run` (perfil `dev`, puerto 8080) y el front con `npm run dev` (5173). Se usó el panel del navegador de Claude con un ancho de 734 px.

**Datos:** se crearon en la base local un profesional ficticio («Prueba, Ana», `MN-C22-001`) y dos cuentas ficticias, `recepcion.c22@consultorio.test` (RECEPCION) y `odontologo.c22@consultorio.test` (ODONTOLOGO, vinculada a ese profesional). Las contraseñas se generaron al azar y no se registran acá.

| Rol | Paso | Resultado observado |
|---|---|---|
| RECEPCION | Abrir «Horarios» | 7 días con «No atiende»; formulario «Nuevo tramo» visible |
| RECEPCION | Agregar lunes de 09:00 a 13:00 | `POST /horarios` → 201 y GET de recarga; «Tramo agregado.» y el lunes muestra 09:00 a 13:00 |
| RECEPCION | Agregar lunes de 12:00 a 14:00 | La SPA muestra «El tramo se superpone con el de 09:00 a 13:00.» **sin emitir el POST** (comprobado en las solicitudes de red) |
| RECEPCION | Editar a 10:00–14:00 | `PUT /horarios/1` → 200; «Tramo actualizado.» y el lunes muestra 10:00 a 14:00 |
| RECEPCION | Eliminar y confirmar | `DELETE /horarios/1` → 204; «Tramo eliminado.» y el lunes vuelve a «No atiende» |
| RECEPCION | Agregar martes de 09:00 a 13:00 | 201 (se dejó para la vista del odontólogo) |
| RECEPCION | Bloqueo del 01/12/2026 18:00 al 10:00 | «El fin debe ser posterior al inicio.» junto a «Hora de fin», sin POST |
| RECEPCION | Bloqueo del 01/12/2026 10:00 al 18:00, «Congreso» | `POST /bloqueos` → 201; se muestra «01/12/2026 10:00 al 01/12/2026 18:00 Congreso». En la base: `2026-12-01 13:00:00+00` a `21:00:00+00` (UTC) |
| RECEPCION | Bloqueo del 10/12 «Trámite», después eliminar y confirmar | 201, después `DELETE /bloqueos/2` → 204; queda solo «Congreso» |
| ODONTOLOGO | `/profesionales` con «Horarios» y «Bloqueos» | Solo su registro; ve el martes de 09:00 a 13:00 y «Congreso» **sin formulario ni botones de editar o eliminar**; solo GET en la red |

**Consola:** dos `401` de `/api/auth/me` antes de iniciar sesión, que es lo esperado. No hubo otros errores. Los `DELETE` 204 figuran como `ERR_ABORTED` en el registro de red del navegador porque la respuesta no tiene cuerpo. El estado es 204 y la UI se actualizó.

**Corregido a partir de la prueba (sin cambiar el comportamiento):**
- Los inputs de fecha y hora desbordaban la grilla del formulario. Se agregó `min-width: 0` a los hijos de `.agenda-formulario`.
- La acción de fila del panel abierto no se distinguía de las demás. Se agregó un estilo para `.btn--secondary[aria-pressed="true"]` con `--color-primary-soft` y `--color-primary`, importado también desde `PaginaProfesionales`.

Después del arreglo se volvieron a correr los tests, lint y `tsc`: siguen en verde.

## Matriz escenario → test

Los 30 escenarios tienen un `it(...)` con el nombre exacto. Se leyó cada test y se confirmó que afirma el THEN.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| horarios-frontend | El panel agrupa los tramos por día y los ordena por hora de inicio | Cubierto | `horarios/PanelHorarios.test.tsx:115` | Orden del lunes, miércoles y «No atiende» en los demás días, con un único GET |
| horarios-frontend | Un profesional sin tramos muestra todos los días sin atención | Cubierto | `horarios/PanelHorarios.test.tsx:129` | 7 grupos con «No atiende» |
| horarios-frontend | Un fallo al consultar los horarios permite reintentar | Cubierto | `horarios/PanelHorarios.test.tsx:139` | Alerta, Reintentar y segundo GET |
| horarios-frontend | Un fin igual o anterior al inicio bloquea el envío sin llamar a la API | Cubierto | `horarios/PanelHorarios.test.tsx:228` | 12:00–12:00 y 12:00–09:00, sin escrituras |
| horarios-frontend | Un tramo superpuesto con otro del mismo día bloquea el envío sin llamar a la API | Cubierto | `horarios/PanelHorarios.test.tsx:240` | El mensaje nombra el tramo 09:00 a 13:00 |
| horarios-frontend | Un tramo contiguo o de otro día se acepta | Cubierto | `horarios/PanelHorarios.test.tsx:251` | Borde `[inicio, fin)` (13:00) y otro día: dos POST |
| horarios-frontend | Agregar un tramo lo envía con el día ISO y lo muestra en su día | Cubierto | `horarios/PanelHorarios.test.tsx:211` | Cuerpo `diaSemana: 3`, recarga, Toast y miércoles |
| horarios-frontend | Modificar un tramo envía PUT y refleja los nuevos valores | Cubierto | `horarios/PanelHorarios.test.tsx:271` | PUT a `/horarios/5` y el lunes de 10:00 a 14:00 |
| horarios-frontend | Eliminar un tramo pide confirmación y lo retira | Cubierto | `horarios/PanelHorarios.test.tsx:293` | Sin escritura antes de confirmar; DELETE |
| horarios-frontend | Cancelar la eliminación no escribe | Cubierto | `horarios/PanelHorarios.test.tsx:306` | |
| horarios-frontend | Un 409 del servidor muestra la superposición y recarga los tramos | Cubierto | `horarios/PanelHorarios.test.tsx:321` | No muestra el `detalle`; segundo GET |
| horarios-frontend | Un tramo que ya no existe avisa y recarga | Parcial | `horarios/PanelHorarios.test.tsx:336` | Prueba el 404 solo en DELETE; el WHEN también nombra «modifica» (mismo `escribir` y traductor) |
| horarios-frontend | Un profesional que ya no existe refresca la lista de profesionales | Cubierto | `horarios/PanelHorarios.test.tsx:352` | Alerta de la página, segundo GET de la lista y el panel se cierra |
| horarios-frontend | Un 403 al escribir un tramo muestra permiso insuficiente sin cerrar la sesión | Cubierto | `horarios/PanelHorarios.test.tsx:368` | Sin `detalle` ni logout |
| horarios-frontend | Recepción ve los controles de edición de horarios | Cubierto | `horarios/PanelHorarios.test.tsx:200` | |
| horarios-frontend | El odontólogo ve sus horarios sin controles de edición | Cubierto | `horarios/PanelHorarios.test.tsx:153` | Sin formulario ni botones; sin escrituras |
| bloqueos-frontend | El panel muestra los bloqueos en hora de Buenos Aires | Cubierto | `bloqueos/PanelBloqueos.test.tsx:154` | `13:00Z` se muestra como 10:00 |
| bloqueos-frontend | Un profesional sin bloqueos muestra un mensaje | Cubierto | `bloqueos/PanelBloqueos.test.tsx:163` | |
| bloqueos-frontend | Un fallo al consultar los bloqueos permite reintentar | Cubierto | `bloqueos/PanelBloqueos.test.tsx:171` | |
| bloqueos-frontend | Un fin igual o anterior al inicio bloquea el envío sin llamar a la API | Cubierto | `bloqueos/PanelBloqueos.test.tsx:227` | Mensaje junto a «Hora de fin» con `aria-invalid` |
| bloqueos-frontend | Un motivo vacío o demasiado largo bloquea el envío sin llamar a la API | Cubierto | `bloqueos/PanelBloqueos.test.tsx:238` | En blanco y con 256 caracteres |
| bloqueos-frontend | Agregar un bloqueo envía los instantes en UTC y lo muestra | Cubierto | `bloqueos/PanelBloqueos.test.tsx:206` | `13:00:00.000Z` y `21:00:00.000Z`, motivo recortado y formulario vacío |
| bloqueos-frontend | Eliminar un bloqueo pide confirmación y lo retira | Cubierto | `bloqueos/PanelBloqueos.test.tsx:251` | |
| bloqueos-frontend | Cancelar la eliminación de un bloqueo no escribe | Cubierto | `bloqueos/PanelBloqueos.test.tsx:264` | |
| bloqueos-frontend | Un bloqueo que ya no existe avisa y recarga | Cubierto | `bloqueos/PanelBloqueos.test.tsx:278` | |
| bloqueos-frontend | Un profesional que ya no existe refresca la lista al escribir un bloqueo | Cubierto | `bloqueos/PanelBloqueos.test.tsx:294` | |
| bloqueos-frontend | Un conflicto del servidor conserva lo ingresado | Cubierto | `bloqueos/PanelBloqueos.test.tsx:310` | 409, sin `detalle`, conserva fecha, hora y motivo |
| bloqueos-frontend | Un 403 al escribir un bloqueo muestra permiso insuficiente sin cerrar la sesión | Parcial | `bloqueos/PanelBloqueos.test.tsx:327` | Prueba el 403 solo en DELETE; el WHEN también nombra «crea» (mismo traductor) |
| bloqueos-frontend | Recepción ve los controles de bloqueos | Cubierto | `bloqueos/PanelBloqueos.test.tsx:196` | |
| bloqueos-frontend | El odontólogo ve sus bloqueos sin controles de edición | Cubierto | `bloqueos/PanelBloqueos.test.tsx:185` | |

Tests de apoyo, que no son escenarios de spec:

- `profesionales/Profesionales.test.tsx`: «Cada fila ofrece Prestaciones, Horarios y Bloqueos y abre un solo panel a la vez», con `aria-pressed` y un panel por vez.
- `horarios/dias.test.ts`, `horarios/validacion.test.ts` y `bloqueos/validacion.test.ts`: funciones puras.

## Ejecución de tests

- **Front:** `npx vitest run` en `frontend/` → 30 archivos y **225 tests pasados**, 0 fallidos y 0 saltados. `npm run lint` sin errores. `npx tsc -b --noEmit` sin errores. `npm run build` OK, ejecutado en el apply el mismo día.
- **Back:** `./mvnw -q verify` en `backend/` con Docker 27.2 → `EXIT=0`. Surefire: 115 tests, 0 fallas, 0 errores y 0 saltados. Failsafe (Testcontainers): 371 tests, 0 fallas, 0 errores y 0 saltados. Los reportes se generaron a las 10:18 del 2026-11-25. Los `ERROR duplicate key` del log son de tests que provocan esos conflictos a propósito. El change no modifica `backend/`, así que esto solo confirma que no rompe nada.

## Reglas de negocio y reglas duras

- **RN-16 (tramos sin superposición, `[inicio, fin)`):** cubierta con validación local en la SPA. El caso de borde (13:00 contiguo) se acepta y la superposición real (12:00–14:00) se rechaza. El `409 HORARIO_SUPERPUESTO` del servidor también está cubierto.
- **RN-33 (el odontólogo es solo lectura):** cubierta en los dos paneles (sin controles ni escrituras). La API sigue siendo la que autoriza y el `403` se informa sin cerrar la sesión.
- **RN-04 y RN-17:** no aplican en el front. RN-04 la valida C-19 al crear turnos, y el rechazo de RN-17 por turnos superpuestos lo agrega el back en C-30. El panel ya muestra un 409 o 422 y conserva lo ingresado.
- **Reglas duras:**
  - Sin datos reales: solo profesionales y motivos ficticios, y no hay emails en los tests de la feature.
  - Sin secretos ni credenciales en el diff.
  - Sin `any`.
  - Sin colores, `px` ni fuentes fijos en `features/profesionales` (`git grep` vacío): todo usa `var(--…)` de `tokens.css`.
  - El backend no se modificó.
  - Ninguna violación.

## Diferencias con design.md

- **Menor:**
  - Se agregaron `features/profesionales/useListadoPanel.ts` (carga y escritura compartida por los dos paneles) y `ConfirmarEliminacion.tsx` (confirmación en línea). Son refactors para no duplicar código y no figuraban en el diseño.
  - La 8.3 listaba los archivos tocados fuera de `horarios/` y `bloqueos/` y estos dos no estaban.
- **Menor:** la decisión 6 nombraba la reacción `conservar` para 409 y 422 de bloqueos. Se implementó como `ninguna`, porque el formulario conserva lo ingresado ante cualquier error de escritura. El comportamiento coincide con la spec.
- **Visual (`system-only`):** se usan solo tokens. El `<select>` replica `.select` de `design-system/components.html` con medidas en tokens, porque el kit de la SPA no tenía esa clase.

## Qué NO se pudo verificar

- **El `409 HORARIO_SUPERPUESTO` real del servidor:** la validación local lo frena antes. Ese camino solo está probado con la API simulada.
- **Visual en escritorio ancho (1440 px) y en móvil (390 px):** la prueba se hizo a 734 px. La barra de scroll horizontal de esa prueba viene de la tabla de profesionales de C-21, no de este change.

## Próximo paso

1. Commit en `valentino-c-22-front-horarios-y-bloqueos` (el agente pregunta antes), push y PR a `master`.
2. Para Juan Pablo en el PR: los 2 escenarios parciales (modificar con `HORARIO_NO_ENCONTRADO`, crear con `403` en bloqueos) y las diferencias menores con `design.md`.
3. Después del merge: `/opsx:archive c-22-front-horarios-y-bloqueos`.

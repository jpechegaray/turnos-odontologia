# Verify — c-47-front-lista-espera (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (una vez mergeado el PR)

## Validación estructural

`openspec validate c-47-front-lista-espera --strict` dio: válido.

## Tareas

Hay 10 de 10 tareas completas. La tarea 5.2 se cierra con este informe.

## Matriz escenario → test

Todos los tests están en `frontend/src/features/lista-espera/ListaEspera.test.tsx`.

| Requisito | Escenario | Estado | Línea | Nota |
|---|---|---|---|---|
| Acceso | Recepción ve la lista de espera en el menú | Cubierto | 154 | El enlace apunta a `/lista-espera` y aparece el título de la pantalla |
| Acceso | El odontólogo no accede a la lista de espera | Cubierto | 161 | Muestra «No tenés permiso», no aparece en el menú y no hay consultas a la API |
| Alta | Un alta válida agrega la entrada al listado | Cubierto | 240 | Hace 1 POST aunque se haga doble envío, con el cuerpo exacto (nulos en lo opcional), y la entrada aparece en el listado |
| Alta | El alta sin datos obligatorios no se envía | Cubierto | 259 | Marca los tres obligatorios y no hace POST |
| Alta | Un rango de fechas invertido no se envía | Cubierto | 269 | El campo «Hasta» queda con `aria-invalid` |
| Alta | Una franja incompleta o invertida no se envía | Cubierto | 279 | Prueba la franja con una sola hora y la franja invertida |
| Rechazos del alta | Una entrada activa duplicada se informa sin perder el formulario | Cubierto | 291 | Se conservan paciente, prestación y fecha |
| Rechazos del alta | Un paciente sin email no se puede anotar | Cubierto | 304 | |
| Listado | El listado muestra las entradas activas con nombres | Cubierto | 171 | Muestra nombres, fechas, franja, «Cualquiera» y «Sin preferencia» |
| Listado | Cambiar el filtro de estado consulta ese estado | Cubierto | 185 | Consulta `?estado=ASIGNADA` |
| Listado | Un estado sin entradas lo informa | Cubierto | 194 | |
| Baja | Dar de baja una entrada la quita de las activas | Cubierto | 202 | Hace 1 DELETE a `/api/lista-espera/10` |
| Baja | Desistir de la baja no envía el pedido | Cubierto | 214 | |
| Baja | Dar de baja una entrada que ya no está activa recarga el listado | Cubierto | 223 | Muestra el aviso y vuelve a consultar |
| Sugerencias | Buscar un hueco lista las entradas compatibles | Cubierto | 316 | 10:00 en Buenos Aires se envía como `13:00:00.000Z` |
| Sugerencias | Un hueco sin compatibles lo informa | Cubierto | 328 | |
| Sugerencias | Un hueco con horario invertido no se consulta | Cubierto | 335 | |
| Asignación | Asignar el hueco crea el turno y cierra la entrada | Cubierto | 343 | El cuerpo es `{profesionalId, inicio}` y la entrada sale de las sugerencias y del listado |
| Asignación | Un segundo clic en asignar no repite el pedido | Cubierto | 353 | |
| Asignación | Un horario ocupado al asignar se informa y recarga las sugerencias | Cubierto | 368 | 409 con `recurso: PROFESIONAL` |
| Asignación | Asignar una entrada que ya no está activa se informa | Cubierto | 377 | |
| Asignación | Una entrada incompatible con el hueco se informa | Cubierto | 386 | |

Los 22 escenarios quedaron cubiertos.

## Ejecución de tests

- `npx vitest run` (en `frontend/`): 46 archivos y 430 tests pasados, 0 fallidos y 0 saltados.
- `npm run lint`: sin errores ni advertencias.
- `npx tsc -b` y `npm run build`: OK.
- Backend (`mvn verify`): no se ejecutó porque el change no modifica `backend/` (`git diff origin/master -- backend` está vacío).

## Reglas de negocio y reglas duras

- RN-21: la prestación y la fecha de fin son obligatorias, y el profesional, la fecha de inicio y la franja son opcionales. La SPA valida la forma antes de enviar. El duplicado y el paciente sin email los decide el servidor, y la pantalla muestra esos rechazos.
- RN-22: la compatibilidad la calcula el backend. La SPA envía el intervalo del hueco en Buenos Aires (DD-05) como instantes ISO con zona.
- RN-20 y RN-33: el odontólogo no ve la pantalla. La API mantiene la autorización efectiva.
- RN-35: la API sigue devolviendo solo IDs. Los nombres se resuelven con los catálogos que el rol ya puede leer.
- Datos ficticios: «Sol Ficticia», «Luz Inventada» y emails `@example.test`.
- Secretos: no se agregaron.
- Reglas 12 y 13: TypeScript estricto sin `any`, componentes en PascalCase y CSS solo con variables de `design-system/tokens.css`.

## Diferencias con design.md

- Menor: se agregaron `Seccion.tsx` (panel con nombre accesible) y `presentacion.ts` (textos de fechas, franja y estado), que no figuraban en la lista de archivos del design.
- Menor: `SeleccionPaciente` (de `features/agenda/crear`) ahora importa su propio `alta-turno.css`. Antes funcionaba solo dentro del alta de turnos. No cambia nada en la agenda.
- Menor: al cambiar el filtro de estado, la tabla queda montada hasta que llega la consulta nueva, en lugar de mostrar «Cargando». Así se evita un salto de foco.

## Verificación visual

La política del design system es `system-only`, así que no hay mockups. Se hizo un recorrido en el navegador con la sesión de recepción y la API simuladas a 1440 × 900 y 390 × 844, con validación, búsqueda de compatibles y asignación. No hay desplazamiento horizontal de la página. En el celular, las tablas se desplazan dentro de su marco, como en el resto del panel. En el recorrido se detectó que el campo «Dato del paciente» desbordaba el panel a 390 px y se corrigió con `box-sizing: border-box`. Las capturas están en `verify/`.

## Qué NO se pudo verificar

- Integración contra el backend real: el recorrido usó respuestas simuladas con la forma de los DTO de C-18, C-31 y C-43.
- El requisito de rechazos del alta menciona los errores 400 por campo. El código los asigna a cada campo (`rechazoDelAlta`), pero la spec no tiene un escenario para ese caso y no hay un test que lo cubra.
- Que un lector de pantalla real anuncie los mensajes. Se verificaron la semántica (regiones con nombre, `role="status"`, `role="alert"` y `aria-invalid`), pero no se probó con NVDA ni con VoiceOver.

## Próximo paso

Commit en `valentino`, PR a `master` (lo revisa Juan Pablo) y, una vez mergeado, `/opsx:archive c-47-front-lista-espera` y actualizar `CHANGES.md`.

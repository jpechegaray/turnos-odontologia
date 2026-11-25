# Verify — c-34-front-registro-notificaciones (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (después del merge del PR)

## Validación estructural

`openspec validate c-34-front-registro-notificaciones --strict` → `Change 'c-34-front-registro-notificaciones' is valid`.

## Tareas

9 de 9 completas. La 6.3 se cierra con este informe. CHANGES.md se actualiza en el commit de archivado, después del merge, que es la convención del equipo.

## Matriz escenario → test

Spec: `specs/notificaciones-frontend/spec.md`. Test: `frontend/src/features/notificaciones/Notificaciones.test.tsx`, integración con `AppRutas`, `construirRegistro` y `simularSesionActiva`.

| Requisito | Escenario | Estado | Línea | Nota |
|---|---|---|---|---|
| Acceso según rol | La pantalla de notificaciones aparece en el menú para administración y recepción | Cubierto | 88 | ADMIN y RECEPCION; el enlace apunta a `/notificaciones`. |
| Acceso según rol | El odontólogo no ve la pantalla de notificaciones | Cubierto | 106 | Aviso de permiso, sin entrada en el menú y 0 consultas a `/api/notificaciones`. |
| Listado | Al abrir la pantalla se lista la primera página del registro | Cubierto | 118 | URL exacta, orden recibido y estados en español. |
| Listado | Los datos del envío se muestran en español y en hora de Buenos Aires | Cubierto | 144 | "Recordatorio", "#42", "04/12/2026 10:00", "1" y "03/12/2026 10:05". |
| Listado | Un aviso de hueco sin turno se identifica como tal | Cubierto | 154 | "Aviso de hueco" en el tipo y en la columna de turno. |
| Listado | Un envío pendiente muestra la marca de vacío en la fecha de envío | Cubierto | 161 | |
| Último error | Un envío fallido muestra su último error | Cubierto | 169 | |
| Último error | Un envío enviado no muestra error | Cubierto | 190 | Aunque la API mande texto, se muestra la marca de vacío. |
| Filtros | Filtrar por estado envía solo ese filtro | Cubierto | 200 | |
| Filtros | Los filtros de estado y turno se combinan | Cubierto | 213 | Turno recortado (" 42 " → `turnoId=42`). |
| Filtros | Un número de turno inválido no consulta | Cubierto | 227 | "abc" y "0": mensaje en el campo y la cantidad de consultas no cambia. |
| Filtros | Buscar vuelve a la primera página | Cubierto | 242 | Desde la página 2, la búsqueda vuelve con `pagina=0`. |
| Filtros | Una búsqueda sin coincidencias muestra un mensaje | Cubierto | 263 | Además, la paginación no se muestra. |
| Filtros | Sin envíos registrados se muestra un mensaje | Cubierto | 278 | |
| Paginación | Siguiente consulta la página contigua con los filtros vigentes | Cubierto | 286 | URL con `estado=FALLIDA&pagina=1`, el texto y los botones en el extremo. |
| Paginación | Los botones se deshabilitan en los extremos | Cubierto | 308 | |
| Errores | Un error de carga ofrece reintentar | Cubierto | 318 | Repite la misma URL; no hay tabla mientras dura el error. |
| Errores | Un 403 de la API muestra el aviso de permiso | Cubierto | 341 | |

**Resultado:** 18 de 18 escenarios cubiertos, ninguno parcial. Hay además 12 tests unitarios en `consulta.test.ts` (URL, validación del turno, fechas y texto de la paginación).

## Ejecución de tests

- **Front:** `npx vitest run` (2026-11-25) → 39 archivos y 349 tests en verde; los de `features/notificaciones` son 30. `npm run lint` sin problemas. `npx tsc -b --noEmit` sin errores. `npm run build` OK.
- **Backend:** no se ejecutó, porque el change no toca `backend/`.
- **Prueba manual en dev** (tarea 6.2): backend y front reales, con recepción temporal y 25 envíos ficticios.
  - La tabla, el último error, el "Aviso de hueco" y la paginación se comportaron como dice la spec.
  - Los filtros se probaron contra la API con las URLs que arma la pantalla.
  - Mobile (375 px) y escritorio revisados.
  - Datos y cuenta borrados al terminar; servidores y contenedor detenidos.

## Reglas de negocio y reglas duras

- **US-020 / RN-30** (registro filtrable por estado y turno, último error de los fallidos): cubierto por los escenarios de listado, filtros y último error.
- **RN-33** (el odontólogo no accede): "El odontólogo no ve la pantalla de notificaciones". La API conserva la autorización efectiva.
- **Reglas duras:**
  - Datos ficticios: los tests usan `@example.test` y la prueba manual usó `*.c34@ejemplo.test`, ya borrados.
  - Sin secretos en el diff.
  - Regla 12: TypeScript `strict`, sin `any` y componentes en PascalCase.
  - Regla 13: `notificaciones.css` usa solo variables de `tokens.css`; un grep de hexadecimales y `px` en la feature no da resultados.
  - Regla 11: cada `it(...)` lleva el nombre exacto del escenario.

## Diferencias con design.md

- **Menor:** la columna de envío se llama "Fecha de envío" (en el design decía "Enviada") para no repetir el texto del estado "Enviada". La spec ya hablaba de "fecha de envío".
- **Menor:** el campo Turno no lleva texto de ayuda; desalineaba la barra de filtros.
- **Menor:** la sección usa `display: grid; grid-template-columns: minmax(0, 1fr)`, como la agenda, para que en el celular la tabla se desplace dentro de su marco y no empuje los filtros.

## Qué NO se pudo verificar

- **Los filtros en el navegador con clics:** durante la prueba manual el usuario estaba usando el panel. Se probaron llamando a la API con las mismas URLs; el comportamiento de la UI lo cubren los tests de Vitest.
- **Un desborde horizontal de unos 17 px en el layout general**, que también pasa en `/pacientes`. Viene de antes y queda fuera de C-34.

## Próximo paso

1. Commit en `valentino-c-34-front-registro-notificaciones`, con consulta previa.
2. Push y PR a `master`, que revisa Juan Pablo.
3. Con C-34 y C-36 mergeados, y después de que Valentino lo confirme: archivar los dos en un mismo PR y actualizar CHANGES.md.

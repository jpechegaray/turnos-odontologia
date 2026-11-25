# Tasks

> TDD estricto: cada tarea arranca con el test en rojo. Cada `it(...)` lleva el nombre exacto del escenario de `specs/notificaciones-frontend/spec.md`. Test de integración con `AppRutas` + `construirRegistro` + `simularSesionActiva`, como `features/pacientes`.

## 1. Ruta y acceso

- [x] 1.1 `routes.ts` (`/notificaciones`, menú "Notificaciones" con orden 80, roles `ADMIN` y `RECEPCION`) con una página mínima; borrar el `.gitkeep`. Tests: "La pantalla de notificaciones aparece en el menú para administración y recepción" y "El odontólogo no ve la pantalla de notificaciones".

## 2. Consulta y presentación (funciones puras)

- [x] 2.1 `tipos.ts`, `consulta.ts` (`consultaDeNotificaciones`, `turnoValido`) y `presentacion.ts` (etiquetas, marca de vacío, `fechaHora`), con tests unitarios de la URL en orden fijo, del recorte del turno y del rechazo de "abc", "0", "-1" y "1.5".

## 3. Listado

- [x] 3.1 `useNotificaciones` + `PaginaNotificaciones` con la tabla. Tests: "Al abrir la pantalla se lista la primera página del registro", "Los datos del envío se muestran en español y en hora de Buenos Aires", "Un aviso de hueco sin turno se identifica como tal", "Un envío pendiente muestra la marca de vacío en la fecha de envío", "Un envío fallido muestra su último error" y "Un envío enviado no muestra error".

## 4. Filtros

- [x] 4.1 `FiltrosNotificaciones` (select de estado + campo de turno con validación). Tests: "Filtrar por estado envía solo ese filtro", "Los filtros de estado y turno se combinan", "Un número de turno inválido no consulta", "Buscar vuelve a la primera página", "Una búsqueda sin coincidencias muestra un mensaje" y "Sin envíos registrados se muestra un mensaje".

## 5. Paginación y errores

- [x] 5.1 Paginación (texto y botones). Tests: "Siguiente consulta la página contigua con los filtros vigentes" y "Los botones se deshabilitan en los extremos".
- [x] 5.2 Errores de carga. Tests: "Un error de carga ofrece reintentar" y "Un 403 de la API muestra el aviso de permiso".

## 6. Cierre

- [x] 6.1 `npx vitest run`, `npm run lint` y `npm run build` en verde, sin `any` y solo con tokens y clases del kit (regla dura 13).
- [x] 6.2 Prueba manual en dev (2026-11-25): backend y front reales, recepción temporal y 25 envíos ficticios (21 enviados, 3 fallidos y 1 pendiente, con avisos de hueco sin turno). Se vieron la tabla en español y hora de Buenos Aires, el último error de los fallidos, "Aviso de hueco" en la columna de turno y la paginación (página 2 con 5 envíos y "Siguiente" deshabilitado). Los filtros se probaron contra la API con las URLs que arma la pantalla (FALLIDA 3, turno 9001 2, combinados 1). Ajustes por lo que se vio: se quitó la ayuda del campo Turno (desalineaba la barra) y la sección pasó a `grid minmax(0, 1fr)` como la agenda, para que en el celular la tabla se desplace dentro de su marco. En el layout general la página excede el ancho en unos 17 px también en /pacientes; queda fuera de C-34.
- [x] 6.3 `openspec-verify` → `verify.md`. CHANGES.md se actualiza en el commit de archivado, después del merge.

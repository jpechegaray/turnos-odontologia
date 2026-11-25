# Verify — c-21-front-profesionales-y-sillones (2026-11-24)

**Veredicto:** LISTO PARA ARCHIVAR

> `openspec validate --strict` pasa. Hay 60 de 61 tareas hechas, y la que falta (**13.3**) figura como
> **Tarea diferida** en `design.md` (Risks / Trade-offs), así que no bloquea. Los **51 escenarios** de las
> tres delta specs están **Cubiertos** por tests que afirman el THEN. Vitest, lint y build terminan en
> verde, y `mvnw verify` del backend también, con Docker encendido. El change toca solo
> `frontend/src/features/profesionales/**`: no modifica ningún archivo de `backend/` ni de
> `features/prestaciones`. No se encontraron violaciones de reglas duras.

## Validación estructural

- `openspec validate c-21-front-profesionales-y-sillones --strict`: **válido** ("Change '…' is valid").
- `openspec status --change … --json`: existen `proposal.md`, `design.md`, `tasks.md` y `specs/**`
  (además de `.openspec.yaml`).
- Delta specs:
  - `profesionales-frontend`: 7 requirements, 21 escenarios.
  - `sillones-frontend`: 6 requirements, 15 escenarios.
  - `profesional-prestaciones-frontend`: 5 requirements, 15 escenarios.
  - En total, **51 escenarios**.

## Tareas

- `tasks.md`: **60/61** con `[x]`.
- Pendiente: **13.3**, la prueba manual en desarrollo contra el backend real con `ADMIN`, `RECEPCION` y
  `ODONTOLOGO`. Es una **tarea diferida y no bloqueante**: `design.md` (Risks / Trade-offs) la marca así
  por decisión del equipo del 2026-11-24.
  - Motivo: el seed de desarrollo solo crea un `ADMIN`, y la pantalla para crear usuarios llega con C-24.
  - La parte de `ADMIN` ya la hizo el usuario el 2026-11-23, con resultado correcto (según el avance
    anotado en `tasks.md`; esta verificación no la repitió).
  - Mientras tanto, el comportamiento de los dos roles de consulta lo cubren los tests de Vitest (ver la
    matriz).
  - Se completa y se marca después del merge de C-24.
- Esta verificación volvió a ejecutar 13.1 (test, lint y build), 13.2 (búsqueda de valores fijos) y
  13.4 (sin cambios de backend).

## Matriz escenario → test

Se respeta la convención del equipo: cada `it(...)` lleva el texto exacto del escenario (51/51). Las rutas
son relativas a `frontend/src/features/profesionales/`. Todos los tests montan `AppRutas` con el
`routes.ts` real de la subfeature y usan `fetch` simulado con `simularSesionActiva`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| profesionales-frontend | El listado muestra los profesionales con su matrícula y estado | Cubierto | profesionales/Profesionales.test.tsx:54 | Afirma «Apellido, Nombre», matrícula, especialidad o «—», Activo/Inactivo y el orden de la API; 1 `GET` y 0 escrituras |
| profesionales-frontend | El listado vacío de profesionales muestra un mensaje | Cubierto | profesionales/Profesionales.test.tsx:74 | Afirma el mensaje vacío. Lo de «ninguna fila» se afirma débil: solo verifica que no haya una celda «Pérez» |
| profesionales-frontend | Un fallo al listar profesionales permite reintentar | Cubierto | profesionales/Profesionales.test.tsx:81 | Con `500`: alerta y sin tabla. «Reintentar» trae la lista; 2 `GET` |
| profesionales-frontend | Una matrícula en blanco bloquea el envío sin llamar a la API | Cubierto | profesionales/Profesionales.test.tsx:117 | Mensaje, `aria-invalid` en Matrícula y 0 escrituras |
| profesionales-frontend | Un nombre o apellido en blanco bloquea el envío sin llamar a la API | Cubierto | profesionales/Profesionales.test.tsx:128 | Ambos mensajes y `aria-invalid`; 0 escrituras. También prueba que el error se limpia al editar |
| profesionales-frontend | Un texto de más de 255 caracteres bloquea el envío sin llamar a la API | Cubierto | profesionales/Profesionales.test.tsx:146 | Especialidad de 256 caracteres: «Máximo 255 caracteres.», `aria-invalid` y 0 escrituras |
| profesionales-frontend | El administrador da de alta un profesional y la lista se refresca | Cubierto | profesionales/Profesionales.test.tsx:189 | `POST` con textos recortados y `especialidad: null`; ≥2 `GET`; fila nueva, formulario vacío y Toast |
| profesionales-frontend | Una matrícula duplicada se muestra como alerta sin perder los datos | Cubierto | profesionales/Profesionales.test.tsx:207 | `409`: mensaje sin `detalle`; conserva los 4 campos |
| profesionales-frontend | El administrador edita un profesional y la lista se refresca | Cubierto | profesionales/Profesionales.test.tsx:234 | Formulario precargado. `PUT /api/profesionales/2` con los datos nuevos y `activo: false` (el actual); ≥2 `GET`; muestra la especialidad nueva |
| profesionales-frontend | Editar con una matrícula de otro profesional muestra una alerta | Cubierto | profesionales/Profesionales.test.tsx:261 | `409`: alerta y edición abierta con lo ingresado |
| profesionales-frontend | Editar un profesional inexistente muestra una alerta y refresca la lista | Cubierto | profesionales/Profesionales.test.tsx:283 | `404`: alerta sin `detalle`; 2 `GET`; la fila desaparece |
| profesionales-frontend | El administrador da de baja un profesional y sigue visible como inactivo | Cubierto | profesionales/Profesionales.test.tsx:305 | Confirma en la fila. `PUT` con los datos actuales y `activo: false`; ningún `DELETE`; fila Inactivo; ≥2 `GET` |
| profesionales-frontend | Cancelar la confirmación de baja no emite escrituras | Cubierto | profesionales/Profesionales.test.tsx:327 | Vuelven Editar y Dar de baja; 0 escrituras |
| profesionales-frontend | El administrador reactiva un profesional dado de baja | Cubierto | profesionales/Profesionales.test.tsx:340 | `PUT` con `activo: true`; Activo; ≥2 `GET` |
| profesionales-frontend | Un error al dar de baja un profesional se muestra como alerta | Cubierto | profesionales/Profesionales.test.tsx:359 | `422`: alerta genérica sin `detalle`; 2 `GET` |
| profesionales-frontend | El administrador ve los controles de escritura de profesionales | Cubierto | profesionales/Profesionales.test.tsx:379 | Formulario, columna «Acciones» y Editar + Dar de baja o Reactivar según el estado |
| profesionales-frontend | Recepción consulta los profesionales sin controles de escritura | Cubierto | profesionales/Profesionales.test.tsx:401 | Lista completa; ni formulario ni botones de escritura ni columna «Acciones»; 0 escrituras |
| profesionales-frontend | La pantalla de profesionales aparece en el menú para los tres roles | Cubierto | profesionales/Profesionales.test.tsx:410 | Recorre los tres roles y afirma en cada uno el link con `href="/profesionales"` |
| profesionales-frontend | Un 403 al escribir un profesional se muestra como permiso insuficiente | Cubierto | profesionales/Profesionales.test.tsx:425 | Alerta de permiso sin `detalle`; datos conservados; menú visible; sin `logout` |
| profesionales-frontend | El odontólogo vinculado ve solo su registro sin controles de escritura | Cubierto | profesionales/Profesionales.test.tsx:447 | 1 fila de datos; sin controles; 0 escrituras |
| profesionales-frontend | El odontólogo sin vínculo ve un aviso de falta de vínculo | Cubierto | profesionales/Profesionales.test.tsx:456 | Aviso informativo, sin `role=alert`, tabla ni «Reintentar»; sesión activa; 0 escrituras |
| sillones-frontend | El listado muestra los sillones con su descripción y estado | Cubierto | sillones/Sillones.test.tsx:48 | Orden «Sillón 1» → «Sillón 2»; descripción o «—»; estado; 0 escrituras |
| sillones-frontend | El listado vacío de sillones muestra un mensaje | Cubierto | sillones/Sillones.test.tsx:66 | Mensaje vacío. «Ninguna fila» se afirma débil, por la ausencia de una celda |
| sillones-frontend | Un fallo al listar sillones permite reintentar | Cubierto | sillones/Sillones.test.tsx:76 | Con `500`: alerta. «Reintentar» trae la lista; 2 `GET` |
| sillones-frontend | Un nombre de sillón en blanco bloquea el envío sin llamar a la API | Cubierto | sillones/Sillones.test.tsx:92 | Mensaje, `aria-invalid` y 0 escrituras |
| sillones-frontend | El administrador da de alta un sillón y la lista se refresca | Cubierto | sillones/Sillones.test.tsx:116 | `POST` `{nombre:"Box 3", descripcion:null}`; ≥2 `GET`; fila nueva; formulario vacío; Toast |
| sillones-frontend | Un nombre de sillón duplicado se muestra como alerta sin perder los datos | Cubierto | sillones/Sillones.test.tsx:142 | `409`: mensaje sin `detalle`; conserva nombre y descripción |
| sillones-frontend | El administrador edita un sillón y la lista se refresca | Cubierto | sillones/Sillones.test.tsx:162 | Precarga. `PUT /api/sillones/2` con nombre, descripción nueva y `activo: true`; ≥2 `GET`; muestra la descripción nueva |
| sillones-frontend | Editar un sillón inexistente muestra una alerta y refresca la lista | Cubierto | sillones/Sillones.test.tsx:192 | `404`: alerta sin `detalle`; ≥2 `GET` |
| sillones-frontend | El administrador da de baja un sillón y sigue visible como inactivo | Cubierto | sillones/Sillones.test.tsx:214 | Confirma en la fila. `PUT` con los datos actuales y `activo: false`; Inactivo; ≥2 `GET` |
| sillones-frontend | El administrador reactiva un sillón dado de baja | Cubierto | sillones/Sillones.test.tsx:242 | `PUT` con `activo: true`; Activo. No cuenta los `GET`, pero «Activo» solo puede venir de la recarga porque no hay actualización optimista |
| sillones-frontend | Un error al dar de baja un sillón se muestra como alerta | Cubierto | sillones/Sillones.test.tsx:268 | `422`: alerta genérica sin `detalle`; ≥2 `GET` |
| sillones-frontend | El administrador ve los controles de escritura de sillones | Cubierto | sillones/Sillones.test.tsx:292 | Formulario y, por fila, Editar + Reactivar o Dar de baja |
| sillones-frontend | Recepción y odontología consultan los sillones sin controles de escritura | Cubierto | sillones/Sillones.test.tsx:304 | Un `it.each` por rol con el nombre exacto: sin formulario, botones ni columna «Acciones»; 0 escrituras |
| sillones-frontend | La pantalla de sillones aparece en el menú para los tres roles | Cubierto | sillones/Sillones.test.tsx:319 | Recorre los tres roles y afirma en cada uno el link con `href="/sillones"` |
| sillones-frontend | Un 403 al escribir un sillón se muestra como permiso insuficiente | Cubierto | sillones/Sillones.test.tsx:334 | Alerta de permiso sin `detalle`; conserva lo ingresado; menú visible; un solo `/api/auth/me` |
| profesional-prestaciones-frontend | El panel muestra las prestaciones asociadas con su duración y la marca de inactiva | Cubierto | prestaciones/PanelPrestaciones.test.tsx:105 | «30 min» y «60 min»; Blanqueamiento «inactiva» y Consulta sin la marca; 1 `GET` de las asociadas |
| profesional-prestaciones-frontend | Un profesional sin prestaciones muestra un mensaje en el panel | Cubierto | prestaciones/PanelPrestaciones.test.tsx:119 | Mensaje y ningún `listitem` |
| profesional-prestaciones-frontend | Un fallo al consultar las prestaciones del profesional permite reintentar | Cubierto | prestaciones/PanelPrestaciones.test.tsx:127 | Con `500`: alerta. «Reintentar» trae Consulta; 2 `GET` |
| profesional-prestaciones-frontend | El administrador ve preseleccionadas las asociadas y solo las activas como nuevas opciones | Cubierto | prestaciones/PanelPrestaciones.test.tsx:168 | Consulta y Blanqueamiento marcadas; Blanqueamiento «inactiva»; Limpieza sin marcar; sin Ortodoncia; 3 casillas |
| profesional-prestaciones-frontend | El administrador guarda un nuevo conjunto de prestaciones | Cubierto | prestaciones/PanelPrestaciones.test.tsx:187 | Un solo `PUT` con [10, 11] sin repetidos; lista devuelta; Toast |
| profesional-prestaciones-frontend | Quitar todas las prestaciones envía una lista vacía | Cubierto | prestaciones/PanelPrestaciones.test.tsx:210 | `PUT` `{prestacionIds: []}`; mensaje de lista vacía |
| profesional-prestaciones-frontend | Cancelar la edición de prestaciones descarta los cambios sin escrituras | Cubierto | prestaciones/PanelPrestaciones.test.tsx:226 | Vuelve la selección inicial; la lista solo tiene Consulta; 0 escrituras |
| profesional-prestaciones-frontend | Guardar las prestaciones dos veces seguidas emite un solo reemplazo | Cubierto | prestaciones/PanelPrestaciones.test.tsx:246 | `PUT` demorado; botón deshabilitado; 1 `PUT` antes y después de resolver |
| profesional-prestaciones-frontend | Una prestación dada de baja mientras se editaba se muestra como alerta | Cubierto | prestaciones/PanelPrestaciones.test.tsx:297 | `422 PRESTACION_INACTIVA`: alerta sin `detalle`; 2 `GET` de asociadas y 2 de catálogo; selección conservada |
| profesional-prestaciones-frontend | Una prestación inexistente al guardar se muestra como alerta | Cubierto | prestaciones/PanelPrestaciones.test.tsx:306 | `422 PRESTACION_INEXISTENTE`: igual que el anterior |
| profesional-prestaciones-frontend | Guardar prestaciones de un profesional inexistente refresca la lista de profesionales | Cubierto | prestaciones/PanelPrestaciones.test.tsx:335 | `404`: alerta en la página; 2 `GET /api/profesionales`; lista vacía; la alerta sigue visible |
| profesional-prestaciones-frontend | Un conflicto al guardar prestaciones pide reintentar | Cubierto | prestaciones/PanelPrestaciones.test.tsx:315 | `409`: alerta «Volvé a intentarlo.» sin `detalle`; selección conservada (y recarga) |
| profesional-prestaciones-frontend | Un 403 al guardar prestaciones se muestra como permiso insuficiente | Cubierto | prestaciones/PanelPrestaciones.test.tsx:324 | Alerta de permiso sin `detalle`; menú visible; sin `logout` |
| profesional-prestaciones-frontend | Recepción consulta las prestaciones de un profesional sin controles de edición | Cubierto | prestaciones/PanelPrestaciones.test.tsx:141 | Consulta en la lista; sin casillas ni Guardar; 0 `GET` del catálogo; 0 escrituras |
| profesional-prestaciones-frontend | El odontólogo consulta sus propias prestaciones sin controles de edición | Cubierto | prestaciones/PanelPrestaciones.test.tsx:151 | Única fila visible; Consulta; sin controles; 0 `GET` del catálogo; 0 escrituras |

**Resultado:** **51/51 Cubiertos**, ningún Parcial y ningún Sin test.

Tests de apoyo que no corresponden a escenarios:
- `routes.test.ts:6` y `:21`: el agregador expone `/profesionales` y `/sillones` sin duplicados, con menú
  30/40 y los tres roles, y falla con un hijo sin `rutas`.
- `prestaciones/opciones.test.ts:9` y `:21`: prueban las funciones puras de opciones y selección.

Dos observaciones sin efecto en la cobertura:
- En los dos escenarios de listado vacío, «ninguna fila de datos» se afirma débil.
- En «reactiva un sillón», la recarga se infiere del resultado en lugar de contar los `GET`.

## Ejecución de tests

| Comando (directorio) | Resultado |
|---|---|
| `npx vitest run` (`frontend/`) | **25 archivos / 180 tests passed**, 0 fallidos, 0 saltados (11,95 s) |
| ↳ de este change | 56 tests: `Profesionales.test.tsx` 21, `Sillones.test.tsx` 16 (15 escenarios; `it.each` aporta 2), `PanelPrestaciones.test.tsx` 15, `routes.test.ts` 2 y `opciones.test.ts` 2, todos en verde. `estructura.test.ts` también pasa sin el `.gitkeep` |
| `npm run lint` (`eslint .`) | Exit 0, sin errores ni warnings |
| `npm run build` (`tsc -b && vite build`) | OK. 103 módulos; `dist/assets/index-*.js` de 202,61 kB (64,40 kB gzip) y CSS de 14,64 kB |
| `./mvnw -q verify` (`backend/`, con Docker encendido según `docker info`) | **EXIT 0**. Surefire: 23 suites / 109 tests. Failsafe (Testcontainers, PostgreSQL real): 45 suites / 342 tests. 0 fallos, 0 errores, 0 saltados |

Notas:
- El backend se corrió porque la skill lo pide cuando hay `pom.xml` y Docker estaba disponible. Este change
  **no toca `backend/`**, así que la corrida solo confirma que `master` sigue en verde con el árbol actual.
- Cuando terminó Maven no quedaron contenedores en `docker ps` ni procesos en segundo plano.
- En stderr de Vitest aparecen avisos de React Router v7 (*future flags*). Son preexistentes y no fallan.

## Reglas de negocio y reglas duras

**Reglas de negocio**

- **RN-11** (recursos activos y relación profesional-prestación), citada por la spec de prestaciones. La
  SPA no ofrece inactivas no asociadas (`opciones.ts`, test :168) y conserva o quita las inactivas ya
  asociadas. La validación definitiva queda en el backend (`422 PRESTACION_INACTIVA`, test :297). ✔
- **RN-37** (baja lógica). La baja es `PUT` con `activo:false`, sin `DELETE` ni borrado físico (test
  Profesionales :305, que afirma que no hay `DELETE`). La guarda de turnos futuros llega con C-30, fuera de
  alcance; la SPA muestra cualquier error de baja como alerta (tests :359 y Sillones :268). ✔
- **RN-20 / RN-33 / PA-03** (odontólogo solo lectura). `ODONTOLOGO` no ve controles de escritura en
  profesionales, sillones ni el panel, y no emite escrituras (tests :447, :456, Sillones :304 y Panel
  :151). ✔
- El change no tiene alcance sobre solapamientos (RN-01 y RN-02), concurrencia de reservas ni token del
  paciente (RN-07): no toca turnos ni la reserva pública.

**Reglas duras.** Se revisó `git status --porcelain --untracked-files=all` junto con `git diff master`,
porque los archivos del change no están commiteados.

- **Alcance**: el único cambio rastreado es la eliminación de `frontend/src/features/profesionales/.gitkeep`.
  Los demás son 28 archivos nuevos en `features/profesionales/**` y los artefactos del change. Ninguno está
  en `backend/` ni en `features/prestaciones`, y no hay imports hacia `features/prestaciones`. ✔
- **Datos ficticios**: «Ana Pérez», «Luis Gómez», matrículas `MN-1001`/`MN-1002`, «Sillón 1/2», «Box 3» y
  la cookie `XSRF-TOKEN=ficticio`. ✔
- **Sin secretos**: la búsqueda de `password|secret|token|apikey` solo encuentra la cookie de test. No se
  tocaron `.env`, `application*.yml` ni `package.json`, y no hay dependencias nuevas. ✔
- **Tokens del design-system**: `git grep` de `#hex|NNpx|rgb(|hsl(|font-family` no encuentra nada, y no hay
  `style=` en línea. `panel.css` usa 14 variables y todas existen en `design-system/tokens.css`: `--space-1…5`,
  `--color-border`, `--color-primary`, `--color-text-muted`, `--radius-md`, `--text-sm`,
  `--weight-semibold`, `--target-min`, `--focus-outline` y `--focus-offset`. El único valor literal es el
  keyword `border: thin`, que no es un color, un espaciado ni una fuente. ✔
- **TypeScript strict sin `any`**: no aparece `any` en la feature, y `tsc -b` pasa. Los componentes están en
  PascalCase (`PaginaProfesionales`, `FormularioSillon`, `PanelPrestaciones`, `AccionesFila`). ✔
- **Nombres exactos de escenario en `it(...)`**: 51/51. ✔

## Diferencias con design.md

No hay diferencias bloqueantes ni mayores. Todas son **menores**:

1. **Menor: columna «Detalle» para los roles de consulta.** La decisión 7 dice que la acción «Prestaciones»
   es visible para los tres roles, pero no nombra la columna. Para `ADMIN`, la acción va dentro de
   «Acciones», antes de Editar y Baja (prop `extra` de `AccionesFila`). Para `RECEPCION` y `ODONTOLOGO`,
   `PaginaProfesionales.tsx:91-94` la pone en otra columna, «Detalle». Así los tests de solo lectura pueden
   afirmar que no existe «Acciones». No contradice ninguna decisión, pero el nombre de la columna no está en
   el design.
2. **Menor: `PanelProfesionalProps.informarError`.** La tarea 12.4 y la decisión 8 piden que la página
   exponga un callback que refresque la lista ante el `404`. Además, se agregó `informarError`
   (`PaginaProfesionales.tsx:25-26` y `:169`) para que la alerta viva en la página y no se pierda cuando la
   recarga desmonta el panel. Es un detalle que va en la misma dirección que el design, y lo cubre el test
   Panel :335.
3. **Menor: envoltorio `PantallaProfesionales` en `profesionales/routes.ts`.** La decisión 7 dice que la
   página «abre `PanelPrestaciones`». En el código, `PaginaProfesionales` recibe el panel por la prop
   `panel` (etiqueta y componente), y lo conecta `PantallaProfesionales` en `profesionales/routes.ts:11-14`.
   Así la página no importa la subcarpeta `prestaciones/`. Es una inversión de dependencia que no cambia el
   comportamiento ni el contrato de rutas, y deja un punto de extensión útil para C-22.
4. **Menor: el guardado envía solo los ids que siguen siendo opción.** La decisión 7 dice que se envía
   «el conjunto completo». `idsSeleccionados` (`opciones.ts:32-37`) filtra la selección contra las opciones
   vigentes. Supongamos que, después de un `422 PRESTACION_INACTIVA`, la recarga deja de ofrecer una
   prestación elegida que no estaba asociada. El siguiente guardado la omite en silencio, aunque la
   selección interna la conserve. Es coherente con la regla «las inactivas no asociadas no se ofrecen» y
   evita repetir el `422`, pero la persona solo lo nota por la casilla que desapareció. Lo cubre
   `opciones.test.ts:21`.
5. **Menor (hallazgo adicional): el alta fallida no recarga la lista.** La decisión 5 dice «después de cada
   escritura, exitosa o fallida, se vuelve a consultar la lista». En `crear()` de
   `PaginaProfesionales.tsx:46-50` y `PaginaSillones.tsx:32-36`, un `POST` que falla, por ejemplo con `409`,
   no recarga; la edición y la baja sí lo hacen. Ningún escenario exige recargar tras un alta fallida, y el
   comportamiento coincide con C-20. Es una diferencia de detalle con el texto del design.

## UI / verificación visual

- `design-system/README.md` tiene `Mode: system-only`, `Revision: 1` y `Approval: approved`, igual que
  `visual_design.mode` en `.active-orchestrator-state.json`.
- La sección `## UI Mockups` de `design.md` dice `Decision: no (project policy)` y `Design system revision: 1`.
- Con la política `system-only` **solo se chequean los tokens**, y el chequeo resultó correcto (ver
  §Reglas duras). No hay mockup aprobado contra el cual comparar. Esta verificación no hizo capturas en el
  navegador.

## Qué NO se pudo verificar

1. **La prueba manual con `RECEPCION` y `ODONTOLOGO` contra el backend real (tarea 13.3, diferida).** No hay
   cuentas de esos roles hasta C-24. Ese comportamiento solo está cubierto por Vitest con `fetch` simulado.
2. **La prueba manual como `ADMIN`.** Según `tasks.md`, ya la hizo el usuario; esta verificación no la
   repitió ni tiene evidencia propia.
3. **La renderización real en el navegador y el layout mobile.** No se abrieron las pantallas ni se hicieron
   capturas. El aspecto visual se verificó solo de forma estática, por el uso de tokens.

## Próximo paso

- **Listo para archivar.** Commitear el change (el agente pregunta antes del commit), abrir el PR a `master`
  (lo revisa Juan Pablo) y, cuando esté mergeado, ejecutar `/opsx:archive c-21-front-profesionales-y-sillones`.
- Para revisar en el PR:
  - las 5 diferencias menores de §Diferencias con design.md, sobre todo la 4 (ids filtrados al guardar) y
    la 5 (sin recarga tras un alta fallida);
  - las dos afirmaciones débiles anotadas en la matriz.
- Después del merge de C-24: hacer la prueba manual de 13.3 con `RECEPCION` y `ODONTOLOGO`, y marcarla.

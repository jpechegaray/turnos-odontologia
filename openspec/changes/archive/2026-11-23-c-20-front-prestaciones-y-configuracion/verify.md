# Verify — c-20-front-prestaciones-y-configuracion (2026-11-23)

**Veredicto:** LISTO PARA ARCHIVAR

> Las 17 tareas están hechas, los 37 escenarios de las dos delta specs están cubiertos por tests que
> afirman comportamiento real, test/lint/build están verdes y la tarea **12.2** (E2E contra el backend
> real de C-04 y C-08) se ejecutó después de la verificación inicial con **18/18 comprobaciones en
> verde** en un navegador real (ver §"E2E en navegador real"). Alcance declarado: **solo frontend, cero
> archivos de `backend/` modificados** (confirmado con `git diff`/`git ls-files`).

## Validación estructural

- `openspec validate c-20-front-prestaciones-y-configuracion --strict` → **válido** (exit 0).
- Presentes: `proposal.md`, `design.md`, `tasks.md`, `specs/prestaciones-frontend/spec.md`,
  `specs/configuracion-frontend/spec.md`, `.openspec.yaml`.
- Delta specs: `prestaciones-frontend` (6 requirements / 21 escenarios), `configuracion-frontend`
  (5 requirements / 16 escenarios). Total **37 escenarios**.

## Tareas

- `tasks.md`: **17/17 completas**.
- 12.1 (`npm test` / `npm run lint` / `npm run build`) y 12.3 (`git diff` sin `backend/`) fueron
  re-ejecutadas en esta verificación (ver abajo).
- 12.2 (E2E contra el backend real) se ejecutó **después** de la verificación inicial: ver
  §"E2E en navegador real (tarea 12.2)". La verificación inicial la había dejado como bloqueante
  porque en ese momento Docker/PostgreSQL no estaban disponibles.

## Matriz escenario → test

Convención del equipo respetada: el `it(...)` de Vitest lleva el texto exacto del escenario.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| prestaciones-frontend | El catálogo lista las prestaciones con su duración y estado | Cubierto | Prestaciones.test.tsx:46 | Afirma nombre, duración y estado; el orden alfabético se implementa en `useCatalogoPrestaciones` (`localeCompare "es"`), no se asserta explícitamente |
| prestaciones-frontend | El catálogo vacío muestra un mensaje | Cubierto | Prestaciones.test.tsx:62 | Assert mensaje vacío y ausencia de filas |
| prestaciones-frontend | Un fallo al listar permite reintentar | Cubierto | Prestaciones.test.tsx:72 | 500 → alerta; «Reintentar» recupera lista |
| prestaciones-frontend | Una duración que no es múltiplo de 5 bloquea el envío sin llamar a la API | Cubierto | Prestaciones.test.tsx:87 | 7 → mensaje RN-09, cero escrituras |
| prestaciones-frontend | Una duración menor a 5 bloquea el envío sin llamar a la API | Cubierto | Prestaciones.test.tsx:99 | 0 → RN-09, cero escrituras |
| prestaciones-frontend | Una duración mayor a 480 bloquea el envío sin llamar a la API | Cubierto | Prestaciones.test.tsx:111 | 481 → RN-09, cero escrituras |
| prestaciones-frontend | Una duración no entera bloquea el envío sin llamar a la API | Cubierto | Prestaciones.test.tsx:123 | 7,5 → RN-09, cero escrituras |
| prestaciones-frontend | Un nombre vacío bloquea el envío sin llamar a la API | Cubierto | Prestaciones.test.tsx:135 | Mensaje obligatorio, cero escrituras |
| prestaciones-frontend | Las duraciones límite 5 y 480 se aceptan | Cubierto | Prestaciones.test.tsx:147 | Dos `POST` con 5 y 480, filas nuevas |
| prestaciones-frontend | El administrador da de alta una prestación y la lista se refresca | Cubierto | Prestaciones.test.tsx:173 | `POST` con body correcto + `GET` posterior (≥2) |
| prestaciones-frontend | Un nombre duplicado se muestra como alerta sin perder los datos | Cubierto | Prestaciones.test.tsx:201 | 409 `PRESTACION-DUPLICADA`; conserva valores; no muestra `detalle`; 1 solo GET |
| prestaciones-frontend | Un 422 de duración del backend se muestra como alerta | Cubierto | Prestaciones.test.tsx:220 | 422 `RN-09`; mensaje RN-09; no refresca (1 GET) |
| prestaciones-frontend | El administrador edita una prestación y la lista se refresca | Cubierto | Prestaciones.test.tsx:238 | `PUT /api/prestaciones/1` con body; GET ≥2 |
| prestaciones-frontend | El administrador reactiva una prestación dada de baja | Cubierto | Prestaciones.test.tsx:275 | `PUT` con `activa: true`; queda activa |
| prestaciones-frontend | Editar una prestación inexistente muestra una alerta y refresca la lista | Cubierto | Prestaciones.test.tsx:305 | 404; alerta; GET ≥2 |
| prestaciones-frontend | El administrador da de baja una prestación y sigue visible como inactiva | Cubierto | Prestaciones.test.tsx:329 | `DELETE` + confirmación en fila; estado Inactiva; GET ≥2 |
| prestaciones-frontend | Dar de baja una prestación inexistente muestra una alerta | Cubierto | Prestaciones.test.tsx:350 | 404; alerta; GET ≥2 |
| prestaciones-frontend | El administrador ve los controles de alta, edición y baja | Cubierto | Prestaciones.test.tsx:371 | Ruta real; «Agregar prestación» + 2 Editar + 2 Dar de baja |
| prestaciones-frontend | Recepción consulta el catálogo sin controles de escritura | Cubierto | Prestaciones.test.tsx:389 | Sin botones de escritura; cero escrituras |
| prestaciones-frontend | El odontólogo consulta el catálogo sin controles de escritura | Cubierto | Prestaciones.test.tsx:400 | Sin botones de escritura; cero escrituras |
| prestaciones-frontend | La pantalla de prestaciones aparece en el menú para los tres roles | Cubierto | Prestaciones.test.tsx:411 | Loop con assert por rol: link `href="/prestaciones"` |
| configuracion-frontend | La configuración se precarga con las siete claves vigentes | Cubierto | Configuracion.test.tsx:67 | Las 7 claves con sus valores |
| configuracion-frontend | Un fallo al consultar la configuración permite reintentar | Cubierto | Configuracion.test.tsx:79 | 500 → alerta; «Reintentar» recarga |
| configuracion-frontend | La pantalla no envía escrituras al cargar | Cubierto | Configuracion.test.tsx:94 | Solo un GET; cero escrituras |
| configuracion-frontend | Un plazo de cancelación fuera de rango bloquea el guardado sin llamar a la API | Cubierto | Configuracion.test.tsx:105 | 169 → mensaje RN-36; cero escrituras |
| configuracion-frontend | Una anticipación de recordatorio fuera de rango bloquea el guardado sin llamar a la API | Cubierto | Configuracion.test.tsx:118 | 0 → RN-36; cero escrituras |
| configuracion-frontend | Un límite positivo en cero bloquea el guardado sin llamar a la API | Cubierto | Configuracion.test.tsx:131 | `horizonte_dias`=0 → RN-36; cero escrituras |
| configuracion-frontend | Un valor no entero bloquea el guardado sin llamar a la API | Cubierto | Configuracion.test.tsx:144 | 2,5 → RN-36; cero escrituras |
| configuracion-frontend | Un nombre de consultorio en blanco bloquea el guardado sin llamar a la API | Cubierto | Configuracion.test.tsx:157 | Vacío → RN-36; cero escrituras |
| configuracion-frontend | Los valores límite permitidos se aceptan | Cubierto | Configuracion.test.tsx:170 | `PUT` con exactamente las 7 claves y límites 0/168; la confirmación se cubre en el test de guardado |
| configuracion-frontend | El administrador guarda las siete claves y ve la confirmación | Cubierto | Configuracion.test.tsx:194 | `PUT` con keys exactas; Toast «Configuración guardada.» |
| configuracion-frontend | Un 422 RN-36 del backend se muestra como alerta sin perder los valores ingresados | Cubierto | Configuracion.test.tsx:212 | 422 `RN-36`; alerta; conserva valores; sin `detalle` |
| configuracion-frontend | Un 400 del backend se muestra como error de operación | Cubierto | Configuracion.test.tsx:230 | Mensaje de operación distinto del de valor inválido; conserva valores |
| configuracion-frontend | El administrador puede editar y guardar | Cubierto | Configuracion.test.tsx:249 | Campos habilitados (sin `readonly`) + botón guardar visible |
| configuracion-frontend | Recepción consulta la configuración en solo lectura y no envía PUT | Cubierto | Configuracion.test.tsx:261 | 7 campos `readonly`; sin botón; cero escrituras |
| configuracion-frontend | El odontólogo no puede abrir la configuración | Cubierto | Configuracion.test.tsx:275 | «No tenés permiso»; sin `GET /api/configuracion`; sin campos |
| configuracion-frontend | La pantalla de configuración aparece en el menú solo para ADMIN y RECEPCION | Cubierto | Configuracion.test.tsx:286 | Loop con assert por rol: link para ADMIN/RECEPCION, ausente para ODONTOLOGO |

**Resultado:** 37/37 escenarios **Cubiertos**; 0 Parciales; 0 Sin test. No se detectaron tests
tautológicos ni de solo tipo: todos afirman elementos renderizados o llamadas a `fetch` con
`expect(...)`, incluidos los tests de menú, que hacen un `assert` real dentro del loop por rol.

## Ejecución de tests

Comandos ejecutados en `frontend/` (workdir = `frontend`):

| Comando | Resultado |
|---|---|
| `npm test -- --run` | **20 archivos / 124 tests passed**, 0 fallidos, 0 saltados (≈20 s) |
| `npm run lint` (`eslint .`) | Sin errores ni warnings |
| `npm run build` (`tsc -b && vite build`) | OK; 82 módulos; `dist` generado (188,33 kB JS / 61,11 kB gzip) |

Cifras reales verificadas en esta corrida (no se confió en el número esperado de la consigna).

### Backend — no ejecutado (no aplica)

`mvn verify` / Testcontainers **no se ejecutaron** porque:

1. Este change **no modifica ningún archivo de `backend/`** (`git diff --name-only` y
   `git ls-files --others --exclude-standard` no devuelven ninguna ruta `backend/`).
2. Aun así, los tests de persistencia del backend requieren PostgreSQL real vía Testcontainers, y
   **Docker no está corriendo ni instalado en este entorno**.

Esto **no se presenta como pasado**: es "no ejecutado — no aplica (sin cambios de backend) + Docker
indisponible". No hay forma de que este change rompa tests de backend dado que no toca esa capa.

## E2E en navegador real (tarea 12.2)

Ejecutado **después** de la verificación inicial, en el mismo equipo, con:

- PostgreSQL 16 en Docker (`docker compose up -d postgres`, contenedor `healthy`).
- Backend de C-04/C-08 con perfil `dev` (`mvnw spring-boot:run`) en `http://localhost:8080`, con las
  APIs `/api/prestaciones` y `/api/configuracion` ya mergeadas.
- Frontend Vite (`npm run dev`) en `http://localhost:5173` con el proxy `/api` → `8080`.
- Navegador real: **Microsoft Edge** (canal `msedge`) vía `playwright-core`, headless, viewport
  1440x900.

**Datos de prueba:** todos ficticios, creados por la API con la sesión de ADMIN de desarrollo:
`recepcion@consultorio.test` (RECEPCION), `odontologo@consultorio.test` (ODONTOLOGO, vinculado al
profesional ficticio «Ana Ficticia», matrícula `MP-0001`) y `admin.e2e@consultorio.test` (ADMIN, usado
para no depender de la clave del seed). Prestaciones creadas: `Limpieza E2E <timestamp>`, 30 min.
`cancelacion.plazo_horas` se cambió **24 → 42** y se dejó persistido en 42. No se registraron
credenciales ni cookies en la evidencia.

| # | Comprobación | Resultado |
|---|---|---|
| 1 | Sin sesión, `/prestaciones` redirige a `/login` | PASS |
| 2 | ADMIN inicia sesión | PASS |
| 3 | ADMIN ve el botón de alta en `/prestaciones` | PASS |
| 4 | ADMIN crea una prestación y la lista la muestra (Toast «Prestación agregada.») | PASS |
| 5 | ADMIN ve el control «Editar» por fila | PASS |
| 6 | ADMIN ve el control «Dar de baja» por fila | PASS |
| 7 | `/configuracion` precarga las **7 claves** | PASS |
| 8 | ADMIN cambia `cancelacion.plazo_horas` (→ 42) y ve «Configuración guardada.» | PASS |
| 9 | El valor 42 **persiste** tras recargar (lo devuelve el backend) | PASS |
| 10 | RECEPCION inicia sesión | PASS |
| 11 | RECEPCION **no ve** controles de escritura en `/prestaciones` | PASS |
| 12 | RECEPCION **no ve** el formulario de alta | PASS |
| 13 | RECEPCION ve los campos de configuración en **solo lectura** (`readonly`) | PASS |
| 14 | RECEPCION **no ve** el botón «Guardar configuración» | PASS |
| 15 | ODONTOLOGO inicia sesión | PASS |
| 16 | ODONTOLOGO ve «No tenés permiso» en `/configuracion` | PASS |
| 17 | ODONTOLOGO **no emite** `GET /api/configuracion` (verificado en la pestaña Red) | PASS |
| 18 | ODONTOLOGO consulta `/prestaciones` sin controles de escritura | PASS |

**Resumen: 18 checks — 18 PASS, 0 FAIL.** Capturas (fuera del repo, en el directorio temporal de la
sesión): `admin-prestaciones.png`, `admin-configuracion.png`, `recepcion-prestaciones.png`,
`recepcion-configuracion.png`, `odontologo-configuracion.png`. Las capturas confirman: la tabla con
duración en mono y estado, el formulario de alta solo para ADMIN, el Toast de confirmación, los siete
campos de configuración con sus etiquetas en español, la ausencia del botón de guardado para RECEPCION
y el aviso de permiso para ODONTOLOGO.

**Limitaciones de esta corrida:** solo viewport desktop 1440x900 (las pantallas son internas de
administración; no se probó mobile), y el E2E se ejecutó contra la base de desarrollo local, no contra
un entorno desplegado.

## Reglas de negocio y reglas duras

**Reglas de negocio citadas por las specs**

- **RN-09** (duración entera, múltiplo de 5, entre 5 y 480): validada en `prestaciones/validacion.ts`
  con `enteroEstricto` + rango, mensaje exacto; cubierta por 5 escenarios de bloqueo + límites + el
  mapeo del `422 RN-09` del backend. ✔
- **RN-36** (rangos de configuración): validada por campo en `configuracion/validacion.ts` con los
  rangos de `claves.ts`; cubierta por 5 escenarios de bloqueo + límites + mapeo del `422 RN-36`. ✔
- **RN-37** (baja lógica: sigue visible como inactiva): el `DELETE` refresca la lista y la fila queda
  «Inactiva»; cubierto por su escenario. ✔
- **RN-20 / PA-03** (odontólogo solo lectura): `ODONTOLOGO` ve prestaciones sin controles de
  escritura y no accede a `/configuracion`; cubierto por escenarios de menú y de bloqueo. ✔

No hay reglas RN de solapamiento, concurrencia ni token de paciente en el alcance de este change
(no toca creación de turnos ni reserva pública).

**Reglas duras del proyecto**

- **TypeScript strict sin `any`**: sin `: any`, `as any`, `<any>`, `any[]` en los archivos del change.
  `npm run build` con `tsc -b` pasa. ✔
- **Componentes en PascalCase**: `PaginaPrestaciones.tsx`, `FormularioPrestacion.tsx`,
  `AccionesFila.tsx`, `PaginaConfiguracion.tsx`, `FormularioConfiguracion.tsx`. ✔
- **Tokens, sin valores fijos**: sin colores hex/`rgb`/`hsl`, sin `font-size`/`margin`/`padding`
  inline; se usan `Campo`, `Tabla`, `Boton`, `Alerta`, `Toast`, `Tarjeta` del kit y
  `numerica`/`className="mono"` provistos por el diseño. ✔
- **Tests con el nombre exacto del escenario**: 37/37 hacen match verbatim. ✔
- **Sin datos reales de pacientes**: fixtures ficticios («Limpieza», «Consultorio Ficticio»,
  `personal@example.test`). ✔
- **Sin secretos**: solo la cookie de test `XSRF-TOKEN=ficticio`; `package.json` sin cambios (sin
  dependencias nuevas; imports solo de `react`, `react-router-dom`, `vitest`,
  `@testing-library/*`, `shared/*`, `app/*`, `features/auth`). ✔
- **No tocar backend**: cero rutas `backend/` en `git diff`/untracked. ✔

## Diferencias con design.md

Sin diferencias bloqueantes ni mayores. Observaciones menores:

- **Menor — confirmación de guardado de configuración.** El `design.md` (decisión 6) decide anunciar
  los éxitos con `Toast`; el escenario pide «mostrar la confirmación del backend». La implementación
  adopta la respuesta del `PUT` (`actualizarValores(aTexto(resultado))`) y muestra un `Toast` local
  «Configuración guardada.». Es consistente con el design; conviene tener presente que el texto del
  toast es local y no un campo del servidor.
- **Menor — mapping de error de configuración.** `configuracion/errores.ts` exige
  `estado === 422 && codigo === "RN-36"` (más estricto que «`RN-36` → mensaje» del design). Coincide
  con los escenarios de spec; no rompe nada.
- **Menor — orden del catálogo.** El design y la spec piden orden por nombre; se implementa en el hook
  (`localeCompare "es"`) pero ningún test lo asserta. Comportamiento correcto, cobertura no explícita.

## UI / verificación visual

- `design-system/README.md` → `Mode: system-only`, `Revision: 1`, `Approval: approved`.
- `design.md` → `## UI Mockups`: `Decision: no (project policy)`, `Revision: 1`.
- **Visual verify: no aplica (system-only).** Por política del proyecto no hay gate de mockup por
  change y no se reclama una coincidencia con mockup (no existe mockup aprobado para este change).
- **Corrida real en navegador:** sí, en Edge headless 1440x900 durante la tarea 12.2 (§"E2E en navegador
  real"). Se inspeccionaron las cinco capturas: los colores, tipografía, espaciados y componentes
  provienen del `design-system` aprobado; no se detectaron valores visuales fuera de token ni defectos
  evidentes. No se verificó el layout mobile.
- Verificación de markup accesible **sí** hecha vía los tests existentes: roles (`cell`, `row`,
  `button`, `alert`, `navigation`, `status`), etiquetas (`getByLabelText`) y `readonly` de los campos
  de solo lectura. Todo verde.

## Qué NO se verificó

1. **Tests de backend (`mvn verify`):** no aplican (sin cambios de backend) y además Testcontainers
   requiere Docker, que no estaba disponible al momento de la verificación inicial.
2. **Layout mobile:** el E2E de 12.2 corrió solo en 1440x900. Las pantallas son de administración
   interna y no hay requisito mobile explícito en las specs, pero no se verificó.
3. **Entorno desplegado:** el E2E usó la base de desarrollo local, no un entorno productivo.

## Pendientes

### 1. Archivos preexistentes ajenos al change (no commitear por accidente)

Ya estaban modificados/sin trackear **antes** de C-20; **no atribuirlos** a este change:

- `frontend/package-lock.json` — modificado (preexistente).
- `package-lock.json` (raíz del repo) — sin trackear (preexistente).

Al preparar el PR, revisar el diff para no incluir estos archivos si no corresponden al change.
`frontend/package.json` **no** fue modificado (no hay dependencias nuevas).

### 2. Datos ficticios creados en la base de desarrollo

La tarea 12.2 dejó en la base local: los usuarios `recepcion@consultorio.test`,
`odontologo@consultorio.test` y `admin.e2e@consultorio.test`; el profesional «Ana Ficticia»
(`MP-0001`); algunas prestaciones «Limpieza E2E …»; y `cancelacion.plazo_horas = 42`. Son datos
ficticios de desarrollo; si se quiere la base como estaba, hay que revertirlos a mano.

## Próximo paso

- **Listo para archivar.** Abrir el PR (lo revisa Juan Pablo) y, una vez mergeado, ejecutar
  `/opsx:archive c-20-front-prestaciones-y-configuracion`.
- Las observaciones menores de la sección "Diferencias con design.md" se listan para revisión en el PR;
  no bloquean.

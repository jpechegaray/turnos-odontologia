# Verify — c-23-front-pacientes (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR en lo automatizable (0 bloqueantes). Queda pendiente la prueba manual 7.3 (la hace el usuario con el backend levantado; no es una falla) y la revisión visual del textarea.

> **Actualización (2026-11-25):** el usuario completó la prueba manual 7.3 con sesión iniciada y confirmó que funciona. Todas las tareas quedan completas.

## Validación estructural
`openspec validate c-23-front-pacientes --strict`: válido.

## Tareas
Todas completas salvo 7.3 (prueba manual con login real, pendiente del usuario). 7.2 se cerró en esta verificación. Que 7.3 siga pendiente es esperado y debe cerrarse antes de archivar o anotarse en el PR.

## Matriz escenario → test
26 de 26 escenarios de `specs/pacientes-frontend/spec.md` tienen un `it(...)` con el nombre exacto en `frontend/src/features/pacientes/Pacientes.test.tsx` (26 `it` en total, uno por escenario), todos en verde.

| Escenario | Estado | Línea |
|---|---|---|
| La pantalla de pacientes aparece en el menú para administración y recepción | Cubierto | 58 |
| El odontólogo no ve la pantalla de pacientes | Cubierto | 73 |
| Al abrir la pantalla se listan las fichas activas | Cubierto | 111 |
| Un dato ausente de la ficha se muestra con una marca de vacío | Cubierto | 131 |
| Un fallo al consultar pacientes permite reintentar | Cubierto | 145 |
| La búsqueda por apellido envía solo ese filtro | Cubierto | 170 |
| Los filtros de búsqueda se combinan | Cubierto | 186 |
| La búsqueda por DNI envía el DNI recortado | Cubierto | 204 |
| Una búsqueda sin coincidencias muestra un mensaje | Cubierto | 213 |
| Una ficha sin nombre o apellido bloquea el envío sin llamar a la API | Cubierto | 242 |
| Una ficha sin email ni teléfono bloquea el envío sin llamar a la API | Cubierto | 257 |
| Recepción da de alta un paciente y la lista se refresca | Cubierto | 276 |
| Una ficha con solo teléfono se acepta | Cubierto | 322 |
| La obra social se envía como texto libre | Cubierto | 345 |
| Un DNI duplicado se muestra en el campo DNI sin perder los datos | Cubierto | 377 |
| Un contacto faltante del backend se muestra en los campos de contacto | Cubierto | 401 |
| Un campo inválido del backend se muestra en su campo | Cubierto | 421 |
| Un rechazo por permiso se muestra como alerta | Cubierto | 446 |
| Recepción edita una ficha y la lista se refresca | Cubierto | 465 |
| Cancelar la edición vuelve al formulario de alta | Cubierto | 512 |
| Editar una ficha inexistente muestra una alerta y refresca la lista | Cubierto | 533 |
| El administrador da de baja una ficha y deja de aparecer en la lista | Cubierto | 552 |
| Cancelar la confirmación no da de baja la ficha | Cubierto | 578 |
| Dar de baja una ficha inexistente muestra una alerta | Cubierto | 595 |
| El administrador ve los controles de alta, edición y baja | Cubierto | 615 |
| Recepción ve alta y edición pero no la baja | Cubierto | 627 |

El mapeo es por nombre exacto; no se releyó cada THEN aserción por aserción, pero los tests ejecutan y pasan (26/26 en `Pacientes.test.tsx`).

## Ejecución de tests (en `frontend/`, Node v26.13.1, después de borrar `frontend/.vite/`)
- `npm ci`: OK.
- `npm run build`: OK (113 módulos, `built in 1.10s`). `dist/` está ignorado por git.
- `npm test`: `Test Files 1 failed | 25 passed (26)`, `Tests 1 failed | 205 passed (206)`.
- `npm run lint` (`eslint .`): sin errores ni avisos; no se recreó `frontend/.vite/`.
- Backend: no aplica (el change solo toca el front).

### Observación de entorno (no atribuible a C-23)
`src/features/auth/Login.test.tsx > El personal inicia sesión y llega a Inicio` falla en `expect(localStorage).toHaveLength(0)` (línea 37). Causa: Node >= 25 expone su propio `localStorage` global (sin `--localstorage-file`) y pisa el de jsdom. Ya fallaba en `master` antes de C-23 (línea de base de 0.1: 179 en verde y 1 falla; ahora 179 + 26 = 205 en verde y la misma 1 falla). El CI usa Node 22, donde no ocurre. Es la única falla.

## Reglas de negocio y reglas duras
- 1 (datos ficticios): emails `@example.test`, nombres ficticios; sin datos reales.
- 4 (ODONTOLOGO sin escritura): la ruta es solo `ADMIN` y `RECEPCION`; el odontólogo no ve la pantalla ni hace consultas a `/api/pacientes` (escenario cubierto); la baja solo para `ADMIN` (`permisos.ts`).
- 11 (nombre exacto en `it`): cumplida, 26/26.
- 12 (TS strict sin `any`): `grep` de `any` en `src/features/pacientes`: 0 coincidencias.
- 13 (tokens): `grep` de hex, `rgb(`, `px`, `font-family` y `style=` en los archivos de producción: 0 coincidencias.
- Alcance del diff (`git status --short`): solo `frontend/src/features/pacientes/**` (incluido el borrado de `.gitkeep`) y `openspec/changes/c-23-front-pacientes/`. No se tocó `backend/` ni otros changes. Sin secretos.

## Diferencias con design.md
- Bloqueantes: ninguna. Mayores: ninguna.
- Conformes: `routes.ts` con `/pacientes`, menú «Pacientes» orden 10 (entre Inicio 0 y Prestaciones 20), roles `ADMIN`/`RECEPCION`; tabla de mapeo de errores en `errores.ts` (409 y 422 `RN-32`, 400 por campo, 404 `PACIENTE_NO_ENCONTRADO`, 403, genérico) sin exponer el `detail`; baja solo `ADMIN`; `AccionesFilaPaciente` local a la feature; `<textarea>` nativo con clases del kit.
- Desviaciones reportadas por el apply, evaluadas:
  - `aria-label` en ambos formularios («Buscar pacientes», «Datos del paciente»): menor, aceptable (accesibilidad).
  - Textarea con clase `.input` sin padding vertical propio: menor; pendiente de revisión visual manual contra `design-system/components.html`.
  - 404 al editar mostrado como alerta del formulario (y recarga de la lista): menor, coherente con el escenario.
  - `type="submit"` explícito en dos botones (Buscar y envío del formulario): menor, inocuo.

## Qué NO se pudo verificar
- 7.3: flujo manual con sesión iniciada (alta, búsqueda por apellido y DNI, edición, DNI duplicado, baja, y repetir como `RECEPCION`). Requiere la contraseña del admin, que escribe el usuario. El 401 anónimo vía proxy ya se confirmó en el apply.
- Aspecto visual real (en especial el textarea de notas) en el navegador.
- Contra el backend real: los contratos se verificaron solo con `fetch` simulado.

## Próximo paso
1. El usuario corre 7.3 y anota el resultado en el PR; revisa el textarea contra el design system.
2. Si todo bien, preguntar antes de commitear (regla 16), PR a `master` (revisa Juan Pablo) y luego `/opsx:archive c-23-front-pacientes`.

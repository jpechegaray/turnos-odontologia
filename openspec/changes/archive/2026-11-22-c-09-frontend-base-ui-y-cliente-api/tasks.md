# Tasks

## 1. Fechas e intervalos

- [x] 1.1 Implementar por TDD `shared/fecha/zonaHoraria.ts` (formateo y conversión de hora local a UTC sin desfase fijo); verificar con Vitest los escenarios de presentación y conversión de `fechas-frontend`, también con `process.env.TZ` forzado a otra zona.
- [x] 1.2 Implementar por TDD `shared/fecha/intervalo.ts`; verificar los escenarios de intervalos semiabiertos (contiguos, un minuto, contenido, fin excluido, intervalo inválido).

## 2. Cliente de la API

- [x] 2.1 Implementar por TDD `shared/api/errorApi.ts` (mapeo de Problem Details por estado); verificar los escenarios de errores tipados de `cliente-api-frontend`, incluidos el cuerpo no JSON y el fallo de red.
- [x] 2.2 Implementar por TDD `shared/api/csrf.ts` y `shared/api/clienteApi.ts` sobre `obtenerApiBaseUrl()`; verificar con `fetch` simulado los escenarios de sesión, 204 y CSRF (cabecera vigente, obtención previa sin cookie, lecturas sin cabecera), sin modificar `apiBaseUrl.ts`.

## 3. Kit UI

- [x] 3.1 Crear `shared/ui/ui.css` desde `components.html` reemplazando los colores fijos por tokens, junto con el test que recorre `shared/ui/*.css` y `app/*.css`; verificar el escenario "El kit UI no usa colores fuera de tokens.css".
- [x] 3.2 Implementar por TDD `Boton`, `Campo`, `BadgeEstadoTurno`, `Tabla`, `Toast`, `Tarjeta`, `Alerta` e `Icono`, exportados en `shared/ui/index.ts`; verificar con Testing Library cada escenario de `kit-ui`.

## 4. Rutas, menú y layout

- [x] 4.1 Implementar por TDD `app/rutas/tipos.ts` y `app/rutas/registro.ts`, con el fixture `__fixtures__/features/ejemplo/routes.ts`; verificar los escenarios de registro y de menú (ejemplo en router y menú, duplicados, orden y ruta sin menú).
- [x] 4.2 Implementar `AppRutas`, `Layout`, `PaginaNoEncontrada`, `app/layout.css`, `app/rutas/modulos.ts` y la feature `inicio`, y montar el router en `App.tsx`; verificar con `MemoryRouter` los escenarios del layout y de la ruta inexistente, y que los tests existentes de `App.test.tsx` siguen pasando sin cambios.

## 5. Verificación de integración

- [x] 5.1 Ejecutar `npm run lint`, `npm test` y `npm run build` en `frontend/`; comprobar que pasan los tests de C-02 y de C-09, y que `git status` solo muestra archivos nuevos más `frontend/src/App.tsx` modificado.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Abrir el PR `C-09 frontend-base-ui-y-cliente-api` de `valentino` a `master` (lo revisa Juan Pablo). Preguntar antes de cada commit.
- Después del merge: `/opsx:archive`.

# Verify — c-02-esqueleto-frontend (2026-11-21)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes)

## Validación estructural
`openspec validate c-02-esqueleto-frontend --strict` -> "Change is valid".

## Tareas
Todas las tareas están completas: 1.1 a 9.5 en `[x]`, incluida 9.1 (cerrada en esta verificación). Sin pendientes ni diferidas.

## Matriz escenario -> test
| Spec | Escenario | Estado | Test / comando | Nota |
|---|---|---|---|---|
| entorno-frontend | La instalación limpia, el build y los tests terminan en verde | Cubierto (comando) | `npm ci && npm run build && npm test` en copia aislada sin `.env` | Exit 0 los tres; vitest run sin watch |
| entorno-frontend | La configuración de TypeScript está en modo estricto | Cubierto (comando) | `tsc -p tsconfig.app.json --showConfig` -> `"strict": true` | |
| entorno-frontend | El lint rechaza el uso explícito de any | Cubierto | `src/app/chequeoEstatico.test.ts:13` | Afirma ruleId `no-explicit-any` y severity 2; triangula con caso sin any |
| entorno-frontend | El lint pasa sin errores sobre el código del frontend | Cubierto (comando) | `npm run lint` -> exit 0 | |
| entorno-frontend | La App muestra el nombre de la marca tomado de BRAND | Cubierto | `src/App.test.tsx:12` | |
| entorno-frontend | La App refleja un cambio del nombre en BRAND | Cubierto | `src/App.test.tsx:17` | Verifica también que no aparece el provisional |
| entorno-frontend | Los estilos globales usan solo variables definidas en tokens.css | Cubierto | `src/app/estilosGlobales.test.ts:39` | Incluye caso del detector con CSS ficticio |
| entorno-frontend | El build incluye los tokens del design system | Cubierto (comando) | grep en `dist/assets/index-BFABl13Y.css`: `--color-primary` y `--font-sans` presentes | |
| entorno-frontend | El servidor de desarrollo sirve la SPA con los tokens aplicados | Manual OK (apply) | Dev server: SPA servida, estilos computados de `body` coinciden con tokens | No re-ejecutado |
| entorno-frontend | Las peticiones a /api se redirigen al backend en desarrollo | Cubierto | `src/app/entornoDesarrollo.test.ts:6` | |
| entorno-frontend | Una petición a /api desde el servidor de desarrollo llega al backend | Manual OK (apply) | `curl http://localhost:5173/api/inexistente` -> 401 application/problem+json de Spring | No re-ejecutado |
| entorno-frontend | La base de la API es /api por defecto | Cubierto | `src/shared/api/apiBaseUrl.test.ts:20` | Incluye vacío y undefined |
| entorno-frontend | La base de la API toma el valor de VITE_API_BASE_URL | Cubierto | `src/shared/api/apiBaseUrl.test.ts:28` | |
| entorno-frontend | La estructura de carpetas sigue la arquitectura del frontend | Cubierto | `src/app/estructura.test.ts:23` | |

Cobertura: 14/14 (8 por test `it(...)` con nombre exacto, 3 por comando, 2 manuales ya realizados en apply y 1 por comando de instalación limpia). Sin escenarios Parciales ni Sin test.

## Ejecución de tests
- `npm run build` (real `frontend/`): `tsc -b && vite build` OK, 33 módulos, `dist/` generado.
- `npm test`: Test Files 6 passed (6), Tests 8 passed (8), 0 fallidos, 0 saltados.
- `npm run lint`: exit 0, sin mensajes.
- 9.1 en copia aislada sin `.env`: `npm ci`, build y test en verde (mismos 6 archivos / 8 tests). Copia borrada.

## Reglas de negocio y reglas duras
- RN-XX: el change no cita reglas de negocio (andamiaje de entorno); no aplica.
- Regla 1 (datos reales): solo nombres ficticios (`Consultorio Ficticio de Prueba`, `/api-prueba`). OK.
- Regla 2 (secretos): no hay `.env` ni credenciales en `frontend/`; la copia limpia no necesitó ninguno. OK.
- Regla 11 (`@DisplayName`/`it` con nombre exacto): los 8 `it(...)` coinciden literalmente con los escenarios. OK.
- Regla 12: `strict: true`; grep de `any` en `src` solo encuentra el texto del fixture del test que verifica el rechazo de `any` (cadena, no tipo); componentes en PascalCase (`App`). OK.
- Regla 13: grep de hex, `rgb()`/`hsl()`, valores `px` y `font-family` en `src` (sin tests) solo halla `font-family: var(--font-sans)`. OK.
- `git check-ignore`: `frontend/node_modules` y `frontend/dist` ignorados. `git status`: cambios solo en `frontend/`, `README.md`, el change y el `docs/discovery/discovery.md` previo no relacionado. OK.

## Diferencias con design.md
Ninguna cambia el comportamiento especificado; todas se juzgan aceptables (menor):
1. Test del proxy con `// @vitest-environment node`; `setupTests` importa `brand.js` dinámicamente protegido por `typeof window`; archivo extra `src/design-system.d.ts`. Menor: necesario porque el entorno node no tiene `window`.
2. CSS leído con `node:fs` en vez de `?raw` porque Vitest 1.6 deja el CSS vacío. Menor: mismo contenido analizado.
3. `vite-env.d.ts` declara `VITE_API_BASE_URL?: string` sin `readonly`. Menor.
4. `index.css` se creó vacío primero (paso RED). Menor, solo de proceso.
Conviene que Juan Pablo las confirme en el PR y que design.md las registre si quiere trazabilidad.

## Observación: npm audit (no corregido)
`npm audit`: 7 vulnerabilidades (4 moderate, 1 high, 2 critical).
| Paquete | Severidad | Alcance |
|---|---|---|
| vitest (<=4.0.0-beta.19) | critical (directa) | Solo dev/test: lectura/ejecución de archivos si se levanta Vitest UI (no se usa) |
| tinypool (<=2.1.1) | critical (indirecta, de vitest) | Solo dev/test: prototype pollution en opciones de worker |
| vite 5.4.x | high (directa) | Solo dev server: path traversal en `.map`, bypass de `server.fs.deny` en Windows, launch-editor; no llega al bundle de producción |
| esbuild (<=0.24.2) | moderate (indirecta) | Solo dev server: cualquier sitio puede consultar el servidor de desarrollo |
| vite-node | moderate (indirecta) | Solo dev/test |
| react-router-dom 6.30.x | moderate (directa) | **Dependencia de runtime**: open redirect via backslash en `<Link>`/`useNavigate`; el SSR hydration (`deserializeErrors`) no aplica a una SPA Vite |
| react-router | moderate (indirecta) | Idem |
Las correcciones requieren `npm audit fix --force` (saltos mayores: Vite 6+/Vitest 3+), que es el plan B de design.md D1. Para el PR: decidir si se sube de línea o se acepta el riesgo, ya que el dev server solo escucha en localhost; revisar `react-router-dom` antes de usar `<Link>` con URLs no confiables.

## Qué NO se pudo verificar
- Escenarios 9 y 11 (dev server y proxy contra backend) no se re-ejecutaron; se toman de los chequeos manuales de apply.
- No se verificó en Node 22.20 (solo la versión local de Node instalada).
- No hay mockups ni verificación visual por política (sin `design.md` de mockups en este change).

## Próximo paso
Sin bloqueantes. Preguntar antes de commitear (regla 16), abrir PR `santino` -> `master` (revisa Juan Pablo) y luego `/opsx:archive c-02-esqueleto-frontend`.

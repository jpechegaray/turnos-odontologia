# Design

## Context

La motivación está en proposal.md, sección Why, y los requisitos en `specs/entorno-frontend/spec.md`. Este documento explica cómo se cumplen.

Situación actual y restricciones:

- No existe `frontend/`. El backend de C-01 corre en `http://localhost:8080` (puerto por defecto de Spring). Su seguridad es deny-by-default, con `/api/public/**` abierto. El login y el CSRF llegan en C-03.
- El `.gitignore` raíz ya ignora `node_modules/`, `dist/`, `build/` y `coverage/`, y también `.env` y `.env.*`, salvo `.env.example`. El `.env.example` raíz ya declara `VITE_API_BASE_URL=/api`. Ninguno de los dos se toca.
- El design system vive en `design-system/`, en la raíz y fuera de `frontend/`. Está en Revision 1, aprobada, en modo `system-only`.
  - `tokens.css` solo declara variables: tiene `:root`, la variante oscura en dos bloques y reduced-motion, pero no aplica estilos a `body`.
  - `brand.js` es un script clásico (IIFE), no un módulo ES. Define `window.BRAND = { name, logoAlt }` congelado y hace un *binding* único de `[data-brand-name]` cuando carga el documento.
- Para el stack, KB 02 y CLAUDE.md fijan como mínimo React 18, TypeScript 5 y Vitest 1.x. La ficha C-02 pide explícitamente React 18 y TypeScript 5.
- Entorno local: Node 26.13.1 y npm 11.20.0. El `README.md` del equipo pide Node 22.20 o más.
- Reglas duras que aplican: 1 (datos ficticios), 11 (`it(...)` con el nombre exacto del escenario), 12 (`strict`, sin `any`, componentes en `PascalCase`) y 13 (solo tokens).
- Skills aplicadas: `vitest` (config compartida con Vite, `jsdom`, `vitest run` sin watch, `it` con el nombre del escenario) y `vercel-react-best-practices`. De esta última solo aplica lo de SPA: imports directos, sin *barrel files* y JSX estático simple. Se ignoran las reglas de SSR y `server-*`.

### Inconsistencias detectadas en la documentación

1. **Ruta del import de los tokens.** `design-system/README.md` §Consumir los tokens muestra `import "../design-system/tokens.css"` desde `src/main.tsx`. Como `main.tsx` está en `frontend/src/`, la ruta correcta es `../../design-system/tokens.css`. Se usa la correcta. Es una errata menor del README y no se corrige en este change.
2. **"Importar `BRAND.name`".** El README dice "En React, importar `BRAND.name` o repetir la constante en un módulo `brand.ts`". `brand.js` no exporta nada: solo se puede importar por su efecto lateral y después leer `window.BRAND`. Además, su *binding* de `[data-brand-name]` corre una sola vez al cargar, antes de que React monte, así que no actualiza nodos que React renderiza después. Por eso React tiene que leer `window.BRAND.name` (ver D4).
3. **Archivos de `frontend/` en KB 08.** El árbol lista solo `package.json`, `vite.config.ts` y `tsconfig.json`. Una app Vite necesita además `index.html`, y la plantilla oficial agrega `tsconfig.app.json`, `tsconfig.node.json` y `eslint.config.js`. Esto no es una contradicción sino un hueco: se completa con los valores por defecto de la plantilla.
4. **Coherencia entre la ficha, KB y C-03 (sin conflicto).** La ficha habla de "cookies sin CORS abierto". C-03 restringe CORS a `APP_CORS_ORIGIN`. Con el proxy, en desarrollo el navegador nunca hace una petición cross-origin, así que las dos cosas son compatibles.
5. **Deduplicación de requests (sin conflicto).** `.atl/skill-registry.md` (vercel-react) sugiere SWR o TanStack Query para deduplicar requests. Ningún change del roadmap lo pide: C-09 define un cliente `fetch` propio. Por eso no se agregan esas librerías (ver D12).

## Goals / Non-Goals

**Goals:**
- Un `frontend/` que equivalga a la plantilla oficial `react-ts` de Vite, con los únicos agregados que pide la ficha C-02.
- Consumir `design-system/` en su lugar (sin copias) en dev, build y tests.
- Un `package.json` cerrado para el resto del roadmap.

**Non-Goals:**
- Router central, cliente HTTP, kit UI y utilidades de fecha: van en C-09. React Router queda **instalado pero sin cablear**.
- Contratos de registro de rutas (`features/*/routes.ts`): los define C-09.
- Cobertura de código, formateador (Prettier) y CI: no están en el plan.

## Decisions

### D1. Versiones: los *majors* que nombra el plan, en su última versión menor

| Paquete | Versión | Motivo |
|---|---|---|
| `react`, `react-dom` | `^18.3.1` | La ficha pide React 18. React 19 cumpliría el "mínimo" de KB 02, pero se aparta de la ficha. |
| `typescript` | `~5.9.3` | Pide TS 5. Se fija la versión menor con `~`, como la plantilla, para que un TS 6 no cambie los defaults. |
| `vitest` | `^1.6.1` | KB 02 dice "Vitest 1.x". 1.6.1 es la última de esa línea. |
| `vite` | `^5.4.21` | Es la línea que soporta Vitest 1.x (Vitest 2 o más pide Vite 5.x/6.x con APIs distintas). |
| `@vitejs/plugin-react` | `^4.7.0` | Compatible con Vite 5. |
| `jsdom` | `^24.1.3` | Entorno de tests. Es contemporáneo de Vitest 1.x y su `engines` es `node >=18`. |

Los `engines` declarados de Vitest 1.6.1 y Vite 5.4.21 (`^18 || >=20`) y de jsdom 24 (`>=18`) **admiten Node 26 y Node 22.20**. Que funcionen en Node 26 en la práctica no está probado: ver Riesgos y Open Questions.

*Alternativa descartada:* Vite 6/7 + Vitest 3. Es más nuevo, pero se aparta de "Vitest 1.x" y del criterio de mínima desviación. Queda como plan B si Vitest 1.6 falla en Node 26.

### D2. Base de la estructura: la plantilla `react-ts` de Vite 5

La base es `index.html`, `src/main.tsx` y `src/App.tsx`, más `tsconfig.json` con *references* a `tsconfig.app.json` (código de `src/`) y `tsconfig.node.json` (`vite.config.ts`), `eslint.config.js` y los scripts de la plantilla (`dev: vite`, `build: tsc -b && vite build`, `lint: eslint .`). Solo se agrega `test: vitest run`, que corre una vez y sin watch, como pide la skill vitest para CI y verify. `package.json` lleva `"private": true` y `"type": "module"`, igual que la plantilla. Se versiona `package-lock.json`, que `npm ci` necesita.

*Alternativa descartada:* un único `tsconfig.json` armado a mano. Coincide literalmente con KB 08, pero se aparta de la plantilla, que es la referencia que el equipo va a encontrar en la documentación de Vite. El `tsconfig.json` raíz sigue existiendo, así que KB 08 se cumple igual.

### D3. Consumir `design-system/` desde fuera de `frontend/`: imports relativos y `server.fs.allow`

- `main.tsx` hace `import "../../design-system/tokens.css"` e `import "../../design-system/brand.js"`, este último solo por su efecto lateral.
- `vite.config.ts` define `server.fs.allow: [".", "../design-system"]`. Por defecto, el servidor de desarrollo de Vite solo sirve archivos dentro de la raíz del proyecto (`frontend/`). Sin esa línea, en dev se bloquean los archivos de `design-system/`. Se permite solo esa carpeta, no todo el repositorio.
- **Build:** Vite empaqueta los dos archivos en `dist/`. El CSS queda en `dist/assets/*.css` y `brand.js` dentro del bundle JS. `fs.allow` no aplica en el build.
- **Tests:** Vitest resuelve las rutas relativas sin configuración extra. El archivo de setup importa `brand.js` para definir `window.BRAND` en jsdom. Los tests que analizan CSS lo leen como texto con el sufijo `?raw` de Vite, porque Vitest no procesa CSS por defecto.

*Alternativas descartadas:*
- Copiar los archivos a `src/styles/` (el README lo admite). Se descarta porque duplica la fuente y se desincroniza en C-50, que solo edita `design-system/`.
- Un alias `@design-system`. Se descarta porque obliga a configurarlo en dos lugares (Vite y `tsconfig`) y no es necesario para dos imports.
- `server.fs.allow: [".."]` (todo el repo). Se descarta porque es más amplio que lo necesario.

### D4. Nombre del producto: leer `window.BRAND.name` en el render

- `src/brand.d.ts` declara el tipo global `Window["BRAND"]` como `Readonly<{ name: string; logoAlt: string }>`. Así no hay `any` y el lint queda en verde.
- `App` renderiza `window.BRAND.name`, por ejemplo en un `<h1>`. `main.tsx` asigna `document.title = window.BRAND.name`, para no repetir el nombre en el `<title>` de `index.html`, cuyo valor estático queda vacío.
- No se modifica `brand.js`, porque es parte del design system aprobado: cambiarlo obligaría a subir la revisión (README §Política).

*Alternativas descartadas:*
- Repetir la constante en un módulo `brand.ts` (opción del README). Se descarta porque crea dos fuentes del nombre y contradice el requisito de un solo lugar para el renombre.
- Convertir `brand.js` en módulo ES. Se descarta porque toca un archivo aprobado.
- Depender del *binding* `[data-brand-name]`. Se descarta porque corre antes de que React monte (Context, punto 2).

### D5. Estilos globales mínimos con tokens

`src/index.css` es el nombre por defecto de la plantilla y lo importa `main.tsx`. Aplica a `body` `background: var(--color-bg)`, `color: var(--color-text)`, `font-family: var(--font-sans)` y `margin: 0`. El `0` es un reset, no un valor de diseño. El espaciado interno de `App` usa `var(--space-*)`. Sin esto, `tokens.css` declara variables pero la página no se ve con los tokens, y no se cumple el criterio de salida.

*Alternativa descartada:* no poner estilos globales. Se descarta porque la página quedaría con los colores del navegador.

### D6. Proxy de desarrollo

`server.proxy: { "/api": { target: "http://localhost:8080" } }`, sin `rewrite`, porque el backend expone `/api/**`. Se deja `changeOrigin` en su valor por defecto (`false`). La cookie de sesión queda ligada a `localhost`, que es el host que ve el navegador, y el destino se fija a mano: KB no define una variable para eso. El puerto de la SPA es el 5173, el valor por defecto de Vite, y coincide con `APP_PUBLIC_URL` y `APP_CORS_ORIGIN`.

*Alternativas descartadas:*
- CORS directo a `:8080`. Se descarta porque KB 08 §Seguridad y DD-03 piden mismo origen.
- `changeOrigin: true`. No hace falta hoy. C-03 puede revisarlo si su política CSRF/CORS lo requiere.

### D7. `VITE_API_BASE_URL`

- `vite.config.ts` define `envDir: ".."`, así Vite lee el `.env` **raíz**. KB y C-01 definen un único `.env` y `.env.example`. Vite solo expone al cliente las variables con prefijo `VITE_` (no se toca `envPrefix`), así que los secretos del `.env` raíz no llegan al bundle.
- `src/vite-env.d.ts` (de la plantilla) tipa `ImportMetaEnv.VITE_API_BASE_URL?: string`.
- `src/shared/api/apiBaseUrl.ts` exporta `obtenerApiBaseUrl()`, que devuelve `import.meta.env.VITE_API_BASE_URL || "/api"`. Se usa `||` y no `??` para que un valor vacío (`VITE_API_BASE_URL=`) también caiga en `/api`. El cliente de C-09 lo va a consumir desde la misma carpeta `shared/api/` (KB 08: "shared/ api client").

*Alternativas descartadas:*
- Un `frontend/.env.example`. Se descarta porque duplica el archivo raíz, y `frontend/.env.development` quedaría ignorado por la regla `.env.*`.
- No poner valor por defecto. Se descarta porque `npm test` y `npm run build` tienen que andar sin `.env` (spec, Requirement de reproducibilidad).

### D8. Carpetas

- `src/app/`, `src/shared/` (con `shared/api/` por D7) y `src/features/{auth,agenda,pacientes,profesionales,prestaciones,lista-espera,configuracion,notificaciones,reserva-publica}/`.
- Git no versiona carpetas vacías, así que las que quedan sin archivos llevan un `.gitkeep`.
- `App.tsx` y `main.tsx` van en la raíz de `src/`, como en el árbol de KB 08.
- No se agregan *barrel files* (`index.ts`), por la regla `bundle-barrel-imports` de vercel-react.

### D9. TypeScript estricto

`tsconfig.app.json` usa los valores de la plantilla: `strict: true`, `noUnusedLocals`, `noUnusedParameters`, `noFallthroughCasesInSwitch`, `moduleResolution: "bundler"` y `jsx: "react-jsx"`. No se activa `allowJs`: el import de `brand.js` es solo por efecto lateral y TS no lo chequea. No se activa `noUncheckedSideEffectImports`, que está apagado por defecto en TS 5.9; si se activara, rompería ese import.

### D10. ESLint de la plantilla

Es la *flat config* de create-vite: `@eslint/js` recommended, `typescript-eslint` recommended (incluye `@typescript-eslint/no-explicit-any` como error), `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh` y `globals.browser`, ignorando `dist`. El lint alcanza solo a `frontend/`, así que `design-system/brand.js` queda fuera.

*Alternativas descartadas:*
- Configs *type-checked*. Son más estrictas y lentas, y no están en el plan.
- Prettier. No está en el plan.

### D11. Vitest y Testing Library

- El bloque `test` va dentro de `vite.config.ts`, usando `defineConfig` de `vitest/config` (config compartida, como pide la skill vitest).
- Opciones: `environment: "jsdom"`, `globals: false` (imports explícitos, que es el valor por defecto de Vitest) y `setupFiles: ["./src/setupTests.ts"]`.
- `setupTests.ts` hace tres cosas:
  - importa `@testing-library/jest-dom/vitest` (matchers tipados);
  - importa `../../design-system/brand.js`;
  - registra `afterEach(cleanup)`, porque sin `globals` Testing Library no limpia sola.
- Los tests van junto al código (`*.test.ts(x)`). Cada `it(...)` repite el nombre exacto del escenario (regla 11).
- **Test que comprueba el rechazo de `any`:** corre con `// @vitest-environment node` y usa la API `ESLint#lintText` sobre un texto ficticio con `filePath` dentro de `src/`, sin dejar el archivo en disco.
- **Test del proxy:** importa la configuración de `vite.config.ts`.
- **Test de la estructura:** usa `node:fs`.

*Alternativa descartada:* `happy-dom`. Es más rápido, pero `jsdom` es el entorno de referencia de Testing Library y el de la skill. No hace falta otro.

### D12. Dependencias "previstas", tomadas del roadmap y no inventadas

| Dependencia | Tipo | Quién la usa |
|---|---|---|
| `react`, `react-dom` | prod | Todos |
| `react-router-dom` `^6.30.x` | prod | C-09 (router y `import.meta.glob`), C-16 (guards), C-48 (`/reservar`), C-49 (`/turno/:token`, `/oferta/:token`) |
| `vite`, `@vitejs/plugin-react`, `typescript` | dev | Build |
| `@types/react`, `@types/react-dom` (`^18.3`) | dev | Tipos de React 18 |
| `@types/node` (`^22`) | dev | `vite.config.ts` y los tests con `node:fs`. Major de la versión mínima del README (Node 22.20). |
| `vitest`, `jsdom` | dev | Todos los tests de front |
| `@testing-library/react` `^16`, `@testing-library/dom` `^10` (*peer* obligatorio de RTL 16), `@testing-library/jest-dom` `^6` | dev | Todos los tests de componentes |
| `@testing-library/user-event` `^14` | dev | Formularios de C-16, C-20 a C-24, C-45 a C-48 ("Tests Vitest de formularios") |
| `eslint`, `@eslint/js`, `typescript-eslint`, `eslint-plugin-react-hooks`, `eslint-plugin-react-refresh`, `globals` | dev | Script `lint` (D10) |

Lo que **no** se agrega, y por qué:

- Librería de fechas: C-09 trabaja con `Intl` y la zona `America/Argentina/Buenos_Aires`, y ninguna ficha nombra una librería.
- SWR o TanStack Query: C-09 define un cliente `fetch` propio.
- MSW: los tests de C-09 en adelante mockean el cliente con `vi.mock` (skill registry).
- Librería de formularios: no figura en el plan.
- `@vitest/coverage-v8`: ningún criterio de salida pide cobertura (ver Open Questions).
- React Router 7: ver Open Questions. Se elige la v6 porque es la línea contemporánea de React 18 y tiene la API `BrowserRouter`/`Routes` sin el modo framework. La v7 también admite React 18, así que es un cambio menor si el líder la prefiere.

## Risks / Trade-offs

- **[Vitest 1.6, Vite 5.4 y jsdom 24 no fueron probados en Node 26]** → La primera tarea de apply es un *spike*: `npm ci && npm test` con un test trivial en Node 26 y, si es posible, en Node 22.20. Si falla, se para y se aplica el plan B de D1 (Vite 6 + Vitest 3, que siguen cumpliendo el "mínimo 1.x"). La desviación se registra en este documento y se le avisa a Juan Pablo antes de seguir.
- **[El lockfile se genera en Windows]** npm tuvo errores con dependencias opcionales nativas de rollup y esbuild cuando el lock se arma en otra plataforma → Se usa npm 11. Si en otra máquina falla `npm ci` por `@rollup/rollup-<plataforma>`, se regenera el lock en limpio (borrar `node_modules` y el lock, y correr `npm install`). Se verifica `npm ci` desde cero antes del PR.
- **[`envDir: ".."` hace que Vite lea el `.env` raíz, que tiene secretos]** → Solo se exponen variables `VITE_*`. Queda prohibido cambiar `envPrefix`. La única variable `VITE_*` de KB no es sensible.
- **[`changeOrigin: false`]** Spring recibe `Host: localhost:5173` → Hoy no afecta a C-01. Si C-03 valida el origen o el host para CSRF, lo ajusta en su change. Es una línea en `vite.config.ts`, que no es `package.json`.
- **[`window.BRAND` es un global]** Si `brand.js` no carga, `App` falla → En `main.tsx` el import va antes del render. Los tests lo cargan en el setup y el escenario de dev verifica que la consola no muestre errores.
- **[Un `package.json` "cerrado" no puede prever todo]** → Las dependencias salen solo de lo que nombran las fichas (D12). Si un change futuro necesita otra, tiene que justificarla en su propio propose: es una excepción, no la regla.

## Migration Plan

No aplica: es código nuevo, sin datos. Para revertirlo alcanza con borrar `frontend/` y la sección del README.

## Open Questions

Ninguna bloquea apply.

1. **Vitest 1.x en Node 26.** ¿Se acepta el plan B (Vite 6 + Vitest 3) si el *spike* falla? *Recomendación:* sí. KB 02 fija 1.x como versión **mínima**, así que el plan B no rompe el plan. Igual se registra y se avisa.
2. **React Router 6 o 7.** *Recomendación:* la v6 (D12). Es una decisión de Juan Pablo al revisar el PR y no cambia specs ni tasks.
3. **Cobertura.** ¿El equipo quiere `@vitest/coverage-v8` ya incluido, para no tener que editar `package.json` después? *Recomendación:* no agregarlo, porque ningún criterio lo pide. Si el líder lo quiere, se suma en este mismo PR, antes del merge, y sigue respetando "no volver a editarlo".

## UI Mockups

- Design system revision: 1
- Decision: no (project policy) — 2026-11-21. La política del proyecto es `system-only`: no se piden mockups por change. La única pantalla de C-02 (el nombre del producto con los tokens globales) se verifica a mano con `npm run dev`, según el escenario "El servidor de desarrollo sirve la SPA con los tokens aplicados".

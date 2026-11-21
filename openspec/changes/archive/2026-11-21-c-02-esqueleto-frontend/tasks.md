# Tasks

> TDD estricto: en cada tarea de código, primero se escribe el test con el nombre exacto del escenario de `specs/entorno-frontend/spec.md` en `it(...)`, se ejecuta y tiene que fallar (RED). Después va el código mínimo (GREEN), la triangulación y el refactor con los tests en verde. Los comandos se corren dentro de `frontend/`, salvo que se indique otra cosa. Solo datos ficticios. No se toca `design-system/`, `backend/`, `.gitignore` ni `.env.example`.

## 1. Spike de compatibilidad y andamiaje

- [x] 1.1 Crear `frontend/package.json` (`private`, `type: module`) con **todas** las dependencias y versiones de design.md D1/D12, y los scripts `dev: vite`, `build: tsc -b && vite build`, `test: vitest run` y `lint: eslint .`. Correr `npm install` para generar `package-lock.json`. Se verifica con `npm ls --depth=0`, que tiene que terminar sin errores ni *peers* faltantes.
- [x] 1.2 Spike en Node 26: crear un test trivial temporal con un `vite.config.ts` mínimo (`environment: "jsdom"`) y correr `npm test`. Verificación: termina con código 0. Si falla por incompatibilidad de Vitest 1.6, Vite 5.4 o jsdom 24 con Node 26, **parar**, aplicar el plan B de design.md D1 (Vite 6 + Vitest 3), registrar la desviación en design.md y avisar antes de seguir. Si hay Node 22.20 disponible, repetir ahí. Al terminar, borrar el test temporal.
- [x] 1.3 Crear el andamiaje de la plantilla `react-ts` de Vite 5: `index.html` (con `<div id="root">`, el `<title>` vacío y `src/main.tsx` como script de módulo), `tsconfig.json` con *references*, `tsconfig.app.json` (`strict`, `noUnusedLocals`, `noUnusedParameters`, `noFallthroughCasesInSwitch`, `moduleResolution: bundler`, `jsx: react-jsx`, `include: ["src"]`), `tsconfig.node.json` (`vite.config.ts`) y `src/vite-env.d.ts`. Se verifica con `npx tsc -b`, que termina con código 0.
- [x] 1.4 Crear `eslint.config.js` (la *flat config* de create-vite de D10, ignorando `dist`). Se verifica con `npm run lint`, que termina con código 0.

## 2. Configuración de Vite y Vitest (proxy, envDir, fs.allow)

- [x] 2.1 RED: en `src/app/entornoDesarrollo.test.ts`, escribir `it("Las peticiones a /api se redirigen al backend en desarrollo")`. Importa la configuración de `../../vite.config` y comprueba que `server.proxy["/api"]` apunta a `http://localhost:8080` y que no tiene `rewrite`. Ejecutar `npm test` y ver el fallo.
- [x] 2.2 GREEN: completar `vite.config.ts` con `defineConfig` de `vitest/config`, que incluye `plugins: [react()]`, `envDir: ".."`, `server.proxy` (D6), `server.fs.allow: [".", "../design-system"]` (D3) y `test: { environment: "jsdom", globals: false, setupFiles: ["./src/setupTests.ts"] }`. Crear `src/setupTests.ts` según D11: jest-dom/vitest, import de `../../design-system/brand.js` y `afterEach(cleanup)`. Se verifica con `npm test`, que tiene que quedar en verde.
- [x] 2.3 Triangular: agregar al mismo `it` la aserción de que `/api` es la única entrada del proxy, sin reglas extra que capturen otras rutas de la SPA. Se verifica con `npm test` en verde. Refactor: extraer la URL del backend a una constante con nombre dentro de `vite.config.ts` y correr `npm test` de nuevo.

## 3. Base de la API (`VITE_API_BASE_URL`)

- [x] 3.1 RED: en `src/shared/api/apiBaseUrl.test.ts`, escribir `it("La base de la API es /api por defecto")` e `it("La base de la API toma el valor de VITE_API_BASE_URL")`, con el valor ficticio `/api-prueba` puesto mediante `vi.stubEnv`. En el caso por defecto, quitar la variable de `import.meta.env` y restaurarla en `afterEach`, para que el resultado no dependa del `.env` local. Ejecutar `npm test` y ver el fallo.
- [x] 3.2 GREEN: crear `src/shared/api/apiBaseUrl.ts` con `obtenerApiBaseUrl()` (D7) y tipar `VITE_API_BASE_URL?: string` en `src/vite-env.d.ts`. Se verifica con `npm test` en verde.
- [x] 3.3 Triangular: agregar el caso de valor vacío (`""`), que devuelve `/api`, dentro de `it("La base de la API es /api por defecto")`. Se verifica con `npm test` en verde.

## 4. Marca: App con el nombre de `BRAND` (test de humo)

- [x] 4.1 RED: en `src/App.test.tsx`, escribir `it("La App muestra el nombre de la marca tomado de BRAND")`. Renderiza `<App />` y espera `screen.getByRole("heading", { name: window.BRAND.name })`. Ejecutar `npm test` y ver el fallo.
- [x] 4.2 GREEN: crear `src/brand.d.ts` (tipo global `Window["BRAND"]`, D4) y `src/App.tsx` (componente `App` en PascalCase, sin `any`) que renderiza `window.BRAND.name` en un `<h1>`. Se verifica con `npm test` en verde.
- [x] 4.3 Triangular: escribir `it("La App refleja un cambio del nombre en BRAND")`. Reemplaza `window.BRAND` por un objeto con el nombre ficticio `Consultorio Ficticio de Prueba`, comprueba que se muestra ese nombre y que no aparece el provisional, y restaura el valor original en `afterEach`. Se verifica con `npm test` en verde.
- [x] 4.4 Crear `src/main.tsx`: importa `../../design-system/tokens.css`, `../../design-system/brand.js` y `./index.css`, asigna `document.title = window.BRAND.name` y monta `<App />` en `StrictMode` con `createRoot`. Se verifica con `npx tsc -b` y `npm run lint`, que terminan con código 0, y con `npm run build`, que tiene que generar `dist/` sin errores de resolución de `design-system/`.

## 5. Tokens aplicados

- [x] 5.1 RED: en `src/app/estilosGlobales.test.ts`, escribir `it("Los estilos globales usan solo variables definidas en tokens.css")`. Lee `../index.css?raw` y `../../../design-system/tokens.css?raw`, y comprueba tres cosas: que `background`, `color` y `font-family` de `body` usan `var(--…)`; que cada `var(--x)` usada en `index.css` está declarada en `tokens.css`; y que no hay hex, `rgb()` ni `hsl()` escritos de forma fija. Ejecutar `npm test` y ver el fallo.
- [x] 5.2 GREEN: crear `src/index.css` según D5 y aplicar `var(--space-*)` al contenedor de `App`, ya sea en `index.css` o con una clase dentro del mismo archivo. Se verifica con `npm test` en verde.
- [x] 5.3 Triangular: agregar al test un caso que valida el detector con un texto CSS ficticio en línea que tiene `color: #123456` y `var(--no-existe)`, y que tiene que informar ambos problemas. Así se prueba que la aserción no pasa siempre. Se verifica con `npm test` en verde. Refactor: extraer el análisis a funciones puras dentro del archivo de test.
- [x] 5.4 Escenario "El build incluye los tokens del design system": correr `npm run build` y verificar con `grep -l -- "--color-primary" dist/assets/*.css` y `grep -l -- "--font-sans" dist/assets/*.css`, que tienen que encontrar coincidencias.

## 6. Chequeo estático estricto

- [x] 6.1 RED → GREEN: en `src/app/chequeoEstatico.test.ts` (`// @vitest-environment node`), escribir `it("El lint rechaza el uso explícito de any")`. Usa `new ESLint({ cwd })` y `lintText("export const valor: any = 1;\n", { filePath: "src/fixtureAny.ts" })`, y espera un mensaje con `ruleId` `@typescript-eslint/no-explicit-any` y `severity` 2. Verificar que falla si se desactiva la regla en `eslint.config.js` (RED controlado) y que pasa con la config de D10. Se verifica con `npm test` en verde.
- [x] 6.2 Triangular en el mismo archivo: el texto `export const valor: number = 1;\n` no produce errores de `no-explicit-any`. Se verifica con `npm test` en verde.
- [x] 6.3 Escenario "La configuración de TypeScript está en modo estricto": se verifica con `npx tsc -p tsconfig.app.json --showConfig`, que tiene que mostrar `"strict": true`.

## 7. Estructura de carpetas por feature

- [x] 7.1 RED: en `src/app/estructura.test.ts` (`// @vitest-environment node`), escribir `it("La estructura de carpetas sigue la arquitectura del frontend")`. Con `node:fs`, comprueba que existen `src/app`, `src/shared` y `src/features/{auth,agenda,pacientes,profesionales,prestaciones,lista-espera,configuracion,notificaciones,reserva-publica}`. Ejecutar `npm test` y ver el fallo.
- [x] 7.2 GREEN: crear las carpetas que faltan, con `.gitkeep` en las que no tienen archivos (D8). No crear `routes.ts` ni *barrel files*. Se verifica con `npm test` en verde y con `git status --porcelain frontend/src/features`, que tiene que listar las nueve carpetas.

## 8. Documentación de arranque

- [x] 8.1 Agregar en `README.md`, sección 2, la subsección "Levantar el frontend", después de "Levantar el backend". Incluye `cd frontend`, `npm ci`, `npm run dev` (en `http://localhost:5173`, con `/api` reenviado a `:8080`), `npm test`, `npm run build` y `npm run lint`, más una nota: el `.env` raíz es opcional para el front porque `VITE_API_BASE_URL` vale `/api` por defecto. Se verifica corriendo los comandos documentados tal como están escritos.

## 9. Verificación integral

- [x] 9.1 Escenario "La instalación limpia, el build y los tests terminan en verde": borrar `frontend/node_modules` y `frontend/dist`, renombrar temporalmente el `.env` raíz si existe y correr `npm ci && npm run build && npm test`. Verificación: los tres terminan con código 0, `vitest run` sale sin quedar en watch, y después se restaura el `.env`.
  - Resultado (2026-11-21): verificado en una copia aislada sin `.env` (solo `frontend/` y `design-system/`): `npm ci`, `npm run build` (tsc -b + vite build OK) y `npm test` (6 archivos, 8 tests) terminaron con código 0, sin watch. La copia se borró.
- [x] 9.2 Escenario "El lint pasa sin errores sobre el código del frontend": se verifica con `npm run lint`, que termina con código 0.
- [x] 9.3 Escenario "El servidor de desarrollo sirve la SPA con los tokens aplicados" (manual o con navegador): con `npm run dev`, abrir `http://localhost:5173` y verificar tres cosas: que se ve el nombre de `BRAND`; que el estilo calculado de `body` muestra el `background-color`, el `color` y la `font-family` de `tokens.css`; y que la consola no muestra errores ni requests fallidos de `tokens.css` o `brand.js`. Si se usa navegador automatizado, capturar 390x844 y 1440x900 como evidencia, sin datos reales. Detener el servidor al terminar.
- [x] 9.4 Escenario "Una petición a /api desde el servidor de desarrollo llega al backend" (manual): con `docker compose up -d` y el backend de C-01 corriendo en `:8080`, más `npm run dev`, correr `curl -i http://localhost:5173/api/inexistente`. Verificación: responde 401 o 403 del backend (no un 200 con `index.html`). Detener los servidores al terminar.
- [x] 9.5 Comprobar que no se tocó nada fuera de alcance: `git status` muestra cambios solo en `frontend/`, `README.md` y `openspec/changes/c-02-esqueleto-frontend/`, además de la modificación previa no relacionada de `docs/discovery/discovery.md`. Comprobar también que `git check-ignore frontend/node_modules frontend/dist` informa que las dos rutas están ignoradas.

## Workflow follow-up

- Correr `openspec-verify` (genera `verify.md`) antes del PR.
- Preguntar antes de commitear (regla 16) y abrir el PR desde la rama `santino` a `master`. El PR lo revisa Juan Pablo, que además decide las Open Questions 2 y 3 de design.md.
- Después del merge: `/opsx:archive c-02-esqueleto-frontend`.

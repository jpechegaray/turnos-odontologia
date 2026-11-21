# Proposal

## Why

Todos los changes de front (C-09, y a través de él C-16, C-20 a C-24, C-34, C-40, C-45 a C-50) necesitan una SPA que compile, que se pruebe y que ya consuma el design system aprobado (Revision 1). Hoy no existe `frontend/`. C-02 es la Ola 0 del front y desbloquea directamente a C-09. Hay que dejarlo listo antes de que el equipo empiece a trabajar en paralelo, porque el `package.json` se arma una sola vez y después no se vuelve a tocar (CHANGES.md, ficha C-02).

## What Changes

- Nuevo proyecto `frontend/` con Vite + React 18 + TypeScript 5 (modo `strict`), React Router, Vitest y Testing Library.
- `package.json` con **todas** las dependencias que ya prevé el roadmap para los changes de front, para que ningún change posterior lo edite. Se versiona `package-lock.json` para que `npm ci` sea reproducible.
- Scripts `dev`, `build`, `test` y `lint`.
- Proxy del servidor de desarrollo de Vite: `/api` va al backend de C-01 (`http://localhost:8080`). Así la SPA y la API comparten origen y las cookies de sesión funcionan sin abrir CORS (KB 08 §Seguridad, DD-03). La base de la API se lee de `VITE_API_BASE_URL` (por defecto `/api`, KB 08 §Variables de entorno).
- Estructura `src/{app,shared,features}` con las carpetas de feature de KB 08 §Estructura de directorios: `auth`, `agenda`, `pacientes`, `profesionales`, `prestaciones`, `lista-espera`, `configuracion`, `notificaciones` y `reserva-publica`.
- `main.tsx` importa `design-system/tokens.css` y `design-system/brand.js` desde la raíz del repo, sin copiarlos. Unos estilos globales mínimos aplican los tokens (fondo, texto y fuente).
- `App` muestra el nombre del producto tomado de `BRAND`, que define `brand.js`.
- Test de humo con Vitest: renderiza `App` y comprueba el nombre que viene de `BRAND`. Suma tests de configuración (proxy, base de la API, estructura, estilos con tokens y lint que rechaza `any`).
- Sección "Levantar el frontend" en el `README.md` raíz, siguiendo la convención de arranque que dejó C-01.

## Capabilities

### New Capabilities
- `entorno-frontend`: el entorno de la SPA. Cubre que sea reproducible (`npm ci`, build, tests y lint), el proxy `/api` en el mismo origen, la base de la API configurable, la estructura de carpetas por feature, el consumo de los tokens y de la marca del design system, y el chequeo estricto de tipos.

### Modified Capabilities
- (ninguna)

## Impact

- **Código nuevo:** `frontend/`, con `package.json`, `package-lock.json`, `vite.config.ts`, los `tsconfig*.json`, `eslint.config.js`, `index.html` y `src/`.
- **Archivos existentes:** `README.md` (se agrega una sección). `design-system/` no se modifica: se consume tal como está. `.gitignore` ya ignora `node_modules/`, `dist/` y `coverage/`, y `.env.example` ya declara `VITE_API_BASE_URL=/api`, así que ninguno de los dos se toca.
- **Dependencias:** solo npm, en `frontend/`. El backend no cambia.
- **Sistemas:** en dev, el front corre en `http://localhost:5173` (puerto por defecto de Vite, que coincide con `APP_PUBLIC_URL` y `APP_CORS_ORIGIN` de KB 08) y le pasa `/api` al backend en `:8080`.
- **Gobernanza:** LOW. Autonomía plena si los tests pasan.

### Fuera de alcance
- Kit de componentes UI, cliente `fetch` con CSRF y Problem Details, utilidades de fecha en zona BA, router central y registro automático de rutas: van en **C-09**.
- Login, contexto de sesión, guards por rol y menú filtrado: van en **C-16**.
- Nombre, logo, color y tipografía finales: van en **C-50**. Acá se usa la marca provisional tal como está.
- Pantallas de features, despliegue productivo y `Dockerfile` (KB 08 §Entorno y despliegue: queda como mejora).

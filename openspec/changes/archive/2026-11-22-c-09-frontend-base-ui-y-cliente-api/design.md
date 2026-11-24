# Design

## Context

C-02 dejó la SPA con Vite 5, React 18, TypeScript estricto, React Router 6, Vitest 1 (jsdom) y Testing Library. `main.tsx` importa `tokens.css` y `brand.js`. `obtenerApiBaseUrl()` vive en `shared/api`, y el proxy `/api` apunta a `:8080`. Las carpetas `features/*` están vacías. Del backend se toma lo siguiente:
- El contrato de C-03 (README): `GET /api/auth/csrf` → 204 con la cookie `XSRF-TOKEN` legible. Las escrituras llevan la cabecera `X-XSRF-TOKEN` y, tras el login o el logout, el token cambia.
- El formato de error de C-01 (`ManejadorErrores`): Problem Details con `status`, `title`, `detail`, `codigo`, `errores[{campo, mensaje}]` y propiedades extra como `recurso`.

Requisitos en `specs/*/spec.md`. Motivación en `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Una sola forma de llamar a la API, de mostrar horas y de sumar pantallas, que los changes siguientes usen sin editar archivos centrales.
- **Tocar lo mínimo de lo existente:** solo `App.tsx`, para montar el router y el layout. No se agregan dependencias: `react-router-dom` ya está en `package.json`.

**Non-Goals:**
- Login, guards por rol y redirección ante un 401 (C-16). El tipo de ruta solo reserva el campo `roles` para que C-16 no tenga que editarlo.
- Pantallas públicas sin layout del personal (C-48 y C-49). Ver Open Questions.
- Logo e identidad final (C-50). El encabezado muestra solo el nombre de `BRAND`.

## Decisions

### D1. Estructura
```
src/
  shared/api/     clienteApi.ts (peticion, api.get/post/put/patch/delete), csrf.ts, errorApi.ts (ErrorApi, TipoErrorApi)
  shared/fecha/   zonaHoraria.ts (ZONA_CONSULTORIO, formatearFecha, formatearHora, aInstanteUtc), intervalo.ts
  shared/ui/      Boton, Campo, BadgeEstadoTurno, Tabla, Toast, Tarjeta, Alerta, Icono (.tsx), ui.css, index.ts
  app/rutas/      tipos.ts (RutaFeature, EntradaMenu), registro.ts (construirRegistro), modulos.ts (import.meta.glob)
  app/            AppRutas.tsx, Layout.tsx, PaginaNoEncontrada.tsx, layout.css
  features/inicio/ routes.ts, PaginaInicio.tsx
```

### D2. Cliente de la API sobre `fetch`
- La función genérica es `peticion<T>(metodo, ruta, cuerpo?)`, con atajos `api.get`, `post`, `put`, `patch` y `delete`. Usa `credentials: "same-origin"` (DD-03: la cookie viaja por el mismo origen gracias al proxy) y `Accept: application/json`. Envía `Content-Type: application/json` solo cuando hay cuerpo. Un 204 resuelve `undefined`.
- **CSRF:** en los métodos que no son GET, HEAD ni OPTIONS, lee `document.cookie` al momento de enviar, así siempre usa el token vigente aunque haya rotado con el login. Si no hay cookie, pide `GET /api/auth/csrf` una sola vez, compartiendo la promesa entre escrituras concurrentes, y vuelve a leerla.
- *Alternativa descartada:* axios o ky. Agregan una dependencia y obligarían a editar `package.json`, y `fetch` alcanza.
- *Alternativa descartada:* guardar el token en memoria. Se desincroniza cuando el backend lo rota.

### D3. Errores tipados
`ErrorApi extends Error` con `tipo`, `estado`, `detalle`, `codigo?`, `campos` (`{campo, mensaje}[]`) y `propiedades` (el resto del cuerpo, por ejemplo `recurso`). Tabla de mapeo: 400 → `validacion`, 401 → `no-autenticado`, 403 → `prohibido`, 404 → `no-encontrado`, 409 → `conflicto`, 422 → `no-procesable`, ≥500 → `servidor`. Otro 4xx → `cliente`. Si `fetch` rechaza → `red`. Si el cuerpo no es Problem Details, el mensaje es genérico y no se muestra texto crudo del servidor (RN-35).

### D4. Fechas sin desfase fijo (RN-14)
- El formateo usa `Intl.DateTimeFormat("es-AR", { timeZone: "America/Argentina/Buenos_Aires", hourCycle: "h23" })` con `formatToParts`, y arma `dd/mm/aaaa` y `HH:mm` a mano para no depender de variaciones del locale.
- Para pasar de hora local a UTC, `desfase(instante)` se calcula reinterpretando como UTC las partes locales de ese instante. Luego `estimado = Date.UTC(local)`, `resultado = estimado − desfase(estimado)`, y se recalcula una vez con `desfase(resultado)` por si la regla cambiara entre las dos fechas. No hay `-03:00` en el código.
- El intervalo es un objeto inmutable `{inicio, fin}` con instantes en milisegundos, más `crearIntervalo`, `seSolapan` y `contiene` como funciones puras. Replica `Intervalo` del backend (KB 08).
- Los tests corren también con `process.env.TZ` forzado a otra zona, para probar que no dependen de la zona de la máquina.

### D5. Kit UI
- Componentes funcionales tipados que reciben las props nativas del elemento (`ComponentProps<"button">`, etc.) y suman las clases del design system. Las clases se copian de `components.html` (`btn`, `field`, `badge st-*`, `table-wrap`, `toast`, `panel`, `alert`).
- **Tokens solamente (regla 13):** `components.html` tiene tres colores fijos: el texto del botón de peligro y el fondo y el texto del toast. Como no se editan `tokens.css` ni `components.html`, en `ui.css` se reemplazan por tokens existentes. El texto del botón de peligro usa `--color-on-primary`. El toast va invertido, con fondo `--color-text` y texto `--color-surface`; lo mismo para su acción y su ícono. Un test recorre `shared/ui/*.css` y `app/*.css` y falla ante un color fijo o una variable que no esté definida.
- **Estados de turno:** `PROGRAMADO` se muestra como "Reservado" con `st-reservado`, como en el design system. También están `CANCELADO_TARDE` ("Cancelación tardía") y `BLOQUEO`. Los íconos son un `<svg aria-hidden>` en línea con los paths de `components.html`.
- `Campo` genera ids con `useId` y conecta `aria-describedby` y `aria-invalid`.

### D6. Rutas por feature
- `features/<x>/routes.ts` exporta `rutas: RutaFeature[]`, donde `RutaFeature = { ruta, componente: ComponentType, menu?: { etiqueta, orden }, roles?: readonly Rol[] }`. Es un `.ts` sin JSX: el componente puede ser `lazy(() => import("./Pagina"))` para dividir el bundle.
- `construirRegistro(modulos)` es una función pura. Recibe el objeto de `import.meta.glob` y devuelve `{ rutas, menu }`, lanza un error ante rutas duplicadas y ordena el menú por `orden` y luego por etiqueta.
- `modulos.ts` es el único lugar con `import.meta.glob("../../features/*/routes.ts", { eager: true })`. Con eso alcanza para que una feature nueva aparezca sola.
- `AppRutas` recibe el registro y monta `<Routes>` con un `Layout` como ruta padre (`<Outlet/>`), cada ruta envuelta en `<Suspense>` y un `*` que lleva a `PaginaNoEncontrada`.
- `App.tsx` envuelve `AppRutas` en `BrowserRouter`. Los tests usan `MemoryRouter`.
- **Criterio de salida:** la feature de ejemplo vive en `src/app/rutas/__fixtures__/features/ejemplo/routes.ts`, así no se publica en la app. El test pasa ese glob a `construirRegistro` y comprueba que la ruta aparece en el router y en el menú sin tocar archivos centrales.
- La feature real `inicio` (ruta `/`, menú "Inicio", orden 0) prueba el glob de producción.
- **Encabezado:** el nombre de `BRAND` va como `<h1>` en el layout, y los títulos de las pantallas serán `<h2>`. Así se conservan sin cambios los tests de C-02 ("La App muestra el nombre de la marca tomado de BRAND").

### D7. Tests
Todos los `it(...)` usan el nombre exacto del escenario. Fechas, intervalo, mapeo de errores y registro se prueban como funciones puras. El cliente se prueba con `fetch` simulado (`vi.fn`) y `document.cookie`. Los componentes se prueban con Testing Library y `user-event`. El layout y las rutas se prueban con `MemoryRouter`. El criterio de cierre es `npm run lint`, `npm test` y `npm run build`.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy) — 2026-11-21. `design-system/README.md` y `.active-orchestrator-state.json` están en modo `system-only`: el sistema está aprobado y no hay gate de mockups por change.
- Mockups: no aplica. La referencia visual es `design-system/components.html`.

## Risks / Trade-offs

- [El glob eager carga todos los `routes.ts` al inicio] → son módulos chicos. Las pantallas pesadas se declaran con `lazy`.
- [Leer `document.cookie` en cada escritura] → costo despreciable, y a cambio el token siempre está vigente.
- [Los colores invertidos del toast difieren levemente de la referencia] → mantiene el contraste en modo claro y oscuro con tokens existentes. Si se quiere un color propio, se agrega como token en C-50.
- [`roles` reservado sin efecto] → queda documentado que lo aplica C-16. Hasta entonces no restringe nada, y el backend sigue siendo la barrera (RN-33).

## Open Questions

- Las pantallas públicas (`/reservar`, `/turno/:token`, `/oferta/:token`) no deberían mostrar la navegación del personal. C-48 o C-49 pueden agregar a `RutaFeature` un campo opcional, por ejemplo `layout: "publico"`, sin romper lo existente. No cambia nada de C-09.

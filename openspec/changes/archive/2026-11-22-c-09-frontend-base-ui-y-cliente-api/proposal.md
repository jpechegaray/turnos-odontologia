# Proposal

## Why

C-02 dejó una SPA vacía: solo muestra el nombre de la marca. Cada pantalla que viene después (login en C-16, agenda en C-40, reserva pública en C-48 y C-49, los fronts de catálogos en C-20 a C-24) necesita lo mismo. Necesita hablar con la API con la cookie de sesión y el CSRF de C-03, entender los errores Problem Details, mostrar horarios en la zona de Buenos Aires y usar componentes del design system. Además, cada pantalla tiene que poder agregarse sin editar un router central. C-09 construye esa base una sola vez, antes de que los demás changes la dupliquen o choquen en los mismos archivos.

## What Changes

- **Cliente de la API:** una función de petición única que usa la base configurable de C-02 y envía la cookie de sesión. En las escrituras, obtiene y envía el token CSRF como espera el backend de C-03. Traduce las respuestas Problem Details a errores tipados, con su estado, `codigo`, campos inválidos y `recurso`.
- **Fechas y zona horaria (RN-14):** formateo y conversión de instantes UTC en `America/Argentina/Buenos_Aires` sin asumir un desfase fijo, e intervalos semiabiertos `[inicio, fin)` con solapamiento y contención.
- **Kit UI:** botón, campo, badge de estado de turno, tabla, toast, tarjeta, alerta y layout con navegación. Se toman de `design-system/components.html` y usan solo variables de `tokens.css`.
- **Rutas por feature:** cada `features/*/routes.ts` declara sus rutas y sus entradas de menú, y el router y el menú las incorporan solos. Ningún change posterior necesita editar un archivo central para sumar una pantalla.
- Fuera de alcance: login, guards por rol y manejo de sesión vencida en la UI (C-16); pantallas de negocio (C-20 en adelante); identidad visual final (C-50).

## Capabilities

### New Capabilities

- `cliente-api-frontend`: cómo la SPA llama a la API, con sesión, CSRF y errores tipados a partir de Problem Details.
- `fechas-frontend`: presentación y conversión de horarios en la zona de Buenos Aires, e intervalos semiabiertos.
- `kit-ui`: componentes visuales reutilizables del design system, con accesibilidad y sin valores visuales fijos.
- `rutas-por-feature`: registro automático de rutas y entradas de menú por feature, con el layout de navegación que las muestra.

### Modified Capabilities

- Ninguna. Los requisitos de `entorno-frontend` (marca tomada de `BRAND`, tokens aplicados, base de la API configurable, estructura por feature) se mantienen y se siguen cumpliendo.

## Impact

- `frontend/src/shared/` (`api/`, `fecha/`, `ui/`), `frontend/src/app/` (registro de rutas y layout) y una feature de inicio. Tests con Vitest y Testing Library.
- **Archivos existentes:** solo se modifica `frontend/src/App.tsx`, para montar el router y el layout. Es estrictamente necesario porque hoy la App solo renderiza un título. `package.json` no cambia: `react-router-dom` ya está instalado. No se tocan `design-system/`, `vite.config.ts`, `main.tsx` ni el backend.
- Contrato interno que consumirán C-16, C-20 a C-24, C-40, C-45 y C-48 a C-49.
- Gobernanza LOW: autonomía si los tests pasan.

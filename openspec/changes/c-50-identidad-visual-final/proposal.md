# Proposal

## Why

El design system aprobado (Revisión 1) dejó en `(TBD)` el nombre del producto, el logo, el color primario y la tipografía, y la navegación es una barra superior con enlaces de texto. Con todo el front del MVP hecho (Olas 3 a 7), el equipo ya decidió la identidad: C-50 la carga en el design system y la aplica a cada pantalla antes del cierre del MVP (C-51).

## What Changes

- Nombre del producto "OdontologIA", con el subtítulo "Sistema de gestión", en `design-system/brand.js`.
- Logo final en `design-system/assets/logo.svg`: contorno de diente con un destello y la palabra "OdontologIA" con "IA" en un verde más claro. Se quita el rótulo "LOGO PROVISIONAL".
- Paleta verde bosque (`#1B3A2F`) y crema (`#F5F1E8`), con neutros cálidos, info en salvia y variante oscura en sus dos bloques. Se suman tokens para la navegación lateral, el color por profesional, la línea de "ahora" de la agenda y los radios nuevos.
- Tipografía Public Sans, autoalojada desde el paquete npm `@fontsource-variable/public-sans` (sin fuentes remotas).
- **BREAKING (visual):** el layout del personal pasa de una barra superior a un menú lateral verde con íconos y grupos (Atención, Consultorio, Administración) en escritorio, y en celular a una barra superior con un botón "Menú" que despliega el mismo menú.
- Inicio muestra las secciones como recuadros en el centro de la pantalla y un resumen del día con los datos de `GET /api/agenda`: cifras de turnos por estado y los próximos turnos. No se agrega ningún endpoint.
- Se revisa cada pantalla con la nueva identidad: ingresar, agenda con sus acciones, pacientes, lista de espera, profesionales con horarios, bloqueos y prestaciones, sillones, prestaciones, usuarios, configuración, notificaciones, reserva pública, turno por token y oferta. Se usa como referencia la exploración de diseño aprobada por el líder.
- El design system sube a `Revision: 2` con `Approval: pending` hasta que lo apruebe Juan Pablo. Se actualizan `README.md`, `brand.md`, `components.html` y las capturas.

## Capabilities

### New Capabilities

- `identidad-visual`: nombre, logo, tipografía autoalojada y paleta final del producto, con el contraste mínimo exigido.
- `inicio-frontend`: pantalla de inicio del personal con el resumen del día tomado de la agenda.

### Modified Capabilities

- `rutas-por-feature`: el layout con navegación pasa a un menú lateral agrupado con íconos en escritorio y a un menú desplegable en celular.

## Impact

- Frontend: `app/Layout.tsx` y su CSS, `features/inicio/`, los CSS de cada feature, el kit UI (`shared/ui`) y `main.tsx` para cargar la fuente.
- Design system: `tokens.css`, `brand.js`, `brand.md`, `assets/logo.svg`, `components.html`, `README.md` y `previews/`.
- Dependencias: `@fontsource-variable/public-sans` (fuente con licencia OFL, se empaqueta con el build).
- Backend: sin cambios. Los emails son de texto plano y no llevan colores.
- Las rutas, los permisos y las llamadas a la API no cambian.

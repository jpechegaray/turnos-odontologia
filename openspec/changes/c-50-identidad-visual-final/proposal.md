# Proposal

## Why

El design system aprobado (Revisión 1) dejó en `(TBD)` el nombre del producto, el logo, el color primario y la tipografía, y la navegación es una barra superior con enlaces de texto. Con todo el front del MVP hecho (Olas 3 a 7), el equipo ya decidió la identidad: C-50 la carga en el design system y la aplica a cada pantalla antes del cierre del MVP (C-51).

## What Changes

- Nombre del producto "OdontologIA", con el subtítulo "Sistema de gestión", en `design-system/brand.js`.
- Logo final en `design-system/assets/logo.svg`: contorno de diente con un destello y la palabra "OdontologIA" con "IA" en un verde más claro. Se quita el rótulo "LOGO PROVISIONAL".
- Paleta verde bosque (`#1B3A2F`) y crema (`#F5F1E8`), con neutros cálidos, info en salvia y variante oscura en sus dos bloques. Se suman tokens para la navegación lateral, el color por profesional, la línea de "ahora" de la agenda y los radios nuevos.
- Tipografía Public Sans, autoalojada desde el paquete npm `@fontsource-variable/public-sans` (sin fuentes remotas).
- El layout del personal usa el menú lateral verde agrupado en escritorio y la navegación inferior de la propuesta en celular. La barra superior abre la búsqueda de pacientes y el alta de turno según el rol.
- Inicio muestra el resumen del día, los próximos turnos, la ocupación de sillones y los accesos rápidos. Los datos provienen de las consultas existentes de agenda, sillones y lista de espera.
- Se revisa cada pantalla con la nueva identidad: ingresar, agenda con sus acciones, pacientes, lista de espera, profesionales con horarios, bloqueos y prestaciones, sillones, prestaciones, usuarios, configuración, notificaciones, reserva pública, turno por token y oferta. Se usa como referencia la exploración de diseño aprobada por el líder.
- El design system sube a `Revision: 2` con `Approval: pending` hasta que lo apruebe Juan Pablo. Se actualizan `README.md`, `brand.md`, `components.html` y las capturas.

## Capabilities

### New Capabilities

- `identidad-visual`: nombre, logo, tipografía autoalojada y paleta final del producto, con el contraste mínimo exigido.
- `inicio-frontend`: pantalla de inicio del personal con el resumen del día tomado de la agenda.

### Modified Capabilities

- `rutas-por-feature`: menú lateral agrupado en escritorio, barra superior de acciones y navegación inferior en celular con el acceso "Más" al menú completo.

## Impact

- Frontend: `app/Layout.tsx` y su CSS, `features/inicio/`, los CSS de cada feature, el kit UI (`shared/ui`) y `main.tsx` para cargar la fuente.
- Design system: `tokens.css`, `brand.js`, `brand.md`, `assets/logo.svg`, `components.html`, `README.md` y `previews/`.
- Dependencias: `@fontsource-variable/public-sans` (fuente con licencia OFL, se empaqueta con el build).
- Backend: sin cambios. Los emails son de texto plano y no llevan colores.
- Se conservan las rutas, los permisos y los contratos de la API.

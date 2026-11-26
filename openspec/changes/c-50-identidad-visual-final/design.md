# Design

## Context

El motivo está en proposal.md. El design system está en la Revisión 1, aprobada, con nombre, logo, primario y tipografía en `(TBD)`. `main.tsx` importa `design-system/tokens.css` y `brand.js`. `app/Layout.tsx` es una barra superior con los `NavLink` del menú que arma el registro de rutas (`etiqueta` y `orden` en cada `routes.ts`) y el botón `CerrarSesion`. Los tests de estilos (`estilosGlobales`, `estilosKitUi`, `estilosAgenda`) rechazan colores fijos y variables que no estén en `tokens.css`. La exploración de diseño aprobada por el líder (`global/exploracion-diseno/propuesta/`, fuera del repo) tiene los mockups HTML de cada pantalla con la paleta bosque y crema, y es la referencia visual de este change. Los emails se arman en texto plano (`PlantillaNotificacion`), sin HTML ni colores.

## Goals / Non-Goals

**Goals:** cargar la identidad final en el design system y aplicarla a todas las pantallas, pantalla por pantalla, sin cambiar rutas, permisos ni llamadas a la API (salvo inicio, que pasa a leer la agenda).

**Non-Goals:** endpoints nuevos. Ocupación de sillones y contador de la lista de espera en inicio, porque necesitarían cálculo nuevo. Buscador global y botón "Nuevo turno" en la barra superior de los mockups, porque no hay API de búsqueda global. Emails en HTML. Selector de tema claro u oscuro en la SPA, ya que el modo oscuro sigue al sistema.

## Decisions

1. **Tokens de la Revisión 2 en un solo archivo.** Los valores de `tokens-bosque.css` de la exploración se vuelcan en `design-system/tokens.css`, sin una hoja que pise a otra. Se conservan todos los nombres de la Revisión 1 y se suman `--color-primary-softer`, `--nav-*`, `--prof-1..5-bg/fg`, `--agenda-ahora`, `--text-3xl`, `--tracking-*` y `--ease-drawer`. Los radios pasan a 6, 10 y 14 px. Alternativa descartada: mantener la hoja de override, porque duplica valores y confunde la regla 13.
2. **Fuente autoalojada con Fontsource.** `@fontsource-variable/public-sans` se importa en `main.tsx`. Vite copia los `woff2` al build, así que no hay pedidos a Google Fonts. `--font-sans` queda `"Public Sans Variable", system-ui, …, sans-serif`. `components.html` carga el mismo archivo desde `frontend/node_modules`, con el stack de sistema como respaldo si no está instalado. Alternativa descartada: copiar los `woff2` a `design-system/assets/`, porque sus actualizaciones no quedarían versionadas en `package.json`.
3. **Logo como SVG con texto vectorizado.** El SVG dibuja el diente y el destello con trazos y el wordmark con `<text>` en Public Sans, con colores de la marca escritos en el propio SVG. Un archivo de imagen no puede leer variables CSS, y la regla 13 aplica al código de la SPA, no a los assets. El layout lo usa con `img` y `alt=""`, porque el nombre ya está escrito al lado.
4. **Grupo del menú declarado por la feature.** `EntradaMenu` suma `grupo?: "atencion" | "consultorio" | "administracion"` e `icono?: NombreIcono`. Sin grupo, la entrada cae en Atención. Así se sigue sin editar un menú central (requisito de registro automático). Los íconos de navegación se suman al mismo `Icono` del kit UI.
5. **Menú desplegable en celular y recuadros en Inicio.** A pedido del líder (26/11) no hay barra inferior: en celular, una barra superior con la marca y un botón "Menú" abre el mismo menú lateral como panel (`aria-expanded`, se cierra con Escape y al navegar). Inicio muestra las secciones permitidas como recuadros en el centro, así el menú está a la vista al entrar. El layout le pasa el menú ya filtrado por rol a la pantalla con el contexto del `Outlet`. El corte es a 900 px.
6. **Inicio reutiliza la consulta de la agenda.** Inicio usa `consultarAgenda({ vista: "dia" })` de la feature agenda. "Ahora" sale de `Date.now()`, comparado con el `inicio` de cada turno, porque los dos son instantes y no depende de la zona del navegador. La hora se muestra en Buenos Aires con `shared/fecha`. Los tests fijan el reloj con `vi.setSystemTime`.
7. **Un commit por pantalla.** El orden es: base (tokens, fuente, logo, layout), ingresar, inicio, agenda, pacientes, lista de espera, profesionales, sillones y prestaciones, usuarios, configuración y notificaciones, pantallas públicas y cierre. Cada pantalla se muestra corriendo en la app real a 390x844 y 1440x900 y se aprueba antes del commit.
8. **Emails sin cambios.** La ficha pedía copiar los hex a las plantillas de email, pero los emails son de texto plano, así que no hay colores que copiar. Se registra en el README del design system.

## Risks / Trade-offs

- [Los CSS de cada feature tienen valores pensados para la paleta azul] → se revisa cada pantalla contra su mockup de la exploración. Los tests de estilos siguen exigiendo solo tokens.
- [Public Sans suma unos 40 KB por subconjunto latino] → solo se importa el subconjunto latin (`wght.css`); el resto de los subconjuntos no se empaqueta.
- [Contraste de los tokens nuevos] → un test calcula el contraste de los pares principales en claro y oscuro (spec `identidad-visual`).
- [El cambio visual toca todo el front] → C-50 es la última ola y no hay otro change de front en vuelo.

## Migration Plan

No hay migración de datos. Para volver atrás alcanza con revertir los commits del PR, porque los tokens, el layout y las pantallas no cambian contratos con el backend.

## UI Mockups

- Design system revision: 2 (pending)
- Decision: provided — 2026-11-25. Política `system-only`; como referencia se usa la exploración de diseño aprobada por el líder en `global/exploracion-diseno/propuesta/` (fuera del repo), con capturas en `propuesta/previews/`.
- Mockups: `global/exploracion-diseno/propuesta/*.html`
- Approval: approved (2026-11-25, by user) para la paleta bosque y crema y el menú lateral verde; cada pantalla se aprueba en la app real antes de su commit.
- Preview: rendered (2026-11-25, `propuesta/previews/`)
- Visual verify: pendiente

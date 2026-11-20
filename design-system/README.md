# Design system - Turnos Odontología (provisional)

Mode: system-only
Revision: 1
Approval: approved (2026-11-20, by user — Juan Pablo, líder). Nombre, logo, color primario y tipografía finales quedan (TBD) y se resuelven en un change del roadmap dedicado al diseño final.

Fundación visual del sistema de turnos para consultorios odontológicos. Los tokens se consumen como variables CSS desde el front (React + TypeScript, Vite). Todo es local: sin fuentes remotas, sin servicios externos.

## Política

`system-only`: el sistema visual se aprueba una vez y los changes de front lo usan directamente. **No se piden mockups por change.** Cada change visual debe respetar estos tokens y componentes. Un cambio de política (a `system-and-mockups` u `off`) solo se hace por pedido explícito del líder. Un cambio en un token compartido sube `Revision` y vuelve `Approval` a `pending`.

## Fuentes

- `knowledge-base/01_vision_y_objetivos.md`: propósito, producto single-tenant, usuarios y referencias de UX.
- `knowledge-base/03_actores_y_roles.md`: recepción, odontólogo (agenda diaria), administrador y paciente sin login con token.
- Tono, estados de turno, bloqueo y hueco libre: pedido del líder para esta tarea.

## Contenido

| Archivo | Para qué |
|---|---|
| `tokens.css` | Color, tipografía, espaciado, radios, sombras, foco, motion. Modo oscuro por `prefers-color-scheme`. |
| `brand.js` | Constante con el nombre y binding a `[data-brand-name]`. |
| `brand.md` | Nombre, tono, reglas de logo e imágenes. |
| `assets/logo.svg` | Logo placeholder rotulado "LOGO PROVISIONAL". |
| `components.html` | Referencia de componentes (botones, campos, badges, tarjeta, agenda, alerta, reserva pública mobile, toast, tabla). Abrir en el navegador; `?theme=light` o `?theme=dark` fuerza el tema. |
| `previews/` | Capturas 1440x900 y 390x844 (y páginas completas, clara y oscura). |

## Decisiones abiertas (TBD)

- **Nombre del producto (TBD).** Provisional: "Turnos Odontología (provisional)".
- **Logo (TBD).** Placeholder visible, no es marca aprobada.
- **Color primario (TBD):** `#0B5F8A` propuesto, sin marca previa que lo respalde.
- **Tipografía final (TBD):** hoy es el stack de sistema (sin descarga). Si el equipo adopta una fuente, se declara en `--font-sans` y se autoaloja.
- **Variante de colores del email de recordatorio (TBD):** los clientes de correo no leen variables CSS; hay que copiar los hex de `tokens.css` en línea.

## Reemplazos exactos

- **Nombre:** editar `BRAND_NAME` en `brand.js`. Todo elemento con `data-brand-name` se actualiza; también el `alt` de `img[data-brand-logo]`. En React, importar `BRAND.name` o repetir la constante en un módulo `brand.ts`. Actualizar además el `<title>` de `components.html`.
- **Logo:** reemplazar `assets/logo.svg` conservando el nombre de archivo y el `<title>` accesible. Quitar el borde punteado y el rótulo "LOGO PROVISIONAL".
- **Valores visuales:** editar solo variables de `tokens.css`. Si cambia `--color-primary`, recalcular contraste de texto blanco (>= 4.5:1), actualizar `--focus-color` y la variante oscura (dos bloques: `@media (prefers-color-scheme: dark)` y `[data-theme="dark"]`, mantenerlos iguales).

## Consumir los tokens desde React/Vite

```ts
// src/main.tsx
import "../design-system/tokens.css"; // o copiar el archivo a src/styles/
```

```css
.boton-primario {
  background: var(--color-primary);
  color: var(--color-on-primary);
  min-height: var(--target-min);
  border-radius: var(--radius-md);
  transition: transform var(--dur-fast) var(--ease-out);
}
.boton-primario:focus-visible { outline: var(--focus-outline); outline-offset: var(--focus-offset); }
.turno--confirmado { background: var(--turno-confirmado-bg); color: var(--turno-confirmado-fg); border-color: var(--turno-confirmado-border); }
```

Reglas de uso:

- Un estado de turno siempre se muestra con color + icono + texto (ver `components.html`). Cancelado además va tachado; bloqueo va rayado; hueco libre va punteado.
- Objetivos táctiles de 44 px mínimo (público y recepción).
- Animar solo `transform` y `opacity`, menos de 300 ms, `ease-out`. No animar acciones de teclado repetidas (agenda). Respetar `prefers-reduced-motion` (ya resuelto en `tokens.css`).
- Los números (horas, duraciones) en `--font-mono` con cifras tabulares.
- Cualquier color nuevo debe pasar 4.5:1 para texto antes de agregarse.

## Contraste y verificación

Pares principales: texto sobre superficie 14.6:1, texto atenuado 7.0:1, blanco sobre primario 6.9:1, blanco sobre peligro 6.5:1. Los pares fg/bg de cada estado de turno fueron elegidos para superar 4.5:1 (cálculo manual aproximado; correr un verificador automático antes de la aprobación final).

Captura revisada en Edge headless (claro y oscuro). Limitación: Edge headless no baja de unos 500 px de ancho de ventana, así que las capturas móviles se hicieron con la página en un iframe de 390 px de ancho (media queries reales de 390). No hay desbordamiento horizontal de página; la agenda y la tabla desplazan dentro de su contenedor.

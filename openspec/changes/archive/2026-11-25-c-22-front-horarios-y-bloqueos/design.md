# Design

## Context

- El back de C-11 (`horarios-de-atencion`) y el de C-12 (`bloqueos-de-agenda`) ya están en `master`. Los dos cuelgan de `/api/profesionales/{id}/…`: escriben `ADMIN` y `RECEPCION`, y `ODONTOLOGO` solo lista lo de su profesional vinculado (si no, recibe `403`). Los horarios usan `diaSemana` ISO de 1 a 7 y `HH:mm`. Los bloqueos usan instantes ISO con offset y la API los devuelve en UTC. El back no tiene una operación de modificar bloqueo.
- C-21 dejó `/profesionales` con un punto de extensión `panel` en `PaginaProfesionales`: una acción de fila abre un componente que recibe `PanelProfesionalProps` (`profesional`, `refrescarLista`, `informarError`), montado con `key` por id. Ese punto admite **un solo** panel («Prestaciones»). Para los roles de consulta, la acción vive en la columna «Detalle», y para `ADMIN`, en `AccionesFila` como `extra`.
- En el front ya existen `shared/fecha` (`aInstanteUtc`, `formatearFecha`, `formatearHora`, `seSolapan`), el kit de UI (`Alerta`, `Boton`, `Campo`, `Tabla`, `Toast`) y el patrón de C-21: un contador de solicitudes que descarta respuestas viejas y un módulo `errores.ts` que traduce por código y devuelve una `reaccion`.
- El permiso existente `useEsAdministrador` no sirve acá, porque en este change también escribe `RECEPCION`.

## Goals / Non-Goals

**Goals:**
- Dos paneles nuevos (`horarios/`, `bloqueos/`) dentro de `features/profesionales/`, con la misma forma que `PanelPrestaciones`.
- Validación local en funciones puras y testeables por separado.
- Cambio mínimo en C-21: solo el punto de extensión.

**Non-Goals:**
- Editar un bloqueo existente: el back no lo permite y se borra y se vuelve a crear.
- Una vista de calendario o grilla semanal: queda para C-40 y C-50.
- Filtrar bloqueos pasados: se muestran todos, en el orden de la API.
- Cambios en el back o en la API.

## Decisions

1. **`panel` pasa a `paneles: readonly PanelDefinicion[]`**. El estado de la página guarda `{ etiqueta, profesionalId }` y monta un solo panel a la vez, con `key` `${etiqueta}-${id}`, así que cambiar de panel o de profesional reinicia su estado. Cada fila muestra un botón por panel, en el orden de la lista: Prestaciones, Horarios, Bloqueos.
   - *Alternativa descartada:* varios paneles abiertos a la vez. Con eso la página se vuelve larga y se pierde contra qué profesional se está escribiendo.
   - *Alternativa descartada:* una ruta aparte `/profesionales/agenda`. Duplica la carga de profesionales y el manejo de `sinVinculo` (el usuario eligió los paneles).
   - El panel abierto se marca con `aria-pressed` en su botón, para que se sepa cuál está activo.
2. **Permiso nuevo `usePuedeGestionarAgenda()`** en `features/profesionales/permisos.ts` (`ADMIN` o `RECEPCION`), junto a `useEsAdministrador`. La UI solo oculta controles: la API sigue siendo la que autoriza, y un `403` se informa sin cerrar la sesión.
3. **Validación pura.**
   - `horarios/validacion.ts`: `validarTramo(tramo, existentes, idEditado?)` pasa `HH:mm` a minutos y reutiliza `seSolapan` de `shared/fecha/intervalo` con intervalos en minutos. Devuelve el primer error, con el tramo en conflicto si lo hay.
   - `bloqueos/validacion.ts`: `validarBloqueo(formulario)` usa `aInstanteUtc` para comparar inicio y fin, recorta y mide el motivo (de 1 a 255 caracteres) y arma el cuerpo con `toISOString()`.
4. **`horarios/dias.ts`** define los días ISO 1 a 7 con su nombre en español y `agruparPorDia(tramos)`. Esa función siempre devuelve los siete días y ordena cada uno por `horaInicio` y después por `id`. El orden no depende de la API.
5. **Después de cada escritura se vuelve a consultar.** Así se evita reconstruir localmente lo que la API normaliza (UTC, orden) y queda alineado con los escenarios. Un contador descarta las respuestas viejas, igual que en C-21.
6. **Errores por código** en `horarios/errores.ts` y `bloqueos/errores.ts`, con la forma `{ mensaje, reaccion }` de `prestaciones/errores.ts`. Las reacciones son `recargar`, `refrescarLista`, `conservar` y `ninguna`. Se reutilizan `MENSAJE_PERMISO`, `MENSAJE_GENERICO` y `MENSAJE_NO_ENCONTRADO`. Un `409` o `422` en bloqueos usa `conservar`: el formulario queda con lo ingresado. Esto ya cubre el rechazo por turnos superpuestos que va a sumar C-30 (RN-17).
7. **Confirmación en línea**, con «Confirmar eliminación» y «Cancelar» en el propio ítem, como la baja en `AccionesFila`. No se usa un modal: el kit no tiene uno y se mantiene el patrón de C-21.
8. **Formularios.**
   - Horarios: un `<select>` de día, dos `Campo` `type="time"` y los botones «Agregar tramo» o «Guardar cambios» y «Cancelar». Editar precarga el formulario con el tramo elegido.
   - Bloqueos: `Campo` `type="date"` y `type="time"` para inicio y para fin, y un `Campo` de motivo con `maxLength` 255.
   - Los estilos usan `design-system/tokens.css` en un `panel-agenda.css` compartido. Para el `<select>` se reutiliza la clase de campo del kit si existe; si no, una clase con tokens.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy), 2026-11-25. `visual_design.mode` es `system-only`: se usa el sistema aprobado sin mockup por change.
- Mockups: no aplica.
- Approval: no aplica.
- Preview: no aplica.
- Visual verify: pendiente para la verificación (revisión manual contra los tokens y los componentes del kit).

## Risks / Trade-offs

- [Cambiar `panel` por `paneles` rompe los tests de C-21 que montan la página con `panel`] → Se ajustan esos tests en el mismo commit y se agrega uno que prueba los tres botones y que solo hay un panel abierto.
- [El orden por `id` en el agrupado puede diferir del de la API cuando dos tramos empatan en hora de inicio] → No es posible, porque la API rechaza tramos superpuestos del mismo día, y el empate solo podría darse en datos inválidos.
- [`input type="date"` y `type="time"` se ven distinto según el navegador] → Se aceptan los controles nativos. Los tests usan el valor `aaaa-mm-dd` y `HH:mm`, no el texto visible.
- [El odontólogo sin vínculo no ve filas, así que no hay paneles] → Es correcto: la página ya muestra el aviso `sinVinculo` de C-21.
- [C-21 sigue sin archivar] → Las specs nuevas no lo modifican. Si al archivar C-21 su spec menciona un único panel, se ajusta en ese momento sin cambiar comportamiento.

## Migration Plan

Solo cambia el front y no hay datos que migrar. Se despliega con el merge a `master`. Para volver atrás, se revierte el PR.

# Design

## Context

La motivación está en `proposal.md`. El frontend ya cuenta con estas piezas:

- La sesión y las guardas de C-16.
- El cliente HTTP con CSRF de C-09.
- El kit UI de `shared/ui`: `Alerta` (`peligro` | `info`), `Boton`, `Campo`, `Tabla`, `Tarjeta` y `Toast`.
- El patrón de pantalla de administración que C-20 aplicó en `features/prestaciones`.

`features/profesionales` solo contiene `.gitkeep`. Los contratos de C-05, C-06 y C-10 ya están mergeados, así que este change no toca el backend.

Restricciones que condicionan el diseño:

- `app/rutas/modulos.ts` descubre las rutas con `import.meta.glob("../../features/*/routes.ts")`, que mira **un solo nivel**. `construirRegistro` junta los módulos y falla si una ruta está repetida.
- C-22 trabaja en la misma carpeta `features/profesionales` (horarios y bloqueos, RM-09).
- `clienteApi` lanza `ErrorApi` con `estado`, `codigo` y `campos`. C-16 no cierra la sesión ante un `403`.
- Sillones y profesionales no tienen `DELETE`. La baja lógica y la reactivación se hacen con `PUT` y `activo` explícito.
- Para `ODONTOLOGO`, `GET /api/profesionales` devuelve solo el registro vinculado. Si no hay vínculo, responde `403`. El vínculo lo resuelve el servidor (C-13).
- El `design-system` está en modo `system-only`, revisión 1: solo se usan tokens de `tokens.css`.

## Goals / Non-Goals

**Goals:**

- Dos pantallas internas, `/profesionales` y `/sillones`, más el panel de prestaciones por profesional. Todo se registra por feature y no se edita ningún archivo central.
- Dejar a C-22 la posibilidad de sumar sus rutas sin tocar archivos de C-21.
- Reutilizar sesión, guardas, cliente HTTP, kit UI, fixture de tests y tokens. No se agregan dependencias.

**Non-Goals:**

- Modificar endpoints, DTOs o permisos del backend.
- Editar el código de C-20, salvo lo que exija el registro de rutas (nada previsto).
- Vincular usuarios con profesionales (C-13), horarios y bloqueos (C-22) y la guarda de RN-37 (C-30).
- Crear un sistema de formularios o una librería de validación, o agregar un checkbox al kit UI compartido.

## Decisions

1. **Una sola feature `profesionales`, con subcarpetas y un `routes.ts` que agrupa las de sus hijas.** `features/profesionales/routes.ts` exporta `rutas` concatenando lo que encuentra `import.meta.glob<ModuloRutas>("./*/routes.ts", { eager: true })`, ordenado por archivo. Las subcarpetas son `profesionales/` (pantalla), `sillones/` (pantalla) y `prestaciones/` (panel sin ruta propia). C-22 podrá sumar `horarios/routes.ts` y `bloqueos/routes.ts` sin editar este archivo. `estructura.test.ts` solo exige que exista la carpeta `features/profesionales`, y se cumple. El `.gitkeep` se elimina. Las rutas repetidas siguen detectándose en `construirRegistro`. Alternativas descartadas:
   - Un `routes.ts` con la lista escrita a mano, que obligaría a C-22 a editarlo y generaría conflictos de merge.
   - Cambiar el glob de `modulos.ts` a dos niveles, que toca un archivo central y cambia el contrato de C-09.
   - Crear features hermanas `features/sillones`, que se apartan de la ficha de C-21 y de la estructura de la KB.

2. **Rutas internas para los tres roles, con formularios en la misma pantalla.** Las rutas son:
   - `/profesionales`, con `menu: { etiqueta: "Profesionales", orden: 30 }`.
   - `/sillones`, con `menu: { etiqueta: "Sillones", orden: 40 }`.

   Ambas declaran `roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"]`. El alta y la edición se hacen en una `Tarjeta` de la misma página, y el panel de prestaciones se abre debajo de la tabla al elegir un profesional. No existen URLs de escritura, igual que en C-20 (decisión 2). Alternativa descartada: rutas hijas `/profesionales/:id`, que habría que proteger por separado y que C-22 podría necesitar con otra forma. Se decide cuando exista esa necesidad.

3. **El control de escritura tiene dos capas y un hook propio de la feature.** La ruta define quién ve la pantalla. Adentro, un `useEsAdministrador()` en `features/profesionales/permisos.ts` decide si se muestran los formularios, las acciones de fila y los controles del panel. Repite la lógica de `features/prestaciones/permisos.ts` (dos líneas) para no editar C-20 ni crear un acoplamiento entre features. Para `RECEPCION` y `ODONTOLOGO` se ocultan los controles; no se deshabilitan. La API conserva `@PreAuthorize`. Un `403` en una escritura se muestra como «No tenés permiso para realizar esta acción.» y la sesión sigue activa. Alternativa descartada: mover el hook a `shared/` en este change. Es razonable, pero toca C-20. Queda anotado en Open Questions.

4. **La validación es una función pura que corre al enviar.** Cada subfeature tiene su módulo `validacion.ts`. Las reglas son:
   - **Profesional:** nombre, apellido y matrícula no blancos después de `trim`, todos con un máximo de 255 caracteres. La especialidad es opcional, con el mismo máximo.
   - **Sillón:** nombre no blanco y descripción opcional.

   Los valores del formulario se guardan como texto. Al enviar se recortan y un texto opcional vacío se convierte en `null`. El formulario usa `noValidate` y los errores se muestran con `Campo error` (`aria-invalid` y `aria-describedby`). El error de un campo se limpia cuando la persona lo edita. Un formulario inválido no emite ninguna escritura. Alternativa descartada: `required` y `maxLength` nativos, que cortan el texto sin avisar y tienen mensajes que no se pueden testear.

5. **La baja y la reactivación usan `PUT` con los datos actuales.** La acción de fila envía la representación vigente con `activo` invertido. La baja pide confirmación en la propia fila («Confirmar baja» / «Cancelar»), sin `window.confirm`. Se copia el `AccionesFila` de C-20 dentro de la feature, adaptado a `activo`, en lugar de generalizar el de prestaciones. Después de cada escritura, exitosa o fallida, se vuelve a consultar la lista. Un contador de solicitudes descarta las respuestas viejas, como en `useCatalogoPrestaciones`. El orden de sillones por nombre se aplica en el cliente (`localeCompare(..., "es")`). El de profesionales respeta el de la API, que ordena por ID.

6. **Un odontólogo sin vínculo ve un aviso informativo.** En `/profesionales`, si el `GET` falla con `ErrorApi` de estado `403`, el hook de carga marca el estado `sinVinculo` en lugar de `errorCarga`. La pantalla muestra entonces `Alerta tipo="info"` con «Tu usuario no tiene un profesional vinculado. Pedile al administrador que lo vincule.», sin tabla ni botón «Reintentar». Cualquier otro fallo muestra la `Alerta tipo="peligro"` con reintento. Alternativa descartada: ocultar la entrada de menú al odontólogo. Tiene acceso legítimo a su registro y la SPA no puede saber de antemano si tiene vínculo.

7. **El panel de prestaciones se abre por profesional y se guarda con un solo `PUT`.** Cada fila tiene la acción «Prestaciones», visible para todos los roles, que abre `PanelPrestaciones` con `key={profesional.id}`. La key reinicia el estado al cambiar de profesional. Al montar, se hacen en paralelo `GET /api/profesionales/{id}/prestaciones` y, solo para `ADMIN`, `GET /api/prestaciones?soloActivas=false`.
   - **Para otros roles:** se muestra la lista de asociadas, con la marca «inactiva» en las que corresponda, o un mensaje de lista vacía.
   - **Para `ADMIN`:** se muestran casillas para las prestaciones activas del catálogo más las asociadas inactivas. Estas últimas aparecen marcadas y se pueden desmarcar. Las inactivas que no están asociadas no se ofrecen. Al guardar se envía `{ prestacionIds: [...] }` con el conjunto completo y se adopta la lista que devuelve el `200`. El botón se deshabilita mientras hay un pedido pendiente.

   Las casillas son `<input type="checkbox">` nativos con `<label>`, dentro de un `<fieldset>` con `<legend>`. El kit no tiene checkbox, así que se agrega un CSS de la feature (`prestaciones/panel.css`) que solo usa variables de `tokens.css` (espaciado, foco y un objetivo táctil de 44 px). Alternativas descartadas:
   - Un PUT por casilla, que rompe la atomicidad del reemplazo y genera carreras.
   - Agregar `Casilla` a `shared/ui`, que vale la pena pero corresponde a C-50 o a un change del kit.

8. **Los errores se mapean por código en cada subfeature y nunca se muestra `detalle`.**
   - **Profesionales:** `PROFESIONAL_MATRICULA_DUPLICADA` → «Ya existe un profesional con esa matrícula.»; `PROFESIONAL_NO_ENCONTRADO` → «El profesional ya no existe.».
   - **Sillones:** `SILLON_NOMBRE_DUPLICADO` → «Ya existe un sillón con ese nombre.»; `SILLON_NO_ENCONTRADO` → «El sillón ya no existe.».
   - **Panel:** `PRESTACION_INEXISTENTE` → «Alguna de las prestaciones elegidas ya no existe.»; `PRESTACION_INACTIVA` → «No se puede asociar una prestación dada de baja.»; `PROFESIONAL_NO_ENCONTRADO` → profesional inexistente; `PROFESIONAL_PRESTACIONES_CONFLICTO` → «Otra persona modificó estas prestaciones. Volvé a intentarlo.». Ante un `422` o un `409`, el panel vuelve a consultar las asociadas y el catálogo.
   - **Comunes:** `403` → permiso insuficiente; cualquier otro error → «No pudimos completar la operación. Intentá de nuevo.».

   Los errores de negocio se muestran en `Alerta tipo="peligro"` y no descartan lo ingresado. Los éxitos se anuncian con `Toast`. El `401` lo maneja C-16.

9. **Los tests de Vitest usan `fetch` simulado y las rutas reales.** Cada subfeature tiene su `*.test.tsx`. Monta `AppRutas` con `construirRegistro({ profesionales: { rutas } })` a partir del `routes.ts` real de la feature y usa `simularSesionActiva(rol, responder)` con respuestas por URL y método. Se fija la cookie `XSRF-TOKEN` como en C-20. Las escrituras se verifican filtrando las llamadas que no son `GET`. Además, un test del agregador confirma que `features/profesionales/routes.ts` expone `/profesionales` y `/sillones` sin duplicados. Cada `it(...)` lleva el nombre exacto del escenario de las specs. Alternativa descartada: mockear `clienteApi`, que ocultaría el contrato de rutas, el CSRF y el mapeo de errores.

10. **Se agregan capacidades delta nuevas sin tocar las specs de la API.** `profesionales-frontend`, `sillones-frontend` y `profesional-prestaciones-frontend` describen la política de la SPA. `catalogo-profesionales`, `catalogo-sillones` y `profesional-prestaciones` siguen siendo el contrato y no se modifican.

## Risks / Trade-offs

- [Si un `routes.ts` hijo no exporta `rutas`, el agregador lo omitiría en silencio] → El agregador usa la misma validación de forma que `construirRegistro` (lanza si `rutas` no es un arreglo), y el test del agregador lo cubre.
- [El hook `useEsAdministrador` queda repetido en dos features] → Son dos líneas sin lógica de negocio. La unificación queda anotada en Open Questions.
- [Desactivar un sillón o un profesional con turnos futuros todavía no está bloqueado (RN-37)] → La guarda llega con C-30. La SPA ya muestra como alerta cualquier error de negocio de la baja, sin cambiar este diseño.
- [La vista del odontólogo depende del vínculo de C-13] → Sin vínculo, el aviso informativo explica la situación en lugar de mostrar un error técnico.
- [Ediciones concurrentes de las prestaciones de un profesional] → El `PUT` es atómico en el backend. El `409 PROFESIONAL_PRESTACIONES_CONFLICTO` provoca una recarga y un aviso.
- [El orden del menú (30 y 40) es provisional] → Se ajusta en C-50 sin cambiar las specs.
- [Los tokens son provisionales] → La identidad final se resuelve en C-50.
- [La prueba manual de `RECEPCION` y `ODONTOLOGO` (tarea 13.3) no se puede hacer todavía] → **Tarea diferida** por decisión del equipo (2026-11-24). El seed de desarrollo solo crea un `ADMIN` y la pantalla para crear usuarios llega con C-24. La prueba manual como `ADMIN` ya se hizo y resultó correcta. Hasta entonces, el comportamiento de los dos roles lo cubren los tests de Vitest de las tres specs. La prueba se hace y se marca después del merge de C-24.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy)
- Mockups: none
- Approval: not applicable
- Preview: not applicable
- Visual verify: pending (se compara contra los tokens y componentes aprobados en apply/verify)

## Migration Plan

No hay migración de datos, API ni dependencias. El frontend se despliega contra el backend de C-05, C-06 y C-10, que ya está disponible. Para revertir, se elimina `features/profesionales/` (y se restaura su `.gitkeep`). Las rutas desaparecen del menú y del router sin afectar datos ni sesiones.

## Open Questions

- Cuándo mover `useEsAdministrador` y el patrón `AccionesFila` a `shared/` para prestaciones, profesionales y sillones. Puede hacerse en un refactor posterior sin cambiar las specs.
- Si C-22 necesita una ruta de detalle `/profesionales/:id`, se agrega con su propio `routes.ts` hijo sin alterar este diseño.

# Design

## Context

Ver `proposal.md` para la motivación. El frontend ya tiene sesión y guardas (C-16), el cliente HTTP con CSRF (C-09), el registro automático de rutas por `features/*/routes.ts` y el kit UI de `shared/ui`. `features/prestaciones` y `features/configuracion` solo contienen `.gitkeep`. Los contratos de C-04 y C-08 están mergeados: no hay cambios de backend en este change.

Restricciones que condicionan el diseño:

- `clienteApi` usa rutas relativas a `/api`, agrega CSRF en escrituras, devuelve `undefined` en `204` y lanza `ErrorApi` con `estado`, `codigo`, `detalle` y `campos`.
- `RutaFeature` ya declara `roles` y `ambito`; `AppRutas` aplica `RequireRole` y filtra el menú con el mismo criterio. Una ruta interna sin `roles` queda disponible a los tres roles.
- `useSesion()` expone el rol en `estado.identidad.rol`; el fixture de tests `simularSesionActiva` hoy fija `ADMIN`.
- `design-system` está en modo `system-only`, revisión 1 aprobada: sin mockups por change y solo tokens de `tokens.css`.

## Goals / Non-Goals

**Goals:**

- Cubrir US-003 y US-006 en la SPA con dos pantallas internas registradas por sus features.
- Validar en el cliente las mismas reglas duras que aplica el backend (RN-09 y RN-36) sin reemplazar su autoridad.
- Reutilizar sesión, guardas, cliente HTTP, kit UI y tokens existentes; no introducir dependencias ni estilos con valores fijos.

**Non-Goals:**

- Modificar endpoints, DTOs, validaciones o permisos del backend.
- Crear un sistema de formularios, estado global o librería de validación nuevos.
- Implementar agenda, profesionales, sillones, pacientes, horarios ni reserva pública (C-21 a C-24 y olas siguientes).
- Definir la identidad visual final: se usan los tokens provisionales aprobados.

## Decisions

1. **Dos capacidades y dos features separadas.** `prestaciones-frontend` y `configuracion-frontend` tienen contratos, permisos y ciclos de vida distintos: configuración excluye a `ODONTOLOGO` de la ruta, prestaciones lo admite en lectura. Una única capacidad `administracion-frontend` sería un cajón de sastre y obligaría a mezclar escenarios de dos APIs. Cada feature exporta su `routes.ts`; `modulos.ts` las descubre sin editar archivos centrales.

2. **Rutas internas, formularios en la misma pantalla.** `features/prestaciones/routes.ts` declara `/prestaciones` con `menu: { etiqueta: "Prestaciones", orden: 20 }` y `roles: ["ADMIN", "RECEPCION", "ODONTOLOGO"]`. `features/configuracion/routes.ts` declara `/configuracion` con `menu: { etiqueta: "Configuración", orden: 90 }` y `roles: ["ADMIN", "RECEPCION"]`. Ambas quedan bajo `RequireSesion` + `Layout` y heredan el filtrado de menú de C-16. No se crean rutas de escritura (`/prestaciones/nueva`, `/configuracion/editar`): el alta y la edición de prestaciones son un panel en la misma página y la configuración es un formulario único. Así no existe una URL que esquive el control de rol y se evita duplicar guardas para un change tamaño S. Alternativa descartada: rutas hijas con `RequireRole` propio, que sumarían escenarios y navegación sin beneficio.

3. **Control de escritura en dos capas.** La ruta define quién puede ver la pantalla; dentro de la pantalla, `const esAdmin = estado.tipo === "activa" && estado.identidad.rol === "ADMIN"` decide si se renderizan el formulario, los botones de alta/edición/baja y la acción de reactivar. Para `RECEPCION` y `ODONTOLOGO` la lista se muestra sin controles de escritura y la configuración se muestra con `readOnly` y sin botón de guardado. La API conserva `@PreAuthorize` y responde `403` si el rol cambió; la SPA lo presenta como permiso insuficiente, no como sesión vencida (C-16 no cierra sesión por `403`). Alternativa descartada: deshabilitar botones en vez de ocultarlos, que deja visible una acción que el rol no puede ejecutar.

4. **Validación en el cliente con funciones puras, disparada al enviar.** Cada feature tiene un módulo de validación pequeño y testeable:
   - Prestaciones: nombre no vacío (máximo 100) y duración entera, múltiplo de 5, entre 5 y 480. El texto de error es el de RN-09: «La duración debe ser un múltiplo de 5 entre 5 y 480 minutos.»
   - Configuración: seis enteros con los rangos de C-08 (`cancelacion.plazo_horas` 0–168; `recordatorio.anticipacion_horas` 1–168; los cuatro límites restantes ≥ 1) y `consultorio.nombre` no blanco. Cada campo tiene su mensaje de RN-36.
   Los valores del formulario viven como texto para no perder lo que la persona escribió; un helper estricto (`/^-?\d+$/` sobre el texto recortado) evita que `Number("2,5")` o `Number("")` se conviertan en valores válidos por accidente. La validación corre al enviar y se limpia al cambiar el campo; el formulario usa `noValidate` para que los mensajes sean propios, accesibles vía `Campo error` (`aria-invalid` + `aria-describedby`). Un valor inválido no emite ninguna escritura. Alternativa descartada: confiar en `min`/`max`/`step` nativos, cuyo comportamiento y mensajes varían entre navegadores y no son testeables de forma estable en jsdom.

5. **Lectura y refresco simple, sin caché.** Al montar, cada pantalla consulta su `GET` y muestra un estado de carga (`role="status"`), la tabla o formulario, o una `Alerta` de error con botón «Reintentar». Después de cada escritura exitosa se vuelve a consultar la lista (`prestaciones`) o se adoptan los valores devueltos por el `PUT` (`configuración`). Un contador de solicitudes descarta respuestas viejas de la lista si se encadenan varias operaciones. No se agrega estado global ni caché: son dos pantallas de administración de bajo tráfico.

6. **Errores de API mapeados por código, nunca por `detalle`.** Un módulo por feature traduce `ErrorApi`:
   - Prestaciones: `RN-09` → mensaje de duración; `PRESTACION-DUPLICADA` → «Ya existe una prestación con ese nombre.»; `PRESTACION-NO-ENCONTRADA` → «La prestación ya no existe.»; `403` → permiso insuficiente; resto → «No pudimos completar la operación. Intentá de nuevo.»
   - Configuración: `RN-36` → «Hay valores fuera de los límites permitidos.»; `400` → «No pudimos guardar la configuración. Revisá los datos e intentá de nuevo.»; `403` → permiso insuficiente; resto → mensaje genérico.
   Los errores de negocio van en `Alerta tipo="peligro"` y no descartan lo ingresado. Los éxitos se anuncian con `Toast` (`role="status"`). El `401` queda a cargo de C-16 y no se trata en estas pantallas. Nunca se muestra `detalle` del servidor.

7. **UI con el kit y los tokens existentes.** Prestaciones usa `Tabla` (duración con `numerica: true`, que ya aplica `--font-mono`), `Tarjeta` para el formulario, `Campo`, `Boton`, `Alerta` y `Toast`. Configuración usa `Campo` por clave (los numéricos con `className="mono"`), `Boton`, `Alerta` y `Toast`. La baja de una prestación pide confirmación en la fila con dos botones («Confirmar baja» / «Cancelar») en lugar de `window.confirm`, para que sea accesible y testeable. No se agregan colores, espaciados ni fuentes fijos; los objetivos táctiles ya son de 44 px por el kit.

8. **Tests Vitest con `fetch` stubbeado y la ruta real.** Los tests montan `AppRutas` con un registro construido a partir de los `routes.ts` reales (más Inicio para el menú) y stubbean `fetch` por URL: `/api/auth/me` devuelve la identidad con el rol del caso y el resto responde con `Response.json(...)`. Se fija la cookie `XSRF-TOKEN` para que `clienteApi` no busque un token extra. El fixture `simularSesionActiva` se extiende de forma compatible para aceptar un rol (por defecto `ADMIN`) y una función que responda las demás rutas, de modo que los tests actuales sigan funcionando. Cada `it(...)` lleva el nombre exacto del escenario de la spec. Alternativa descartada: mockear `clienteApi`, que ocultaría el contrato real de rutas, CSRF y mapeo de errores.

9. **Capacidades delta nuevas, sin tocar las specs existentes.** `catalogo-prestaciones` y `configuracion-parametros` siguen siendo el contrato de la API; estos deltas describen la política de la SPA, igual que `sesion-frontend-personal` sobre `control-acceso`. No se modifica ninguna spec principal ni se duplican sus reglas.

## Risks / Trade-offs

- **La validación del cliente se desincroniza del backend** → los mensajes y rangos se declaran en un módulo por feature y se cubren con tests de los límites; el backend sigue siendo la autoridad y sus `422` se muestran como alerta.
- **Doble envío o respuestas fuera de orden** → se bloquea el botón mientras hay un pedido pendiente y un contador descarta listas viejas.
- **`RECEPCION` conserva una sesión con rol cambiado** → la API responde `403`; la SPA muestra permiso insuficiente sin cerrar la sesión.
- **El orden de menú (20 y 90) es provisional** → C-21 a C-24 agregarán entradas; el orden se ajusta entonces sin cambiar estas specs.
- **Los tokens son provisionales** → cualquier cambio de identidad visual se resuelve en el change de diseño final, no acá.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy)
- Mockups: none
- Approval: not applicable
- Preview: not applicable
- Visual verify: pending (se compara contra los tokens y componentes aprobados en apply/verify)

## Migration Plan

No hay migración de datos, API ni dependencias. Se despliega el frontend junto con el backend de C-04 y C-08 ya disponibles. Para revertir, se quitan las dos entradas de `routes.ts` y sus features; las rutas desaparecen del menú y del router sin afectar datos ni sesiones.

## Open Questions

- El orden exacto de las entradas de menú frente a C-21 a C-24 se ajusta cuando esas features existan; no cambia el comportamiento especificado.

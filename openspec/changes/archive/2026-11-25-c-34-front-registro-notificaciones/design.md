# Design

## Context

- **API (C-25):** `GET /api/notificaciones?estado&turnoId&pagina&tamanio`, solo para `ADMIN` y `RECEPCION`. Devuelve `Pagina<T>` = `{contenido, pagina, tamanio, totalElementos, totalPaginas}`, de lo más reciente a lo más antiguo. Cada elemento trae `id`, `tipo`, `estado`, `canal`, `destinatario`, `turnoId`, `ofertaHuecoId`, `pacienteId`, `inicioReferencia`, `intentos`, `ultimoError`, `programadaPara` y `enviadaEn`. No incluye la fecha de creación.
- **Front (C-09, C-16):**
  - las features se descubren solas por `features/*/routes.ts` con `menu` y `roles`, y el guardia de rol ya muestra el aviso de permiso sin montar la pantalla;
  - el cliente `api.get<T>` lanza `ErrorApi` con `estado`;
  - el kit de UI exporta `Tabla`, `Campo`, `Boton`, `Alerta` y `Toast`;
  - `shared/fecha` ofrece `formatearFecha` y `formatearHora` en Buenos Aires.
- **Patrón a seguir:** `features/pacientes` (`consulta.ts` arma la URL, un hook carga con un contador contra respuestas viejas, un formulario `role="search"` y un test de integración con `AppRutas` y `simularSesionActiva`).
- **Sistema visual:** `system-only`, revisión 1 aprobada. No lleva mockups por change; se usan solo tokens y clases del kit.

## Goals / Non-Goals

**Goals:**
- Ver el registro, filtrarlo por estado y turno, paginarlo y leer el último error de los fallidos.
- Dejar armado el primer consumo del formato `Pagina` en el front, sin generalizarlo antes de tiempo.

**Non-Goals:**
- Reenviar, ver el cuerpo del email, filtrar por paciente o fecha.
- Un componente de paginación compartido en `shared/ui`: se extrae cuando aparezca un segundo listado paginado.

## Decisions

### D1. Estructura de `features/notificaciones`
- `routes.ts`: `/notificaciones`, `menu: { etiqueta: "Notificaciones", orden: 80 }` (después de Sillones con 40 y antes de Configuración y Usuarios con 90), `roles: ["ADMIN", "RECEPCION"]`.
- `tipos.ts`: `NotificacionRespuesta`, `PaginaNotificaciones`, `FiltrosNotificaciones { estado: "" | EstadoNotificacion; turno: string }`.
- `consulta.ts`: `consultaDeNotificaciones(filtros, pagina)`, en orden fijo `estado`, `turnoId`, `pagina`, `tamanio=20`, y `turnoValido(texto)`, que exige un entero positivo después de recortar.
- `presentacion.ts`: etiquetas en español de tipo y estado, `MARCA_VACIO` y `fechaHora(instante)` = `formatearFecha + " " + formatearHora`.
- `useNotificaciones.ts`: estado `{ pagina?, cargando, error?: "permiso" | "carga" }`, más `buscar(filtros)`, `irAPagina(n)` y `reintentar()`. Recuerda los filtros y la página vigentes en `useRef` y descarta las respuestas viejas con un contador, como `usePacientes`.
- `FiltrosNotificaciones.tsx`: un `<form role="search">` con un `<select class="select">` para el estado y un `Campo` para el turno, que muestra el error de validación.
- `PaginaNotificaciones.tsx`: la tabla (`Tabla`), la paginación y las alertas.
- `textos.ts`: los mensajes, para reutilizarlos en los tests.

### D2. Celdas
- **Turno:** `#id` si hay `turnoId`; "Aviso de hueco" si solo hay `ofertaHuecoId`; si no, la marca de vacío.
- **Referencia y envío:** `fechaHora`, o la marca de vacío si es nulo.
- **Error:** `ultimoError` solo si `estado === "FALLIDA"`; en otro caso, la marca de vacío, aunque la API mandara algo.
- **Intentos:** columna numérica.
- **Estado:** texto en español. Para no sumar un componente visual nuevo al kit en un change S, va sin badge de color. El estado se lee como texto, así que es accesible.

### D3. Mensajes vacíos y errores
- **Vacío:** "No hay envíos registrados." sin filtros activos; "No hay envíos que coincidan con los filtros." con alguno.
- **`403`:** `Alerta` "No tenés permiso para ver el registro de notificaciones." y no se muestra la tabla.
- **Otro error:** `Alerta` "No pudimos cargar el registro de notificaciones." con `Boton` "Reintentar", que repite la última consulta (mismos filtros y página).
- En ambos casos de error se descarta la página anterior para no mostrar datos viejos.

### D4. Paginación
- Texto: "Página {pagina+1} de {totalPaginas} · {totalElementos} envíos" (en singular, "1 envío").
- "Anterior" y "Siguiente" con `disabled` en los extremos. No se muestra si `totalElementos === 0`.
- Si la API devuelve una página posterior a la última (por ejemplo, porque se borraron filas), el listado queda vacío y "Anterior" sigue habilitado para volver. Es un caso raro y no se corrige solo.

## Risks / Trade-offs

- **Paginación propia en la feature** → se duplicará en el segundo listado paginado. Es aceptable: se extrae entonces a `shared/ui` con dos casos reales.
- **Datos personales en pantalla** (email del destinatario) → solo para `ADMIN` y `RECEPCION`, que ya acceden a las fichas. No se loguea nada en la consola.
- **Sin fecha de registro en la API** → se muestran la referencia y el envío; alcanza para US-020.

# Design

## Context

- C-39 expone `GET /api/public/turnos/{token}`, que devuelve `{inicio, fin, estado, profesional, prestacion, cancelacionSeriaTardia}`. También expone `POST …/confirmar`, que responde 200 con el mismo detalle, y `POST …/cancelar`, que responde 204. Los rechazos de token se responden siempre con un 404 uniforme.
- C-42 expone `GET /api/public/ofertas/{token}`, que devuelve `{inicio, fin, profesional, prestacion, estado}` con un estado entre `DISPONIBLE`, `TOMADA`, `VENCIDA` y `NO_DISPONIBLE`. También expone `POST …/tomar`, que responde 201 con `{inicio, fin, estado, profesional, prestacion, token}`. Sus 409 traen los códigos `HUECO_YA_TOMADO`, `OFERTA_VENCIDA` y `OFERTA_NO_DISPONIBLE`, y el 404 trae `OFERTA_NO_ENCONTRADA`.
- C-48 dejó la infraestructura pública en la SPA: rutas con `ambito: "publico"`, el cliente `api` sin CSRF para `/public/`, `ErrorApi` con `tipo`, `estado` y `codigo`, y los formateadores de Buenos Aires. La pantalla de reserva ya enlaza a `/turno/{token}`.
- `/api/public/**` tiene límite de tasa por IP, así que puede responder 429.
- El design system está en modo `system-only`, con la revisión 1 aprobada.

## Goals / Non-Goals

**Goals:**
- Dos pantallas públicas, usables a 390 × 844, con teclado y lector de pantalla, construidas con el kit de C-09 y los tokens del sistema.
- Una sola pantalla genérica de «enlace no disponible», compartida por las dos rutas.

**Non-Goals:**
- Reprogramar desde el enlace (RN-07). Cambios en el backend. Mockups, porque rige la política `system-only`. Identidad visual final, que corresponde a C-50.

## Decisions

- **Ubicación.** Las pantallas van en `features/reserva-publica/turno-token/` y `features/reserva-publica/oferta/`, y se registran en el `routes.ts` existente de la feature. RM-09 asigna esa carpeta al mismo responsable de C-48 y C-49, así que no hace falta tocar archivos centrales. La alternativa de crear una feature nueva duplicaría el registro y la `PantallaEnlaceNoDisponible`.
- **Lectura del token.** El token sale de `useParams` y se usa solo para construir la ruta con `encodeURIComponent`. Vive en memoria: no se guarda en estado global, en storage ni en el título. Las vistas no lo muestran. El único caso en que la SPA arma una URL con un token es el enlace relativo del turno creado al tomar una oferta, igual que en C-48.
- **Referrer-Policy.** Se agrega `<meta name="referrer" content="no-referrer">` en `index.html`, de modo que vale para toda la SPA y en cualquier servidor estático. La alternativa de configurar cabeceras HTTP depende de cómo se sirva el front en producción, algo que el proyecto no define. Se puede sumar más adelante sin cambiar el comportamiento.
- **Estado de cada pantalla.** Cada pantalla tiene un hook con una máquina de estados explícita: `cargando | no-disponible | listo | error-recuperable`. Las acciones llevan una bandera `enviando` que deshabilita el botón y evita el doble POST. No hay reintentos automáticos. Un 404 en cualquier punto pasa a `no-disponible`.
- **Acciones del turno según el estado.** Confirmar se ofrece solo en `PROGRAMADO`. Cancelar se ofrece en `PROGRAMADO` y en `CONFIRMADO`. Los demás estados no muestran acciones. Después de cancelar (204), la vista marca el turno como `CANCELADO` de forma local, sin volver a consultar: el token ya quedó revocado y otra consulta daría 404.
- **Confirmación de la cancelación.** Se resuelve dentro de la misma tarjeta, con el patrón de `ConfirmarEliminacion`: un paso con «Sí, cancelar» y «Volver». Si `cancelacionSeriaTardia` es verdadero, ese paso muestra una `Alerta` de advertencia. Se descarta un modal porque el kit no tiene diálogo accesible y un paso inline es más simple con el teclado y el lector de pantalla. Al abrir el paso, el foco pasa al aviso.
- **Mensajes de la oferta.** Una única tabla traduce tanto el estado de la consulta como el código del 409 a un mismo mensaje: `TOMADA` y `HUECO_YA_TOMADO` → «ya fue tomado», `VENCIDA` y `OFERTA_VENCIDA` → «venció», y los demás → «no disponible». Así la consulta y la toma no divergen.
- **Validación de las respuestas.** Igual que `esHorario` en C-48, se validan la forma y las fechas de cada respuesta antes de mostrarla. Una respuesta mal formada se trata como error de servidor recuperable.
- **Accesibilidad.** Cada pantalla tiene un `h1`. Los cambios de estado se anuncian con `role="status"` o con `Alerta`. Los objetivos táctiles respetan el tamaño mínimo del sistema.

## Risks / Trade-offs

- [El token aparece en la URL y en el historial del navegador] → Es inherente al diseño del enlace (DD-06). `no-referrer` evita que se filtre a terceros y la SPA no carga recursos externos.
- [Recargar después de cancelar da 404 y muestra la pantalla genérica] → Es el comportamiento esperado de RN-25. El paciente ya vio la confirmación de la cancelación.
- [Que otra persona tome el hueco mientras la pantalla está abierta] → El 409 `HUECO_YA_TOMADO` lo resuelve al intentar tomarlo.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy) — 2026-11-25

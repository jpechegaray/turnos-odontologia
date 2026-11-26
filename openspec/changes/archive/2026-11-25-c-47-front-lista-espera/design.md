# Design

## Context

La API ya ofrece estos endpoints:

- `POST /api/lista-espera`. Recibe `{pacienteId, prestacionId, profesionalId?, fechaDesde?, fechaHasta, franjaDesde?, franjaHasta?}`, con fechas `YYYY-MM-DD` y horas `HH:mm`. Responde 201 con la entrada.
- `GET /api/lista-espera?estado=`. Devuelve una lista de `RespuestaEntrada`, que trae **solo IDs** y ningún dato personal (RN-35).
- `DELETE /api/lista-espera/{id}`. Responde 204, o 409 `ENTRADA_NO_ACTIVA`.
- `GET /api/lista-espera/compatibles?profesionalId&inicio&fin`. Recibe instantes ISO con zona.
- `POST /api/lista-espera/{id}/asignar`. Recibe `{profesionalId, inicio}` y responde 201 con el turno. Los rechazos posibles son:
  - 409 `ENTRADA_NO_ACTIVA`.
  - 409 de solapamiento con `recurso`.
  - 422 `ENTRADA_INCOMPATIBLE`, `SIN_SILLON_DISPONIBLE` o una regla del alta del turno.
- Al alta la rechazan 409 `ENTRADA_ACTIVA_DUPLICADA` y 422 `PACIENTE_SIN_EMAIL`, `PACIENTE_INACTIVO` o `*_INEXISTENTE` / `*_INACTIVA`.

El listado de pacientes y las compatibles son solo para `ADMIN` y `RECEPCION`. El front ya tiene piezas reutilizables:

- `SeleccionPaciente` y `mensajeDelTurno` en `features/agenda/crear`.
- Los catálogos `/profesionales` y `/prestaciones`.
- `Tabla`, `Campo`, `CampoSeleccion`, `Alerta`, `Boton` y el patrón de confirmación inline de `ConfirmarEliminacion`.
- `aInstanteUtc` para la zona de Buenos Aires.

## Goals / Non-Goals

**Goals:**
- Una pantalla de recepción que resuelva el ciclo completo: anotar, ver, dar de baja y asignar un hueco.

**Non-Goals:**
- Una vista de solo lectura para el odontólogo. La API la permite, pero no figura en el scope del MVP y obligaría a resolver nombres de pacientes que el odontólogo no puede listar.
- Editar entradas (la API no lo ofrece).
- Cambios en el backend.

## Decisions

- **Roles.** La ruta lleva `roles: ["ADMIN", "RECEPCION"]` y aparece en el menú con orden 15, entre Pacientes y Prestaciones. El guard existente (`RequireRole`) muestra «No tenés permiso» al odontólogo, igual que en Notificaciones.
- **Nombres en lugar de IDs.** Al montar la pantalla se cargan en paralelo `/pacientes`, `/prestaciones` y `/profesionales`, y se arman mapas por ID. Como la API devuelve solo IDs, esta es la única forma de mostrar nombres. Un consultorio de 2 a 5 profesionales tiene un padrón chico, así que cargar el listado de pacientes es aceptable. Si un catálogo falla, se usa un respaldo («Paciente #12»), igual que `useCatalogosAgenda`. La alternativa de pedir `/pacientes/{id}` por cada fila multiplica los pedidos.
- **Paciente del alta.** Se reutiliza `SeleccionPaciente`, que busca por apellido, nombre, DNI o email y filtra a los pacientes activos. La condición de que el paciente tenga email la valida el servidor (422 `PACIENTE_SIN_EMAIL`). Filtrarla en el front obligaría a duplicar la regla.
- **Validación en la SPA.** La pantalla replica las reglas de forma de `SolicitudAlta`: obligatorios, `fechaHasta >= fechaDesde`, franja con las dos horas o ninguna y fin posterior al inicio. Los rechazos del servidor se traducen con una tabla de códigos propia del alta. Un 400 con campos marca cada campo.
- **Hueco.** El formulario pide profesional, fecha y horas de inicio y fin. Con `aInstanteUtc(fecha, hora)` se obtienen los instantes en Buenos Aires para `compatibles`. La asignación envía `{profesionalId, inicio}` con el inicio del hueco, y el backend calcula el fin con la duración de la prestación.
- **Mensajes de la asignación.** Primero se mira el código propio (`ENTRADA_NO_ACTIVA`, `ENTRADA_INCOMPATIBLE`, `SIN_SILLON_DISPONIBLE`). Si no es uno de esos, se delega en `mensajeDelTurno`, que ya traduce el solapamiento por `recurso` y las reglas del alta del turno. Ante un 409 o un 422 se vuelven a consultar las compatibles. Ante un 201, se recargan las compatibles y el listado.
- **Un solo pedido en curso.** Cada escritura (alta, baja y asignación) usa un `useRef` de pedido en curso más el botón deshabilitado. No hay reintentos automáticos.
- **Estructura.** El código va en `features/lista-espera/` y se reparte así:
  - `routes.ts`.
  - `tipos.ts`.
  - `api.ts`, con las llamadas y las rutas.
  - `validacion.ts`.
  - `mensajes.ts`.
  - `useCatalogosListaEspera.ts`.
  - `PaginaListaEspera.tsx`, que compone las secciones.
  - `FormularioAlta.tsx`.
  - `ListadoEntradas.tsx`.
  - `SugerenciasHueco.tsx`.
  - `lista-espera.css`, que usa solo tokens.

## Risks / Trade-offs

- [Un padrón grande hace pesada la carga de `/pacientes`] → En el MVP el padrón es chico. Si crece, se puede sumar un endpoint de nombres por IDs sin cambiar la pantalla.
- [El catálogo de pacientes quedó desactualizado frente a un alta reciente] → Se recarga junto con el listado después de cada alta.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy) — 2026-11-25

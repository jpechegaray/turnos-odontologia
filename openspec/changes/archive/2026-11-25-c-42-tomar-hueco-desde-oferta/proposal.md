# Proposal

## Why

C-36 ya avisa por email a la lista de espera cuando se libera un hueco, pero el enlace del aviso todavía no lleva a ningún lado: el paciente no puede ver la oferta ni quedarse con el turno. C-42 cierra el diferenciador del producto (US-022, Flujo 7). Es el paso del camino crítico que desbloquea la pantalla pública de C-49.

## What Changes

- Nuevo `GET /api/public/ofertas/{token}`. Devuelve el hueco ofrecido (inicio y fin del turno que se crearía, profesional y prestación) y su estado para el paciente: `DISPONIBLE`, `TOMADA`, `VENCIDA` o `NO_DISPONIBLE`. No devuelve datos personales.
  - Un token inexistente o mal formado responde `404` uniforme (RN-38).
  - Un token existente cuya oferta ya fue tomada o venció responde `200` con ese estado (KB §Excepciones).
- Nuevo `POST /api/public/ofertas/{token}/tomar`. Crea el turno con la agenda: canal `ONLINE`, inicio del hueco, duración por defecto de la prestación de la entrada y sillón elegido por el sistema (el sugerido si sigue libre; si no, el primero activo libre). Luego:
  - la entrada pasa a `ASIGNADA` con el turno;
  - la oferta y las demás ofertas `ENVIADA` del mismo hueco pasan a `TOMADA`;
  - se emite el token del turno nuevo y se envía el email de confirmación con el enlace `/turno/{token}`;
  - responde `201` con el turno y el token del turno, que se muestra una única vez (RN-25).
- Carrera y errores (RN-15, RN-24):
  - De dos confirmaciones simultáneas sobre el mismo hueco, una responde `201` y la otra `409 HUECO_YA_TOMADO`.
  - Si el hueco ya empezó, responde `409 OFERTA_VENCIDA`.
  - Si la entrada ya no está `ACTIVA` o el hueco dejó de ser asignable (horario, bloqueo, recursos inactivos), responde `409 OFERTA_NO_DISPONIBLE`.
  - Un token inválido responde `404` uniforme.
- Nueva operación interna de la agenda: alta de un turno `ONLINE` con sillón elegido por el sistema. Aplica las mismas reglas del alta (RN-01 a RN-04, RN-09, RN-11 y RN-13) y la misma serialización por recurso. C-41 la va a reutilizar.
- Nuevo reconstructor del email `CONFIRMACION_RESERVA`, para que el reintento (RN-30) vuelva a armar el enlace con un token nuevo. C-41 también lo va a reutilizar.
- La protección del enlace con token que hoy cubre `/api/public/turnos` se extiende a `/api/public/ofertas`: `Cache-Control: no-store` y errores sin el token en `instance`. El token nunca aparece en los logs.

## Capabilities

### New Capabilities
- `toma-oferta-hueco`: consulta pública de una oferta de hueco por su token y la toma del hueco. Cubre la creación del turno, los estados resultantes de la entrada y las ofertas, la carrera entre confirmaciones, el vencimiento, el token del turno nuevo con su email de confirmación y la protección del token.

### Modified Capabilities
- `agenda-turnos`: se agrega el requisito de alta interna de un turno `ONLINE` con sillón asignado por el sistema, sin rol de personal y con las mismas reglas y garantías de no solapamiento que el alta del personal.
- `notificaciones-email`: se agrega el requisito de reintento del email `CONFIRMACION_RESERVA`, que se rearma con un token nuevo del turno y se descarta si el turno ya no está vigente.

## Impact

- **Backend**:
  - `listaespera.oferta`: servicio de toma, controlador público y persistencia (búsqueda por hash en cualquier estado, bloqueo de la oferta, paso a `TOMADA` de las ofertas del hueco).
  - `listaespera`: bloqueo y asignación de la entrada.
  - `agenda.application`: alta online.
  - `agenda.domain.Turno`: fábrica con canal `ONLINE`.
  - `shared.notificacion.application`: reconstructor de la confirmación.
  - `shared.web`: protección de la ruta de ofertas.
- **API**: dos endpoints públicos nuevos bajo `/api/public/ofertas/{token}`, cubiertos por el límite de tasa existente de C-17 y sin CSRF, como el resto de `/api/public/**`.
- **Base de datos**: sin tablas nuevas. Se usan `oferta_hueco`, `lista_espera`, `turno` y `token_turno`, con sus restricciones `EXCLUDE` como garantía final.
- **Dependencias**: C-17, C-19, C-32 y C-36, todas mergeadas. Desbloquea C-49.
- **Riesgo RM-06**: C-43 trabaja en el mismo módulo `listaespera`, en otro subpaquete. C-41 va a usar el alta online y el reconstructor de la confirmación, así que conviene coordinar con Lionel.

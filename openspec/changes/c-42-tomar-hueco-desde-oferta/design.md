# Design

## Context

Estado actual relevante (ver `proposal.md` para el porqué):

- **C-36** dejó `listaespera.oferta`:
  - la entidad `OfertaHueco` (record de dominio con `vigente(ahora)` y `conToken`);
  - `OfertasHuecoPersistencia`, con `porTokenHash`, `bloquear(id)` (`PESSIMISTIC_WRITE`) y `vencer(ahora)`;
  - `OfertasHueco.buscarVigentePorToken`, que solo encuentra ofertas `ENVIADA` y vigentes.

  La toma necesita encontrar la oferta en cualquier estado (el `GET` informa `TOMADA` o `VENCIDA`) y bloquearla.
- **C-19**: `CrearTurnoService.crear` exige `ADMIN`/`RECEPCION` (`@PreAuthorize`) y crea siempre con canal `INTERNO` (`Turno.nuevo`). Antes de validar llama a `TurnosPersistencia.bloquearRecursos(profesional, sillón)`, que toma advisory locks en orden fijo (primero el profesional, después el sillón). `guardar` traduce las violaciones `EXCLUDE` a `409 RN-01` con el recurso.
- **C-32**: `ServicioTokenTurno.emitir(turnoId)` genera el token, guarda el hash con la expiración en el inicio del turno y devuelve el token en claro. Ya lo usan C-37 y C-39.
- **C-15/C-37**: `ServicioNotificaciones.registrarYEnviar(tipo, turnoId, ofertaId, pacienteId, datos)` registra y envía con transacción propia. Los reintentos piden los datos a los `ReconstructorDatosNotificacion`. Hoy solo existe `DatosRecordatorios` (para `RECORDATORIO`) y la oferta de C-36. Nadie arma todavía `CONFIRMACION_RESERVA`.
- **C-17**: `FiltroLimiteTasaPublico` ya limita todo `/api/public/**` por IP. `SecurityConfig` deja `/api/public/**` sin sesión y sin CSRF.
- **C-32/C-39**: `ProteccionEnlaceTurnoConfig` y `RutaConTokenDeTurno` ponen `Cache-Control: no-store` y quitan el token de los errores, pero solo para `/api/public/turnos/`.

## Goals / Non-Goals

**Goals:**
- Que la toma sea atómica: turno, entrada `ASIGNADA` y ofertas `TOMADA` en una sola transacción, con la restricción `EXCLUDE` como garantía final ante la carrera.
- Que la agenda tenga una sola puerta de alta online que use el mismo validador y los mismos locks que el alta del personal, para que C-41 la reutilice sin duplicar reglas.
- Que los tokens nunca queden en logs, caché ni cuerpos de error.

**Non-Goals:**
- Pantalla pública `/oferta/:token` (C-49).
- Asignación manual del hueco por recepción (`POST /api/lista-espera/{id}/asignar`, SU-15). Queda para C-43.
- Reserva online `POST /api/public/reservas` (C-41). C-42 solo deja lista el alta online que C-41 va a usar.
- Clave foránea `notificacion_enviada.oferta_hueco_id`: es una mejora pendiente de C-36 y no se toca acá.

## Decisions

### D1. La orquestación vive en `listaespera.oferta`
`TomaDeOferta` (en `application`) y `OfertaPublicaController` (en `web`, con `@RequestMapping("/api/public/ofertas/{token}")`) quedan en el subpaquete `oferta`. La ficha lo ubica en `listaespera` (RM-06: C-43 usa otro subpaquete), y la oferta y la entrada son de ese módulo.
- `TomaDeOferta` depende de la agenda (`CrearTurnoOnlineService`), de `ServicioTokenTurno` y de `ServicioNotificaciones`.
- *Alternativa descartada*: ubicarla en `reservapublica`. Habría obligado a exponer la persistencia de ofertas y entradas fuera de su módulo.

### D2. Alta online como servicio aparte en la agenda
Se agrega `agenda.application.CrearTurnoOnlineService.crear(pacienteId, profesionalId, prestacionId, inicio, sillonPreferidoId)`:
- sin `@PreAuthorize`, porque lo invocan solo flujos públicos ya autenticados por token;
- `@Transactional`, que se une a la transacción de la toma;
- usa `Turno.nuevoOnline` (canal `ONLINE`, sin usuario creador).

Pasos:
1. Bloquea el profesional.
2. Arma la lista de candidatos: el sillón preferido primero (si está activo), después los activos por id ascendente.
3. Para cada candidato, consulta con `ConsultasAgenda.turnosQueSolapan` si el sillón está libre en el intervalo.
4. Elige el primero libre, bloquea ese sillón y valida con `ValidadorTurno`, igual que el alta del personal.
5. Guarda y publica `TurnoCreado`.

Si ningún sillón está libre, lanza `ValidadorTurno.solapado("SILLON")`. La duración sale de la prestación (`SolicitudTurno` con `duracionMin = null`).
- *Alternativa descartada*: abrir `CrearTurnoService.crear` a un llamador sin rol. Rompe el requisito "Solo el personal autorizado crea turnos" y mezcla canales.
- *Alternativa descartada*: bloquear todos los sillones candidatos por adelantado. Toma locks de más y, con el sillón preferido fuera de orden, abre la puerta a deadlocks entre profesionales distintos.

Para no duplicar código se extrae el paso común "bloquear, validar, guardar y publicar" a un método package-private que comparten los dos servicios.

### D3. Transacción de la toma y orden de locks
`TomaDeOferta.tomar(token)` no es transaccional por sí mismo. Primero busca la oferta por hash; si no está o el hash no coincide en tiempo constante, responde `404 OFERTA_NO_ENCONTRADA`. Después ejecuta un `TransactionTemplate` con estos pasos:
1. Toma el advisory lock del profesional del hueco con `CrearTurnoOnlineService.ordenarPorProfesional`, que exige una transacción en curso.
2. Bloquea la fila de la oferta (`bloquear(id)`). Una oferta `TOMADA` responde `409 HUECO_YA_TOMADO`. Una oferta `VENCIDA`, o `ahora >= hueco_inicio`, responde `OFERTA_VENCIDA`.
3. Bloquea la fila de la entrada (nuevo `ListaEsperaPersistencia.bloquear(id)`, `PESSIMISTIC_WRITE`). Si no está `ACTIVA`, responde `OFERTA_NO_DISPONIBLE`. El bloqueo evita que la misma entrada tome dos huecos distintos a la vez.
4. Llama a `CrearTurnoOnlineService.crear(...)` con el sillón sugerido de la oferta. Vuelve a tomar el lock del profesional (es reentrante) y toma el del sillón.
5. Guarda `entrada.asignar(turnoId)` y pasa a `TOMADA`, con un `UPDATE` masivo nuevo, todas las ofertas `ENVIADA` con el mismo `(profesional_id, hueco_inicio, hueco_fin)`, incluida la propia.
6. Llama a `ServicioTokenTurno.emitir(turnoId)` dentro de la misma transacción, así el token se confirma junto con el turno.

El orden de locks es profesional → oferta → entrada → sillón. Se toma primero el profesional porque el `UPDATE` masivo del paso 5 necesita las filas de las otras ofertas del hueco. Si cada toma bloqueara primero su propia oferta, la ganadora esperaría la fila de la perdedora mientras la perdedora espera el profesional, y PostgreSQL abortaría una por deadlock (500). El IT de carrera lo detectó durante el apply.

Con el profesional primero, dos tomas del mismo hueco se serializan desde el arranque, y la segunda encuentra la oferta ya `TOMADA` o choca con el turno de la primera. El alta del personal (profesional → sillón) no bloquea ofertas ni entradas, así que tampoco forma ciclo. Si dos inserciones llegaran a la vez, igual las frena la restricción `EXCLUDE`.

Después del commit, fuera de la transacción, `registrarYEnviar(CONFIRMACION_RESERVA, turnoId, null, pacienteId, datos)` con el enlace `APP_PUBLIC_URL + "/turno/" + token`. Si el correo falla, queda `FALLIDA` y no afecta la respuesta `201`.

### D4. Traducción de errores de la agenda
`TomaDeOferta` captura la `ReglaNegocioException` que sale de la transacción y la traduce así:

| Error de la agenda | Respuesta de la toma |
|---|---|
| `RN-01` con recurso `PROFESIONAL` (otro turno del profesional en el hueco, sea de otra oferta, de recepción o de una reserva) | `409 HUECO_YA_TOMADO` |
| `RN-13` (el hueco empezó entre la lectura y la validación) | `409 OFERTA_VENCIDA` |
| `RN-01` con recurso `SILLON`, `RN-04`, `RN-11`, recursos inactivos o inexistentes | `409 OFERTA_NO_DISPONIBLE` |

Después del rollback, para `HUECO_YA_TOMADO` y `OFERTA_VENCIDA` corre una transacción nueva (`REQUIRES_NEW`). Esa transacción marca `TOMADA` las ofertas `ENVIADA` del hueco (en el primer caso) o `VENCIDA` la oferta (en el segundo), para que los `GET` siguientes informen el estado real. La entrada no se toca.
- *Alternativa descartada*: responder `410 Gone` para la oferta vencida. `EstadoError` no lo admite y la KB solo distingue `404` y `409`. Se usa `409` con un código propio.

Los códigos `OFERTA_NO_ENCONTRADA` (404), `HUECO_YA_TOMADO`, `OFERTA_VENCIDA` y `OFERTA_NO_DISPONIBLE` (409) son `ReglaNegocioException` de `listaespera.oferta`. Salen como Problem Details por el `ManejadorErrores` existente.

### D5. Consulta `GET` sin bloquear
`TomaDeOferta.consultar(token)` (`@Transactional(readOnly = true)`) busca la oferta por hash en cualquier estado, con un nuevo `OfertasHueco.buscarPorToken` que no filtra por estado. Lee la entrada y calcula el estado visible:
- `TOMADA` y `VENCIDA` salen tal cual de la oferta;
- `ENVIADA` con `ahora >= hueco_inicio` se informa `VENCIDA`;
- `ENVIADA` con la entrada no `ACTIVA` se informa `NO_DISPONIBLE`;
- en otro caso, `DISPONIBLE`.

El fin informado es `hueco_inicio + duración por defecto de la prestación de la entrada`. Los nombres salen de `ContenidoAvisoOferta`, que ya resuelve profesional y prestación para el email de C-36. El DTO `OfertaPublica` no lleva ids ni datos del paciente.

### D6. Respuesta `201` con el token del turno
`TurnoTomado` contiene inicio, fin, estado, profesional, prestación y `token`. Su `toString` oculta el token, como `TokenEmitido`. Se devuelve el token una sola vez porque RN-25 indica mostrarlo en la pantalla de confirmación, y C-49 puede llevar al paciente directo a `/turno/{token}`.
- *Alternativa descartada*: mandarlo solo por email. Es más restrictivo, pero obliga a C-49 a pedirle al paciente que abra el correo para ver su turno, y quien tiene el token de la oferta ya es el paciente identificado.

### D7. Reconstructor de `CONFIRMACION_RESERVA` en `shared.notificacion`
Se agrega `DatosConfirmacionReserva implements ReconstructorDatosNotificacion`, al lado de `DatosRecordatorios` y con la misma lógica:
- bloquea el turno;
- exige estado `PROGRAMADO`/`CONFIRMADO`, el mismo `inicio_referencia` y que no haya empezado;
- toma el email actual del paciente y emite un token nuevo con `emitirSiVigente`.

`descartarSiFaltanDatos` devuelve `true` para `CONFIRMACION_RESERVA`. No depende de la oferta, así que sirve también para C-41. El mensaje de descarte de `procesarVencidas` (hoy dice "Recordatorio descartado") se generaliza a "Notificación descartada: turno o destinatario no vigente".

### D8. Protección de la ruta de ofertas
`RutaConTokenDeTurno` pasa a reconocer dos prefijos, `/api/public/turnos/` y `/api/public/ofertas/`, y la referencia segura del error se elige según el prefijo (`/api/public/ofertas`). `ProteccionEnlaceTurnoConfig` no cambia: hereda el `no-store` y la limpieza del `path` para la ruta nueva.

Los logs de `TomaDeOferta` llevan solo ids de oferta, entrada y turno. `OfertaHueco.toString` ya oculta el hash, y `TurnoTomado` y `TokenEmitido` ocultan el token.

## Risks / Trade-offs

- **[Deadlock entre tomas o con un alta del personal]** Todas las tomas y el alta del personal empiezan por el lock del profesional. La toma después bloquea filas de oferta y entrada que el alta del personal nunca toma, así que no hay ciclo (D3).
  → Un test de carrera con Testcontainers cubre dos tomas simultáneas y una toma contra un alta de recepción.
- **[Sillón elegido por lectura previa]** Entre la consulta de solapes y el lock del sillón, otro profesional puede ocuparlo. Entonces la validación posterior al lock (o la `EXCLUDE`) responde `RN-01 SILLON` → `OFERTA_NO_DISPONIBLE`, aunque quizás había otro sillón libre.
  → Es un caso raro y el paciente puede reintentar. Se acepta en lugar de bloquear todos los sillones (D2).
- **[El token del turno viaja en la respuesta]** Más exposición que solo por email.
  → Respuesta `no-store`, HTTPS y el token nunca en logs. Se puede revertir sin cambiar las specs del alta, solo el requisito de la respuesta.
- **[Coordinación con C-41]** Lionel va a usar `CrearTurnoOnlineService` y `DatosConfirmacionReserva`.
  → Se avisa en el PR. Si C-41 llega antes, se integra su versión en lugar de duplicarla.
- **[El correo se arma fuera de la transacción]** Si la aplicación cae entre el commit y el registro de la notificación, el turno existe sin email.
  → Es el mismo riesgo que C-36 ya acepta. El paciente tiene el token en la respuesta `201`.

## Migration Plan

Sin migración de datos. Hibernate `ddl-auto` no agrega columnas, porque las consultas y los `UPDATE` nuevos usan columnas existentes. Rollback: revertir el PR. Las ofertas `TOMADA` y las entradas `ASIGNADA` que hayan quedado son estados válidos del modelo actual.

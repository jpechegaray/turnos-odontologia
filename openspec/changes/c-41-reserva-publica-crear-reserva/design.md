# Design

## Context

La motivación está en `proposal.md`. Este es el estado del código en `master` (`f913eff`) que condiciona el diseño:

- **`CrearTurnoService.crear`** (`agenda`) tiene `@PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")` y crea siempre con `Turno.nuevo(...)`, que fija `CanalTurno.INTERNO`. Un llamado anónimo desde `/api/public/**` lo rechazaría. El núcleo (advisory locks por profesional y sillón, `ValidadorTurno`, `saveAndFlush` con la traducción de `EXCLUDE` a `409 RN-01` y el evento `TurnoCreado`) es justo lo que hay que reusar.
- **`DisponibilidadService.consultar`** devuelve `HorarioDisponible(inicio, fin, sillonId)` con el primer sillón activo libre de cada horario. `DisponibilidadPublica` lo envuelve con los recortes de RN-27 y el 404 uniforme `RECURSO_NO_DISPONIBLE`.
- **`ServicioTokenTurno.emitir(turnoId)`** necesita una transacción activa (bloquea la fila del turno) y devuelve el token en claro una sola vez.
- **`ServicioNotificaciones.registrarYEnviar`** registra la fila en `REQUIRES_NEW` y envía en el mismo hilo. Nunca propaga el fallo: deja `FALLIDA` y programa el reintento. Para reintentar le pide los datos a un `ReconstructorDatosNotificacion`, porque la fila no guarda el cuerpo (que lleva el token). Así lo hace C-37 con `DatosRecordatorios`.
- **`PacienteRepositorio.porEmail`** devuelve un `Optional` (`findByEmailIgnoreCase`). El email no es único, así que con dos fichas iguales esa consulta falla. RN-28 necesita una lista.
- **`Paciente`** ya tiene `consentimientoDatosEn` y no tiene versión. **`Turno`** no tiene campos de consentimiento: es un record de 19 componentes con 13 construcciones en el código.
- **`ReemplazarConfiguracion`** exige todas las claves del enum. Por eso agregar parámetros obliga a actualizar `features/configuracion` en el front, que hoy manda siete.
- **Política visual:** `system-only` (Revision 1, aprobada).

## Goals / Non-Goals

**Goals:**

- Que una reserva online pase exactamente por las mismas reglas y la misma garantía de concurrencia que el alta del personal.
- Que ninguna reserva modifique una ficha existente y que la respuesta no revele si la ficha ya existía.
- Que el tope por email y la creación de fichas no se rompan ante reservas simultáneas.
- Que un fallo de SMTP no pierda la reserva ni el token.

**Non-Goals:**

- Fusionar fichas duplicadas (SU-04).
- Elegir "cualquier profesional" (PA-09, que sigue abierta).
- Suprimir datos o borrarlos automáticamente por retención (PA-08 §4).
- Pantalla de reserva y texto de consentimiento, que son de C-48.

## Decisions

### D1. Alta `ONLINE` en `agenda`, sobre el mismo núcleo

Se agrega `CrearTurnoService.crearOnline(SolicitudTurno)`, sin `@PreAuthorize` y con `@Transactional(propagation = MANDATORY)`, y `Turno.nuevoOnline(...)`, con canal `ONLINE` y sin usuario creador. Las dos altas comparten un método privado (bloqueo, validación, guardado y evento). El método público de personal no cambia.

- *Alternativa descartada:* que `reservapublica` llame a `TurnosPersistencia` y `ValidadorTurno` directamente. Duplicaría el orden de bloqueos y validaciones, que es justo lo que la ficha prohíbe.
- *Alternativa descartada:* ejecutar con un contexto de seguridad "de sistema". Ocultaría quién puede crear turnos y ensuciaría la auditoría.
- `MANDATORY` garantiza que solo se llame dentro de la transacción de la reserva. El endpoint público sigue siendo la única puerta anónima.

### D2. Orquestación en dos fases

`ReservaOnline.reservar(...)` vive en `reservapublica.reserva.application` y no es transaccional. Abre una transacción con `TransactionTemplate` que hace:

1. Validar el consentimiento (422) y los recursos con `DisponibilidadPublica` (404 uniforme).
2. Validar la anticipación y el horizonte (422 `RN-27`) con los mismos bordes que el recorte de `DisponibilidadPublica`: `inicio >= ahora + anticipación` y `inicio + duración <= ahora + horizonte`. Así todo horario ofrecido se puede reservar.
3. Tomar un advisory lock por email normalizado (D4).
4. Contar los turnos activos del email y comparar con el tope.
5. Resolver o crear la ficha.
6. Elegir el sillón (D3).
7. Llamar a `crearOnline`.
8. Registrar el consentimiento (D5).
9. Emitir el token.

Después del commit, y fuera de la transacción, llama a `registrarYEnviar(CONFIRMACION_RESERVA, ...)`. Así el email nunca sale para un turno que se revirtió. Como el servicio de notificaciones no propaga fallos, la respuesta 201 sale igual.

- *Alternativa descartada:* `@TransactionalEventListener(AFTER_COMMIT)` sobre `TurnoCreado`. El listener no tiene el token en claro y el evento también lo disparan las altas internas.

### D3. Sillón: primer sillón activo libre según la disponibilidad

Se consulta `DisponibilidadService.consultar(profesional, prestacion, inicio, inicio + duración)` y se busca el `HorarioDisponible` cuyo `inicio` coincide con el pedido.

- **Si existe**, su `sillonId` (el primer sillón activo libre ordenado por id) es el que se pasa a `crearOnline`.
- **Si no existe**, no se responde 409 directamente: la disponibilidad también devuelve vacío cuando el inicio cae fuera de la atención del profesional o en un bloqueo (RN-04), o fuera de la grilla de 5 minutos (RN-09). Se pasa a `crearOnline` el primer sillón activo del consultorio (por id), y `ValidadorTurno` decide en su orden habitual (`RN-11`, `RN-13`, `RN-09`, `RN-04` y recién después `RN-01`). Así un horario inválido responde el mismo `422` que el alta del personal, y solo un horario válido pero ocupado llega a `409 RN-01`, que `ReservaOnline` atrapa para adjuntar las alternativas (D8).
- **Si no hay ningún sillón activo**, se responde `409 RN-01` con alternativas sin llamar a la agenda.

`crearOnline` revalida siempre dentro del lock: si otra transacción ganó entre la consulta y el lock, la validación o el `EXCLUDE` devuelven `409 RN-01`.

### D4. Lock por email para el tope y para la deduplicación

`pg_advisory_xact_lock(41_001, hash(email normalizado))` se toma en la misma transacción, antes de contar y de resolver la ficha. Sin el lock, dos reservas simultáneas del mismo email podrían superar el tope o crear dos fichas nuevas. El lock se toma **antes** que los de la agenda (primero el email, después el profesional y después el sillón), siempre en el mismo orden, así que no puede haber ciclos. Una colisión de hash solo serializa de más, igual que en C-19.

### D5. Dónde se guarda el consentimiento

- **Ficha nueva:** se guardan `Paciente.consentimientoDatosEn = reloj.instant()` y una columna nueva `consentimiento_version`. Se agrega el componente `consentimientoVersion` al record `Paciente`, que tiene 8 construcciones.
- **Ficha existente:** se crea una tabla nueva `consentimiento_turno` en `reservapublica` (`turno_id` único, `aceptado_en timestamptz`, `version`). Cumple el "consentimiento propio en el turno" de PA-08 §5 sin agregar dos componentes al record `Turno`. Eso obligaría a tocar 13 construcciones en `agenda`, y además cada `saveAndFlush(TurnoJpa.de(turno))` de cancelar o reprogramar pisaría las columnas con `null` si el dominio no las transporta.
- **Punto de revisión:** si preferís columnas literales en `turno`, cambia D5 y las tareas 3.x, pero no cambian las specs.

### D6. Deduplicación (RN-28)

- Nuevo método `PacienteRepositorio.activosPorEmail(email)`, con JPQL parametrizado, `lower(trim(email))` y orden por `id` ascendente.
- El teléfono se compara solo por sus dígitos. Un teléfono `null` (la ficha puede tener solo email) o sin ningún dígito nunca coincide, ni siquiera con otro vacío.
- La elección es: una sola ficha, la que coincide en teléfono, o la más antigua.
- La ficha nueva se crea con un puerto `AltaPacienteReservaOnline` de `pacientes`, sin la autorización de personal. Ese puerto aplica `Paciente.nuevo(...)` y guarda el consentimiento.
- El `porEmail` existente no cambia.

### D7. Tope por email

Nuevo método de `TurnosActivosConsulta`: `contarFuturosActivos(Collection<Long> pacienteIds, Instant ahora)`, con JPQL `count` sobre `PROGRAMADO` y `CONFIRMADO` con `inicio > :ahora`. Las fichas son las activas del email (D6). Si el resultado es mayor o igual que el tope, responde 422 `RN-27` con `limite = TOPE_EMAIL`.

### D8. 409 con alternativas

Cuando el hueco está ocupado (D3, o un `RN-01` que propaga `crearOnline`), la transacción se revierte. Después se consulta `DisponibilidadPublica.consultar(prof, prest, inicio, inicio + 7 días)`, que ya recorta al horizonte, y se toman los 5 primeros horarios posteriores al pedido. Se arma una `ReglaNegocioException("RN-01", 409, ..., {alternativas})` nueva, sin la propiedad `recurso`. La ventana de 7 días y el máximo de 5 son constantes de `reservapublica`.

### D9. Versión vigente del consentimiento

Constante `ConsentimientoDatos.VERSION_VIGENTE = "2026-11-v1"` en `reservapublica`. No es un parámetro de configuración, porque cambiar la versión implica cambiar el texto en el front (C-48) en el mismo PR. Se expone en `GET /api/public/consultorio` para que el front pueda comprobarla en un test.

### D10. Reintento de `CONFIRMACION_RESERVA`

`ReconstructorConfirmacionReserva` vive en `reservapublica`, implementa `ReconstructorDatosNotificacion` y devuelve `descartarSiFaltanDatos = true` para su tipo. Bloquea el turno. Si está `PROGRAMADO` o `CONFIRMADO` y no empezó, emite un token nuevo con `emitirSiVigente` (sin revocar el anterior, como C-37) y arma el enlace con `APP_PUBLIC_URL`. Si no, lo descarta.

### D11. Datos del consultorio

Se agregan tres valores a `ParametroConfiguracion`:

- `DOMICILIO_CONSULTORIO`: texto.
- `EMAIL_CONTACTO_CONSULTORIO`: texto no blanco. No se valida el formato del email, para no abrir un tipo de validación nuevo en RN-36.
- `RETENCION_ANIOS`: entero de 1 a 20.

`ConfiguracionServicio` suma tres lecturas tipadas. `GET /api/public/consultorio` devuelve un DTO `ConsultorioPublicoDto` con cinco campos y nunca el mapa completo.

### D12. API

- **Pedido** `SolicitudReservaPublica` (DTO con Bean Validation): `prestacionId`, `profesionalId`, `inicio` (`OffsetDateTime`), `nombre`, `apellido`, `email` (`@Email`), `telefono`, `obraSocial`, `aceptaTratamientoDatos` (`Boolean`, sin `@NotNull`) y `versionConsentimiento` (sin `@NotBlank`).
- **Respuesta 201** `ReservaCreadaDto`: `inicio`, `fin`, `prestacion` y `profesional` (nombres), y `token`. No incluye ids de paciente, sillón ni turno.
- Los DTO redefinen `toString` para no imprimir el token ni los datos personales.

### UI Mockups

- Design system revision: 1
- Decision: no (project policy), 2026-11-25. Solo se agregan tres campos al formulario existente de `/configuracion` con los componentes actuales.

## Risks / Trade-offs

- [El `PUT /api/configuracion` pasa a diez claves y un cliente viejo recibe 400] → El front se actualiza en el mismo PR. No hay otros clientes.
- [`crearOnline` es una puerta sin autorización en `agenda`] → Exige una transacción ya abierta (`MANDATORY`), solo la usa `ReservaOnline` y un test de arquitectura o de seguridad comprueba que el endpoint de personal sigue rechazando a un anónimo.
- [La tabla `consentimiento_turno` está separada del turno] → La tabla es 1:1 por `turno_id` único. La retención de PA-08 la borra junto con el turno.
- [Las alternativas se calculan después del rollback y pueden quedar viejas] → Son orientativas (RN-08). Una nueva reserva se revalida.
- [Una colisión de hash en el lock por email serializa reservas ajenas] → Solo afecta la latencia, nunca la corrección.
- [Varias fichas con el mismo email y ningún teléfono coincidente se asignan a la más antigua] → Es determinista y no modifica ninguna ficha. La fusión queda en SU-04.

## Migration Plan

`ddl-auto` crea la columna `paciente.consentimiento_version` (nullable) y la tabla `consentimiento_turno`. El inicializador de C-08 repone las tres claves nuevas en bases existentes. No hay datos que migrar, y para volver atrás alcanza con revertir el PR, porque las columnas y la tabla nuevas quedan sin uso.

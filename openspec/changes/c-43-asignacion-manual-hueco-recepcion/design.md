# Design

## Context

Ver `proposal.md` para la motivación. El estado actual que condiciona el enfoque:

- `CrearTurnoService.crear(SolicitudTurno, notas, creadoPorUsuarioId)` (C-19) ya aplica todas las validaciones del alta, bloquea profesional y sillón con advisory locks en orden fijo, guarda con `saveAndFlush` para que la violación de las `EXCLUDE` se traduzca a `409 RN-01` y publica `TurnoCreado`. Es `@Transactional`; llamado desde otra transacción, se une a ella.
- `DisponibilidadService.consultar(profesionalId, prestacionId, desde, hasta, duracionMin)` (C-26) devuelve `HorarioDisponible(inicio, fin, sillonId)` solo para huecos libres que respetan horario y bloqueos. Es el cálculo reutilizable que la ficha de C-26 prevé para C-43.
- `BuscadorCompatibles`/`CompatibilidadListaEspera.esCompatible` (C-31) implementan RN-22 como función pura; los datos de la prestación y si el profesional la realiza llegan por `ConsultaPrestaciones` y `ProfesionalRealizaPrestacion`.
- `EntradaListaEspera.asignar(turnoId)` pasa a `ASIGNADA` y falla con `ENTRADA_NO_ACTIVA` (409) si no está `ACTIVA`. `ListaEsperaJpa` no tiene `@Version` ni bloqueo de lectura.
- `OfertaHueco` (C-36) es un record inmutable con estado `ENVIADA`/`TOMADA`/`VENCIDA`; la persistencia expone `bloquear(id)` y `vencer(ahora)`, pero no una forma de marcar `TOMADA` en bloque.
- El manejador de errores traduce cualquier `ReglaNegocioException(codigo, EstadoError, mensaje)` a Problem Details; `SecurityConfig` restringe las escrituras de `/api/**` a `ADMIN`/`RECEPCION` (con reglas explícitas por endpoint para dar `403` antes de validar el cuerpo).

## Goals / Non-Goals

**Goals:**

- Un endpoint de backend que asigne un hueco a una entrada y deje la entrada cerrada y las ofertas tomadas, con todas las garantías del alta.
- Resolver la compatibilidad y el sillón de forma determinística y reutilizando los servicios existentes (C-26, C-31, C-19).
- Atomicidad: o se crea el turno y se cierra la entrada y las ofertas, o no cambia nada.
- Errores claros: `404`, `409` (`RN-01`/`ENTRADA_NO_ACTIVA`), `422` (`ENTRADA_INCOMPATIBLE`), `401`/`403`.

**Non-Goals:**

- Tomar el hueco desde el enlace público de la oferta (C-42) y el frontend de la lista de espera (C-47).
- Cambiar las reglas de compatibilidad (C-31), el aviso automático (C-36) o el alta de turnos (C-19).
- Permitir sala de espera con prioridad, lista de turnos en espera o asignación automática: la decisión del líder es "el primero que confirma" (RN-24, SU-15).

## Decisions

### D1. Capability nueva `asignacion-hueco` y subpaquete `listaespera.asignacion`

Se crea una capability nueva, como hizo C-36 con `oferta-hueco`, porque la asignación manual es una capacidad con comportamiento propio (endpoint, validaciones, cierre de entrada y de ofertas) que evoluciona aparte de la compatibilidad (`lista-espera`) y del aviso (`oferta-hueco`). El código va en `listaespera.asignacion` con las cuatro capas (`domain`, `application`, `infrastructure`, `web`), siguiendo el patrón de `listaespera.oferta`; así se respeta la coordinación RM-06 con C-42 (subpaquetes distintos).

- *Alternativa descartada:* extender `lista-espera`. Mezclaría la consulta/gestión de entradas con la creación de turnos y forzaría a `lista-espera` a depender del alta de agenda.

### D2. Duración del turno a partir de la prestación de la entrada

El cuerpo solo trae `inicio`. La duración se toma de `ConsultaPrestaciones.buscarPorId(entrada.prestacionId()).duracionDefaultMin()` (RN-24, SU-14 d) y el hueco evaluado es `[inicio, inicio + duraciónDefaultMin)`. No se acepta `duracionMin` en el cuerpo: la asignación manual reproduce lo que el paciente habría obtenido al tomar la oferta, con la misma duración de la prestación.

- *Alternativa descartada:* aceptar `duracionMin` opcional. Agregaría una decisión de negocio no pedida y podría crear turnos que no caben en el hueco que se está asignando.

### D3. Compatibilidad previa con la función pura de RN-22

Antes de crear el turno se valida `CompatibilidadListaEspera.esCompatible(entrada, hueco, prestacion, profesionalRealiza)`, con `prestacion = new PrestacionParaHueco(duracionDefaultMin, activa)` y `profesionalRealiza = ProfesionalRealizaPrestacion.realiza(profesionalId, prestacionId)`. Si no es compatible, se lanza `ReglaNegocioException("ENTRADA_INCOMPATIBLE", NO_PROCESABLE_422, ...)`. Se usa la función pura en vez de `BuscadorCompatibles.compatibles(Hueco)` porque la consulta de candidatas exige fecha y devuelve todas las entradas; acá ya tenemos la entrada puntual y solo necesitamos una respuesta sí/no, sin depender del tope ni de la deduplicación del aviso.

- *Nota:* la compatibilidad no valida que el hueco esté libre ni dentro del horario; eso lo hace `CrearTurnoService` (RN-01, RN-04). Una franja fuera de horario se informa por el alta (`422` de agenda), no como `ENTRADA_INCOMPATIBLE`.
- *Alternativa descartada:* reutilizar `BuscadorCompatibles.compatibles` y comprobar pertenencia. Es correcto pero más costoso y arrastra la semántica de sugerencias (orden, fecha), innecesaria para un id puntual.

### D4. Sillón opcional determinístico con `DisponibilidadService`

- Si el cuerpo trae `sillonId`, se usa tal cual; `CrearTurnoService` revalida que exista, esté activo y libre (si no, `422`/`409` de agenda).
- Si no trae, se pide `DisponibilidadService.consultar(profesionalId, prestacionId, inicio, inicio + duración, duración)` y se buscan los `HorarioDisponible` cuyo `inicio` sea exactamente el `inicio` del hueco. Entre esos:
  1. se prefiere el `sillonSugeridoId` de una oferta del hueco (mismo `profesionalId` y `huecoInicio`) si aparece en la lista de libres (SU-14 d);
  2. si no, se usa el de menor `sillonId` (primero activo libre, orden estable).
  Si no hay ningún `HorarioDisponible` en ese inicio, se usa el sillón sugerido por la oferta si está activo o, si no, el primero activo, y se deja que `CrearTurnoService` determine el error exacto: `409 RN-01` si el profesional o el sillón están ocupados, `422 RN-04`/`RN-13` si el hueco está fuera de horario o en el pasado. La creación siempre revalida y la `EXCLUDE` es la garantía final.

- *Alternativa descartada:* resolver el sillón con `SillonConsulta.listarActivos()` + consultar solapamientos a mano. Duplicaría el cálculo de C-26 y podría divergir de las reglas de horario/bloqueo; `DisponibilidadService` ya es el núcleo reutilizable previsto para C-43.
- *Ajuste en apply:* `DisponibilidadService` informa un solo `HorarioDisponible` por inicio, con el sillón libre de menor id, así que el sugerido casi nunca figura en esa lista. Cuando el hueco tiene horario disponible y el sugerido no es el informado, se verifica aparte que esté activo (`SillonConsulta`) y sin turnos que solapen (`ConsultaOcupacionDisponibilidad.buscar`, la misma consulta de ocupación que usa C-26). Si está libre, se usa; si no, se usa el informado.
- *Trade-off aceptado:* entre la consulta de disponibilidad y el `INSERT` el sillón puede ocuparse; en ese caso gana la `EXCLUDE` y el pedido recibe `409`, que es exactamente el comportamiento de RN-15.

### D5. Transacción única y concurrencia de la entrada

`AsignarHuecoService.asignar(...)` es `@Transactional` y ejecuta en orden:

1. `listaEspera.bloquear(id)` (nuevo, `PESSIMISTIC_WRITE` sobre la fila de la entrada) o `404 ENTRADA_NO_ENCONTRADA`.
2. Verificar `estado == ACTIVA`; si no, `409 ENTRADA_NO_ACTIVA` (la transición `asignar` ya lo hace, pero se valida antes de tocar nada de agenda).
3. Cargar la prestación y la compatibilidad (D3); si no es compatible, `422 ENTRADA_INCOMPATIBLE`.
4. Resolver el sillón (D4).
5. `crearTurno.crear(new SolicitudTurno(...), notas, identidadId)`: valida RN-01..RN-04/RN-09/RN-11/RN-13, bloquea recursos, inserta y publica `TurnoCreado` en la misma transacción.
6. `listaEspera.guardar(entrada.asignar(turno.id()))`.
7. `ofertas.tomarDelHueco(profesionalId, inicio)` (D6).

Como todo comparte la transacción, un fallo en cualquier paso revierte el turno, la asignación y las ofertas. El bloqueo de la entrada serializa dos asignaciones concurrentes sobre la **misma** entrada (la segunda ve `ASIGNADA` y recibe `409 ENTRADA_NO_ACTIVA`); dos entradas que compiten por el **mismo hueco** se serializan en el alta (advisory locks + `EXCLUDE`) y la que pierde recibe `409 RN-01`. El orden de locks sigue siendo el del alta (primero profesional, después sillón) porque el bloqueo de la entrada es una fila independiente que no participa del ciclo de las `EXCLUDE`.

- `ListaEsperaPersistencia.bloquear(id)`: `@Lock(PESSIMISTIC_WRITE)` en `ListaEsperaJpaRepository` (la entidad no tiene `@Version`; el lock pesimista es la opción más simple y no cambia el esquema).
- *Alternativa descartada:* `@Version` optimista en `ListaEsperaJpa`. Requiere tocar la tabla y traducir la excepción a 409; el lock pesimista ya existe como patrón en ofertas y turnos.

### D6. Marcar `TOMADA` todas las ofertas del hueco

Se agrega `OfertaHueco.tomar()` al dominio (devuelve una oferta `TOMADA`, solo si estaba `ENVIADA`) y una actualización en bloque en la persistencia:

```
@Modifying
@Query("update OfertaHuecoJpa o set o.estado = EstadoOferta.TOMADA
       where o.estado = EstadoOferta.ENVIADA
         and o.profesionalId = :profesionalId and o.huecoInicio = :huecoInicio")
int tomarDelHueco(profesionalId, huecoInicio)
```

- **Criterio de matching:** `(profesionalId, huecoInicio)`. Una oferta pudo crearse con un `huecoFin` mayor que la duración de la entrada (el hueco liberado era más largo); igual pertenece al mismo hueco que recepción está tomando, porque su `huecoInicio` es el mismo. Comparar por el fin dejaría ofertas `ENVIADA` de un hueco ya tomado.
- **Ajuste en apply:** C-42 ya trae `OfertaHuecoJpaRepository.tomarDelHueco(profesionalId, inicio, fin)`, que compara también el fin. Para no cambiar su comportamiento, la consulta de C-43 se llama `tomarDelHuecoPorInicio` en el repositorio y se expone como `OfertasHuecoPersistencia.tomarDelHueco(profesionalId, huecoInicio)`. Para el sillón sugerido (D4) se agrega `sugeridaPara(entradaId, profesionalId, huecoInicio)`: devuelve la oferta de la entrada si existe y, si no, otra del mismo hueco.
- **Idempotencia:** el `UPDATE` solo toca `ENVIADA`; repetirlo no cambia nada. Las ofertas `VENCIDA` o ya `TOMADA` se conservan.
- **RN-38:** la oferta pasa a `TOMADA` porque "otro la tomó"; por eso se marcan todas las del hueco, no solo la de la entrada asignada.
- *Alternativa descartada:* leer cada oferta y guardarla con `save`. Más lenta y sujeta a conflictos de versión; la actualización por JPQL es atómica y no necesita token ni lock por oferta porque la transacción de la asignación ya ordena el hueco.

### D7. Roles, sesión y CSRF

- El endpoint vive en `listaespera.asignacion.web.AsignarHuecoController` (`POST /api/lista-espera/{id}/asignar`).
- `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")` en el servicio, igual que `ServicioListaEspera`.
- Se agrega una regla explícita en `SecurityConfig` para `POST /api/lista-espera/{id}/asignar` con `hasAnyRole("ADMIN", "RECEPCION")`, antes de la regla genérica de `POST /api/**`, para que el `ODONTOLOGO` reciba `403` antes de validar el cuerpo. Sin sesión, la entrada de seguridad responde `401`; el CSRF se exige como en el resto de escrituras.
- El usuario que asigna se pasa como `creadoPorUsuarioId` a `CrearTurnoService` (`@AuthenticationPrincipal IdentidadPersonal`), para trazabilidad del turno.

### D8. Contrato HTTP, notas y respuesta

```
POST /api/lista-espera/{id}/asignar
{ "profesionalId": 3, "inicio": "2026-12-03T10:00:00-03:00", "sillonId": null, "notas": "..." }
→ 201 Created  RespuestaTurno (id, pacienteId, profesionalId, sillonId, prestacionId, inicio, fin, ...)
```

- `inicio` es `OffsetDateTime` obligatorio (una fecha sin zona da `400`, DD-05).
- `sillonId` y `notas` son opcionales; `notas` con `@Size(max = 1000)` como en `SolicitudCrearTurno`.
- La respuesta es la representación del turno (reutiliza `agenda.web.RespuestaTurno`) y `Location` apunta al turno, igual que `POST /api/turnos`. Se devuelve el turno y no la entrada porque es el recurso que crea la operación; el front (C-47) puede refrescar la entrada con `GET /api/lista-espera/{id}`.
- *Alternativa descartada:* devolver un compuesto `{ turno, entrada }`. No hay precedente y agranda el contrato sin necesidad.

### D9. Errores y códigos

| Situación | HTTP | Código |
|---|---|---|
| Entrada inexistente | 404 | `ENTRADA_NO_ENCONTRADA` |
| Entrada no `ACTIVA` | 409 | `ENTRADA_NO_ACTIVA` |
| Entrada incompatible con el hueco | 422 | `ENTRADA_INCOMPATIBLE` |
| Hueco ya tomado / solapamiento | 409 | `RN-01` |
| Hueco fuera de horario/bloqueo, recursos inválidos | 422 | `RN-04`, `RN-11`, etc. (los del alta) |
| ODONTOLOGO | 403 | Problem Details |
| Sin sesión / sin CSRF | 401 / 403 | Problem Details |

Los códigos de agenda (RN-01, RN-04, RN-11) los sigue produciendo `CrearTurnoService`/`TurnosPersistencia`, sin duplicar su traducción.

### D10. Tests: backend puro (JUnit 5 + Testcontainers + MockMvc)

- **Stack:** JUnit 5, Spring Boot Test, AssertJ, Testcontainers (PostgreSQL real) y MockMvc, con `PruebaIntegracionBase`, `DatosAgenda`, `DatosFicticios` y `RelojFijoConfig` como en `ListaEsperaCompatiblesApiIT`.
- Cada test lleva `@DisplayName` con el **nombre exacto** del escenario de `specs/asignacion-hueco/spec.md`.
- Se cubren los cuatro escenarios que pide la ficha: asignación feliz, hueco ya tomado, entrada ya cerrada y roles.
- **No aplica Vitest:** la ficha menciona tests pero este change es backend puro; el frontend de la lista de espera es C-47.
- Casos de concurrencia real (dos asignaciones simultáneas del mismo hueco) se prueban con dos transacciones/hilos como en los IT de concurrencia existentes; el resto puede usar llamadas secuenciales.

## Risks / Trade-offs

- **Carrera entre disponibilidad y `INSERT`** → la `EXCLUDE` y los advisory locks de C-19 son la garantía final; el perdedor recibe `409 RN-01`. La resolución del sillón en D4 es solo una sugerencia determinística.
- **Hueco fuera del horario de atención o en el pasado** → `DisponibilidadService` no devuelve ese inicio; se cae al sillón sugerido o al primero activo y `CrearTurnoService` responde `422` por RN-04 o RN-13 (o `409 RN-01` si el hueco está ocupado). Así el error refleja la regla incumplida y no se disfraza de hueco tomado.
- **Ofertas con `huecoFin` distinto** → el matching por `(profesionalId, huecoInicio)` toma todas las ofertas del mismo hueco aunque su fin original fuese más largo; es intencional (RN-38: "otro la tomó").
- **Bloqueo pesimista de la entrada** → puede aumentar la contención si muchas personas asignan la misma entrada; es el caso deseado y el volumen del consultorio (2 a 5 profesionales) es bajo.
- **Sin auditoría de la asignación más allá del turno** → el `turno.creadoPor` registra al usuario que asignó; no se agrega un evento ni una tabla nueva.

## Open Questions

Ninguna que cambie las specs, el enfoque o las tareas. La decisión de devolver el turno (D8) y de no aceptar `duracionMin` (D2) quedan registradas como decisiones de diseño.

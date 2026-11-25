# Design

## Context

- **Lo que ya existe:**
  - C-18 creó la lista de espera: `EntradaListaEspera`, con fechas `LocalDate` y franja `LocalTime` opcional, su persistencia y `ServicioListaEspera`.
  - C-19 dejó `ProfesionalRealizaPrestacion` y `ConsultaPrestaciones`, que da la duración por defecto.
  - C-27 publica `HuecoLiberado(profesionalId, sillonId, hueco, turnoOrigenId)`.
  - `ConfiguracionServicio.maxAvisosPorHueco()` ya existe, con default 10.
- **Lo que falta:** la regla que une todo esto (RN-22) y la selección de avisos (SU-14). C-36 las va a consumir desde un listener `AFTER_COMMIT` sin sesión de usuario.

## Goals / Non-Goals

**Goals:**
- RN-22 y SU-14 como funciones puras, testeables sin Spring.
- Una consulta de sugerencias para recepción.
- Un punto de entrada sin seguridad para C-36.

**Non-Goals:**
- Verificar que el hueco esté libre o dentro del horario.
- Crear ofertas, enviar emails o asignar turnos.

## Decisions

### D1. `Hueco` propio de `listaespera.domain`

`record Hueco(Long profesionalId, Instant inicio, Instant fin)`. Valida que no haya nulos y que `inicio < fin`, y expone `duracion()`.

No se reutiliza `agenda.domain.Intervalo`, para que el dominio de lista de espera no dependa del de agenda. C-36 convierte `HuecoLiberado` en `Hueco` en una línea.

### D2. `CompatibilidadListaEspera.esCompatible(...)`

```java
public static boolean esCompatible(EntradaListaEspera entrada, Hueco hueco,
        PrestacionParaHueco prestacion, boolean profesionalRealiza)
```

- `PrestacionParaHueco(int duracionMin, boolean activa)` es un record de dominio. Evita que el dominio importe `PrestacionResumen` de `prestaciones.application`.
- Los datos externos entran como parámetros, sin repositorios. Así la función queda pura y la arma la capa de aplicación.
- **Orden de las comprobaciones:** estado `ACTIVA` → prestación activa → realiza → preferido → duración → fecha → franja. Todas devuelven `false`; no hay excepciones de negocio.
- **Zona horaria:** fecha y franja se calculan con `ZonaHoraria.BUENOS_AIRES` (DD-05).
- **Franja:** `!inicioLocal.isBefore(franjaDesde) && !finLocal.isAfter(franjaHasta)`, con `inicioLocal` y `finLocal` como `LocalTime` del turno resultante. Además, el turno resultante debe terminar el mismo día: si cruza la medianoche, no cae en ninguna franja. Sin franja no hay restricción.
- **Fecha:** se toma la del `inicio` del hueco. Como el turno empieza al inicio del hueco (SU-14 d), es la fecha del turno.

### D3. `SelectorDeAvisos.seleccionar(compatibles, hueco, ahora, tope)`

- Si `!ahora.isBefore(hueco.inicio())`, devuelve una lista vacía.
- Si no:
  1. ordena por `creadoEn` y luego por `id`;
  2. recorre la lista y conserva la primera entrada de cada `pacienteId`, con un `LinkedHashSet` de ids;
  3. corta en `tope`.
- La deduplicación va antes del tope, así el tope cuenta pacientes distintos.
- Con `tope <= 0` devuelve una lista vacía. Hoy la configuración no lo permite, pero el selector no lo asume.

### D4. Lectura de candidatas en la base

`ListaEsperaPersistencia.candidatas(profesionalId, fecha)` hace una consulta JPQL:

```
estado = ACTIVA
and (profesionalId is null or profesionalId = :p)
and fechaDesde <= :fecha and fechaHasta >= :fecha
order by creadoEn, id
```

- La consulta solo reduce el volumen; la decisión final la toma la función pura sobre cada entrada. El filtro de fecha y profesional se repite en las dos capas a propósito.
- Para la duración se consulta `ConsultaPrestaciones` una vez por prestación distinta (un `Map` local).
- Para `realiza` se consulta `ProfesionalRealizaPrestacion` una vez por prestación distinta.
- Las entradas son pocas (un consultorio de 2 a 5 profesionales), así que no hace falta un join.

### D5. Dos puertas de entrada

- **`BuscadorCompatibles`** (`@Component`, `@Transactional(readOnly = true)`, sin `@PreAuthorize`):
  - `compatibles(Hueco)` devuelve todas las entradas compatibles ordenadas;
  - `aAvisar(Hueco)` aplica además `SelectorDeAvisos` con el `Clock` y el tope.
  - C-36 usa `aAvisar` desde el listener. Con `@PreAuthorize` fallaría por falta de sesión.
- **`ServicioListaEspera.compatibles(profesionalId, inicio, fin)`** lleva `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")`:
  1. verifica el profesional (reutiliza `verificarProfesional`, `422`);
  2. si el hueco ya empezó, devuelve una lista vacía;
  3. si no, delega en `BuscadorCompatibles.compatibles`.

### D6. Endpoint y errores

- `GET /api/lista-espera/compatibles` recibe `@RequestParam Long profesionalId` e `inicio` y `fin` como `OffsetDateTime` (ISO-8601 con zona), y se convierten a `Instant`.
- **`400`:**
  - un parámetro faltante o mal formado lo rechaza el manejador global de errores con Problem Details;
  - si `fin <= inicio`, se lanza `ResponseStatusException(BAD_REQUEST)`, como el alta de C-18.
- La ruta literal `/compatibles` tiene prioridad sobre `/{id}` en Spring MVC, así que no choca con el detalle de una entrada.

### D7. Seguridad

- `SecurityConfig` suma `GET /api/lista-espera/compatibles` para `ADMIN` y `RECEPCION`, y el servicio lleva `@PreAuthorize`.
- Hay doble barrera, como en C-27. El odontólogo no lo consulta porque las sugerencias sirven para asignar un hueco, que es una escritura que no le corresponde (RN-33). Así lo indica la ficha de C-31.

### D8. Tests

- **Unitarios, sin Spring:**
  - `HuecoTest`;
  - `CompatibilidadListaEsperaTest`: todos los escenarios de RN-22, incluidos los bordes de duración, fechas, franja y zona;
  - `SelectorDeAvisosTest`.
- **Integración con Testcontainers:**
  - `ListaEsperaPersistenciaIT` amplía la consulta de candidatas;
  - `BuscadorCompatiblesIT`: arma prestaciones, profesionales y entradas, y verifica `compatibles` y `aAvisar`;
  - `ListaEsperaCompatiblesApiIT`: escenarios de API y de acceso.
- **Reloj:** se usa el `Clock` fijo de los tests de integración existentes.
- **CSRF:** como es un `GET`, no aplica CSRF.

### D9. Rama

`juan-pablo-c-31`, creada desde `master` después del merge de #103.

## Risks / Trade-offs

- **[Riesgo] Las sugerencias pueden incluir un hueco que no está libre o que cae fuera del horario.** Mitigación: la asignación (C-43) y la oferta (C-36) crean el turno con todas las validaciones y las restricciones `EXCLUDE`, así que una sugerencia de más no produce un sobreturno.
- **[Trade-off] Duplicar los filtros en JPQL y en la función pura.** Se acepta: la consulta es una optimización y la regla vive en un solo lugar testeado.
- **[Riesgo] `listaespera` lo comparten C-36, C-42 y C-43 (RM-06).** Mitigación: C-31 solo agrega clases y un método; no cambia firmas existentes.

## Migration Plan

No hay cambios de esquema. Se agrega una consulta de lectura y un endpoint nuevo.

## Open Questions

Ninguna bloqueante.

## UI Mockups

- Design system revision: no aplica
- Decision: no (no UI) (2026-11-25)

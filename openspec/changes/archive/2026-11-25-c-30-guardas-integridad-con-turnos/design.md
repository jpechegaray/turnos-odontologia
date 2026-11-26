# Design

## Context

`C-19` dejó implementada la entidad `Turno` en `agenda` con estados `PROGRAMADO`, `CONFIRMADO`, `ATENDIDO`, `AUSENTE` y `CANCELADO`, y `C-27` agregó la cancelación de turnos (`POST /api/turnos/{id}/cancelar`). Por su parte, los catálogos de prestaciones (`C-04`), sillones (`C-05`), profesionales (`C-06`) y los bloqueos de agenda (`C-12`) dejaron pendiente la validación cruzada contra turnos activos (RN-17 y RN-37).

En la Ola 4 conviven varios changes sobre `agenda` (`C-26`, `C-28`, `C-29`, `C-30`), por lo que rige la mitigación **RM-04** de `CHANGES.md`: los changes de esta ola solo agregan archivos nuevos en `agenda`, sin editar `TurnoJpaRepository` ni `TurnosPersistencia`.

## Goals / Non-Goals

**Goals:**
- Proveer el puerto `TurnosActivosConsulta` en `agenda.application` y su adaptador JPA en `agenda.infrastructure` usando únicamente archivos nuevos en `agenda`.
- Rechazar con `409 Conflict` (`application/problem+json`) la creación de bloqueos superpuestos con turnos activos del profesional (RN-17) y la transición de activo a inactivo de sillones, profesionales y prestaciones que tengan turnos futuros activos (RN-37), adjuntando la lista de turnos en conflicto sin datos del paciente (RN-35).

**Non-Goals:**
- No se modifican `ReglaNegocioException` ni `ManejadorErrores` (ya soportan `Map<String, Object> propiedades` en `ProblemDetail`).
- No se modifican las pantallas del frontend (C-20, C-21 y C-22 ya manejan respuestas 409).
- No se cancelan ni reprograman turnos automáticamente al bloquear o desactivar.

## UI Mockups

Decision: no (no UI)

## Decisions

### 1. Puerto `TurnosActivosConsulta` y record `TurnoEnConflicto` en `agenda.application`
- **Decisión**: Definir en `ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta` la interfaz pública del módulo y un record anidado (o en el mismo paquete) `TurnoEnConflicto(Long id, Instant inicio, Instant fin, String estado)` con cuatro métodos:
  - `List<TurnoEnConflicto> enIntervaloDeProfesional(Long profesionalId, Intervalo intervalo)`
  - `List<TurnoEnConflicto> futurosDeProfesional(Long profesionalId)`
  - `List<TurnoEnConflicto> futurosDeSillon(Long sillonId)`
  - `List<TurnoEnConflicto> futurosDePrestacion(Long prestacionId)`
- **Justificación**: Respeta el contrato de `CHANGES.md` para C-30, mantiene encapsuladas las entidades JPA (`TurnoJpa`) dentro de `agenda.infrastructure` y cumple RN-35 al proyectar únicamente `id`, `inicio`, `fin` y `estado` (como `String` con el nombre del enum `EstadoTurno`), sin datos del paciente.
- **Alternativas descartadas**: Exponer la entidad de dominio `Turno` fuera de `agenda` (acopla innecesariamente a catálogos y bloqueos con detalles internos de `agenda` e incluye `pacienteId`).

### 2. Adaptador `TurnosActivosPersistencia` con `EntityManager` y `Clock` en archivo nuevo
- **Decisión**: Implementar `TurnosActivosConsulta` en `ar.edu.utn.turnos.agenda.infrastructure.TurnosActivosPersistencia` (`@Repository`) utilizando JPQL parametrizado sobre `TurnoJpa` a través de `EntityManager` y el `Clock` inyectable del sistema (`Instant.now(clock)`).
  - Estados considerados activos para RN-17 y RN-37: `t.estado IN (EstadoTurno.PROGRAMADO, EstadoTurno.CONFIRMADO)`.
  - Condición de solapamiento en RN-17: `t.profesionalId = :profesionalId AND t.inicio < :fin AND :inicio < t.fin` (intervalo semiabierto `[inicio, fin)`, RN-02).
  - Condición de turno futuro en RN-37: `t.fin > :ahora` (incluye cualquier turno pendiente de finalización al instante actual del `Clock`).
  - Ordenamiento uniforme: `ORDER BY t.inicio ASC, t.id ASC`.
- **Justificación**: Cumple estrictamente **RM-04** (cero ediciones en `TurnoJpaRepository` y `TurnosPersistencia`) y aprovecha los índices existentes `ix_turno_profesional_inicio`, `ix_turno_sillon_inicio` e `ix_turno_estado_inicio`.

### 3. Estructura uniforme del error `409 Conflict` en Problem Details
- **Decisión**: Lanzar `ReglaNegocioException` con `EstadoError.CONFLICTO_409`:
  - **En `ServicioBloqueos.crear` (RN-17)**:
    - `codigo = "RN-17"`
    - `propiedades = Map.of("turnos", conflictos)`
  - **En `ServicioSillones`, `ServicioProfesionales` y `CatalogoPrestaciones` (RN-37)**:
    - `codigo = "RN-37"`
    - `propiedades = Map.of("recurso", "SILLON" | "PROFESIONAL" | "PRESTACION", "turnos", conflictos)`
- **Justificación**: Sigue el patrón establecido por `ValidadorTurno` en C-19 (`codigo = "RN-01"`, `Map.of("recurso", ...)`), sin tocar `shared`.

### 4. Condición de disparo en bajas lógicas (solo en transición `activa/activo: true -> false`)
- **Decisión**:
  - `ServicioSillones.actualizar`: ya distingue `if (anterior.activo() && !activo) -> desactivar(...)`; la consulta `turnosActivos.futurosDeSillon(id)` se ejecuta dentro de `desactivar`.
  - `ServicioProfesionales.actualizar`: leer primero el estado actual con `profesionales.buscarPorId(id)` (lanzando 404 `PROFESIONAL_NO_ENCONTRADO` si no existe); solo cuando `anterior.activo() && !activo` invocar `turnosActivos.futurosDeProfesional(id)`.
  - `CatalogoPrestaciones`: tanto en `darDeBaja(id)` como en `modificar(id, nombre, duracion, activa)` verificar `if (prestacion.isActiva() && !activa)` antes de desactivar. Así `DELETE` sobre una prestación ya inactiva sigue siendo idempotente (`204`) y `PUT` sobre una inactiva permite editar nombre o duración sin bloqueo falso.

## Risks / Trade-offs

- **[Risk] Colisión de merge en `agenda` con otros changes de la Ola 4** → Mitigación: `TurnosActivosConsulta` y `TurnosActivosPersistencia` son archivos 100 % nuevos; no se toca ningún archivo existente de `agenda`.
- **[Risk] Tests unitarios existentes de `ServicioSillones`, `ServicioProfesionales`, `ServicioBloqueos` o `CatalogoPrestaciones` que instancian el servicio por constructor** → Mitigación: revisar y actualizar constructores/mocks en los tests existentes como red de seguridad (Safety Net de TDD) antes de agregar los casos nuevos.

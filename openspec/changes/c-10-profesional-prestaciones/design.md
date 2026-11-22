# Design

## Context

Ver `proposal.md` (Why) y `specs/profesional-prestaciones/spec.md` (requisitos). Estado actual relevante, observado en `backend/`:

- **C-04 (`prestaciones`)**: la entidad `Prestacion` y su repositorio son *package-private* en `prestaciones.infrastructure`. Hacia afuera solo se publica el puerto `ConsultaPrestaciones.buscarPorId(Long) → Optional<PrestacionResumen>` (`id`, `nombre`, `duracionDefaultMin`, `activa`), que no exige sesión. Sus códigos de error usan guiones (`PRESTACION-NO-ENCONTRADA`).
- **C-06 (`profesionales.profesional`)**: la entidad `Profesional` también es *package-private*. Se publican `ProfesionalConsulta.buscarPorId` (sin seguridad) y `ServicioProfesionales.detalle(id)`, que aplica `@PreAuthorize("hasAnyRole('ADMIN','RECEPCION','ODONTOLOGO')")` y la titularidad del odontólogo (403 para un id ajeno, exista o no, y 404 `PROFESIONAL_NO_ENCONTRADO` para roles con lectura global). Los códigos usan guion bajo.
- **C-12 (`profesionales.bloqueo`)** fijó el patrón del subpaquete hermano: entidad con `Long profesionalId` (sin `@ManyToOne`), FK `fk_bloqueo_profesional` agregada en `schema-constraints.sql` con un bloque `DO $$` idempotente, servicio en `application` con `@PreAuthorize` por método y lectura que reutiliza `ServicioProfesionales.detalle(id)`.
- **Errores**: `ReglaNegocioException(codigo, EstadoError, mensaje)` → `ManejadorErrores` lo traduce a Problem Details; `AccessDeniedException` → 403; Bean Validation → 400; sin sesión → 401 (`RespuestasSeguridadProblemDetail`). CSRF ya protege los `PUT` de `/api/**`.
- **Esquema**: Hibernate `ddl-auto` (DD-08) y `schema-constraints.sql` corre después (`defer-datasource-initialization`), con separador `^^^`.

## Goals / Non-Goals

**Goals:**
- Asociación profesional-prestación persistida con unicidad e integridad referencial garantizadas por la base.
- API de consulta y reemplazo con la matriz de permisos del catálogo de profesionales.
- Puerto `ProfesionalRealizaPrestacion` estable para C-19 y C-38.
- Cero ediciones en `Profesional.java`, `Prestacion.java` y en los demás archivos de C-04, C-06, C-11 y C-12 (RM-01).

**Non-Goals:**
- Validar que profesional y prestación estén activos al crear un turno: lo hace C-19 con sus propios códigos.
- Catálogo público de profesionales por prestación (`/api/public/...`): queda para C-38, que puede ampliar el puerto.
- Guardas de baja con turnos futuros (RN-37): C-30.
- Front de asignación de prestaciones: C-21.

## Decisions

### D1 — Referencias por id con FK en SQL, en lugar de `@ManyToOne`
La ficha de CHANGES.md pide `@ManyToOne` a `Profesional` y `Prestacion`, pero ambas clases son *package-private* en otros paquetes: no se pueden referenciar sin editarlas, y editarlas viola RM-01. Además, la regla compacta de `jpa-patterns` del proyecto indica "entre módulos referenciar por id, no por entidad de otro módulo", y C-12 ya resolvió el mismo problema así.

Se mapea `ProfesionalPrestacion` con clave compuesta `@EmbeddedId ProfesionalPrestacionId(profesionalId, prestacionId)` sobre columnas `profesional_id` y `prestacion_id` (coincide con el ERD de la KB: PK compuesta, sin id sustituto). La PK garantiza la pareja única. Las FK `fk_profesional_prestacion_profesional` y `fk_profesional_prestacion_prestacion` se agregan en `schema-constraints.sql` con bloques `DO $$ ... IF NOT EXISTS` (mismo patrón que `fk_bloqueo_profesional`), con `ON DELETE RESTRICT` implícito (no hay borrado físico en ninguno de los dos catálogos).

- *Alternativa descartada*: hacer públicas las entidades de C-04/C-06 → edita archivos ajenos (RM-01) y acopla módulos.
- *Alternativa descartada*: una entidad "sombra" de solo lectura mapeada a `profesional` dentro de este paquete → dos entidades para la misma tabla confunden a Hibernate `ddl-auto` y a los lectores.
- *Alternativa descartada*: `@ManyToMany` con `@JoinTable` → requiere tocar `Profesional`.

### D2 — Índice sobre `prestacion_id`
La PK `(profesional_id, prestacion_id)` cubre las búsquedas por profesional y por pareja (`realiza`). PostgreSQL no indexa FKs, así que se agrega `ix_profesional_prestacion_prestacion (prestacion_id)` vía `@Table(indexes = ...)` para las consultas inversas previstas (lista de espera RN-22, C-38) y para que la verificación de la FK al actualizar `prestacion` no recorra la tabla.

### D3 — `PUT` como reemplazo completo del conjunto, aplicado por diferencia
El cuerpo es `{"prestacionIds": [Long...]}` (`@NotNull List<@NotNull Long>`). El servicio deduplica en un `Set`, carga las asociaciones actuales, calcula altas y bajas y solo borra o inserta la diferencia, dentro de una transacción. Así un `PUT` idempotente no toca filas, y los errores de validación (D4) se detectan antes de escribir.

- *Alternativa descartada*: `POST`/`DELETE` por prestación → más endpoints y más estados intermedios; la ficha pide `PUT`, y C-21 editará el conjunto entero con checkboxes.
- *Alternativa descartada*: borrar todo y reinsertar → más escrituras y mayor ventana de conflicto concurrente.

### D4 — Validación de prestaciones mediante `ConsultaPrestaciones`
Por cada id del conjunto se llama a `ConsultaPrestaciones.buscarPorId`:
- vacío → `ReglaNegocioException("PRESTACION_INEXISTENTE", NO_PROCESABLE_422, ...)`. Se elige 422 y no 404 porque el recurso de la ruta (el profesional) sí existe; lo inválido es el contenido del cuerpo.
- inactiva y no asociada hoy → `PRESTACION_INACTIVA` (422). Inactiva y ya asociada → se conserva (RN-37, sin perder historial).

Los códigos usan guion bajo, como el resto del paquete `profesionales` (C-06, C-12). Las llamadas son N consultas por id; con catálogos de decenas de prestaciones el costo es despreciable y evita editar el puerto de C-04 para agregar una búsqueda por lote.

### D5 — Seguridad: `@PreAuthorize` por método y titularidad reutilizada
`ServicioProfesionalPrestaciones`:
- `reemplazar(...)`: `@PreAuthorize("hasRole('ADMIN')")`, `@Transactional`. Verifica el profesional con `ProfesionalConsulta.buscarPorId` → 404 `PROFESIONAL_NO_ENCONTRADO` (igual que C-12). Se admite un profesional inactivo: RN-11 se valida al crear el turno, y el administrador puede preparar la ficha antes de reactivarlo.
- `listar(id)`: `@PreAuthorize("hasAnyRole('ADMIN','RECEPCION','ODONTOLOGO')")`, `@Transactional(readOnly = true)`. Llama primero a `ServicioProfesionales.detalle(id)`, que ya aplica la titularidad del odontólogo y el 404, y no se duplica esa lógica.

No se modifica `SecurityConfig`: `/api/**` ya exige sesión y CSRF.

### D6 — Puerto `ProfesionalRealizaPrestacion`
Interfaz en `profesionales.prestacion.application` con un único método `boolean realiza(Long profesionalId, Long prestacionId)`, implementada por `ProfesionalPrestacionesPersistencia` (`@Component` público en `infrastructure`, mismo rol que `ProfesionalesPersistencia` y `BloqueosPersistencia`) con `existsById` sobre la PK. No lleva `@PreAuthorize` (lo usa la reserva pública) ni mira la actividad: el puerto responde una sola pregunta, y la actividad de profesional y prestación la valida C-19 con `ProfesionalConsulta` y `ConsultaPrestaciones`, como pide C-04 ("quien consume decide").

### D7 — Respuesta y orden
La consulta y el `PUT` devuelven `List<PrestacionResumen>` (el record público de C-04), ordenada en memoria por `nombre` sin distinguir mayúsculas (`String.CASE_INSENSITIVE_ORDER`) y luego por `id`. Reutilizar el record evita un DTO duplicado y no expone entidades (regla dura 9).

### D8 — Estructura de paquetes (RM-01)
```
profesionales/prestacion/
├── application/  ProfesionalRealizaPrestacion, ServicioProfesionalPrestaciones
├── infrastructure/ ProfesionalPrestacion, ProfesionalPrestacionId,
│                   ProfesionalPrestacionJpaRepository (package-private),
│                   ProfesionalPrestacionesPersistencia
└── web/          ProfesionalPrestacionesController, AsignacionPrestaciones (DTO)
```
El controlador mapea `/api/profesionales/{id}/prestaciones`, una ruta que no colisiona con `ProfesionalesController` (`/api/profesionales/{id}`) ni con `/bloqueos` u `/horarios`.

### Estrategia de tests (TDD estricto)
- **Persistencia** (`@SpringBootTest` + Testcontainers vía `PruebaIntegracionBase`): unicidad de la pareja y FKs con `JdbcTemplate`, y el puerto `realiza` en sus cuatro escenarios.
- **API** (`MockMvc` + `user(...).roles(...)` + `csrf()`): consulta, reemplazo, validaciones 400/404/422 y matriz de roles; el vínculo del odontólogo se simula con un `@TestConfiguration` de `ProfesionalVinculadoConsulta`, como `BloqueosApiIT`.
- Cada test lleva `@DisplayName` con el nombre exacto del escenario de la spec.

## Risks / Trade-offs

- [Desvío de la ficha: no hay `@ManyToOne`] → Se documenta en D1. La integridad queda igual de garantizada (PK + FK), y la ficha de CHANGES.md puede actualizarse al archivar si el equipo lo pide.
- [Dos `PUT` concurrentes sobre el mismo profesional pueden chocar en la PK al insertar la misma pareja] → La transacción falla con `DataIntegrityViolationException`; se traduce a `409` con código `PROFESIONAL_PRESTACIONES_CONFLICTO` para que el cliente reintente. Es poco probable (un solo ADMIN).
- [`schema-constraints.sql` es compartido con C-11/C-12] → Solo se agregan bloques nuevos al final, sin tocar los existentes; el conflicto de merge sería trivial.
- [N consultas a `ConsultaPrestaciones` por request] → Aceptable por tamaño del catálogo; si C-38 lo necesita, se agrega una consulta por lote en ese change.

## Migration Plan

Sin migración de datos: la tabla es nueva y en dev/test Hibernate la crea (`update` / `create-drop`). El script idempotente agrega FKs e índice sin fallar si ya existen. Rollback: revertir el PR; en dev se puede borrar la tabla `profesional_prestacion`.

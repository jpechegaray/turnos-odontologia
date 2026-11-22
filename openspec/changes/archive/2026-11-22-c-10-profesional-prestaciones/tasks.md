# Tasks

> TDD estricto: en cada tarea, primero el test en rojo (con `@DisplayName` igual al nombre exacto del escenario de `specs/profesional-prestaciones/spec.md`), después el mínimo código para pasarlo, triangulación y refactor. Todo el código nuevo va en `ar.edu.utn.turnos.profesionales.prestacion`; no se editan `Profesional.java`, `Prestacion.java` ni otros archivos de C-04, C-06, C-11 o C-12 (RM-01). Solo datos ficticios.

## 1. Red de seguridad

- [x] 1.1 Correr `./mvnw -q verify` en `backend/` con Docker encendido y registrar la línea de base (tests en verde, incluidos `ProfesionalesApiIT`, `CatalogoPrestacionesIT` y `BloqueosApiIT`). Verificación: build en verde; si algo falla, se reporta como falla preexistente y no se corrige en este change.

## 2. Persistencia e integridad de la asociación

- [x] 2.1 Escribir `ProfesionalPrestacionJpaIT` (`infrastructure`, extiende `PruebaIntegracionBase`) con los escenarios "La base rechaza una pareja profesional-prestación duplicada" y "La base rechaza una asociación con un profesional o una prestación inexistentes" (inserciones con `JdbcTemplate`, que esperan `DataIntegrityViolationException`). Verificación: el test compila y falla en rojo porque la tabla no existe.
- [x] 2.2 Crear `ProfesionalPrestacionId` (`@Embeddable`, `profesionalId`/`prestacionId`, `equals`/`hashCode`), la entidad package-private `ProfesionalPrestacion` (`@Table(name = "profesional_prestacion", indexes = ix_profesional_prestacion_prestacion)`) y `ProfesionalPrestacionJpaRepository` package-private (D1, D2). Verificación: pasa el escenario de duplicado.
- [x] 2.3 Agregar al final de `backend/src/main/resources/schema-constraints.sql` dos bloques `DO $$ ... IF NOT EXISTS` idempotentes para `fk_profesional_prestacion_profesional` y `fk_profesional_prestacion_prestacion`, cada uno terminado en `^^^`, sin tocar los bloques existentes. Verificación: pasa el escenario de FK y el contexto levanta dos veces sin error (idempotencia, se prueba reiniciando la app en dev o ejecutando el script dos veces en el IT).

## 3. Puerto `ProfesionalRealizaPrestacion`

- [x] 3.1 Escribir `ProfesionalRealizaPrestacionIT` (`infrastructure`) con los escenarios "La consulta interna confirma un profesional que realiza la prestación", "La consulta interna niega un profesional que no realiza la prestación", "La consulta interna niega ids inexistentes sin error" y "La consulta interna no depende de que la prestación esté activa", invocando el puerto sin usuario autenticado. Verificación: rojo por falta del puerto.
- [x] 3.2 Crear la interfaz `application/ProfesionalRealizaPrestacion` (`boolean realiza(Long, Long)`) y `infrastructure/ProfesionalPrestacionesPersistencia` (`@Component` público que la implementa con `existsById`; además expone `prestacionIdsDe(profesionalId)` y `reemplazar(profesionalId, altas, bajas)` para el servicio) (D6). Verificación: los 4 escenarios del puerto en verde.

## 4. Consulta `GET /api/profesionales/{id}/prestaciones`

- [x] 4.1 Escribir `ProfesionalPrestacionesApiIT` (`web`, con `@TestConfiguration` de `ProfesionalVinculadoConsulta` como `BloqueosApiIT`) con los escenarios de consulta: "Se listan las prestaciones del profesional ordenadas por nombre", "Un profesional sin prestaciones devuelve una lista vacía", "Una prestación asociada que se dio de baja se muestra inactiva" y "Consultar las prestaciones de un profesional inexistente responde 404". Verificación: rojo (404 de ruta inexistente).
- [x] 4.2 Crear `application/ServicioProfesionalPrestaciones.listar(id)` (`@PreAuthorize` de lectura para los tres roles, `readOnly`, reutiliza `ServicioProfesionales.detalle(id)`, resuelve cada id con `ConsultaPrestaciones` y ordena por nombre sin distinguir mayúsculas y luego por id) y `web/ProfesionalPrestacionesController` con `@GetMapping` que devuelve `List<PrestacionResumen>` (D5, D7). Verificación: los 4 escenarios de consulta en verde.

## 5. Reemplazo `PUT /api/profesionales/{id}/prestaciones`

- [x] 5.1 Agregar a `ProfesionalPrestacionesApiIT` los escenarios de reemplazo: "El administrador asocia prestaciones a un profesional", "El reemplazo desasocia las prestaciones que no se envían", "Una lista vacía deja al profesional sin prestaciones", "Los ids repetidos se asocian una sola vez", "Reemplazar las prestaciones de un profesional inexistente responde 404", "Las prestaciones de un profesional inactivo se pueden consultar y reemplazar" y "Un cuerpo inválido responde 400 sin cambios" (todos con `csrf()`). Verificación: rojo.
- [x] 5.2 Crear el DTO `web/AsignacionPrestaciones(@NotNull List<@NotNull Long> prestacionIds)`, el `@PutMapping` con `@Valid` y `ServicioProfesionalPrestaciones.reemplazar(id, ids)` (`@PreAuthorize("hasRole('ADMIN')")`, `@Transactional`, verificación del profesional con `ProfesionalConsulta` → 404 `PROFESIONAL_NO_ENCONTRADO`, deduplicación y aplicación por diferencia) (D3). Verificación: los 7 escenarios de reemplazo en verde.
- [x] 5.3 Agregar los escenarios de validación "Asociar una prestación inactiva responde 422", "Se puede conservar una prestación ya asociada que se dio de baja" y "Asociar una prestación inexistente responde 422"; implementar `PRESTACION_INEXISTENTE` y `PRESTACION_INACTIVA` (422) con `ReglaNegocioException` antes de escribir, y traducir una `DataIntegrityViolationException` de la PK a `409 PROFESIONAL_PRESTACIONES_CONFLICTO` (D4, riesgo de concurrencia). Verificación: los 3 escenarios en verde y sin cambios persistidos en los rechazos (se comprueba con una consulta posterior).

## 6. Permisos

- [x] 6.1 Agregar a `ProfesionalPrestacionesApiIT` los escenarios "Recepción consulta las prestaciones de cualquier profesional", "Un odontólogo vinculado consulta solo sus propias prestaciones", "Un odontólogo sin vínculo no puede consultar prestaciones de profesionales", "Recepción y odontología no pueden reemplazar las prestaciones" (parametrizado por rol, con `csrf()` y comprobación de que no hubo cambios) y "Sin sesión las prestaciones de un profesional responden 401" (GET y PUT). Verificación: todos en verde; si alguno pasa sin código nuevo, se confirma que lo cubre la configuración existente (`@PreAuthorize`, `SecurityConfig`) y se deja como test de regresión.

## 7. Verificación integral

- [x] 7.1 Correr `./mvnw -q verify` completo: todos los tests nuevos y la línea de base de 1.1 en verde y cobertura JaCoCo del subpaquete `profesionales.prestacion` ≥ 80 %. Verificación: salida del build y reporte JaCoCo.
- [x] 7.2 Revisar el diff contra `master`: `git diff --stat master` muestra cambios solo en `profesionales/prestacion/**` (main y test), en `schema-constraints.sql` y en `openspec/changes/c-10-profesional-prestaciones/**`; sin datos reales ni secretos. Verificación: salida de `git diff --stat` revisada.

## Workflow follow-up

- Correr `openspec-verify` para generar `verify.md` con el veredicto.
- Preguntar antes de commitear (regla dura 16); abrir el PR desde `lionel` hacia `master` para que lo revise Juan Pablo.
- Tras el merge, `/opsx:archive` para sincronizar `openspec/specs/profesional-prestaciones/spec.md` y marcar C-10 en CHANGES.md.

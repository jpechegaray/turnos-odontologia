# Verify — c-30-guardas-integridad-con-turnos (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

- `cmd /c "openspec status --change c-30-guardas-integridad-con-turnos --json"`: `isPlanningComplete: true`, `isComplete: true`, artefactos `proposal`, `specs`, `design` y `tasks` en estado `done` (5 delta specs: `agenda-turnos`, `bloqueos-de-agenda`, `catalogo-prestaciones`, `catalogo-profesionales`, `catalogo-sillones`).
- `cmd /c "openspec validate c-30-guardas-integridad-con-turnos --strict"`: `Change 'c-30-guardas-integridad-con-turnos' is valid` (código de salida `0`).

## Tareas

**8/8 completas (`[x]`); 0 pendientes.**

- `[x]` **1.1** Crear `TurnosActivosConsulta` con `TurnoEnConflicto(Long id, Instant inicio, Instant fin, String estado)` y los 4 métodos en archivo nuevo.
- `[x]` **1.2** Implementar `TurnosActivosPersistencia` con `EntityManager` y `Clock` inyectable y verificar con `TurnosActivosPersistenciaIT`.
- `[x]` **2.1** Inyectar `TurnosActivosConsulta` en `ServicioBloqueos` y rechazar en `crear` con `409 RN-17` y `Map.of("turnos", conflictos)`.
- `[x]` **2.2** Agregar tests de integración en `BloqueosApiIT` para los 3 escenarios de `bloqueos-de-agenda`.
- `[x]` **3.1** Inyectar `TurnosActivosConsulta` en `ServicioSillones` y validar en `desactivar` con `409 RN-37`, `recurso = "SILLON"`, `turnos`.
- `[x]` **3.2** Inyectar `TurnosActivosConsulta` en `ServicioProfesionales` y validar en `actualizar` (`activo: true -> false`) con `409 RN-37`, `recurso = "PROFESIONAL"`, `turnos`.
- `[x]` **3.3** Inyectar `TurnosActivosConsulta` en `CatalogoPrestaciones` y validar en `darDeBaja` y `modificar` (`activa: true -> false`) con `409 RN-37`, `recurso = "PRESTACION"`, `turnos`.
- `[x]` **4.1** Ejecutar la suite completa del backend (`mvn verify`) en verde sin regresiones.

## Matriz escenario → test

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| `agenda-turnos` | La consulta por intervalo incluye solo turnos programados o confirmados que se solapan | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/agenda/infrastructure/TurnosActivosPersistenciaIT.java:58` | Verifica inclusión de `PROGRAMADO` y `CONFIRMADO` solapados, orden `inicio ASC, id ASC`, exclusión de contiguos (`RN-02`), estados finales (`ATENDIDO`, `AUSENTE`, `CANCELADO`) y otro profesional. |
| `agenda-turnos` | La consulta de turnos futuros activos filtra por fin posterior al reloj actual | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/agenda/infrastructure/TurnosActivosPersistenciaIT.java:92` | Verifica filtro `fin > ahora` con `RelojFijoConfig` (excluye `fin < ahora`, borde exacto `fin == ahora` y estados finales futuros; incluye en curso y futuros por profesional, sillón y prestación ordenados por `inicio ASC, id ASC`). |
| `bloqueos-de-agenda` | Crear un bloqueo superpuesto con turnos activos devuelve 409 con la lista de conflictos | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java:231` | Parametrizado para `ADMIN` y `RECEPCION`; afirma `409`, `application/problem+json`, `codigo = "RN-17"`, `turnos` ordenados por `inicio ASC, id ASC`, sin `pacienteId`/`paciente` (`RN-35`), y `cantidadDeBloqueos == 0`. Además cubierto en unitario en `ServicioBloqueosTest.java:53`. |
| `bloqueos-de-agenda` | Crear un bloqueo contiguo a turnos activos se acepta | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java:258` | Parametrizado para `ADMIN` y `RECEPCION`; afirma `201 Created` con turnos activos en `[09:30, 10:00)` y `[11:00, 11:30)` para bloqueo `[10:00, 11:00)` (`RN-02`). |
| `bloqueos-de-agenda` | Cancelar el turno activo permite crear el bloqueo antes rechazado | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java:273` | Parametrizado para `ADMIN` y `RECEPCION`; verifica `409 RN-17`, cancela vía `POST /api/turnos/{id}/cancelar` (`200`) y reintenta alta del bloqueo recibiendo `201`. |
| `catalogo-sillones` | Desactivar un sillón conserva su registro | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/sillon/web/SillonesApiIT.java:182` | Verifica `200`, conservación de ID en detalle y listado con `activo=false`. |
| `catalogo-sillones` | Reactivar un sillón conserva su registro | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/sillon/web/SillonesApiIT.java:188` | Verifica `200`, conservación de ID en detalle y listado con `activo=true`. |
| `catalogo-sillones` | Desactivar un sillón con turnos futuros activos devuelve 409 con la lista de conflictos | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/sillon/web/SillonesApiIT.java:195` | Afirma `409`, `application/problem+json`, `codigo = "RN-37"`, `recurso = "SILLON"`, `turnos` ordenados sin `pacienteId` y que el sillón permanece `activo = true` en BD. También cubierto en unitario en `ServicioSillonesTest.java:60`. |
| `catalogo-sillones` | Cancelar los turnos futuros permite desactivar el sillón | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/sillon/web/SillonesApiIT.java:231` | Verifica rechazo `409 RN-37`, cancela el turno vía `POST /api/turnos/{id}/cancelar` y desactiva con `PUT /api/sillones/{id}` recibiendo `200` y `activo = false`. |
| `catalogo-profesionales` | El administrador desactiva y reactiva el mismo registro | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/web/ProfesionalesApiIT.java:134` | Verifica transición a `activo=false` y luego `activo=true` con `200`, conservando ID en detalle y en BD. |
| `catalogo-profesionales` | El catálogo no permite borrar físicamente un profesional | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/web/ProfesionalesApiIT.java:151` | Verifica `405 Method Not Allowed` ante `DELETE /api/profesionales/{id}` y que la fila permanece intacta. |
| `catalogo-profesionales` | Desactivar un profesional con turnos futuros activos devuelve 409 con la lista de conflictos | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/web/ProfesionalesApiIT.java:162` | Afirma `409`, `application/problem+json`, `codigo = "RN-37"`, `recurso = "PROFESIONAL"`, `turnos` ordenados sin `pacienteId` y que el profesional sigue con `activo = true`. También cubierto en unitario en `ServicioProfesionalesTest.java:45`. |
| `catalogo-profesionales` | Cancelar los turnos futuros permite desactivar al profesional | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/web/ProfesionalesApiIT.java:198` | Verifica rechazo `409 RN-37`, cancela el turno vía `POST /api/turnos/{id}/cancelar` y desactiva con `PUT /api/profesionales/{id}` recibiendo `200` y `activo = false`. |
| `catalogo-prestaciones` | Dar de baja o desactivar una prestación con turnos futuros activos devuelve 409 con la lista de conflictos | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/prestaciones/web/CatalogoPrestacionesIT.java:208` | Verifica tanto `DELETE /api/prestaciones/{id}` como `PUT /api/prestaciones/{id}` con `activa=false`; en ambos afirma `409`, `codigo = "RN-37"`, `recurso = "PRESTACION"`, `turnos` ordenados sin `pacienteId` y que la prestación sigue `activa = true`. También cubierto en unitario en `CatalogoPrestacionesTest.java:50` y `69`. |
| `catalogo-prestaciones` | Cancelar los turnos futuros permite dar de baja la prestación | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/prestaciones/web/CatalogoPrestacionesIT.java:250` | Verifica rechazo `409 RN-37`, cancela el turno vía `POST /api/turnos/{id}/cancelar` y ejecuta `DELETE /api/prestaciones/{id}` recibiendo `204` y `activa = false`. |

**Resumen de cobertura de escenarios:** 15/15 escenarios **Cubiertos** (0 Parciales, 0 Sin test).

## Ejecución de tests

- **Comando ejecutado:** `.\mvnw.cmd -q verify` en `backend/` (sobre `master` sincronizado en `9d502c9`).
- **Resultado:** `0` código de salida (verde completo).
  - **Surefire (tests unitarios, `@WebMvcTest` y arquitectura):** 50 clases, **342 tests ejecutados**, 0 fallidos, 0 errores, 0 saltados.
    - Incluye `ArquitecturaTest` (2/2), `ServicioBloqueosTest` (3/3), `ServicioSillonesTest` (5/5), `ServicioProfesionalesTest` (4/4), `CatalogoPrestacionesTest` (4/4), `SillonesControllerTest` (6/6).
  - **Failsafe (tests de integración `*IT` con Testcontainers PostgreSQL 16):** 82 clases, **671 tests ejecutados**, 0 fallidos, 0 errores, 0 saltados.
    - Incluye `TurnosActivosPersistenciaIT` (2/2), `BloqueosApiIT` (24/24), `SillonesApiIT` (36/36), `ProfesionalesApiIT` (16/16), `CatalogoPrestacionesIT` (24/24).
  - **Total suite backend:** **1013 tests ejecutados, 1013 pasados, 0 fallidos, 0 errores, 0 saltados.**

## Reglas de negocio y reglas duras

- **RN-02 (Borde no solapado `[inicio, fin)`):** Cumplido. La consulta JPQL usa `t.inicio < :fin and t.fin > :inicio` (`TurnosActivosPersistencia.java:42-43`), y se prueba explícitamente con turnos contiguos tanto en `TurnosActivosPersistenciaIT` como en `BloqueosApiIT` (`Crear un bloqueo contiguo a turnos activos se acepta`).
- **RN-14 (Zona horaria y persistencia en UTC con `timestamptz`):** Cumplido. Las marcas temporales de `TurnoEnConflicto` son `java.time.Instant` serializadas en ISO-8601 UTC (`...Z`).
- **RN-17 (Bloqueos y turnos existentes):** Cumplido. `ServicioBloqueos.crear` rechaza con `409 Conflict` (`codigo = "RN-17"`, `propiedades = Map.of("turnos", conflictos)`) sin persistir el bloqueo; tras cancelar el turno activo, el bloqueo se crea con `201`.
- **RN-35 (Protección de datos personales del paciente):** Cumplido. `TurnoEnConflicto` proyecta exclusivamente `(id, inicio, fin, estado)` sin exponer `pacienteId` ni datos del paciente en las respuestas `409`, verificado con `jsonPath("$.turnos[0].pacienteId").doesNotExist()` en los tests de integración.
- **RN-37 (Bajas lógicas de catálogos con turnos futuros activos):** Cumplido en sillones (`recurso = "SILLON"`), profesionales (`recurso = "PROFESIONAL"`) y prestaciones (`recurso = "PRESTACION"`, tanto en `DELETE` como en `PUT` con transición `activa: true -> false`), permitiendo editar o repetir `DELETE` sobre recursos que ya estaban inactivos sin bloqueo falso.
- **Reglas duras del proyecto y mitigación RM-04:**
  - **RM-04 (cero ediciones en archivos existentes de `agenda`):** Cumplido. En `agenda` solo se agregaron los archivos nuevos `TurnosActivosConsulta.java`, `TurnosActivosPersistencia.java` y `TurnosActivosPersistenciaIT.java`; `git status` y `git diff` confirman que no se modificó ningún archivo previo de `agenda`.
  - **Sin datos reales de pacientes ni secretos:** Cumplido (`git diff` revisado; solo nombres ficticios como `"Ana Ficticia"`).
  - **Reloj (`Clock` inyectable):** Cumplido. `TurnosActivosPersistencia` inyecta `java.time.Clock` y calcula `Instant.now(reloj)` (`TurnosActivosPersistencia.java:28-30, 70`).
  - **PostgreSQL real con Testcontainers (sin H2):** Cumplido. Todos los `*IT` extienden `PruebaIntegracionBase` sobre `postgres:16-alpine`.
  - **`@DisplayName` exactos:** Cumplido en los 15 escenarios de las 5 delta specs.

## Diferencias con design.md

- **Bloqueantes:** Ninguna.
- **Mayores:** Ninguna.
- **Menores:**
  - En `design.md` §1 y en `tasks.md` 1.1 se menciona el paquete de `CatalogoPrestaciones` por su nombre conceptual; en el código real del proyecto reside en `ar.edu.utn.turnos.prestaciones.infrastructure.CatalogoPrestaciones` (creado así en `C-04`). La implementación respetó la ubicación real del archivo sin romper fronteras de módulo (`ArquitecturaTest` en verde).

## Qué NO se pudo verificar

- Nada pendiente: todas las validaciones estructurales, lectura de código, cruce de reglas y ejecuciones de tests unitarios y de integración con Testcontainers (Docker activo) se ejecutaron de punta a punta.

## Próximo paso

El change **`c-30-guardas-integridad-con-turnos`** está listo para abrir PR / archivar con `/opsx:archive c-30-guardas-integridad-con-turnos`.

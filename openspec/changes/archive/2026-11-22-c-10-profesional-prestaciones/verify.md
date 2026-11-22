# Verify — c-10-profesional-prestaciones (2026-11-22)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

- `openspec.cmd status --change "c-10-profesional-prestaciones" --json`: completo (`isPlanningComplete: true`, `isComplete: true`), con los cuatro artefactos (`proposal.md`, `specs/profesional-prestaciones/spec.md`, `design.md`, `tasks.md`) en estado `done`.
- `openspec.cmd validate "c-10-profesional-prestaciones" --strict`: `Change 'c-10-profesional-prestaciones' is valid` (código de salida `0`, 0 bloqueantes).

## Tareas

14/14 tareas completadas (`[x]`) en `openspec/changes/c-10-profesional-prestaciones/tasks.md` (1.1, 2.1, 2.2, 2.3, 3.1, 3.2, 4.1, 4.2, 5.1, 5.2, 5.3, 6.1, 7.1, 7.2). Pendientes: 0.

## Matriz escenario → test

La spec delta `openspec/changes/c-10-profesional-prestaciones/specs/profesional-prestaciones/spec.md` define **13 Requirements** y **25 escenarios** (`#### Scenario:`), todos cubiertos con `@DisplayName` idéntico al nombre del escenario (más un test adicional de idempotencia del script SQL en `ProfesionalPrestacionJpaIT.java:53`, totalizando 26 métodos de test / 27 ejecuciones por el `@ParameterizedTest` de dos roles).

| Requirement | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| Consulta de las prestaciones de un profesional | Se listan las prestaciones del profesional ordenadas por nombre | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:55` | Verifica `200`, orden alfabético ("Consulta", "Limpieza"), campos `id`, `nombre`, `duracionDefaultMin`, `activa` y exclusión de "Extracción" de otro profesional. |
| Consulta de las prestaciones de un profesional | Un profesional sin prestaciones devuelve una lista vacía | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:78` | Verifica `200` y cuerpo JSON `[]`. |
| Consulta de las prestaciones de un profesional | Una prestación asociada que se dio de baja se muestra inactiva | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:86` | Desactiva la prestación tras asociarla y verifica `200` con `"Blanqueamiento"` y `activa: false`. |
| Profesional de la ruta inexistente o inactivo | Consultar las prestaciones de un profesional inexistente responde 404 | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:101` | Verifica `404`, `application/problem+json` y `$.codigo == "PROFESIONAL_NO_ENCONTRADO"`. |
| Profesional de la ruta inexistente o inactivo | Reemplazar las prestaciones de un profesional inexistente responde 404 | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:182` | Verifica `404`, `application/problem+json` y `$.codigo == "PROFESIONAL_NO_ENCONTRADO"` en `PUT`. |
| Profesional de la ruta inexistente o inactivo | Las prestaciones de un profesional inactivo se pueden consultar y reemplazar | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:192` | Verifica `200` y contenido `"Consulta"` tanto en `PUT` como en `GET` posterior sobre un profesional con `activo = false`. |
| Reemplazo del conjunto de prestaciones de un profesional | El administrador asocia prestaciones a un profesional | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:110` | Verifica `200` con `"Consulta"` y `"Limpieza"` ordenadas en la respuesta del `PUT` y en un `GET` posterior. |
| Reemplazo del conjunto de prestaciones de un profesional | El reemplazo desasocia las prestaciones que no se envían | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:132` | Verifica `200` con `"Extracción"` y `"Limpieza"`, y comprueba por `ProfesionalRealizaPrestacion` que `"Consulta"` quedó desasociada (`false`) y las otras dos asociadas (`true`). |
| Reemplazo del conjunto de prestaciones de un profesional | Una lista vacía deja al profesional sin prestaciones | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:155` | Verifica `200` con `[]` y que `puertoRealiza.realiza(profesional, consulta)` devuelve `false`. |
| Pareja profesional-prestación única en el reemplazo | Los ids repetidos se asocian una sola vez | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:169` | Envía el mismo id dos veces y verifica `200` con `$.length() == 1` y `"Consulta"`. |
| Cuerpo del reemplazo válido | Un cuerpo inválido responde 400 sin cambios | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:212` | Prueba `{}`, `{"prestacionIds":null}` y `{"prestacionIds":[null]}`, verifica `400` Problem Details en los tres y que la asociación previa con `"Consulta"` permanece intacta. |
| Solo se asocian prestaciones existentes | Asociar una prestación inexistente responde 422 | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:227` | Verifica `422` Problem Details con `$.codigo == "PRESTACION_INEXISTENTE"` y que `"Consulta"` sigue siendo la única prestación asociada. |
| Prestaciones inactivas en el reemplazo | Asociar una prestación inactiva responde 422 | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:242` | Verifica `422` Problem Details con `$.codigo == "PRESTACION_INACTIVA"` al intentar asociar `"Blanqueamiento"` inactiva y que `"Consulta"` se conserva sin cambios. |
| Prestaciones inactivas en el reemplazo | Se puede conservar una prestación ya asociada que se dio de baja | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:258` | Verifica `200` al enviar `"Consulta"` (activa) y `"Blanqueamiento"` (dada de baja tras asociarse), devolviendo ambas con `activa: false` para `"Blanqueamiento"`. |
| Lectura de prestaciones de un profesional según el rol | Recepción consulta las prestaciones de cualquier profesional | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:279` | Verifica `200` con `"Consulta"` para usuario con rol `RECEPCION`. |
| Lectura de prestaciones de un profesional según el rol | Un odontólogo vinculado consulta solo sus propias prestaciones | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:292` | Verifica `200` con `"Consulta"` para el propio id, y `403` Problem Details sin exponer datos para `otroProfesional` y `-1L`. |
| Lectura de prestaciones de un profesional según el rol | Un odontólogo sin vínculo no puede consultar prestaciones de profesionales | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:314` | Verifica `403` Problem Details sin exponer datos para un usuario `ODONTOLOGO` sin vínculo. |
| Solo el administrador reemplaza prestaciones | Recepción y odontología no pueden reemplazar las prestaciones | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:327` | `@ParameterizedTest` con `RECEPCION` y `ODONTOLOGO` y CSRF válido; verifica `403` Problem Details y que `"Consulta"` sigue siendo la única asociada. |
| Sesión obligatoria | Sin sesión las prestaciones de un profesional responden 401 | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/web/ProfesionalPrestacionesApiIT.java:342` | Verifica `401` Problem Details tanto en `GET` como en `PUT` (con CSRF) sin sesión y sin cambios persistidos. |
| Consulta interna de si un profesional realiza una prestación | La consulta interna confirma un profesional que realiza la prestación | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalRealizaPrestacionIT.java:39` | Invoca `consulta.realiza(profesional, consultaId)` sin sesión y verifica `true`. |
| Consulta interna de si un profesional realiza una prestación | La consulta interna niega un profesional que no realiza la prestación | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalRealizaPrestacionIT.java:45` | Invoca `consulta.realiza(profesional, limpiezaId)` (asociada a otro profesional) y verifica `false`. |
| Consulta interna de si un profesional realiza una prestación | La consulta interna niega ids inexistentes sin error | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalRealizaPrestacionIT.java:51` | Verifica `false` sin excepción para `(-1L, consultaId)`, `(profesional, -1L)` y `(-1L, -1L)`. |
| La consulta interna no evalúa la actividad | La consulta interna no depende de que la prestación esté activa | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalRealizaPrestacionIT.java:59` | Da de baja la prestación asociada en base y verifica que `consulta.realiza(profesional, blanqueamiento)` sigue devolviendo `true`. |
| Integridad de la asociación en la base de datos | La base rechaza una pareja profesional-prestación duplicada | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalPrestacionJpaIT.java:27` | Inserta dos veces la misma pareja con `JdbcTemplate` en PostgreSQL real y verifica `DataIntegrityViolationException`. |
| Integridad de la asociación en la base de datos | La base rechaza una asociación con un profesional o una prestación inexistentes | Cubierto | `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/infrastructure/ProfesionalPrestacionJpaIT.java:37` | Verifica `DataIntegrityViolationException` con mensaje de `fk_profesional_prestacion_profesional` y de `fk_profesional_prestacion_prestacion` ante ids `-1L`. |

## Ejecución de tests

- **Comando ejecutado:** `.\mvnw.cmd -B verify` en `backend/` (con Docker activo y contenedor `postgres:16-alpine` vía Testcontainers).
- **Resultado global:** `BUILD SUCCESS` en `01:20 min`.
  - **Surefire (unitarios y slice tests + `ArquitecturaTest`):** 95 ejecutados, 95 pasados, 0 fallidos, 0 errores, 0 saltados.
  - **Failsafe (integración con PostgreSQL 16 real en Testcontainers):** 213 ejecutados, 213 pasados, 0 fallidos, 0 errores, 0 saltados.
  - **Total backend:** **308 tests ejecutados, 308 pasados, 0 fallidos, 0 errores, 0 saltados.**
- **Desglose de C-10:**
  - `ProfesionalPrestacionJpaIT`: 3 ejecutados, 3 pasados, 0 fallidos.
  - `ProfesionalRealizaPrestacionIT`: 4 ejecutados, 4 pasados, 0 fallidos.
  - `ProfesionalPrestacionesApiIT`: 20 ejecutados, 20 pasados, 0 fallidos.
- **Cobertura JaCoCo (`backend/target/site/jacoco/jacoco.csv` para `ar.edu.utn.turnos.profesionales.prestacion.**`):**
  - `ProfesionalPrestacionesController`: 18/18 instrucciones (100 %), 5/5 líneas (100 %).
  - `AsignacionPrestaciones`: 6/6 instrucciones (100 %), 1/1 líneas (100 %).
  - `ServicioProfesionalPrestaciones`: 172/180 instrucciones (95,56 %), 36/38 líneas (94,74 %), 12/12 ramas (100 %).
  - `ProfesionalPrestacionesPersistencia`: 56/56 instrucciones (100 %), 15/15 líneas (100 %), 4/4 ramas (100 %).
  - `ProfesionalPrestacion`: 17/17 instrucciones (100 %), 6/6 líneas (100 %).
  - `ProfesionalPrestacionId`: 15/59 instrucciones, 7/14 líneas (ramas de `equals`/`hashCode` generadas por contrato de `@Embeddable`).
  - **Total del subpaquete `profesionales.prestacion`:** 284/336 instrucciones (**84,52 %**) y 70/79 líneas (**88,61 %**), superando el umbral del 80 %.

## Reglas de negocio y reglas duras

- **RN-11 (Relación profesional-prestación obligatoria):** cubierta por la entidad `ProfesionalPrestacion`, la API de administración/consulta y el puerto `ProfesionalRealizaPrestacion.realiza(profesionalId, prestacionId)` que valida la asociación sin acoplar la evaluación de actividad.
- **RN-20 y RN-33 (Permisos por rol; odontólogo solo lectura; recepción no administra catálogos):** cubiertas por `@PreAuthorize("hasRole('ADMIN')")` en `reemplazar`, `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION', 'ODONTOLOGO')")` + `ServicioProfesionales.detalle(id)` en `listar`, y probadas en `ProfesionalPrestacionesApiIT` para `ADMIN`, `RECEPCION`, `ODONTOLOGO` (vinculado y sin vínculo) y sin sesión.
- **RN-37 (Bajas lógicas e historial):** cubierta al impedir asociar por primera vez una prestación inactiva (`422 PRESTACION_INACTIVA`) pero permitir conservar una prestación ya asociada que fue dada de baja posteriormente, y al mostrarla con `activa: false` en `GET`.
- **Reglas duras de `AGENTS.md` (1 a 17) y mitigación RM-01:**
  - **Regla 1 (Datos ficticios):** todos los tests usan nombres y matrículas ficticias (`'Ana'`, `'Ficticia'`, `MP-PRESTACIONES-API`, `admin-ficticio`, `odontologo-ficticio`).
  - **Regla 2 (Sin secretos):** no se tocaron archivos de configuración ni se agregaron credenciales.
  - **Regla 4 (`ODONTOLOGO` sin escritura):** `PUT /api/profesionales/{id}/prestaciones` rechaza `ODONTOLOGO` con `403`.
  - **Regla 7 (Hibernate `ddl-auto` + script SQL idempotente):** tabla e índice definidos en JPA (`ProfesionalPrestacion`) y FKs agregadas con bloques `DO $$ ... IF NOT EXISTS` terminados en `^^^` al final de `backend/src/main/resources/schema-constraints.sql`, con test de doble ejecución (`ProfesionalPrestacionJpaIT.java:53`).
  - **Regla 8 (Sesión de servidor con cookie + CSRF):** el endpoint se integra con `SecurityConfig` existente sin JWT.
  - **Regla 9 (No exponer entidades JPA + Bean Validation):** la entidad `ProfesionalPrestacion` y su repositorio son *package-private* en `infrastructure`; la API recibe `AsignacionPrestaciones` (`@NotNull List<@NotNull Long>`) con `@Valid` y devuelve `List<PrestacionResumen>`.
  - **Regla 10 (PostgreSQL real con Testcontainers):** los tres IT extienden `PruebaIntegracionBase` y corren contra `postgres:16-alpine`.
  - **Regla 11 (`@DisplayName` con el nombre exacto del escenario):** verificado en los 25 escenarios de la spec.
  - **Regla 17 (Español):** nombres de dominio, endpoints, códigos de error y tests en español.
  - **Mitigación RM-01:** `git status -sb` confirma que en C-10 solo se modificó `backend/src/main/resources/schema-constraints.sql` (agregando únicamente los dos bloques al final) y solo se crearon archivos bajo `backend/src/main/java/ar/edu/utn/turnos/profesionales/prestacion/**`, `backend/src/test/java/ar/edu/utn/turnos/profesionales/prestacion/**` y `openspec/changes/c-10-profesional-prestaciones/**`. No se editaron `Profesional.java`, `Prestacion.java` ni ningún otro archivo de C-04, C-06, C-11 o C-12.

## Diferencias con design.md

Ninguna diferencia bloqueante, mayor ni menor. Las decisiones **D1 a D8** se implementaron tal como fueron diseñadas:
- **D1:** `@EmbeddedId ProfesionalPrestacionId(profesionalId, prestacionId)` sin `@ManyToOne` y FKs `fk_profesional_prestacion_profesional` y `fk_profesional_prestacion_prestacion` en `schema-constraints.sql`.
- **D2:** índice `ix_profesional_prestacion_prestacion` sobre `prestacion_id` en `@Table(indexes = ...)`.
- **D3:** reemplazo por diferencia (`altas` y `bajas`) con deduplicación en `LinkedHashSet` dentro de `@Transactional`.
- **D4:** validación con `ConsultaPrestaciones.buscarPorId` → `422 PRESTACION_INEXISTENTE` o `422 PRESTACION_INACTIVA` (conservando inactivas ya asociadas) y traducción de `DataIntegrityViolationException` a `409 PROFESIONAL_PRESTACIONES_CONFLICTO`.
- **D5:** `@PreAuthorize` por método y reutilización de `ServicioProfesionales.detalle(id)` para lectura y titularidad del odontólogo, y `ProfesionalConsulta.buscarPorId` para `404 PROFESIONAL_NO_ENCONTRADO` en reemplazo (admitiendo profesionales inactivos).
- **D6:** interfaz pública `ProfesionalRealizaPrestacion` en `application` implementada por `ProfesionalPrestacionesPersistencia` en `infrastructure` con `existsById` sin `@PreAuthorize` ni filtro de actividad.
- **D7:** retorno `List<PrestacionResumen>` ordenado por `nombre` (`String.CASE_INSENSITIVE_ORDER`) y luego por `id`.
- **D8:** estructura de paquetes exacta `profesionales.prestacion.{application,infrastructure,web}` y `ArquitecturaTest` en verde.

## Qué NO se pudo verificar

- Nada pendiente dentro del alcance de C-10. La rama `catch (DataIntegrityViolationException)` de `ServicioProfesionalPrestaciones.reemplazar` (`409 PROFESIONAL_PRESTACIONES_CONFLICTO` ante dos `PUT` simultáneos que inserten la misma pareja al mismo tiempo) está implementada como defensa y la unicidad de la PK que la dispara está probada en `ProfesionalPrestacionJpaIT`.

## Próximo paso

El change `c-10-profesional-prestaciones` está **LISTO PARA ARCHIVAR**. Siguiente paso: consultar antes de hacer el commit (regla dura 16), abrir el PR hacia `master` para revisión de Juan Pablo y luego ejecutar `/opsx:archive c-10-profesional-prestaciones`.

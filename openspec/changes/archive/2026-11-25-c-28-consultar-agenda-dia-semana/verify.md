# Verify — c-28-consultar-agenda-dia-semana (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

No hay bloqueantes. Los 25 escenarios de las dos specs del change tienen un test con su nombre exacto, y todos terminaron en verde. En el PR conviene que Juan Pablo revise la adaptación de un test de C-03 (sección «Diferencias con design.md», ítem 1), tres diferencias menores y dos notas de cobertura.

## Validación estructural

`openspec validate c-28-consultar-agenda-dia-semana --strict` → `Change 'c-28-consultar-agenda-dia-semana' is valid`.

## Tareas

10/10 completas (1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.3, 4.1, 5.1 y 6.1). No hay pendientes.

## Matriz escenario → test

Rutas de test relativas a `backend/src/test/java/ar/edu/utn/turnos/`.

### Spec `consulta-agenda` (ADDED)

| Escenario | Estado | Test | Nota |
|---|---|---|---|
| La vista diaria devuelve turnos, horarios y bloqueos del día | Cubierto | `agenda/web/AgendaApiIT.java:113` | Parametrizado con `ADMIN` y `RECEPCION`. Afirma `vista`, `desde` y `hasta`, todos los campos del turno, el horario de la fecha con `-03:00` y el bloqueo. Consulta con `profesionalId` para aislarse de datos de otros IT; el filtro no cambia lo que se afirma. |
| La vista semanal va de lunes a domingo | Cubierto | `AgendaApiIT.java:146` | `desde` 07, `hasta` 13. Incluye el lunes 7 y el domingo 13, pero no el lunes 14. |
| Sin parámetros se consulta el día de hoy | Cubierto | `AgendaApiIT.java:162` | Con el reloj fijo en 2026-12-01: vista `dia`, el turno de hoy aparece y el de mañana no. |
| Parámetros inválidos responden 400 | Cubierto | `AgendaApiIT.java:178` | `vista=mes`, `fecha=07/12/2026` y `fecha=2026-13-01`, en Problem Details. |
| Un turno a las 23:30 hora local pertenece a ese día | Cubierto | `AgendaApiIT.java:188` | Aparece el 7 con `inicio` `2026-12-07T23:30:00-03:00` y no aparece el 8. También lo prueba `AgendaLecturaPersistenciaIT`. |
| Un turno a las 00:00 hora local pertenece al día que empieza | Cubierto | `AgendaApiIT.java:200` | Solo en el día 8. |
| Un bloqueo de varios días aparece en cada día que toca | Cubierto | `AgendaApiIT.java:211` | En el día 8 aparece con su `inicio` y su `fin` completos. |
| Los turnos de cualquier estado se muestran con su estado | Cubierto | `AgendaApiIT.java:224` | `CANCELADO` y `PROGRAMADO`, ordenados por inicio. |
| Un horario semanal se proyecta en cada fecha del período | Cubierto | `AgendaApiIT.java:237` | Fechas 07 y 09, con `inicio` y `fin` en `-03:00`. También lo prueba `ProyeccionHorariosTest`. |
| Filtrar por profesional muestra solo su agenda | Cubierto | `AgendaApiIT.java:252` | Turnos, horarios y bloqueos son solo del profesional pedido. |
| Filtrar por sillón muestra solo los turnos de ese sillón | Cubierto | `AgendaApiIT.java:268` | |
| Los filtros de profesional y sillón se combinan | Cubierto | `AgendaApiIT.java:278` | Solo el turno del profesional en ese sillón. |
| Un profesional sin datos devuelve listas vacías | Cubierto | `AgendaApiIT.java:290` | `200` con las tres listas vacías. |
| Recepción ve el paciente de todos los turnos | Cubierto | `AgendaApiIT.java:304` | `ADMIN` y `RECEPCION`: nombre, apellido y notas en turnos de los dos profesionales, y motivo del bloqueo del otro profesional. |
| El odontólogo ve el paciente solo en sus turnos | Cubierto | `AgendaApiIT.java:324` | Usuario ficticio vinculado, con sesión real (`IdentidadPersonal`). En lo propio trae paciente, notas y motivo; en lo ajeno, `paciente`, `notas` y `motivo` nulos con `pacienteId` presente. Sin filtros ve los horarios de los dos profesionales y los turnos de ambos. |
| Un odontólogo sin profesional vinculado recibe 403 | Cubierto | `AgendaApiIT.java:361` | Problem Details, sin `turnos`. |
| Sin sesión la agenda responde 401 | Cubierto | `AgendaApiIT.java:370` | Problem Details, sin `turnos`. |
| Toda escritura de un odontólogo responde 403 | Cubierto | `seguridad/web/EscriturasOdontologoIT.java:66` | Recorre los mappings de escritura registrados (no puede ser una lista vacía e incluye `POST /api/turnos`). Cada uno responde 403 con CSRF válido, y la cantidad de filas de 10 tablas de negocio no cambia. |

### Spec `gestion-pacientes` (MODIFIED: «Permisos por rol en la ficha de paciente»)

| Escenario | Estado | Test | Nota |
|---|---|---|---|
| Recepción crea, edita y busca fichas | Cubierto | `pacientes/web/PacienteGestionIT.java:422` | Sin cambios respecto de C-07. |
| Recepción no puede dar de baja una ficha | Cubierto | `PacienteGestionIT.java:456` | Sin cambios. |
| Administrador da de baja una ficha | Cubierto | `PacienteGestionIT.java:470` | Sin cambios. |
| El odontólogo no accede a las fichas | Cubierto | `PacienteGestionIT.java:483` y `pacientes/web/PacienteFichaOdontologoIT.java:74` y `:93` | El test de C-07 usa un odontólogo **sin vínculo**. El caso de la spec modificada, un odontólogo vinculado sin turnos con el paciente, lo cubren `PacienteFichaOdontologoIT` (consulta: 403; búsqueda: 403) y `EscriturasOdontologoIT` (crear: 403). |
| El odontólogo consulta la ficha de un paciente de sus turnos | Cubierto | `PacienteFichaOdontologoIT.java:64` | `200` con id, nombre y apellido. |
| El odontólogo no consulta la ficha de un paciente de otro profesional | Cubierto | `PacienteFichaOdontologoIT.java:74` | `403` en Problem Details, sin `nombre` ni `email`. |
| Una petición anónima recibe 401 | Cubierto | `PacienteGestionIT.java:510` | Sin cambios. |

Tests de apoyo, sin un escenario propio:
- `PeriodoAgendaTest` (6) y `ProyeccionHorariosTest` (3).
- `AgendaLecturaPersistenciaIT` (4).
- Métodos nuevos en `HorarioConsultaIT`, `BloqueoConsultaIT` y `PacienteConsultaIT`.
- `PacienteFichaOdontologoIT`: id inexistente da 403.

## Ejecución de tests

- **Comando:** `.\mvnw.cmd verify` en `backend/`, con Docker encendido (Testcontainers con PostgreSQL 16).
- **Primera corrida completa:** `BUILD FAILURE`. Fallaron 2 de 461 tests de integración, los dos en `SesionPersonalIT`:
  - `rechazaCsrfFaltanteOInvalido`: esperaba 200 y recibió 403.
  - `noRegistraSecretos`: falló como consecuencia del anterior.
  Se corrigieron como explica la sección siguiente.
- **Corrida final:** `BUILD SUCCESS`, EXIT=0.
  - Unitarios (surefire): 229 corridos, 0 fallidos, 0 con error, 0 saltados.
  - Integración (failsafe): 461 corridos, 0 fallidos, 0 con error, 0 saltados.
- **Clases del change o alcanzadas por él:**
  - Agenda: `AgendaApiIT` 21/21, `AgendaLecturaPersistenciaIT` 4/4, `PeriodoAgendaTest` 6/6 y `ProyeccionHorariosTest` 3/3.
  - Pacientes: `PacienteFichaOdontologoIT` 4/4, `PacienteGestionIT` 33/33, `PacienteConsultaIT` 6/6 y `PacienteServicioIT` 5/5.
  - Seguridad: `EscriturasOdontologoIT` 1/1 y `SesionPersonalIT` 8/8.
  - Horarios y bloqueos: `HorarioConsultaIT` 6/6 y `BloqueoConsultaIT` 4/4.
  - Turnos (C-19): `TurnosApiIT` 26/26, `TurnosConcurrenciaIT` 5/5 y `TurnosPersistenciaIT` 5/5.
  - `ArquitecturaTest` 2/2.
- **Vigencia:** después de la corrida final no se modificó ningún `.java`; solo cambiaron `design.md`, `tasks.md` y este informe.
- **Frontend:** no ejecutado, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

- **RN-14 (zona única):**
  - El período se calcula con `ZoneId` (`atStartOfDay(BUENOS_AIRES)`), sin desfase fijo.
  - Los bordes de 23:30 y 00:00 están probados en la API y en la persistencia.
  - Los instantes salen con el desfase de Buenos Aires.
- **RN-33 (permisos):**
  - Lectura global de la agenda, con datos según el rol.
  - Ficha solo de pacientes de sus turnos.
  - Toda escritura del odontólogo da 403, probado de forma transversal.
- **US-013:** se cumplen los cinco criterios:
  - Vistas y filtros: :113 a :290.
  - Horarios, bloqueos y estados: :113, :224 y :237.
  - Hora de Buenos Aires: :188 y :200.
  - Ficha de los pacientes de sus turnos: `PacienteFichaOdontologoIT`.
  - Solo lectura con 403: `EscriturasOdontologoIT`.
- **RN-02, comparación `[inicio, fin)`:** un turno contiguo al período queda afuera (`AgendaLecturaPersistenciaIT`) y un bloqueo contiguo no se lista (`BloqueoConsultaIT`). El change no crea turnos, así que no aplican la concurrencia ni `EXCLUDE`.
- **Reglas duras, revisadas sobre el diff sin commitear de la rama `valentino-c-28-consultar-agenda-dia-semana`:**
  - Datos ficticios: «Ana Ficticia», «Bruno Inventado», `@correo.test`, `@consultorio.test` y `@agenda.test`. No hay DNI ni emails reales.
  - Secretos: no hay credenciales. Los hashes BCrypt son de relleno (`$2a$10$xxx…`) o se generan en el test. `.env.example` y `docker-compose.yml` tienen cambios locales previos de Valentino y **no deben entrar en el commit**.
  - Regla 4: el odontólogo no tiene ningún endpoint de escritura (regla general más test transversal).
  - Regla 6: se usa `Instant` y `OffsetDateTime`, nunca `LocalDateTime`. La fecha por defecto sale del `Clock` inyectable.
  - Regla 9: los DTO son `RespuestaAgenda` y los records anidados, sin entidades JPA, con JPQL parametrizado.
  - Regla 10: los tests de persistencia usan Testcontainers.
  - Regla 11: los 25 escenarios tienen su `@DisplayName` exacto.
  - Regla 7: no hay cambios de esquema.
  - `TurnoJpaRepository` no se modificó, como pide la ficha.

## Diferencias con design.md

No hay diferencias bloqueantes. Hay un ítem para revisar y tres diferencias menores:

1. **A revisar por Juan Pablo: test de C-03 adaptado.** `SesionPersonalIT.rechazaCsrfFaltanteOInvalido` usaba un `ODONTOLOGO` para probar una escritura con CSRF válido y esperaba 200. Con la regla general de RN-33 (D9) eso da 403, que es lo correcto. Como el test verifica CSRF y no permisos por rol, ahora usa un usuario `RECEPCION` mediante el overload `guardar(email, rol)`. El cambio está documentado en el test y en D9. No cambia lo que verifica el test.
2. **Menor, D1:** el design nombraba los DTO `RespuestaTurnoAgenda`, `RespuestaHorarioAgenda` y `RespuestaBloqueoAgenda`. Quedaron como records anidados `RespuestaAgenda.Turno`, `.Horario` y `.Bloqueo`, y el JSON es el mismo.
3. **Menor, D5:** `PacienteRepositorio.porIds` usa `findAllById` de Spring Data en lugar de un JPQL `where id in :ids` escrito a mano. El resultado es equivalente.
4. **Menor, D1:** `ConsultaAgenda` (application) usa `AgendaLecturaPersistencia` (infrastructure) directamente. Es el mismo patrón que ya tenía `ConsultasAgendaAdaptador` con `TurnosPersistencia` en C-19; `ArquitecturaTest` solo restringe `domain`.

## Qué NO se pudo verificar

- No se probó a mano contra el back levantado con la base de dev ni con una sesión real de login. Lo cubren los IT con sesión equivalente (`IdentidadPersonal` más usuario en la base).
- No se midió el rendimiento con un volumen grande de turnos. Para 2 a 5 profesionales no hace falta y está anotado en Risks.
- El formato `-03:00` depende de que Jackson no tenga una zona configurada. Hoy no la tiene, y los IT fijan el formato.

## Próximo paso

- Commit de los archivos de C-28 en `valentino-c-28-consultar-agenda-dia-semana`, sin `.env.example`, `docker-compose.yml` ni `.claude/launch.json`. Después, push y PR a `master`, que revisa Juan Pablo.
- En el PR, revisar la regla general de escrituras (D9), el test de C-03 adaptado y el formato `-03:00` (D3).
- Una vez mergeado: `/opsx:archive c-28-consultar-agenda-dia-semana`.

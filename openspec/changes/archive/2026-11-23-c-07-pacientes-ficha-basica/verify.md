# Verify — c-07-pacientes-ficha-basica (2026-11-23)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes)

Reverificación después del rework (campos `nroAfiliado`, `notas`, `creadoEn`, `consentimiento_datos_en`, CSRF por cookie) y de la decisión de DNI: el DNI de una ficha inactiva queda libre (índice único parcial `uk_paciente_dni_activo`). El change sigue sin commitear.

## Validación estructural

```text
$ openspec validate c-07-pacientes-ficha-basica --strict
Change 'c-07-pacientes-ficha-basica' is valid
```

## Tareas

- 24/24 completas (`[x]`), 0 pendientes. Incluye las dos tareas de rework (5.0a y 5.0b) y las de verificación integral (6.1 a 6.4).

## Matriz escenario → test

Archivos: `G` = `web/PacienteGestionIT`, `R` = `infrastructure/PacienteRepositorioIT`, `C` = `application/PacienteConsultaIT`, `D` = `domain/PacienteTest` (todos bajo `backend/src/test/java/ar/edu/utn/turnos/pacientes/`).

| Escenario | Estado | Test |
|---|---|---|
| Alta con email y sin teléfono | Cubierto | G:59 |
| Alta con teléfono y sin email | Cubierto | G:79 |
| La obra social se guarda como texto libre | Cubierto | G:99 |
| Alta sin nombre o apellido se rechaza | Cubierto | G:117 |
| Alta sin email ni teléfono se rechaza | Cubierto | G:140 (y D:15) |
| Edición que quita el único medio de contacto se rechaza | Cubierto | G:158 |
| Edición que reemplaza email por teléfono se acepta | Cubierto | G:183 |
| Un DNI repetido devuelve 409 | Cubierto | G:209 (y R:30 a nivel base) |
| Varios pacientes pueden no tener DNI | Cubierto | G:231 |
| El DNI de una ficha inactiva queda libre para una ficha nueva | Cubierto | G:250 (y PacienteServicioIT:66, R:70) |
| La base acepta una ficha inactiva y una activa con el mismo DNI | Cubierto | R:42 |
| La base rechaza dos fichas activas con el mismo DNI aunque exista una inactiva | Cubierto | R:56 |
| Un DNI en blanco se trata como no informado | Cubierto | G:272 (y D:25) |
| Alta con número de afiliado y notas | Cubierto | G:569 |
| Las notas demasiado largas se rechazan | Cubierto | G:589 |
| La edición actualiza el número de afiliado y las notas | Cubierto | G:608 |
| El alta registra la fecha de creación y la edición no la cambia | Cubierto | G:630 |
| La respuesta no expone el consentimiento de datos ni acepta informarlo | Cubierto | G:658 |
| La ficha conserva número de afiliado, notas, creación y consentimiento nulo | Cubierto | R:82 |
| El consentimiento de datos se persiste cuando se informa | Cubierto | R:98 |
| Búsqueda por apellido sin distinguir mayúsculas | Cubierto | G:296 |
| Búsqueda por nombre con coincidencia parcial | Cubierto | G:308 |
| Búsqueda por DNI exacto | Cubierto | G:320 |
| Búsqueda por email sin distinguir mayúsculas | Cubierto | G:333 |
| Los filtros se combinan | Cubierto | G:345 |
| Una búsqueda sin filtros devuelve las fichas activas | Cubierto | G:361 |
| Una búsqueda sin coincidencias devuelve una lista vacía | Cubierto | G:376 |
| Un paciente inactivo no aparece en la búsqueda | Cubierto | G:385 |
| La baja deja la ficha inactiva y consultable | Cubierto | G:401 |
| Una ficha dada de baja no se puede consultar como activa | Cubierto | G:415 |
| Dar de baja una ficha inexistente devuelve 404 | Cubierto | G:427 |
| Recepción crea, edita y busca fichas | Cubierto | G:440 |
| Recepción no puede dar de baja una ficha | Cubierto | G:474 |
| Administrador da de baja una ficha | Cubierto | G:488 |
| El odontólogo no accede a las fichas | Cubierto | G:501 (GET, GET por id y POST dan 403) |
| Una petición anónima recibe 401 | Cubierto | G:528 |
| La consulta por identificador devuelve la ficha | Cubierto | C:27 |
| La consulta por email ignora mayúsculas | Cubierto | C:44 |
| La consulta por identificador devuelve también una ficha inactiva | Cubierto | C:60 |
| La consulta de una ficha inexistente devuelve vacío | Cubierto | C:76 |
| Un DNI duplicado no expone los datos del otro paciente | Cubierto | G:540 |

40 de 40 escenarios cubiertos; ninguno parcial ni sin test.

## Ejecución de tests

Comando: `./mvnw verify` en `backend/` (Docker activo, Testcontainers).

- Surefire (unitarios + ArchUnit): 108 tests, 0 fallos, 0 errores, 0 omitidos.
- Failsafe (integración): 342 tests, 0 fallos, 0 errores, 0 omitidos.
- Total: 450 tests, 0 fallos, 0 errores. `BUILD SUCCESS`. La suite completa se corrió dos veces seguidas con el mismo resultado (sin flake de `creadoEn` ni de orden).
- Antes del rework, `master` + C-07 daba 439 tests con 38 fallos y 44 errores.
- Frontend: sin cambios, no se ejecutó `npm test`.

## Reglas de negocio y reglas duras

- RN-32 (contacto y DNI único): cubierta en dominio, API y base (`uk_paciente_dni_activo` (único parcial sobre fichas activas)). RN-35: G:540.
- Regla 4: `ODONTOLOGO` recibe 403 en GET, GET por id y POST; `DELETE` solo `ADMIN` (G:474, G:488).
- Regla 6: `creadoEn` y `consentimientoDatosEn` son `Instant` (`timestamptz`); no hay `LocalDateTime`.
- Regla 9: la API usa `PacienteRequest` y `PacienteResponse` (record); JPQL parametrizado con `CAST(:p AS String)`.
- Regla 11: cada escenario de la spec tiene `@DisplayName` con su nombre exacto.
- Baja solo lógica: `darDeBaja` marca `activo = false`; no hay borrado físico.
- Datos de prueba ficticios (`@example.test`, DNI `0000000x`). Sin secretos ni cambios en `pom.xml`, `SecurityConfig` ni `application*.yml`; `schema-constraints.sql` cambió solo por el índice parcial del DNI.

## Diferencias con design.md

- Ninguna bloqueante ni mayor. `design.md` se actualizó en este rework (D5, D9, D10 y riesgos) para reflejar los campos nuevos y el CSRF por cookie.
- Menor: los `@DisplayName` de `PacienteServicioIT` no son nombres de escenario (son pruebas del servicio, no de la spec).

## Qué NO se pudo verificar

- El arranque con `spring-boot:run` y perfil `dev` no se ejecutó: lo cubre `ContextoAplicacionIT` dentro de `verify`.
- La reserva pública (C-41) todavía no completa `consentimiento_datos_en`; hoy solo se verifica la columna.

## Próximo paso

- Revisión de Juan Pablo en el PR y luego `/opsx:archive c-07-pacientes-ficha-basica`.

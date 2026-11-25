# Verify — c-19-crear-turno-sin-solapamientos (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 1 escenario Parcial y 3 desviaciones del design ya documentadas, para que Juan Pablo las revise en el PR)

## Validación estructural

`openspec validate c-19-crear-turno-sin-solapamientos --strict` → `Change 'c-19-crear-turno-sin-solapamientos' is valid`.

## Tareas

15/15 completas. La 8.1 se completa con esta verificación. Las del `## Workflow follow-up` (PR, merge y archivo) quedan fuera del seguimiento, como define el schema.

## Matriz escenario → test

Spec `agenda-turnos`: 12 requisitos y 29 escenarios. Las rutas son relativas a `backend/src/test/java/ar/edu/utn/turnos/`. Varios escenarios tienen dos tests: el unitario del validador (rápido, sin base) y el de API (de punta a punta con PostgreSQL).

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Recepción crea un turno válido | Cubierto | agenda/web/TurnosApiIT.java:116 | Verifica el 201, `Location`, todos los campos, el creador guardado y un único `TurnoCreado` publicado |
| Datos obligatorios o con formato inválido | **Parcial** | agenda/web/TurnosApiIT.java:171 | Faltantes y `notas` largas devuelven 400 con `errores[].campo`. Un `inicio` con formato inválido o sin zona devuelve 400, pero **sin** el campo: Jackson falla antes de Bean Validation |
| Sin duración se usa la de la prestación | Cubierto | agenda/domain/ValidadorTurnoTest.java:176; agenda/web/TurnosApiIT.java:151 | |
| La duración se puede ajustar | Cubierto | agenda/domain/ValidadorTurnoTest.java:184; agenda/web/TurnosApiIT.java:162 | |
| Un recurso inexistente se rechaza | Cubierto | agenda/web/TurnosApiIT.java:203 | Los cuatro recursos, sin guardar nada |
| Un recurso inactivo se rechaza | Cubierto | agenda/web/TurnosApiIT.java:213 | Los cuatro recursos (`PRESTACION_INACTIVA`) |
| El profesional no realiza la prestación | Cubierto | agenda/domain/ValidadorTurnoTest.java:118; agenda/web/TurnosApiIT.java:229 | |
| Un turno en el pasado se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:142; agenda/web/TurnosApiIT.java:236 | Incluye el caso "justo ahora" |
| Un inicio fuera de la grilla de cinco minutos se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:160; agenda/web/TurnosApiIT.java:245 | |
| Una duración fuera de rango se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:168; agenda/web/TurnosApiIT.java:251 | 7 y 485 (y 0 en el unitario) |
| Un turno que sale del horario se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:205; agenda/web/TurnosApiIT.java:266 | `motivo` `FUERA_DE_HORARIO` |
| Un turno que termina justo al cierre del tramo se acepta | Cubierto | agenda/web/TurnosApiIT.java:272 | |
| Un turno que cruza el corte entre dos tramos se rechaza | Cubierto | agenda/web/TurnosApiIT.java:278 | |
| Un turno dentro de un bloqueo se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:213; agenda/web/TurnosApiIT.java:288 | `motivo` `BLOQUEO` |
| Un turno que empieza cuando termina un bloqueo se acepta | Cubierto | agenda/web/TurnosApiIT.java:296 | |
| Un turno solapado por profesional se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:282; agenda/web/TurnosApiIT.java:320 | `recurso` `PROFESIONAL`; no guarda el segundo |
| Un turno solapado por sillón se rechaza | Cubierto | agenda/domain/ValidadorTurnoTest.java:288; agenda/web/TurnosApiIT.java:329 | `recurso` `SILLON` |
| Un turno que empieza justo cuando termina otro no se solapa | Cubierto | agenda/domain/ValidadorTurnoTest.java:302; agenda/web/TurnosApiIT.java:336 | Borde de RN-02. El 10:30 también se prueba en `IntervaloTest` y en la base (`TurnoTablaIT`) |
| Un turno que empieza un minuto antes del fin de otro se solapa | Cubierto | agenda/domain/ValidadorTurnoTest.java:311; agenda/web/TurnosApiIT.java:344 | Por la API se usa 10:25, porque 10:29 viola RN-09 (design D9). El 10:29 se prueba en `IntervaloTest` |
| Un turno atendido sigue ocupando lugar | Cubierto | agenda/domain/ValidadorTurnoTest.java:323; agenda/web/TurnosApiIT.java:352 | |
| Un turno cancelado no ocupa lugar | Cubierto | agenda/domain/ValidadorTurnoTest.java:317; agenda/web/TurnosApiIT.java:413 | |
| La base rechaza un solapamiento que esquivó la validación | Cubierto | agenda/infrastructure/TurnosPersistenciaIT.java:91 | Por sillón. La variante por profesional está en la línea 101 |
| Dos creaciones simultáneas sobre el mismo hueco | Cubierto | agenda/web/TurnosConcurrenciaIT.java:70 | `@RepeatedTest(5)`, por HTTP: un 201, un 409 `RN-01` y una sola fila |
| Las restricciones de exclusión existen en la base | Cubierto | agenda/infrastructure/TurnoTablaIT.java:66 | El AND de idempotencia lo cubre `scriptIdempotente` (TurnoTablaIT.java:77) |
| El odontólogo no puede crear turnos | Cubierto | agenda/web/TurnosApiIT.java:366 | Además, `elServicioRechazaAlOdontologo` prueba `@PreAuthorize` por separado |
| Sin sesión no se puede crear un turno | Cubierto | agenda/web/TurnosApiIT.java:396 | |
| Sin CSRF válido no se puede crear un turno | Cubierto | agenda/web/TurnosApiIT.java:404 | Usa `csrfConCabeceraIncorrecta()`, sin `csrf()` |
| Transiciones permitidas desde un turno programado | Cubierto | agenda/domain/TurnoTest.java:42 | Parametrizado por estado |
| Un estado final no cambia | Cubierto | agenda/domain/TurnoTest.java:69 | Los tres finales contra los cinco destinos |

**Resumen:** 28 Cubiertos, 1 Parcial y 0 Sin test.

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `./mvnw verify` (backend, Testcontainers con PostgreSQL 16) | `BUILD SUCCESS`. Unitarios: 186 de 186. Integración: 415 de 415. 0 fallidos y 0 saltados |
| `npm run lint` (frontend) | Sin errores |
| `npm test` (Vitest) | 34 archivos, 290 de 290 tests |
| `npm run build` | Correcto |

Tests de C-19 dentro de esa corrida:

| Tipo | Clase | Tests |
|---|---|---|
| Unitario | `IntervaloTest` | 9 |
| Unitario | `TurnoTest` | 14 |
| Unitario | `TurnoCreadoTest` | 2 |
| Unitario | `ValidadorTurnoTest` | 25 |
| Unitario | `ArquitecturaTest` | 2 |
| Integración | `TurnoTablaIT` | 8 |
| Integración | `TurnosPersistenciaIT` | 5 |
| Integración | `TurnosApiIT` | 26 |
| Integración | `TurnosConcurrenciaIT` | 5 |

**Pruebas de mutación durante el apply.** Se rompió el código a propósito para comprobar que los tests detectan la falla. Después se restauró y se verificó que no quedara ningún `MUTANTE` en `src/`.

| Mutación | Resultado |
|---|---|
| Adaptador que ignora horario y bloqueos | Fallan los 3 tests de RN-04 |
| Sin consulta previa de solapamiento | La carrera sigue en verde (5/5): la base sola sostiene el invariante |
| Sin consulta previa y sin traducción del error | La carrera falla (5/5) |
| Sin `@PreAuthorize` | Falla `elServicioRechazaAlOdontologo` |

## Reglas de negocio y reglas duras

- **RN-01 y RN-02:** hay solapamiento real por profesional y por sillón, y el borde contiguo se prueba en cuatro niveles: `Intervalo`, validador, API y `EXCLUDE` en PostgreSQL.
- **RN-15:** hay test con las restricciones `EXCLUDE` reales (Testcontainers), test de traducción a 409 y test de carrera por HTTP.
- **Las demás reglas que cita la spec:** RN-03, RN-04, RN-09, RN-10, RN-11, RN-12, RN-13, RN-16, RN-20 y RN-33 tienen escenario y test.
- **Datos ficticios (regla 1):** los fixtures (`DatosAgenda`) usan nombres "Ficticio", emails `@agenda.test` y `@consultorio.test`, y sufijos aleatorios. No hay DNI ni datos reales.
- **Secretos (regla 2):** el diff no toca `.env` ni `application*.yml`. El único hash de contraseña es un relleno de prueba (`$2a$10$xxx…`), no una credencial.
- **Reglas 5 a 10:**
  - `EXCLUDE` como garantía final;
  - `timestamptz`/`Instant` y `Clock` inyectable;
  - sin Flyway: las restricciones van en el script idempotente;
  - sesión, sin JWT;
  - DTO, sin entidades en la API;
  - Testcontainers, sin H2.
- **Regla 11:** los 29 escenarios tienen `@DisplayName` con su nombre exacto.
- **`.with(csrf())`:** no se usa. `ArquitecturaTestsTest` está en verde.

## Diferencias con design.md

Ninguna bloqueante. Las tres desviaciones surgieron durante el apply y ya quedaron registradas en el design:

1. **Mayor, documentada en D5.** El test de carrera encontró que dos `INSERT` simultáneos se bloqueaban en cruz entre las dos `EXCLUDE`. PostgreSQL abortaba uno por deadlock y el perdedor recibía un 500. Se agregó `pg_advisory_xact_lock` por profesional y por sillón, en orden fijo, antes de validar. **Para revisar en el PR:** C-35 (reprogramar) tiene que tomar los mismos locks.
2. **Menor, documentada en D2.** El validador recibe la interfaz `ConsultasAgenda` en lugar de "todos los datos ya cargados", porque el intervalo depende de la duración de la prestación.
3. **Menor, documentada en D7.** `TurnoCreado` lleva `sillonId` y el `Intervalo` completo.

Otros detalles menores, no listados en el design:

- `actualizado_en` queda nulo al crear; lo completarán C-27, C-29 y C-35.
- El adaptador `ConsultasAgendaAdaptador` vive en `agenda.application`.

## Qué NO se pudo verificar

- Nada quedó sin ejecutar. C-19 no tiene UI (`Decision: no (no UI)`), así que no hay verificación visual.
- El test de carrera prueba la garantía con dos pedidos simultáneos repetidos 5 veces; no es una prueba de carga.

## Próximo paso

PR de `juan-pablo-c-19` a `master`. Con el CI en verde y la revisión hecha, merge y `/opsx:archive c-19-crear-turno-sin-solapamientos`.

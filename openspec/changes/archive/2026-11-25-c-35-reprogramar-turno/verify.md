# Verify — c-35-reprogramar-turno (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 24 de 24 escenarios cubiertos; sin desvíos del design)

## Validación estructural

`openspec validate c-35-reprogramar-turno --strict` → `Change 'c-35-reprogramar-turno' is valid`.

## Tareas

13/13 completas. La 4.3 se completa con esta verificación. Las del `## Workflow follow-up` (PR, merge, archivo, CHANGES.md e issue) quedan fuera del seguimiento, como define el schema.

## Matriz escenario → test

Spec `agenda-turnos` (delta): 7 requisitos y 24 escenarios. Las rutas son relativas a `backend/src/test/java/ar/edu/utn/turnos/`.

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Recepción mueve un turno a otro horario | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:143 | Mismo `id` y un solo turno del paciente en la base |
| Se puede cambiar de profesional, de sillón y de duración | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:163 | Como ADMIN, 45 minutos |
| Un turno confirmado sigue confirmado | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:181 | |
| Reprogramar al mismo horario no cambia nada | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:190 | La fila, incluida la versión, queda igual y no hay eventos |
| Un pedido sin inicio se rechaza | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:203 | `{}`, fecha mal formada y JSON roto |
| Un turno inexistente no se puede reprogramar | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:216 | `404 TURNO_INEXISTENTE` |
| Un turno cancelado no se puede reprogramar | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:228; agenda/domain/TurnoTest.java | Parametrizado: `CANCELADO`, `ATENDIDO` y `AUSENTE` → `409` |
| Un turno que ya empezó no se puede reprogramar | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:240; agenda/domain/TurnoTest.java | Inicio igual a "ahora" → `422 RN-05` |
| Un turno se mueve quince minutos dentro de su propio intervalo | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:252 | 10:00 → 10:15 |
| Un horario ocupado por otro turno responde conflicto | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:262 | Choque por profesional y por sillón → `409 RN-01` |
| Un horario fuera de atención se rechaza | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:281 | Fuera de horario y dentro de un bloqueo → `422 RN-04` |
| El horario nuevo respeta el inicio futuro, la grilla y los recursos activos | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:295 | `RN-13`, `RN-09`, `SILLON_INACTIVO` y `RN-11` |
| Reprogramar dentro del plazo queda marcado como tardío | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:317 | Faltan 10 horas |
| Reprogramar a 24 horas exactas no es tardío | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:327; agenda/domain/TurnoTest.java | Además, un minuto después del plazo ya es tardío (dominio) |
| La marca de tardía se conserva | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:336; agenda/domain/TurnoTest.java | |
| El horario anterior se puede volver a dar | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:348 | Antes: 409; después de reprogramar: 201 |
| Reprogramar publica el turno reprogramado y el hueco anterior | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:362 | Un `TurnoReprogramado` y un `HuecoLiberado` con los recursos anteriores |
| Moverse dentro del propio intervalo libera solo lo que dejó | Cubierto | agenda/domain/evento/EventosReprogramacionTest.java:51 | También por la API (TurnosReprogramacionApiIT.java:380) |
| Cambiar de profesional libera el intervalo completo | Cubierto | agenda/domain/evento/EventosReprogramacionTest.java:61 | |
| Una reprogramación rechazada no publica eventos | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:391 | Casos `404`, `409` y dos `422` |
| Una reprogramación y un alta compiten por el mismo horario | Cubierto | agenda/web/TurnosReprogramacionConcurrenciaIT.java:81 | `@RepeatedTest(5)`: un éxito, un `409 RN-01` y un solo turno a esa hora |
| El odontólogo no puede reprogramar turnos | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:407 | `403` también con `{}`; `@PreAuthorize` se prueba aparte (línea 422) |
| Sin sesión no se puede reprogramar un turno | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:438 | |
| Sin CSRF válido no se puede reprogramar un turno | Cubierto | agenda/web/TurnosReprogramacionApiIT.java:449 | Usa `csrfConCabeceraIncorrecta()`, sin `csrf()` |

**Resumen:** 24 Cubiertos, 0 Parciales y 0 Sin test.

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `./mvnw verify` (backend, Testcontainers con PostgreSQL 16) | `BUILD SUCCESS`. Unitarios: 293 de 293. Integración: 546 de 546. 0 fallidos y 0 saltados |

C-35 no toca el frontend.

**Pruebas de mutación durante el apply.** Se rompió el código a propósito y después se restauró.

| Mutación | Resultado |
|---|---|
| Sin excluir al propio turno (`turnoExcluidoId = null`) | Fallan `quinceMinutosDentro`, `publicaSoloElTramoLibre` y `mismoHorarioNoCambiaNada` (409) |
| Sin matcher en `SecurityConfig` | El odontólogo con `{}` recibe 400 en lugar de 403 |
| Marca de tardía sin conservar (`reprogramadoTarde \|\|` quitado) | Falla `marcaSeConserva` |

## Reglas de negocio y reglas duras

- **RN-18:** se mueve el mismo turno; se revalidan RN-01 a RN-04, RN-09, RN-11 y RN-13 excluyendo al propio turno, y se libera el horario anterior.
- **RN-05, SU-07 y PA-06:** la tardía se mide sobre el inicio original con límite inclusivo, se permite, se registra y se conserva. PA-06 quedó resuelta en la KB.
- **RN-12:** solo desde `PROGRAMADO` o `CONFIRMADO`, que se conserva.
- **RN-15:** lock de fila y advisory locks, y el `EXCLUDE` como garantía final (test de carrera).
- **Regla dura 4:** el odontólogo recibe 403 en el filtro y en el servicio.
- **Reglas 5 y 6:** `Intervalo` con `[inicio, fin)` e `Instant` con `Clock` inyectable.
- **Regla 7:** sin cambios de esquema.
- **Regla 9:** DTOs (`SolicitudReprogramarTurno`, `RespuestaTurno`).
- **Regla 10:** Testcontainers, sin H2.
- **Regla 11:** los 24 escenarios tienen `@DisplayName` con su nombre exacto.
- **Datos ficticios:** fixtures de `DatosAgenda`.

## Diferencias con design.md

Ninguna. La KB (Flujo 2) se alineó con el design: `POST`, `409` para un turno en estado final y `422` para uno ya empezado.

## Para los changes siguientes

- **C-32:** escuchar `TurnoReprogramado` con `AFTER_COMMIT` para recalcular la expiración del token con `nuevo().inicio()`.
- **C-36:** una reprogramación puede publicar 0, 1 o 2 `HuecoLiberado`; cada uno se evalúa por separado.
- **C-44:** el email de reprogramación sale de `TurnoReprogramado`, que trae los intervalos anterior y nuevo.
- **C-46:** el frontend llama a `POST /api/turnos/{id}/reprogramar` con `inicio` y, opcionalmente, `profesionalId`, `sillonId` y `duracionMin`.

## Qué NO se pudo verificar

- C-35 no tiene UI (`Decision: no (no UI)`), así que no hay verificación visual.
- El test de carrera repite 5 veces dos pedidos simultáneos; no es una prueba de carga.

## Próximo paso

PR de `juan-pablo-c-35` a `master` con "Closes #35". Con el CI en verde, merge y `/opsx:archive c-35-reprogramar-turno`.

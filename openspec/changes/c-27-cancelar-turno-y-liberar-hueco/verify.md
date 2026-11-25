# Verify — c-27-cancelar-turno-y-liberar-hueco (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 19 de 19 escenarios cubiertos; 1 ajuste del design documentado en D5)

## Validación estructural

`openspec validate c-27-cancelar-turno-y-liberar-hueco --strict` → `Change 'c-27-cancelar-turno-y-liberar-hueco' is valid`.

## Tareas

16/16 completas. La 7.1 se completa con esta verificación. Las del `## Workflow follow-up` (PR, merge y archivo) quedan fuera del seguimiento, como define el schema.

## Matriz escenario → test

Spec `agenda-turnos` (delta): 8 requisitos y 19 escenarios. Las rutas son relativas a `backend/src/test/java/ar/edu/utn/turnos/`.

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Recepción cancela un turno programado | Cubierto | agenda/web/TurnosCancelacionApiIT.java:114 | 200, campos de la respuesta y fila en la base |
| Se puede cancelar un turno confirmado | Cubierto | agenda/web/TurnosCancelacionApiIT.java:133 | Como ADMIN y sin cuerpo |
| Un motivo demasiado largo se rechaza | Cubierto | agenda/web/TurnosCancelacionApiIT.java:146 | 256 caracteres → 400 `motivo`; JSON roto → 400 |
| Un turno inexistente no se puede cancelar | Cubierto | agenda/web/TurnosCancelacionApiIT.java:160 | 404 `TURNO_INEXISTENTE` |
| Un turno ya cancelado no se vuelve a cancelar | Cubierto | agenda/web/TurnosCancelacionApiIT.java:183 | La fila de la primera cancelación no cambia |
| Un turno atendido no se puede cancelar | Cubierto | agenda/web/TurnosCancelacionApiIT.java:197; agenda/domain/TurnoTest.java | 409 también en el pasado (design D2) |
| Un turno que ya empezó no se puede cancelar | Cubierto | agenda/web/TurnosCancelacionApiIT.java:208; agenda/domain/TurnoTest.java | Inicio igual a "ahora" → 422 `RN-05` |
| Cancelar a 24 horas exactas no es tardío | Cubierto | agenda/domain/PoliticaCancelacionTest.java:16 | También por la API (TurnosCancelacionApiIT.java:219) |
| Cancelar un minuto después del plazo es tardío | Cubierto | agenda/domain/PoliticaCancelacionTest.java:24 | Ídem; además un milisegundo después ya es tardío |
| Un cambio del plazo no altera cancelaciones anteriores | Cubierto | agenda/web/TurnosCancelacionApiIT.java:233 | Con 72 h, una cancelación nueva sí queda tardía |
| La cancelación guarda quién la hizo y cuándo | Cubierto | agenda/web/TurnosCancelacionApiIT.java:168 | `USUARIO`, id del usuario y el instante del reloj |
| Tras cancelar se puede dar otro turno en el mismo horario | Cubierto | agenda/web/TurnosCancelacionApiIT.java:262 | Antes de cancelar: 409; después: 201 |
| Una cancelación tardía también libera el horario | Cubierto | agenda/web/TurnosCancelacionApiIT.java:274 | |
| Una cancelación publica un único hueco liberado | Cubierto | agenda/web/TurnosCancelacionApiIT.java:289 | Un `TurnoCancelado` y un `HuecoLiberado` |
| Una cancelación rechazada no publica avisos | Cubierto | agenda/web/TurnosCancelacionApiIT.java:310 | Casos 409 y 422 |
| Dos cancelaciones simultáneas del mismo turno | Cubierto | agenda/web/TurnosCancelacionConcurrenciaIT.java:91 | `@RepeatedTest(5)`: un 200, un 409 y un solo `HuecoLiberado` |
| El odontólogo no puede cancelar turnos | Cubierto | agenda/web/TurnosCancelacionApiIT.java:328 | 403 también con un motivo inválido; `@PreAuthorize` se prueba aparte |
| Sin sesión no se puede cancelar un turno | Cubierto | agenda/web/TurnosCancelacionApiIT.java:359 | |
| Sin CSRF válido no se puede cancelar un turno | Cubierto | agenda/web/TurnosCancelacionApiIT.java:368 | Usa `csrfConCabeceraIncorrecta()`, sin `csrf()` |

**Resumen:** 19 Cubiertos, 0 Parciales y 0 Sin test.

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `./mvnw verify` (backend, Testcontainers con PostgreSQL 16) | `BUILD SUCCESS`. Unitarios: 236 de 236. Integración: 453 de 453. 0 fallidos y 0 saltados |
| `npm run lint` (frontend) | Sin errores |
| `npm test` (Vitest) | 34 archivos, 290 de 290 tests |
| `npm run build` | Correcto |

**Pruebas de mutación durante el apply.** Se rompió el código a propósito y después se restauró; no quedó ningún `MUTANTE` en `src/`.

| Mutación | Resultado |
|---|---|
| Plazo con `<=` en lugar de `<` | Fallan los 2 tests del borde exacto |
| Sin `@PreAuthorize` en el servicio | Falla `elServicioRechazaAlOdontologo` |
| Sin lock de fila | La carrera sigue en verde: el `@Version` responde 409 al perdedor (segunda barrera) |
| Sin lock de fila y sin traducción del `@Version` | La carrera falla 5 de 5 |
| Sin matcher en `SecurityConfig` | El odontólogo con motivo inválido recibe 400 en lugar de 403 (test en rojo antes del matcher) |

## Reglas de negocio y reglas duras

- **RN-05:** límite inclusivo probado en milisegundos (dominio) y por HTTP; un turno iniciado no se cancela.
- **RN-06 y RN-10:** el intervalo queda libre sin tocar las `EXCLUDE` de C-19; se crea otro turno en el mismo horario.
- **RN-12:** solo desde `PROGRAMADO` o `CONFIRMADO`.
- **RN-19:** se guardan actor, usuario (con FK), instante, motivo y marca de tardía.
- **Regla dura 4:** el odontólogo recibe 403 en el filtro y en el servicio.
- **Reglas 6 y 7:** `Instant` y `Clock` inyectable; la columna nueva la crea Hibernate y la FK va en el script idempotente, sin Flyway.
- **Regla 9:** DTOs (`SolicitudCancelarTurno`, `RespuestaTurno`), sin entidades en la API.
- **Regla 10:** Testcontainers, sin H2.
- **Regla 11:** los 19 escenarios tienen `@DisplayName` con su nombre exacto.
- **Datos ficticios:** los fixtures de `DatosAgenda`; ningún dato real.

## Diferencias con design.md

1. **Menor, documentada en D5.** La traducción del conflicto de versión a `409 TURNO_MODIFICADO` está en `TurnosPersistencia.guardar` y no en `ManejadorErrores`: `turno` es la única entidad con `@Version`.
2. **Desvío de la KB, aprobado en D3.** Un turno en estado final responde `409` (Flujo 2 de la KB dice `422`).

## Para los changes siguientes

- **C-36:** escuchar `HuecoLiberado` con `@TransactionalEventListener(phase = AFTER_COMMIT)` y agregar a la respuesta las entradas avisadas.
- **C-35:** tomar el lock de fila (`buscarParaActualizar`) y los advisory locks de C-19.
- **C-32/C-39:** usar `CancelarTurnoService.cancelar(id, PACIENTE_TOKEN, null, motivo)` después de validar el token.

## Qué NO se pudo verificar

- C-27 no tiene UI (`Decision: no (no UI)`), así que no hay verificación visual.
- El test de carrera usa dos pedidos simultáneos repetidos 5 veces; no es una prueba de carga.

## Próximo paso

PR de `juan-pablo-c-27` a `master` con "Closes #27". Con el CI en verde, merge y `/opsx:archive c-27-cancelar-turno-y-liberar-hueco`.

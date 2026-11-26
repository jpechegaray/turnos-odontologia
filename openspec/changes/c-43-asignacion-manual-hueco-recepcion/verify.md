# Verify — c-43-asignacion-manual-hueco-recepcion (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (sin bloqueantes)

## Validación estructural

`openspec validate c-43-asignacion-manual-hueco-recepcion --strict` → `Change 'c-43-asignacion-manual-hueco-recepcion' is valid`.

## Tareas

Están las 19 completas.

## Prueba manual en dev (tarea 5.3, 2026-11-25)

Se levantó el backend con el código del change en el perfil `dev` (puerto 8081), contra el PostgreSQL de `docker compose`. Se usaron datos ficticios propios, todos marcados `C43-PRUEBA`:

- un profesional con horario los jueves de 08:00 a 18:00 y una prestación de 30 minutos;
- un sillón nuevo, que es el de id más alto;
- dos pacientes, Ana y Beto, con una entrada `ACTIVA` cada uno;
- dos ofertas `ENVIADA` del hueco del jueves 3/12 a las 10:00, con fin a las 11:00 y ese sillón como sugerido;
- un usuario RECEPCION temporal.

| # | Petición | Esperado | Obtenido |
|---|---|---|---|
| 1 | Asignar el hueco a Ana sin sillón y con notas | 201, sillón sugerido, 30 min | 201: turno en el sillón sugerido, de 10:00 a 10:30 ART, con notas y `creadoPor` = el usuario RECEPCION |
| 2 | Asignar el mismo hueco a Beto | 409 `RN-01` | 409 `RN-01`; Beto sigue `ACTIVA` |
| 3 | Volver a asignar a Ana | 409 `ENTRADA_NO_ACTIVA` | 409 `ENTRADA_NO_ACTIVA` |
| 4 | Entrada inexistente | 404 `ENTRADA_NO_ENCONTRADA` | 404 `ENTRADA_NO_ENCONTRADA` |
| 5 | `inicio` sin zona | 400 | 400 en Problem Details |
| 6 | Sin sesión | 401 | 401 |
| 7 | Con sesión y sin CSRF | 403 | 403 |

Estado final en la base:

- La entrada de Ana quedó `ASIGNADA` con el `turnoAsignadoId` del turno creado. La de Beto sigue `ACTIVA`.
- Las **dos** ofertas quedaron `TOMADA`, aunque su fin original (11:00) era distinto del fin del turno (10:30). Esto confirma el criterio por inicio de D6.

Al terminar se borraron todos los datos de prueba y el usuario temporal, y un recuento de los registros `C43-PRUEBA` dio 0 en todas las tablas. No hubo tokens ni notificaciones para limpiar. El caso del odontólogo no se probó a mano porque no hay una cuenta ODONTOLOGO de prueba; lo cubre el IT.

## Matriz escenario → test

Spec: `asignacion-hueco`. Las rutas son relativas a `backend/src/test/java/ar/edu/utn/turnos/listaespera/`.

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Recepción asigna un hueco a una entrada compatible | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:211` | ADMIN y RECEPCION; verifica 201, Location, datos del turno, entrada ASIGNADA y turnoAsignadoId |
| La asignación guarda las notas del turno | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:234` | Revisa las notas en la respuesta y en la tabla |
| El cuerpo exige profesional e inicio con zona | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:252` | Los tres casos dan 400 en Problem Details y no se crea turno |
| Una entrada inexistente responde 404 | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:272`, `asignacion/application/AsignarHuecoServiceTest.java:159` | |
| Una entrada que ya no está activa responde 409 | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:283`, `AsignarHuecoServiceTest.java:144` | El IT prueba con CANCELADA |
| Una entrada incompatible con el hueco responde 422 | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:297`, `AsignarHuecoServiceTest.java:109` | El IT usa un profesional preferido distinto |
| La duración del turno es la de la prestación de la entrada | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:314` | |
| Sin sillón se prefiere el sugerido por la oferta si sigue libre | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:327`, `AsignarHuecoServiceTest.java:191` | El IT detectó que el sugerido se ignoraba si no era el de menor id; se corrigió en apply |
| Sin sillón libre sugerido se usa el primero activo libre | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:341`, `AsignarHuecoServiceTest.java:252` | |
| Un sillón indicado ocupado responde 409 | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:359` | |
| Una segunda asignación del mismo hueco responde 409 | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:379` | Las asignaciones son secuenciales; ver la diferencia menor 3 |
| Las ofertas del hueco pasan a TOMADA | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:397` | Más `oferta/infrastructure/TomarOfertasDelHuecoIT` |
| Sin ofertas la asignación igual funciona | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:412` | |
| El odontólogo no asigna huecos | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:448` | Prueba con cuerpo válido y con cuerpo malformado |
| Sin sesión no se asigna un hueco | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:463` | |
| Las escrituras requieren CSRF válido | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:480` | Prueba sin token y con token incorrecto; la entrada y la oferta no cambian |
| Un fallo al crear el turno no deja la entrada cerrada ni las ofertas tomadas | Cubierto | `asignacion/web/AsignarHuecoApiIT.java:426` | Prueba la atomicidad real: el IT no usa `@Transactional` |

Tests de apoyo de las tareas 1.x:

- `oferta/domain/OfertaHuecoTest` ("Tomar una oferta enviada la deja tomada" y "Tomar una oferta vencida o ya tomada no la cambia").
- `oferta/infrastructure/TomarOfertasDelHuecoIT`.

## Ejecución de tests

- **Suite completa:** `./mvnw -o verify` → BUILD SUCCESS, con 381 tests unitarios (surefire) y 809 de integración (failsafe con Testcontainers). No hubo fallos, errores ni saltados. Incluye `ArquitecturaTest`.
- **Tests del change, en una corrida focalizada previa:** todos en verde. Son `AsignarHuecoServiceTest` (12), `OfertaHuecoTest` (6), `AsignarHuecoApiIT` (19) y `TomarOfertasDelHuecoIT` (2).
- **Front:** no aplica. El change es solo backend y el front de la lista de espera es C-47.

## Reglas de negocio y reglas duras

- **RN-01/RN-02 y EXCLUDE:** la asignación usa `CrearTurnoService`, que ya tiene sus tests de borde y de concurrencia con `EXCLUDE` (C-19). C-43 prueba el 409 `RN-01` con un sillón ocupado y con una segunda asignación del hueco.
- **RN-15, RN-22, RN-24 y RN-38:** están cubiertas por los escenarios de la matriz.
- **Datos:** los tests usan solo datos ficticios (`DatosAgenda`, `DatosFicticios` y correos `*.test`).
- **Secretos:** no hay. No cambió ningún `application*.yml` ni `.env`, y tampoco el esquema.
- **Rol ODONTOLOGO:** no tiene ningún endpoint de escritura. Hay una regla explícita en `SecurityConfig` y `@PreAuthorize` en el servicio.

## Diferencias con design.md

- **Menor 1 (D4):** `DisponibilidadService` informa un solo sillón por horario. Por eso el sillón sugerido se verifica aparte con `ConsultaOcupacionDisponibilidad`. Quedó registrado en `design.md` como "Ajuste en apply".
- **Menor 2 (D6):** el método que toma ofertas por inicio se llama `tomarDelHuecoPorInicio` en el repositorio, porque C-42 ya usa `tomarDelHueco(prof, inicio, fin)`. Se agregó `sugeridaPara(...)`. También quedó registrado en `design.md`.
- **Menor 3 (D10):** la competición por el mismo hueco se prueba con llamadas secuenciales, no con dos hilos. La garantía concurrente la dan los advisory locks y la `EXCLUDE` de C-19, que ya están probados en ese change.
- **Menor 4:** `agenda.web.RespuestaTurno` pasó a ser `public`. D8 pide reutilizarla desde `listaespera.asignacion.web`.

## Qué NO se pudo verificar

- La concurrencia real con dos hilos específica de C-43.
- El 403 del odontólogo a mano en dev (sí lo cubre el IT).

## Próximo paso

Abrir el PR a `master` con el ID C-43, que revisa Juan Pablo. Después del merge, correr `/opsx:archive c-43-asignacion-manual-hueco-recepcion`.

Juan Pablo puede revisar en el PR las diferencias menores 3 y 4.

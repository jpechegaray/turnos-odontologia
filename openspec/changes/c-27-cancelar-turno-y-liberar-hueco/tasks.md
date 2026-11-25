# Tasks

Aplicar TDD estricto en cada tarea: primero el test que falla, después el código mínimo y por último el refactor. Usar solo datos ficticios. Cuando un test cubra un escenario de la spec, su `@DisplayName` lleva el nombre exacto del escenario. En los tests de integración, usar `csrfValido()` y `csrfConCabeceraIncorrecta()` de `PruebaIntegracionBase`, nunca `.with(csrf())`. Un commit por etapa, con confirmación previa.

## 1. Política de cancelación

- [x] 1.1 Crear `agenda.domain.PoliticaCancelacion` con `esTardia(inicio, ahora, plazo)` (design D1). Se verifica con JUnit, sin Spring:
  - "Cancelar a 24 horas exactas no es tardío";
  - "Cancelar un minuto después del plazo es tardío";
  - plazo 0 (nunca tardía antes del inicio) y plazo 48 horas.

## 2. Dominio del turno

- [x] 2.1 Crear `ActorCancelacion` y sumar `canceladoPorUsuarioId` al record `Turno` (design D4). Ajustar `Turno.nuevo`, `pasarA` y los tests existentes; la suite de C-19 sigue en verde.
- [x] 2.2 Agregar `Turno.cancelar(actor, usuarioId, motivo, ahora, plazo)` (design D2). Se verifica con JUnit:
  - desde `PROGRAMADO` y `CONFIRMADO` queda `CANCELADO` con fecha, actor, usuario, motivo y marca de tardía;
  - desde `CANCELADO`, `ATENDIDO` y `AUSENTE` lanza `409 TURNO_TRANSICION_INVALIDA`;
  - con `ahora` igual al inicio o posterior lanza `422 RN-05`;
  - un `ATENDIDO` en el pasado da `409`, no `422`.
- [x] 2.3 Agregar las fábricas `TurnoCancelado.de(turno)` y `HuecoLiberado.de(turno)`. Se verifica con JUnit: copian los datos del turno y exigen un turno cancelado con id.

## 3. Persistencia

- [x] 3.1 Mapear `cancelado_por_usuario_id` en `TurnoJpa` y agregar su FK a `usuario` en `schema-constraints.sql`, en un bloque `DO $$` idempotente. Se verifica con Testcontainers: la FK existe en `pg_constraint` y correr el script dos veces no falla.
- [x] 3.2 Agregar a `TurnosPersistencia` la lectura `buscarParaActualizar(id)` con `PESSIMISTIC_WRITE` (design D5). Se verifica con Testcontainers: una cancelación guardada se relee con todos los campos.

## 4. Servicio y API

- [x] 4.1 Crear `CancelarTurnoService` y `POST /api/turnos/{id}/cancelar`, con `RespuestaTurno` ampliada (design D7). Se verifica con integración:
  - "Recepción cancela un turno programado";
  - "Se puede cancelar un turno confirmado";
  - "Un motivo demasiado largo se rechaza";
  - "Un turno inexistente no se puede cancelar";
  - "La cancelación guarda quién la hizo y cuándo".
- [x] 4.2 Cubrir por API las reglas de estado y plazo:
  - "Un turno ya cancelado no se vuelve a cancelar";
  - "Un turno atendido no se puede cancelar";
  - "Un turno que ya empezó no se puede cancelar";
  - "Un cambio del plazo no altera cancelaciones anteriores".
- [x] 4.3 Cubrir la liberación del intervalo:
  - "Tras cancelar se puede dar otro turno en el mismo horario";
  - "Una cancelación tardía también libera el horario".
- [x] 4.4 Publicar `TurnoCancelado` y `HuecoLiberado` (design D6). Se verifica con `@RecordApplicationEvents`:
  - "Una cancelación publica un único hueco liberado";
  - "Una cancelación rechazada no publica avisos".

## 5. Concurrencia

- [x] 5.1 Traducir `ObjectOptimisticLockingFailureException` a `409 TURNO_MODIFICADO` en `TurnosPersistencia.guardar` (design D5, ajuste del apply). Se verifica con `TurnosPersistenciaIT`: guardar con una versión vieja responde ese 409 y no cambia la fila.
- [x] 5.2 Escribir "Dos cancelaciones simultáneas del mismo turno": dos hilos con sesiones distintas y un `CountDownLatch`, `@RepeatedTest(5)`. Se verifica: un `200`, un `409` y un solo `HuecoLiberado`.

## 6. Acceso

- [ ] 6.1 Agregar el matcher en `SecurityConfig` y `@PreAuthorize` en el servicio. Se verifica con:
  - "El odontólogo no puede cancelar turnos";
  - "Sin sesión no se puede cancelar un turno";
  - "Sin CSRF válido no se puede cancelar un turno";
  - un test que llama al servicio como odontólogo y espera `AccessDeniedException`.

## 7. Verificación final

- [ ] 7.1 Correr `./mvnw verify` (con Docker) y, en `frontend`, `npm run lint`, `npm test` y `npm run build`: todo en verde. Después correr `openspec-verify` para generar `verify.md`, con cada escenario mapeado a su test y el veredicto.

## Workflow follow-up

- Abrir el PR de `juan-pablo-c-27` a `master` con "Closes #27", esperar el CI en verde y mergear con `--admin --merge`.
- Después del merge: `/opsx:archive`, CHANGES.md (C-27 hecho por Juan Pablo, con PR y fecha) y verificar que el issue #27 quedó cerrado.

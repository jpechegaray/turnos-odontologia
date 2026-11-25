# Tasks

Aplicar TDD estricto en cada tarea: primero el test que falla, después el código mínimo y por último el refactor. Usar solo datos ficticios. Cuando un test cubra un escenario de la spec, su `@DisplayName` lleva el nombre exacto del escenario. En los tests de integración, usar `csrfValido()` y `csrfConCabeceraIncorrecta()` de `PruebaIntegracionBase`, nunca `.with(csrf())`. Un commit por etapa, con confirmación previa.

## 1. Dominio del turno

- [x] 1.1 Agregar `Turno.marcarAsistencia(destino, ahora)` (design D1). Se verifica con JUnit, sin Spring y con instantes fijos:
  - "Recepción marca un turno como atendido" y "Recepción marca la ausencia de un turno confirmado" a nivel de dominio, desde `PROGRAMADO` y `CONFIRMADO`;
  - "A la hora exacta de inicio ya se puede marcar" y "Antes del inicio no se puede marcar" (`422 RN-20`);
  - desde `CANCELADO` lanza `409 TURNO_TRANSICION_INVALIDA`, incluso con el inicio en el pasado, y un `ATENDIDO` en el pasado da `409`, no `422`;
  - un destino distinto de `ATENDIDO` y `AUSENTE` es rechazado;
  - el intervalo, los recursos y las notas no cambian.
- [x] 1.2 Agregar `Turno.corregirAsistencia(destino)` (design D1). Se verifica con JUnit:
  - `ATENDIDO → AUSENTE` y `AUSENTE → ATENDIDO` se aceptan, sin mirar el reloj;
  - "Repetir la misma marca se rechaza" (`409 TURNO_TRANSICION_INVALIDA`);
  - desde `PROGRAMADO`, `CONFIRMADO` y `CANCELADO` lanza `409`;
  - "La corrección no cambia lo demás".
- [x] 1.3 Confirmar que `EstadoTurno` no cambió. Se verifica con los tests existentes en verde y con "La corrección de asistencia no abre otras transiciones": un turno corregido sigue rechazando `cancelar` y `pasarA(CONFIRMADO)` con `409`.

## 2. Servicio y API

- [x] 2.1 Crear `MarcarAsistenciaService` y `POST /api/turnos/{id}/asistencia` con `SolicitudMarcarAsistencia` (design D2 y D4). Se verifica con integración y PostgreSQL:
  - "Recepción marca un turno como atendido", con el estado guardado en la base;
  - "Recepción marca la ausencia de un turno confirmado";
  - "A la hora exacta de inicio ya se puede marcar" y "Antes del inicio no se puede marcar", con `RelojFijoConfig`;
  - "Un estado destino inválido se rechaza": `CANCELADO`, `CONFIRMADO` y cuerpo sin estado dan `400` con el campo `estado`;
  - "Un turno inexistente no admite asistencia".
- [x] 2.2 Cubrir por API la corrección y el estado cancelado:
  - "Un turno cancelado no se marca";
  - "El administrador corrige una marca";
  - "Recepción no puede corregir una marca" (`403`, el turno no cambia);
  - "Repetir la misma marca se rechaza".
- [x] 2.3 Verificar la serialización de dos marcas simultáneas del mismo turno: dos hilos con sesiones distintas y un `CountDownLatch`, `@RepeatedTest(5)`, como en C-27. Se verifica: nunca hay un `500`, el estado final es uno de los dos pedidos y el turno queda en un estado válido.

## 3. Acceso

- [x] 3.1 Agregar el matcher en `SecurityConfig` y `@PreAuthorize` en el servicio. Se verifica con:
  - "El odontólogo no puede marcar asistencia";
  - "Sin sesión no se puede marcar asistencia";
  - "Sin CSRF válido no se puede marcar asistencia";
  - un test que llama al servicio como odontólogo y espera `AccessDeniedException`.

## 4. Verificación final

- [x] 4.1 Correr `./mvnw verify` (con Docker) y, en `frontend`, `npm run lint`, `npm test` y `npm run build`: todo en verde. Después correr `openspec-verify` para generar `verify.md`, con cada escenario mapeado a su test y el veredicto.

## Workflow follow-up

- Abrir el PR de la rama propia a `master` con el título "C-29 marcar-asistencia-ausencia", esperar el CI en verde y que Juan Pablo lo revise y lo mergee.
- Después del merge: `/opsx:archive`, marcar C-29 como hecho en CHANGES.md y avisar que C-46 queda con una dependencia menos.

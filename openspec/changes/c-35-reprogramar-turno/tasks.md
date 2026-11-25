# Tasks

**Reglas para todas las tareas:**
- TDD estricto en cada tarea: primero el test que falla, después el código mínimo y por último el refactor.
- Solo datos ficticios.
- Cuando un test cubra un escenario de la spec, su `@DisplayName` lleva el nombre exacto del escenario.
- En los tests de integración, usar `csrfValido()` y `csrfConCabeceraIncorrecta()`, nunca `.with(csrf())`.
- Un commit por etapa. El líder autorizó hacer todas las etapas seguidas.

## 1. Dominio

- [x] 1.1 Agregar `Intervalo.menos(otro)` (design D1). Se verifica con JUnit:
  - sin contacto;
  - contiguo;
  - cubierto;
  - corrimiento a la derecha y a la izquierda;
  - recorte en el medio (dos tramos).
- [x] 1.2 Agregar `Turno.exigirReprogramable`, `Turno.reprogramar` y `Turno.mismoHorario` (design D2). Se verifica con JUnit:
  - estados finales → `409`;
  - inicio igual a ahora → `422 RN-05`;
  - tardía con el plazo exacto y un minuto después;
  - la marca se conserva;
  - se conservan `id`, estado, notas y versión.
- [x] 1.3 Agregar `TurnoReprogramado.de` y `HuecoLiberado.porReprogramacion` (design D3). Se verifica con JUnit:
  - "Moverse dentro del propio intervalo libera solo lo que dejó";
  - "Cambiar de profesional libera el intervalo completo";
  - sin hueco si el nuevo cubre al anterior.

## 2. Servicio y API

- [x] 2.1 Extraer `ConsultaRecursosTurno` de `CrearTurnoService` (design D5). La suite de C-19 sigue en verde.
- [x] 2.2 Crear `ReprogramarTurnoService`, `SolicitudReprogramarTurno` y `POST /api/turnos/{id}/reprogramar`, y sumar `reprogramadoTarde` a `RespuestaTurno` (design D4 y D6). Se verifica con integración:
  - "Recepción mueve un turno a otro horario";
  - "Se puede cambiar de profesional, de sillón y de duración";
  - "Un turno confirmado sigue confirmado";
  - "Reprogramar al mismo horario no cambia nada";
  - "Un pedido sin inicio se rechaza";
  - "Un turno inexistente no se puede reprogramar".
- [x] 2.3 Cubrir estado y validaciones del horario:
  - "Un turno cancelado no se puede reprogramar";
  - "Un turno que ya empezó no se puede reprogramar";
  - "Un turno se mueve quince minutos dentro de su propio intervalo";
  - "Un horario ocupado por otro turno responde conflicto";
  - "Un horario fuera de atención se rechaza";
  - "El horario nuevo respeta el inicio futuro, la grilla y los recursos activos".
- [x] 2.4 Cubrir la tardía:
  - "Reprogramar dentro del plazo queda marcado como tardío";
  - "Reprogramar a 24 horas exactas no es tardío";
  - "La marca de tardía se conserva".
- [x] 2.5 Cubrir la liberación y los eventos con `@RecordApplicationEvents`:
  - "El horario anterior se puede volver a dar";
  - "Reprogramar publica el turno reprogramado y el hueco anterior";
  - "Una reprogramación rechazada no publica eventos".

## 3. Concurrencia y acceso

- [x] 3.1 Escribir "Una reprogramación y un alta compiten por el mismo horario" (design D7): dos hilos con un `CountDownLatch` y `@RepeatedTest(5)`.
- [x] 3.2 Agregar el matcher en `SecurityConfig` y `@PreAuthorize`. Se verifica con:
  - "El odontólogo no puede reprogramar turnos";
  - "Sin sesión no se puede reprogramar un turno";
  - "Sin CSRF válido no se puede reprogramar un turno";
  - un test que llama al servicio como odontólogo y espera `AccessDeniedException`.

## 4. Verificación final

- [ ] 4.1 Actualizar PA-06 en la KB y la nota de `CLAUDE.md` y `AGENTS.md` (design D8).
- [ ] 4.2 Correr la suite completa del backend (`./mvnw verify`): todo en verde.
- [ ] 4.3 Escribir `verify.md` con el veredicto y la trazabilidad entre escenarios y tests.

## Workflow follow-up

- Abrir el PR de `juan-pablo-c-35` a `master` con "Closes #35", esperar el CI en verde y mergear con `--admin --merge`.
- Después del merge:
  - correr `/opsx:archive`;
  - en CHANGES.md, marcar C-35 hecho y a nombre de Juan Pablo en todos los lugares, incluida la carga por integrante;
  - en el issue #35, título con "— Juan Pablo" y asignado.

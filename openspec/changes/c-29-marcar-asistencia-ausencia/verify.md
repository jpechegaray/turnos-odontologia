# Verify — c-29-marcar-asistencia-ausencia (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

`openspec validate c-29-marcar-asistencia-ausencia --strict` → `Change 'c-29-marcar-asistencia-ausencia' is valid`. `openspec status` informa los cuatro artefactos (`proposal`, `specs`, `design`, `tasks`) en `done`.

## Tareas

7/8 completas al momento de correr la verificación (hoy 8/8 según `openspec instructions apply`). La única pendiente era la 4.1, que es esta misma verificación; se marca al cerrar este informe porque todos los comandos dieron verde.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/agenda/`. La línea indicada es la del `@DisplayName`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| agenda-turnos | Recepción marca un turno como atendido | Cubierto | `web/TurnosAsistenciaApiIT.java:103` | `200`, `$.estado = ATENDIDO` y estado leído de la base. Dominio: `domain/TurnoTest.java:186` (desde PROGRAMADO y CONFIRMADO). |
| agenda-turnos | Recepción marca la ausencia de un turno confirmado | Cubierto | `web/TurnosAsistenciaApiIT.java:116` | `200`, `AUSENTE` en respuesta y en la base. Dominio: `domain/TurnoTest.java:198`. |
| agenda-turnos | A la hora exacta de inicio ya se puede marcar | Cubierto | `web/TurnosAsistenciaApiIT.java:128`, `domain/TurnoTest.java:206` | Con `RelojFijoConfig`, inicio = ahora. |
| agenda-turnos | Antes del inicio no se puede marcar | Cubierto | `web/TurnosAsistenciaApiIT.java:140`, `domain/TurnoTest.java:214` | `422`, código `RN-20`, el turno sigue `PROGRAMADO`. |
| agenda-turnos | Un estado destino inválido se rechaza | Cubierto | `web/TurnosAsistenciaApiIT.java:154` | Parametrizado: `CANCELADO`, `CONFIRMADO`, `PROGRAMADO`, `{}` y `null` → `400`, un solo error con `campo = estado`, turno sin cambios. Extra: `:167` (enum inexistente o JSON ilegible → `400`, no `500`). |
| agenda-turnos | Un turno inexistente no admite asistencia | Cubierto | `web/TurnosAsistenciaApiIT.java:179` | `404 TURNO_INEXISTENTE`. |
| agenda-turnos | Un turno cancelado no se marca | Cubierto | `web/TurnosAsistenciaApiIT.java:189`, `domain/TurnoTest.java:227` | `409 TURNO_TRANSICION_INVALIDA` para ADMIN y también para RECEPCION; sigue `CANCELADO`. |
| agenda-turnos | El administrador corrige una marca | Cubierto | `web/TurnosAsistenciaApiIT.java:205`, `domain/TurnoTest.java:264` | `AUSENTE → ATENDIDO` con `200` y estado en la base. |
| agenda-turnos | Recepción no puede corregir una marca | Cubierto | `web/TurnosAsistenciaApiIT.java:217` | `403` (también pidiendo la misma marca); sigue `ATENDIDO`. |
| agenda-turnos | Repetir la misma marca se rechaza | Cubierto | `web/TurnosAsistenciaApiIT.java:229`, `domain/TurnoTest.java:274` | `409 TURNO_TRANSICION_INVALIDA`, turno sin cambios. |
| agenda-turnos | La corrección no cambia lo demás | Cubierto | `domain/TurnoTest.java:302` | Comparación recursiva ignorando `estado`; el turno de prueba tiene recursos, intervalo y notas ("Control ficticio"). Solo a nivel de dominio. |
| agenda-turnos | El odontólogo no puede marcar asistencia | Cubierto | `web/TurnosAsistenciaApiIT.java:243` | `403` también con cuerpo inválido (antes de validar); sigue `PROGRAMADO`. Defensa en profundidad: `:256` llama al servicio como odontólogo y espera `AccessDeniedException`. |
| agenda-turnos | Sin sesión no se puede marcar asistencia | Cubierto | `web/TurnosAsistenciaApiIT.java:272` | `401`; turno sin cambios. |
| agenda-turnos | Sin CSRF válido no se puede marcar asistencia | Cubierto | `web/TurnosAsistenciaApiIT.java:283` | `csrfConCabeceraIncorrecta()` → `403`. |
| agenda-turnos (MODIFIED) | Transiciones permitidas desde un turno programado | Cubierto | `domain/TurnoTest.java:43` | Heredado de C-19; parametrizado sobre los cuatro destinos. |
| agenda-turnos (MODIFIED) | Un estado final no cambia | Cubierto | `domain/TurnoTest.java:70` | Heredado de C-19; cada estado final contra todos los destinos → `409 TURNO_TRANSICION_INVALIDA`. |
| agenda-turnos (MODIFIED) | La corrección de asistencia no abre otras transiciones | Cubierto | `domain/TurnoTest.java:313` | Turno corregido: `cancelar` y `pasarA(CONFIRMADO)` → `409`. |

Tarea 2.3 (sin escenario en la spec): `web/TurnosAsistenciaConcurrenciaIT.java:74`, `@RepeatedTest(5)`, "Dos marcas de asistencia simultáneas del mismo turno se serializan": nunca `500`, combinación de códigos válida y estado final `ATENDIDO` o `AUSENTE`.

Resumen: 17/17 escenarios cubiertos, 0 parciales, 0 sin test.

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `backend/: ./mvnw -B verify` (Docker activo, Testcontainers) | BUILD SUCCESS en 1:53 min. Surefire: 265 tests, 0 fallas, 0 errores, 0 saltados. Failsafe: 498 tests, 0 fallas, 0 errores, 0 saltados. Del change: `TurnoTest` 45/45, `TurnosAsistenciaApiIT` 19/19, `TurnosAsistenciaConcurrenciaIT` 5/5. |
| `frontend/: npm run lint` | OK (exit 0, sin warnings). |
| `frontend/: npm test -- --run` | 34 archivos, 290 tests pasados. Solo los avisos de *future flags* de React Router v7 en `App.test.tsx`, ajenos a este change. |
| `frontend/: npm run build` | OK (`tsc -b && vite build`, 139 módulos). |

Ningún test fallido.

## Prueba manual contra la app (2026-11-25)

Se probó el endpoint con curl contra el backend levantado en `localhost:8080` (perfil `dev`, PostgreSQL de `docker compose`), con sesiones reales de login.

**Datos:** ficticios, cargados por psql en la base de desarrollo porque la API no admite turnos con inicio pasado (RN-13): usuarios `admin.c29@`, `recepcion.c29@` y `odontologo.c29.N@consultorio.test`, un paciente, un profesional, un sillón, una prestación y cinco turnos (pasado `PROGRAMADO`, pasado `CONFIRMADO`, futuro `PROGRAMADO`, pasado `CANCELADO` y uno para el odontólogo).

| Caso | Sesión | Esperado | Obtenido |
|---|---|---|---|
| Antes del inicio no se puede marcar | recepción | `422` | `422 RN-20` |
| Estado destino inválido (`CANCELADO`) | recepción | `400` | `400`, `campo = estado` |
| Cuerpo sin estado | recepción | `400` | `400`, `campo = estado` |
| El odontólogo no puede marcar asistencia | odontólogo | `403` | `403` |
| Sin sesión no se puede marcar asistencia | ninguna | `401` | `401` |
| Turno inexistente | recepción | `404` | `404 TURNO_INEXISTENTE` |
| Turno cancelado no se marca | admin | `409` | `409 TURNO_TRANSICION_INVALIDA` |
| Recepción marca un turno como atendido | recepción | `200` | `200`, `ATENDIDO` |
| Recepción marca la ausencia de un turno confirmado | recepción | `200` | `200`, `AUSENTE` |
| Recepción no puede corregir una marca | recepción | `403` | `403` |
| Repetir la misma marca se rechaza | admin | `409` | `409 TURNO_TRANSICION_INVALIDA` |
| El administrador corrige una marca (`ATENDIDO → AUSENTE`) | admin | `200` | `200`, `AUSENTE` |

**Resultado:** 12/12 OK. Al final, la base muestra cada turno en el estado esperado: el pasado queda `AUSENTE` tras la corrección, el confirmado `AUSENTE`, y el futuro, el cancelado y el del odontólogo sin cambios.

**Nota:** en una primera corrida los primeros POST de cada sesión dieron `403`, porque el script reutilizaba el token CSRF previo al login, y el login lo rota (`CsrfAuthenticationStrategy`). No es un defecto: el front vuelve a pedir `GET /api/auth/csrf`. Con ese paso agregado, una corrida limpia dio 12/12.

**Para repetirla:** colección de Postman en `pruebas-manuales/C-29.postman_collection.json`. Hay que completar la variable `clave` y cargar turnos nuevos, porque cada turno se consume al marcarlo.

## Reglas de negocio y reglas duras

- **RN-20** (marcar desde el inicio): borde exacto (`ahora == inicio` acepta) y caso anterior (`422 RN-20`) probados en dominio y por API con reloj fijo.
- **RN-12** (estados finales): `EstadoTurno` no cambió; los tests heredados de C-19 siguen en verde y la excepción `ATENDIDO ↔ AUSENTE` está acotada a `corregirAsistencia`, con un test que confirma que no abre otras transiciones.
- **RN-33 / regla dura 4** (odontólogo sin escritura): matcher en `SecurityConfig` más `@PreAuthorize` en el servicio, ambos probados.
- **Concurrencia:** lock de fila (`buscarParaActualizar`) probado con dos hilos y sesiones distintas.
- **RN-01/RN-02 y EXCLUDE:** no aplican; el change no crea ni mueve turnos.
- **Datos ficticios:** los tests usan `DatosAgenda` y textos como "Control ficticio" / "odontologo-ficticio". Sin datos reales.
- **Secretos:** el diff del change no toca `.env`, `application*.yml` ni agrega credenciales.
- **Regla 11:** todos los escenarios tienen un `@DisplayName` con el nombre exacto.

Ninguna regla dura violada.

## Diferencias con design.md

- Sin diferencias **bloqueantes** ni **mayores**. D1 (dos métodos en `Turno`, `EstadoTurno` intacto), D2 (servicio con `@PreAuthorize`, `@Transactional`, lock de fila, rama de corrección por estado `ATENDIDO`/`AUSENTE` y no por `esFinal()`), D3 (códigos), D4 (endpoint, DTO con `@NotNull` + `@MarcaDeAsistencia` sobre el componente, matcher) y D5 (tests) coinciden con el código.
- **Menor (documentación):** `proposal.md` dice que "la falta de sesión o de CSRF reciben `403`", pero la spec y el test esperan `401` sin sesión (y `403` sin CSRF). El código sigue a la spec; conviene corregir esa línea del proposal. **Resuelto (2026-11-25):** el proposal ya dice `401` sin sesión y `403` con CSRF inválido.
- **Menor:** "La corrección no cambia lo demás" se prueba solo en el dominio, no por API. `corregirAsistencia` copia todos los campos y el servicio guarda ese record, así que el riesgo es bajo.
- **Menor:** el test de concurrencia acepta varias combinaciones de códigos (`200/200`, `403/200`, `200/409`, `409/200`); `200/409` no debería ocurrir con el lock, pero aceptarla no debilita la garantía de "sin `500` y estado final válido".

UI: no aplica (`Decision: no (no UI)` en `design.md`).

## Qué NO se pudo verificar

- No hay verificación visual: el change no tiene frontend (C-46). El endpoint sí se probó a mano por API (ver "Prueba manual contra la app").
- El test de concurrencia muestra que las marcas se serializan en 5 repeticiones; no prueba todas las intercalaciones posibles.

## Próximo paso

Abrir el PR "C-29 marcar-asistencia-ausencia" a `master` para que lo revise Juan Pablo (las diferencias menores quedan a revisar en el PR) y, después del merge, `/opsx:archive c-29-marcar-asistencia-ausencia`.

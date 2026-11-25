# Verify — c-32-token-turno-emision-y-validacion (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes)

## Validación estructural

`openspec validate c-32-token-turno-emision-y-validacion --strict`: válido. `openspec status --change c-32-token-turno-emision-y-validacion --json`: 4/4 artefactos. `git diff --check`: sin errores.

## Tareas

7/7 marcadas completas; sin pendientes en `tasks.md`. El apply correctivo reforzó las dos pruebas observadas en la tarea 3.1. Esta reverificación no modificó el código, los tests ni las casillas.

La aprobación previa para implementar fue confirmada en la conversación; no se inspeccionó evidencia escrita en un PR.

## Matriz escenario → test

Los archivos de la tabla están en `backend/src/test/java/ar/edu/utn/turnos/reservapublica/token/`. Los 12 escenarios tienen un `@DisplayName` exacto.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| token-turno | La emisión entrega el secreto y guarda solo su hash | Cubierto | `ServicioTokenTurnoIT.java:51` | Comprueba valor emitido, asociación, hash, vencimiento, creación y marcas nulas. |
| token-turno | No se emite para un turno inexistente, cancelado o iniciado | Cubierto | `ServicioTokenTurnoIT.java:68` | Cuatro variantes sin resultado ni fila persistida. |
| token-turno | PostgreSQL no admite un hash repetido ni un turno inexistente | Cubierto | `TokenTurnoTablaIT.java:55`, `:67` | PostgreSQL rechaza por la restricción esperada en ambos casos. |
| token-turno | Un token válido identifica solo su turno y registra el uso | Cubierto | `ServicioTokenTurnoIT.java:83` | Comprueba el ID, la marca exacta y el segundo uso. |
| token-turno | El token vence exactamente al inicio del turno | Cubierto | `ServicioTokenTurnoIT.java:96` | Antes, al inicio y después; los rechazos no cambian el último uso. |
| token-turno | El token del turno A no abre el turno B | Cubierto | `ServicioTokenTurnoIT.java:109` | Compara estado, código y detalle con el inexistente y comprueba que no se registra uso. |
| token-turno | Los rechazos de token son indistinguibles | Cubierto | `ServicioTokenTurnoIT.java:126` | Contrasta entradas inválidas, vencidas, revocadas y ajenas con el rechazo por inexistencia. |
| token-turno | El secreto no aparece en logs ni en errores del servicio | Cubierto | `ServicioTokenTurnoIT.java:153` | Captura la salida de emisión, éxito y rechazo; revisa también el mensaje y `toString()`. |
| token-turno | Cancelar revoca solo los tokens del turno cancelado | Cubierto | `CicloTokenTurnoIT.java:72` | Verifica ambas marcas, aislamiento e idempotencia. Para ambos tokens revocados compara estado `404`, código y detalle con un token inexistente. |
| token-turno | Reprogramar conserva el secreto y cambia el instante de expiración | Cubierto | `CicloTokenTurnoIT.java:102` | Al adelantar y al postergar, ambos secretos identifican el turno justo antes del nuevo inicio y se rechazan exactamente en ese instante; las filas ajenas o revocadas no cambian. |
| token-turno | Los eventos de turnos sin token no generan credenciales | Cubierto | `CicloTokenTurnoIT.java:132` | Ambos eventos se procesan sin error y dejan cero filas de token. |
| token-turno | Revertir la operación de agenda no cambia los tokens | Cubierto | `CicloTokenTurnoIT.java:143` | Rollback parametrizado para ambos eventos; el token conserva la vigencia y sigue validando. |

**Cobertura:** 12/12 cubiertos, 0 parciales, 0 sin test.

## Ejecución de tests

- Backend: `mvn -q verify` desde `backend/`, con Docker y PostgreSQL 16.16. Pasaron 226 pruebas unitarias y 469 de integración (21 de C-32), con 0 fallidas, 0 errores y 0 omitidas. JaCoCo: 48/48 líneas y 18/20 ramas en `reservapublica.token`.
- Frontend: `npx --no-install vitest run` desde `frontend/`: 34 archivos y 290 pruebas aprobadas, 0 fallidas, 0 omitidas. Solo advertencias de React Router.

## Reglas de negocio y reglas duras

- **RN-07:** la operación de validación devuelve solo el ID, rechaza un turno ajeno y no expone recursos de agenda ni pacientes. Se verificó el contrato interno, no la futura API HTTP.
- **RN-25:** se reutiliza C-14 para el secreto y su comparación; se verifica almacenamiento solo del hash, límite estricto con `Clock`, rechazo uniforme y ausencia del secreto en logs. La revocación y el recálculo se comprueban en base y al validar ambos tokens.
- El diff incluye cambios rastreados y archivos nuevos del árbol de trabajo. Las pruebas usan filas ficticias de `DatosAgenda` y secretos generados localmente; no hay datos de pacientes reales ni archivos de credenciales en los cambios. Las escrituras de C-32 usan consultas parametrizadas y no agregan rutas ni cambian el alta interna.
- C-32 no cambia los intervalos ni las restricciones `EXCLUDE` de C-19. La regresión de agenda incluye solapamiento real, borde contiguo y dos altas simultáneas sobre el mismo hueco en `TurnoTablaIT` y `TurnosConcurrenciaIT`.

## Diferencias con design.md

- No se encontraron contradicciones mayores. El puerto de agenda no expone entidades JPA, se conserva la relación 0..N y la FK se añade en el script idempotente, sin dependencias nuevas.
- Los escuchas están en `BEFORE_COMMIT`. Las pruebas en `CicloTokenTurnoIT.java:160` y `:174` comprueban que, con una espera de bloqueo observada en PostgreSQL, la validación no pisa la revocación y la emisión usa el inicio actualizado.

## Qué NO se pudo verificar

- Los productores reales de C-27 y C-35 no están en esta rama. Las pruebas publican los eventos reales desde transacciones de prueba que respetan el orden turno → token.
- Las rutas y la exposición de URLs en logs o en Problem Details quedan para C-39. La entrega del enlace y los recordatorios no son parte de C-32.
- No se ejecutó un arranque separado con el perfil `dev`; el esquema arrancó con PostgreSQL 16 y el perfil `test` en las pruebas de integración.

## Próximo paso

Solicitar autorización antes del commit, abrir un PR a `master` para revisión de Juan Pablo y, tras la integración, ejecutar `/opsx:archive c-32-token-turno-emision-y-validacion`.

# Verify — c-41-reserva-publica-crear-reserva (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (después del merge del PR)

## Validación estructural

`openspec validate c-41-reserva-publica-crear-reserva --strict` → `Change 'c-41-reserva-publica-crear-reserva' is valid`.

## Tareas

24 de 24 completas. No hay tareas diferidas.

## Matriz escenario → test

Los 64 escenarios de las tres specs delta tienen un test con `@DisplayName` o `it(...)` igual al nombre del escenario, y cada test afirma su THEN.

### reserva-publica (32)

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Una reserva válida responde 201 con el turno y el token | Cubierto | `ReservaOnlineApiIT.java:141` | Verifica `PROGRAMADO`, `ONLINE`, duración por defecto y sin creador |
| Una reserva sin email o sin obra social responde 400 | Cubierto | `ReservaOnlineApiIT.java:159` | Sin email, teléfono u obra social; nombre en blanco; email inválido; JSON mal formado |
| La reserva no pide ni guarda DNI | Cubierto | `ReservaOnlineApiIT.java:176` | |
| Reservar sin aceptar el tratamiento de datos responde 422 | Cubierto | `ReservaOnlineApiIT.java:186` | `false` y campo ausente |
| Reservar con una versión de consentimiento que no es la vigente responde 422 | Cubierto | `ReservaOnlineApiIT.java:196` | Versión vieja, vacía y nula |
| Reservar con una ficha nueva guarda el consentimiento en la ficha | Cubierto | `ReservaOnlineApiIT.java:207`, `AltaPacienteReservaOnlineTest.java:21` | |
| Reservar con una ficha existente guarda el consentimiento en el turno | Cubierto | `ReservaOnlineApiIT.java:221` | La ficha sigue sin consentimiento |
| Una reserva con una ficha existente no modifica la ficha | Cubierto | `ReservaOnlineApiIT.java:242`, `SeleccionFichaTest.java:53` | Compara la fila completa antes y después |
| Con varias fichas del mismo email se elige la que coincide en teléfono | Cubierto | `ReservaOnlineApiIT.java:260`, `SeleccionFichaTest.java:26` | |
| Con varias fichas del mismo email y ningún teléfono coincidente se elige la más antigua | Cubierto | `ReservaOnlineApiIT.java:272`, `SeleccionFichaTest.java:34` | Incluye una ficha con teléfono nulo |
| Un email nuevo crea una ficha con los datos de la reserva | Cubierto | `ReservaOnlineApiIT.java:284`, `SeleccionFichaTest.java:20` | Con una ficha dada de baja del mismo email |
| La respuesta no revela si la ficha ya existía | Cubierto | `ReservaOnlineApiIT.java:300` | |
| Se supera el tope de turnos activos por email | Cubierto | `ReservaOnlineApiIT.java:322` | Suma dos fichas del mismo email |
| Los turnos cancelados o pasados no cuentan para el tope | Cubierto | `ReservaOnlineApiIT.java:341`, `TurnosActivosPersistenciaIT.java:135` | |
| Dos reservas simultáneas del mismo email no superan el tope | Cubierto | `ReservaOnlineConcurrenciaIT.java:221` | Commit real; 201 y 422 con `limite = TOPE_EMAIL` |
| Un inicio antes de la anticipación mínima responde 422 | Cubierto | `ReservaOnlineApiIT.java:358` | |
| Un inicio después del horizonte responde 422 | Cubierto | `ReservaOnlineApiIT.java:367` | |
| Un horario ofrecido en el borde del horizonte se puede reservar | Cubierto | `ReservaOnlineApiIT.java:376` | Consulta la disponibilidad pública, reserva el último horario ofrecido y el siguiente da `HORIZONTE` |
| Reservar con un profesional inactivo responde el 404 uniforme | Cubierto | `ReservaOnlineApiIT.java:396` | Los tres casos |
| El sistema asigna el primer sillón activo libre | Cubierto | `ReservaOnlineApiIT.java:413` | |
| Un horario fuera de la atención del profesional responde 422 | Cubierto | `ReservaOnlineApiIT.java:430` | `RN-04` fuera de horario y en bloqueo, `RN-09` fuera de grilla (D3) |
| Dos reservas simultáneas del mismo hueco: una responde 201 y la otra 409 | Cubierto | `ReservaOnlineConcurrenciaIT.java:171` | Commit real; un único turno en el horario |
| Un hueco ocupado responde 409 con alternativas | Cubierto | `ReservaOnlineConcurrenciaIT.java:183` | 5 alternativas con solo `inicio` y `fin`, sin `recurso` |
| Un 409 sin horarios libres devuelve alternativas vacías | Cubierto | `ReservaOnlineConcurrenciaIT.java:205` | |
| El token de la respuesta da acceso al turno reservado | Cubierto | `ReservaOnlineApiIT.java:449` | También verifica que el token no se guarda en claro |
| El token de la reserva no aparece en los logs | Cubierto | `ReservaOnlineApiIT.java:461` | `OutputCaptureExtension`; tampoco aparece el email |
| La reserva envía el email de confirmación con el enlace | Cubierto | `ReservaOnlineConcurrenciaIT.java:266` | |
| Un fallo del email deja la reserva hecha y la notificación FALLIDA | Cubierto | `ReservaOnlineConcurrenciaIT.java:280` | Un intento y un próximo intento programado |
| El reintento de la confirmación lleva un enlace nuevo válido | Cubierto | `ReservaOnlineConcurrenciaIT.java:297` | El token original sigue válido |
| El reintento de un turno cancelado se descarta | Cubierto | `ReservaOnlineConcurrenciaIT.java:316` | |
| El consultorio expone los datos del aviso de consentimiento | Cubierto | `ReservaOnlineApiIT.java:471` | Exactamente cinco campos |
| Los datos del consultorio reflejan la configuración vigente | Cubierto | `ReservaOnlineApiIT.java:485` | |

### configuracion-parametros (14)

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| El primer arranque inicializa los siete valores por defecto | Cubierto | `ConfiguracionInicialIT.java:29` | El nombre se conserva por el validador; el cuerpo exige diez claves |
| Los parámetros nuevos del consultorio tienen su valor por defecto | Cubierto | `ConfiguracionInicialIT.java:50` | |
| La inicialización repone solo claves faltantes | Cubierto | `ConfiguracionInicialIT.java:78` | |
| Admin y Recepción consultan los parámetros vigentes | Cubierto | `ConfiguracionApiIT.java:56` | JSON estricto con diez claves |
| Odontólogo no puede consultar configuración | Cubierto | `ConfiguracionApiIT.java:64` | |
| Un cliente anónimo no puede consultar configuración | Cubierto | `ConfiguracionApiIT.java:72` | |
| Admin reemplaza toda la configuración con límites válidos | Cubierto | `ConfiguracionApiIT.java:82` | Incluye retención en 1 y 20 |
| Repetir el mismo reemplazo no cambia las fechas | Cubierto | `ConfiguracionApiIT.java:111` | |
| Dos reemplazos concurrentes no mezclan valores | Cubierto | `ConfiguracionReemplazoIT.java:113` | |
| Recepción y Odontólogo no pueden editar configuración | Cubierto | `ConfiguracionApiIT.java:122` | |
| Admin no puede editar configuración sin CSRF válido | Cubierto | `ConfiguracionApiIT.java:133` | |
| Un cuerpo que no coincide con el contrato devuelve 400 | Cubierto | `ConfiguracionApiIT.java:148` | |
| Un valor inválido devuelve RN-36 sin guardar los demás | Cubierto | `ConfiguracionApiIT.java:159` | Incluye retención en 0 y 21, y domicilio o email en blanco |
| El consumidor lee valores nuevos sin alterar su resultado anterior | Cubierto | `ConfiguracionServicioIT.java:27` | Diez lecturas tipadas |

### configuracion-frontend (18)

Todos cubiertos en `frontend/src/features/configuracion/Configuracion.test.tsx`, en las líneas 73, 85, 100, 111, 124, 137, 150, 163, 176, 190, 204, 230, 248, 266, 285, 297, 311 y 322. Los dos escenarios nuevos son "Un domicilio o un email de contacto en blanco bloquea el guardado sin llamar a la API" (176) y "Una retención fuera de rango bloquea el guardado sin llamar a la API" (190).

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `./mvnw verify` en `backend/` (JDK 21, Testcontainers con PostgreSQL 16) | Surefire: 367 ejecutados, 0 fallas, 0 errores, 0 saltados. Failsafe: 748 ejecutados, 0 fallas, 0 errores, 0 saltados. `BUILD SUCCESS` |
| `npx vitest run` en `frontend/` | 41 archivos y 371 tests ejecutados, todos verdes |
| `npx tsc -b --noEmit` y `npm run lint` en `frontend/` | Sin errores |

Durante el verify se completó la aserción de `limite = TOPE_EMAIL` en la carrera por el tope (`ReservaOnlineConcurrenciaIT.java:221`), que antes solo miraba el estado 422. Las corridas de esta tabla son posteriores a ese ajuste.

## Reglas de negocio y reglas duras

- **RN-01, RN-02 y RN-15:** la reserva usa el mismo `CrearTurnoService` que el personal, con los mismos advisory locks, `ValidadorTurno` y traducción del `EXCLUDE`. La carrera de dos reservas por el mismo hueco corre con commit real contra PostgreSQL y deja un único turno. Los casos de borde `[inicio, fin)` y el `EXCLUDE` directo ya los cubre C-19 (`TurnosPersistenciaIT`, `TurnosConcurrenciaIT`), que sigue en verde.
- **RN-08:** el 409 trae alternativas sin el recurso que chocó.
- **RN-25 y RN-07:** el token se emite con `ServicioTokenTurno`, se guarda solo hasheado y no aparece en los logs.
- **RN-26:** no se pide DNI, y el consentimiento es obligatorio con la versión vigente.
- **RN-27:** cubre anticipación, horizonte (con el borde de C-38), tope por email y carrera por el tope.
- **RN-28:** la ficha se elige sin distinguir mayúsculas ni espacios, por los dígitos del teléfono (un teléfono nulo o sin dígitos nunca coincide) o la más antigua, y nunca se modifica.
- **RN-31:** un fallo del email no afecta la reserva, y el reintento lleva un enlace nuevo.
- **RN-35:** la respuesta no incluye ids ni datos de la ficha, y las alternativas no traen sillón.
- **RN-36:** valida los tres parámetros nuevos.
- **Reglas duras:**
  - Solo hay datos ficticios: emails `@example.com`, `@*.test` y `@*.example`.
  - No se agregan secretos ni cambios en `application*.yml`.
  - La API no expone entidades JPA: solo DTOs `record` que ocultan el token y los datos personales en `toString`.
  - El JPQL es parametrizado; el lock es SQL nativo parametrizado, como el de C-19.
  - Se usan `Instant` y el `Clock` inyectable.
  - No hay Flyway, JWT ni H2.
  - No se agregan endpoints de escritura para `ODONTOLOGO`.
  - Todos los tests nuevos llevan `@DisplayName`.

## Diferencias con design.md

- **Bloqueantes:** ninguna.
- **Mayores:** ninguna. D3 se actualizó antes del apply por pedido del revisor y el código la sigue.
- **Menores:**
  - El email se envía con `TransactionSynchronization.afterCommit` si la reserva corre dentro de una transacción externa, y si no, apenas termina la `TransactionTemplate`. En producción no hay transacción externa, así que el comportamiento coincide con D2. Este detalle evita que los ITs con rollback dejen notificaciones.
  - `SolicitudReservaPublica` recorta los textos antes de Bean Validation, para que un email con espacios en los extremos encuentre la ficha (RN-28) en lugar de dar 400.
  - Se agregó el test "Dos reservas simultáneas de un email nuevo crean una sola ficha", que no estaba en tasks, para cubrir el lock de D4 en la deduplicación.
- **UI:** la política es `system-only` y `design.md` registra `Decision: no (project policy)`. Los tres campos nuevos usan los componentes existentes (`Campo`) sin estilos fijos.

## Qué NO se pudo verificar

- La pantalla de configuración no se probó en un navegador. Solo la cubren los tests de Vitest.
- El envío por SMTP real no se probó: la suite usa el adaptador en memoria.

## Próximo paso

1. Abrir el PR a `master` con este change y los commits de C-38 (archivo) y PA-08. Lo revisa Juan Pablo.
2. Después del merge, correr `/opsx:archive c-41-reserva-publica-crear-reserva` y completar la ficha de `CHANGES.md` con el número de PR.

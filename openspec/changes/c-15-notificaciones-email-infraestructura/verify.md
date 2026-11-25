# Verify — c-15-notificaciones-email-infraestructura (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

> Evidencia cruda de las tareas 6.1 y 6.2. La suite completa de backend está en verde
> (Surefire 141 + Failsafe 354, sin fallos ni errores), `openspec validate --strict` es válido,
> los 21 escenarios de la delta spec están cubiertos por tests y el escaneo de secretos no encontró
> credenciales reales. El informe formal de `openspec-verify` se hará después; este archivo solo
> registra la evidencia que piden las tareas 6.1 y 6.2.

## Alcance

- **Solo backend.** Archivos nuevos en `backend/src/main/java/ar/edu/utn/turnos/shared/notificacion/**`
  y `backend/src/test/java/ar/edu/utn/turnos/shared/notificacion/**`.
- Archivos modificados: `.env.example`, `backend/src/main/resources/application.yml`,
  `backend/src/main/resources/schema-constraints.sql`,
  `backend/src/test/resources/application-test.yml`.
- Delta spec: `specs/notificaciones-email/spec.md` (5 requirements, 21 escenarios).
- **Frontend NO se ejecutó** (`npm test` en `frontend/` no es requerido): C-15 es backend-only y no
  toca archivos de `frontend/`.
- No se cambió de rama, ni se hizo commit, push ni merge. Rama actual intacta:
  `julian-c-20-archivo`.

## 6.1 — Suite completa de backend

Comando (workdir = `backend`), con Docker 29.9.3 y Java 21 (Temurin 21.0.12.1) disponibles:

```
.\mvnw.cmd verify
```

Salida real resumida:

| Fase | Tests run | Failures | Errors | Skipped |
|---|---|---|---|---|
| Surefire (unit) | 141 | 0 | 0 | 0 |
| Failsafe (integration) | 354 | 0 | 0 | 0 |

- **BUILD SUCCESS** — Total time: 03:36 min (finished 2026-11-25T12:14:26-03:00).
- Los tests de notificación ejecutados en esta corrida:
  - Surefire: `PlantillaNotificacionTest` 3, `ServicioNotificacionesTest` 7,
    `ConfiguracionCorreoTest` 4, `EnumsNotificacionTest` 2, `NotificacionEnviadaTest` 5,
    `InMemoryNotificacionAdapterTest` 3, `PropiedadesNotificacionesTest` 4,
    `SmtpNotificacionAdapterTest` 4.
  - Failsafe: `ConfiguracionNotificacionesIT` 2, `NotificacionRepositorioIT` 5,
    `NotificacionRestriccionesIT` 2, `PlanificadorNotificacionesIT` 3.
  - Total notificación: **44 tests, 0 fallos**.
- **Capa de dominio sin Spring/JPA:** `ArquitecturaTest` 1/1 en verde y `ArquitecturaTestsTest` 1/1
  en verde (ambos en Surefire).
- Nota: en el log de Surefire aparece un stack trace de
  `ManejadorErroresTest.errorInesperadoNoExponeDetallesInternos` (línea 135). Es un `RuntimeException`
  lanzado **a propósito** por ese test para verificar que el manejador de errores no filtra detalles;
  no es una falla (la fase reporta 0 failures / 0 errors).

## 6.2 — Validación estructural y escaneo de secretos

### Validación OpenSpec

```
openspec validate "c-15-notificaciones-email-infraestructura" --strict
→ Change 'c-15-notificaciones-email-infraestructura' is valid
```

### Escaneo de secretos

Qué se buscó: credenciales SMTP reales (host/usuario/contraseña) en todos los archivos versionados
del change y en el árbol de trabajo, con `git grep` sobre archivos rastreados y `Select-String`
sobre el paquete nuevo de notificaciones.

| Archivo | Contenido sensible | Resultado |
|---|---|---|
| `.env.example` | `SMTP_HOST=`, `SMTP_USER=usuario-smtp`, `SMTP_PASSWORD=cambiar-en-env-local`, `SMTP_FROM=turnos@consultorio.test`, `NOTIFICACIONES_CANAL=smtp` | Solo placeholders y marcadores de ejemplo. Aceptable. |
| `application.yml` | `host: ${SMTP_HOST:}`, `username: ${SMTP_USER:}`, `password: ${SMTP_PASSWORD:}`, `remitente: ${SMTP_FROM:turnos@consultorio.test}` | Referencias a variables de entorno, sin valores. Aceptable. |
| `application-test.yml` | `canal: memoria`, `planificador.habilitado: false` | Sin credenciales. |
| `ConfiguracionNotificaciones.java` | `@Value("${spring.mail.password:}")` | Inyección por entorno, sin literal. |
| `SmtpNotificacionAdapter.java` | `exigirCredencial(password, "SMTP_PASSWORD")` | Valida que exista; nunca imprime el valor. |
| Tests (`SmtpNotificacionAdapterTest`, etc.) | `"usuario-smtp"`, `"secreto-en-entorno"`, `"secreto-super"` | Fixtures ficticios, no credenciales reales. |
| `schema-constraints.sql` | — | Sin credenciales. |

- `.env` está **git-ignored** y **no está trackeado**:
  - `git check-ignore .env` → `.env` (ignorado).
  - `git ls-files --error-unmatch .env` → *pathspec did not match* (no versionado).
- **Resultado:** no se encontró ninguna credencial SMTP real versionada. Solo marcadores,
  `${SMTP_*}` y fixtures ficticios.

### Restricciones SQL

Bloques anexados al final de `schema-constraints.sql`, cada uno separado por `^^^` e idempotente
con `DO $$ ... $$` (verifican `pg_constraint`/`IF NOT EXISTS` antes de crear):

- `uk_notificacion_turno_tipo_inicio` → `UNIQUE (turno_id, tipo, inicio_referencia)`.
- `uk_notificacion_oferta_tipo` → `UNIQUE (oferta_hueco_id, tipo)`.
- `ix_notificacion_estado_programada` → `CREATE INDEX IF NOT EXISTS ... (estado, programada_para)`.

`git diff -- schema-constraints.sql` muestra **solo líneas agregadas al final**; los bloques
existentes de C-07/C-10/C-11/C-12 no se tocaron. El total de separadores `^^^` es 11 (8 previos + 3
nuevos). `NotificacionRestriccionesIT` (2/2 en verde) comprueba en PostgreSQL real que las tres
existen y que el script es idempotente.

### `application.yml`

`git diff -- application.yml` confirma que **solo se completó el bloque reservado**: se agregó
`spring.mail` y `app.notificaciones` (canal, remitente, planificador, reintento). No se modificó
ninguna otra sección.

## Matriz escenario → test (21/21)

| # | Requirement | Escenario | Test (archivo:línea) | Resultado |
|---|---|---|---|---|
| 1 | Registro del resultado | Un envío exitoso queda ENVIADA con la fecha de envío | `NotificacionEnviadaTest.java:31` | Pasa |
| 2 | Registro del resultado | Un fallo de SMTP deja la notificación FALLIDA y conserva el turno | `ServicioNotificacionesTest.java:102` | Pasa |
| 3 | Registro del resultado | Un paciente sin email no genera notificación ni error | `ServicioNotificacionesTest.java:90` | Pasa |
| 4 | Registro del resultado | La notificación usa el nombre del consultorio y no expone otros pacientes | `PlantillaNotificacionTest.java:30` | Pasa |
| 5 | Idempotencia | No se reenvía un aviso que ya tiene registro | `ServicioNotificacionesTest.java:78` | Pasa |
| 6 | Idempotencia | Dos inserciones iguales violan la restricción única | `NotificacionRepositorioIT.java:99` | Pasa |
| 7 | Idempotencia | Un turno reprogramado genera un nuevo recordatorio | `NotificacionRepositorioIT.java:110` | Pasa |
| 8 | Idempotencia | Los avisos de oferta no colisionan entre sí ni con los de turno | `NotificacionRepositorioIT.java:125` | Pasa |
| 9 | Reintentos | Una notificación fallida se reintenta hasta tres veces con espera creciente | `ServicioNotificacionesTest.java:118` (+ `NotificacionEnviadaTest.java:59`) | Pasa |
| 10 | Reintentos | Una notificación fallida no se reintenta después del tercer intento | `NotificacionEnviadaTest.java:70` | Pasa |
| 11 | Reintentos | El próximo intento se calcula con el reloj inyectable | `NotificacionEnviadaTest.java:42` | Pasa |
| 12 | Reintentos | Los reintentos de un aviso de oferta cesan al comenzar el hueco | `ServicioNotificacionesTest.java:187` | Pasa |
| 13 | Planificador | El planificador envía las notificaciones pendientes vencidas | `ServicioNotificacionesTest.java:148` | Pasa |
| 14 | Planificador | El planificador reintenta las fallidas cuyo próximo intento venció | `PlanificadorNotificacionesIT.java:78` | Pasa |
| 15 | Planificador | El planificador no envía notificaciones programadas a futuro | `ServicioNotificacionesTest.java:168` | Pasa |
| 16 | Planificador | Dos ejecuciones concurrentes del planificador no duplican un envío | `PlanificadorNotificacionesIT.java:92` | Pasa |
| 17 | Planificador | Un reinicio del servicio retoma las pendientes en el siguiente ciclo | `PlanificadorNotificacionesIT.java:130` | Pasa (ver desvío 1) |
| 18 | Configuración por entorno | Las credenciales SMTP se toman solo del entorno | `SmtpNotificacionAdapterTest.java:38` + `ConfiguracionCorreoTest.java:28` | Pasa |
| 19 | Configuración por entorno | El arranque falla si falta una credencial SMTP obligatoria | `SmtpNotificacionAdapterTest.java:55` | Pasa |
| 20 | Configuración por entorno | Las pruebas usan el adaptador en memoria | `ConfiguracionNotificacionesIT.java:29` + `InMemoryNotificacionAdapterTest.java:23` + `ConfiguracionCorreoTest.java:55` | Pasa |
| 21 | Configuración por entorno | El repositorio no contiene credenciales SMTP | `ConfiguracionCorreoTest.java:64` + escaneo `git grep` de §6.2 | Pasa |

**Resultado:** 21/21 escenarios cubiertos y en verde. Cada `@DisplayName` usa el texto exacto del
escenario. El escenario 21 combina el test automatizado (verifica que `.env.example` solo tenga el
placeholder) con el escaneo manual del repositorio.

## Checklist de tasks.md

- [x] 1.1 Enums, `MensajeNotificacion`, `ErrorEnvioNotificacion` y `NotificacionPort`; dominio sin Spring/JPA.
- [x] 1.2 Record `NotificacionEnviada` con transiciones y validación TDD.
- [x] 1.3 Javadoc de contrato del puerto y los estados.
- [x] 2.1 `NotificacionEnviadaJpa`, repositorio y adaptador.
- [x] 2.2 Bloques `DO $$` idempotentes de uniques e índice en `schema-constraints.sql`.
- [x] 2.3 Idempotencia cubierta en `NotificacionRepositorioIT`.
- [x] 3.1 Bloque `spring.mail` / `app.notificaciones` y `application-test.yml`.
- [x] 3.2 `PropiedadesNotificaciones` y `ConfiguracionNotificaciones` con beans condicionales.
- [x] 3.3 `InMemoryNotificacionAdapter` y `SmtpNotificacionAdapter`.
- [x] 4.1 `PlantillaNotificacion` en texto plano.
- [x] 4.2 `ServicioNotificaciones.registrarYEnviar(...)`.
- [x] 4.3 Manejo de resultado del envío con `Clock`.
- [x] 5.1 `PlanificadorNotificaciones` + `procesarVencidas()` con `FOR UPDATE SKIP LOCKED`.
- [x] 5.2 Bordes del planificador (reintentos, concurrencia, reinicio).
- [x] 5.3 Corte de avisos de oferta cuando `inicio_referencia <= ahora`.
- [x] 6.1 `.\mvnw.cmd verify` completo en verde.
- [x] 6.2 `openspec validate --strict` + escaneo de secretos documentado.

## Desvíos respecto de design.md

1. **Escenario "reinicio".** Se simula con una instancia nueva de `ServicioNotificaciones` sobre las
   mismas filas persistidas, no con un segundo contexto Spring. *Impacto:* valida el contrato de
   persistencia (retoma lo guardado sin crear filas nuevas); no ejercita un reciclado real del
   contenedor, que es un detalle de infraestructura.
2. **`catch (RuntimeException)`.** El registro/transacción captura `RuntimeException` (más amplio que
   solo `ErrorEnvioNotificacion`) para garantizar que el turno nunca se pierda ante cualquier fallo
   inesperado. *Impacto:* más robusto; el error se persiste en `ultimo_error`.
3. **`procesarVencidas()` con `TransactionTemplate` (REQUIRED)** en lugar de `@Transactional`.
   *Impacto:* control explícito del límite transaccional del bloqueo de fila; mismo comportamiento.
4. **Reintentos con plantilla reducida.** Los reintentos que arma el planificador solo tienen campos
   persistidos (`destinatario`, `inicioReferencia`); nombre/prestación/profesional/enlace no están
   disponibles en el reintento. *Impacto:* el cuerpo del reintento es más escueto que el envío
   original; aceptable para el MVP.
5. **Paquete `shared.notificacion`** en lugar del módulo `notificaciones` de la KB 08. *Impacto:*
   desviación deliberada para evitar ciclos de dependencia; el contrato no cambia. Pendiente de
   aprobación del equipo en la revisión HIGH.
6. **FKs a `turno`/`oferta_hueco` diferidas** a C-19/C-36 (las tablas aún no existen). *Impacto:*
   `turno_id`/`oferta_hueco_id` quedan `bigint` nullable sin FK; se agregan en el change que cree cada
   tabla.

## Riesgos residuales

- **IT de concurrencia sensible al timing.** `PlanificadorNotificacionesIT` ("dos ejecuciones
  concurrentes") depende del bloqueo `FOR UPDATE SKIP LOCKED` y de la sincronización de dos hilos;
  podría volverse intermitente en una máquina muy cargada. Mitigación: correrlo en CI con recursos
  suficientes; el diseño garantiza la corrección por la base, no por el timing del test.
- **Reintentos con plantilla reducida** (desvío 4): un reintento puede enviar un cuerpo menos rico
  que el primer envío. Aceptado en el MVP; se resolverá cuando los consumidores (C-25/C-36/C-37/C-41)
  persistan los datos de plantilla o se adopte un outbox.
- **`schema-constraints.sql` compartido.** Varios changes anexan bloques al mismo archivo; un merge
  puede producir un conflicto **textual** en la cola del archivo. Mitigación: los bloques son
  independientes y separados por `^^^`; resolver el conflicto conservando ambos bloques. Avisar en el PR.

## Veredicto

**LISTO PARA ARCHIVAR.** Justificación: las tareas 1.1–6.2 están completas y verificadas; la suite
completa de backend está en verde (Surefire 141 + Failsafe 354, 0 fallos / 0 errores, BUILD SUCCESS);
los 21 escenarios de la delta spec tienen test con el `@DisplayName` exacto y pasan;
`ArquitecturaTest` y `ArquitecturaTestsTest` pasan (dominio sin Spring/JPA); `openspec validate
--strict` es válido; y el escaneo de secretos no encontró credenciales reales (`.env` ignorado y no
versionado). Los desvíos y riesgos quedan documentados y no bloquean el archivado.

# Design

## Context

- `agenda` es un módulo nuevo. Lee a `profesionales`, `prestaciones` y `pacientes` solo a través de puertos que ya existen (KB 08):
  - `ProfesionalConsulta`, `SillonConsulta`, `ConsultaPrestaciones` y `PacienteConsulta` devuelven los datos con el flag de activo.
  - `ProfesionalRealizaPrestacion.realiza(profesionalId, prestacionId)` cubre la relación profesional-prestación de RN-11.
  - `HorarioConsulta.contiene(profesionalId, Intervalo)` pasa el intervalo a hora de Buenos Aires con `FranjaLocal`, rechaza los que cruzan la medianoche y exige que entre en un único tramo (RN-16).
  - `BloqueoConsulta.solapa(profesionalId, Intervalo)` usa la comparación semiabierta `inicio < :fin and fin > :inicio`.
- `shared/tiempo/Intervalo` ya existe como record `[inicio, fin)`, pero solo valida `fin > inicio`. Lo usan los dos puertos anteriores.
- `prestaciones/domain/DuracionPrestacion` ya aplica RN-09 a la duración (múltiplo de 5 entre 5 y 480). Responde `422` con código `RN-09`.
- Los errores de negocio se lanzan con `ReglaNegocioException(codigo, EstadoError, mensaje, propiedades)`. `ManejadorErrores` los traduce a Problem Details y copia `codigo` y las propiedades al cuerpo.
- C-18 dejó dos convenciones:
  - Un id inexistente o inactivo que llega en el cuerpo responde `422` con un código por recurso, por ejemplo `PACIENTE_INEXISTENTE` o `PACIENTE_INACTIVO`. El `404` queda para un recurso de la URL.
  - Una violación de índice se traduce con `saveAndFlush` y un `catch` de `DataIntegrityViolationException` que busca el nombre de la restricción en la cadena de causas (`ListaEsperaPersistencia`).
- El esquema lo genera Hibernate (`ddl-auto`). `schema-constraints.sql` corre después, con el separador `^^^`, y ya crea `btree_gist`. Cada `ALTER` va en un bloque `DO $$` que consulta `pg_constraint`, para que sea idempotente (DD-08).
- `ArquitecturaTest` prohíbe que `..domain..` dependa de Spring, JPA o Hibernate.
- El `Clock` inyectable (`ConfiguracionReloj`) es la única fuente de la hora actual (DD-05).

## Goals / Non-Goals

**Goals:**

- Que la regla `[inicio, fin)` viva en un solo lugar y se pruebe sin base de datos.
- Que las reglas de creación queden en una función pura, reutilizable por la reprogramación (C-35) y la reserva pública (C-41), sin copiar lógica.
- Que la base de datos sea la garantía final contra sobreturnos ante pedidos concurrentes, con el mismo `409` que la validación previa.
- Que `Turno` quede completa para que C-27, C-29 y C-35 no tengan que reabrir la entidad ni el script.

**Non-Goals:**

- No se escuchan eventos: el email (C-15), la lista de espera (C-36) y los tokens (C-32) se suscriben después.
- No se elige sillón automáticamente: recepción lo indica. La asignación automática es de la lista de espera (RN-24).
- No hay endpoints de lectura de turnos: la agenda es de C-28. La respuesta del `POST` alcanza para los tests.

## Decisions

### D1. `Intervalo` de `shared` ampliado con `solapaCon`, `contiene` y `desde`

Se agregan al record existente:

- `solapaCon(otro)`: `inicio < otro.fin && otro.inicio < fin`, con comparación estricta para cumplir RN-02;
- `contiene(otro)`: el otro intervalo cabe completo;
- `desde(inicio, minutos)`: fábrica que calcula `fin = inicio + duración` (RN-09).

- *Alternativa descartada:* crear `agenda.domain.Intervalo`, como dice KB 08. Habría dos records con el mismo nombre y casi la misma semántica, y el validador tendría que convertir entre ellos para llamar a `HorarioConsulta`. **Desviación de la KB aprobada por Juan Pablo (2026-11-25).**

### D2. `ValidadorTurno` puro en `agenda.domain`

Recibe la solicitud y los datos ya cargados: recursos con su estado, si el intervalo cae dentro del horario, si toca un bloqueo y los turnos activos que solapan. Recibe también un `turnoExcluidoId` opcional. No consulta la base ni depende de Spring. Usa el `Clock` para RN-13.

Aplica las reglas en el orden del Flujo 1 y corta en la primera que falla: RN-11, RN-13, RN-09, RN-16/RN-04 (horario), RN-04 (bloqueo) y RN-01. El orden va de lo más barato y definitivo a lo más caro. Además, garantiza que recepción vea el error más útil: un profesional inactivo no tiene que mostrarse como solapamiento.

`CrearTurnoService` (en `application`) usa los puertos, arma esos datos y llama al validador.

- *Alternativa descartada:* validar dentro del servicio con las consultas intercaladas. Es más difícil de testear: cada borde de RN-02 necesitaría PostgreSQL. Además, C-35 tendría que copiar la lógica.

### D3. RN-09: reutilizar `DuracionPrestacion` y validar el inicio en agenda

La duración efectiva, la enviada o la de la prestación, se construye como `DuracionPrestacion`, que ya responde `422 RN-09`. El validador agrega la condición sobre el inicio: minutos múltiplos de 5 y segundos y nanos en cero, con el mismo código `RN-09`. Así los números 5, 480 y 5 no se duplican.

### D4. Códigos de error

| Caso | Estado | `codigo` | Propiedades |
|---|---|---|---|
| Id de paciente, profesional, sillón o prestación inexistente | 422 | `PACIENTE_INEXISTENTE`, `PROFESIONAL_INEXISTENTE`, `SILLON_INEXISTENTE`, `PRESTACION_INEXISTENTE` | — |
| Recurso inactivo | 422 | `PACIENTE_INACTIVO`, `PROFESIONAL_INACTIVO`, `SILLON_INACTIVO`, `PRESTACION_INACTIVA` | — |
| El profesional no realiza la prestación | 422 | `RN-11` | — |
| Inicio en el pasado o igual al momento actual | 422 | `RN-13` | — |
| Inicio o duración fuera de la granularidad | 422 | `RN-09` | — |
| Fuera del horario de atención | 422 | `RN-04` | `motivo: FUERA_DE_HORARIO` |
| Dentro de un bloqueo | 422 | `RN-04` | `motivo: BLOQUEO` |
| Solapamiento | 409 | `RN-01` | `recurso: PROFESIONAL` o `SILLON` |

Los mensajes no incluyen datos personales. Si un turno choca por los dos ejes, se informa `PROFESIONAL`, porque se evalúa primero.

### D5. Doble barrera contra el solapamiento

1. **Consulta previa:** el repositorio busca los turnos no cancelados del profesional o del sillón con `t.inicio < :fin and t.fin > :inicio`. Si hay alguno, el validador responde `409 RN-01` con el eje del primero que choca.
2. **Restricciones de PostgreSQL** en `schema-constraints.sql`:
   - `turno_sin_solape_profesional`: `EXCLUDE USING gist (profesional_id WITH =, tstzrange(inicio, fin, '[)') WITH &&) WHERE (estado <> 'CANCELADO')`.
   - `turno_sin_solape_sillon`: igual, sobre `sillon_id`.

La persistencia guarda con `saveAndFlush`, para que el `INSERT` ocurra dentro del `try`. Ante una `DataIntegrityViolationException`, busca en la cadena de causas el nombre de la restricción y lanza el mismo `409 RN-01` con el `recurso` correspondiente. Cualquier otra violación se relanza.

- *Por qué las dos:* la consulta da el mensaje claro en el caso normal. Solo la restricción cierra la ventana entre consultar y guardar cuando dos pedidos llegan a la vez (RN-15).
- *Alternativa descartada:* `SELECT ... FOR UPDATE` o aislamiento `SERIALIZABLE`. No bloquean filas que todavía no existen y agregan reintentos. La restricción es declarativa y cubre también la reserva online (C-41).

### D6. Entidad `TurnoJpa` y dominio `Turno`

Se separan como en `listaespera`:

- `Turno` es el record de dominio, con `EstadoTurno` y su tabla de transiciones de RN-12: `PROGRAMADO → CONFIRMADO`, `PROGRAMADO|CONFIRMADO → CANCELADO|ATENDIDO|AUSENTE`. Los estados finales no salen. Una transición inválida responde `409 TURNO_TRANSICION_INVALIDA`. C-19 solo crea turnos `PROGRAMADO`, pero la tabla queda probada para C-27 y C-29.
- `TurnoJpa` mapea la tabla `turno` con las columnas del ERD:
  - tiempos y duración: `inicio`, `fin` y `creado_en`/`actualizado_en` como `timestamptz`, y `duracion_min`;
  - `estado` y `canal` como texto;
  - cancelación y reprogramación: `cancelacion_tardia`, `reprogramado_tarde`, `cancelado_en`, `cancelado_por` y `motivo_cancelacion`;
  - `notas` y `creado_por_usuario_id`;
  - `version`, con `@Version`.

  Los otros módulos se referencian solo por id (`Long`), como manda KB 08.
- Índices: `(profesional_id, inicio)`, `(sillon_id, inicio)`, `(paciente_id, inicio)` y `(estado, inicio)`.
- En `schema-constraints.sql`, en bloques idempotentes:
  - las FK a `pacientes`, `profesional`, `sillon`, `prestacion` y `usuario`;
  - `CHECK (fin > inicio)` y `CHECK (duracion_min > 0)`;
  - las dos `EXCLUDE`.

### D7. Eventos de dominio

`TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `HuecoLiberado` son records sin dependencias en `agenda.domain.evento`.

`CrearTurnoService` publica `TurnoCreado(turnoId, pacienteId, profesionalId, sillonId, intervalo)`, que arma `TurnoCreado.de(turno)` a partir del turno guardado, con `ApplicationEventPublisher`, después de guardar y dentro de la transacción. Los futuros oyentes usarán `@TransactionalEventListener` para actuar solo si la transacción se confirma. Hoy nadie escucha el evento.

### D8. API y seguridad

- `POST /api/turnos` recibe `{pacienteId, profesionalId, sillonId, prestacionId, inicio, duracionMin?, notas?}`:
  - `inicio` es un `OffsetDateTime` ISO-8601 con zona;
  - `duracionMin` es opcional;
  - `notas` admite hasta 1000 caracteres.
- Bean Validation responde `400` si falta un campo obligatorio o el formato es inválido. Los campos desconocidos se ignoran, como en el resto de la API: Jackson usa la configuración por defecto de Spring Boot.
- Responde `201` con `Location: /api/turnos/{id}` y el DTO `{id, pacienteId, profesionalId, sillonId, prestacionId, inicio, fin, duracionMin, estado, canal, notas, creadoEn}`. Nunca expone la entidad.
- El creador sale de `@AuthenticationPrincipal IdentidadPersonal`.
- Acceso:
  - `SecurityConfig` suma `POST /api/turnos` para `ADMIN` y `RECEPCION`.
  - El servicio repite `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")`, para que la regla dura 4 tenga dos barreras.

### D9. El caso de borde "10:29" se prueba en `Intervalo`, no en la API

La ficha de C-19 pide probar que "uno de 10:29 responde 409". Por la API eso no puede pasar: 10:29 no cae en la grilla de 5 minutos, así que RN-09 lo rechaza antes con `422`. Por eso el borde se prueba en dos niveles:

- en `Intervalo.solapaCon`, con 10:29 contra 10:00-10:30, sin la granularidad de por medio;
- en la API, con 10:25, el primer inicio válido que solapa.

Así se cubre la intención de la ficha sin contradecir RN-09.

### D10. Rama y PR

Se trabaja en `juan-pablo-c-19`, que sale de `master`, porque `juan-pablo` tiene pendiente el PR de archivo de C-14 y C-23. Va un change por PR (regla 15).

## Risks / Trade-offs

- **[Riesgo] Buscar el nombre de la restricción en el texto de la excepción depende del mensaje del driver** → Es el mismo mecanismo probado en `ListaEsperaPersistencia`. Un test de integración fuerza cada restricción por separado y verifica el `recurso`.
- **[Riesgo] El test de carrera puede ser intermitente** → Se coordinan los dos hilos con un `CountDownLatch`, para que lleguen juntos al guardado. Lo que se verifica es el invariante: exactamente un `201`, un `409` y una fila en la tabla, sin depender de cuál gana.
- **[Riesgo] Hibernate `update` no recrea la tabla si cambia el modelo en dev** → Está documentado en el README (`docker compose down -v`). En los tests se usa `create-drop`.
- **[Trade-off] Doble consulta, previa y restricción** → Cuesta una consulta extra por alta. A cambio, los mensajes son claros sin depender del texto del error de PostgreSQL en el caso normal.
- **[Trade-off] El `Intervalo` de `shared` contra KB 08** → Se documenta en D1. La regla queda centralizada.

## Migration Plan

No hay datos que migrar: la tabla `turno` es nueva. En dev, Hibernate la crea al arrancar y el script agrega las restricciones de forma idempotente. Para revertir, se quitan el paquete `agenda`, los bloques de `turno` del script y el matcher de `SecurityConfig`, y en dev se ejecuta `DROP TABLE turno`.

## UI Mockups

- Design system revision: no aplica.
- Decision: no (no UI) — 2026-11-25. Es un endpoint del backend, sin pantallas. El formulario de turnos es de C-45.
- Mockups: ninguno.
- Approval: no aplica.
- Preview: no aplica.
- Visual verify: no aplica.

# Design

## Context

- `listaespera` es un módulo nuevo; la KB (08) lo ubica como paquete de primer nivel, junto a `pacientes` y `prestaciones`.
- Los puertos que consume ya existen y no se modifican:
  - `PacienteConsulta.porId`: devuelve el `Paciente` con `email` y `activo`.
  - `ConsultaPrestaciones.buscarPorId`: devuelve `PrestacionResumen` con `activa`.
  - `ProfesionalConsulta.buscarPorId`: devuelve `Datos` con `activo`.
- Ya existen `ReglaNegocioException`, el manejador de Problem Details, el `Clock` inyectable en Buenos Aires (`ConfiguracionReloj`) y la convención de matchers de escritura en `SecurityConfig`.
- Todavía no hay ninguna tarea programada en el proyecto.
- Gotcha conocido de C-11: con `hibernate.jdbc.time_zone: UTC`, Hibernate desplaza los `LocalTime`.

## Goals / Non-Goals

**Goals:**

- Que la regla "una sola entrada activa por paciente y prestación" sea correcta también ante concurrencia.
- Que las transiciones de estado vivan en un dominio puro, testeable sin Spring y reutilizable por C-31, C-36 y C-42.
- Que el vencimiento dependa del `Clock`, para probarlo con una fecha fija.

**Non-Goals:**

- Calcular compatibilidad con huecos.
- Exponer la asignación en la API.
- Mostrar datos personales del paciente en la lista; la representación lleva solo IDs.

## Decisions

1. **Paquete `listaespera` con las capas `domain`, `application`, `infrastructure` y `web`**, como `pacientes`.
   - El dominio contiene el record de la entrada, el enum `EstadoListaEspera` y las transiciones. No depende de Spring ni de JPA, como exige `ArquitecturaTest`.
   - La aplicación contiene `ServicioListaEspera` y el planificador.
   - Se descartó meterlo dentro de `pacientes`: la KB lo separa porque va a reaccionar a eventos de `agenda`.

2. **Transiciones en el dominio.** `asignar(turnoId)`, `cancelar()` y `vencer()` devuelven una entrada nueva solo si el estado actual es `ACTIVA`. Si no, lanzan `ReglaNegocioException("ENTRADA_NO_ACTIVA", CONFLICTO_409, …)`. `asignar` exige un `turnoId` no nulo.
   - C-18 usa `cancelar` desde la API y `vencer` desde el planificador. `asignar` queda probado por tests de dominio para que C-31 y C-42 no reabran la entidad.
   - Se descartó una máquina de estados genérica: con cuatro estados y tres transiciones, un `switch` explícito es más legible.

3. **Duplicados garantizados por PostgreSQL.**
   - El servicio comprueba antes si existe otra entrada `ACTIVA` del mismo paciente y prestación, y responde `409 ENTRADA_ACTIVA_DUPLICADA` con un mensaje claro.
   - La garantía final es un índice único parcial: `CREATE UNIQUE INDEX IF NOT EXISTS ux_lista_espera_activa ON lista_espera (paciente_id, prestacion_id) WHERE estado = 'ACTIVA'`, en un bloque nuevo al final de `schema-constraints.sql`. Es idempotente y Hibernate no lo genera.
   - La persistencia hace `saveAndFlush` y traduce la violación de ese índice al mismo `409`.
   - Se descartó un bloqueo de fila, como en C-11: no hay una fila natural para bloquear (el paciente vive en otro módulo), y el índice cumple la regla aunque alguien escriba fuera del servicio.

4. **Validación de recursos con 422**, igual que C-10 para los IDs que vienen en el cuerpo. Los códigos son `PACIENTE_INEXISTENTE`, `PACIENTE_INACTIVO`, `PACIENTE_SIN_EMAIL`, `PRESTACION_INEXISTENTE`, `PRESTACION_INACTIVA`, `PROFESIONAL_INEXISTENTE` y `PROFESIONAL_INACTIVO`.
   - El orden es paciente, prestación, profesional y después duplicado. Así, un paciente sin email se informa antes que un duplicado.
   - No se valida que el profesional preferido realice la prestación: es parte de la compatibilidad (RN-22, C-31) y una preferencia no tiene por qué ser estricta.
   - Los mensajes no incluyen datos del paciente (RN-35).

5. **Representación y formato.**
   - Fechas en `AAAA-MM-DD`, guardadas como `LocalDate` en columnas `date`.
   - Franja en `HH:mm`, guardada como `LocalTime` en columnas `time` con `@JdbcType(LocalTimeJdbcType.class)`, por el gotcha de C-11. Un test verifica en la base que no haya desplazamiento ni en fechas ni en horas.
   - Los campos se leen como texto o entero estricto, como `SolicitudTramo` de C-11, así una coerción o un formato raro devuelve `400`.
   - `fechaDesde` se completa en el servicio con `LocalDate.now(clock)`. El servicio también rechaza con `400` una `fechaHasta` anterior a hoy: la validación depende del reloj, así que no puede vivir solo en el DTO.
   - `creadoEn` es un `Instant` (`timestamptz`) tomado del `Clock`.
   - La respuesta lleva IDs y no nombres. El frontend (C-47) puede cruzar los datos con los catálogos.

6. **Filtros del listado.** Son JPQL con parámetros opcionales (`:estado is null or e.estado = :estado`, y lo mismo para paciente y prestación), ordenados por `creadoEn` y `id`. `estado` se recibe como enum: un valor desconocido produce el `400` del manejador existente. Por ahora no hay paginación; se puede agregar en C-47 si hace falta.

7. **Planificador de vencimiento.**
   - Una configuración `@EnableScheduling` propia del módulo, `PlanificadorVencimiento`, con `@Scheduled(cron = "${lista-espera.vencimiento.cron:0 5 0 * * *}", zone = "America/Argentina/Buenos_Aires")`, que llama a `ServicioListaEspera.vencerEntradas()`.
   - Ese método ejecuta un `UPDATE` masivo parametrizado: `estado = VENCIDA where estado = ACTIVA and fechaHasta < :hoy`, con `hoy = LocalDate.now(clock)`. Devuelve la cantidad de entradas vencidas y no registra datos personales en el log.
   - En el perfil `test` el cron se desactiva con `-` (`lista-espera.vencimiento.cron: "-"`). Los tests llaman al método con un `Clock` fijo, como en `RelojFijoIT`.
   - Se descartó vencer de forma perezosa al consultar: la ficha pide un planificador, y C-31 necesita que el estado en la base sea confiable.

8. **Acceso.** Se agregan en `SecurityConfig` los matchers `POST /api/lista-espera` y `DELETE /api/lista-espera/{id}` con `hasAnyRole('ADMIN','RECEPCION')`, junto a los de horarios y usuarios. El servicio mantiene `@PreAuthorize` en cada método. Para leer alcanzan los tres roles: la matriz da lectura total al odontólogo, no solo de lo propio.

## UI Mockups

- Decision: no (no UI), 2026-11-25. C-18 es solo backend; la pantalla llega en C-47.

## Risks / Trade-offs

- [`SecurityConfig` y `schema-constraints.sql` se tocan en cada change] → Bloques nuevos al final con un comentario; si hay conflicto de merge, se resuelve conservando ambos bloques.
- [El odontólogo ve todas las entradas, aunque solo como IDs] → Es lo que fija la matriz (R). Si se decide limitarlo a "propias" (profesional preferido), cambia solo el filtro del servicio.
- [El índice parcial depende del texto `'ACTIVA'`] → El enum se persiste con `@Enumerated(STRING)`, y un test de integración cubre la violación del índice.
- [Primera tarea programada del proyecto] → El cron se desactiva en tests y es configurable por propiedad, para no disparar escrituras inesperadas.

## Migration Plan

El cambio es aditivo. Hibernate crea `lista_espera` y el script idempotente agrega las FK hacia `paciente`, `prestacion` y `profesional` y el índice parcial. No se migran datos. Para volver atrás, se conserva la tabla.

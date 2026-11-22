# Design

## Context

C-12 dejó el patrón que sigue este change:

- un subpaquete `profesionales.bloqueo`, organizado en `application`, `infrastructure` y `web`;
- un puerto de consulta interna, `BloqueoConsulta`;
- matchers de ruta en `SecurityConfig`, que deniegan las escrituras de `ODONTOLOGO` antes de que MVC valide el cuerpo;
- el uso de `ServicioProfesionales.detalle` para aplicar la titularidad en las lecturas;
- la FK idempotente en `schema-constraints.sql`.

`shared.tiempo` ya ofrece `Intervalo` (semiabierto, en `Instant`) y `ZonaHoraria.BUENOS_AIRES`. El adaptador productivo del vínculo odontólogo-profesional llega con C-13, que todavía no está mergeado. Igual que en C-12, aquí se prueba con un adaptador de test.

## Goals / Non-Goals

**Goals:**

- Repetir la estructura de C-12 para que los tres subpaquetes de la Ola 2 sean homogéneos.
- Mantener la conversión de zona horaria en un único lugar, que se pueda probar sin base de datos.
- Garantizar RN-16 también cuando dos altas llegan a la vez.

**Non-Goals:**

- Modificar `Profesional`, el catálogo de C-06 o la política global de seguridad.
- Anticipar el chequeo de turnos afectados al cambiar un horario. Eso requiere turnos y no lo cubre C-11.

## Decisions

1. **Subrecurso y servicio propios en `profesionales.horario`.** Se agregan la entidad `HorarioAtencion`, un repositorio JPA, la clase de persistencia (que implementa el puerto), `ServicioHorarios`, DTOs y `HorariosController`. El profesional se toma de la ruta, nunca del cuerpo. Modificar y eliminar se resuelven por el par `(profesionalId, horarioId)`, así un ID de tramo de otro profesional responde `404` sin tocar nada. Se descartó un único recurso global `/api/horarios`, porque rompería la convención de subrecursos de C-12.

2. **Representación de horas como `HH:mm` y columna `time`.** La API recibe y devuelve `"09:00"`, y se persiste como `LocalTime` en una columna `time`. Esto no contradice la regla de no guardar fechas sin zona (DD-05): un tramo es una hora de pared recurrente y no un instante. La zona se aplica recién al comparar contra un turno. Se aceptan solo minutos, porque los segundos no tienen sentido en una agenda. Se descartó guardar minutos desde la medianoche en un `int`: es menos legible en la base y en la API.
   - Igual que en `AltaBloqueo`, los campos se leen como texto estricto y se validan con un patrón y con `@AssertTrue` para `horaFin > horaInicio`. Así un número o un formato raro devuelve `400` y no un error de deserialización.
   - `diaSemana` es un `Integer` con `@Min(1) @Max(7)`.
   - En la base, `CHECK (hora_fin > hora_inicio)` y `CHECK (dia_semana BETWEEN 1 AND 7)` se declaran con `@Check` de Hibernate.

3. **No superposición validada en dominio y serializada por profesional.** Antes de guardar, el servicio busca con una consulta parametrizada si existe otro tramo del mismo profesional y día con `inicio < finNuevo AND fin > inicioNuevo`. En una modificación excluye el propio ID. Si existe, lanza `ReglaNegocioException("HORARIO_SUPERPUESTO", CONFLICTO_409, …)`.
   - Para que dos altas simultáneas no pasen las dos el chequeo, cada escritura toma primero un bloqueo de fila sobre el profesional (`SELECT … FOR UPDATE` parametrizado), dentro de la misma transacción. Esa misma consulta sirve como verificación de existencia y devuelve `404 PROFESIONAL_NO_ENCONTRADO`.
   - Se descartó una restricción `EXCLUDE USING gist` sobre un rango derivado de las horas. La KB fija que RN-16 se valida en dominio, la carga de escrituras es mínima (recepción, pocas veces) y una restricción con expresiones exigiría traducir otra violación de la base, cosa que recién construye C-19.

4. **Puerto `HorarioConsulta.contiene(Long profesionalId, Intervalo intervalo)` con conversión pura.** La conversión vive en una función pura del subpaquete: pasa los dos `Instant` a `ZonedDateTime` en `ZonaHoraria.BUENOS_AIRES`, devuelve vacío si las fechas locales difieren y, si no, devuelve el día ISO y las dos horas locales. Se prueba con JUnit sin Spring.
   - La persistencia ejecuta un `exists` parametrizado: `profesional_id = :p AND dia_semana = :d AND hora_inicio <= :i AND hora_fin >= :f`.
   - **Ajuste durante apply:** la aplicación configura `hibernate.jdbc.time_zone: UTC`. Con eso, Hibernate desplaza los `LocalTime` según la zona de la JVM: 12:30 llegaba a la base como 15:30. Por eso:
     - las columnas usan `@JdbcType(LocalTimeJdbcType.class)`, que envía la hora de pared sin convertir;
     - las dos consultas de comparación son nativas y reciben las horas como texto ISO casteado a `time`.

     Siguen parametrizadas, sin concatenar SQL. El `CHECK` del día se declara en el campo, porque Hibernate no generó un segundo `@Check` a nivel de clase.
   - Como los tramos de un día no se superponen, que exista un tramo que contenga a los dos extremos equivale a "completamente contenido en un único tramo". Un candidato que cruza un corte entre tramos contiguos no tiene ningún tramo que lo contenga entero, así que se rechaza.
   - No se usa `Clock`, porque el puerto no depende de la hora actual. La zona sale de la constante compartida y no de un desfase fijo (RN-14).
   - Un candidato que termina exactamente a las 00:00 del día siguiente se rechaza, porque las fechas locales difieren y ningún tramo termina después de las 23:59. Es coherente con la prohibición de cruzar la medianoche.

5. **Acceso.** Se agregan en `SecurityConfig` matchers `POST /api/profesionales/{id}/horarios`, `PUT` y `DELETE /api/profesionales/{id}/horarios/{horarioId}`, con `hasAnyRole('ADMIN','RECEPCION')`, junto a los de bloqueos. Los métodos de escritura del servicio mantienen `@PreAuthorize` como segunda barrera. El listado se protege para los tres roles y llama a `ServicioProfesionales.detalle` antes de consultar, igual que C-12. Así el odontólogo ajeno o sin vínculo recibe `403` sin revelar si el ID existe.

6. **Errores.** Los formatos inválidos devuelven `400` con el manejador existente. Se reutiliza `PROFESIONAL_NO_ENCONTRADO` (404) y se definen `HORARIO_NO_ENCONTRADO` (404) y `HORARIO_SUPERPUESTO` (409). Los mensajes no llevan datos personales.

## UI Mockups

- Decision: no (no UI), 2026-11-22. C-11 es solo backend; las pantallas llegan en C-22.

## Risks / Trade-offs

- [`SecurityConfig` lo tocan también otros changes, como C-13 y C-12] → Agregar solo un bloque de matchers junto al de bloqueos, con un comentario; el PR queda chico y el conflicto, si aparece, es trivial.
- [El bloqueo de fila sobre `profesional` lo toma otro subpaquete] → Es una lectura con bloqueo por ID parametrizado, sin abrir la entidad de C-06; queda encapsulada en la persistencia de horarios.
- [Editar o borrar un tramo puede dejar turnos futuros fuera de horario] → Hoy no hay turnos. Se deja explícito para que C-19 o un change posterior agregue la guarda (análogo a RN-17 en C-30).
- [El vínculo productivo de odontólogo depende de C-13] → Igual que en C-12, se prueba con un adaptador de test; al mergear C-13 no cambia este código.

## Migration Plan

El cambio es aditivo. Hibernate crea `horario_atencion` en desarrollo y tests, y el script idempotente agrega `fk_horario_atencion_profesional` después del DDL. No se mueven ni se rellenan datos. Para volver atrás, se conserva la tabla en lugar de borrarla automáticamente.

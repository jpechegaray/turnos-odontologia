# Verify — c-11-horarios-de-atencion (2026-11-22)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

`openspec validate c-11-horarios-de-atencion --strict` → `Change 'c-11-horarios-de-atencion' is valid`.

## Tareas

8 de 8 completas; no hay pendientes.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/profesionales/horario/`. Cada test tiene `@DisplayName` con el nombre exacto del escenario y afirma el THEN: estado HTTP, código de Problem Details y estado de la base después de la operación.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| Alta y listado | Administración y recepción crean y listan tramos del profesional solicitado | Cubierto | web/HorariosApiIT.java:64 | ADMIN y RECEPCION; orden; formato `HH:mm`; verifica en la base que la hora de pared no se desplaza |
| Alta y listado | Un profesional sin tramos devuelve un listado vacío | Cubierto | web/HorariosApiIT.java:93 | |
| Alta y listado | El alta rechaza días u horas inválidos | Cubierto | web/HorariosApiIT.java:101 | 16 cuerpos inválidos: faltantes, día fuera de rango, coerciones de tipo, formato, fin ≤ inicio y JSON roto; ninguno persiste |
| Alta y listado | Gestionar tramos de un profesional inexistente devuelve 404 | Cubierto | web/HorariosApiIT.java:124 | GET, POST, PUT y DELETE |
| Sin superposición | Se aceptan varios tramos contiguos o separados en el mismo día | Cubierto | web/HorariosApiIT.java:139 | |
| Sin superposición | Un tramo superpuesto en el mismo día se rechaza | Cubierto | web/HorariosApiIT.java:151 | Superposición parcial y contenida; `409 HORARIO_SUPERPUESTO` |
| Sin superposición | Los tramos de otro día o de otro profesional no generan conflicto | Cubierto | web/HorariosApiIT.java:165 | |
| Modificación y eliminación | Modificar un tramo dentro de su propio rango se acepta | Cubierto | web/HorariosApiIT.java:178 | Prueba que se excluye al propio tramo |
| Modificación y eliminación | Modificar un tramo para superponerlo con otro se rechaza | Cubierto | web/HorariosApiIT.java:194 | |
| Modificación y eliminación | Eliminar un tramo lo retira del listado y de la consulta de contención | Cubierto | web/HorariosApiIT.java:206 | |
| Modificación y eliminación | Un tramo ausente o de otro profesional no se puede modificar ni eliminar | Cubierto | web/HorariosApiIT.java:223 | PUT y DELETE; los dos tramos quedan intactos |
| Acceso | El odontólogo vinculado lista sus tramos pero no los ajenos | Cubierto | web/HorariosApiIT.java:243 | Vínculo de test (el productivo llega con C-13) |
| Acceso | El odontólogo sin vínculo no puede listar tramos | Cubierto | web/HorariosApiIT.java:260 | |
| Acceso | El odontólogo no puede crear, modificar ni eliminar tramos | Cubierto | web/HorariosApiIT.java:270 | Incluye cuerpo malformado e ID no numérico |
| Acceso | Las escrituras de tramos requieren CSRF válido | Cubierto | web/HorariosApiIT.java:286 | Sin token y con token inválido |
| Acceso | Consultar o escribir tramos sin sesión devuelve 401 | Cubierto | web/HorariosApiIT.java:306 | |
| Contención | Un turno que termina justo al cierre del tramo queda contenido | Cubierto | infrastructure/HorarioConsultaIT.java:41 | Criterio de salida: 12:30–13:00 |
| Contención | Un turno que excede el cierre del tramo no queda contenido | Cubierto | infrastructure/HorarioConsultaIT.java:47 | Criterio de salida: 12:45–13:15 |
| Contención | Un turno que cruza el corte entre dos tramos no queda contenido | Cubierto | infrastructure/HorarioConsultaIT.java:53 | |
| Contención | Un turno en otro día o de otro profesional no queda contenido | Cubierto | infrastructure/HorarioConsultaIT.java:60 | |
| Contención | La contención se calcula en hora de Buenos Aires | Cubierto | infrastructure/HorarioConsultaIT.java:67 | |

21 de 21 escenarios cubiertos. Además hay tests de apoyo que no corresponden a escenarios:

- `domain/FranjaLocalTest.java`: conversión pura a la zona de Buenos Aires y rechazo por cruce de medianoche.
- `infrastructure/HorarioJpaIT.java`: columna `time`, `CHECK`, FK y script idempotente.
- `application/ServicioHorariosConcurrenciaIT.java:53`: carrera de dos altas superpuestas. Se comprobó con una prueba de mutación: sin el bloqueo de fila, este test falla.

## Ejecución de tests

- Comando: `./mvnw -q -o verify` en `backend/`, con Docker 27.2.0 activo.
- Resultado: BUILD SUCCESS, código de salida 0.
  - Surefire (unitarios): 98 tests, 0 fallidos, 0 errores, 0 saltados.
  - Failsafe (integración con Testcontainers): 226 tests, 0 fallidos, 0 errores, 0 saltados.
  - Siguen en verde los tests existentes de C-03, C-06, C-12 y C-17.
- Frontend: no se ejecutó `vitest`, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

- **RN-04** (fuera de horario): el puerto `HorarioConsulta.contiene` la cubre con los bordes de la DCE (12:30–13:00 se acepta, 12:45–13:15 se rechaza). El rechazo de turnos lo aplicará C-19.
- **RN-14** (zona única): se usa `ZonaHoraria.BUENOS_AIRES` por `ZoneId`, sin desfase fijo; hay un test con candidato en UTC.
- **RN-16** (tramos sin superposición y turno en un único tramo): validación en dominio con intervalos semiabiertos, test de contigüidad, test de cruce de corte y test de concurrencia.
- **RN-33** (odontólogo solo lectura): 403 en las tres escrituras, también antes de validar el cuerpo.
- **Reglas duras**:
  - Solo hay datos ficticios ("Ana Ficticia", matrículas `MP-…`).
  - No hay secretos en el diff contra `origin/master`.
  - No se usa Flyway, JWT ni H2, ni se expone ninguna entidad JPA (los DTO son `SolicitudTramo` y `RespuestaTramo`).
  - Las consultas son parametrizadas.
  - Las horas de turno no se guardan sin zona: los tramos son horas de pared recurrentes, como justifica `design.md` (decisión 2).

## Diferencias con design.md

- Ninguna bloqueante ni mayor.
- **Menor, ya registrada en `design.md` durante el apply:** las consultas de comparación son nativas, no JPQL, y reciben la hora como texto casteado a `time`. Las columnas usan `LocalTimeJdbcType`. La causa es que `hibernate.jdbc.time_zone: UTC` desplazaba los `LocalTime`.
- **Menor:** el `CHECK` del día se declara en el campo y no en la clase.

## Qué NO se pudo verificar

- El vínculo productivo odontólogo-profesional, que depende de C-13 (no mergeado). La titularidad se probó con un adaptador de test, igual que en C-12.
- El impacto de editar o borrar tramos sobre turnos ya dados: todavía no hay turnos. Queda como riesgo documentado para C-19 o un change posterior.

## Próximo paso

Commit, PR de `valentino-c-11-horarios-de-atencion` a `master` (lo revisa Juan Pablo) y, después del merge, `/opsx:archive c-11-horarios-de-atencion`. Para Juan Pablo en el PR: revisar las consultas nativas con cast de hora (`HorarioJpaRepository`), que son una excepción al uso de JPQL del resto del módulo.

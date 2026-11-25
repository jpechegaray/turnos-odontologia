# Verify — c-26-horarios-libres-disponibilidad (2026-11-25)

**Veredicto: LISTO PARA ARCHIVAR** (verificación técnica; commit, PR, merge y archivo pendientes).

## Validación estructural

`openspec validate c-26-horarios-libres-disponibilidad --strict`: PASS. Proposal, design, delta spec y tasks presentes. `git diff --check`: PASS. La rama es `juan-pablo-c-26`, actualizada a master `545c1ad` (incluye C-25 y C-27).

## Tareas

5/5 completas. El seguimiento posterior al merge está fuera de las tareas de implementación.

## Matriz escenario → test

Los caminos de esta tabla son relativos a `backend/src/test/java/ar/edu/utn/turnos/agenda/`. Se leyó el cuerpo de los tests y se comprobó que afirma el resultado esperado. `D` = `domain/CalculadorDisponibilidadTest.java`; `A` = `web/DisponibilidadApiIT.java`. Todos los escenarios pertenecen a `agenda-disponibilidad`.

| Escenario | Estado | Test |
|---|---|---|
| Un hueco exacto admite la prestación | Cubierto | D:20 |
| Una prestación mayor al hueco no se ofrece | Cubierto | D:40 |
| Los tramos no se unen para completar una prestación | Cubierto | D:47 |
| La búsqueda redondea hacia la próxima grilla | Cubierto | D:55 |
| No se ofrecen inicios pasados ni iguales a ahora | Cubierto | D:65 |
| Una duración inválida se rechaza | Cubierto | D:74 |
| El borde contiguo de un turno queda disponible | Cubierto | D:91 y A:181 |
| Un bloqueo excluye solapamientos y permite el borde | Cubierto | D:100 y A:191 |
| Se elige el primer sillón libre para todo el intervalo | Cubierto | D:107 y A:209 |
| Los turnos cancelados no quitan disponibilidad | Cubierto | D:117 y A:200 |
| Sin sillones libres no se ofrecen horarios | Cubierto | D:127 y A:222 |
| Recepción consulta disponibilidad y crea un turno sugerido | Cubierto | A:79 |
| Se puede consultar una duración ajustada | Cubierto | A:98 |
| Recursos inexistentes o inactivos se rechazan | Cubierto | A:108 (4 casos) |
| Un profesional sin la prestación se rechaza | Cubierto | A:123 |
| Un rango inválido se rechaza | Cubierto | A:132 (3 casos) |
| Parámetros obligatorios o con formato inválido | Cubierto | A:139 |
| Fechas fuera del calendario soportado se rechazan | Cubierto | A:238 |
| La búsqueda respeta el día de Buenos Aires | Cubierto | A:149 |
| El administrador consulta disponibilidad | Cubierto | A:159 |
| El odontólogo no consulta disponibilidad | Cubierto | A:166 (API y fachada) |
| Sin sesión no se consulta disponibilidad | Cubierto | A:175 |
| El núcleo de disponibilidad se reutiliza sin sesión del personal | Cubierto | A:230 |

## Ejecución de tests

| Comando | Resultado |
|---|---|
| `backend/mvnw.cmd -q test` antes del cambio | PASS, 220 unitarios |
| `backend/mvnw.cmd -q -Dtest=CalculadorDisponibilidadTest,DisponibilidadApiIT test` | PASS, 11 unitarios y 22 casos de integración |
| `backend/mvnw.cmd -q verify` | PASS, 231 unitarios y 449 de integración en la base inicial `fd2ef51` |
| `backend/mvnw.cmd -B -ntp verify` sobre `545c1ad` | PASS, 253 unitarios y 496 de integración: 749 tests, 0 fallos, 0 errores y 0 saltados |
| `npm ci` en frontend | PASS, sin modificar manifiesto ni lockfile |
| `npm run lint` en frontend | PASS |
| `npm test` en frontend | PASS, 290 tests en 34 archivos |
| `npm run build` en frontend | PASS, 139 módulos |

Los tests backend de integración usaron PostgreSQL 16 real de Testcontainers. La suite completa incluye las pruebas de concurrencia y restricciones EXCLUDE de C-19. Se contaron los reportes Surefire de `*Test` y los Failsafe de `*IT`; se excluyó de la suma Surefire el reporte IT de la ejecución focalizada para no contar sus 22 casos dos veces. Failsafe informa 496 completados y cero fallos, errores o saltados en la base actualizada.

JaCoCo en la base actualizada: 1993/2074 líneas globales cubiertas (96,09%). El calculador tiene 28/28 líneas y el servicio 53/59; el resto de clases nuevas ejecutables supera el 90% de líneas. Reporte local: `backend/target/site/jacoco/index.html`.

### Evidencia TDD

| Tarea | Red de seguridad | RED ejecutado | GREEN ejecutado | Triangulación y refactor |
|---|---|---|---|---|
| 1.1/1.2 dominio | 220 unitarios existentes verdes | Compilación falla porque CalculadorDisponibilidad y HorarioDisponible no existen | 1 caso exacto verde con implementación mínima | 11 casos: inicialmente 9 fallos y 1 error; algoritmo generalizado, 11/11 verdes |
| 2.1/2.2 consulta y API | Dominio 11/11 verde | Consulta HTTP devuelve 404 en vez de 200 | Consulta devuelve 200 y creación sugerida devuelve 201 | 21 casos de integración; se corrigió el orden del test de seguridad porque MockMvc limpia el contexto tras el request; finalmente 22/22 verdes |
| Fechas extremas | Pruebas normales verdes | Año +999999999 produce 500 por desbordamiento de PostgreSQL | Validación previa devuelve 422 | Suite focalizada 33/33 y suite backend completa 680/680 verdes |

Logs reproducibles de la sesión en `backend/target/c26-*.log` (no versionados). El dominio no importa Spring ni JPA y las pruebas de arquitectura siguen en verde.

## Reglas de negocio y reglas duras

- RN-01/RN-02: se excluyen ambos recursos, incluso cuando el sillón lo ocupa otro profesional; los extremos contiguos se aceptan.
- RN-03/RN-09: duración efectiva opcional o por defecto, límites de 5 a 480 y grilla de cinco minutos, sin segundos ni nanos. Rango inferior redondeado hacia arriba.
- RN-04/RN-16: contención completa en un tramo y un día local, sin fusionar tramos; bloqueos precargados y verificados por integración.
- RN-10: cancelados no ocupan; atendidos y ausentes sí.
- RN-11: profesional y prestación existentes, activos y relacionados; solo sillones activos, elección determinista por id.
- RN-13/RN-14: inicio estrictamente futuro respecto de Clock inyectado; cálculos con ZoneId de Buenos Aires, instantes UTC en la salida.
- RN-15: la sugerencia no reserva. La creación revalida y las restricciones EXCLUDE siguen siendo la garantía final ante carreras.
- Datos y credenciales: solo fixtures ficticios. No se leyó `.env`; no se agregaron secretos ni cambios de configuración o dependencias. La salida tiene exactamente inicio, fin y sillonId; no se consultan ni exponen datos de pacientes.
- Tests: no se agregó `.with(csrf())`; el test cruzado usa `csrfValido()`.
- No se cambiaron TurnoJpaRepository, Turno, SecurityConfig ni archivos del trabajo C-27. Solo se agregan archivos propios del change. Sin commits, pushes ni cambios en master.

## Diferencias con design.md

Ninguna. La validación MVC de formato precede a la autorización de la fachada; el 403 del odontólogo se verifica con parámetros válidos, según el contrato documentado. Fechas con año local fuera de 1 a 9999 producen 422 antes de enviar parámetros no representables a PostgreSQL.

## Qué NO se pudo verificar

No se ejecutó un escaneo CVE del backend ni análisis con SpotBugs/PMD/Checkstyle (no están configurados). No cambian dependencias. La instalación frontend informó 7 avisos de auditoría preexistentes; no se aplicaron actualizaciones ajenas al change. Los avisos Future Flag de React Router no causaron fallos. No hay UI nueva ni verificación visual aplicable.

## Próximo paso

Revisión por Juan Pablo y autorización antes del commit (regla dura 16). Después, PR a master con `Closes #26`, CI verde y merge autorizado. Solo tras el merge: archivar, actualizar CHANGES.md y pasar el issue/tablero a Hecho.

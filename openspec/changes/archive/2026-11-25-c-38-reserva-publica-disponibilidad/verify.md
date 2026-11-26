# Verify — c-38-reserva-publica-disponibilidad (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

`openspec validate c-38-reserva-publica-disponibilidad --strict` → `Change 'c-38-reserva-publica-disponibilidad' is valid`.

## Tareas

15/15 completas; no hay pendientes.

## Matriz escenario → test

Todos los tests están en `backend/src/test/java/ar/edu/utn/turnos/reservapublica/web/`. Cada uno lleva `@DisplayName` con el nombre exacto del escenario y afirma el THEN (estado HTTP y contenido o código).

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| reserva-publica | Se listan solo prestaciones activas con profesional activo | Cubierto | ReservaPublicaApiIT.java:103 | Verifica los 3 campos y excluye la prestación inactiva y la que no tiene profesional |
| reserva-publica | Un profesional inactivo no hace reservable una prestación | Cubierto | ReservaPublicaApiIT.java:121 | |
| reserva-publica | Se listan profesionales activos de la prestación sin matrícula | Cubierto | ReservaPublicaApiIT.java:134 | 4 campos, sin `matricula` |
| reserva-publica | Una prestación inexistente o inactiva devuelve lista vacía | Cubierto | ReservaPublicaApiIT.java:152 | Prueba los dos casos |
| reserva-publica | Falta el parámetro | Cubierto | ReservaPublicaApiIT.java:164 | También con id mal formado |
| reserva-publica | Un horario ocupado no se ofrece | Cubierto | ReservaPublicaApiIT.java:173 | |
| reserva-publica | Un horario contiguo a un turno sí se ofrece | Cubierto | ReservaPublicaApiIT.java:184 | Bordes antes y después del turno (RN-02) |
| reserva-publica | El JSON tiene solo inicio y fin | Cubierto | ReservaPublicaApiIT.java:195 | Cada elemento tiene 2 campos; el cuerpo no contiene "sillon" |
| reserva-publica | Parámetros ausentes o mal formados | Cubierto | ReservaPublicaApiIT.java:212 | Sin parámetros, fecha sin zona, id no numérico, `hasta` ausente |
| reserva-publica | Fin no posterior al inicio | Cubierto | ReservaPublicaApiIT.java:229 | Fin igual y fin anterior |
| reserva-publica | Rango de más de 31 días | Cubierto | ReservaPublicaApiIT.java:240 | 31 días + 1 ms |
| reserva-publica | Rango válido fuera del horizonte no es un error de rango | Cubierto | ReservaPublicaApiIT.java:248 | |
| reserva-publica | Borde de anticipación mínima | Cubierto | ReservaPublicaApiIT.java:257 | `RelojFijoConfig`; el primer inicio es justo el límite |
| reserva-publica | Borde del horizonte | Cubierto | ReservaPublicaApiIT.java:270 | El último fin es justo el horizonte |
| reserva-publica | Un cambio de configuración aplica a la siguiente consulta | Cubierto | ReservaPublicaApiIT.java:284 | Cambian la anticipación y el horizonte |
| reserva-publica | Los tres casos dan la misma respuesta | Cubierto | ReservaPublicaApiIT.java:300 | 5 variantes; cuerpos idénticos salvo `instance` |
| reserva-publica | Un profesional inactivo con rango fuera del horizonte da 404 | Cubierto | ReservaPublicaApiIT.java:330 | |
| reserva-publica | La consulta del personal conserva sus errores detallados | Cubierto | ReservaPublicaApiIT.java:340 | 422 `PROFESIONAL_INACTIVO` y `RN-11` |
| reserva-publica | Se supera el límite de solicitudes | Cubierto | ReservaPublicaLimiteTasaIT.java:23 | `max-solicitudes-por-ip=2`; 429 con `Retry-After` |

Otros tests de apoyo:
- `reservapublica/application/DisponibilidadPublicaTest`: orden de validación, recorte, traducción defensiva de los 5 códigos del núcleo y errores que no se traducen.
- `ProfesionalRealizaPrestacionIT` y `CatalogoPrestacionesServicioIT`: los puertos nuevos.

## Ejecución de tests

- Backend: `./mvnw verify`, corrido en esta sesión sobre el código final. Desde esa corrida solo cambiaron `tasks.md` y este informe. Resultado `BUILD SUCCESS`: 352 unitarios y 693 de integración (Testcontainers con PostgreSQL 16), 0 fallidos, 0 con error, 0 saltados.
- Frontend: `npx vitest run` → 38 archivos y 328 tests, todos pasan. El change no toca el frontend.

## Reglas de negocio y reglas duras

- RN-27 (horizonte y anticipación mínima): cubierta por los escenarios de bordes y de cambio de configuración. El rate limit por IP está cubierto por el escenario de 429. El tope por email queda fuera de alcance (C-41).
- RN-08 (solo horarios libres): cubierta por los escenarios de ocupado y contiguo. La validación definitiva al reservar (409) es de C-41.
- RN-01/RN-02: el borde semiabierto se prueba en "Un horario contiguo a un turno sí se ofrece". El cálculo de fondo es el núcleo de C-26, cuyos tests siguen en verde. Este change no crea turnos, así que no aplica el test de la constraint `EXCLUDE`.
- RN-11: profesional sin la prestación → 404 en el endpoint público y 422 `RN-11` en el del personal.
- Reglas duras: los datos son ficticios (`DatosAgenda`, `@*.test`); no hay secretos ni cambios en `application*.yml`; no se exponen entidades JPA (solo DTOs `record`); el JPQL es parametrizado; no se agregan endpoints de escritura; todos los tests llevan `@DisplayName`.

## Diferencias con design.md

- Ninguna bloqueante ni mayor.
- Menor, ya incorporada al design: los puertos `profesionalesQueRealizan` y `prestacionesConProfesional` devuelven ids sin filtrar por estado; el filtro por activo lo hace `CatalogoPublico`, para no hacer un JOIN con la entidad de otro módulo.

## Qué NO se pudo verificar

- No hay prueba de carga ni de concurrencia del rate limit con varios nodos (fuera de alcance: el limitador es en memoria, según C-17).
- No hay verificación visual (no hay UI; `Decision: no (no UI)`).

## Próximo paso

PR desde `lionel` a `master` (lo revisa Juan Pablo), con referencia al issue #38. Después del merge, `/opsx:archive c-38-reserva-publica-disponibilidad`.

# Tasks

Aplicar TDD estricto por tarea. Usar datos ficticios y el nombre exacto del escenario en `@DisplayName` cuando el test cubra la spec.

## 1. Dominio y persistencia

- [x] 1.1 Implementar la conversión pura de un `Intervalo` a día ISO y horas locales de Buenos Aires. Se verifica con JUnit sin Spring: un candidato `12:00Z–12:30Z` de un lunes da lunes `09:00–09:30`, uno que cruza la medianoche local da vacío y uno que termina a las 00:00 del día siguiente da vacío.
- [x] 1.2 Mapear `HorarioAtencion` con `time`, los `CHECK` de día y horas y el índice `(profesional_id, dia_semana)`, y agregar la FK idempotente al final de `schema-constraints.sql`. Se verifica con PostgreSQL real (Testcontainers): tipo `time`, rechazo de día fuera de rango, de fin no posterior y de profesional inexistente, y que correr el script dos veces no duplica la FK.
- [x] 1.3 Implementar `HorarioConsulta.contiene` con una consulta nativa parametrizada (horas como texto casteado a `time`). Se verifica contra PostgreSQL con los escenarios de contención de la spec: 12:30–13:00 contenido, 12:45–13:15 no contenido, cruce del corte entre tramos contiguos, otro día u otro profesional, y candidato expresado en UTC.

## 2. API de gestión y reglas

- [x] 2.1 Implementar `POST` y `GET` con DTOs, validación estricta de `HH:mm` y `diaSemana`, y transacciones. Se verifica con MockMvc y PostgreSQL, para `ADMIN` y `RECEPCION`: `201`/`200`, orden por día, hora e `id`, listado vacío, `400` por entradas inválidas y `404 PROFESIONAL_NO_ENCONTRADO`.
- [x] 2.2 Agregar la regla de no superposición con bloqueo de fila del profesional. Se verifica con los escenarios de la spec: tramos contiguos o separados aceptados, superpuestos con `409 HORARIO_SUPERPUESTO` sin persistir, y otro día u otro profesional sin conflicto. Se suma un test de dos transacciones concurrentes que intentan crear tramos superpuestos: una gana y la otra recibe `409`.
- [x] 2.3 Implementar `PUT` y `DELETE` por `(profesionalId, horarioId)`. Se verifica para ambos roles: modificación dentro del propio rango aceptada, modificación que superpone con `409` sin cambios, `204` sin cuerpo, ausencia posterior en el listado y en la consulta de contención, y `404 HORARIO_NO_ENCONTRADO` sin mutación ante un tramo ajeno o inexistente.
- [x] 2.4 Agregar los matchers de escritura en `SecurityConfig` y completar las pruebas de permisos. Se verifica:
  - lectura propia con un vínculo de test, y `403` sin vínculo o con un ID ajeno, exista o no;
  - `403` para `ODONTOLOGO` en `POST`, `PUT` y `DELETE` con CSRF válido, incluso con un cuerpo malformado;
  - `403` sin CSRF para los roles autorizados;
  - `401` sin sesión en las cuatro operaciones.

  En todos los casos se comprueban Problem Details y que no haya mutaciones.

## 3. Verificación integrada

- [x] 3.1 Ejecutar `mvn verify` desde `backend` con Docker disponible y `openspec validate c-11-horarios-de-atencion --strict` en la raíz. Se comprueba que siguen pasando los tests de C-03, C-06 y C-12 y que cada escenario nuevo tiene una prueba identificable.

## Workflow follow-up

- `openspec-verify` para generar `verify.md` con el veredicto.
- PR de `valentino-c-11-horarios-de-atencion` a `master`, revisado por Juan Pablo.
- `/opsx:archive` después del merge.

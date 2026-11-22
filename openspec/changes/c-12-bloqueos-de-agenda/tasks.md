# Tasks

Aplicar TDD estricto por tarea. Usar datos ficticios y el nombre exacto del escenario en `@DisplayName` cuando el test cubra la spec.

## 1. Intervalos y persistencia

- [x] 1.1 Introducir `Intervalo` en `shared.tiempo`; verificar con JUnit que acepta extremos crecientes y rechaza iguales e invertidos.
- [x] 1.2 Mapear `Bloqueo` y agregar la FK idempotente; verificar con PostgreSQL real el tipo `timestamptz`, el rechazo de rangos inválidos y profesionales inexistentes, y que ejecutar dos veces el script no duplica la FK.
- [x] 1.3 Implementar `BloqueoConsulta.solapa` con consulta parametrizada; verificar contra PostgreSQL el cruce 10:30–11:30 con un bloqueo 10:00–11:00, la contigüidad en ambos extremos y el aislamiento por profesional.

## 2. API de gestión y acceso

- [x] 2.1 Implementar `POST` y `GET` con DTOs, validación y transacciones; verificar con MockMvc y PostgreSQL `201`/`200`, normalización UTC, orden y listado vacío, `400` para fechas o motivos inválidos y `404` para un profesional inexistente. Cubrir `ADMIN` y `RECEPCION`.
- [x] 2.2 Implementar `DELETE` por profesional e ID de bloqueo; verificar para ambos roles `204` sin cuerpo, ausencia posterior en el listado y en la consulta interna, y `404` sin mutación ante profesional inexistente o bloqueo ajeno o inexistente.
- [x] 2.3 Completar las pruebas de los permisos junto con las operaciones: verificar lectura propia con vínculo de test y `403` sin vínculo o para ID ajeno (existente o no); verificar con CSRF válido que `ODONTOLOGO` no escribe, que sin CSRF válido tampoco escriben los roles autorizados y que sin sesión las tres operaciones devuelven `401`. Comprobar Problem Details y ausencia de mutaciones.

## 3. Verificación integrada

- [x] 3.1 Ejecutar `mvn verify` desde `backend` con Docker disponible y `openspec validate c-12-bloqueos-de-agenda --strict` en la raíz; comprobar que pasan los tests existentes de C-03 y C-06 y que cada escenario nuevo tiene una prueba identificable.

# Tasks

Aplicar TDD estricto por tarea. Usar datos ficticios y el nombre exacto del escenario en `@DisplayName` cuando el test cubra la spec.

## 1. Dominio

- [x] 1.1 Implementar la entrada de lista de espera y `EstadoListaEspera`, con las transiciones `asignar`, `cancelar` y `vencer`. Se verifica con JUnit sin Spring: desde `ACTIVA` cada transición llega a su estado (y `asignar` registra el turno); desde cada estado final, las tres se rechazan con `ENTRADA_NO_ACTIVA`; `asignar` sin turno se rechaza. `ArquitecturaTest` sigue en verde.

## 2. Persistencia

- [x] 2.1 Mapear la tabla `lista_espera` (fechas `date`, franja `time` con `LocalTimeJdbcType`, estado como texto, `CHECK` de fechas y franja, índices `(estado, prestacion_id)` y `(paciente_id)`), y agregar al final de `schema-constraints.sql` las FK y el índice único parcial `ux_lista_espera_activa`. Se verifica con PostgreSQL real:
  - los tipos de columna;
  - que fechas y horas guardadas por la entidad se lean sin desplazamiento;
  - el rechazo de rangos y franjas inválidos y de FK inexistentes;
  - que el índice impide dos `ACTIVA` y permite una `CANCELADA` más una `ACTIVA`;
  - que correr el script dos veces no duplica nada.

## 3. API y reglas

- [x] 3.1 Implementar `POST /api/lista-espera` con DTO estricto, `fechaDesde` por defecto desde el `Clock` y validación de recursos. Se verifica con MockMvc y PostgreSQL, para `ADMIN` y `RECEPCION`:
  - alta completa;
  - alta mínima con `fechaDesde` igual a hoy (reloj fijo);
  - `400` por cada formato inválido, incluida una `fechaHasta` pasada;
  - `422` con cada código de recurso, incluido `PACIENTE_SIN_EMAIL`;
  - en ningún caso de error se persiste la entrada.
- [x] 3.2 Agregar la regla de duplicado (consulta previa más traducción de la violación del índice). Se verifica con los escenarios de la spec: duplicado activo con `409 ENTRADA_ACTIVA_DUPLICADA`; otra prestación o una entrada cancelada sin conflicto; y un test de dos transacciones concurrentes donde una gana y la otra recibe el error.
- [x] 3.3 Implementar `GET /api/lista-espera` con filtros combinables y orden por antigüedad, y `GET /api/lista-espera/{id}`. Se verifica: filtros por estado, paciente y prestación, solos y combinados; orden; estado desconocido con `400`; `404 ENTRADA_NO_ENCONTRADA`.
- [x] 3.4 Implementar `DELETE /api/lista-espera/{id}` como cancelación. Se verifica: `204` y la entrada conservada como `CANCELADA`; después el paciente se puede volver a anotar; `409 ENTRADA_NO_ACTIVA` sin cambios para `VENCIDA`, `CANCELADA` y `ASIGNADA`; `404` para un ID inexistente.

## 4. Vencimiento

- [x] 4.1 Implementar `vencerEntradas()` y el planificador con `@EnableScheduling`, el cron configurable y desactivado en el perfil `test`. Se verifica con un `Clock` fijo el escenario de la spec (vence la de ayer, no la de hoy ni la cancelada) y que el método devuelva la cantidad vencida. Un test de contexto confirma que el bean programado existe con su cron y su zona.

## 5. Acceso

- [x] 5.1 Agregar los matchers de escritura en `SecurityConfig` y completar las pruebas de permisos. Se verifica:
  - `ODONTOLOGO` lee (`200`) y recibe `403` al crear o dar de baja, incluso con un cuerpo malformado;
  - sin CSRF válido, los roles autorizados reciben `403`;
  - sin sesión, las cuatro operaciones devuelven `401`.

  En todos los casos se comprueban Problem Details y que no haya mutaciones.

## 6. Verificación integrada

- [x] 6.1 Ejecutar `mvn clean verify` desde `backend` con Docker y `openspec validate c-18-lista-espera-alta-y-gestion --strict`. Se comprueba que siguen pasando los tests de los demás módulos y que cada escenario nuevo tiene una prueba identificable.

## Workflow follow-up

- `openspec-verify` para generar `verify.md`.
- PR desde `valentino` a `master`, revisado por Juan Pablo.
- `/opsx:archive` después del merge.

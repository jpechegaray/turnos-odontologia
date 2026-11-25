# Tasks

Aplicar TDD estricto en cada tarea: primero el test que falla, después el código mínimo y por último el refactor. Usar solo datos ficticios. Cuando un test cubra un escenario de la spec, su `@DisplayName` lleva el nombre exacto del escenario. En los tests de integración, usar `csrfValido()` y `csrfConCabeceraIncorrecta()` de `PruebaIntegracionBase`, nunca `.with(csrf())`.

## 1. Intervalo y bordes de RN-02

- [x] 1.1 Agregar a `shared/tiempo/Intervalo` los métodos `solapaCon`, `contiene` y `desde(inicio, minutos)`. Se verifica con JUnit, sin Spring:
  - 10:30-11:00 no solapa con 10:00-10:30 en ningún sentido;
  - 10:29-10:59 sí solapa;
  - un intervalo contenido, uno igual y uno que sobresale;
  - `desde` calcula el fin.

  Los tests actuales de horarios y bloqueos siguen en verde.

## 2. Dominio del turno

- [x] 2.1 Crear `EstadoTurno` con la tabla de transiciones de RN-12 y el record de dominio `Turno`. Se verifica con JUnit:
  - los escenarios "Transiciones permitidas desde un turno programado" y "Un estado final no cambia", este último con `TURNO_TRANSICION_INVALIDA`;
  - `CONFIRMADO` también puede pasar a `CANCELADO`, `ATENDIDO` o `AUSENTE`;
  - un turno nuevo queda `PROGRAMADO` y con canal `INTERNO`.
- [x] 2.2 Crear los eventos `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `HuecoLiberado` como records en `agenda.domain.evento`. Se verifica con `ArquitecturaTest` en verde: el paquete no depende de frameworks.

## 3. Validador puro

- [x] 3.1 Implementar en `ValidadorTurno` la verificación de RN-11: inexistente, inactivo y profesional que no realiza la prestación, con los códigos de D4. Se verifica con JUnit, un test por recurso y por motivo.
- [x] 3.2 Agregar RN-13 con un `Clock` fijo (un inicio igual al momento actual se rechaza) y RN-09, reutilizando `DuracionPrestacion` y con un inicio de 10:03 rechazado. Se verifica con JUnit.
- [x] 3.3 Agregar RN-04 y RN-16 a partir de los resultados de horario y bloqueo, con la propiedad `motivo`. Se verifica con JUnit.
- [x] 3.4 Agregar RN-01 por profesional y por sillón a partir de la lista de ocupados: los cancelados no cuentan, cuando chocan los dos ejes gana `PROFESIONAL` y se ignora el `turnoExcluidoId`. Se verifica con JUnit, incluido el orden: un caso con varias reglas incumplidas devuelve la primera del Flujo 1.

## 4. Persistencia y restricciones

- [ ] 4.1 Mapear `TurnoJpa` y la tabla `turno` con las columnas del ERD, `@Version` y los cuatro índices. Agregar al final de `schema-constraints.sql`, en bloques `DO $$` idempotentes:
  - las FK a `pacientes`, `profesional`, `sillon`, `prestacion` y `usuario`;
  - los `CHECK`;
  - `turno_sin_solape_profesional` y `turno_sin_solape_sillon`.

  Se verifica con Testcontainers:
  - el escenario "Las restricciones de exclusión existen en la base", que consulta `pg_constraint`;
  - los tipos `timestamptz`;
  - que correr el script dos veces no falla.
- [ ] 4.2 Implementar el repositorio y la persistencia con `saveAndFlush` y la traducción de cada restricción a `409 RN-01` con su `recurso`. Se verifica con Testcontainers:
  - el escenario "La base rechaza un solapamiento que esquivó la validación", por sillón y por profesional;
  - un turno `CANCELADO` superpuesto se guarda sin error;
  - la consulta de ocupados devuelve solo los no cancelados que solapan.

## 5. Servicio y API

- [ ] 5.1 Implementar `CrearTurnoService`: arma los datos con los puertos, llama al validador, guarda y publica `TurnoCreado`. Agregar `POST /api/turnos` con el DTO de entrada y el de salida. Se verifica con MockMvc y PostgreSQL:
  - "Recepción crea un turno válido", con `Location`, el creador guardado y el evento publicado;
  - "Sin duración se usa la de la prestación";
  - "La duración se puede ajustar";
  - "Datos obligatorios o con formato inválido".
- [ ] 5.2 Cubrir por la API los escenarios de recursos, tiempo y horario, verificando en cada rechazo que no se guarda nada:
  - "Un recurso inexistente se rechaza";
  - "Un recurso inactivo se rechaza";
  - "El profesional no realiza la prestación";
  - "Un turno en el pasado se rechaza";
  - "Un inicio fuera de la grilla de cinco minutos se rechaza";
  - "Una duración fuera de rango se rechaza";
  - los cinco escenarios de horario y bloqueos.
- [ ] 5.3 Cubrir por la API los escenarios de solapamiento:
  - "Un turno solapado por profesional se rechaza";
  - "Un turno solapado por sillón se rechaza";
  - "Un turno que empieza justo cuando termina otro no se solapa";
  - "Un turno que empieza un minuto antes del fin de otro se solapa";
  - "Un turno atendido sigue ocupando lugar";
  - "Un turno cancelado no ocupa lugar".

  Los turnos `ATENDIDO` y `CANCELADO` de partida se insertan directamente en la tabla, porque C-19 no tiene endpoints para cambiar estados.

## 6. Concurrencia

- [ ] 6.1 Escribir el test de integración "Dos creaciones simultáneas sobre el mismo hueco". Dos hilos con sesiones distintas se sincronizan con un `CountDownLatch` y llaman a `POST /api/turnos` con el mismo profesional y horario. Se verifica:
  - exactamente un `201` y un `409 RN-01`;
  - `count` de turnos igual a 1;
  - el test se corre 5 veces seguidas sin fallar.

## 7. Acceso

- [ ] 7.1 Agregar el matcher de `POST /api/turnos` en `SecurityConfig` y `@PreAuthorize` en el servicio. Se verifica con los escenarios:
  - "El odontólogo no puede crear turnos";
  - "Sin sesión no se puede crear un turno";
  - "Sin CSRF válido no se puede crear un turno".

  `ADMIN` también crea con `201`.

## 8. Verificación final

- [ ] 8.1 Correr `./mvnw verify` (con Docker) y, en `frontend`, `npm run lint`, `npm test` y `npm run build`: todo en verde. Después correr `openspec-verify` para generar `verify.md`, con cada escenario mapeado a su test y el veredicto LISTO PARA ARCHIVAR.

## Workflow follow-up

- Abrir el PR de `juan-pablo-c-19` a `master` con "Closes #19", esperar el CI en verde y mergear con `--admin --merge`.
- Después del merge: `/opsx:archive`, CHANGES.md (C-19 hecho por Juan Pablo, con PR y fecha), tablero en Hecho y verificar que el issue #19 quedó cerrado.

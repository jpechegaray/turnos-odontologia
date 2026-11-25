# Verify — c-18-lista-espera-alta-y-gestion (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR

## Validación estructural

`openspec validate c-18-lista-espera-alta-y-gestion --strict` → `Change 'c-18-lista-espera-alta-y-gestion' is valid`.

## Tareas

9 de 9 completas; no hay pendientes.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/listaespera/`. Cada test tiene `@DisplayName` con el nombre exacto del escenario y afirma el THEN: estado HTTP, código de Problem Details y estado de la base después de la operación.

| Requisito | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| Alta | Recepción anota a un paciente con todos los datos | Cubierto | web/ListaEsperaApiIT.java:55 | ADMIN y RECEPCION; todos los campos de la respuesta; `creadoEn` del reloj fijo; franja leída en la base sin desplazamiento |
| Alta | Solo con los datos obligatorios la fecha de inicio es hoy | Cubierto | web/ListaEsperaApiIT.java:78 | Reloj fijo: `fechaDesde` 2026-12-01 |
| Alta | El alta rechaza datos con formato inválido | Cubierto | web/ListaEsperaApiIT.java:91 | 15 cuerpos inválidos, incluida una `fechaHasta` pasada y coerciones de tipo; nada se persiste |
| Recursos válidos | Un paciente sin email no se puede anotar | Cubierto | web/ListaEsperaApiIT.java:119 | `422 PACIENTE_SIN_EMAIL` |
| Recursos válidos | Paciente, prestación o profesional inexistente o inactivo se rechaza | Cubierto | web/ListaEsperaApiIT.java:129 | Los 6 códigos 422 |
| Sin duplicados | Una segunda entrada activa para la misma prestación se rechaza | Cubierto | web/ListaEsperaApiIT.java:153 | `409 ENTRADA_ACTIVA_DUPLICADA` |
| Sin duplicados | Otra prestación o una entrada ya cancelada no generan duplicado | Cubierto | web/ListaEsperaApiIT.java:164 | |
| Sin duplicados | Dos altas simultáneas duplicadas: una gana y la otra recibe 409 | Cubierto | application/ServicioListaEsperaConcurrenciaIT.java:55 | Dos hilos con transacciones reales. Prueba de mutación: sin el índice parcial, el test falla |
| Consulta | El listado filtra y ordena por antigüedad | Cubierto | web/ListaEsperaApiIT.java:180 | Los tres roles; filtros solos y combinados; estado desconocido con 400; detalle |
| Consulta | Consultar una entrada inexistente devuelve 404 | Cubierto | web/ListaEsperaApiIT.java:206 | |
| Baja | Cancelar una entrada activa la conserva como cancelada | Cubierto | web/ListaEsperaApiIT.java:217 | `204`, estado `CANCELADA`, nueva alta permitida y 404 para un ID inexistente |
| Baja | Una entrada que no está activa no se puede cancelar | Cubierto | web/ListaEsperaApiIT.java:231 | VENCIDA, CANCELADA y ASIGNADA |
| Ciclo de estados | Desde ACTIVA se puede asignar, cancelar o vencer | Cubierto | domain/EntradaListaEsperaTest.java:19 | JUnit puro |
| Ciclo de estados | Un estado final no admite otra transición | Cubierto | domain/EntradaListaEsperaTest.java:36 | Parametrizado por los tres estados finales |
| Vencimiento | Una entrada con fecha de fin pasada vence en el siguiente ciclo | Cubierto | application/VencimientoListaEsperaIT.java:31 | Reloj fijo: vence la de ayer, no la de hoy ni la cancelada; un segundo ciclo devuelve 0 |
| Acceso | El odontólogo consulta pero no escribe | Cubierto | web/ListaEsperaApiIT.java:245 | Incluye cuerpo malformado e ID no numérico |
| Acceso | Las escrituras requieren CSRF válido | Cubierto | web/ListaEsperaApiIT.java:263 | Sin token y con token incorrecto |
| Acceso | Sin sesión no se accede a la lista de espera | Cubierto | web/ListaEsperaApiIT.java:284 | Las 4 operaciones |

18 de 18 escenarios cubiertos. Además hay tests de apoyo que no corresponden a escenarios:

- `domain/EntradaListaEsperaTest.java:51` y `:57`: invariantes de la entrada.
- `infrastructure/ListaEsperaJpaIT.java`: tipos de columna, persistencia sin desplazamiento, `CHECK`, FK, índice parcial y script idempotente.
- `application/VencimientoListaEsperaIT.java:49`: cron y zona del planificador.

## Ejecución de tests

- Comando: `./mvnw -q -o clean verify` en `backend/`, con Docker activo. Se ejecutó después del último cambio de código del change.
- Resultado: código de salida 0.
  - Surefire (unitarios): 115 tests, 0 fallidos, 0 errores, 0 saltados.
  - Failsafe (integración con Testcontainers): 371 tests, 0 fallidos, 0 errores, 0 saltados.
- Una corrida anterior tuvo 4 fallos en `CatalogoPrestacionesIT` y `CatalogoPrestacionesServicioIT`. La causa era que `ListaEsperaJpaIT` dejaba prestaciones ficticias confirmadas en la base. Se corrigió con una limpieza en `@AfterEach`, y la corrida que figura arriba es posterior a ese arreglo.
- Frontend: no se ejecutó `vitest`, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

- **RN-21:**
  - prestación obligatoria;
  - una sola entrada `ACTIVA` por paciente y prestación, con validación en Java y un índice único parcial en PostgreSQL, más el test de concurrencia;
  - paciente con email (PA-16);
  - vencimiento por `fechaHasta` pasada.
- **RN-14:** "hoy" se calcula con el `Clock` inyectable en `America/Argentina/Buenos_Aires`; el cron usa esa zona.
- **RN-33:** el odontólogo solo lee y recibe 403 al escribir, antes de que se valide el cuerpo.
- **RN-35:** la respuesta lleva solo IDs y los mensajes de error no incluyen datos del paciente.
- **Reglas duras:**
  - Solo hay datos ficticios: "Ana Ficticia", emails `@correo.test`, matrículas `MP-…`.
  - No hay secretos en el diff; `application-test.yml` solo agrega el cron desactivado.
  - No se usa Flyway, JWT ni H2, ni se expone ninguna entidad JPA (los DTO son `SolicitudAlta` y `RespuestaEntrada`).
  - El JPQL es parametrizado.
  - Las fechas son `LocalDate` de pared y `creadoEn` es `Instant` (`timestamptz`).

## Diferencias con design.md

- Ninguna bloqueante ni mayor.
- **Menor:** el `400` por `fechaHasta` anterior a hoy se lanza desde el servicio como `ResponseStatusException(BAD_REQUEST)`, porque `EstadoError` no tiene 400. El manejador existente lo devuelve como Problem Details. Es coherente con la decisión 5, que pedía validarlo en el servicio.
- **Menor:** los `CHECK` de fechas y franja se declaran en los campos y no en la clase, por la misma limitación de Hibernate que se vio en C-11.

## Qué NO se pudo verificar

- La ejecución real del cron a las 00:05: se verificó su configuración (cron y zona) y el método que invoca, pero no se esperó a que se disparara.
- La integración con C-31, C-36 y C-42, que todavía no existen. `asignar` solo está probado a nivel de dominio.

## Próximo paso

Commit, PR desde `valentino` a `master` (lo revisa Juan Pablo) y, después del merge, `/opsx:archive c-18-lista-espera-alta-y-gestion`. Para Juan Pablo en el PR:

- Revisar que se haya agregado `@EnableScheduling`, porque es la primera tarea programada del proyecto.
- Revisar el índice parcial en `schema-constraints.sql`.

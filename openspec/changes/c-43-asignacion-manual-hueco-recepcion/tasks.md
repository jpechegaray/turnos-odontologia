# Tasks

> TDD estricto: cada tarea arranca con el test en rojo. Todo test lleva `@DisplayName` con el nombre exacto del escenario de `specs/asignacion-hueco/spec.md`. Stack de tests: JUnit 5 + Spring Boot Test + AssertJ + Testcontainers (PostgreSQL real) + MockMvc. No aplica Vitest: este change es backend puro y el frontend de la lista de espera es C-47.

## 1. Persistencia y dominio de apoyo

- [x] 1.1 Agregar `bloquear(id)` (`@Lock(PESSIMISTIC_WRITE)`) a `ListaEsperaJpaRepository` y exponerlo en `ListaEsperaPersistencia`; verificar con un IT de `listaespera.infrastructure` que `bloquear` devuelve la entrada y que `guardar` posterior persiste el nuevo estado.
- [x] 1.2 Agregar `OfertaHueco.tomar()` al dominio (solo desde `ENVIADA`) con test unitario que verifica que una oferta enviada pasa a `TOMADA` y que las `VENCIDA` o ya `TOMADA` no cambian.
- [x] 1.3 Agregar `tomarDelHueco(profesionalId, huecoInicio)` (`@Modifying` JPQL, `ENVIADA → TOMADA`) a `OfertaHuecoJpaRepository` y exponerlo en `OfertasHuecoPersistencia`; verificar con un IT de `listaespera.oferta.infrastructure` que solo toma las ofertas `ENVIADA` del mismo profesional y `huecoInicio`, no toca las de otro inicio y es idempotente al repetirse.

## 2. Servicio de aplicación de la asignación

- [x] 2.1 Crear `listaespera.asignacion.application.AsignarHuecoService` y su `SolicitudAsignacion` (`profesionalId`, `inicio`, `sillonId` opcional, `notas` opcional): cargar la entrada, resolver la prestación y construir el `Hueco` `[inicio, inicio + duracionDefaultMin)`; verificar con test unitario (puertos mockeados) que la duración sale de la prestación de la entrada y no del cuerpo.
- [x] 2.2 Validar la compatibilidad (RN-22) con `CompatibilidadListaEspera.esCompatible`, `ConsultaPrestaciones` y `ProfesionalRealizaPrestacion`; verificar con test unitario que una entrada incompatible lanza `ReglaNegocioException("ENTRADA_INCOMPATIBLE", 422)` y que una compatible continúa.
- [x] 2.3 Implementar la resolución del sillón (design D4): `sillonId` indicado, si no el `sillonSugeridoId` de una oferta del mismo `(profesionalId, huecoInicio)` que siga libre vía `DisponibilidadService.consultar`, si no el primero activo libre; verificar con tests unitarios (mock de `DisponibilidadService`) los tres caminos y el caso "sin `HorarioDisponible` en el inicio" (usa el sillón de respaldo y deja el error exacto al alta).
- [x] 2.4 Orquestar la transacción: `@Transactional`, `@PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")`, `listaEspera.bloquear`, verificar `ACTIVA`, `CrearTurnoService.crear(...)` con el `creadoPorUsuarioId`, `listaEspera.guardar(entrada.asignar(turno.id()))` y `ofertas.tomarDelHueco(...)`; la verificación de este paso la dan los IT de la sección 3.

## 3. Endpoint, transacción y competición

- [ ] 3.1 Crear `listaespera.asignacion.web.AsignarHuecoController` (`POST /api/lista-espera/{id}/asignar`) y `SolicitudAsignarHueco` (`inicio` `OffsetDateTime` obligatorio, `profesionalId` obligatorio, `sillonId` y `notas` opcionales con `@Size(max = 1000)`), respondiendo `201` con `agenda.web.RespuestaTurno` y `Location` al turno; verificar con `@DisplayName("Recepción asigna un hueco a una entrada compatible")` y `@DisplayName("La asignación guarda las notas del turno")`.
- [ ] 3.2 Cubrir el contrato de entrada con `@DisplayName("El cuerpo exige profesional e inicio con zona")`: sin `profesionalId`, sin `inicio` o `inicio` sin zona dan `400` en Problem Details y no crean turno.
- [ ] 3.3 Cubrir las validaciones de la entrada con `@DisplayName("Una entrada inexistente responde 404")`, `@DisplayName("Una entrada que ya no está activa responde 409")` (código `ENTRADA_NO_ACTIVA`) y `@DisplayName("Una entrada incompatible con el hueco responde 422")` (código `ENTRADA_INCOMPATIBLE`).
- [ ] 3.4 Cubrir la duración y el sillón con `@DisplayName("La duración del turno es la de la prestación de la entrada")`, `@DisplayName("Sin sillón se prefiere el sugerido por la oferta si sigue libre")`, `@DisplayName("Sin sillón libre sugerido se usa el primero activo libre")` y `@DisplayName("Un sillón indicado ocupado responde 409")` (código `RN-01`).
- [ ] 3.5 Cubrir la competición y el cierre de ofertas con `@DisplayName("Una segunda asignación del mismo hueco responde 409")` (código `RN-01`, sin segundo turno y segunda entrada `ACTIVA`), `@DisplayName("Las ofertas del hueco pasan a TOMADA")` y `@DisplayName("Sin ofertas la asignación igual funciona")`.
- [ ] 3.6 Cubrir la atomicidad con `@DisplayName("Un fallo al crear el turno no deja la entrada cerrada ni las ofertas tomadas")`: forzar el fallo del alta (por ejemplo, hueco fuera de horario o sillón ocupado) y verificar que la entrada sigue `ACTIVA` sin `turnoAsignadoId` y las ofertas siguen `ENVIADA`.

## 4. Seguridad

- [ ] 4.1 Agregar en `SecurityConfig` la regla explícita `POST /api/lista-espera/{id}/asignar` con `hasAnyRole("ADMIN", "RECEPCION")`, antes de la regla genérica de `POST /api/**`; verificar con `@DisplayName("El odontólogo no asigna huecos")` (403 con cuerpo malformado) y `@DisplayName("Sin sesión no se asigna un hueco")` (401).
- [ ] 4.2 Verificar `@DisplayName("Las escrituras requieren CSRF válido")`: `ADMIN`/`RECEPCION` sin CSRF reciben `403` y la entrada y las ofertas no cambian.

## 5. Integración y cierre

- [ ] 5.1 `mvn verify` en verde (backend completo, incluido `ArquitecturaTest`) y verificar que no hay cambios de esquema ni de `application.yml`.
- [ ] 5.2 `openspec validate c-43-asignacion-manual-hueco-recepcion --strict` en verde.
- [ ] 5.3 Prueba manual en dev con datos ficticios propios: anotar una entrada, liberar un hueco, asignarlo por `POST /api/lista-espera/{id}/asignar` y comprobar `201`, entrada `ASIGNADA` y ofertas `TOMADA`; borrar los datos de prueba al terminar.
- [ ] 5.4 Ejecutar `openspec-verify` y dejar `verify.md` con el veredicto.

## Workflow follow-up

- Abrir el PR a `master` con el ID `C-43`; lo revisa Juan Pablo.
- Archivar el change después del merge y actualizar el estado de C-43 en `CHANGES.md`.

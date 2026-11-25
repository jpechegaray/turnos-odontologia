# Tasks

## 1. Puerto y persistencia de consulta de turnos activos en `agenda`

- [x] 1.1 Crear `ar.edu.utn.turnos.agenda.application.TurnosActivosConsulta` con el record `TurnoEnConflicto(Long id, Instant inicio, Instant fin, String estado)` y los métodos `enIntervaloDeProfesional`, `futurosDeProfesional`, `futurosDeSillon` y `futurosDePrestacion` en archivo nuevo, verificando que compila sin tocar otros archivos de `agenda`.
- [x] 1.2 Implementar `ar.edu.utn.turnos.agenda.infrastructure.TurnosActivosPersistencia` con `EntityManager` y `Clock` inyectable (`estado IN (PROGRAMADO, CONFIRMADO)`, solapamiento `[inicio, fin)` y `fin > ahora`, ordenados por `inicio ASC, id ASC`), y verificar con `TurnosActivosPersistenciaIT` (Testcontainers) los escenarios `@DisplayName("La consulta por intervalo incluye solo turnos programados o confirmados que se solapan")` y `@DisplayName("La consulta de turnos futuros activos filtra por fin posterior al reloj actual")`.

## 2. Guarda de integridad al crear bloqueos de agenda (RN-17)

- [x] 2.1 Inyectar `TurnosActivosConsulta` en `ServicioBloqueos` y rechazar en `crear` cuando `enIntervaloDeProfesional` no esté vacía lanzando `ReglaNegocioException` (`CONFLICTO_409`, `codigo = "RN-17"`, `Map.of("turnos", conflictos)`), verificando con tests unitarios que los tests previos de `ServicioBloqueos` siguen en verde.
- [x] 2.2 Agregar tests de integración con Testcontainers para bloqueos verificando los escenarios `@DisplayName("Crear un bloqueo superpuesto con turnos activos devuelve 409 con la lista de conflictos")`, `@DisplayName("Crear un bloqueo contiguo a turnos activos se acepta")` y `@DisplayName("Cancelar el turno activo permite crear el bloqueo antes rechazado")`.

## 3. Guarda de integridad al desactivar sillones, profesionales y prestaciones (RN-37)

- [x] 3.1 Inyectar `TurnosActivosConsulta` en `ServicioSillones` y validar en `desactivar` que `futurosDeSillon(id)` esté vacía (o lanzar `409` con `codigo = "RN-37"`, `recurso = "SILLON"`, `turnos`), verificando en integración los escenarios `@DisplayName("Desactivar un sillón con turnos futuros activos devuelve 409 con la lista de conflictos")` y `@DisplayName("Cancelar los turnos futuros permite desactivar el sillón")`.
- [x] 3.2 Inyectar `TurnosActivosConsulta` en `ServicioProfesionales` y validar en `actualizar` cuando pasa de `activo: true -> false` que `futurosDeProfesional(id)` esté vacía (o lanzar `409` con `codigo = "RN-37"`, `recurso = "PROFESIONAL"`, `turnos`), verificando en integración los escenarios `@DisplayName("Desactivar un profesional con turnos futuros activos devuelve 409 con la lista de conflictos")` y `@DisplayName("Cancelar los turnos futuros permite desactivar al profesional")`.
- [x] 3.3 Inyectar `TurnosActivosConsulta` en `CatalogoPrestaciones` y validar tanto en `darDeBaja` como en `modificar` (cuando pasa de `activa: true -> false`) que `futurosDePrestacion(id)` esté vacía (o lanzar `409` con `codigo = "RN-37"`, `recurso = "PRESTACION"`, `turnos`), verificando en integración los escenarios `@DisplayName("Dar de baja o desactivar una prestación con turnos futuros activos devuelve 409 con la lista de conflictos")` y `@DisplayName("Cancelar los turnos futuros permite dar de baja la prestación")`.

## 4. Verificación integral del backend

- [x] 4.1 Ejecutar la suite completa del backend (`mvn verify` en `backend/`) y comprobar que todos los tests unitarios, de arquitectura (`ArquitecturaTest`) y de integración con Testcontainers pasan en verde sin regresiones.

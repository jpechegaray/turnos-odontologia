# Tasks

> Governance HIGH: no se escribe código hasta que Lionel revise y apruebe este plan. TDD estricto: cada test primero en rojo. Todo test lleva `@DisplayName` con el nombre exacto del escenario de `specs/reserva-publica/spec.md`. Issue #38.

## 1. Puertos de lectura

- [x] 1.1 Agregar `ConsultaPrestaciones.listarActivas()` con su implementación en `CatalogoPrestaciones` (JPQL parametrizado); verificar con un test de persistencia (Testcontainers) que devuelve solo activas
- [x] 1.2 Agregar `ProfesionalRealizaPrestacion.profesionalesQueRealizan(prestacionId)` que devuelve, en una sola consulta JPQL, los ids de los profesionales asociados (el filtro por activo lo hace la capa de aplicación con `ProfesionalConsulta.listarActivos()`, sin N+1); verificar con test de persistencia
- [x] 1.3 Agregar `ProfesionalRealizaPrestacion.prestacionesConProfesional(ids)` que devuelve, en una sola consulta, los ids de las prestaciones que realiza alguno de los profesionales dados (vacío sin consulta si no hay ids); verificar con test de persistencia

## 2. Catálogo público

- [x] 2.1 Crear DTOs `record` `PrestacionPublicaDto(id, nombre, duracionMin)` y `ProfesionalPublicoDto(id, nombre, apellido, especialidad)` en `reservapublica.web`
- [x] 2.2 Implementar `GET /api/public/prestaciones`; verificar con IT sin sesión los escenarios "Se listan solo prestaciones activas con profesional activo" y "Un profesional inactivo no hace reservable una prestación", y que el JSON no tiene campos extra
- [x] 2.3 Implementar `GET /api/public/profesionales?prestacionId=`; verificar con IT "Se listan profesionales activos de la prestación sin matrícula", "Una prestación inexistente o inactiva devuelve lista vacía" y "Falta el parámetro" (400, también con id mal formado)

## 3. Disponibilidad pública

- [x] 3.1 Crear `reservapublica.application.DisponibilidadPublica` (sin `@PreAuthorize`) con el orden: validar rango pedido → validar recursos → recortar con `ConfiguracionServicio` leído en cada consulta → núcleo con `duracionMin = null` → resultado sin sillón; verificar con tests unitarios del orden (rango inválido antes que recurso; recurso inactivo con rango fuera del horizonte da 404)
- [x] 3.2 Lanzar 404 `RECURSO_NO_DISPONIBLE` con mensaje único para los tres casos y traducir defensivamente `PROFESIONAL_INEXISTENTE`, `PROFESIONAL_INACTIVO`, `PRESTACION_INEXISTENTE`, `PRESTACION_INACTIVA` y `RN-11` del núcleo; verificar con test unitario que simula cada código del núcleo
- [x] 3.3 Crear `HorarioPublicoDto(inicio, fin)` e implementar `GET /api/public/disponibilidad` con `desde`/`hasta` `OffsetDateTime`; verificar con IT "Un horario ocupado no se ofrece", "Un horario contiguo a un turno sí se ofrece", "El JSON tiene solo inicio y fin" y "Parámetros ausentes o mal formados"
- [x] 3.4 Verificar con IT los escenarios de rango: "Fin no posterior al inicio", "Rango de más de 31 días" y "Rango válido fuera del horizonte no es un error de rango"
- [x] 3.5 Verificar con IT y `@Import(RelojFijoConfig.class)` "Borde de anticipación mínima", "Borde del horizonte" y "Un cambio de configuración aplica a la siguiente consulta"
- [x] 3.6 Verificar con IT "Los tres casos dan la misma respuesta" (cuerpo idéntico salvo `instance`), "Un profesional inactivo con rango fuera del horizonte da 404" y "La consulta del personal conserva sus errores detallados" (`/api/agenda/disponibilidad` sigue con 422 específicos)

## 4. Rate limit

- [x] 4.1 Crear una clase IT con `app.ratelimit.max-solicitudes-por-ip=2` que verifique "Se supera el límite de solicitudes" (tercera solicitud a un endpoint de reserva pública → 429 con `Retry-After`)

## 5. Integración

- [x] 5.1 Correr `mvn verify` en `backend/` y confirmar que todo pasa, incluidos los tests de `agenda-disponibilidad` existentes
- [x] 5.2 Correr `openspec validate c-38-reserva-publica-disponibilidad --strict` sin errores ni avisos

## Workflow follow-up

- Abrir el PR desde la rama `lionel` (lo revisa Juan Pablo), con referencia al issue #38.
- Tras el merge: `openspec-verify`, luego `/opsx:archive` y actualizar el estado de C-38 en CHANGES.md.

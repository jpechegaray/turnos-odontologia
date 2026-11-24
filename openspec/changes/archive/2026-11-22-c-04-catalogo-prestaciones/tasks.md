# Tasks

## 1. Reglas de dominio

- [x] 1.1 Implementar por TDD `DuracionPrestacion` (RN-09); verificar con `DuracionPrestacionTest` que 5 y 480 se aceptan y que 0, 7, 481 y 485 lanzan `ReglaNegocioException` con código `RN-09` y estado 422.
- [x] 1.2 Implementar por TDD `NombrePrestacion` (recorte y clave de unicidad); verificar con `NombrePrestacionTest` que " Limpieza " y "limpieza" producen la misma clave. Comprobar que `ArquitecturaTest` sigue en verde, con `domain` sin Spring ni JPA.

## 2. Persistencia

- [x] 2.1 Crear la entidad `Prestacion`, con `nombre_clave` único y `@Check` de la duración, y `PrestacionJpaRepository`; verificar en PostgreSQL (Testcontainers) que se rechaza un nombre con la misma clave y una duración inválida insertada directo por el repositorio, sin editar `schema-constraints.sql`.

## 3. Servicio y puerto de consulta

- [x] 3.1 Implementar `CatalogoPrestaciones` (alta, modificación, baja lógica idempotente, listado ordenado con `soloActivas`, búsqueda por id) con 409 `PRESTACION-DUPLICADA` (incluida la carrera traducida desde `DataIntegrityViolationException`) y 404 `PRESTACION-NO-ENCONTRADA`; verificar con tests de integración del servicio.
- [x] 3.2 Exponer `ConsultaPrestaciones` y `PrestacionResumen` en `application`; verificar los escenarios "Otro módulo consulta una prestación existente" e "Otro módulo consulta una prestación inexistente".
- [x] 3.3 Proteger la escritura con `@PreAuthorize("hasRole('ADMIN')")`; verificar que el puerto de lectura funciona sin usuario autenticado.

## 4. API REST

- [x] 4.1 Implementar `PrestacionController` con los DTOs `PrestacionSolicitud`, `PrestacionModificacion` y `PrestacionRespuesta` (Bean Validation solo de presencia y formato); verificar con MockMvc en `CatalogoPrestacionesIT` cada escenario de alta, duración, unicidad, modificación, baja y consulta, con `@DisplayName` igual al nombre del escenario.
- [x] 4.2 Verificar en `CatalogoPrestacionesIT` los escenarios de permisos: lectura de `RECEPCION` y `ODONTOLOGO`, 403 Problem Details sin cambios al escribir con esos roles (con CSRF válido) y 401 sin sesión.

## 5. Verificación de integración

- [x] 5.1 Ejecutar `mvnw.cmd verify` desde `backend/` con Docker encendido; comprobar que pasan C-01, C-03 y C-04, y que `git status` no muestra archivos existentes modificados fuera de `openspec/changes/c-04-catalogo-prestaciones/` y del módulo nuevo `prestaciones`.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Abrir el PR `C-04 catalogo-prestaciones` de la rama `valentino` a `master` (lo revisa Juan Pablo). Preguntar antes de cada commit.
- Después del merge: `/opsx:archive`.

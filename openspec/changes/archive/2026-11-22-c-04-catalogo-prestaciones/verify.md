# Verify — c-04-catalogo-prestaciones (2026-11-21)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; archivar después del merge del PR)

## Validación estructural

`openspec validate c-04-catalogo-prestaciones --strict` → `Change 'c-04-catalogo-prestaciones' is valid`.

## Tareas

9/9 completas. Ninguna pendiente.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/prestaciones/`. Cada test tiene `@DisplayName` igual al nombre del escenario. Se leyó cada test y se confirmó que afirma el THEN.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| catalogo-prestaciones | El administrador da de alta una prestación válida | Cubierto | web/CatalogoPrestacionesIT.java:48 | 201, `Location`, id, nombre, duración y `activa=true` |
| catalogo-prestaciones | Un nombre vacío responde 400 | Cubierto | web/CatalogoPrestacionesIT.java:61 | Nombre vacío y duración ausente: los dos campos aparecen en `errores` |
| catalogo-prestaciones | Una duración no múltiplo de 5 responde 422 | Cubierto | web/CatalogoPrestacionesIT.java:75; domain/DuracionPrestacionTest.java:23 | Además verifica que la prestación no se crea |
| catalogo-prestaciones | Una duración mayor a 480 responde 422 | Cubierto | web/CatalogoPrestacionesIT.java:83; domain/DuracionPrestacionTest.java:29 | `codigo` RN-09 |
| catalogo-prestaciones | Una duración menor a 5 responde 422 | Cubierto | web/CatalogoPrestacionesIT.java:90; domain/DuracionPrestacionTest.java:36 | `codigo` RN-09 |
| catalogo-prestaciones | Las duraciones límite 5 y 480 se aceptan | Cubierto | web/CatalogoPrestacionesIT.java:97; domain/DuracionPrestacionTest.java:17 | |
| catalogo-prestaciones | Un nombre repetido responde 409 | Cubierto | web/CatalogoPrestacionesIT.java:107 | " limpieza " contra "Limpieza", `PRESTACION-DUPLICADA` |
| catalogo-prestaciones | Modificar una prestación conservando su propio nombre se acepta | Cubierto | web/CatalogoPrestacionesIT.java:118 | |
| catalogo-prestaciones | El administrador modifica una prestación | Cubierto | web/CatalogoPrestacionesIT.java:131 | |
| catalogo-prestaciones | El administrador reactiva una prestación dada de baja | Cubierto | web/CatalogoPrestacionesIT.java:145 | Vuelve a aparecer con `soloActivas=true` |
| catalogo-prestaciones | Modificar una prestación inexistente responde 404 | Cubierto | web/CatalogoPrestacionesIT.java:158 | |
| catalogo-prestaciones | La baja deja la prestación inactiva pero consultable | Cubierto | web/CatalogoPrestacionesIT.java:167 | 204 y luego GET con `activa=false` |
| catalogo-prestaciones | Repetir la baja responde 204 | Cubierto | web/CatalogoPrestacionesIT.java:181 | |
| catalogo-prestaciones | Dar de baja una prestación inexistente responde 404 | Cubierto | web/CatalogoPrestacionesIT.java:191 | |
| catalogo-prestaciones | Una prestación inactiva no aparece con soloActivas | Cubierto | web/CatalogoPrestacionesIT.java:200 | |
| catalogo-prestaciones | Sin filtro el listado incluye las inactivas ordenadas por nombre | Cubierto | web/CatalogoPrestacionesIT.java:214 | Comprueba el orden y el total |
| catalogo-prestaciones | Consultar una prestación inexistente responde 404 | Cubierto | web/CatalogoPrestacionesIT.java:230 | |
| catalogo-prestaciones | Recepción lee el catálogo | Cubierto | web/CatalogoPrestacionesIT.java:239 | |
| catalogo-prestaciones | El odontólogo lee el catálogo | Cubierto | web/CatalogoPrestacionesIT.java:246 | |
| catalogo-prestaciones | Recepción no puede escribir prestaciones | Cubierto | web/CatalogoPrestacionesIT.java:253 | POST, PUT y DELETE con CSRF real dan 403 Problem Details; la prestación previa queda intacta |
| catalogo-prestaciones | El odontólogo no puede escribir prestaciones | Cubierto | web/CatalogoPrestacionesIT.java:260 | Ídem |
| catalogo-prestaciones | Sin sesión el catálogo responde 401 | Cubierto | web/CatalogoPrestacionesIT.java:266 | |
| catalogo-prestaciones | Otro módulo consulta una prestación existente | Cubierto | infrastructure/CatalogoPrestacionesServicioIT.java:111 | Prestación inactiva: devuelve nombre, duración y `activa=false` |
| catalogo-prestaciones | Otro módulo consulta una prestación inexistente | Cubierto | infrastructure/CatalogoPrestacionesServicioIT.java:127 | `Optional` vacío, sin excepción |

24/24 escenarios cubiertos (la spec tiene 24; el resumen de la propuesta decía 29 por error).

Tests adicionales, fuera de la matriz: `PrestacionIT` (UNIQUE de `nombre_clave` y CHECK `prestacion_duracion_rn09` en PostgreSQL), `NombrePrestacionTest`, `CatalogoPrestacionesTest` (la carrera por el nombre se traduce a 409) y el resto de `CatalogoPrestacionesServicioIT` (permisos del servicio, baja idempotente, orden).

## Ejecución de tests

- Comando: `./mvnw -q clean verify` desde `backend/`, con Docker Desktop 27.2.0 encendido, después de hacer rebase sobre `origin/master` (2d3c3a5, que ya incluye C-05 y C-06).
- Resultado: BUILD OK (exit 0). Surefire: 56 tests, 0 fallas, 0 errores, 0 saltados. Failsafe (Testcontainers, PostgreSQL 16): 125 tests, 0 fallas, 0 errores, 0 saltados.
- Del change: DuracionPrestacionTest 8, NombrePrestacionTest 3, CatalogoPrestacionesTest 1, PrestacionIT 3, CatalogoPrestacionesServicioIT 11, CatalogoPrestacionesIT 22. Los tests de C-01, C-03, C-05 y C-06 siguen en verde.
- Frontend (Vitest): **no ejecutado**, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

| Regla | Cobertura |
|---|---|
| RN-09 (duración múltiplo de 5 entre 5 y 480) | Dominio (422 `RN-09`), API y CHECK en la base |
| RN-03 (duración por defecto por prestación) | La prestación guarda la duración por defecto. El ajuste al dar el turno corresponde a C-19 |
| RN-37 (baja lógica) | La baja desactiva y no borra. **La guarda de turnos futuros queda diferida a C-30**, como dicen la ficha y el design |
| RN-33 (permisos por rol) | Solo ADMIN escribe; RECEPCION y ODONTOLOGO reciben 403 |

Reglas duras revisadas sobre los archivos nuevos (el change solo agrega archivos; `git diff HEAD` sobre archivos versionados está vacío):
- Sin datos de pacientes: los fixtures son solo nombres de prestaciones ficticias.
- Sin secretos ni credenciales: la única coincidencia de "token" es la cookie CSRF de prueba.
- Sin Flyway ni H2: el esquema lo genera Hibernate y los tests corren sobre PostgreSQL real.
- La API no expone la entidad JPA: se usan los DTOs `PrestacionSolicitud`, `PrestacionModificacion` y `PrestacionRespuesta`.
- La capa `domain` no depende de Spring ni de JPA: `ArquitecturaTest` está en verde.
- No se modificó ningún archivo compartido (`pom.xml`, `application*.yml`, `schema-constraints.sql`, `shared/`, `SecurityConfig`).

## Diferencias con design.md

- **Menor:** el PUT usa un DTO propio, `PrestacionModificacion` (incluye `activa`). El design solo nombraba `PrestacionSolicitud` y `PrestacionRespuesta`; tasks.md 4.1 ya lo refleja.
- **Menor:** además de `CatalogoPrestacionesIT` hay tests de persistencia y de servicio separados (`PrestacionIT`, `CatalogoPrestacionesServicioIT`, `CatalogoPrestacionesTest`). Suman cobertura y no cambian el diseño.
- **Menor:** los tests de escritura obtienen el token CSRF real (`GET /api/auth/csrf`, cookie más cabecera `X-XSRF-TOKEN`), porque `csrf()` de spring-security-test no es compatible con el `CsrfTokenRequestHandler` de C-03. No afecta la API.

No hay diferencias mayores ni bloqueantes.

## Qué NO se pudo verificar

- El arranque manual con el perfil `dev` contra la base de `docker compose` (`ddl-auto: update`). Solo se probó el perfil `test` (`create-drop`).
- Una carrera real entre dos altas simultáneas contra PostgreSQL. La traducción a 409 se probó con un mock del repositorio y el UNIQUE con PostgreSQL por separado.
- El documento OpenAPI generado por springdoc. No se revisó, pero no forma parte de la spec.

## Próximo paso

1. Commit y PR `C-04 catalogo-prestaciones` de `valentino` a `master`, que revisa Juan Pablo.
2. Después del merge: `/opsx:archive c-04-catalogo-prestaciones`.

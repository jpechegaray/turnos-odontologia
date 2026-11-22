# Design

## Context

C-01 dejó el `ManejadorErrores` (traduce cualquier `ReglaNegocioException` a Problem Details), el `Clock`, la base de tests con Testcontainers y ArchUnit (`..domain..` sin Spring ni JPA). C-03 dejó `SecurityConfig` con denegación por defecto (`anyRequest().authenticated()`), CSRF por cookie y `@EnableMethodSecurity`. El módulo `seguridad` fija el patrón del repo: entidad JPA y servicio en `infrastructure`, DTOs como `record` en `web`, reglas puras en `domain`. Requisitos en `specs/catalogo-prestaciones/spec.md`.

## Goals / Non-Goals

**Goals:**
- Módulo `ar.edu.utn.turnos.prestaciones` autocontenido, siguiendo el mismo patrón que `seguridad`.
- **No modificar ningún archivo existente** del proyecto: ni `pom.xml`, `application.yml`, `schema-constraints.sql`, `shared/` ni `SecurityConfig`. Todo lo necesario ya está disponible.

**Non-Goals:**
- Endpoint público para la reserva online (C-38) y bloqueo de la baja con turnos futuros (C-30).
- Paginación: el catálogo de un consultorio tiene decenas de filas como mucho.

## Decisions

### Estructura del módulo
- `domain/DuracionPrestacion`: value object (`record`) que valida RN-09 y lanza `ReglaNegocioException("RN-09", NO_PROCESABLE_422, …)`. Es Java puro y cumple la regla ArchUnit.
- `domain/NombrePrestacion`: normaliza el nombre (recorta espacios) y calcula su clave de unicidad (minúsculas y sin espacios en los extremos).
- `application/ConsultaPrestaciones` + `application/PrestacionResumen`: puerto de solo lectura para otros módulos (`Optional<PrestacionResumen> buscarPorId(Long id)`).
- `infrastructure/Prestacion` (entidad, package-private como `Usuario`), `PrestacionJpaRepository` y `CatalogoPrestaciones` (`@Service`, implementa el puerto y las operaciones de escritura).
- `web/PrestacionController` con DTOs `PrestacionSolicitud` y `PrestacionRespuesta`. La API nunca expone la entidad.

*Alternativa descartada:* repositorio como interfaz de dominio con adaptador JPA. Para un CRUD LOW suma piezas sin beneficio, y se aparta de lo que hizo `seguridad`.

### Autorización
`@PreAuthorize("hasRole('ADMIN')")` en los métodos de escritura de `CatalogoPrestaciones`. Las lecturas no llevan anotación: la cadena de C-03 ya exige sesión en `/api/prestaciones/**` (401), y el puerto `ConsultaPrestaciones` tiene que poder usarse desde flujos sin sesión (la reserva pública de C-38). Así no hay que tocar `SecurityConfig`.

### 422 de RN-09 contra 400 de Bean Validation
Bean Validation en el DTO cubre solo presencia y formato: `@NotBlank @Size(max = 100)` para el nombre, `@NotNull` para la duración y `activa` en el PUT. Esos casos dan 400. El rango y el múltiplo de 5 son regla de negocio en `DuracionPrestacion`, que da 422 con `RN-09`, como pide el criterio de salida de CHANGES.md ("duración 7 o 481 devuelve 422").

### Unicidad del nombre
Se agrega la columna `nombre_clave` (minúsculas y recortado) con `unique = true`, que Hibernate genera solo (DD-08), así que no hace falta un índice funcional en `schema-constraints.sql`. El servicio verifica antes con `existsByNombreClave…` para responder 409 `PRESTACION-DUPLICADA` con un mensaje claro. Si dos altas compiten, la `DataIntegrityViolationException` se traduce dentro del servicio a la misma excepción.

### Defensa en base para la duración
La entidad declara `@org.hibernate.annotations.Check` con `duracion_default_min BETWEEN 5 AND 480 AND duracion_default_min % 5 = 0` (KB 04 §Prestacion). Hibernate la genera con `ddl-auto`, sin editar el script compartido.

### Contrato HTTP
| Operación | Respuesta |
|---|---|
| `GET /api/prestaciones?soloActivas=` | 200, lista ordenada por nombre; `soloActivas` vale `false` por defecto |
| `GET /api/prestaciones/{id}` | 200 / 404 |
| `POST /api/prestaciones` `{nombre, duracionDefaultMin}` | 201 + `Location` |
| `PUT /api/prestaciones/{id}` `{nombre, duracionDefaultMin, activa}` | 200 / 404; reemplazo completo, permite reactivar |
| `DELETE /api/prestaciones/{id}` | 204 (idempotente) / 404 |

Respuesta: `{id, nombre, duracionDefaultMin, activa}`. Los códigos que no son RN (`PRESTACION-DUPLICADA` y `PRESTACION-NO-ENCONTRADA`) usan la misma `ReglaNegocioException`, de modo que el `ManejadorErrores` no cambia.

### Tests
- Unitarios: `DuracionPrestacionTest` (bordes 0, 5, 7, 480, 481, 485) y `NombrePrestacionTest`.
- Integración (`PruebaIntegracionBase`, PostgreSQL real): `CatalogoPrestacionesIT` vía MockMvc con `@WithMockUser(roles = …)` y `csrf()`, uno o más tests por escenario de la spec con `@DisplayName` igual al nombre del escenario; prueba del puerto `ConsultaPrestaciones`; y una prueba de que el `CHECK` de la base rechaza una duración inválida insertada por el repositorio.

## Risks / Trade-offs

- [Columna derivada `nombre_clave` que puede desincronizarse] → solo la asigna la entidad en su constructor y su método de modificación, nunca desde afuera.
- [`ddl-auto: update` no agrega el `CHECK` a una tabla que ya existe en una base de desarrollo] → la tabla es nueva en este change; ante cambios futuros se recrea la base ficticia (PA-10).
- [La baja no verifica turnos futuros] → aceptado; lo cierra C-30 (RN-37), como indica la ficha.

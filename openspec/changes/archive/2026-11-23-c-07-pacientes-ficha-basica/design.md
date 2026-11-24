# Design

## Context

La motivación está en `proposal.md` §Why y el comportamiento exigido en `specs/gestion-pacientes/spec.md`. Este documento explica cómo se cumple.

Situación actual y restricciones:

- C-01 ya dejó el backend con Spring Boot 3.5.16, Java 21, PostgreSQL real en tests (Testcontainers) y `./mvnw verify` en verde. No existe ningún módulo de negocio todavía.
- `shared/error` provee `ReglaNegocioException` y el enum `EstadoError` (403, 404, 409, 422). `shared/web/ManejadorErrores` ya traduce esa excepción a Problem Details con `codigo` y agrega la lista `errores` a los 400 de Bean Validation.
- `shared/tiempo` provee un `Clock` con zona `America/Argentina/Buenos_Aires`. Se inyecta por constructor; no se llama a `Instant.now()` sin reloj.
- `seguridad/infrastructure/SecurityConfig` es denegar por defecto, tiene `@EnableMethodSecurity` y deja `/api/public/**` abierto. El login y los roles persistidos llegan en C-03; los tests usan `@WithMockUser` o `SecurityMockMvcRequestPostProcessors.user(...)`, como ya hace `SecurityConfigTest`.
- `ArquitecturaTest` prohíbe que `..domain..` dependa de Spring, JPA o Hibernate. Por eso la entidad JPA no puede vivir en `domain`.
- KB 08 ubica las entidades JPA en `infrastructure` (así lo hace el ejemplo de `agenda`) y dice que cada módulo tiene su propio paquete y sus propias entidades. KB 04 §Paciente fija los atributos, la unicidad del DNI y el medio de contacto.
- Reglas duras aplicables: 1 (datos ficticios), 5 (solo los solapamientos exigen garantía en la base), 6 (horas con zona; acá solo hay `Instant` de creación y consentimiento), 8 (sesión, no JWT), 9 (DTOs, JPQL parametrizado, sin entidades en la API), 10 (PostgreSQL real) y 11 (`@DisplayName` con el nombre exacto del escenario).
- Skills aplicadas: `springboot-tdd` (RED-GREEN-refactor, `*Test` unitarios y `*IT` de integración), `jpa-patterns` (entidad, índices, JPQL, `saveAndFlush`), `postgresql-table-design` (unique nullable, índices) y `springboot-security` (autorización por método, sin filtrar datos). Choca con DD-08 (Flyway) y DD-03 (JWT); no se aplican.

## Goals / Non-Goals

**Goals:**
- Un módulo `pacientes` autocontenido que respete la separación `domain` / `application` / `infrastructure` / `web` y pase ArchUnit.
- Una API estable para C-23 (front) y un puerto de lectura estable para C-19, C-18 y C-41.
- Dejar la ficha lista para que C-41 agregue el consentimiento y C-19 la protección de pacientes con turnos, sin reescribir el módulo.

**Non-Goals:**
- Reactivación de fichas, paginación, orden configurable y búsqueda por texto libre único: no están en el plan.
- Validación de formato de email, DNI o teléfono: RN-32 no la pide. El front de C-23 puede validar formato en el formulario.
- Consentimiento de datos (texto, captura y fecha): lo define y captura C-41. Acá solo se reserva la columna.
- Lectura del odontólogo limitada a sus pacientes: C-28.

## Decisions

### D1. Paquete `pacientes` con cuatro capas

```
backend/src/main/java/ar/edu/utn/turnos/pacientes/
├── domain/
│   ├── Paciente.java              # record inmutable, reglas RN-32, sin Spring ni JPA
│   └── PacienteRepositorio.java   # puerto de persistencia
├── application/
│   ├── PacienteConsulta.java      # puerto de lectura para otros módulos
│   └── PacienteServicio.java      # casos de uso; implementa PacienteConsulta
├── infrastructure/
│   ├── PacienteJpa.java           # @Entity, mapeo a/desde dominio
│   ├── PacienteRepositorioJpa.java# Spring Data + JPQL de búsqueda
│   └── PacienteRepositorioAdapter.java
└── web/
    ├── PacienteController.java
    ├── PacienteRequest.java
    └── PacienteResponse.java
```

- La entidad JPA va en `infrastructure` porque `domain` no puede importar `jakarta.persistence` (ArchUnit). El dominio queda como un `record` puro y la entidad se mapea con `aDominio()` / `desdeDominio(...)`.
- **Alternativa descartada:** un paquete plano `pacientes/Paciente.java` con `@Entity` (lo permitiría KB 08 al no exigir subpaquetes para este módulo). Se descarta porque C-19 va a necesitar reglas de dominio y porque deja el módulo sin la separación que el propio C-01 declaró como objetivo.
- **Alternativa descartada:** usar la entidad JPA como modelo en `application` y `web`. Se descarta por la regla dura 9 y porque acopla los DTOs al esquema.

### D2. `Paciente` como record inmutable con normalización y RN-32

- Campos: `id`, `nombre`, `apellido`, `dni`, `email`, `telefono`, `obraSocial`, `nroAfiliado`, `notas`, `activo`, `consentimientoDatosEn`, `creadoEn` (los dos últimos `Instant`).
- El constructor compacto normaliza: `trim` en todos los textos y `null` para los opcionales en blanco. Así un DNI `"  "` es "no informado" y un email vacío no cuenta como contacto.
- Valida en el dominio: `nombre` y `apellido` no vacíos; al menos `email` o `telefono`; si falla, lanza `ReglaNegocioException("RN-32", NO_PROCESABLE_422, ...)`. El mensaje no lleva datos del paciente.
- Métodos de copia: `nuevo(...)`, `conCreacion(Instant)`, `conDatos(...)` y `dadoDeBaja()`. `conDatos` preserva `id`, `activo`, `consentimientoDatosEn` y `creadoEn`.
- **Alternativa descartada:** validar solo con Bean Validation en el DTO. Se descarta porque el puerto de lectura y C-41 también construyen fichas y no pasan por el controlador.
- **Alternativa descartada:** una clase mutable con setters. Se descarta porque permite estados inválidos y complica el mapeo.

### D3. Persistencia: entidad JPA, unique del DNI e índices

- `PacienteJpa` mapea la tabla `pacientes` con `@Table(name = "pacientes")`, sin unique sobre el DNI en el mapeo (Hibernate no genera índices parciales).
- `dni` es nullable. La unicidad es un índice único parcial `uk_paciente_dni_activo ON pacientes (dni) WHERE activo = true AND dni IS NOT NULL`: el DNI es único solo entre fichas activas y solo cuando se informa.
- `notas` usa `columnDefinition = "text"`. `creado_en` y `consentimiento_datos_en` son `Instant` (Hibernate 6 los mapea a `timestamp with time zone`).
- `creado_en` es `nullable = false` y `updatable = false`; lo setea el servicio con el `Clock`.
- `consentimiento_datos_en` queda nullable y C-07 no lo escribe: lo completa C-41.
- **Se agrega un bloque idempotente a `schema-constraints.sql`** (DD-08, corre después de Hibernate): `ALTER TABLE IF EXISTS pacientes DROP CONSTRAINT IF EXISTS uk_paciente_dni`, lo mismo para `pacientes_dni_key`, y `CREATE UNIQUE INDEX IF NOT EXISTS uk_paciente_dni_activo ... WHERE activo = true AND dni IS NOT NULL`. Los `DROP` hacen que las bases de dev que ya tenían la unique completa converjan sin `docker compose down -v`. Correrlo dos veces no falla. La regla "al menos un contacto" queda en el dominio y el servicio: un `CHECK` obligaría a editar un archivo compartido por un caso que el dominio ya cubre.
- **Decisión del líder (2026-11-23):** una ficha dada de baja (`activo = false`) NO reserva su DNI. Reemplaza la decisión previa de que el DNI de una ficha inactiva siguiera ocupado.
- **Riesgo asumido:** si el script no corriera, la base no tendría la garantía; el pre-chequeo del servicio igual devuelve 409 entre fichas activas. El índice es la red de seguridad ante carreras.

### D4. Servicio y puerto de lectura; la seguridad va en el controlador

- `PacienteServicio` implementa los casos de uso y `PacienteConsulta`:
  - `crear(Paciente)`: si hay DNI, verifica `existePorDni` (solo cuenta fichas ACTIVAS: `existsByDniAndActivoTrue`); setea `creadoEn` con `Instant.now(reloj)`; guarda. Si la base rechaza por unicidad, traduce a 409 RN-32.
  - `actualizar(Long, Paciente)`: carga la ficha (404 si no existe), verifica el DNI si cambió, aplica `conDatos` y guarda. Si la base rechaza, 409.
  - `obtener(Long)`: 404 si no existe; devuelve activas e inactivas.
  - `buscar(...)`: delega en el repositorio con los filtros normalizados.
  - `darDeBaja(Long)`: carga, marca `activo = false` y guarda. Si ya estaba inactiva, es idempotente. 404 si no existe.
  - `porId(Long)` y `porEmail(String)` (el puerto): solo lectura, devuelven activas e inactivas.
- **La autorización va en `PacienteController` con `@PreAuthorize`, no en el servicio.** El puerto `PacienteConsulta` lo va a invocar C-41 desde una petición anónima (`/api/public/reservas`); si el servicio tuviera `@PreAuthorize`, la reserva pública fallaría. La seguridad en el borde HTTP es suficiente porque ningún otro camino llega al servicio con datos de un usuario no confiable.
  - **Desvío respecto de KB 03** ("por método de servicio y reglas por ruta"): se documenta acá. La alternativa era un segundo servicio de lectura sin seguridad, que duplica el mapeo; se prefiere una sola implementación y la anotación en el controlador.
- `PacienteServicio` captura `DataIntegrityViolationException` de `saveAndFlush`. En la tabla solo hay una restricción única (el índice parcial del DNI entre fichas activas), así que cualquier violación de integridad en el alta/edición es un DNI repetido. Se documenta esa suposición.

### D5. Contrato HTTP

| Método | Ruta | Rol | Éxito |
|---|---|---|---|
| `POST` | `/api/pacientes` | RECEPCION, ADMIN | 201 + ficha |
| `PUT` | `/api/pacientes/{id}` | RECEPCION, ADMIN | 200 + ficha |
| `GET` | `/api/pacientes/{id}` | RECEPCION, ADMIN | 200 + ficha |
| `GET` | `/api/pacientes?apellido=&nombre=&dni=&email=` | RECEPCION, ADMIN | 200 + lista |
| `DELETE` | `/api/pacientes/{id}` | **solo ADMIN** | 204 |

- `PacienteRequest` lleva `@NotBlank` en nombre y apellido y `@Size` con los largos de las columnas. No lleva `@Email` (ver Non-Goals).
- `PUT` es reemplazo completo: un campo omitido o vacío se guarda como `null`, salvo nombre y apellido. Eso permite borrar el email e informar un teléfono en la misma edición.
- `POST` siempre crea con `activo = true`; no se acepta `activo` en el request. La baja solo se hace con `DELETE`.
- `PacienteRequest` y `PacienteResponse` incluyen `nroAfiliado` (opcional, máx. 50) y `notas` (opcional, texto limitado a 1000 caracteres). La respuesta incluye además `creadoEn` (`Instant` UTC, lo setea el servicio al crear y no cambia al editar). **`consentimientoDatosEn` no está en el request ni en la respuesta**: es una columna `timestamptz` nullable solo de la entidad, que completará la reserva pública (C-41) cuando el paciente acepte el tratamiento de datos; C-07 la deja en `null` e ignora cualquier valor que llegue en el cuerpo. `PacienteResponse` es un `record` con `desde(Paciente)`; no hay `actualizadoEn` porque la KB no lo define.
- `DELETE` es una baja lógica aunque use el verbo `DELETE`: no hay borrado físico. Se documenta en el javadoc del controlador para que no se confunda con un borrado real.
- **Alternativa descartada:** `PUT /api/pacientes/{id}/baja`. Se descarta porque la matriz de KB 03 habla de "D" y `DELETE` es el verbo natural; el comportamiento lógico queda documentado.

### D6. Búsqueda con filtros opcionales y JPQL parametrizado

- `GET /api/pacientes` recibe `apellido`, `nombre`, `dni` y `email`, todos opcionales y combinables con AND. Sin filtros devuelve todas las fichas activas.
- `apellido`, `nombre` y `email` ignoran mayúsculas; apellido y nombre son coincidencia parcial (`like %valor%`); DNI y email son exactos. La búsqueda filtra siempre `activo = true`.
- Un JPQL único con `(:param is null or ...)` evita cuatro queries y mantiene el SQL parametrizado (regla 9). El orden es `apellido, nombre`.
- No hay paginación: un consultorio de 2 a 5 profesionales no la necesita y el plan no la pide. Se puede agregar después sin romper el contrato (parámetros nuevos).
- **Alternativa descartada:** un único parámetro `q` que busque en los cuatro campos con OR. Se descarta porque el resto de los listados de la KB usa filtros explícitos (`turnos?desde&hasta&...`) y porque C-23 puede ofrecer un selector "buscar por" sin ambigüedad.
- **Alternativa descartada:** `Specification`/Criteria. Es más dinámico, pero el proyecto pide JPQL y el número de filtros es fijo.

### D7. Errores, códigos y datos personales

- Falta de medio de contacto: `ReglaNegocioException("RN-32", NO_PROCESABLE_422, "El paciente debe tener al menos un medio de contacto")`.
- DNI repetido (pre-chequeo o constraint): `ReglaNegocioException("RN-32", CONFLICTO_409, "El DNI ya está registrado")`.
- Ficha inexistente: `ReglaNegocioException("PACIENTE_NO_ENCONTRADO", NO_ENCONTRADO_404, "No existe el paciente solicitado")`.
- Formato inválido (nombre/apellido vacíos, `@Size` excedido): 400 con la lista `errores` que ya arma `ManejadorErrores`.
- Ningún mensaje incluye nombre, apellido, DNI, email, teléfono ni notas de otra ficha (RN-35). No se loguean fichas ni datos personales.
- **Alternativa descartada:** códigos por caso (`DNI_DUPLICADO`, `CONTACTO_FALTANTE`). Se descarta porque el formato existente usa el código de la regla y RN-32 cubre los dos casos; el `detail` distingue la causa.

### D8. Baja lógica y consecuencias

- La baja marca `activo = false`; la fila y todos sus datos quedan. La búsqueda la excluye; `GET /{id}` la sigue mostrando con `activo = false`.
- El DNI de una ficha inactiva queda libre: la unicidad rige solo entre fichas activas (decisión del líder, 2026-11-23). La reactivación no está en el plan; si un change posterior la agrega, deberá rechazar la reactivación cuando otra ficha activa ya use ese DNI (el índice parcial lo impediría con 409).
- La protección "no borrar un paciente con turnos" se cumple trivialmente porque C-07 no borra nunca; C-19 agrega la validación cuando exista `Turno`.

### D9. Tests

- **Integración (`*IT`)** con `PruebaIntegracionBase` (contexto completo, perfil `test`, PostgreSQL de Testcontainers, `MockMvc`). Cada `@Test` lleva `@DisplayName` con el nombre exacto del escenario de la spec.
- Se autentica con `@WithMockUser(roles = ...)` por test (recepción, admin, odontólogo) y sin usuario para el caso anónimo. En toda escritura se envía el CSRF como la SPA (cookie `XSRF-TOKEN` obtenida de `GET /api/auth/csrf` más cabecera `X-XSRF-TOKEN`), igual que `CatalogoPrestacionesIT`. **No se usa `SecurityMockMvcRequestPostProcessors.csrf()`**: reemplaza de forma permanente el repositorio de tokens del filtro CSRF del contexto compartido y deja sin cookie a las demás clases que corren después. Un 403 de CSRF taparía un 403 de rol, por eso el token es siempre válido.
- La limpieza es `@BeforeEach` con `PacienteRepositorioJpa.deleteAll()`, no `@Transactional` en la clase: una violación de unicidad deja la transacción marcada para rollback y contaminaría los tests siguientes.
- **Unitario (`*Test`)** de `Paciente` para la normalización (DNI en blanco, contactos en blanco, RN-32), sin Spring.
- **Integración de persistencia (`*IT`)** que guarda dos fichas ACTIVAS con el mismo DNI a través del adaptador (salteando el pre-chequeo del servicio) y verifica que la constraint de la base lanza `DataIntegrityViolationException`. Es la red de seguridad de la carrera.
- **Integración del puerto** `PacienteConsultaIT` para los cuatro escenarios de consulta (por id, por email sin distinguir mayúsculas, inactiva por id, inexistente vacío).
- Solo datos ficticios: `Paciente Ficticio Uno`, DNI `00000001`, `paciente1@example.test`, teléfono `0000-000001`, obra social `Obra Social Ficticia`.

### D10. Relación con C-03

C-07 solo depende de C-01. Los endpoints quedan protegidos por la política denegar por defecto y por `@PreAuthorize`. El login real, los roles persistidos y el CSRF con cookie ya existen (C-03, C-13), y los tests usan ese CSRF. No se edita `SecurityConfig` ni el `pom.xml`.

## Risks / Trade-offs

- **[El JPQL con parámetros nulos puede fallar en PostgreSQL]** → El test de búsqueda cubre los cuatro filtros y las combinaciones; si Hibernate no infiere el tipo, se pasa a `Specification` o a queries por combinación sin cambiar la spec.
- **[Dos fichas con el mismo DNI en el historial (una inactiva y una activa)]** → Es la decisión del líder (2026-11-23): la baja libera el DNI. Los módulos que cruzan por DNI deben considerar solo la ficha activa; la reactivación futura deberá validar el DNI.
- **[Una base de dev con la unique completa `uk_paciente_dni`]** → El script idempotente la descarta (`DROP CONSTRAINT IF EXISTS`) y crea el índice parcial; no hace falta `docker compose down -v`.
- **[Cualquier `DataIntegrityViolationException` se traduce a DNI duplicado]** → Hoy la tabla solo tiene el índice parcial del DNI. Si C-41 o C-19 agregan otra, deben revisar esta traducción; queda anotado en el javadoc del servicio.
- **[La seguridad en el controlador deja el servicio accesible desde código interno]** → Es intencional: el puerto lo usa la reserva pública anónima de C-41. No hay endpoint HTTP que llegue al servicio sin `@PreAuthorize`.
- **[La sesión real se prueba en otras clases]** → C-03 y C-13 ya están en `master` (login por cookie, CSRF con cookie y verificación de sesión vigente). Estos tests usan `@WithMockUser` más el CSRF por cookie y cabecera, como el resto de los ITs de API; la verificación de sesión vigente solo actúa sobre `IdentidadPersonal` y la cubren `SesionVigenteIT` y compañía.
- **[Datos personales en logs]** → No se loguean fichas ni mensajes con datos. El manejador genérico loguea la excepción, no el cuerpo. Los mensajes de error son genéricos.

## Migration Plan

No hay datos ni versiones previas del módulo. La tabla `pacientes` la crea Hibernate al arrancar. Para revertir, se revierte el PR y, si se quiere limpiar la tabla en dev, `docker compose down -v`. En las máquinas del equipo no hay pasos manuales: `./mvnw verify` levanta su propio PostgreSQL con Testcontainers.

## Open Questions

Ninguna. La pregunta sobre si una ficha inactiva libera su DNI quedó resuelta por el líder el 2026-11-23: sí lo libera, y la unicidad rige solo entre fichas activas.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-21. C-07 es solo backend; la política del proyecto es `system-only` y no hay pantallas en este change.
- Mockups: n/a
- Approval: n/a
- Preview: n/a
- Visual verify: n/a

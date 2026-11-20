# Registro de Skills

**Uso exclusivo del delegador.** Cualquier agente que lance sub-agentes lee este registro para resolver las reglas compactas e inyectarlas directamente en el prompt del sub-agente. Los sub-agentes NO leen este registro ni los SKILL.md.

Proyecto: turnos-odontologia (Engram: `global`). Stack: Java 21 + Spring Boot 3 (Web, Data JPA/JPQL, Validation, Security, Mail), PostgreSQL 16 en Docker, esquema por Hibernate `ddl-auto` (sin Flyway), React + TypeScript + Vite. Tests: JUnit 5, Testcontainers, Vitest. TDD estricto.

Prioridad ante duplicados: skills de proyecto (`.claude/skills/`) sobre las de usuario (`~/.claude/skills/`). Las copias en `.agents/skills/` duplican las `openspec-*` y no se listan aparte.

## Skills del proyecto

| Trigger | Skill | Ruta |
|---------|-------|------|
| Agregar features/endpoints, corregir bugs o refactorizar en el backend (TDD, JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo) | springboot-tdd | .claude/skills/springboot-tdd/SKILL.md |
| Autenticación/autorización, validación de entrada, CSRF, secretos, cabeceras, CORS, rate limit, token del paciente | springboot-security | .claude/skills/springboot-security/SKILL.md |
| Preparar un PR o verificación previa: build Maven, análisis estático, tests con cobertura, escaneo de secretos/dependencias, revisión del diff | springboot-verification | .claude/skills/springboot-verification/SKILL.md |
| Diseñar entidades JPA, relaciones, consultas JPQL, N+1, transacciones, paginación, índices | jpa-patterns | .claude/skills/jpa-patterns/SKILL.md |
| Diseñar o revisar el esquema PostgreSQL: tipos, constraints (EXCLUDE/gist), índices | postgresql-table-design | .claude/skills/postgresql-table-design/SKILL.md |
| Dockerfile / docker-compose (PostgreSQL 16, entorno local), seguridad de contenedores | docker-patterns | .claude/skills/docker-patterns/SKILL.md |
| Escribir tests del front, mocks, cobertura, configuración de Vitest | vitest | .claude/skills/vitest/SKILL.md |
| Escribir/revisar/refactorizar componentes React de la SPA (rendimiento; solo reglas aplicables a Vite) | vercel-react-best-practices | .claude/skills/vercel-react-best-practices/SKILL.md |
| "verificar el change", "openspec verify", "opsx verify", "¿está listo para archivar?" (después de apply, antes de archive) | openspec-verify | .claude/skills/openspec-verify/SKILL.md |
| "openspec apply", "opsx apply": implementar tareas de un change | openspec-apply-change | .claude/skills/openspec-apply-change/SKILL.md |
| "openspec archive", "opsx archive": cerrar y archivar un change terminado | openspec-archive-change | .claude/skills/openspec-archive-change/SKILL.md |
| "openspec explore", "opsx explore": pensar/investigar antes o durante un change | openspec-explore | .claude/skills/openspec-explore/SKILL.md |
| "openspec propose", "opsx propose": crear un change con todos sus artefactos | openspec-propose | .claude/skills/openspec-propose/SKILL.md |
| "openspec sync", "opsx sync": sincronizar delta specs con las specs principales | openspec-sync-specs | .claude/skills/openspec-sync-specs/SKILL.md |
| "openspec update change", "opsx update": revisar artefactos de planificación de un change (nunca edita código) | openspec-update-change | .claude/skills/openspec-update-change/SKILL.md |

## Skills de usuario (UI, dependencias blandas)

| Trigger | Skill | Ruta |
|---------|-------|------|
| Pulido de UI, animaciones, detalles de componentes | emil-design-eng | ~/.claude/skills/emil-design-eng/SKILL.md |
| Diseño frontend con criterio estético (v1) | design-taste-frontend-v1 | ~/.claude/skills/design-taste-frontend-v1/SKILL.md |
| Definir/auditar el sistema de diseño del proyecto (`design-system/`) | design-system | ~/.claude/skills/design-system/SKILL.md |

Omitidas por ser de flujo de fundación o de este registro: active-orchestrator, agent-instruction, discovery-research, find-skill, kb-creator, roadmap-generator, web-scraper, skill-creator, skill-registry.

## Reglas compactas

Reglas pre-digeridas por skill. El delegador copia los bloques que correspondan al prompt del sub-agente como `## Project Standards (auto-resolved)`.

### springboot-tdd
- Ciclo Red-Green-Refactor: test que falla primero, mínimo código para pasar, refactor en verde. Patrón Arrange-Act-Assert.
- Dominio puro (`Intervalo`, `ValidadorTurno`, `PoliticaCancelacion`, etc.): JUnit 5 + AssertJ sin Spring ni BD; `@ParameterizedTest` para bordes.
- Servicios con Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`); evitar mocks parciales, stubbing explícito.
- Web: `@WebMvcTest` + MockMvc + `jsonPath`; integración: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`.
- Persistencia: `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = NONE)` contra PostgreSQL real vía Testcontainers; inyectar la URL con `@DynamicPropertySource`. No usar H2.
- Excepciones con `assertThatThrownBy(...)`; builders de datos de prueba; solo datos ficticios.
- Cobertura con JaCoCo (objetivo 80%+); correr con `./mvnw verify`.
- Convención del equipo: `@DisplayName("<nombre exacto del escenario>")` para que openspec-verify mapee escenario a test.
- Test obligatorio: dos turnos solapados rechazados por la constraint `EXCLUDE`, y el borde contiguo `[inicio, fin)` aceptado (RN-02). Reloj inyectable (`Clock`) para tests deterministas.
- Tests rápidos, aislados, deterministas: probar comportamiento, no implementación.

### springboot-security
- Denegar por defecto; `@EnableMethodSecurity` y `@PreAuthorize` por servicio además de reglas por ruta. Odontólogo es solo lectura (403 en escrituras, RN-33). La SPA oculta opciones pero no es barrera.
- Personal: sesión de servidor con cookie `HttpOnly`, `SameSite=Lax`, `Secure` fuera de dev; BCrypt; bloqueo temporal tras intentos fallidos (RN-34). El skill sugiere JWT, pero en este proyecto rige la decisión DD-03 (sesión).
- CSRF habilitado con `CookieCsrfTokenRepository` para `/api/**` autenticado; `/api/public/**` exceptuado.
- Token de paciente (RN-25/RN-07): `SecureRandom` 256 bits base64url, guardar solo hash SHA-256, 404 uniforme ante cualquier fallo, nunca en logs, acceso solo a su propio turno.
- Entrada validada con Bean Validation (`@Valid`) en DTOs; JPQL siempre parametrizado, jamás concatenar SQL.
- Rate limit por IP y por email en endpoints públicos; respuestas que no revelan datos de otros pacientes.
- CORS restringido al origen del front (`APP_CORS_ORIGIN`), nunca `*`; cabeceras de seguridad por defecto.
- Secretos solo por variables de entorno/`.env` (fuera del repo); `application*.yml` con placeholders `${VAR}`.
- Logs sin PII ni tokens (Ley 25.326 y 26.529); sin mensajes de excepción crudos en respuestas (Problem Details RFC 7807).

### springboot-verification
- Orden: build (`./mvnw clean verify -DskipTests`), análisis estático (SpotBugs/PMD/Checkstyle si están configurados), tests con JaCoCo, escaneo de seguridad, revisión del diff.
- Testcontainers requiere Docker; si está apagado, informar "no ejecutado", no marcar como pasado.
- Buscar secretos en el código (y en el historial git) y dependencias con CVE (OWASP dependency-check).
- Detectar `System.out.println` (usar logger), mensajes de excepción crudos en respuestas y CORS comodín.
- Revisar el diff contra la rama base buscando cambios no intencionados y falta de tests.
- Resultado final: informe PASS/FAIL con cobertura y hallazgos; no afirmar nada que no se ejecutó.

### jpa-patterns
- Entidades delgadas; `@Enumerated(EnumType.STRING)` siempre; `Instant`/`OffsetDateTime` para fechas (mapea a `timestamptz`); `@Version` para bloqueo optimista en `Turno`.
- Relaciones `LAZY` por defecto; nunca `EAGER` en colecciones; `JOIN FETCH` o proyecciones DTO para evitar N+1.
- Entre módulos referenciar por id (`Long profesionalId`), no por entidad de otro módulo; los repositorios no se usan fuera de su módulo.
- JPQL con `@Query` y `@Param`; proyecciones para lecturas livianas; paginación con `Pageable`.
- `@Transactional` en servicios (`readOnly = true` en lecturas); transacciones cortas.
- Índices en columnas de filtro y FKs; índices compuestos según las consultas reales.
- Gotcha: el skill recomienda Flyway/Liquibase y no confiar en ddl-auto. Este proyecto decidió (DD-08) usar Hibernate `ddl-auto` (`update` dev, `create-drop` test, `validate` prod); lo que Hibernate no genera (`btree_gist`, `EXCLUDE`) va en `schema-constraints.sql`.
- Tests de acceso a datos con `@DataJpaTest` + Testcontainers.

### postgresql-table-design
- PK `BIGINT GENERATED ALWAYS AS IDENTITY` (UUID solo si hace falta opacidad); `snake_case` sin comillas.
- Tiempo siempre `TIMESTAMPTZ`; dinero `NUMERIC`; strings `TEXT` (+ `CHECK` de largo); evitar `timestamp`, `char/varchar(n)`, `money`, `serial`.
- `NOT NULL` y `DEFAULT` donde corresponda; valores de negocio evolutivos como `TEXT` + `CHECK`, no ENUM de PG.
- PostgreSQL no indexa FKs automáticamente: crear el índice a mano.
- Sin solapamientos: `EXCLUDE USING gist (profesional_id WITH =, tstzrange(inicio, fin, '[)') WITH &&) WHERE (estado <> 'CANCELADO')` (igual para `sillon_id`); requiere `btree_gist`.
- `UNIQUE` admite varios NULL; usar `NULLS NOT DISTINCT` (PG15+) si no se quiere.
- Índices parciales y de expresión (`LOWER(email)`) para subconjuntos y búsquedas insensibles a mayúsculas; GiST para rangos.
- Adaptar al proyecto: el DDL lo genera Hibernate; estas reglas guían el mapeo de entidades y `schema-constraints.sql` (idempotente, separador `^^^` por los bloques `DO $$`).

### docker-patterns
- `docker-compose.yml` con PostgreSQL 16 (tag fijo, nunca `:latest`), volumen nombrado para datos, `healthcheck` y `depends_on` con condición.
- Credenciales por variables de entorno desde `.env` (gitignored); versionar solo `.env.example`; nunca secretos en el compose ni en capas de imagen.
- Publicar solo los puertos necesarios (5432 a localhost); redes personalizadas para aislar servicios.
- Si se agrega Dockerfile: multi-stage, usuario no root, `.dockerignore`.
- Un proceso por contenedor; datos solo en volúmenes (los contenedores son efímeros).
- Ignorar las secciones sobre "hardened CLI installer harnesses" y ECC: no aplican a este proyecto.

### vitest
- Config compartida con Vite (`defineConfig`); entorno `jsdom` o `happy-dom` para componentes React.
- API compatible con Jest: `describe`/`it`/`expect`; hooks `beforeEach`/`afterEach`; mocks con `vi.fn()`, `vi.mock()`, `vi.spyOn()`; fechas con `vi.useFakeTimers()` y `vi.setSystemTime()` (útil para la zona horaria de Buenos Aires).
- Cobertura con provider V8 (`vitest run --coverage`); correr con `npx vitest run` (sin watch) en CI y verify.
- Convención del equipo: el texto del `it(...)` repite el nombre del escenario de la spec para el mapeo de openspec-verify.
- Testing Library para formularios, guards por rol y manejo de 409; `vi.mock` del cliente de API.

### vercel-react-best-practices
- Solo aplican las reglas de React puro (SPA Vite). Ignorar las de Next.js/RSC: `server-*`, `next/dynamic`, `after()`, `React.cache`, streaming con Suspense del servidor, hidratación SSR.
- Cascadas: `async-parallel` (`Promise.all` para llamadas independientes), `async-defer-await` (await en la rama donde se usa).
- Bundle: importar directo, sin barrel files (`bundle-barrel-imports`); `React.lazy` + `Suspense` para rutas/componentes pesados (equivalente a `next/dynamic`); cargar analytics después de hidratar.
- Datos del cliente: deduplicar requests (SWR o TanStack Query); versionar y minimizar `localStorage`.
- Re-renders: derivar estado durante el render (no en effects); setState funcional; dependencias de effect primitivas; no definir componentes dentro de componentes; `startTransition` para actualizaciones no urgentes; lógica de interacción en handlers, no en effects.
- Render: ternario en vez de `&&` para condicionales; extraer JSX estático fuera del componente.
- JS: `Map`/`Set` para búsquedas repetidas, retorno temprano, `toSorted()` para inmutabilidad.
- Prioridad: waterfalls y bundle (críticos) antes que micro-optimizaciones.

### openspec-verify
- Entre `/opsx:apply` y `/opsx:archive`; no escribe código, no archiva, no marca tareas.
- Pasos: `openspec validate "<name>" --strict`; tareas sin `[x]` = bloqueante; matriz escenario WHEN/THEN a test (Cubierto/Parcial/Sin test); correr `./mvnw -q verify` y `npx vitest run`; cruzar con `knowledge-base/05_reglas_de_negocio.md`; comparar con `design.md`.
- Bordes críticos: RN-01/RN-02 (solapamiento real y borde contiguo), concurrencia con `EXCLUDE` en Testcontainers, token RN-07.
- Reglas duras bloqueantes: datos reales de pacientes o secretos en el diff.
- Salida: `openspec/changes/<name>/verify.md` con veredicto LISTO PARA ARCHIVAR / NO LISTO; no afirmar nada no ejecutado.

### openspec-apply-change
- Obtener contexto con `openspec instructions apply --change "<name>" --json`; leer los artefactos indicados antes de codificar.
- Implementar tarea por tarea, mínimo cambio; marcar `- [x]` al terminar cada una; pausar ante ambigüedad, bloqueo o diseño contradictorio.
- Con TDD estricto: test rojo primero, luego código (ver springboot-tdd y vitest).
- Al terminar, sugerir el siguiente paso: openspec-verify.

### openspec-archive-change
- Verificar estado con `openspec status`; avisar si hay artefactos o tareas incompletas (pedir confirmación).
- Ofrecer sincronizar delta specs antes de archivar; mover a `openspec/changes/archive/YYYY-MM-DD-<name>/`; no sobrescribir un archivo existente.

### openspec-explore
- Modo de pensamiento: no se implementa código; solo leer, investigar y dibujar diagramas.
- Se pueden crear/actualizar artefactos de OpenSpec si el usuario lo decide; ofrecer capturar decisiones, no imponerlas.

### openspec-propose
- `openspec new change "<name>"` (kebab-case) y luego generar proposal, design, specs y tasks en orden de dependencia con `openspec instructions <artifact> --change "<name>" --json`.
- Parar cuando `applyRequires` esté completo; pedir aclaración si el contexto es crítico. Escenarios con `#### Scenario:` y pasos WHEN/THEN.

### openspec-sync-specs
- Aplicar las delta specs (ADDED/MODIFIED/REMOVED/RENAMED) del change sobre `openspec/specs/<capability>/spec.md` sin archivar; cambios inteligentes, no copia ciega.

### openspec-update-change
- Revisar y mantener coherentes los artefactos de planificación (proposal, design, specs, tasks) de un change; nunca edita código. Si se refiere al comando CLI `openspec update`, ejecutarlo.

### emil-design-eng
- Pulido de UI invisible: easing y duraciones cortas, animaciones que respeten `prefers-reduced-motion`, estados hover/focus/active cuidados. Dependencia blanda: seguir antes los tokens del `design-system/`.

### design-taste-frontend-v1
- Criterio estético para frontend; si contradice el sistema de diseño aprobado (`design-system/`, revisión 1), prevalece el sistema aprobado.

### design-system
- Modo del proyecto: `system-only` (sistema aprobado rev. 1, sin mockups por change). Marca final pendiente (`pending_final_brand_change`). Usar `design-system/tokens.css`, `brand.js` y `assets/logo.svg`; no inventar valores.

## Convenciones del proyecto

| Archivo | Ruta | Notas |
|---------|------|-------|
| AGENTS.md / CLAUDE.md | (no existen aún) | Los generará agent-instruction en el paso siguiente |
| Arquitectura y convenciones | knowledge-base/08_arquitectura_propuesta.md | Monolito modular por feature (`ar.edu.utn.turnos`), dominio puro, hexagonal, Problem Details, Hibernate ddl-auto + `schema-constraints.sql` |
| Reglas de negocio | knowledge-base/05_reglas_de_negocio.md | RN-XX para escenarios y tests |
| Base de conocimiento | knowledge-base/README.md | Índice de los 10 archivos de la KB |
| Roadmap de changes | CHANGES.md | C-01 a C-51, dependencias y "Leer antes" |
| Sistema de diseño | design-system/README.md | Rev. 1, aprobado, modo `system-only` |
| Estado compartido | .active-orchestrator-state.json | Propiedad del active-orchestrator |

Leer los archivos de convenciones de arriba para patrones específicos del proyecto. No hay índice AGENTS.md que referencie rutas adicionales.

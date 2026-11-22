<!-- active-stack:agent-instruction -->
# Turnos Odontología — Instrucciones para Agentes

> Todo agente lee primero este archivo o su copia idéntica `CLAUDE.md` al entrar al repo.
> Se generó a partir de `knowledge-base/`, `CHANGES.md` y `.atl/skill-registry.md`. Si lo editás, mantené sincronizados los dos archivos (`diff AGENTS.md CLAUDE.md`).

El sistema maneja turnos y agenda para consultorios odontológicos argentinos de 2 a 5 profesionales. Es un proyecto académico (UTN, Metodología I) con un equipo de 6 personas y se trabaja con SDD, OpenSpec y Active Stack. **Todo se escribe en español.**

---

## Stack Tecnológico

| Capa | Tecnologías | Versión mínima |
|---|---|---|
| Backend | Java, Spring Boot (Spring Web, Spring Data JPA con JPQL, Bean Validation, Spring Security, Spring Mail) | Java 21, Spring Boot 3.x |
| Base de datos | PostgreSQL en Docker (`docker compose`), extensión `btree_gist` | PostgreSQL 16 |
| Esquema | Hibernate (`ddl-auto`) a partir de las entidades JPA, más un script SQL idempotente para `btree_gist` y las restricciones `EXCLUDE` | Incluido en Spring Boot 3 |
| Frontend | React + TypeScript con Vite | React 18, TypeScript 5 |
| Tests backend | JUnit 5, Spring Boot Test, AssertJ, Testcontainers (PostgreSQL real) | Versiones del BOM de Spring Boot |
| Tests frontend | Vitest + Testing Library | Vitest 1.x |
| Email | SMTP real con Spring Mail; las credenciales van solo en `.env` y se versiona `.env.example` | n/a |
| Build | Maven | Maven 3.9 |
| Zona horaria | `America/Argentina/Buenos_Aires`; se persiste en UTC (`timestamptz`) | n/a |

El detalle completo está en [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md).

---

## Base de Conocimiento

`knowledge-base/` es la fuente de verdad del dominio. **Leé el archivo que corresponda ANTES de implementar.**

| Archivo | Cuándo leerlo |
|---|---|
| [01_vision_y_objetivos.md](knowledge-base/01_vision_y_objetivos.md) | Para entender el propósito, el alcance del MVP y lo que queda fuera |
| [03_actores_y_roles.md](knowledge-base/03_actores_y_roles.md) | Para autenticación, RBAC y permisos (el odontólogo es solo lectura; el paciente usa token) |
| [04_modelo_de_datos.md](knowledge-base/04_modelo_de_datos.md) | Para entidades, ERD, restricciones `EXCLUDE` y seed ficticio |
| [05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Para las reglas codificadas RN-01 a RN-38 |
| [06_funcionalidades.md](knowledge-base/06_funcionalidades.md) | Para las historias de usuario US-001 a US-022 |
| [07_flujos_principales.md](knowledge-base/07_flujos_principales.md) | Para los flujos de punta a punta |
| [08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Para patrones, estructura de paquetes, seguridad y variables de entorno |
| [09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) | Para las decisiones (DD-XX) y los supuestos (SU-XX) |
| [10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) | Para las preguntas abiertas que hay que resolver antes de codear |

> Las preguntas de prioridad alta PA-01 a PA-04 están resueltas. Sigue abierta en parte **PA-06**, sobre la política de reprogramación tardía del personal: hoy se permite y se registra (SU-07). Hay que confirmarla antes de implementar la reprogramación. El índice de la KB está en [knowledge-base/README.md](knowledge-base/README.md) y el sistema visual en [design-system/README.md](design-system/README.md).

---

## Skills Disponibles

| Rol | Contexto | Skills que carga |
|---|---|---|
| **Backend** | Endpoints, servicios de dominio, seguridad y tests | `springboot-tdd`, `springboot-security`, `springboot-verification`, `jpa-patterns` |
| **Datos e infraestructura** | Esquema, constraints `EXCLUDE`, `docker compose` | `postgresql-table-design`, `docker-patterns` |
| **Frontend** | Componentes React + Vite y tests | `vitest`, `vercel-react-best-practices` (solo lo que aplica a Vite: se ignoran SSR y `server-*`) |
| **Ciclo OPSX** | Explorar, proponer, implementar, verificar y archivar changes | `openspec-explore`, `openspec-propose`, `openspec-apply-change`, `openspec-verify`, `openspec-archive-change`, `openspec-sync-specs`, `openspec-update-change` |
| **UI (apoyo)** | Pulido visual siguiendo el sistema aprobado | `design-system`, `emil-design-eng` (si están instaladas) |

Cargá la skill que corresponda al contexto ANTES de escribir código. Las skills de proyecto están en `.claude/skills/` (Claude Code) y en una copia idéntica en `.agents/skills/` (Codex, OpenCode y otros agentes), versionadas en `skills-lock.json`. Si actualizás una skill, actualizá las dos carpetas (`diff -r .claude/skills .agents/skills`).

> Las reglas compactas de cada skill están en [.atl/skill-registry.md](.atl/skill-registry.md), que genera `skill-registry` y se versiona en el repo. Ahí también figuran los choques con decisiones del proyecto: jpa-patterns recomienda Flyway, pero rige DD-08 (Hibernate); springboot-security sugiere JWT, pero rige DD-03 (sesión). Esta tabla solo asigna skills a roles.

---

## Roadmap de Changes

El plan completo de implementación está en [CHANGES.md](CHANGES.md). Resumen:

- **Total:** 51 changes en 9 olas (Ola 0 a Ola 8). En promedio llevan ~4,5 h cada uno (S 2-3 h, M 4-6 h, L 8-10 h).
- **Camino crítico (9):** `C-01 → C-04 → C-10 → C-19 → C-27 → C-36 → C-42 → C-49 → C-50`.
- **Primer change:** `C-01` (esqueleto-backend-y-entorno), de Santino, en la Ola 0.
- **Change OPSX evaluado:** `C-19` (crear-turno-sin-solapamientos), de Juan Pablo, en la Ola 3.
- **Asignación:** la "Asignación rápida" de CHANGES.md muestra, para cada change, su responsable (con dueño o "Cualquiera") y sus dependencias.

**Antes de empezar un change:** leé su ficha en [CHANGES.md](CHANGES.md), confirmá que sus dependencias ("Depende de") ya estén mergeadas en `master` y leé los archivos de "Leer antes".

---

## Reglas Duras (específicas del proyecto)

> Las reglas **globales** del archivo de instrucciones global del cliente (orquestador, gobernanza, TDD estricto, Engram) ya las hereda el proyecto y no se repiten acá. Acá van las reglas de este proyecto y las universales que el global no cubre (git y commits). Son un contrato: romperlas es un defecto.

### Datos y seguridad
1. NUNCA usar datos reales de pacientes (seeds, fixtures, tests, capturas, ejemplos) → solo datos ficticios.
2. NUNCA subir secretos ni credenciales al repo → van en `.env`, que está ignorado; se versiona solo `.env.example`.
3. NUNCA guardar el token del paciente en claro → se guarda hasheado, sirve para un solo turno y vence al inicio del turno o al cancelarse (RN-07, RN-25, DD-06).
4. NUNCA dejar un endpoint de escritura accesible al rol `ODONTOLOGO` → para el odontólogo la agenda es solo lectura (RN-20, PA-03).

### Dominio y backend
5. NUNCA confiar solo en la validación en Java para evitar sobreturnos → la garantía final es `EXCLUDE USING gist` en PostgreSQL (por profesional y por sillón). Los intervalos son `[inicio, fin)`: un turno que empieza justo cuando termina otro NO se solapa (RN-01, RN-02).
6. NUNCA guardar horas de turno sin zona (`LocalDateTime`) → usar `timestamptz` (`Instant`/`OffsetDateTime`); los cálculos se hacen en `America/Argentina/Buenos_Aires` con un `Clock` inyectable (DD-05).
7. NUNCA usar Flyway ni Liquibase → el esquema lo maneja Hibernate `ddl-auto` (`update` en dev, `create-drop` en tests); lo que Hibernate no genera va en el script SQL idempotente (DD-08).
8. NUNCA usar JWT → se usa sesión de servidor con cookie `HttpOnly` (DD-03).
9. NUNCA exponer entidades JPA en la API → se usan DTOs con Bean Validation y JPQL parametrizado, sin concatenar SQL.
10. NUNCA usar H2 para tests de persistencia → se usa PostgreSQL real con Testcontainers.
11. Todo test lleva `@DisplayName` (JUnit) o `it(...)` (Vitest) con el **nombre exacto del escenario** de la spec, para que `openspec-verify` lo pueda mapear.

### Frontend
12. TypeScript en modo `strict`, sin `any`, y componentes en `PascalCase`.
13. NUNCA poner colores, espaciados o fuentes fijos en el código → se usan los tokens de `design-system/tokens.css`.

### Equipo y git
14. NUNCA hacer push directo a `master` → se trabaja en la rama propia de cada integrante y se abre un PR, que revisa Juan Pablo.
15. Antes de empezar un change hay que confirmar que sus dependencias ("Depende de" en CHANGES.md) estén mergeadas. Va un change por PR.
16. Un commit por etapa o por change, con un mensaje claro. **El agente pregunta antes de cada commit.**
17. La documentación, las specs y los mensajes se escriben en español, y los nombres del dominio también (`Turno`, `CrearTurno`), según `knowledge-base/08_arquitectura_propuesta.md`.

---

## Flujo de Trabajo

```
1. Leer la KB relevante (knowledge-base/)            → entender el dominio
2. Tomar el change en CHANGES.md                     → respetar dueño y dependencias
3. /opsx:propose → proposal, design, specs y tasks   → alcance y criterios de aceptación
4. /opsx:apply   → implementar con TDD estricto
5. openspec-verify → verify.md con el veredicto
6. PR a master (lo revisa Juan Pablo) → /opsx:archive
```

Cada paso cumple TODAS las reglas duras. Si la KB y este archivo se contradicen, prevalecen las reglas duras.
<!-- /active-stack:agent-instruction -->

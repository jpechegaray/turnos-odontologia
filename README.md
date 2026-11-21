# Turnos Odontología

Este sistema maneja los turnos y la agenda de consultorios odontológicos argentinos de 2 a 5 profesionales. Garantiza que no haya turnos superpuestos, ni del mismo profesional ni en el mismo sillón, y su diferencial es una **lista de espera que avisa sola por email** cuando se libera un horario.

Proyecto de la UTN (Metodología I), hecho con **SDD + OpenSpec + Active Stack** en Claude Code.

| | |
|---|---|
| **Estado** | Fundación cerrada (discovery, KB, diseño, roadmap, skills y reglas). Listo para arrancar los changes. |
| **Equipo** | Juan Pablo (líder; revisa todos los PR), Santino, Valentino, Lionel, Julián, Nico |
| **Tablero** | [Turnos Odontología — Roadmap](https://github.com/users/jpechegaray/projects/3) |
| **Issues** | [Un issue por change](https://github.com/jpechegaray/turnos-odontologia/issues): C-NN es el issue #NN |

---

## 1. Qué leer y en qué orden

Antes de tocar código, leé esto en este orden:

| # | Archivo | Para qué | Tiempo aprox. |
|---|---|---|---|
| 1 | [docs/discovery/discovery.md](docs/discovery/discovery.md) | Problema, usuarios, reglas de negocio y riesgos (11 puntos) | 10 min |
| 2 | [knowledge-base/README.md](knowledge-base/README.md) | Índice de la base de conocimiento (fuente de verdad del dominio) | 5 min |
| 3 | [knowledge-base/05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Reglas RN-01 a RN-38: no se puede implementar sin conocerlas | 20 min |
| 4 | [knowledge-base/08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Paquetes, capas, seguridad y estructura del repo | 15 min |
| 5 | [AGENTS.md](AGENTS.md) | Stack, skills por rol y las **17 reglas duras** del proyecto | 10 min |
| 6 | [CHANGES.md](CHANGES.md) → "Asignación rápida" | Qué change te toca, de qué depende y cuándo se puede empezar | 10 min |

Después, según lo que vayas a hacer:
- **Un change concreto:** su ficha en [CHANGES.md](CHANGES.md) o su issue. Cada uno indica qué leer antes.
- **Pantallas:** [design-system/README.md](design-system/README.md) y [components.html](design-system/components.html).
- **Contexto de mercado:** [docs/discovery/informe-discovery.md](docs/discovery/informe-discovery.md), con 23 sistemas relevados.
- **Por qué se decidió cada cosa:** [knowledge-base/09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) y [docs/bitacora-inspeccion.md](docs/bitacora-inspeccion.md).

---

## 2. Preparar el entorno

### Lo que hay que instalar

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera reciente | Repo |
| Java (JDK) | 21 | Backend Spring Boot |
| Maven | 3.9 (o el `mvnw` del repo, en `backend/`) | Build del backend |
| Docker Desktop | reciente | PostgreSQL 16 y Testcontainers |
| Node.js | 22.20 o más | Front (Vite) y CLIs |
| OpenSpec CLI | 2.x | Ciclo de changes: `npm install -g @fission-ai/openspec` |
| Claude Code | reciente | Agente: lee `CLAUDE.md` y las skills del repo solo |

### Levantar el backend

Hace falta JDK 21 y Docker Desktop encendido. Desde la raíz del repo:

```bash
cp .env.example .env              # y reemplazar los valores "cambiar-en-env-local"
docker compose up -d              # PostgreSQL 16 en 127.0.0.1:5432
cd backend
./mvnw spring-boot:run            # perfil dev por defecto
./mvnw verify                     # tests unitarios (*Test) y de integración (*IT) con Testcontainers
```

Notas:

- **Puerto 5432 ocupado** (por ejemplo, un PostgreSQL instalado en tu máquina): cambiá el puerto publicado en `docker-compose.yml` (`"127.0.0.1:<otro>:5432"`) y el puerto de `SPRING_DATASOURCE_URL` en tu `.env`.
- **Recrear la base** desde cero: `docker compose down -v` borra el volumen `turnos-pgdata`. En dev Hibernate usa `ddl-auto: update`, que agrega tablas y columnas pero no borra ni renombra (PA-10), así que si cambiaste el modelo de forma incompatible recreá la base.
- **Formato del `.env`**: `CLAVE=valor`, sin comillas y sin `${}`. Spring lo importa como `.properties`, que conserva las comillas.
- **`SPRING_PROFILES_ACTIVE`** solo vale si lo exportás en la shell o en el IDE: un `.env` importado no puede activar el perfil que lo importa. Con `./mvnw spring-boot:run` el perfil `dev` ya viene por defecto.
- `./mvnw verify` necesita Docker encendido (Testcontainers levanta su propio PostgreSQL) y **no** usa tu `.env` ni la base de `docker compose`.

### Clonar y pasarte a tu rama

```bash
git clone https://github.com/jpechegaray/turnos-odontologia.git
cd turnos-odontologia
git switch <tu-rama>      # juan-pablo | santino | valentino | lionel | julian | nico
```

Las skills del proyecto (Spring Boot, JPA, PostgreSQL, Docker, Vitest, React y `openspec-verify`) ya vienen en `.claude/skills/`, así que no hace falta instalarlas.

---

## 3. Cómo se trabaja un change

```
Issue del change ─► ¿dependencias hechas? ─► /opsx:propose ─► /opsx:apply (TDD) ─► openspec-verify ─► PR a master ─► /opsx:archive
```

1. **Elegí el change.** Si tiene dueño, lo hace esa persona. Si dice **Cualquiera**, asignátelo en el issue antes de empezar: el primero que se anota se lo queda y nadie tiene más de un "Cualquiera" abierto. Las reglas completas están en CHANGES.md → *Reglas para tomar un change "Cualquiera"*.
2. **Revisá las dependencias.** El issue dice "Para empezar necesita: C-03 y C-06 hechos" y muestra *Blocked by* en *Relationships*. Si alguna sigue abierta, todavía no se puede empezar.
3. **Actualizá tu rama** antes de arrancar: `git switch <tu-rama> && git pull origin master`.
4. **Movelo a "En curso"** en el tablero.
5. **`/opsx:propose <nombre-del-change>`** genera el proposal, el design, las specs y las tasks. Revisalos antes de implementar.
6. **`/opsx:apply`** implementa con TDD estricto: primero el test que falla y después el código. Cada test lleva `@DisplayName` con el nombre exacto del escenario de la spec.
7. **`openspec-verify`** (pedile al agente "verificá el change") genera `verify.md` con el veredicto. Si dice NO LISTO, volvé a apply.
8. **Abrí el PR de tu rama a `master`** y movelo a "En revisión (PR)". Juan Pablo lo revisa; no se mergea sin su aprobación.
9. **Después del merge,** `/opsx:archive` y el issue pasa a "Hecho".

Los changes con gobernanza **CRITICAL** (etiqueta `gob:CRITICAL`) arrancan solo con análisis y propuesta. No se escribe código hasta que Juan Pablo lo aprueba.

---

## 4. Reglas que no se negocian

El detalle está en [AGENTS.md](AGENTS.md); estas son las principales:

- **Nunca datos reales de pacientes**, ni en tests, ni en seeds, ni en capturas. Solo datos ficticios.
- **Nunca secretos en el repo.** Van en `.env`, que está ignorado; solo se versiona `.env.example`.
- **Nunca push directo a `master`.** Siempre por PR.
- **Sin sobreturnos:** intervalos `[inicio, fin)`. La garantía final es la constraint `EXCLUDE` de PostgreSQL.
- **Hibernate `ddl-auto`, no Flyway. Sesión de servidor, no JWT. Testcontainers, no H2.**
- Un change por PR, con mensajes claros y en español.

---

## 5. Mapa del repo

```
AGENTS.md / CLAUDE.md     Instrucciones para agentes (iguales): stack, skills, reglas duras
CHANGES.md                Roadmap: 51 changes en 9 olas, dependencias, camino crítico, asignación
knowledge-base/           Fuente de verdad del dominio (10 archivos)
docs/discovery/           Discovery, informe de mercado (+ PDF) y fichas de 23 competidores
docs/bitacora-inspeccion.md  Decisiones del líder etapa por etapa
design-system/            Tokens, componentes y previews (modo system-only, aprobado)
openspec/                 Specs y changes de OpenSpec
.claude/skills/           Skills del proyecto (versionadas en skills-lock.json)
.atl/skill-registry.md    Reglas compactas de cada skill para los agentes
tools/                    Utilidades (por ejemplo, regenerar el PDF del informe)
```

---

## 6. Dudas

- **Del dominio:** buscá primero en la KB. Si no está, revisá [knowledge-base/10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md); si tampoco está ahí, preguntale a Juan Pablo.
- **De un change:** comentá en su issue.
- **Pendiente conocido:** PA-06 (política de reprogramación tardía del personal) se resuelve antes del change de reprogramación.

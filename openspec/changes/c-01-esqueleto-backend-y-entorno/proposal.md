# Proposal

## Why

El repositorio todavía no tiene código: no hay `pom.xml`, `docker-compose.yml` ni paquete base, y C-01 es el primer eslabón del camino crítico (`C-01 → C-04 → C-10 → C-19 → …`). Sin este esqueleto no abre el GATE 1 y quedan bloqueados los 9 changes que dependen de él (C-03, C-04, C-05, C-06, C-07, C-08, C-14, C-15 y C-17). Además, tiene que dejar listos los archivos compartidos (`pom.xml`, `application.yml`, `schema-constraints.sql`, `shared/`, `SecurityConfig`), para que seis personas trabajen en paralelo sin pisarse (RM-02, RM-07 y RM-08 de CHANGES.md).

## What Changes

- **Backend Maven nuevo** en `backend/`: Java 21, Spring Boot 3.x, paquete base `ar.edu.utn.turnos` y `TurnosApplication`. Incluye el Maven Wrapper y declara desde ya TODAS las dependencias previstas para el proyecto (web, data-jpa, validation, security, mail, postgresql, springdoc-openapi, spring-security-test, Testcontainers y ArchUnit), para que ningún change posterior tenga que editar el `pom.xml`.
- **Entorno local reproducible**: `docker-compose.yml` en la raíz con PostgreSQL 16, volumen nombrado y healthcheck. `.env.example` versionado con las variables de KB 08 §Variables de entorno. Se confirma que el `.gitignore` existente excluye `.env` y se le agrega `target/`.
- **Configuración por perfiles**: `application.yml` con un bloque comentado por módulo, `application-dev.yml` (`ddl-auto: update`) y `application-test.yml` (`create-drop`).
- **Script de restricciones** `schema-constraints.sql`: por ahora solo tiene `CREATE EXTENSION IF NOT EXISTS btree_gist`, con el separador `^^^` y ejecución posterior a Hibernate. Las `EXCLUDE` las agrega C-19.
- **Piezas transversales en `shared`**:
  - un `Clock` inyectable en `America/Argentina/Buenos_Aires`;
  - `ReglaNegocioException` con código de regla y estado HTTP;
  - `ManejadorErrores`, que responde Problem Details (RFC 7807) para 403, 404, 409 y 422 a partir de cualquier `ReglaNegocioException`, así que nadie vuelve a editarlo.
- **`SecurityConfig` base** que deniega por defecto: `/api/public/**` queda abierto y se activa `@EnableMethodSecurity`. No hay login: lo agrega C-03, que reemplaza esta configuración.
- **Base de tests**: una clase base con PostgreSQL 16 en Testcontainers y el perfil `test`, más tests de humo de `btree_gist`, Problem Details, seguridad y reloj.
- **README de arranque**: se actualiza el `README.md` raíz, que ya existe, con `docker compose up -d`, `./mvnw spring-boot:run` y `./mvnw verify`.

## Capabilities

### New Capabilities
- `entorno-desarrollo`: un entorno local reproducible con un solo comando, que cubre la base PostgreSQL 16 con `btree_gist`, la configuración por perfiles, el manejo de secretos fuera del repo, el script de restricciones idempotente y las pruebas de integración contra PostgreSQL real (US-021, RN-35).
- `errores-api`: el formato uniforme de error de la API (Problem Details RFC 7807 con código de regla), su correspondencia con los estados 403, 404, 409 y 422, y la garantía de no exponer detalles internos ni datos personales (RN-35).
- `control-acceso`: la política base de acceso a la API. Deniega todo por defecto, deja abierta la zona pública `/api/public/**` y responde 401 o 403 con formato uniforme. C-03 la extiende después con el login real.
- `reloj-del-sistema`: la fuente única de la hora actual del sistema, en la zona `America/Argentina/Buenos_Aires` y reemplazable en tests (RN-14, DD-05).

### Modified Capabilities
- Ninguna. `openspec/specs/` está vacío.

## Impact

- **Código nuevo**: `backend/` (pom, wrapper, `TurnosApplication`, `shared/**`, `SecurityConfig`, recursos y tests). En la raíz: `docker-compose.yml` y `.env.example`.
- **Archivos existentes que se modifican**: `.gitignore` (se agrega `target/`) y `README.md` (sección de arranque del backend).
- **Dependencias**: se declara de una vez todo el set del proyecto (el detalle está en design.md). Hacen falta Docker en todas las máquinas (compose y Testcontainers), JDK 21 y Maven 3.9 o el wrapper.
- **Equipo**: deja disponibles `Clock`, `ManejadorErrores`, la base de Testcontainers, el `SecurityConfig` base y el README, que es lo que pide la condición del GATE 1. El GATE 1 también exige C-02 (frontend), que es otro change.
- **Gobernanza**: MEDIUM. Se implementa por pasos y se muestran las decisiones no obvias en cada checkpoint de tasks.md.

### Fuera de alcance
- El login real, los usuarios, CSRF con `CookieCsrfTokenRepository`, CORS y el bootstrap del admin, que son de C-03.
- El frontend (`frontend/`, Vite y React), que es de C-02.
- Las entidades de dominio, los repositorios y endpoints de negocio, y las restricciones `EXCLUDE` (C-19).
- El seed de datos ficticios (`DevDataSeeder`, C-33). US-021 se cubre en parte: el criterio "el seed solo contiene datos inventados" queda para C-33.
- La configuración de mail (C-15) y el rate limit (C-17). C-01 solo deja sus bloques comentados en `application.yml`.
- Dockerfile y despliegue productivo (KB 08 §Entorno y despliegue).

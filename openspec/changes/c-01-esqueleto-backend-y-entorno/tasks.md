# Tasks

> Gobernanza **MEDIUM**: se implementa por grupos y en cada **Checkpoint** se muestran al usuario las decisiones no obvias antes de seguir. TDD estricto: dentro de cada grupo con código, el test va primero (RED), después el mínimo código (GREEN), luego la triangulación y el refactor con los tests en verde. Cada test lleva `@DisplayName` con el nombre **exacto** del escenario de la spec.
>
> **Decisiones tomadas (2026-11-21):** Q1 = Spring Boot 3.5.x; Q4 = sí a ArchUnit con `EstadoError`; Q6 = 400 por defecto de Spring para validación de formato, sin cambios al plan. Criterio: mantenerse apegado al plan original y cambiar solo ante errores críticos.

## 1. Entorno local (raíz del repo)

- [x] 1.1 Agregar `target/` al `.gitignore` existente sin tocar sus reglas de `.env`. Se verifica con `git check-ignore backend/target/x` y `git check-ignore .env`: ambos deben informar ignorado.
- [x] 1.2 Crear `.env.example` con las 16 variables de KB 08 §Variables de entorno, con los valores de ejemplo de la tabla (las sensibles con `cambiar-en-env-local`) y en formato `CLAVE=valor` sin comillas. Se verifica comparando la lista de claves con la tabla del KB y con `git ls-files | grep .env`, que debe mostrar solo `.env.example`. **Resultado (2026-11-21):** el usuario comparó las claves de `.env.example` contra la tabla de KB 08 con `Compare-Object` (sin mostrar valores): 16 de 16, sin diferencias.
- [x] 1.3 Crear `docker-compose.yml` según design D9: `postgres:16-alpine`, puerto `127.0.0.1:5432`, volumen nombrado `turnos-pgdata` y healthcheck `pg_isready`. Se verifica con `cp .env.example .env && docker compose up -d`, después con `docker compose ps` (debe dar *healthy*) y con `docker compose exec postgres psql -U turnos_app -d turnos -c "select version()"` (debe informar 16).
- [x] 1.4 Verificar que `btree_gist` está disponible en la base de compose con `select name from pg_available_extensions where name='btree_gist'`, que debe devolver una fila.

## 2. Proyecto Maven (sin código de producción)

- [x] 2.1 Crear `backend/pom.xml` con el parent `spring-boot-starter-parent` 3.5.x, Java 21, `groupId` `ar.edu.utn` y `artifactId` `turnos`, con TODAS las dependencias de design D2 y las versiones de springdoc y ArchUnit en properties. Se verifica con `mvn -f backend/pom.xml -q dependency:resolve`, que debe terminar sin errores.
- [x] 2.2 Configurar los plugins de design D2: `spring-boot-maven-plugin` con perfil `dev` por defecto, Surefire para `*Test`, Failsafe para `*IT` y JaCoCo con informe en `verify`. Se verifica con `mvn -f backend/pom.xml help:effective-pom`, que debe mostrar los cuatro plugins.
- [x] 2.3 Generar el Maven Wrapper 3.9.x en la variante `only-script` y marcar `mvnw` como ejecutable (`git update-index --chmod=+x backend/mvnw`). Se verifica con `cd backend && ./mvnw -v`, que debe informar Maven 3.9 y Java 21.

> **Checkpoint MEDIUM 1:** mostrar al usuario el `pom.xml` (versiones elegidas de Boot, springdoc, ArchUnit y Testcontainers), el compose y el `.env.example`.

## 3. Arranque, perfiles y esquema (capability `entorno-desarrollo`)

- [x] 3.1 RED: crear en `src/test` la `ContenedoresPrueba` (`@TestConfiguration` con `@Bean @ServiceConnection PostgreSQLContainer`, sobre la imagen de una constante igual a la de compose) y la `PruebaIntegracionBase` abstracta (`@SpringBootTest`, `@ActiveProfiles("test")`, `@Import`, `@AutoConfigureMockMvc`), según design D10. Escribir también `ContextoAplicacionIT` con el escenario "El contexto de la aplicación carga con el perfil test sobre PostgreSQL real", que verifica que la base informa la versión 16. Se verifica con `./mvnw verify`, que debe fallar porque `TurnosApplication` no existe.
- [x] 3.2 GREEN: crear `TurnosApplication`, el `application.yml` común (JPA, `open-in-view: false`, `hibernate.jdbc.time_zone: UTC`, springdoc apagado, bloques comentados por módulo con su dueño según design D8) y `src/test/resources/application-test.yml` con `ddl-auto: create-drop`. Se verifica con `ContextoAplicacionIT` en verde.
- [x] 3.3 RED a GREEN: escribir el escenario "El perfil test arranca con un esquema limpio", que lee la propiedad efectiva `spring.jpa.hibernate.ddl-auto` del `Environment` y espera `create-drop`. Se verifica con el test en verde.
- [x] 3.4 RED: escribir `ExtensionBtreeGistIT` con el escenario "La extensión btree_gist queda instalada al arrancar la aplicación", que consulta `pg_extension` con `JdbcTemplate`. Se verifica con el test en rojo.
- [x] 3.5 GREEN: crear `src/main/resources/schema-constraints.sql`, que solo tiene `CREATE EXTENSION IF NOT EXISTS btree_gist` seguido de `^^^`, y la configuración `spring.sql.init.*` más `defer-datasource-initialization` de design D4. Se verifica con `ExtensionBtreeGistIT` en verde.
- [x] 3.6 Triangular con el escenario "El script de restricciones es idempotente": volver a ejecutar el script con un `ResourceDatabasePopulator` que usa el separador `^^^`, sin error, y contar que `btree_gist` aparece una sola vez en `pg_extension`. Se verifica con el test en verde.
- [x] 3.7 RED a GREEN: escribir el test unitario `PerfilDevTest` con el escenario "El perfil dev conserva el esquema entre arranques", que carga `application-dev.yml` con `YamlPropertySourceLoader` y espera `update`. Después crear `application-dev.yml` con el `spring.config.import` opcional de `.env`, el datasource con placeholders y springdoc encendido (design D8). Se verifica con el test en verde y con `grep` de que ningún `application*.yml` contiene contraseñas literales.
- [x] 3.8 (Q4 aceptada) Escribir `ArquitecturaTest` con ArchUnit, con la regla "`..domain..` no depende de `org.springframework..`, `jakarta.persistence..` ni `org.hibernate..`" y `allowEmptyShould(true)`, para que pase sin dominio. Se verifica con el test en verde y con una prueba manual: una clase temporal en `x.domain` que importa Spring debe ponerlo en rojo, y después se borra.

## 4. Reloj del sistema (capability `reloj-del-sistema`)

- [x] 4.1 RED: escribir el test unitario `ConfiguracionRelojTest` con los escenarios "El reloj del sistema usa la zona de Buenos Aires" (la zona es la región `America/Argentina/Buenos_Aires` y no un `ZoneOffset`) y "Un instante UTC se interpreta en hora local de Buenos Aires" (con un `Clock.fixed` en `2026-12-01T12:00:00Z` y la zona del sistema, `LocalDateTime.now(clock)` da `2026-12-01T09:00`). Se verifica con el test en rojo.
- [x] 4.2 GREEN: crear `shared.tiempo.ZonaHoraria` (con la constante `BUENOS_AIRES`) y `ConfiguracionReloj` (con el `@Bean Clock`). Se verifica con `ConfiguracionRelojTest` en verde, más un IT que inyecta el `Clock` del contexto y comprueba su zona.
- [x] 4.3 RED a GREEN: crear en `src/test` la `RelojFijoConfig` (`@TestConfiguration` con un `@Bean @Primary Clock` fijo) y el `RelojFijoIT` con el escenario "Una prueba fija la hora actual del sistema", que usa un componente de prueba que lee `Instant.now(clock)` y espera `2026-12-01T12:00:00Z`. Se verifica con el test en verde.

## 5. Errores de la API (capability `errores-api`)

- [x] 5.1 RED: escribir `ManejadorErroresTest` (`@WebMvcTest` con un controlador de prueba anidado registrado con `@Import`, más `@WithMockUser`) con el escenario "Un error de regla responde Problem Details con su código": `RN-04`, 422, `application/problem+json`, `status`, `title`, `detail` y `codigo`. Se verifica con el test en rojo.
- [x] 5.2 GREEN: crear `shared.error.ReglaNegocioException` (con `codigo`, estado, `mensaje` y `propiedades` según design D6, más el enum `EstadoError`, Q4 aceptada) y `shared.web.ManejadorErrores`, que extiende `ResponseEntityExceptionHandler`. Se verifica con el test 5.1 en verde.
- [x] 5.3 Triangular con el escenario "Cada estado de regla responde con el mismo formato", un test parametrizado con 403, 404, 409 y 422, y con el escenario "Una regla puede agregar datos de contexto al error" (409 con `recurso=PROFESIONAL`). Agregar un test unitario: un estado que no es 4xx debe ser rechazado al construir la excepción. Se verifica con los tests en verde.
- [x] 5.4 RED a GREEN: escribir el escenario "Una petición con campos inválidos responde 400 Problem Details", con un DTO de prueba con `@NotBlank`, que espera la propiedad `errores` con el campo y sin el valor recibido. Se verifica con el test en verde.
- [x] 5.5 RED a GREEN: escribir el escenario "Un error inesperado no expone detalles internos", donde el controlador lanza una `RuntimeException("SELECT secreto…")`. Se espera 500 con `detail` genérico y sin el mensaje ni `trace` en el cuerpo. Se verifica con el test en verde.
- [x] 5.6 REFACTOR: extraer constantes (el mensaje genérico y el nombre de la propiedad `codigo`) y revisar que el manejador vuelva a lanzar `AccessDeniedException` y `AuthenticationException` (design D6). Se verifica con toda la suite en verde.

> **Checkpoint MEDIUM 2:** mostrar al usuario el contrato de error (un ejemplo de JSON de 422, 409 con `recurso`, 400 y 500) y el desvío respecto de la firma de CHANGES.md (`mensaje` y `propiedades`).

## 6. Control de acceso (capability `control-acceso`)

- [x] 6.1 RED: escribir `SecurityConfigTest` (`@WebMvcTest` que importa `SecurityConfig`, `ManejadorErrores` y un controlador de prueba anidado con una ruta pública GET y POST y una ruta `@PreAuthorize("hasRole('RECEPCION')")`) con el escenario "Una petición anónima a una ruta protegida recibe 401", que espera un 401 Problem Details en `GET /api/turnos`. Se verifica con el test en rojo.
- [x] 6.2 GREEN: crear `seguridad.infrastructure.SecurityConfig` (con denegación por defecto, las rutas `permitAll` de design D7, CSRF ignorando `/api/public/**`, `@EnableMethodSecurity` y un `UserDetailsService` vacío) y `shared.web.RespuestasSeguridadProblemDetail` (el *entry point* 401 y el *access denied handler* 403 en JSON). Se verifica con el test 6.1 en verde.
- [x] 6.3 Triangular con los escenarios "Una ruta protegida inexistente también recibe 401", "Una ruta pública responde sin autenticación" y "Una escritura pública no exige token CSRF". Se verifica con los tests en verde.
- [x] 6.4 RED a GREEN: escribir los escenarios "Un usuario sin el rol requerido recibe 403 Problem Details" (con `@WithMockUser(roles = "ODONTOLOGO")`) y "Un usuario con el rol requerido accede a la operación" (con `RECEPCION`, que espera 200). Se verifica con los tests en verde, que confirman que el 403 del `@PreAuthorize` no termina en un 500 del manejador genérico.
- [x] 6.5 Verificar que el arranque no imprime "Using generated security password". Se verifica revisando el log de `ContextoAplicacionIT`.

> **Checkpoint MEDIUM 3:** mostrar al usuario las rutas abiertas (`/api/public/**`, `/error` y springdoc solo en dev) y confirmar que C-03 puede reemplazar `SecurityConfig` reutilizando `RespuestasSeguridadProblemDetail`.

## 7. README de arranque

- [x] 7.1 Actualizar el `README.md` raíz (ya existe): en la §2 reemplazar la nota "El código todavía no existe" por los pasos `cp .env.example .env`, `docker compose up -d`, `cd backend && ./mvnw spring-boot:run` (perfil dev por defecto) y `./mvnw verify`. Agregar notas sobre el puerto 5432 ocupado, `docker compose down -v` para recrear la base (PA-10), el formato del `.env` sin comillas y el hecho de que `SPRING_PROFILES_ACTIVE` solo vale si se exporta. Se verifica siguiendo el README tal cual en una terminal limpia.

## 8. Verificación integral

- [x] 8.1 Ejecutar la verificación de compose desde cero (escenarios "El entorno levanta PostgreSQL 16 desde cero" y "La extensión btree_gist está disponible en la base local") con `docker compose down -v && docker compose up -d`, más las consultas de 1.3 y 1.4. El resultado se registra para openspec-verify como verificación manual. **Resultado (2026-11-21):** `down -v` + `up -d --wait` → *healthy*; `select version()` → PostgreSQL 16.16; `btree_gist` disponible. Con el puerto 5432 libre (se detuvo el servicio local `postgresql-x64-18`).
- [x] 8.2 Ejecutar la verificación de secretos (escenarios "El archivo .env no se versiona" y "El archivo .env.example declara todas las variables del entorno") con `git check-ignore .env`, `git ls-files | grep -i env` y la comparación de claves con KB 08. Además, buscar secretos en el diff (`git diff master --stat` y `grep` de `password=`, sin valores reales). **Resultado (2026-11-21):** el usuario comparó las claves de `.env.example` contra la tabla de KB 08 con `Compare-Object` (sin mostrar valores): 16 de 16, sin diferencias. `git check-ignore .env` → ignorado; `.env.example` es el único archivo env versionable.
- [x] 8.3 Ejecutar el escenario "La build completa pasa en una máquina limpia" con `cd backend && ./mvnw clean verify -Dmaven.repo.local=<carpeta-temporal-vacía>` y Docker encendido. Debe terminar en BUILD SUCCESS con todos los `*Test` y `*IT` en verde y el informe de JaCoCo generado.
- [x] 8.4 Revisión de springboot-verification: sin `System.out.println`, sin mensajes de excepción crudos en las respuestas y sin CORS comodín, con todos los tests con `@DisplayName` igual a su escenario. Se verifica con `grep` sobre `backend/src` y con la lista de escenarios contra los `@DisplayName`.
- [x] 8.5 Ejecutar `openspec validate c-01-esqueleto-backend-y-entorno --strict`, que debe pasar sin errores.

## Workflow follow-up

- Correr `openspec-verify`, que genera `verify.md` con el veredicto.
- Preguntar al usuario antes de cada commit (regla dura 16). Un commit por etapa o por change, en español, en la rama `santino`.
- Abrir el PR `C-01 esqueleto-backend-y-entorno` hacia `master`, que revisa Juan Pablo. No hacer push directo a `master`.
- Después del merge: `/opsx:archive`, marcar `[x]` en la ficha de C-01 de CHANGES.md y avisar al equipo que el GATE 1 sigue esperando a C-02.

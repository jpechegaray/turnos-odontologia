# Design

## Context

El repositorio solo tiene documentación (KB, CHANGES.md, design-system, skills). No existen `backend/`, `docker-compose.yml` ni `.env.example`. Sí existen tres archivos que este change reutiliza:

- el `.gitignore`, que ya ignora `.env` y `.env.*` (salvo `!.env.example`) pero no `target/`;
- el `README.md` raíz, que tiene una guía para el equipo y anuncia que C-01 creará el código;
- el `.gitattributes`, que fuerza LF y CRLF para `*.cmd`, lo que conviene a `mvnw` y `mvnw.cmd`.

Las restricciones fijas son DD-01 (stack), DD-02 (monolito modular), DD-03 (sesión, no JWT), DD-05 (zona y `Clock`), DD-08 (Hibernate `ddl-auto` y `schema-constraints.sql`, sin Flyway) y las reglas duras 1 a 17 de CLAUDE.md. La motivación está en proposal.md §Why y el comportamiento exigido en `specs/`.

Este change se apoya en las reglas compactas de varias skills: springboot-tdd, springboot-security, springboot-verification, jpa-patterns, postgresql-table-design y docker-patterns. Dos recomendaciones de esas skills chocan con decisiones del proyecto y no se aplican: Flyway (rige DD-08) y JWT (rige DD-03).

## Goals / Non-Goals

**Goals:**
- Dejar los archivos compartidos (`pom.xml`, `application*.yml`, `schema-constraints.sql`, `shared/`, `SecurityConfig`) en una forma que los changes de la Ola 1 y la Ola 2 puedan **extender sin editar**. Las únicas excepciones son su bloque propio en `application.yml` y el reemplazo del `SecurityConfig` que hace C-03.
- Que el ciclo TDD de los changes siguientes arranque con una base de tests lista: una clase base de integración con Testcontainers, un reloj fijo reemplazable y controladores de prueba aislados.
- Que la build sea reproducible en una máquina limpia (`./mvnw verify` solo necesita JDK 21 y Docker).

**Non-Goals:**
- Ningún endpoint de negocio, ninguna entidad JPA y ningún `@Entity`.
- No se configuran CSRF con cookie, CORS, sesión ni `formLogin`: todo eso es de C-03.
- No hay perfil `prod` ni Dockerfile. `validate` en prod queda como suposición de KB 08.

## Decisions

### D1. Versión de Spring Boot: última 3.5.x
- **Elección:** `spring-boot-starter-parent` en la última versión 3.5.x publicada al momento de aplicar el change, que se verifica en Maven Central. DD-01 y CLAUDE.md piden "Spring Boot 3.x" y 3.5 es la última línea 3.x.
- **Alternativas:**
  - 3.4.x, que ya no tiene soporte OSS.
  - 4.0.x, que tiene soporte vigente pero viola DD-01 (que pide 3.x), trae Jakarta EE 11 y Testcontainers 2 con artefactos renombrados, y deja material de cátedra desactualizado.
- **Riesgo aceptado:** a la fecha (2026-11) el soporte OSS de 3.5 ya terminó, así que no recibe parches gratuitos. El equipo decide quedarse en 3.5.x para no cambiar el plan (DD-01). La migración a 4.x queda fuera del alcance del MVP. Ver Q1 (resuelta).

### D2. `pom.xml` con todas las dependencias del proyecto declaradas desde el inicio
Así se cumple RM-08: ningún change posterior edita el `pom.xml`. Las versiones las fija el BOM de Spring Boot, salvo las que se indican.

| Ámbito | Artefacto | Uso previsto |
|---|---|---|
| compile | `spring-boot-starter-web` | API REST (todos) |
| compile | `spring-boot-starter-data-jpa` | Entidades y JPQL (C-03 en adelante) |
| compile | `spring-boot-starter-validation` | Bean Validation en DTOs |
| compile | `spring-boot-starter-security` | `SecurityConfig` (C-01 y C-03) |
| compile | `spring-boot-starter-mail` | SMTP (C-15); no se autoconfigura sin `spring.mail.host` |
| runtime | `org.postgresql:postgresql` | Driver JDBC |
| compile | `org.springdoc:springdoc-openapi-starter-webmvc-ui` (versión explícita 2.8.x compatible con Boot 3.5, en una property) | Contrato OpenAPI (GATE 2 pide el de C-03) |
| test | `spring-boot-starter-test` | JUnit 5, AssertJ, Mockito, MockMvc |
| test | `spring-security-test` | `@WithMockUser` (C-01 y todos los módulos con `@PreAuthorize`) |
| test | `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql` | PostgreSQL real |
| test | `com.tngtech.archunit:archunit-junit5` (versión explícita en una property) | Test de arquitectura (ver Q4) |

Plugins:
- `spring-boot-maven-plugin`, con el perfil `dev` por defecto para `spring-boot:run` (ver D8);
- `maven-surefire-plugin` para `*Test`;
- `maven-failsafe-plugin` para `*IT`;
- `jacoco-maven-plugin`, que en esta etapa solo genera el informe (ver Q3).

Una dependencia sin usar en C-01, como mail o springdoc, puede requerir configuración mínima para que el contexto arranque. Eso se cubre en D7.

- **Alternativa:** que cada change agregue sus dependencias. Se descarta porque genera conflictos de merge en el `pom.xml` entre changes paralelos (RM-08).

### D3. Maven Wrapper versionado
Se genera con `mvn -N wrapper:wrapper -Dmaven=3.9.x`, en la variante `only-script` (sin `maven-wrapper.jar`). Se versionan `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`. `.gitattributes` ya fija LF para `mvnw` y CRLF para `mvnw.cmd`. A `mvnw` hay que darle permiso de ejecución con `git update-index --chmod=+x`.
- **Alternativa:** exigir Maven instalado. Se descarta porque la versión variaría entre las 6 máquinas.

### D4. Esquema: `ddl-auto` y `schema-constraints.sql`
Se sigue KB 08 §Gestión del esquema y DD-08 al pie de la letra:

- En `application.yml`:
  - `spring.jpa.defer-datasource-initialization: true`;
  - `spring.sql.init.mode: always`;
  - `spring.sql.init.data-locations: classpath:schema-constraints.sql`;
  - `spring.sql.init.separator: "^^^"`;
  - `spring.sql.init.continue-on-error: false`.
- En `application-dev.yml`: `ddl-auto: update`.
- En `src/test/resources/application-test.yml`: `ddl-auto: create-drop`.

El orden de arranque es: Hibernate genera o actualiza el esquema y, al final del arranque del `EntityManagerFactory`, se ejecuta `schema-constraints.sql`. En C-01 el script tiene una sola sentencia y termina con el separador:

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist
^^^
```

El separador `^^^` queda configurado desde ahora, aunque todavía no hay bloques `DO $$ … $$`, para que C-19 solo agregue sus bloques. El test de idempotencia vuelve a ejecutar el script con un `ResourceDatabasePopulator` que usa el mismo separador.

- **Alternativas:**
  - Un script de init del contenedor en `/docker-entrypoint-initdb.d`. Se descarta: solo corre con un volumen vacío y no cubre Testcontainers, y deja dos fuentes de verdad.
  - Flyway, que DD-08 prohíbe.
- **Nota:** `btree_gist` es una extensión *trusted* desde PG 13 y el usuario de la imagen oficial es superusuario, así que `CREATE EXTENSION` no requiere permisos extra.

### D5. Reloj del sistema: un `Clock` como bean
`shared.tiempo.ConfiguracionReloj` declara `@Bean Clock reloj()`, que devuelve `Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"))`. La zona queda en la constante `ZonaHoraria.BUENOS_AIRES` para reutilizarla en conversiones. Toda regla temporal recibe el `Clock` por constructor y nunca llama a `Instant.now()` (KB 05 §Excepciones globales).

En los tests hay dos usos:
- los unitarios usan `Clock.fixed(...)` directamente;
- los de integración importan una `@TestConfiguration` con un `@Bean @Primary Clock` fijo, que vive en `src/test/.../shared/tiempo/RelojFijoConfig`.

Además, Hibernate persiste en UTC con `spring.jpa.properties.hibernate.jdbc.time_zone: UTC`. Jackson ya serializa ISO-8601 por defecto en Boot.

- **Alternativas:**
  - Fijar `TimeZone.setDefault` en la JVM. Se descarta porque oculta el problema y no es testeable.
  - Un `Clock` en UTC y conversión en cada uso. Se descarta porque el KB pide calcular en la zona BA.
  - `@ConditionalOnMissingBean` para reemplazarlo en tests. Se descarta porque depende del orden de carga en una `@Configuration` de usuario; `@Primary` es explícito.

### D6. Errores: `ReglaNegocioException` y `ManejadorErrores`
- `shared.error.ReglaNegocioException extends RuntimeException` tiene estos campos:
  - `codigo` (String, por ejemplo `RN-04`);
  - `estadoHttp` (`HttpStatus`, que se valida como 4xx en el constructor);
  - `mensaje` (el `detail`, que lo escribe quien programa y no puede llevar PII);
  - `propiedades` (`Map<String, Object>`, inmutable, opcional).
- Los módulos lanzan esta excepción o subclases propias, como `SolapamientoException`, que C-19 define con `recurso=PROFESIONAL|SILLON`. Así se cumple KB 05 §Excepciones globales sin editar el manejador.
- `shared.web.ManejadorErrores` es un `@RestControllerAdvice` que extiende `ResponseEntityExceptionHandler`. Con eso, los errores de Spring MVC, como los 400 de `MethodArgumentNotValidException`, salen como `ProblemDetail`. Además:
  - mapea `ReglaNegocioException` a `ProblemDetail.forStatusAndDetail(estado, mensaje)`, con `title` igual a la *reason phrase* del estado, la propiedad `codigo` y las `propiedades` extra;
  - para los 400 de validación agrega la propiedad `errores`: una lista de `{campo, mensaje}` sin el valor rechazado;
  - responde a `Exception` con 500 y un `detail` genérico ("Ocurrió un error inesperado"). La excepción se registra en el log del servidor y no viaja en la respuesta;
  - vuelve a lanzar `AccessDeniedException` y `AuthenticationException`, para que las traduzca Spring Security (D7). Si no, el manejador genérico las convertiría en 500, y un anónimo recibiría 403 en vez de 401.
- Se usa `type` por defecto (`about:blank`). Ver la pregunta abierta Q5.
- **Desvío respecto de CHANGES.md:** la ficha dice `ReglaNegocioException(codigo, estadoHttp)`. Se agregan `mensaje` (sin él no hay `detail`) y `propiedades` (las necesita el 409 por profesional o por sillón). El constructor `(codigo, estadoHttp, mensaje)` sigue siendo el caso común.
- **Alternativas:**
  - Una excepción por estado (`ConflictoException`, `NoEncontradoException`). Se descarta porque multiplica las clases y obliga a tocar el manejador.
  - `ErrorResponseException` de Spring directamente. Se descarta porque acopla el dominio a Spring Web.
- **Nota de capas:** `ReglaNegocioException` usa `HttpStatus` de `spring-web`. Si ArchUnit prohíbe Spring en `..domain..` (Q4), el dominio no podría lanzarla directamente. Hay dos salidas posibles: un enum propio `EstadoError`, o lanzarla solo desde `application`. **Decisión (Q4):** un enum propio en `shared.error` (`CONFLICTO_409`, `NO_PROCESABLE_422`, `NO_ENCONTRADO_404`, `PROHIBIDO_403`, y lo que haga falta) que el manejador traduce. Ver Q4.

### D7. `SecurityConfig` base: denegar por defecto
`seguridad.infrastructure.SecurityConfig` usa `@EnableWebSecurity` y `@EnableMethodSecurity`, y define una `SecurityFilterChain` con estas reglas:
- `authorizeHttpRequests`:
  - `permitAll` para `/api/public/**`, `/error`, `/v3/api-docs/**`, `/swagger-ui/**` y `/swagger-ui.html`;
  - `anyRequest().authenticated()` para todo lo demás.
- CSRF queda activo (el valor por defecto) con `ignoringRequestMatchers("/api/public/**")`. Esto cumple KB 08 §Seguridad, y C-03 cambia el repositorio a `CookieCsrfTokenRepository`.
- No hay `httpBasic` ni `formLogin`.
- `exceptionHandling` usa un `authenticationEntryPoint` (401) y un `accessDeniedHandler` (403) que escriben `ProblemDetail` en JSON. Viven en `shared.web.RespuestasSeguridadProblemDetail` para que C-03 los reutilice.
- Se declara un `UserDetailsService` vacío (un `InMemoryUserDetailsManager` sin usuarios) para que Boot no genere el usuario `user` con contraseña aleatoria en el log.

springdoc queda deshabilitado por defecto (`springdoc.api-docs.enabled: false` y `springdoc.swagger-ui.enabled: false`) y se habilita en `application-dev.yml`. Por eso permitir esas rutas no expone nada fuera de dev.

Los roles se declaran con `@PreAuthorize("hasRole('RECEPCION')")`, y los tests usan `@WithMockUser(roles = ...)`.

- **Alternativa:** dejar abierto todo hasta C-03. Se descarta porque viola la denegación por defecto y RN-20 si alguien mergea un endpoint de escritura antes que C-03.

### D8. Configuración por perfiles y `.env`
- **`application.yml`** contiene `spring.application.name`, la configuración común de JPA y SQL init de D4, `open-in-view: false`, `hibernate.jdbc.time_zone: UTC` y springdoc apagado. Además lleva **un bloque comentado por módulo** con su dueño, en el orden de KB 08: `seguridad` (C-03), `configuracion` (C-08), `notificaciones`/mail (C-15), `ratelimit` (C-17), `reservapublica` (C-14 y C-36), `listaespera` y `agenda`. Cada change descomenta y completa solo su bloque (RM-02).
- **`application-dev.yml`** contiene:
  - `spring.config.import: optional:file:.env[.properties],optional:file:../.env[.properties]`, que cubre la ejecución desde la raíz (IDE) y desde `backend/` (`./mvnw`);
  - el datasource: `url: ${SPRING_DATASOURCE_URL}`, `username: ${POSTGRES_USER}` y `password: ${POSTGRES_PASSWORD}`;
  - `ddl-auto: update`;
  - springdoc encendido.

  El datasource va **solo en dev**: si estuviera en `application.yml`, los placeholders sin resolver romperían el contexto de test cuando no hay `.env`, aunque `@ServiceConnection` pise la URL.
- **`application-test.yml`** contiene `create-drop`. No tiene datasource: lo aporta `@ServiceConnection`.
- **Perfil por defecto para correr:** en el `pom.xml`, `spring-boot-maven-plugin` lleva `<profiles><profile>dev</profile></profiles>`. Así `./mvnw spring-boot:run`, el comando de la ficha, usa dev, y `-Dspring-boot.run.profiles=dev`, el que usa C-33, también funciona. `SPRING_PROFILES_ACTIVE` se mantiene en `.env.example` porque está en el KB, pero el README aclara que solo vale si se exporta en la shell o en el IDE: un `.env` importado no puede activar el mismo perfil que lo importa.
- **`.env.example`** tiene las 16 variables de KB 08 §Variables de entorno con los valores de ejemplo de esa tabla. Las sensibles van con `cambiar-en-env-local`.
- **`docker-compose.yml`** interpola esas variables desde `.env`, que Compose lee solo desde la raíz.
- **Alternativas:**
  - Exportar las variables a mano. Se descarta porque es frágil en Windows.
  - Un plugin dotenv. Se descarta porque es una dependencia extra.
  - Credenciales por defecto en el yml. Se descarta porque viola la regla dura 2.

### D9. `docker-compose.yml`
Hay un solo servicio, `postgres`:
- `image: postgres:16-alpine`, con el major fijo y nunca `latest` (ver Q2);
- `environment`, con `POSTGRES_DB`, `POSTGRES_USER` y `POSTGRES_PASSWORD` interpolados;
- `ports: "127.0.0.1:5432:5432"`, solo localhost;
- el volumen nombrado `turnos-pgdata:/var/lib/postgresql/data`;
- un `healthcheck` con `pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}`;
- `restart: unless-stopped`.

No hay red custom porque es un solo servicio. Una base vacía se recrea con `docker compose down -v` (PA-10).

### D10. Base de tests
- **Convención:** los unitarios son `*Test` (Surefire, sin Spring ni Docker) y los de integración son `*IT` (Failsafe). `./mvnw verify` corre ambos. Todo test lleva `@DisplayName` con el nombre exacto del escenario de la spec (regla dura 11).
- **`ContenedoresPrueba`** es una `@TestConfiguration(proxyBeanMethods = false)` con `@Bean @ServiceConnection PostgreSQLContainer<?>` sobre la imagen `postgres:16-alpine`, que se declara en una constante compartida y documentada como "igual a docker-compose".
- **`PruebaIntegracionBase`** es una clase abstracta con `@SpringBootTest`, `@ActiveProfiles("test")`, `@Import(ContenedoresPrueba.class)` y `@AutoConfigureMockMvc`. El contenedor vive con el contexto de Spring, que se cachea entre clases con la misma configuración: un solo contenedor para toda la suite mientras nadie cambie la configuración del contexto.
- **Por qué esta opción:**
  - Un contenedor `static` con `@Testcontainers`/`@Container` por clase se descarta porque arranca un contenedor por cada clase de test.
  - Un *singleton* manual con un bloque `static` tiene el mismo efecto, pero con más código y fuera del ciclo de vida de Spring.
  - H2 está prohibido por la regla dura 10.
- **Tests de errores y de acceso:** se hacen con `@WebMvcTest`, más rápido y sin Docker, importando `ManejadorErrores`, `SecurityConfig` y un **controlador de prueba anidado** en la clase de test que se registra solo con `@Import`. Los controladores de prueba de `src/test` no se dejan como `@RestController` sueltos bajo `ar.edu.utn.turnos`, porque el escaneo de `@SpringBootTest` los registraría en todos los contextos.
- **Tests con Docker:** `btree_gist`, idempotencia, carga del contexto, versión de PostgreSQL, zona del `Clock` y `RelojFijoConfig` heredan de `PruebaIntegracionBase`.
- **El escenario del perfil dev** (`update`) se verifica con un test unitario que carga `application-dev.yml` con `YamlPropertySourceLoader`. No levanta el contexto dev, que necesitaría `.env`.
- **Escenarios sin test automático:** los de `docker compose` y `.env` (los dos de "Base de datos local" y los dos de "Secretos fuera del repositorio") y "La build completa pasa en una máquina limpia" se verifican con los comandos de la sección 8 de tasks.md. openspec-verify los registrará como verificación manual.

### D11. Estructura de paquetes que crea C-01

```
backend/src/main/java/ar/edu/utn/turnos/
├── TurnosApplication.java
├── shared/
│   ├── error/ReglaNegocioException.java   (+ EstadoError si se acepta Q4)
│   ├── tiempo/ConfiguracionReloj.java, ZonaHoraria.java
│   └── web/ManejadorErrores.java, RespuestasSeguridadProblemDetail.java
└── seguridad/infrastructure/SecurityConfig.java
```

Los demás paquetes de módulo (`agenda`, `pacientes`, …) **no** se crean vacíos: los crea cada change con su primer archivo.

## Risks / Trade-offs

- [El soporte OSS de Spring Boot 3.5 está vencido a la fecha] → Riesgo aceptado (Q1 resuelta, 2026-11-21): se mantiene 3.5.x por DD-01. La migración a 4.x queda como mejora sin asignar.
- [Docker apagado o sin permisos en alguna máquina, que rompe `verify` y compose] → El README lista Docker Desktop como requisito, y springboot-verification informa "no ejecutado" en vez de "pasado".
- [Puerto 5432 ocupado por un PostgreSQL instalado localmente] → El README documenta cómo cambiar el puerto publicado y `SPRING_DATASOURCE_URL`.
- [`ddl-auto: update` no borra ni renombra columnas (PA-10)] → Datos ficticios recreables con `docker compose down -v`, documentado en el README.
- [Un change agrega un `@MockitoBean` o propiedades distintas al contexto de test, eso crea un contexto nuevo y arranca otro contenedor] → Se documenta en `PruebaIntegracionBase` que hay que preferir `@Import` de configuraciones compartidas. Es un costo de tiempo, no de correctitud.
- [El manejador genérico de `Exception` oculta errores durante el desarrollo] → La excepción completa se registra en el log del servidor (sin PII) y la respuesta lleva el mensaje genérico.
- [El `.env` importado como `.properties` conserva las comillas y no expande `${}`] → El README pide `CLAVE=valor` sin comillas, igual que `.env.example`.
- [Declarar dependencias que todavía no se usan, como mail y springdoc, agrega peso y superficie de CVE] → Es un costo aceptado por RM-08. springboot-verification revisa los CVE en cada PR.

## Migration Plan

No hay datos ni versiones previas. El rollback es revertir el PR. En las máquinas del equipo, después del merge, se corre `cp .env.example .env`, luego `docker compose up -d` y luego `cd backend && ./mvnw verify`.

## Open Questions

Q1, Q4 y Q6 quedaron **resueltas por el usuario el 2026-11-21**. Criterio general acordado: mantenerse lo más apegado posible al plan original (CHANGES.md y la KB) y cambiar algo solo ante errores críticos que afecten al plan, no por conveniencia. Las demás preguntas se resuelven con la opción que menos se aparta del plan.

- **Q1 (resuelta): versión de Spring Boot.** Se usa la última 3.5.x, fiel a DD-01 ("Spring Boot 3"). Se acepta que su soporte OSS está vencido; no se tocan DD-01 ni CLAUDE.md. Queda aclarado en DD-01 (trade-offs).
- **Q2: imagen de PostgreSQL.** ¿`postgres:16-alpine` (major fijo) o un minor fijo (`16.x-alpine`) para reproducibilidad total? Recomendación: `16-alpine`, con la misma constante en compose y en Testcontainers.
- **Q3: umbral de JaCoCo.** springboot-tdd sugiere 80%. ¿Se activa `jacoco:check` desde C-01 o solo el informe? Recomendación: solo el informe en C-01. El umbral se activa cuando haya dominio (C-04 o C-19), porque C-01 tiene muy poco código.
- **Q4 (resuelta): test de arquitectura y `HttpStatus` en la excepción.** KB 08 marca ArchUnit como opcional (SU-13), pero CHANGES declara la dependencia. **Resuelta: sí.** C-01 agrega la regla "`..domain..` no importa Spring, JPA ni `jakarta.persistence`" (con `allowEmptyShould`) y `ReglaNegocioException` usa el enum propio `EstadoError` en lugar de `HttpStatus` (D6). Es coherente con KB 08 ("`domain` no importa Spring") y con CHANGES.md, que ya declara ArchUnit. Tarea 3.8 de tasks.md.
- **Q5: campo `type` de Problem Details.** ¿Se usa `about:blank` o una URI por regla (`urn:turnos:regla:RN-04`)? Recomendación: `about:blank` más `codigo`, porque es más simple y la SPA (C-09) mapea por `codigo`.
- **Q6 (resuelta): estado para la validación de formato.** Se respeta el plan tal como está: 409, 422 y 404 según KB 05 y KB 02 para las reglas de negocio. Para la validación de formato la KB no define nada, así que se deja el comportamiento por defecto de Spring (400), sin agregar ninguna regla nueva.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-21
- Mockups: n/a
- Approval: n/a
- Preview: n/a
- Visual verify: n/a

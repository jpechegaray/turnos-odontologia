# Arquitectura Propuesta

Monolito modular por **feature** (un paquete por módulo), con dominio puro separado de la infraestructura y pensado para que **6 personas trabajen en paralelo con mínimos conflictos de merge**.

## Patrones aplicados

| Patrón | Dónde se usa | Por qué |
|---|---|---|
| Monolito modular por feature | Un paquete por módulo bajo `ar.edu.utn.turnos` | Límites claros, un solo despliegue, reparto del trabajo por módulo |
| Capas dentro de cada módulo (`domain` / `application` / `infrastructure`) | Todos los módulos | Dominio puro testeable sin Spring ni base de datos |
| Puertos y adaptadores (hexagonal) | `NotificacionPort` con `SmtpNotificacionAdapter` y `InMemoryNotificacionAdapter`; repositorios como interfaces de dominio | Testear sin SMTP real y sin acoplar el dominio a JPA |
| Value Object `Intervalo` | `agenda.domain` | Encapsula `[inicio, fin)`, `solapaCon`, `contiene` y la regla RN-02 en un solo lugar |
| Funciones puras de política | `PoliticaCancelacion`, `CompatibilidadListaEspera`, `CalculadorDisponibilidad`, `ValidadorTurno` | Reglas críticas testeables con JUnit/AssertJ puro |
| Reloj inyectable (`Clock`) | Plazos, recordatorios, expiración de token | Tests deterministas |
| Defensa en profundidad contra solapamientos | Dominio + `EXCLUDE USING gist` en PostgreSQL | Concurrencia entre recepción y reserva online (RN-15) |
| Bloqueo optimista (`@Version`) | `Turno` | Ediciones simultáneas del mismo turno |
| Problem Details (RFC 7807) | `shared.web.ManejadorErrores` | Errores uniformes con código de regla |
| DTO de entrada/salida separados de entidades | `web` de cada módulo | La API no expone entidades JPA |
| Outbox simplificado (`NotificacionEnviada`) | `notificaciones` | Registro, idempotencia y reintento de emails |

## Estructura de directorios

```
turnos-odontologia/
├── docker-compose.yml            # PostgreSQL 16
├── .env.example                  # versionado; .env fuera del repo
├── .gitignore                    # incluye .env
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/ar/edu/utn/turnos/
│       │   │   ├── TurnosApplication.java
│       │   │   ├── shared/                   # transversal mínimo: Intervalo base, Clock, errores, rate limit, DevDataSeeder
│       │   │   ├── seguridad/                # usuarios, login, roles
│       │   │   │   ├── domain/ application/ infrastructure/ web/
│       │   │   ├── configuracion/            # Configuracion clave-valor
│       │   │   ├── profesionales/            # Profesional, Sillon, HorarioAtencion, Bloqueo, ProfesionalPrestacion
│       │   │   ├── prestaciones/             # Prestacion
│       │   │   ├── pacientes/                # Paciente
│       │   │   ├── agenda/                   # Turno, reglas de solapamiento, disponibilidad
│       │   │   │   ├── domain/               # Intervalo, Turno, EstadoTurno, ValidadorTurno, PoliticaCancelacion, CalculadorDisponibilidad
│       │   │   │   ├── application/          # CrearTurno, ReprogramarTurno, CancelarTurno, MarcarAsistencia, ConsultarAgenda
│       │   │   │   ├── infrastructure/       # TurnoJpaRepository (JPQL), entidades JPA, traducción de violaciones de restricción
│       │   │   │   └── web/                  # TurnoController, DTOs
│       │   │   ├── reservapublica/           # reserva sin login, TokenTurno, ServicioToken
│       │   │   ├── listaespera/              # ListaEspera, OfertaHueco, CompatibilidadListaEspera
│       │   │   └── notificaciones/           # NotificacionPort, Smtp/InMemory adapters, planificador
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── schema-constraints.sql    # btree_gist + EXCLUDE; idempotente
│       └── test/
│           ├── java/ar/edu/utn/turnos/       # espejo de main: dominio (unit), integración (Testcontainers)
│           └── resources/application-test.yml
└── frontend/
    ├── package.json  vite.config.ts  tsconfig.json
    └── src/
        ├── main.tsx  App.tsx
        ├── app/                              # router, providers, guards por rol
        ├── shared/                           # api client, componentes UI, utilidades de fecha (zona BA)
        └── features/
            ├── auth/
            ├── agenda/                       # vistas día/semana, alta y reprogramación de turnos
            ├── pacientes/
            ├── profesionales/                # incluye horarios, bloqueos y sillones
            ├── prestaciones/
            ├── lista-espera/
            ├── configuracion/
            ├── notificaciones/
            └── reserva-publica/              # páginas /reservar y /turno/:token
```

**Suposición:** paquete base `ar.edu.utn.turnos` y Maven; el equipo puede renombrarlos sin cambiar la estructura.

### Reglas de dependencia entre módulos

- Un módulo **no** accede a las entidades JPA ni a los repositorios de otro: se comunica por su interfaz de `application` (servicio o puerto) o por eventos de dominio de Spring.
- `agenda` depende de lectura de `profesionales`, `prestaciones` y `pacientes` mediante interfaces de consulta; no al revés.
- `listaespera` y `notificaciones` reaccionan a eventos (`TurnoCancelado`, `TurnoReprogramado`, `TurnoCreado`) publicados por `agenda`.
- `reservapublica` usa el servicio de creación de turnos de `agenda`; no duplica reglas.
- `domain` no importa Spring, JPA ni jakarta.persistence. Se verifica con un test de arquitectura (ArchUnit, **Suposición:** opcional).

### Cómo se trabaja en paralelo con mínimos conflictos de merge

- **Cada módulo tiene su propio paquete y sus propias entidades JPA.** Dos personas en módulos distintos casi nunca tocan los mismos archivos. El esquema se deriva de las entidades de cada módulo, así que no hay un archivo central de migraciones que dispute el equipo.
- Los archivos compartidos son pocos y de cambio infrecuente: `application.yml`, `schema-constraints.sql`, `shared/` y `docker-compose.yml`. Se modifican con PRs chicos y se avisa al equipo.
- Las entidades referencian a otros módulos solo por **id** (por ejemplo `Long profesionalId`) o con `@ManyToOne` hacia entidades de módulos de menor nivel (`profesionales`, `prestaciones`, `pacientes`), nunca hacia `agenda`.
- Orden sugerido de construcción (dependencias): `shared` → `seguridad` + `configuracion` → `profesionales`, `prestaciones`, `pacientes` (en paralelo) → `agenda` → `reservapublica`, `listaespera`, `notificaciones` (en paralelo) → frontend de cada feature en paralelo con su backend usando el contrato de la API.
- Ramas cortas por módulo, rebase frecuente, y un contrato OpenAPI (`springdoc-openapi`, **Suposición:** se usa) para desacoplar frontend y backend.

## Gestión del esquema (Hibernate)

Decisión del equipo (DD-08): el esquema lo genera **Hibernate** a partir de las entidades JPA, no Flyway.

| Perfil | `spring.jpa.hibernate.ddl-auto` | Notas |
|---|---|---|
| `dev` | `update` | Crea y agrega tablas y columnas. **No borra ni renombra columnas**: si hay un cambio incompatible, se recrea la base ficticia (`docker compose down -v`). |
| `test` | `create-drop` | Cada ejecución arranca con un esquema limpio en un PostgreSQL de Testcontainers |
| `prod` | `validate` (**Suposición:** si algún día hay producción; ver PA-10) | No se cambia el esquema automáticamente |

Lo que Hibernate **no** puede generar (extensión `btree_gist` y las restricciones `EXCLUDE USING gist` con `tstzrange` por profesional y por sillón) va en `src/main/resources/schema-constraints.sql`, que corre **después** de Hibernate:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update          # create-drop en application-test.yml
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
      data-locations: classpath:schema-constraints.sql
      separator: "^^^"          # necesario por los bloques DO $$ ... $$ (ver nota)
```

Forma del script (idempotente):

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist
^^^
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'turno_sin_solape_profesional') THEN
    ALTER TABLE turno ADD CONSTRAINT turno_sin_solape_profesional
      EXCLUDE USING gist (profesional_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
      WHERE (estado <> 'CANCELADO');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'turno_sin_solape_sillon') THEN
    ALTER TABLE turno ADD CONSTRAINT turno_sin_solape_sillon
      EXCLUDE USING gist (sillon_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
      WHERE (estado <> 'CANCELADO');
  END IF;
END $$
^^^
```

Notas y gotchas:

- **Separador:** Spring parte el script por `;`. Los bloques `DO $$ ... $$` contienen `;` internos, así que hay que definir un separador distinto (`^^^`) y usarlo en todo el script.
- **Orden:** `defer-datasource-initialization=true` hace que el script corra después de que Hibernate crea las tablas. Con `create-drop` en tests, las restricciones se recrean en cada ejecución.
- **Tipos:** `inicio` y `fin` deben mapearse a `timestamptz` (`Instant` u `OffsetDateTime`, que Hibernate 6 mapea a `timestamp with time zone` en PostgreSQL). **Suposición:** se verifica con un test de integración que la restricción efectivamente existe.
- **Nombres de columnas y de enumeraciones:** `EnumType.STRING` para `estado`, de modo que el predicado `estado <> 'CANCELADO'` sea válido.
- **Test obligatorio:** un test de integración (Testcontainers) intenta insertar dos turnos solapados y verifica que PostgreSQL rechaza el segundo; otro verifica el borde contiguo (RN-02).
- **Riesgo asumido:** no hay versionado de cambios de esquema; se mitiga con datos ficticios recreables. Ver DD-08 y PA-10.

## Seguridad

- **Autenticación del personal:** sesión de servidor con Spring Security, cookie `HttpOnly`, `SameSite=Lax` y `Secure` fuera de desarrollo. Contraseñas con BCrypt. Bloqueo temporal tras intentos fallidos (RN-34). Justificación en DD-03.
- **CSRF:** habilitado con token en cookie legible por la SPA (`CookieCsrfTokenRepository`) para `/api/**` autenticado. Los endpoints `/api/public/**` no usan sesión y se exceptúan.
- **Autorización:** reglas por ruta y `@PreAuthorize` por servicio; el odontólogo es solo lectura (`403` ante cualquier escritura, RN-33). La SPA oculta opciones pero no es una barrera.
- **Token del paciente (RN-25):** `SecureRandom` 256 bits, base64url; se guarda su hash SHA-256; vigente hasta el inicio del turno o su cancelación; revocación; 404 uniforme ante cualquier fallo; no se registra en logs. El token de oferta de hueco (RN-38) usa el mismo `ServicioToken` y la misma política.
- **Endpoints públicos:** rate limit en memoria por IP (**Suposición:** un filtro propio o Bucket4j; suficiente para una instalación), tope por email, respuestas que no revelan datos de otros pacientes.
- **Validación de entrada:** Bean Validation en DTOs, reglas de negocio en dominio; consultas JPQL parametrizadas (sin concatenar SQL).
- **Cabeceras y CORS:** CORS restringido al origen del frontend; cabeceras de seguridad por defecto de Spring Security. La SPA se sirve detrás del mismo origen (proxy de Vite en dev) para usar cookies sin CORS abierto.
- **Secrets management:** credenciales de SMTP y de base de datos solo en `.env` (excluido por `.gitignore`); se versiona `.env.example` con valores de ejemplo. Nunca secretos en el repositorio ni en la KB.
- **Datos:** solo ficticios. Logs sin datos personales ni tokens. Aplican Ley 25.326 y Ley 26.529 (RN-35).
- **Auditoría mínima:** `creado_por_usuario_id`, `canal`, `cancelado_por`, `NotificacionEnviada`. No es una bitácora completa (PA-11).

## Estrategia de testing

| Nivel | Herramientas | Qué cubre |
|---|---|---|
| Unitario de dominio | JUnit 5 + AssertJ | `Intervalo`, `ValidadorTurno`, `PoliticaCancelacion`, `CompatibilidadListaEspera`, `ServicioToken`; todos los bordes de RN-01, RN-02, RN-04, RN-05 |
| Integración de persistencia | Spring Boot Test + Testcontainers (PostgreSQL 16 real) | Consultas JPQL, restricciones `EXCLUDE`, carrera de dos transacciones sobre el mismo hueco |
| Integración web | `@SpringBootTest` + MockMvc | Roles, códigos 409/422/404, rate limit, flujo de token |
| Notificaciones | `InMemoryNotificacionAdapter` | Recordatorio, idempotencia y reintentos sin SMTP real |
| Frontend | Vitest + Testing Library | Formularios, guards por rol, manejo de 409 y de horarios en zona BA |

Enfoque **TDD** para el dominio (Red-Green-Refactor): primero los tests de borde de RN-02, luego la implementación.

## Entorno y despliegue

`docker-compose.yml` levanta PostgreSQL 16 con volumen. El backend y el frontend se ejecutan localmente (`mvn spring-boot:run`, `npm run dev`). **Suposición:** no se define despliegue productivo en esta etapa; un `Dockerfile` por servicio queda como mejora.

## Variables de entorno

Definidas en `.env.example` (versionado, sin secretos reales).

| Variable | Descripción | Ejemplo | Sensible |
|---|---|---|---|
| `POSTGRES_DB` | Nombre de la base | `turnos` | N |
| `POSTGRES_USER` | Usuario de la base | `turnos_app` | N |
| `POSTGRES_PASSWORD` | Contraseña de la base | `cambiar-en-env-local` | **Y** |
| `SPRING_DATASOURCE_URL` | URL JDBC | `jdbc:postgresql://localhost:5432/turnos` | N |
| `SPRING_PROFILES_ACTIVE` | Perfil | `dev` | N |
| `SMTP_HOST` | Servidor SMTP | `smtp.ejemplo.test` | N |
| `SMTP_PORT` | Puerto SMTP | `587` | N |
| `SMTP_USER` | Usuario SMTP | `usuario-smtp` | **Y** |
| `SMTP_PASSWORD` | Contraseña SMTP | `cambiar-en-env-local` | **Y** |
| `SMTP_FROM` | Remitente | `turnos@consultorio.test` | N |
| `APP_PUBLIC_URL` | URL base para los enlaces con token | `http://localhost:5173` | N |
| `APP_CORS_ORIGIN` | Origen permitido del frontend | `http://localhost:5173` | N |
| `APP_COOKIE_SECURE` | Cookie `Secure` | `false` en dev | N |
| `SEED_ADMIN_PASSWORD` | Contraseña del admin ficticio del seed (solo dev) | `cambiar-en-env-local` | **Y** |
| `SEED_STAFF_PASSWORD` | Contraseña de los demás usuarios ficticios (solo dev) | `cambiar-en-env-local` | **Y** |
| `VITE_API_BASE_URL` | Base de la API en el frontend | `/api` | N |

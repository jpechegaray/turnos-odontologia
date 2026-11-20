# Descripción General

## Stack tecnológico

Decisiones ya tomadas por el equipo (ver `09_decisiones_y_supuestos.md`, DD-01 y siguientes).

| Capa | Tecnologías | Versión mínima |
|---|---|---|
| Backend | Java, Spring Boot (Spring Web, Spring Data JPA con JPQL, Bean Validation, Spring Security, Spring Mail) | Java 21, Spring Boot 3.x |
| Base de datos | PostgreSQL en Docker (`docker compose`), extensión `btree_gist` | PostgreSQL 16 |
| Esquema | Hibernate (`ddl-auto`) a partir de las entidades JPA, más un script SQL idempotente para `btree_gist` y las restricciones `EXCLUDE` | Incluido en Spring Boot 3 |
| Frontend | React + TypeScript con Vite | React 18, TypeScript 5 |
| Tests backend | JUnit 5, Spring Boot Test, AssertJ, Testcontainers (PostgreSQL real) | Versiones del BOM de Spring Boot |
| Tests frontend | Vitest + Testing Library | Vitest 1.x |
| Email | SMTP real vía Spring Mail; credenciales solo en `.env`, con `.env.example` versionado | n/a |
| Build | Maven (**Suposición:** Maven en vez de Gradle por ser más habitual en la cátedra) | Maven 3.9 |
| Zona horaria | `America/Argentina/Buenos_Aires`; persistencia en UTC (`timestamptz`) | n/a |

## Arquitectura general

**Monolito modular** desplegado como un solo backend, más una SPA estática. Un consultorio por instalación (single-tenant). La lógica de dominio es pura y testeable, separada de la infraestructura.

```
 Navegador (personal)        Navegador (paciente anónimo con token)
        │                                   │
        └────────────┬──────────────────────┘
                     ▼
        SPA React + TypeScript (Vite)
                     │  HTTP/JSON (sesión por cookie para personal;
                     │  token en la ruta para el paciente)
                     ▼
   ┌───────────────────────────────────────────────┐
   │ Backend Spring Boot (monolito modular)         │
   │  seguridad │ agenda(turnos) │ pacientes        │
   │  profesionales(+sillones) │ prestaciones       │
   │  reservapublica │ listaespera                  │
   │  notificaciones │ configuracion                │
   │  Dominio puro  ◄── puertos ──► Infraestructura │
   └───────────────┬───────────────┬───────────────┘
                   ▼               ▼
        PostgreSQL 16 (Docker)   Servidor SMTP
        EXCLUDE USING gist       (adaptador SMTP;
        sobre tstzrange          adaptador de prueba en tests)
```

Justificación de alto nivel:

- **Monolito modular** y no microservicios: el equipo es de 6 personas, una sola instalación por consultorio y un plazo académico. Los módulos tienen límites claros (un paquete por módulo) para trabajar en paralelo sin conflictos de merge.
- **Dominio puro** (reglas de solapamiento, políticas de cancelación, compatibilidad de lista de espera, generación y validación de tokens) sin dependencias de Spring ni JPA: se testea con JUnit y AssertJ sin levantar contexto.
- **Doble defensa contra solapamientos:** validación en dominio (mensajes claros) y restricción `EXCLUDE USING gist` en PostgreSQL (garantía final ante concurrencia entre recepción y reserva online). Ver `05_reglas_de_negocio.md` (RN-01, RN-02, RN-15).
- **Notificaciones detrás de un puerto** (interfaz) con un adaptador SMTP y un adaptador de prueba, para testear sin servidor de correo.
- **Un reloj inyectable** (`java.time.Clock`) para testear plazos de cancelación y recordatorios sin depender de la hora real.

## Integraciones externas

| Servicio | Propósito | Tipo | En el MVP |
|---|---|---|---|
| Servidor SMTP | Recordatorios y notificaciones por email | SMTP vía Spring Mail | Sí, real |
| PostgreSQL | Persistencia | JDBC | Sí (Docker) |
| WhatsApp Business API | Recordatorios y confirmaciones | REST (Meta) | No, etapa posterior |
| Mercado Pago, ARCA, obras sociales, Google Calendar | Cobros, facturación, validación, sincronización | REST/SOAP/SDK | No, etapa posterior |

## API REST (resumen)

Prefijo `/api`. JSON. Errores en formato RFC 7807 (`ProblemDetail`). Fechas en ISO-8601 con offset; el backend persiste en UTC y la SPA muestra en `America/Argentina/Buenos_Aires`. Detalle de contratos a definir en cada change (**Suposición:** los endpoints exactos se afinan en el diseño de cada módulo).

| Recurso | Endpoints principales | Acceso |
|---|---|---|
| Auth | `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` | Público (login) / autenticado |
| Turnos | `GET /api/turnos?desde&hasta&profesionalId&sillonId`, `POST /api/turnos`, `PUT /api/turnos/{id}/reprogramar`, `POST /api/turnos/{id}/cancelar`, `POST /api/turnos/{id}/asistencia` | RECEPCION, ADMIN (incluye marcar asistencia); ODONTOLOGO solo lectura (`GET`) |
| Disponibilidad | `GET /api/disponibilidad?profesionalId&prestacionId&desde&hasta` | Autenticado |
| Pacientes | `GET/POST/PUT /api/pacientes`, `GET /api/pacientes/{id}` | RECEPCION, ADMIN |
| Profesionales | `GET/POST/PUT /api/profesionales`, `GET/PUT /api/profesionales/{id}/horarios`, `GET/POST/DELETE /api/profesionales/{id}/bloqueos` | ADMIN y RECEPCION para escribir (horarios y bloqueos); ODONTOLOGO solo lectura de los propios |
| Sillones | `GET/POST/PUT /api/sillones` | ADMIN (lectura para todos los roles del personal) |
| Prestaciones | `GET/POST/PUT /api/prestaciones` | ADMIN (lectura para el personal) |
| Lista de espera | `GET/POST/DELETE /api/lista-espera`, `GET /api/lista-espera/sugerencias?turnoId`, `POST /api/lista-espera/{id}/asignar` | RECEPCION, ADMIN |
| Configuración | `GET/PUT /api/configuracion` | ADMIN |
| Usuarios | `GET/POST/PUT /api/usuarios` | ADMIN |
| Reserva pública | `GET /api/public/disponibilidad`, `GET /api/public/prestaciones`, `GET /api/public/profesionales`, `POST /api/public/reservas` | Público, con rate limit |
| Turno del paciente (token) | `GET /api/public/turnos/{token}`, `POST /api/public/turnos/{token}/confirmar`, `POST /api/public/turnos/{token}/cancelar` | Público, el token da acceso solo a su turno (RN-07); no reprograma |
| Oferta de hueco (token) | `GET /api/public/ofertas/{token}`, `POST /api/public/ofertas/{token}/tomar` | Público, token propio de la oferta (RN-38) |

Códigos HTTP relevantes: `409 Conflict` ante un solapamiento (RN-01), `422` ante violación de reglas de negocio (fuera de horario, bloqueo, plazo), `404` ante token inexistente, vencido o inválido (sin distinguir causas, para no filtrar información).

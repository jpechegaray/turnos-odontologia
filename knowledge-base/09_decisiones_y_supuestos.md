# Decisiones y Supuestos

Las decisiones **DD-01 a DD-08** fueron tomadas por el equipo (no se reabren sin acuerdo). **DD-09 a DD-14** son decisiones de producto del Discovery respaldadas por la evidencia del informe de mercado. Los supuestos (SU) son inferencias que el equipo debe validar; cada uno se marca también como `**Suposición:**` donde aparece en la KB.

## Decisiones documentadas

### DD-01 — Stack: Java 21 + Spring Boot 3, PostgreSQL 16 y React + TypeScript
**Decisión**: Backend con Java 21 y Spring Boot 3 (Spring Web, Spring Data JPA con consultas JPQL, Bean Validation, Spring Security, Spring Mail). Base de datos PostgreSQL 16 en Docker (`docker compose`). Frontend React + TypeScript con Vite. Tests con JUnit 5, Spring Boot Test, AssertJ y Testcontainers en backend; Vitest + Testing Library en frontend. Email SMTP real.
**Contexto**: el equipo vio en la cátedra TypeScript, JavaScript, Spring Boot, JPA/JPQL, FastAPI, PostgreSQL y React. El proyecto exige API con lógica de dominio y tests automatizados, más un front web simple.
**Alternativas consideradas**:
- **FastAPI (Python):** descartada. Es viable y más liviana, pero no aprovecha JPA/JPQL, que el equipo sí cursó y que encaja con un modelo relacional rico (turnos, sillones, profesionales). Además, el ecosistema de Spring (Security, Validation, Testcontainers) cubre el resto de los requisitos sin armar piezas a mano.
- **Node/TypeScript full stack (por ejemplo NestJS o Express + un ORM):** descartada. Permitiría un solo lenguaje en front y back, pero el equipo no cursó un ORM equivalente a JPA en Node ni el ecosistema de testing con Testcontainers en ese stack; se priorizó el conocimiento común del equipo sobre la unificación de lenguaje.
**Justificación**: se eligió Spring Boot para aprovechar JPA/JPQL, con tipado fuerte para un dominio con reglas sensibles, y Testcontainers permite probar contra PostgreSQL real, necesario para validar las restricciones `EXCLUDE`.
**Trade-offs aceptados**: mayor verbosidad que Python o Node; dos lenguajes en el proyecto (Java y TypeScript); arranque y consumo de memoria de la JVM.

### DD-02 — Monolito modular, single-tenant, un consultorio por instalación
**Decisión**: una sola aplicación backend con módulos por feature (agenda/turnos, pacientes, profesionales y sillones, prestaciones, reserva pública, lista de espera, notificaciones y configuración, más seguridad). Un consultorio por instalación; multi-tenant y multisede quedan para etapas posteriores.
**Contexto**: consultorio de 2 a 5 profesionales (menos de 50 usuarios internos), equipo de 6 con plazo académico.
**Alternativas consideradas**: microservicios; monolito en capas técnicas (controllers/services/repositories globales); multi-tenant desde el inicio.
**Justificación**: simplicidad operativa y de testing; los paquetes por módulo permiten trabajo en paralelo con pocos conflictos de merge (ver `08`).
**Trade-offs aceptados**: pasar a multi-tenant implica agregar una dimensión (consultorio o sede) al modelo; los módulos comparten una base de datos.

### DD-03 — Autenticación del personal: sesión de servidor con cookie HttpOnly
**Decisión**: sesión de servidor (Spring Security, `HttpSession`) con cookie `HttpOnly`, `SameSite=Lax` y CSRF habilitado; contraseñas con BCrypt.
**Contexto**: el enunciado permite sesión o JWT. La SPA y la API se sirven desde el mismo origen (proxy de Vite en desarrollo), hay una sola instancia del backend y menos de 50 usuarios.
**Alternativas consideradas**: JWT stateless (access token, opcionalmente con refresh).
**Justificación**: con una sola instancia no se necesita la escalabilidad stateless de JWT. La cookie `HttpOnly` evita que un XSS robe el token, que con JWT típicamente quedaría en `localStorage` o en memoria. Logout e invalidación (bloqueo o baja de un usuario) son inmediatos; con JWT requieren listas de revocación o expiraciones cortas. Es la opción con menos código propio y menos partes para equivocarse.
**Trade-offs aceptados**: las sesiones viven en memoria y se pierden al reiniciar el backend (**Suposición:** aceptable en esta etapa); hay que manejar CSRF; una futura app móvil o varias instancias requerirían sesión compartida (por ejemplo Spring Session JDBC) o migrar a JWT.

### DD-04 — Prevención de solapamientos: dominio + restricción de exclusión en PostgreSQL
**Decisión**: intervalos semiabiertos `[inicio, fin)`; validación en dominio (mensajes claros) y además `EXCLUDE USING gist` con `tstzrange(inicio, fin, '[)')` por profesional y por sillón, con la extensión `btree_gist`, sobre turnos no cancelados.
**Contexto**: RN-01 y RN-02; riesgo de concurrencia entre recepción y reserva online (`discovery.md` §10).
**Alternativas consideradas**: solo validación en la aplicación (insuficiente ante concurrencia); bloqueo pesimista de filas del profesional y del sillón (más complejo, no cubre todos los casos); nivel de aislamiento `SERIALIZABLE` (reintentos y menor rendimiento).
**Justificación**: la base de datos es la única garantía atómica; el dominio aporta experiencia de usuario y testeo.
**Trade-offs aceptados**: dependencia de PostgreSQL y de `btree_gist`; hay que traducir la violación de la restricción a `409`; la restricción no la genera Hibernate (ver DD-08).

### DD-05 — Zona horaria `America/Argentina/Buenos_Aires`, persistencia en UTC
**Decisión**: columnas `timestamptz`; todo cálculo de horario de atención, plazos y recordatorios en `America/Argentina/Buenos_Aires`; `Clock` inyectable.
**Contexto**: riesgo de errores de zona horaria y de borde (`discovery.md` §10).
**Alternativas consideradas**: guardar hora local sin zona (`timestamp`); guardar el desfase fijo (-03:00).
**Justificación**: UTC elimina ambigüedades; usar `ZoneId` evita asumir un desfase si la regla cambiara.
**Trade-offs aceptados**: conversión explícita en los bordes (API, UI y horarios de atención en `time` local).

### DD-06 — Token del paciente: aleatorio, hasheado, con expiración y alcance limitado
**Decisión**: 256 bits con `SecureRandom`, base64url; se persiste solo el hash SHA-256; válido desde la reserva hasta el inicio del turno o su cancelación (lo que ocurra primero) y revocado al cancelar; de un solo turno; solo da acceso a su turno (RN-07, RN-25). Sin login de pacientes. Decisión del líder 2026-11-20 (resuelve PA-01). Las ofertas de la lista de espera usan un token propio con la misma política (RN-38).
**Contexto**: el paciente no tiene cuenta; el riesgo es que el token se filtre o se pueda adivinar (`discovery.md` §10).
**Alternativas consideradas**: UUID v4 (menos entropía y formato predecible); JWT firmado con datos del turno (no revocable); código corto por email (adivinable).
**Justificación**: con 256 bits de entropía la adivinanza es inviable; guardar solo el hash protege ante una filtración de la base.
**Trade-offs aceptados**: no se puede reenviar el mismo enlace si se pierde (hay que generar uno nuevo o reenviar el email original); el enlace en el email es un secreto en tránsito.

### DD-07 — Notificaciones detrás de un puerto; solo email SMTP en el MVP
**Decisión**: `NotificacionPort` con un adaptador SMTP (Spring Mail) y un adaptador de prueba en memoria; registro de cada envío en `NotificacionEnviada` con idempotencia y reintentos. WhatsApp queda para una etapa posterior.
**Contexto**: el email es el canal más simple y sin costo variable; Meta cobra por mensaje de WhatsApp (informe C.3 y D.3).
**Alternativas consideradas**: llamadas directas al `JavaMailSender` desde los servicios (acopla y complica los tests); WhatsApp desde el inicio.
**Justificación**: el puerto permite testear sin servidor SMTP y agregar canales sin tocar el dominio.
**Trade-offs aceptados**: una capa de indirección extra; un planificador `@Scheduled` en la misma instancia (sin cola externa).

### DD-08 — Esquema con Hibernate `ddl-auto` en lugar de Flyway
**Decisión**: **Esquema con Hibernate ddl-auto (lo que el equipo conoce) en lugar de Flyway.** `ddl-auto=update` en desarrollo y `create-drop` en tests (Testcontainers). Lo que Hibernate no puede generar (`CREATE EXTENSION btree_gist` y las restricciones `EXCLUDE USING gist` con `tstzrange` por profesional y por sillón) va en un script SQL idempotente `src/main/resources/schema-constraints.sql`, ejecutado después de Hibernate con `spring.jpa.defer-datasource-initialization=true` y `spring.sql.init.mode=always`. El script usa `CREATE EXTENSION IF NOT EXISTS` y bloques `DO` que agregan cada restricción solo si no existe.
**Contexto**: el equipo maneja el esquema con JPA en la cátedra y prefiere no sumar una herramienta nueva en un proyecto académico con plazo.
**Alternativas consideradas**: Flyway con migraciones numeradas por módulo (descartada por decisión del equipo); Liquibase.
**Justificación**: menos piezas que aprender; las entidades de cada módulo ya definen su esquema, lo que además ayuda al trabajo en paralelo (ver `08`).
**Trade-offs aceptados / riesgos**:
- No hay versionado de migraciones ni historial auditable del esquema.
- `update` no borra ni renombra columnas (deja columnas huérfanas o crea duplicadas ante un renombre) y puede fallar ante cambios incompatibles de tipo o de nulabilidad.
- El script de restricciones corre en cada arranque (por eso debe ser idempotente) y los bloques `DO` requieren un separador de sentencias distinto de `;`.
**Mitigación**: los datos son ficticios y recreables (`docker compose down -v`); tests de integración que verifican que las restricciones existen; evaluar Flyway si el proyecto pasa a producción (PA-10).

### DD-09 — MVP respaldado por la evidencia del mercado (agenda por sillón con reglas explícitas)
**Decisión**: el MVP se concentra en agenda por profesional y por sillón con prevención de solapamientos, duración por prestación, bloqueos y horarios, cancelación con plazo configurable, reserva online con token y lista de espera.
**Evidencia** (`informe-discovery.md`, C.3 y D.3): solo 2 de 23 sistemas declaran prevención de solapamientos, 7 gestión por sillón y 3 duración variable; ninguno argentino evidencia las tres a la vez; ninguna ficha documenta plazo de cancelación configurable.
**Alternativas consideradas**: competir en gestión clínica integral (odontograma, historia clínica), donde el estándar de mercado ya está cubierto (C.1) y el peso del esfuerzo es mucho mayor.
**Trade-offs aceptados**: el producto no es un software de gestión clínica completo. La evidencia es mayormente comercial y no fue verificada por el equipo (IN-04).

### DD-10 — Diferenciador: lista de espera con aviso automático a pacientes compatibles
**Decisión** (revisada 2026-11-20 por el líder, resuelve PA-02): al liberarse un hueco, el sistema **avisa solo por email a todas las entradas compatibles** (profesional, prestación, duración que entra en el hueco, fechas y franja), cada una con un enlace con token; **el primero que confirma se queda con el turno** (RN-22, RN-23, RN-24, RN-38). Si nadie confirma antes del inicio del hueco, queda libre.
**Evidencia** (C.2, C.3.6, C.4.2): hay lista de espera en 6 de 23 productos; DriCloud avisa a todos y gana el primero; Open Dental envía un enlace ASAP por paciente; ninguna ficha describe filtro por compatibilidad. El diferenciador es combinar el aviso masivo con el filtro de compatibilidad.
**Alternativas consideradas**: que el sistema solo proponga y recepción elija a quién ofrecerle (decisión anterior, descartada: más trabajo para recepción, que debe ser lo más simple posible); asignación automática sin confirmación del paciente.
**Trade-offs aceptados**: varios pacientes reciben aviso por un mismo hueco y solo uno lo logra (los demás ven "hueco ya tomado"); hay que modelar la oferta con su token (`OfertaHueco`) y manejar la concurrencia (RN-15); más emails (ver SU-14).

### DD-11 — Reserva y gestión del turno sin cuenta, por enlace único
**Decisión**: sin login de pacientes; el token da ver, confirmar y cancelar solo su turno.
**Evidencia** (C.4.3, D.2.1): Open Dental comprueba el patrón de enlace único por paciente; DentalCore y Nubimed declaran confirmar o cancelar con enlace.
**Trade-offs aceptados**: sin historial de turnos del paciente ni reprogramación autoservicio (confirmado por el líder 2026-11-20: el paciente solo cancela; reprograma recepción, RN-18).

### DD-12 — Rol de recepción mínimo y tres roles
**Decisión**: ODONTOLOGO, RECEPCION y ADMIN.
**Evidencia** (C.4.5, D.3): el mercado ofrece hasta 17 perfiles de rol (Dentia) y 10 (DentalTec); más de los que necesita un MVP.
**Trade-offs aceptados**: permisos poco granulares; cualquier necesidad futura requiere ampliar la matriz.

### DD-13 — Email antes que WhatsApp para recordatorios
**Decisión**: recordatorio por email SMTP real en el MVP.
**Evidencia** (D.3): WhatsApp aparece en 17 de 23 productos pero con costo variable de Meta y cuenta oficial; email es sin costo variable.
**Trade-offs aceptados**: menor tasa de lectura que WhatsApp; se mitiga dejando el puerto de notificaciones listo para otro canal.

### DD-14 — Obra social como dato; ARCA y Mercado Pago fuera del MVP
**Decisión**: la obra social es un campo de la ficha; sin cobertura, copago, liquidación, facturación ni cobros.
**Evidencia** (D.3): ARCA declarada en 5 productos y ninguno la comprueba; la validación online de obras sociales la declara solo DentalTec y exige convenios de terceros.

## Supuestos inferidos

### SU-01 — Granularidad de 5 minutos y duración entre 5 y 480 minutos
**Supuesto**: inicios y duraciones en múltiplos de 5 minutos.
**Origen**: inferido de las prestaciones habituales de un consultorio; el Discovery no define granularidad.
**Riesgo si es falso**: hay prestaciones o turnos que no encajan.
**Cómo validar**: confirmar con el equipo la granularidad deseada.

### SU-02 — El odontólogo es solo lectura; horarios y bloqueos los gestionan recepción y ADMIN
**Estado**: la parte principal quedó **resuelta por decisión del líder (2026-11-20, PA-03)**: el odontólogo solo ve su agenda y la ficha básica de sus pacientes; no da, reprograma ni cancela turnos, no marca asistencia y no define horarios ni bloqueos. Pide los cambios a recepción fuera del sistema.
**Supuesto que sigue vigente**: la reasignación de acciones elegida por el equipo de la KB: asistencia o ausencia → recepción (corrige ADMIN); horarios de atención y bloqueos → **recepción y administrador**, con CRUD de todos los profesionales (recepción conoce la agenda y resuelve los conflictos con turnos, RN-17).
**Origen**: `discovery.md` §2 y §11.
**Riesgo si es falso**: si recepción no debe tocar horarios, hay que limitarlos a ADMIN (cambia solo la matriz de permisos).
**Cómo validar**: confirmar con el líder la asignación a recepción.

### SU-03 — Reserva online: el sistema asigna el sillón
**Supuesto**: el paciente elige prestación, profesional y horario; el sistema asigna el primer sillón activo libre.
**Origen**: el paciente no conoce los sillones; `discovery.md` no lo especifica.
**Riesgo si es falso**: el riesgo de que ciertas prestaciones requieran un sillón específico quedó descartado: el líder decidió (2026-11-20, PA-04) que **cualquier sillón libre sirve** (RN-11).
**Cómo validar**: PA-04 (resuelta). Queda abierta solo la pregunta de PA-09 (si el paciente puede elegir "cualquier profesional").

### SU-04 — Datos mínimos de la reserva online y deduplicación de pacientes
**Estado**: la vigencia del token (inicio del turno o cancelación) y los datos pedidos (nombre completo, email, teléfono y obra social; sin DNI) quedaron **resueltos por decisión del líder (2026-11-20, PA-01)**: RN-25, RN-26.
**Supuesto que sigue vigente**: la **deduplicación** de pacientes no está decidida en detalle. Se asume: por email exacto (y teléfono si coincide, para desambiguar), reutilizando la ficha sin modificar sus datos (RN-28); sin fusión de duplicados (p. ej. una ficha cargada por recepción con otro email genera una segunda ficha). Se mantiene también la aceptación del tratamiento de datos (Ley 25.326) y se admite "Particular" como obra social.
**Origen**: preguntas abiertas de `discovery.md` §11.
**Riesgo si es falso**: fichas duplicadas, o que una ficha existente se asigne a quien no corresponde (por eso no se modifican sus datos).
**Cómo validar**: revisión con el líder y, si hace falta, una herramienta de fusión de fichas en una etapa posterior.

### SU-05 — La lista de espera avisa sola y gana el primero que confirma
**Estado**: **resuelto por decisión del líder (2026-11-20, PA-02)**; reemplaza al supuesto anterior ("el sistema propone y recepción elige"). Al liberarse un hueco el sistema avisa por email a todos los compatibles y el primero que confirma se queda con el turno (RN-23, RN-24, RN-38; DD-10). Los detalles no decididos están en SU-14 y SU-15.
**Origen**: `discovery.md` §3 CU5 ("me proponga pacientes", redacción anterior a la decisión) y §11.
**Riesgo si es falso**: ninguno pendiente sobre el principio; ver SU-14.
**Cómo validar**: PA-02 (resuelta).

### SU-06 — Recordatorio 24 hs antes, configurable
**Supuesto**: por defecto 24 hs de anticipación, un solo recordatorio.
**Origen**: `discovery.md` §11 (24 hs, 48 hs o configurable).
**Riesgo si es falso**: ajustar el default y eventualmente permitir varios recordatorios.
**Cómo validar**: PA-05.

### SU-07 — Reprogramación tardía: se permite y se registra
**Supuesto**: el personal puede reprogramar fuera de plazo; queda `reprogramado_tarde = true`. (Que **el paciente no reprograma por token** ya no es supuesto: lo decidió el líder el 2026-11-20; solo cancela, RN-18, RN-19.)
**Origen**: RN-05 menciona cancelar y reprogramar, pero solo define el registro como tardía para la cancelación.
**Riesgo si es falso**: falta una política de penalidad o bloqueo.
**Cómo validar**: IN-02 y PA-06.

### SU-08 — Bloqueos solo por profesional y horarios solo por profesional
**Supuesto**: no hay horario general del consultorio ni bloqueo por sillón; un sillón se saca de servicio desactivándolo.
**Origen**: `discovery.md` §5 habla de horarios y bloqueos "por profesional".
**Riesgo si es falso**: no se puede modelar mantenimiento de un sillón en un intervalo.
**Cómo validar**: PA-12.

### SU-09 — Relación profesional-prestación
**Supuesto**: cada profesional realiza un subconjunto de prestaciones (`ProfesionalPrestacion`).
**Origen**: necesaria para la compatibilidad de la lista de espera y para que la reserva online ofrezca combinaciones válidas.
**Riesgo si es falso**: carga administrativa innecesaria si todos hacen todo.
**Cómo validar**: confirmar con la cátedra o con un consultorio ficticio de referencia.

### SU-10 — Sesiones en memoria y una sola instancia
**Supuesto**: una instancia del backend; las sesiones se pierden al reiniciar.
**Origen**: DD-02 y la escala (menos de 50 usuarios).
**Riesgo si es falso**: con más instancias hay que compartir sesión y planificador (el planificador de recordatorios podría ejecutarse duplicado; la restricción única de `NotificacionEnviada` mitiga el doble envío).
**Cómo validar**: antes de cualquier despliegue con más de una instancia.

### SU-11 — Límites y parámetros numéricos por defecto
**Supuesto**: horizonte 60 días, anticipación mínima 2 hs, tope 3 turnos activos por email, 5 intentos de login y bloqueo de 15 minutos, 3 reintentos de email.
**Origen**: valores razonables propuestos para un MVP; el Discovery no los define.
**Riesgo si es falso**: fricción o abuso; todos son configurables o fáciles de ajustar.
**Cómo validar**: revisión en demo con el equipo.

### SU-12 — Estado CONFIRMADO y corrección de asistencia
**Supuesto**: `CONFIRMADO` lo fija el paciente (token) o el personal; la asistencia la marca recepción (o ADMIN) solo desde el inicio del turno y solo ADMIN corrige.
**Origen**: el Discovery menciona "confirmarlo" (RN-07) y "marca asistencia o ausencia" sin detallar estados.
**Riesgo si es falso**: estados o transiciones distintos.
**Cómo validar**: revisión del diagrama de estados de RN-12.

### SU-13 — Herramientas auxiliares
**Supuesto**: Maven, `springdoc-openapi`, rate limit en memoria y ArchUnit opcional.
**Origen**: elecciones habituales de bajo costo; el equipo no las fijó.
**Riesgo si es falso**: reemplazo sencillo, sin impacto en el dominio.
**Cómo validar**: acuerdo del equipo al iniciar el primer change.

### SU-14 — Detalles del aviso automático de la lista de espera
**Supuesto**: (a) se avisa a lo sumo a `lista_espera.max_avisos_por_hueco` entradas (default 10), por antigüedad; (b) un solo aviso por paciente y por hueco, y una sola tanda; (c) el hueco se define por profesional e intervalo, el sillón es indistinto; (d) el turno resultante empieza al inicio del hueco, con la duración por defecto de la prestación, en el sillón del turno liberado si sigue libre o, si no, en el primero activo libre; el resto del hueco, si sobra, queda libre sin un nuevo aviso; (e) la oferta vence al inicio del hueco y la anticipación mínima de la reserva online (RN-27) no se aplica; (f) el paciente de una entrada debe tener email; (g) el aviso fallido se reintenta (RN-30) hasta el inicio del hueco.
**Origen**: la decisión del líder (2026-11-20, PA-02) fija el principio pero no estos detalles.
**Riesgo si es falso**: demasiados o pocos avisos; huecos que se desaprovechan o un aviso repetido.
**Cómo validar**: revisión con el líder; el tope es configurable.

### SU-15 — Recepción conserva la asignación manual del hueco
**Supuesto**: además del aviso automático, recepción ve las entradas compatibles y puede asignar el hueco a una de ellas (por ejemplo, tras un llamado). Compite con las ofertas bajo RN-15 (gana el primero).
**Origen**: ya existía en el diseño anterior (`POST /api/lista-espera/{id}/asignar`); la decisión del líder no lo menciona.
**Riesgo si es falso**: si se quiere solo el camino automático, se elimina ese endpoint y la pantalla de sugerencias.
**Cómo validar**: confirmar con el líder.

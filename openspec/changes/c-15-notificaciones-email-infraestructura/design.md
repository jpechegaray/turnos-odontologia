# Design

## Context

Ver [proposal.md](proposal.md) para la motivación y [la especificación](specs/notificaciones-email/spec.md) para el contrato. El estado actual relevante:

- C-01 ya provee `Clock` (`shared.tiempo.ConfiguracionReloj`), `ZonaHoraria.BUENOS_AIRES`, Hibernate `ddl-auto` con `schema-constraints.sql` idempotente (separador `^^^`) y Problem Details.
- C-08 expone `ConfiguracionServicio.nombreConsultorio()` leyendo `consultorio.nombre` de la tabla `configuracion`, sin caché.
- `spring-boot-starter-mail` ya está en `backend/pom.xml`; `.env.example` ya declara `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD` y `SMTP_FROM`.
- Los módulos referencian a otros por id (`Long`), no con `@ManyToOne` cruzado, y la capa `domain` no importa Spring/JPA (verificado por `ArquitecturaTest`).
- `turno` y `oferta_hueco` todavía no existen: los crean C-19 y C-36.

## Goals / Non-Goals

**Goals:** Un contrato de envío estable para los consumidores futuros; persistencia idempotente con garantía de PostgreSQL; reintentos deterministas con `Clock`; un planificador seguro ante ejecuciones concurrentes; configuración SMTP solo por entorno.

**Non-Goals:** Definir qué evento dispara cada tipo de email ni su copy final (lo hacen C-25/C-36/C-37/C-41/C-44); enviar WhatsApp; agregar una cola externa, broker o tabla de outbox separada; exponer endpoints HTTP; crear FKs hacia tablas que aún no existen.

## Decisions

### 1. Ubicación: `shared.notificacion`, no un feature `notificaciones`

Se crea `ar.edu.utn.turnos.shared.notificacion` con subpaquetes `domain`, `application` e `infrastructure`. Es la desviación deliberada respecto de la KB 08 (que dibuja un módulo `notificaciones`): el puerto es un **contrato transversal** que consumirán `agenda` (C-25), `reservapublica`/`listaespera` (C-36, C-37, C-41) y C-44. Ubicarlo en un feature hoja obligaría a esos módulos a depender de un módulo de notificaciones y a `notificaciones` a conocer sus tipos, generando un ciclo. `shared` ya aloja contratos transversales (`tiempo`, `error`, `ratelimit`).

El paquete queda así:

- `domain`: `NotificacionPort`, `MensajeNotificacion`, `EstadoNotificacion`, `TipoNotificacion`, `NotificacionEnviada`, `NotificacionRepositorio`, `ErrorEnvioNotificacion`.
- `application`: `ServicioNotificaciones` (registro + envío + reintento), `PlanificadorNotificaciones` (`@Scheduled`), `PlantillaNotificacion`.
- `infrastructure`: `NotificacionEnviadaJpa`, `NotificacionEnviadaJpaRepository`, `NotificacionRepositorioAdapter`, `SmtpNotificacionAdapter`, `InMemoryNotificacionAdapter`, `PropiedadesNotificaciones` (`@ConfigurationProperties("app.notificaciones")`), `ConfiguracionNotificaciones` (`@EnableScheduling` + beans condicionales).

Alternativa descartada: `notificaciones.application` como en la KB 08; se documenta la desviación en `verify.md` y en la KB si el equipo la aprueba.

### 2. Puerto y adaptadores

```java
public interface NotificacionPort {
    void enviar(MensajeNotificacion mensaje) throws ErrorEnvioNotificacion;
}

public record MensajeNotificacion(String destinatario, String asunto, String cuerpo) {}
```

- `SmtpNotificacionAdapter` usa `JavaMailSender`; se activa con `app.notificaciones.canal=smtp` (por defecto). No loguea cuerpo ni credenciales; registra solo `tipo`, `id` de notificación y destinatario enmascarado si hace falta.
- `InMemoryNotificacionAdapter` se activa con `app.notificaciones.canal=memoria` (perfil `test`). Guarda los mensajes enviados en una lista concurrente y permite forzar fallos (`fallarProximos(n)`) y contar intentos, para los tests de reintento e idempotencia.
- La selección se hace con `@ConditionalOnProperty`, no con `@Profile`, para que un consumidor pueda cambiar de canal sin recompilar.

### 3. Modelo de estados y transiciones

`EstadoNotificacion` es `PENDIENTE | ENVIADA | FALLIDA` (exactamente los tres del modelo de datos). Se persiste con `@Enumerated(EnumType.STRING)`.

```
PENDIENTE --envío ok--> ENVIADA (terminal)
PENDIENTE --fallo-----> FALLIDA (intentos=1, programada_para=ahora+espera(1))
FALLIDA   --envío ok--> ENVIADA (terminal)
FALLIDA   --fallo-----> FALLIDA (intentos++, si intentos<3 reprograma; si intentos=3 queda terminal)
```

Reglas concretas:

- `intentos` cuenta intentos reales, incluido el primero. Éxito y fallo lo incrementan.
- Espera creciente configurable: `espera-inicial` (5 min) por `multiplicador` (2) elevado a `intentos-1`. Tras el intento 1 espera 5 min; tras el 2, 10 min; el intento 3 es el último.
- `enviada_en` se setea solo en `ENVIADA`; `ultimo_error` se limpia al pasar a `ENVIADA`.
- No hay estado intermedio `EN_PROCESO` para no ampliar el modelo de la KB; la exclusión mutua la da el bloqueo de fila (decisión 6).
- `NotificacionEnviada` es un record de dominio inmutable con métodos `conIntentoFallido(...)`, `conEnvioExitoso(...)`, `vencida(Instant)`; los setters no existen. El mapeo a JPA vive en `NotificacionEnviadaJpa` (mismo patrón que `PacienteJpa`).

### 4. Idempotencia con las dos restricciones únicas

- `UNIQUE (turno_id, tipo, inicio_referencia)` para avisos de turno: impide duplicar un recordatorio y permite uno nuevo cuando el turno se reprograma (`inicio_referencia` = `inicio` del turno al momento del aviso).
- `UNIQUE (oferta_hueco_id, tipo)` para `OFERTA_HUECO`: PostgreSQL considera distintos los `NULL`, así que las filas de turno (con `oferta_hueco_id NULL`) no colisionan con las de oferta, y las de oferta (con `turno_id NULL`) no colisionan con las de turno. Es el comportamiento buscado, no un defecto.
- Para `OFERTA_HUECO`, `inicio_referencia` guarda el inicio del hueco (sirve para cortar reintentos, RN-38) y `turno_id` queda nulo.
- El registro se hace **antes** de enviar, en una transacción `REQUIRES_NEW` que captura `DataIntegrityViolationException`: si la fila ya existe, devuelve vacío y no se envía. `REQUIRES_NEW` evita envenenar la transacción externa con un rollback-only. Esto convierte el envío en *at-most-once* por fila (a nivel de registro) con entrega *at-least-once* ante una caída entre envío y commit, trade-off aceptado en el MVP.

### 5. Dónde viven las restricciones

- **Fuente de verdad en la entidad**: `@Table(name="notificacion_enviada", uniqueConstraints = {...}, indexes = @Index(name="ix_notificacion_estado_programada", columnList="estado, programada_para"))`. Así `create-drop` de tests y una base nueva de dev las obtienen solas.
- **Convergencia en dev con `ddl-auto=update`**: Hibernate `update` no agrega uniques a una tabla ya creada. Se agregan bloques `DO $$ ... $$` idempotentes en `schema-constraints.sql` que crean cada unique y el índice solo si no existen. Se anexan al final del archivo, cada uno separado por `^^^`, sin tocar los bloques de C-07/C-10/C-11/C-12.
- **Sin FKs en C-15**: `turno_id` y `oferta_hueco_id` apuntan a tablas que aún no existen (C-19/C-36). Agregar `FOREIGN KEY ... REFERENCES turno` rompería el arranque hoy. Se dejan columnas `bigint` nullable; la FK se agregará en el change que cree cada tabla. `paciente_id` se deja también sin FK para mantener una sola política y no bloquear el seed de tests.
- Índice `(estado, programada_para)` para la consulta del planificador (KB 04).

### 6. Planificador y concurrencia

- `PlanificadorNotificaciones` corre con `@Scheduled(fixedDelayString = "${app.notificaciones.planificador.intervalo:5m}")` (Flujo 5: cada 5 minutos) y delega en `ServicioNotificaciones.procesarVencidas()`.
- La selección usa una consulta nativa con `FOR UPDATE SKIP LOCKED` sobre `estado IN ('PENDIENTE','FALLIDA') AND programada_para <= :ahora AND (estado='PENDIENTE' OR intentos < 3)`, dentro de una transacción. La segunda ejecución concurrente saltea las filas bloqueadas y, al commitear la primera, ya no las ve como pendientes. Esto satisface «dos ejecuciones concurrentes no duplican un envío» sin agregar un cuarto estado.
- El envío SMTP ocurre dentro de esa transacción. A la escala del MVP (2–5 profesionales) el lock dura milisegundos; se documenta como trade-off frente a un outbox con estado de reclamo.
- El planificador se desactiva en tests con `app.notificaciones.planificador.habilitado=false` (`@ConditionalOnProperty`), y los tests invocan `procesarVencidas()` de forma explícita con un `Clock` fijo. `@EnableScheduling` se declara en `ConfiguracionNotificaciones`, no en `TurnosApplication`, para no tocar la clase raíz.
- Un aviso `OFERTA_HUECO` se descarta del reintento cuando `inicio_referencia <= ahora` (RN-38).

### 7. Plantilla y datos

- `PlantillaNotificacion` arma asunto y cuerpo en texto plano (sin Thymeleaf: no se agrega dependencia) a partir de un `DatosNotificacion` que aporta el consumidor: tipo, nombre del paciente destinatario, fecha/hora del turno, prestación, profesional y enlace. El nombre del consultorio se lee de `ConfiguracionServicio.nombreConsultorio()` en cada armado (sin caché), y la fecha se formatea en `ZonaHoraria.BUENOS_AIRES`.
- La plantilla recibe datos de **un solo** destinatario; no consulta otras fichas. Un test arma el email del paciente A y verifica que el cuerpo no contiene el nombre ni el email del paciente B (RN-35).
- El puerto no recibe entidades: `MensajeNotificacion` es un value object plano.

### 8. Configuración de correo por entorno

`application.yml` completa solo el bloque reservado:

```yaml
spring:
  mail:
    host: ${SMTP_HOST:}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USER:}
    password: ${SMTP_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
app:
  notificaciones:
    canal: ${NOTIFICACIONES_CANAL:smtp}
    remitente: ${SMTP_FROM:turnos@consultorio.test}
    planificador:
      habilitado: true
      intervalo: 5m
    reintento:
      maximo-intentos: 3
      espera-inicial: 5m
      multiplicador: 2
```

- `PropiedadesNotificaciones` es un `record` con `@ConfigurationProperties` + `@Validated`; `remitente` con `@NotBlank` y `maximo-intentos` acotado a `1..3`.
- `SmtpNotificacionAdapter` valida al construirse que `spring.mail.host`, `username` y `password` no estén en blanco cuando el canal es `smtp`; lanza `IllegalStateException` con un mensaje que nombra la variable faltante y **nunca** imprime el valor. Es el fail-fast del arranque.
- `application-test.yml` fija `app.notificaciones.canal: memoria` y `app.notificaciones.planificador.habilitado: false`, de modo que la suite no abra SMTP ni dispare tareas programadas.
- `.env.example` ya trae las variables; se agrega `NOTIFICACIONES_CANAL=smtp` como documentación del interruptor. `.env` sigue ignorado por git.

### 9. Ajustes de la revisión del PR (Juan Pablo, 2026-11-25)

- **Envío inmediato y planificador no se pisan.** `registrarYEnviar` guarda la fila `PENDIENTE` con `programada_para = ahora + espera inicial` (5 min), no `ahora`. Antes, el planificador podía tomar la fila mientras el envío inmediato seguía en curso y el paciente recibía dos emails. La fila sigue sirviendo de red de seguridad: si el proceso cae antes de guardar el resultado, el planificador la retoma después de esa espera.
- **Los reintentos conservan el contenido.** La fila no guarda el cuerpo, porque puede llevar un enlace con token (RN-07, regla dura 3). Cada consumidor implementa `ReconstructorDatosNotificacion` y devuelve los datos de sus notificaciones; el servicio los pide al reintentar. Sin consumidor que responda, se envía la versión mínima (destinatario y fecha). **C-25, C-36, C-37, C-41 y C-44 tienen que registrar su reconstructor.**
- **FK a `turno`.** `turno` ya existe (C-19), pero la FK sigue fuera: el registro se inserta en `REQUIRES_NEW` y no ve un turno que la transacción del consumidor todavía no confirmó.
- **Consumidores:** conviene llamar a `registrarYEnviar` después del commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`), para no avisar de un turno que termina en rollback.

## Risks / Trade-offs

- **Envío dentro de la transacción con lock de fila** → A la escala del MVP es aceptable; si el SMTP demora, el lock se extiende. Mitigación futura: outbox con estado de reclamo, fuera de C-15.
- **At-least-once ante caída entre envío y commit** → Puede repetirse un email, nunca duplicarse la fila; el registro idempotente evita el doble envío por ejecución del planificador, no por crash. Aceptado y documentado.
- **`ddl-auto=update` no agrega uniques a tablas existentes** → Se cubre con los bloques idempotentes de `schema-constraints.sql` y un test de integración que verifica la violación de la restricción en PostgreSQL real.
- **Archivo compartido `schema-constraints.sql`** → Se anexa al final con separador `^^^`; un conflicto de merge es textual y acotado a los últimos bloques. Avisar al equipo en el PR.
- **`spring.mail.host` vacío crea igual el `JavaMailSender`** → El validador del adaptador falla al arrancar con un mensaje claro; en tests el adaptador SMTP ni se instancia (`canal=memoria`).
- **Cambio de ubicación respecto de la KB 08 (`shared.notificacion` vs `notificaciones`)** → Se justifica por dependencias; si el equipo prefiere el nombre de la KB, el paquete puede renombrarse sin cambiar el contrato, pero conviene decidirlo en la revisión HIGH.

## Migration Plan

No hay migración de datos: la tabla es nueva. En dev, Hibernate la crea y los bloques de `schema-constraints.sql` agregan uniques e índice si faltan. En tests, `create-drop` la crea desde `@Table`. Rollback: quitar los beans y la tabla no afecta a otros módulos porque nadie la consume todavía; los consumidores llegan en C-25/C-36/C-37/C-41/C-44.

## Open Questions

- Nombre definitivo del paquete (`shared.notificacion` propuesto vs `notificaciones` de la KB 08) para aprobar en la revisión HIGH.
- Si el equipo quiere FKs hacia `turno`/`oferta_hueco`, se agregan en los changes que crean esas tablas, no acá.

## UI Mockups

- Decision: no (no UI)
- Este change es backend puro: no crea ni modifica pantallas, componentes ni estilos. El proyecto está en modo `system-only`, así que no hay gate de mockups.

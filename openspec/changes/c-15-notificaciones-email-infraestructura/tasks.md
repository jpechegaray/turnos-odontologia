# Tasks

> Ciclo TDD estricto (RED→GREEN→TRIANGULATE→REFACTOR): cada tarea de test se escribe antes de la implementación y lleva `@DisplayName` con el nombre exacto del escenario de la spec. Governance HIGH: no empezar hasta que el equipo apruebe proposal, design y specs.

## 1. Dominio y puerto (sin Spring ni JPA)

- [x] 1.1 Crear en `shared/notificacion/domain` los enums `EstadoNotificacion` (`PENDIENTE|ENVIADA|FALLIDA`) y `TipoNotificacion` (`RECORDATORIO|CONFIRMACION_RESERVA|CANCELACION|REPROGRAMACION|OFERTA_HUECO`), `MensajeNotificacion`, `ErrorEnvioNotificacion` y la interfaz `NotificacionPort`. Verificar con `ArquitecturaTest` que `..domain..` sigue sin importar Spring/JPA y con un test unitario de los valores de cada enum.
- [x] 1.2 Implementar el record inmutable `NotificacionEnviada` con sus transiciones (`conEnvioExitoso`, `conIntentoFallido`, `vencida`) y validar por TDD: "Un envío exitoso queda ENVIADA con la fecha de envío", "El próximo intento se calcula con el reloj inyectable" (espera creciente 5 min → 10 min, intentos 1..3) y "Una notificación fallida no se reintenta después del tercer intento".
- [x] 1.3 Documentar el contrato del puerto y de los estados en el Javadoc de `NotificacionPort` y `NotificacionEnviada` (qué recibe el consumidor, quién arma el mensaje y por qué no se envían entidades); verificar que `mvnw.cmd -q test -Dtest=NotificacionEnviadaTest` pasa.

## 2. Persistencia e idempotencia

- [x] 2.1 Implementar `NotificacionEnviadaJpa` (`@Table` con las dos `uniqueConstraints` y el índice `(estado, programada_para)`, `@Enumerated(STRING)`, `Instant` para todas las fechas), `NotificacionEnviadaJpaRepository` y `NotificacionRepositorioAdapter`, siguiendo el patrón de `PacienteJpa`/`PacienteRepositorioAdapter`. Verificar con `NotificacionRepositorioIT` que guarda y relee una fila con todos sus campos.
- [x] 2.2 Anexar al final de `schema-constraints.sql` los bloques `DO $$ ... $$` idempotentes (separados por `^^^`) que crean `uk_notificacion_turno_tipo_inicio`, `uk_notificacion_oferta_tipo` e `ix_notificacion_estado_programada` solo si faltan, sin tocar los bloques existentes. Verificar con un IT en PostgreSQL real que las tres existen en `pg_constraint`/`pg_indexes`.
- [x] 2.3 Cubrir la idempotencia con `NotificacionRepositorioIT`: "Dos inserciones iguales violan la restricción única" (`DataIntegrityViolationException`), "Un turno reprogramado genera un nuevo recordatorio" y "Los avisos de oferta no colisionan entre sí ni con los de turno" (dos ofertas distintas más un turno se guardan; repetir el mismo par oferta–tipo falla).

## 3. Adaptadores y configuración de correo

- [x] 3.1 Completar en `application.yml` solo el bloque `spring.mail` y `app.notificaciones` del diseño (`canal`, `remitente`, `planificador`, `reintento`), y en `application-test.yml` fijar `canal: memoria` y `planificador.habilitado: false`. Agregar `NOTIFICACIONES_CANAL=smtp` a `.env.example` sin credenciales reales; verificar que `application.yml` parsea y que `.env.example` solo tiene marcadores.
- [x] 3.2 Implementar `PropiedadesNotificaciones` (`@ConfigurationProperties` + `@Validated`, `remitente` `@NotBlank`, `maximo-intentos` acotado a 1..3) y `ConfiguracionNotificaciones` con `@EnableScheduling` y los beans condicionales por `canal`. Verificar con un test de contexto que el perfil `test` levanta el adaptador en memoria y no un `JavaMailSender`.
- [x] 3.3 Implementar `InMemoryNotificacionAdapter` (lista concurrente, contador de intentos, `fallarProximos(n)`) y `SmtpNotificacionAdapter` (envío con `JavaMailSender`, sin loguear cuerpo ni credenciales, validación fail-fast de host/usuario/password). Verificar "Las credenciales SMTP se toman solo del entorno", "El arranque falla si falta una credencial SMTP obligatoria" y "Las pruebas usan el adaptador en memoria" sin abrir SMTP.

## 4. Servicio de envío, plantilla y reintentos

- [x] 4.1 Implementar `PlantillaNotificacion` (texto plano, sin Thymeleaf) que use `ConfiguracionServicio.nombreConsultorio()` y formatee la fecha en `ZonaHoraria.BUENOS_AIRES`. Verificar "La notificación usa el nombre del consultorio y no expone otros pacientes": armar el email del paciente A y comprobar que no contiene nombre ni email del paciente B.
- [x] 4.2 Implementar `ServicioNotificaciones.registrarYEnviar(...)`: registro `PENDIENTE` en transacción `REQUIRES_NEW` que captura la violación única, omite destinatario nulo/blanco y delega en `NotificacionPort`. Verificar "No se reenvía un aviso que ya tiene registro" (el adaptador en memoria no recibe un segundo envío) y "Un paciente sin email no genera notificación ni error".
- [x] 4.3 Implementar el manejo de resultado del envío: éxito → `ENVIADA` con `enviada_en`; fallo → `FALLIDA` con `ultimo_error`, `intentos++` y `programada_para` según la espera creciente con el `Clock` inyectado. Verificar "Un fallo de SMTP deja la notificación FALLIDA y conserva el turno" y "Una notificación fallida se reintenta hasta tres veces con espera creciente" con el adaptador en memoria y un reloj fijo.

## 5. Planificador de pendientes y fallidas

- [x] 5.1 Implementar `PlanificadorNotificaciones` con `@Scheduled(fixedDelayString = "${app.notificaciones.planificador.intervalo:5m}")` condicionado a `planificador.habilitado`, y `ServicioNotificaciones.procesarVencidas()` con consulta nativa `FOR UPDATE SKIP LOCKED` sobre `PENDIENTE`/`FALLIDA` vencidas con `intentos < 3`. Verificar "El planificador envía las notificaciones pendientes vencidas" y "El planificador no envía notificaciones programadas a futuro".
- [x] 5.2 Verificar los bordes del planificador: "El planificador reintenta las fallidas cuyo próximo intento venció", "Dos ejecuciones concurrentes del planificador no duplican un envío" (dos hilos o dos invocaciones simultáneas contra PostgreSQL real) y "Un reinicio del servicio retoma las pendientes en el siguiente ciclo" (nuevo contexto contra la misma base).
- [x] 5.3 Implementar el corte de avisos de oferta: no reintentar `OFERTA_HUECO` cuando `inicio_referencia <= ahora`. Verificar "Los reintentos de un aviso de oferta cesan al comenzar el hueco" con el reloj fijo justo antes y después del inicio del hueco.

## 6. Verificación integrada

- [x] 6.1 Ejecutar `.\mvnw.cmd verify` desde `backend/` con Docker disponible: suite completa en verde, cada escenario de la delta spec con su `@DisplayName` exacto y `ArquitecturaTest` sin violaciones.
- [x] 6.2 Ejecutar `openspec validate "c-15-notificaciones-email-infraestructura" --strict` y revisar que ningún archivo versionado (`.env.example`, `application*.yml`, `schema-constraints.sql`, tests, docs) contenga credenciales SMTP reales; documentar el resultado en `verify.md`.

## Workflow follow-up

- Abrir el PR con la rama del responsable y pedir revisión de Juan Pablo (governance HIGH).
- Tras el merge, archivar el change con `openspec-archive-change` y actualizar la KB si se aprueba el nombre `shared.notificacion`.

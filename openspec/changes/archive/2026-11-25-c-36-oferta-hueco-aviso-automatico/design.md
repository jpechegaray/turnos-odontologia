# Design

## Context

- `CancelarTurnoService` (C-27) publica `TurnoCancelado` y `HuecoLiberado(profesionalId, sillonId, hueco, turnoOrigenId)` dentro de su transacción; el turno se lee con lock de fila, así que el evento sale una sola vez por cancelación. Su Javadoc pide que los oyentes de C-36 escuchen **después del commit**.
- `BuscadorCompatibles.aAvisar(Hueco)` (C-31) ya devuelve las entradas a avisar (RN-22 + SU-14: tope, antigüedad, un aviso por paciente, nada si el hueco empezó) y no chequea rol: está pensado para este oyente.
- `ServicioNotificaciones.registrarYEnviar(tipo, turnoId, ofertaHuecoId, pacienteId, datos)` (C-15) registra en `REQUIRES_NEW` con idempotencia por `UNIQUE (oferta_hueco_id, tipo)`, envía y nunca propaga el fallo. El planificador ya descarta `OFERTA_HUECO` cuyo `inicio_referencia` pasó. Para reintentar pide el contenido a los `ReconstructorDatosNotificacion`, porque la fila no guarda el cuerpo (puede llevar un token).
- `ServicioToken` (C-14) es dominio puro, sin bean de Spring; C-32 (rama de Nico) lo instancia con `new ServicioToken()` dentro de su servicio. Hacemos lo mismo para no chocar con un bean que otro change pueda declarar.
- `APP_PUBLIC_URL` está en `.env.example` pero ningún componente lo lee todavía; `application.yml` reserva un bloque `listaespera:` sin dueño.
- ArchUnit: `..domain..` no depende de Spring/JPA, y `shared.token` es puro.

## Goals / Non-Goals

**Goals:**
- Cancelar un turno crea ofertas solo para las entradas elegidas y envía un email por paciente, sin bloquear ni romper la cancelación.
- Idempotencia por hueco (una tanda), token hasheado, nada de tokens en logs.
- Reintentos de email que lleguen con un enlace válido.
- Vencimiento automático al inicio del hueco.
- Dejarle a C-42 una búsqueda por token lista para usar.

**Non-Goals:**
- Endpoint público y toma del hueco (C-42); asignación manual (C-43); frontend (C-47).
- Escuchar reprogramaciones: el oyente escucha `HuecoLiberado`, así que C-35 queda cubierto sin cambios cuando lo publique, pero el extremo a extremo se prueba en C-51.
- Ampliar el hueco a los minutos libres contiguos: el hueco es exactamente el intervalo del turno liberado (SU-14 c).

## Decisions

### D1. Paquete `listaespera.oferta`
`domain` (`OfertaHueco`, `EstadoOferta`, puro), `application` (`AvisoHuecoLiberado`, `OfertasHueco`, `ReconstructorAvisoOferta`, `PlanificadorVencimientoOfertas`) e `infrastructure` (`OfertaHuecoJpa`, repositorio). La lista de espera depende de `agenda.domain.evento.HuecoLiberado`, que es el contrato publicado por agenda; la traducción a `listaespera.domain.Hueco` se hace en el oyente.

### D2. Oyente después del commit y asincrónico
`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("avisosHuecoExecutor")`. Después del commit no hay transacción usable, así que el trabajo corre en transacciones propias.
- **Por qué asincrónico:** con SMTP real, hasta 10 envíos (tope por defecto) harían esperar varios segundos la respuesta de la cancelación. Además, cualquier excepción queda aislada del pedido HTTP.
- **Pruebas:** con la propiedad `app.listaespera.avisos.sincronico=true` (perfil `test`), el ejecutor es un `SyncTaskExecutor`; los IT siguen siendo deterministas sin Awaitility.
- **`@EnableAsync`** se declara en `ConfiguracionAvisosHueco` (el mismo patrón que `@EnableScheduling` en la lista de espera y en notificaciones).
- *Alternativa descartada:* sincrónico después del commit (más simple, pero la cancelación espera al SMTP). *Otra descartada:* dejar los avisos `PENDIENTE` para el planificador (hasta 5 min de demora en un mecanismo de "el primero que confirma gana").
- **A revisar por Valentino:** si preferís sincrónico por simplicidad, el cambio es quitar `@Async` y el ejecutor.

### D3. Una transacción por oferta e idempotencia por constraint
Por cada entrada elegida: en una transacción `REQUIRES_NEW` se genera el token, se inserta la oferta y se hace flush; si salta la violación de `uk_oferta_hueco_entrada_hueco` (`lista_espera_id, profesional_id, hueco_inicio`), se omite esa entrada (ya fue avisada por este hueco: una sola tanda). Después del commit de la oferta se llama a `registrarYEnviar(OFERTA_HUECO, null, ofertaId, pacienteId, datos)` con `inicio = hueco_inicio`. Así una oferta guardada siempre tiene su intento de aviso, y un fallo en una entrada no afecta a las demás. La garantía final de "una tanda" es la constraint de la base (regla dura 5, mismo espíritu).

### D4. Datos del email
`PacienteConsulta.porIds` (email y nombre, en una consulta), `ConsultaPrestaciones.buscarPorId` (nombre) y `ProfesionalConsulta` (nombre). Si el paciente no tiene email (RN-21 lo exige al anotarse, pero la ficha puede editarse), se omite la entrada **antes** de crear la oferta. Enlace: `app.listaespera.url-publica` (`${APP_PUBLIC_URL:http://localhost:5173}`) + `/oferta/` + token, sin barra doble.

### D5. Reintento con rotación de token
`ReconstructorAvisoOferta implements ReconstructorDatosNotificacion` responde solo a `OFERTA_HUECO` con `ofertaHuecoId`. Corre dentro de la transacción del planificador de notificaciones (por eso vive en el reconstructor y no en `OfertasHueco`: evita el ciclo de beans `OfertasHueco → ServicioNotificaciones → reconstructores`): bloquea la oferta, y si está `ENVIADA` y vigente genera un token nuevo, reemplaza `token_hash` y devuelve los datos con el enlace nuevo. El primer envío falló, así que el enlace anterior no llegó; invalidarlo no le quita nada al paciente.
- **Oferta no `ENVIADA` o paciente sin email:** el reconstructor devuelve vacío y declara `descartarSiFaltanDatos` para `OFERTA_HUECO`, el gancho que agregó C-37. `ServicioNotificaciones` deja entonces la fila terminal (`descartada`) sin enviar, en vez de caer a la versión mínima sin enlace. *Actualizado al integrar con master:* el primer borrador cambiaba `ServicioNotificaciones` (C-15); ya no hace falta.
- Caso límite aceptado: si el SMTP reportó fallo pero el email sí salió, el paciente recibe dos emails y solo el segundo enlace sirve.

### D6. Vencimiento
`PlanificadorVencimientoOfertas` con `@Scheduled(fixedDelayString = "${app.listaespera.ofertas.vencimiento.intervalo:1m}")`, deshabilitable en `test` con `app.listaespera.ofertas.vencimiento.habilitado=false` (los IT llaman al servicio con reloj fijo). Un `UPDATE ... SET estado='VENCIDA' WHERE estado='ENVIADA' AND expira_en <= :ahora` (JPQL), idempotente. La búsqueda por token también chequea `expira_en > ahora`, así que una oferta es inválida desde el inicio del hueco aunque el planificador todavía no haya corrido.

### D7. Búsqueda por token
`OfertasHueco.buscarVigentePorToken(String claro)`: valida formato (`[A-Za-z0-9_-]{43}`, como C-32), hashea y busca por `token_hash`; devuelve `Optional.empty()` para cualquier caso inválido. La comparación por igualdad de hash en la base es aceptable porque el atacante no controla el hash; `ServicioToken.coincide` se usa como doble chequeo en tiempo constante sobre la fila encontrada.

### D8. Esquema
Hibernate genera `oferta_hueco` con `@UniqueConstraint` (`uk_oferta_hueco_entrada_hueco`, `uk_oferta_hueco_token`) y el índice `(estado, expira_en)` vía `@Index`. El `CHECK (hueco_fin > hueco_inicio)` lo genera Hibernate con `@Check` (mismo patrón que `lista_espera`). Columnas `timestamptz` (`Instant`). Sin FK hacia `lista_espera` desde Hibernate si cruza módulos; se agrega la FK `fk_oferta_hueco_lista_espera` en el script, como hicieron los otros módulos.

### D9. Logs
Solo ids (oferta, entrada, profesional) y cantidades. `TokenGenerado.toString()` ya oculta valores; además, el enlace nunca se loguea. Un IT captura la salida (`OutputCaptureExtension`) y verifica que ningún token enviado aparece.

## Risks / Trade-offs

- **Asincronía** → los errores del aviso solo se ven en logs y en el registro de notificaciones. Mitigación: log de error con ids; el registro de C-25 muestra los fallos.
- **Rotación de token** → un falso fallo de SMTP deja un enlace muerto en la bandeja. Aceptado y documentado.
- **C-42 y C-43 dependen de este contrato** (estados, búsqueda por token, sillón sugerido). Mitigación: requisitos explícitos en la spec `oferta-hueco`.

## Open Questions

- ¿Asincrónico (D2) o sincrónico? Recomendado: asincrónico.
- Intervalo del planificador de vencimiento: propuesto 1 minuto (la búsqueda por token ya es exacta, el planificador solo limpia estados).

# Tasks

> TDD estricto: cada tarea arranca con el test en rojo. Cada test lleva `@DisplayName` con el nombre exacto del escenario de `specs/oferta-hueco/spec.md`. IT con Testcontainers (PostgreSQL real) e `InMemoryNotificacionAdapter`.

## 1. Dominio de la oferta

- [x] 1.1 `EstadoOferta` (`ENVIADA`, `TOMADA`, `VENCIDA`) y `OfertaHueco` puro en `listaespera.oferta.domain`: creación (`ENVIADA`, `expira_en = hueco_inicio`, valida `hueco_fin > hueco_inicio`), `rotarToken(hash)`, `vencer(ahora)` (solo si `ENVIADA` y `expira_en <= ahora`), `vigente(ahora)`. Test unitario: "La oferta nueva queda enviada y vence al inicio del hueco".
- [x] 1.2 Confirmar que `ArquitecturaTest` sigue en verde con el paquete nuevo.

## 2. Persistencia

- [x] 2.1 `OfertaHuecoJpa` + repositorio en `listaespera.oferta.infrastructure`: tabla `oferta_hueco`, `timestamptz`, `uk_oferta_hueco_entrada_hueco`, `uk_oferta_hueco_token`, índice `(estado, expira_en)`.
- [x] 2.2 `CHECK (hueco_fin > hueco_inicio)` con `@Check` de Hibernate (como `lista_espera`) y FK `fk_oferta_hueco_lista_espera` y `fk_oferta_hueco_profesional` en `schema-constraints.sql` (idempotente).
- [x] 2.3 Consultas: buscar por `token_hash`, bloquear por id (`PESSIMISTIC_WRITE`), vencer ofertas `ENVIADA` con `expira_en <= :ahora` (UPDATE JPQL).
- [x] 2.4 `OfertaHuecoTablaIT`: "La base rechaza una segunda oferta para la misma entrada y el mismo hueco" y "La base rechaza un hueco vacío o invertido".

## 3. Creación de ofertas y aviso

- [x] 3.1 `ConfiguracionAvisosHueco`: `@EnableAsync`, ejecutor `avisosHuecoExecutor` (`ThreadPoolTaskExecutor` chico o `SyncTaskExecutor` si `app.listaespera.avisos.sincronico=true`), propiedad `app.listaespera.url-publica` desde `APP_PUBLIC_URL`. Agregar el bloque `listaespera` a `application.yml` y el modo sincrónico + planificador apagado a `application-test.yml`.
- [x] 3.2 `OfertasHueco.crearYAvisar(HuecoLiberado)`: traduce a `Hueco`, pide `BuscadorCompatibles.aAvisar`, carga pacientes (`porIds`), prestaciones y profesional; omite pacientes sin email; por entrada, en `REQUIRES_NEW`, genera token (`new ServicioToken()`), inserta y hace flush; ante la violación de la unique omite la entrada; luego `registrarYEnviar(OFERTA_HUECO, null, ofertaId, pacienteId, datos)` con el enlace `url-publica/oferta/{token}` y `inicio = hueco_inicio`. Logs solo con ids.
- [x] 3.3 `AvisoHuecoLiberado`: `@Async("avisosHuecoExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)` que llama a 3.2 y captura y loguea (sin token) cualquier excepción.
- [x] 3.4 `AvisoHuecoLiberadoIT` (cancelación real por `CancelarTurnoService` o por la API): "Tras cancelar solo reciben aviso las entradas compatibles", "El tope de avisos elige a las entradas más antiguas", "Un paciente con dos entradas compatibles recibe un solo aviso", "Sin entradas compatibles no se avisa a nadie", "Una cancelación revertida no avisa", "Un fallo al avisar no hace fallar la cancelación".
- [x] 3.5 IT de idempotencia: "Liberar dos veces el mismo hueco no duplica ofertas".
- [x] 3.6 IT del email y del registro: "El email lleva el enlace con el token de la oferta", "Un paciente sin email no genera oferta", "El aviso queda registrado asociado a la oferta", "Solo se guarda el hash del token".
- [x] 3.7 IT con `OutputCaptureExtension`: "El token no aparece en los logs".

## 4. Reintento con rotación de token

- [x] 4.1 Que un aviso de oferta no vigente no se reenvíe sin enlace. Al integrar con master se resolvió con el gancho `descartarSiFaltanDatos` de C-37, sin tocar `ServicioNotificaciones`; lo cubre "No se reintenta una oferta que ya no está enviada".
- [x] 4.2 `ReconstructorAvisoOferta implements ReconstructorDatosNotificacion`: solo `OFERTA_HUECO` con `ofertaHuecoId`; bloquea la oferta; si está `ENVIADA` y vigente rota el token y devuelve los datos con el enlace nuevo; si no, devuelve datos sin destinatario.
- [x] 4.3 IT con `fallarProximos(1)` y reloj fijo: "El reintento envía un enlace nuevo y válido", "No se reintenta después de empezar el hueco", "No se reintenta una oferta que ya no está enviada".

## 5. Vencimiento y búsqueda por token

- [x] 5.1 `OfertasHueco.vencerOfertas()` y `PlanificadorVencimientoOfertas` (`@Scheduled`, intervalo `app.listaespera.ofertas.vencimiento.intervalo:1m`, `@ConditionalOnProperty` habilitado, apagado en `test`).
- [x] 5.2 IT con reloj fijo: "Una oferta vence al inicio del hueco", "Una oferta futura sigue vigente", "El vencimiento no toca ofertas tomadas y se puede repetir".
- [x] 5.3 `OfertasHueco.buscarVigentePorToken(String)`: formato de 43 caracteres base64url, hash, búsqueda, `ServicioToken.coincide`, `ENVIADA` y `expira_en > ahora`; cualquier otro caso devuelve vacío.
- [x] 5.4 IT: "Un token vigente encuentra su oferta" y "Tokens inválidos dan el mismo resultado".

## 6. Cierre

- [x] 6.1 `mvn verify` en verde (backend completo, incluido ArchUnit) y `npm test` del frontend sin regresiones.
- [x] 6.2 Prueba manual en dev (2026-11-25): backend real con canal memoria y datos ficticios propios (la base local ya tenía datos y el seed de C-33 se omitió). `POST /api/turnos/1/cancelar` respondió 200 en ~150 ms; el aviso corrió en el hilo `aviso-hueco-1` y creó 1 oferta `ENVIADA` (hash de 64, `expira_en` = inicio del hueco, sillón sugerido) y 1 notificación `OFERTA_HUECO` `ENVIADA` asociada a la oferta; sin enlaces en los logs. Datos de prueba borrados al terminar.
- [x] 6.3 `openspec-verify` → `verify.md` (LISTO). CHANGES.md se actualiza en el commit de archivado, después del merge, como hizo el equipo con C-28, C-31 y C-33.

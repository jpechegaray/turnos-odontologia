# Tasks

> TDD estricto: cada tarea arranca con el test en rojo. Cada test lleva `@DisplayName` con el nombre exacto del escenario de las specs del change. Los IT usan Testcontainers (PostgreSQL real) e `InMemoryNotificacionAdapter`. El reloj es el `Clock` fijo de los tests.

## 1. Alta online en la agenda (`agenda-turnos`)

- [x] 1.1 Agregar `Turno.nuevoOnline(...)` (canal `ONLINE`, `PROGRAMADO`, sin usuario creador), con test unitario de dominio. *(Al integrar `master` se usa la versión de C-41.)*
- [x] 1.2 Extraer de `CrearTurnoService` el paso común "bloquear, validar, guardar y publicar `TurnoCreado`" a un método package-private compartido, sin cambiar el comportamiento del alta del personal. Los IT de C-19 siguen en verde.
- [x] 1.3 Crear `CrearTurnoOnlineService.crear(pacienteId, profesionalId, prestacionId, inicio, sillonPreferidoId)`:
  - sin `@PreAuthorize`;
  - duración por defecto de la prestación;
  - candidatos: el preferido activo primero, después los activos por id;
  - elige el primer sillón sin solapes, bloquea el profesional y ese sillón, y valida con `ValidadorTurno`;
  - si no hay sillón libre, `solapado("SILLON")`.
- [x] 1.4 `CrearTurnoOnlineIT`: "Alta online en el sillón preferido", "Alta online sin sillón preferido libre", "Alta online sin ningún sillón libre" y "Alta online solapada con otro turno del profesional".

## 2. Persistencia de ofertas y entradas

- [x] 2.1 `ListaEsperaPersistencia.bloquear(id)` (`PESSIMISTIC_WRITE`).
- [x] 2.2 `OfertasHuecoPersistencia`:
  - `tomarOfertasDelHueco(profesionalId, huecoInicio, huecoFin)`: `UPDATE` de `ENVIADA` a `TOMADA`;
  - `vencerOferta(id)`: de `ENVIADA` a `VENCIDA`.
- [x] 2.3 `OfertasHueco.buscarPorToken(token)`: oferta en cualquier estado, con validación de formato y comparación de hash en tiempo constante.

## 3. Consulta pública de la oferta (`toma-oferta-hueco`)

- [x] 3.1 `TomaDeOferta.consultar(token)` y DTO `OfertaPublica`:
  - estado visible `DISPONIBLE` / `TOMADA` / `VENCIDA` / `NO_DISPONIBLE`;
  - fin = inicio del hueco + duración por defecto de la prestación;
  - sin ids ni datos del paciente;
  - `404 OFERTA_NO_ENCONTRADA` uniforme.
- [x] 3.2 Crear `OfertaPublicaController`, con `GET /api/public/ofertas/{token}`.
- [x] 3.3 `ConsultaOfertaPublicaIT`: "Se consulta una oferta disponible", "Una oferta tomada informa que el hueco ya fue tomado", "Una oferta cuyo hueco ya empezó informa que venció" y "Una oferta de una entrada que ya no está activa no está disponible".

## 4. Tomar el hueco (`toma-oferta-hueco`)

- [x] 4.1 `TomaDeOferta.tomar(token)` dentro de un `TransactionTemplate`:
  - bloquea oferta → entrada;
  - verifica el estado de la oferta, la vigencia y que la entrada esté `ACTIVA`;
  - crea el turno con `CrearTurnoOnlineService` usando el sillón sugerido;
  - asigna la entrada y pasa a `TOMADA` las ofertas del hueco;
  - emite el token del turno.
  - `DTO TurnoTomado`, con `toString` que oculta el token.
- [x] 4.2 Traducir los errores de la agenda (D4): `RN-01 PROFESIONAL` → `HUECO_YA_TOMADO`, `RN-13` → `OFERTA_VENCIDA`, el resto → `OFERTA_NO_DISPONIBLE`. Después del rollback, en `REQUIRES_NEW`, marcar `TOMADA` las ofertas del hueco o `VENCIDA` la oferta, según corresponda.
- [x] 4.3 Agregar `POST /api/public/ofertas/{token}/tomar`, con respuesta `201`.
- [x] 4.4 `TomaOfertaIT`: "El paciente toma un hueco disponible", "Si el sillón sugerido está ocupado se usa el primero activo libre", "Las demás ofertas del hueco pasan a tomadas", "Volver a tomar una oferta ya tomada", "El hueco lo ocupó recepción antes de que el paciente confirme", "Tomar una oferta cuyo hueco ya empezó", "Se puede tomar un hueco que empieza en pocos minutos", "La entrada fue cancelada antes de tomar la oferta" y "El profesional bloqueó la agenda en el horario del hueco".
- [x] 4.5 IT de carrera con dos hilos y un `CountDownLatch` contra el endpoint real: "Dos pacientes confirman a la vez y solo uno obtiene el turno". También el caso de una toma contra un alta simultánea de recepción, para comprobar que no hay deadlock.
- [x] 4.6 IT: "Tokens inválidos responden el mismo 404", con un token inexistente, uno mal formado y un token de turno, en `GET` y en `POST`.

## 5. Email de confirmación (`toma-oferta-hueco`, `notificaciones-email`)

- [x] 5.1 Después del commit, `registrarYEnviar(CONFIRMACION_RESERVA, turnoId, null, pacienteId, datos)` con el enlace `APP_PUBLIC_URL/turno/{token}`.
- [x] 5.2 Crear `DatosConfirmacionReserva implements ReconstructorDatosNotificacion` con `descartarSiFaltanDatos = true` para `CONFIRMACION_RESERVA`. Generalizar el mensaje de descarte de `procesarVencidas`. *(Al integrar `master` se usa `ReconstructorConfirmacionReserva` de C-41.)*
- [x] 5.3 IT: "El paciente recibe el enlace de su nuevo turno" (el enlace sirve en `GET /api/public/turnos/{token}`) y "Una falla de correo no pierde el turno".
- [x] 5.4 IT del planificador: "El reintento lleva un enlace válido" y "No se reintenta la confirmación de un turno cancelado".

## 6. Protección del token

- [x] 6.1 `RutaConTokenDeTurno`: reconocer `/api/public/ofertas/` además de `/api/public/turnos/`, con una referencia segura por prefijo.
- [x] 6.2 IT: "Las respuestas no se guardan en caché" (`no-store` en `200`, `201` y `404`, y sin el token en el cuerpo del error) y "Los tokens no aparecen en los logs" (con `OutputCaptureExtension`).

## 7. Cierre

- [x] 7.1 Correr completas las suites unitaria e IT del backend (`mvn verify`) y la del frontend (`npx vitest run`) en verde. `ArquitecturaTest` en verde.
- [x] 7.2 Prueba manual contra el backend real: cancelar un turno con lista de espera, tomar la oferta con `curl`, revisar el email en memoria y el turno en la agenda. Limpiar los datos de prueba al terminar.

## Workflow follow-up

- `openspec-verify` → `verify.md`.
- PR a `master` (lo revisa Juan Pablo), con la integración de `master` (C-41, C-44, C-48) ya resuelta.
- Archivo del change y actualización de CHANGES.md después del merge.

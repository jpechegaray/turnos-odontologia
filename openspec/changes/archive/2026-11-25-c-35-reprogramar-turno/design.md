# Design

## Context

- **Lo que ya existe:**
  - C-19 dejó en `ValidadorTurno` el parámetro `turnoExcluidoId` y el evento `TurnoReprogramado`;
  - `TurnoJpa` ya tiene la columna `reprogramado_tarde`;
  - C-27 dejó `PoliticaCancelacion`, el lock de fila (`buscarParaActualizar`), la traducción del `@Version` a `409 TURNO_MODIFICADO` y `HuecoLiberado`.
- **Lo que falta:** el caso de uso, el endpoint y cómo armar los huecos que deja una reprogramación.
- **Decisiones del líder (2026-11-25):**
  - PA-06: se permite y se registra;
  - se publica solo el hueco que quedó libre;
  - un turno ya empezado da `422 RN-05`.

## Goals / Non-Goals

**Goals:**
- Reprogramar el mismo turno con las validaciones del alta, excluyéndose a sí mismo.
- Marcar la tardía, publicar los eventos y quedar seguro ante concurrencia.

**Non-Goals:**
- Token, email, lista de espera y frontend.
- Cambiar la prestación o el paciente del turno.

## Decisions

### D1. `Intervalo.menos(otro)`

- Devuelve `List<Intervalo>` con los tramos de `this` que no se solapan con `otro`, en orden:
  - vacío si `otro` lo cubre;
  - `this` completo si no se tocan;
  - uno o dos tramos en el resto de los casos.
- Respeta `[inicio, fin)`: un intervalo contiguo no resta nada.
- Vive en `shared.tiempo`, junto a `solapaCon` y `contiene`, y es puro.

### D2. Dominio del turno

- **`Turno.exigirReprogramable(ahora)`:**
  - si el estado no admite reprogramarse (`!estado.ocupaLugar()` o estado final), lanza `409 TURNO_TRANSICION_INVALIDA`. Se reutiliza `TURNO_TRANSICION_INVALIDA` para un solo código de "estado final", como C-27 (D3), aunque el Flujo 2 de la KB dice `422`;
  - después, si `!ahora.isBefore(inicio)`, lanza `422 RN-05`.
- **`Turno.reprogramar(profesionalId, sillonId, intervalo, ahora, plazo)`:**
  - vuelve a llamar a `exigirReprogramable`;
  - calcula la tardía con `PoliticaCancelacion.esTardia(inicioOriginal, ahora, plazo)`;
  - devuelve una copia con los recursos, el intervalo, la duración y `reprogramadoTarde = this.reprogramadoTarde || tardia`;
  - conserva `id`, estado, notas, canal y `version`.
- **`Turno.mismoHorario(profesionalId, sillonId, intervalo)`** detecta el pedido sin cambios.

### D3. Eventos

- `TurnoReprogramado.de(anterior, nuevo)` copia los recursos y los intervalos, y la marca de tardía del nuevo.
- `HuecoLiberado.porReprogramacion(anterior, nuevo)` devuelve una lista:
  - con el mismo profesional, `anterior.intervalo().menos(nuevo.intervalo())`;
  - si no, `[anterior.intervalo()]`;
  - siempre con el profesional y el sillón **anteriores** y `turnoOrigenId = id`.
- Si el profesional es el mismo pero cambia el sillón, el hueco se calcula por profesional, porque el sillón es indistinto para la lista de espera (SU-14 c).
- Los eventos se publican dentro de la transacción, como en C-27; C-32, C-36 y C-44 escuchan con `AFTER_COMMIT`.

### D4. Servicio

`ReprogramarTurnoService.reprogramar(id, solicitud, usuarioId)` lleva `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")` y `@Transactional`. Pasos:

1. `buscarParaActualizar(id)` → si no, `404 TURNO_INEXISTENTE`. Toma el lock de la fila.
2. `turno.exigirReprogramable(ahora)` → `409` o `422`. Va antes de validar el horario, para que un turno cancelado no informe "horario ocupado".
3. Completar la solicitud: profesional, sillón y duración del turno si faltan. La prestación y el paciente son los del turno.
4. `bloquearRecursos(profesionalNuevo, sillonNuevo)`: advisory locks, como en el alta.
5. `validador.validar(solicitud, recursos, consultas, id)`.
6. Si `mismoHorario`, devuelve el turno sin guardar ni publicar.
7. `guardar(turno.reprogramar(...))`: el `EXCLUDE` y el `@Version` se traducen a `409`.
8. Publica `TurnoReprogramado` y cada `HuecoLiberado`.

**Orden de los locks:** siempre fila → profesional → sillón. El alta no toma locks de fila y la cancelación no toma advisory locks, así que no hay ciclos.

### D5. Recursos compartidos con el alta

- `CrearTurnoService.recursos(...)` se extrae a `ConsultaRecursosTurno`, un `@Component` en `agenda.application`, y la usan los dos servicios.
- Es un refactor sin cambio de comportamiento; lo cubre la suite de C-19.

### D6. Endpoint y DTOs

- `SolicitudReprogramarTurno(@NotNull OffsetDateTime inicio, Long profesionalId, Long sillonId, Integer duracionMin)`.
- Sin `notas`: reprogramar no cambia las notas.
- La duración inválida la rechaza `DuracionPrestacion` con `RN-09`, igual que en el alta.
- `RespuestaTurno` suma `boolean reprogramadoTarde`.
- `SecurityConfig` suma `POST /api/turnos/{id}/reprogramar` para `ADMIN` y `RECEPCION`, así el odontólogo recibe 403 antes de validar el cuerpo.

### D7. Tests

- **Unitarios:**
  - `IntervaloTest.menos`;
  - `TurnoTest`: exigir, reprogramar, tardía, marca pegajosa y `mismoHorario`;
  - `EventosReprogramacionTest`.
- **Integración:**
  - `TurnosReprogramacionApiIT`: todos los escenarios de API, eventos con `@RecordApplicationEvents` y acceso;
  - `TurnosReprogramacionConcurrenciaIT`: reprogramación y alta al mismo horario, `@RepeatedTest(5)`, sin `@Transactional`, con un oyente propio.
- **CSRF:** `csrfValido()` y `csrfConCabeceraIncorrecta()`, nunca `.with(csrf())`.

### D8. KB

PA-06 pasa a resuelta en `knowledge-base/10_preguntas_abiertas.md`. La nota de `CLAUDE.md` y `AGENTS.md` deja de mencionarla como abierta, y se verifica con `diff`.

### D9. Rama

`juan-pablo-c-35`, desde `master` después de C-31 y C-33.

## Risks / Trade-offs

- **[Riesgo] Mantener `CONFIRMADO` tras mover el turno.** La confirmación fue para el horario anterior. Se acepta porque RN-12 no define una vuelta a `PROGRAMADO`; C-44 avisa al paciente por email.
- **[Riesgo] Deadlock entre reprogramaciones.** Mitigación: el orden fijo de los locks (D4).
- **[Trade-off] Varios `HuecoLiberado` por reprogramación.** Se acepta: cada tramo es un hueco real, y C-36 los evalúa por separado.

## Migration Plan

No hay cambios de esquema.

## Open Questions

Ninguna. PA-06 quedó resuelta el 2026-11-25.

## UI Mockups

- Design system revision: no aplica
- Decision: no (no UI) (2026-11-25)

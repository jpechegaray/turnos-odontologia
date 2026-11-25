# Design

## Context

C-19 dejó el módulo `agenda` con el record `Turno` completo (incluidas las columnas de cancelación), la tabla de transiciones de RN-12, las restricciones `EXCLUDE` que ignoran los turnos `CANCELADO` y los eventos `TurnoCancelado` y `HuecoLiberado`, todavía sin publicar. `ConfiguracionServicio.plazoCancelacionHoras()` (C-08) devuelve el plazo de RN-05. C-27 agrega la operación de cancelar sobre esa base. Es MEDIUM: se implementa por etapas y se muestran las decisiones no obvias.

## Goals / Non-Goals

**Goals:** cancelar con las reglas RN-05, RN-06, RN-10, RN-12 y RN-19; bordes del plazo exactos; liberar el intervalo sin tocar las restricciones; publicar el hueco liberado una sola vez, incluso con pedidos simultáneos.

**Non-Goals:** aviso a la lista de espera (C-36), cancelación por token del paciente (C-32/C-39), email de cancelación (C-44), revocación del token (C-32), frontend (C-46).

## Decisions

### D1. `PoliticaCancelacion` pura con `Duration`

```java
public static boolean esTardia(Instant inicio, Instant ahora, Duration plazo) {
    return Duration.between(ahora, inicio).compareTo(plazo) < 0;
}
```

Se compara `Duration` contra `Duration`, no horas enteras: con horas enteras 23:59 se redondea y el borde de RN-05 falla. La marca se calcula al cancelar y queda guardada, así que un cambio posterior del plazo no altera cancelaciones anteriores.

**Alternativa descartada:** calcular la marca al consultar. Haría depender el pasado de la configuración actual.

### D2. `Turno.cancelar(...)` se apoya en `pasarA`

`cancelar(actor, usuarioId, motivo, ahora, plazo)` primero llama a `pasarA(CANCELADO)` y después verifica que el turno no empezó (`ahora < inicio`). El orden importa: un turno `ATENDIDO` en el pasado responde con la transición inválida (409), que es el error más informativo. A la hora exacta de inicio el turno ya empezó.

### D3. Códigos de error

| Caso | Respuesta |
|---|---|
| Turno inexistente | `404 TURNO_INEXISTENTE` (es el recurso de la ruta) |
| Estado final | `409 TURNO_TRANSICION_INVALIDA` (convención de C-19) |
| Turno iniciado | `422 RN-05` |
| Motivo de más de 255 caracteres | `400` con `errores[].campo = motivo` |
| Conflicto de versión | `409 TURNO_MODIFICADO` |

**Desvío aprobado de la KB:** el Flujo 2 dice "cancelado, atendido o ausente → `422`". Se usa `409`, porque C-19 ya fijó ese código para cualquier transición inválida y conviene que el cliente vea un único código.

### D4. `cancelado_por` y `cancelado_por_usuario_id`

- `ActorCancelacion { USUARIO, PACIENTE_TOKEN }`. `cancelado_por` guarda su nombre (cabe en `varchar(16)` y respeta la KB).
- Nueva columna `cancelado_por_usuario_id` (`bigint`, nula para `PACIENTE_TOKEN`), con FK a `usuario` en `schema-constraints.sql`, en un bloque idempotente.
- El record `Turno` gana el campo `canceladoPorUsuarioId`. El cambio toca `Turno.nuevo`, `pasarA`, `TurnoJpa.de` y `TurnoJpa.dominio`, y los tests que construyen `Turno`.

**Alternativa descartada:** `"USUARIO:12"` dentro de `cancelado_por`. Es frágil y no permite FK.

### D5. Concurrencia: lock de la fila

`CancelarTurnoService` lee el turno con `PESSIMISTIC_WRITE` (`SELECT … FOR UPDATE`). Una segunda cancelación simultánea espera, lee `CANCELADO` y recibe `409`. Así `HuecoLiberado` se publica una sola vez.

Además, `TurnosPersistencia.guardar` traduce `ObjectOptimisticLockingFailureException` a `409 TURNO_MODIFICADO`. Antes, `guardar` hacía un `merge` con la versión vieja y respondía `500`. Es la red de seguridad para C-29 y C-35.

**Ajuste durante el apply:** la traducción se hizo en `TurnosPersistencia` y no en `ManejadorErrores`, porque `turno` es la única entidad con `@Version` y un código `TURNO_*` no corresponde al manejador compartido. Es el mismo lugar donde C-19 traduce las violaciones de las `EXCLUDE`.

**Dos barreras, verificadas con mutaciones:** sin el lock de fila, la carrera sigue en verde porque el `@Version` responde `409 TURNO_MODIFICADO` al perdedor, que no llega a publicar eventos. Sin el lock y sin la traducción, la carrera falla 5 de 5.

**Para C-35:** reprogramar tiene que tomar este mismo lock de fila y, además, los advisory locks de C-19 antes de validar el nuevo intervalo.

### D6. Eventos y contrato para C-36

`TurnoCancelado.de(turno)` y `HuecoLiberado.de(turno)` se publican con `ApplicationEventPublisher` después de guardar, dentro de la transacción, igual que `TurnoCreado`. **C-36 debe escuchar con `@TransactionalEventListener(phase = AFTER_COMMIT)`** para no ofrecer un hueco de una cancelación que terminó en rollback.

### D7. Servicio, endpoint y seguridad

- `CancelarTurnoService.cancelarPorPersonal(id, motivo, usuarioId)` con `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")` y `@Transactional`.
- Un método interno `cancelar(id, ActorCancelacion, usuarioId, motivo)` deja preparado el actor `PACIENTE_TOKEN` para C-32/C-39. No se expone por HTTP en C-27.
- `POST /api/turnos/{id}/cancelar` con cuerpo opcional `{ "motivo": "..." }` (`@Size(max = 255)`, el largo de la columna). Responde `200` con `RespuestaTurno`, que suma `cancelacionTardia`, `canceladoEn`, `canceladoPor` y `motivoCancelacion`.
- `SecurityConfig` suma `requestMatchers(POST, "/api/turnos/{id}/cancelar").hasAnyRole("ADMIN", "RECEPCION")`, para responder `403` antes de validar el cuerpo.

### D8. Tests

- Unitarios: `PoliticaCancelacion` (bordes de RN-05 con plazos de 0, 24 y 48 horas) y `Turno.cancelar` (estados de origen, turno iniciado, campos).
- Integración con Testcontainers: los escenarios de la delta spec por HTTP, `@RecordApplicationEvents` para los avisos y `RelojFijoConfig` para "ahora".
- Concurrencia: dos hilos con `CountDownLatch` contra el mismo turno, `@RepeatedTest(5)`, como `TurnosConcurrenciaIT`.

### D9. Rama

`juan-pablo-c-27`, desde `master` con C-19 y C-15 ya mergeados. Un commit por etapa, con confirmación previa.

## Risks / Trade-offs

- **El lock de fila serializa las modificaciones del mismo turno.** Es lo buscado; afecta solo a pedidos sobre un mismo turno.
- **El campo nuevo de `Turno` toca tests de C-19.** Es un cambio mecánico; la suite completa lo detecta.
- **La respuesta todavía no incluye las entradas avisadas de la lista de espera (Flujo 3, paso 6).** La agrega C-36.

## Migration Plan

Sin migración de datos. En dev, Hibernate (`ddl-auto=update`) agrega la columna `cancelado_por_usuario_id` y el bloque idempotente agrega la FK. En tests, `create-drop`.

## Open Questions

Ninguna.

## UI Mockups

- Design system revision: no aplica
- Decision: no (no UI) (2026-11-25)

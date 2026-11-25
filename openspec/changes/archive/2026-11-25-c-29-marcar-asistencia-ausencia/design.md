# Design

## Context

C-19 dejó el record `Turno`, la tabla de transiciones de RN-12 (`EstadoTurno`) y `Turno.pasarA`. C-27 sumó `TurnosPersistencia.buscarParaActualizar` (lock de fila), la traducción del conflicto de versión a `409 TURNO_MODIFICADO` y el patrón `Turno.cancelar` / `CancelarTurnoService` / `POST /api/turnos/{id}/cancelar`. C-29 agrega el registro de asistencia siguiendo esa plantilla. Es MEDIUM: se implementa por etapas y se muestran las decisiones no obvias.

## Goals / Non-Goals

**Goals:** marcar `ATENDIDO` o `AUSENTE` desde el inicio del turno (RN-20); permitir que solo `ADMIN` corrija una marca; no abrir otras transiciones desde los estados finales (RN-12).

**Non-Goals:** auditoría de la corrección, eventos o emails por asistencia, política de ausencias reiteradas (PA-13), frontend (C-46).

## Decisions

### D1. Dos métodos en `Turno` y `EstadoTurno` sin cambios

```java
Turno marcarAsistencia(EstadoTurno destino, Instant ahora)  // primera marca
Turno corregirAsistencia(EstadoTurno destino)               // solo desde ATENDIDO/AUSENTE
```

- `marcarAsistencia`: exige destino `ATENDIDO` o `AUSENTE` (`IllegalArgumentException` si no: el DTO ya lo valida con `400`). Primero aplica `pasarA(destino)`, así un turno en estado final responde `409` aunque ya haya pasado; después rechaza con `422 RN-20` si `ahora < inicio`. A la hora exacta de inicio se acepta (`!ahora.isBefore(inicio)`), igual que el borde de `cancelar`.
- `corregirAsistencia`: exige que el estado actual sea `ATENDIDO` o `AUSENTE` y que el destino sea el **otro** de los dos; en cualquier otro caso `409 TURNO_TRANSICION_INVALIDA`. No mira el reloj.
- `EstadoTurno` no se toca: `esFinal()` y `puedePasarA()` siguen diciendo que `ATENDIDO` y `AUSENTE` son finales, así que cancelar y reprogramar (C-35) los rechazan sin cambios. La excepción de RN-12 vive solo en `corregirAsistencia`.
- No hay un booleano `esCorreccion` ni en el record ni en la base: el servicio lo deduce del estado actual, comparando explícitamente con `ATENDIDO` y `AUSENTE`.

**Ajuste durante el apply:** el borrador decía "deduce de `esFinal()`", pero `esFinal()` también es verdadero para `CANCELADO`, que así entraría por la rama de corrección y daría `403` a recepción en lugar de `409`. Por eso el servicio compara los dos estados de asistencia.

**Alternativas descartadas:** (a) abrir la tabla (`ATENDIDO → AUSENTE`), porque cambia la semántica de "final" para C-27 y C-35; (b) un único método con un flag `correccion`, por ser un booleano suelto que mezcla dos reglas distintas.

### D2. Servicio y permisos

`MarcarAsistenciaService.marcar(id, destino, rol)`:

1. `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")` y `@Transactional`.
2. Lee el turno con `buscarParaActualizar` (lock de fila); `404 TURNO_INEXISTENTE` si no existe.
3. Si el estado actual es una marca de asistencia (`ATENDIDO` o `AUSENTE`):
   - si el rol no es `ADMIN`, lanza `AccessDeniedException` (el manejador la traduce a `403`);
   - si no, llama a `corregirAsistencia`.
   En cualquier otro estado (incluido `CANCELADO`) llama a `marcarAsistencia`, que para `CANCELADO` responde `409` por `pasarA`.
4. Guarda con `TurnosPersistencia.guardar` (que ya traduce el conflicto de versión) y devuelve el turno.

El controlador pasa el `Rol` de `IdentidadPersonal`. Se usa el rol y no el `SecurityContext` dentro del servicio para que el servicio sea testeable sin sesión y no dependa de una estática.

**Orden de los chequeos:** existe (`404`) → `CANCELADO` (`409`) → corrección por no-ADMIN (`403`) → misma marca (`409`) → reloj (`422`). Un `CANCELADO` da `409` para todos los roles, y recepción recibe `403` sobre una marca final aunque pida el mismo estado.

**Por qué el lock de fila:** dos marcas simultáneas (recepción y admin) se serializan; la segunda ve el estado ya final y entra por la rama de corrección o recibe `409`, en lugar de pisarse.

### D3. Códigos de error

| Caso | Respuesta |
|---|---|
| Turno inexistente | `404 TURNO_INEXISTENTE` |
| Estado destino ausente o inválido | `400`, `errores[].campo = estado` |
| Turno `CANCELADO` | `409 TURNO_TRANSICION_INVALIDA` |
| Marca final y rol `RECEPCION` | `403` |
| Misma marca que la actual | `409 TURNO_TRANSICION_INVALIDA` |
| Turno que todavía no empezó | `422 RN-20` |
| Conflicto de versión | `409 TURNO_MODIFICADO` (ya existente, C-27) |

**Desvío de la KB:** el Flujo 2 dice "cancelado, atendido o ausente → `422`"; se usa `409` por coherencia con C-19 y C-27, igual que ya se aprobó en C-27.

### D4. Endpoint y seguridad

- `POST /api/turnos/{id}/asistencia` con `SolicitudMarcarAsistencia(@NotNull EstadoTurno estado)`; la restricción a `ATENDIDO`/`AUSENTE` se valida en el DTO con una constraint propia `@MarcaDeAsistencia` sobre el componente, para responder `400` con el campo `estado` (el nulo lo reporta solo `@NotNull`). **Ajuste durante el apply:** el borrador proponía `@AssertTrue`, pero en un record reporta el nombre del método (por ejemplo `estadoValido`) y no `estado`; se siguió el precedente de `ClaveValida`. Responde `200` con `RespuestaTurno`, sin cambios.
- `SecurityConfig` suma `requestMatchers(POST, "/api/turnos/{id}/asistencia").hasAnyRole("ADMIN", "RECEPCION")`, para responder `403` al odontólogo antes de validar el cuerpo.
- La distinción entre `RECEPCION` y `ADMIN` para la corrección no puede ir en el matcher (depende del estado del turno), por eso la decide el servicio.

### D5. Tests

- Unitarios (JUnit, sin Spring): `Turno.marcarAsistencia` (estados de origen, borde del inicio con `Clock` fijo, turno que no empezó, `CANCELADO`) y `Turno.corregirAsistencia` (ATENDIDO↔AUSENTE, misma marca, estados no finales). Un test que cancela un turno corregido y espera `409`.
- Integración con Testcontainers: los escenarios de la delta spec por HTTP con `RelojFijoConfig`, y un test que llama al servicio como odontólogo y espera `AccessDeniedException`.

### D6. Rama

Una rama propia desde `master`, un commit por etapa con confirmación previa, y un PR por change (regla 14 y 16).

## Risks / Trade-offs

- **La corrección no deja rastro** (quién y cuándo): `Turno` no tiene columnas para eso. Si el equipo lo pide, es un change aparte que agregue columnas y auditoría.
- **La excepción a RN-12 vive en un solo método.** Si en el futuro se agrega otra transición de corrección, conviene reevaluar si la tabla debe modelarla.
- **`ADMIN` puede corregir sin límite de tiempo**, porque la KB no fija uno.

## Migration Plan

Sin migración de datos ni de esquema.

## Open Questions

Ninguna.

## UI Mockups

- Design system revision: no aplica
- Decision: no (no UI) (2026-11-25)

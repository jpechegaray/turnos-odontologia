# Proposal

## Why

El sistema todavía no puede dar turnos. Garantizar que nunca haya dos turnos superpuestos, ni del mismo profesional ni en el mismo sillón, es el núcleo del producto (RN-01). Además, C-19 está en el camino crítico: 14 changes de agenda, reserva online y lista de espera dependen de él. Sus dependencias (C-03, C-04, C-05, C-07, C-10, C-11 y C-12) ya están en `master`.

## What Changes

- Agregar `POST /api/turnos` para que `RECEPCION` y `ADMIN` creen un turno con:
  - paciente, profesional, sillón y prestación, todos obligatorios;
  - el `inicio`, como instante con zona;
  - una duración opcional en minutos: si no se envía, se usa la duración por defecto de la prestación (RN-03);
  - notas opcionales.
- El turno nuevo queda `PROGRAMADO`, con canal `INTERNO`, el usuario que lo creó y su fecha de creación. Su `fin` es `inicio + duración` (RN-09).
- Antes de guardar se validan, en este orden:
  1. Recursos existentes y activos, y que el profesional realice la prestación (RN-11) → `422`.
  2. Inicio posterior al momento actual (RN-13) → `422`.
  3. Inicio en minutos múltiplos de 5, y duración múltiplo de 5 entre 5 y 480 (RN-09) → `422`.
  4. Turno contenido completo en un único tramo del horario del profesional (RN-04, RN-16) → `422`.
  5. Turno fuera de los bloqueos del profesional (RN-04) → `422`.
  6. Sin solapamiento con turnos no cancelados del mismo profesional o del mismo sillón, con intervalos `[inicio, fin)` (RN-01, RN-02, RN-10) → `409`, indicando el recurso en conflicto.
- PostgreSQL garantiza RN-01 aunque dos pedidos lleguen a la vez (RN-15). Para eso se agregan dos restricciones `EXCLUDE USING gist`, una por profesional y otra por sillón, que ignoran los turnos `CANCELADO`. Si una creación las viola, la API responde el mismo `409` que la validación previa.
- La entidad `Turno` queda completa según el modelo de datos:
  - estados y tabla de transiciones de RN-12;
  - canal;
  - columnas de cancelación y reprogramación;
  - bloqueo optimista con `version`.

  Así, los changes de agenda posteriores solo agregan archivos.
- Se definen los eventos de dominio `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `HuecoLiberado`. C-19 publica solo `TurnoCreado`. Los demás los publicarán C-27 y C-35.
- `ODONTOLOGO` recibe `403` al intentar crear un turno (RN-20, RN-33).
- Quedan fuera:
  - los horarios libres sugeridos (C-26);
  - el email de confirmación: depende de C-15, que podrá escuchar `TurnoCreado`;
  - el token del turno (C-32);
  - cancelar, reprogramar y marcar asistencia (C-27, C-35 y C-29);
  - la consulta de la agenda (C-28);
  - el frontend (C-45).

## Capabilities

### New Capabilities

- `agenda-turnos`: creación de turnos del personal con las reglas de agenda y la garantía de no solapamiento por profesional y por sillón, incluso ante pedidos simultáneos. Los changes de cancelación, reprogramación, asistencia y consulta de la agenda ampliarán esta capacidad.

### Modified Capabilities

Ninguna.

## Impact

- Se agrega el paquete backend `agenda`, organizado en dominio, aplicación, infraestructura y web.
- Se crea la tabla `turno`. En `schema-constraints.sql` se suman sus FK, los `CHECK` y las dos restricciones de exclusión, de forma idempotente.
- `shared/tiempo/Intervalo` gana `solapaCon`, `contiene` y `desde`. Es un cambio compatible: los usos actuales de horarios y bloqueos no cambian.
- Se consumen sin modificarlos:
  - `PacienteConsulta`, `ProfesionalConsulta`, `SillonConsulta`, `ConsultaPrestaciones` y `ProfesionalRealizaPrestacion`;
  - `HorarioConsulta` y `BloqueoConsulta`;
  - `DuracionPrestacion`, para RN-09.
- Se suma el matcher de escritura de `POST /api/turnos` en `SecurityConfig`.
- Desbloquea C-26 a C-33, C-35, C-37 y C-41 a C-43, además de C-45.

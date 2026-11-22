# Proposal

## Why

Para dar turnos solo cuando el profesional atiende (RN-04), la agenda necesita conocer los horarios semanales de cada uno. C-06 ya provee el catálogo de profesionales. C-11 agrega los tramos de atención y la consulta que C-19 usará para rechazar los turnos fuera de horario. Es una de las tres condiciones que abren el Gate 3.

## What Changes

- Agregar la gestión de tramos de atención por profesional con `POST` y `GET /api/profesionales/{id}/horarios`, y `PUT` y `DELETE /api/profesionales/{id}/horarios/{horarioId}`. Cada tramo tiene día de la semana (1 a 7, ISO) y un intervalo `[horaInicio, horaFin)` en hora local.
- Validar que `horaFin` sea posterior a `horaInicio` y que los tramos del mismo profesional y día no se superpongan (RN-16). Los tramos contiguos son válidos.
- Permitir que `RECEPCION` y `ADMIN` gestionen los horarios de cualquier profesional. `ODONTOLOGO` solo lista los propios cuando su vínculo se resuelve del lado servidor y recibe `403` al escribir (RN-33, SU-02).
- Incorporar una consulta interna que indique si un intervalo candidato cabe completo en un único tramo del profesional. Se calcula en `America/Argentina/Buenos_Aires` (RN-04, RN-14, RN-16).
- Quedan fuera:
  - el frontend (C-22);
  - la validación al crear turnos (C-19);
  - el impacto de editar horarios sobre turnos ya dados, que no existen todavía;
  - los horarios con vigencia por fechas;
  - un horario general del consultorio (RN-16, suposición).

## Capabilities

### New Capabilities

- `horarios-de-atencion`: administración de los tramos semanales de atención por profesional y consulta de contención de un intervalo para los consumidores de agenda.

### Modified Capabilities

Ninguna.

## Impact

- Se agregan el subpaquete backend `profesionales.horario` y la tabla `horario_atencion` en PostgreSQL, con una FK idempotente en `schema-constraints.sql`.
- Se suman matchers de ruta en `SecurityConfig` para denegar las escrituras de `ODONTOLOGO` antes de la validación de MVC, como se hizo en C-12.
- Se reutilizan `Intervalo`, `ZonaHoraria` y la política de titularidad de C-06, sin modificar `Profesional` ni sus endpoints.
- C-19, C-22 y C-33 consumirán esta capacidad.

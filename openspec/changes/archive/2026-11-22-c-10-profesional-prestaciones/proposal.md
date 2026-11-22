# Proposal

## Why

RN-11 exige que el profesional de un turno realice la prestación elegida, y SU-09 supone que cada profesional hace solo un subconjunto del catálogo. Hoy existen el catálogo de prestaciones (C-04) y el de profesionales (C-06), pero nada los relaciona: sin esa asociación, la creación de turnos (C-19), la disponibilidad pública (C-38), el seed (C-33) y la pantalla de profesionales (C-21) no pueden ofrecer ni validar combinaciones profesional-prestación. C-10 está en el camino crítico y es uno de los tres changes de la Ola 2 que abren C-19.

## What Changes

- Nueva asociación profesional-prestación (`ProfesionalPrestacion`): una pareja única por profesional y prestación, guardada en la tabla `profesional_prestacion` con clave compuesta e integridad referencial hacia `profesional` y `prestacion`.
- `GET /api/profesionales/{id}/prestaciones`: devuelve las prestaciones que realiza el profesional (id, nombre, duración por defecto y si está activa). Leen `ADMIN` y `RECEPCION` cualquier profesional; `ODONTOLOGO` solo el propio, con la misma regla de titularidad que el catálogo de profesionales (C-06).
- `PUT /api/profesionales/{id}/prestaciones`: reemplaza el conjunto completo de prestaciones del profesional (asocia y desasocia en una sola operación). Solo escribe `ADMIN`; `RECEPCION` y `ODONTOLOGO` reciben `403` (RN-20, RN-33).
- Una prestación inactiva no puede asociarse por primera vez; una asociación previa con una prestación que luego se dio de baja se puede conservar (RN-37: no se pierde historial).
- Puerto de lectura `ProfesionalRealizaPrestacion` para que agenda (C-19) y reserva pública (C-38) consulten si un profesional realiza una prestación, sin acceder al almacenamiento de este módulo.
- Todo el código nuevo vive en el subpaquete `ar.edu.utn.turnos.profesionales.prestacion`; **no se editan** `Profesional.java` ni `Prestacion.java` (mitigación RM-01). El único archivo compartido que se toca es `schema-constraints.sql`, para agregar las claves foráneas de forma idempotente.

## Capabilities

### New Capabilities
- `profesional-prestaciones`: qué prestaciones realiza cada profesional, su consulta y reemplazo por API según el rol, y la consulta interna "¿el profesional realiza esta prestación?" para los módulos de agenda y reserva.

### Modified Capabilities
<!-- Ninguna: catalogo-profesionales y catalogo-prestaciones (C-04) no cambian sus requisitos; solo se consumen sus consultas internas. -->

## Impact

- **Código nuevo**: `backend/src/main/java/ar/edu/utn/turnos/profesionales/prestacion/{application,infrastructure,web}` y sus tests en `backend/src/test/java/.../profesionales/prestacion/`.
- **Esquema**: tabla nueva `profesional_prestacion` (Hibernate `ddl-auto`, DD-08) y dos claves foráneas más un índice agregados en `backend/src/main/resources/schema-constraints.sql`.
- **API**: dos endpoints nuevos bajo `/api/profesionales/{id}/prestaciones`, autenticados por sesión y con CSRF en el `PUT` (configuración existente de C-03/C-07; no se modifica `SecurityConfig`).
- **Dependencias internas**: consume `ProfesionalConsulta` y `ServicioProfesionales.detalle` (C-06) y `ConsultaPrestaciones` (C-04) sin modificarlos.
- **Desbloquea**: C-19, C-21, C-33 y C-38. Sin dependencias externas nuevas.

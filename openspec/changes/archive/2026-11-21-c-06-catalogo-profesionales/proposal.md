# Proposal

## Why

El consultorio necesita mantener un catálogo de profesionales sin perder sus referencias históricas. Agenda, horarios y bloqueos necesitan consultar ese catálogo sin depender de su API HTTP ni de sus entidades JPA.

## What Changes

- Agregar el catálogo interno `/api/profesionales` con alta, listado, detalle y actualización. Cada profesional tiene nombre, apellido, matrícula única, especialidad opcional y estado activo.
- `ADMIN` administra y puede desactivar o reactivar sin borrar; `RECEPCION` consulta ambos estados. `ODONTOLOGO` solo puede consultar su propio registro y no tiene acceso mientras no exista el vínculo de C-13.
- Publicar `ProfesionalConsulta` para consultar por ID y listar activos.
- Quedan fuera las prestaciones (C-10), el vínculo con usuarios (C-13), la interfaz (C-21), los horarios y bloqueos (C-11/C-12), la reserva pública y la guarda de turnos futuros (C-30). C-06 cubre la parte de catálogo de US-005 y de RN-37.

## Capabilities

### New Capabilities

- `catalogo-profesionales`: administración, lectura por rol y consultas internas del catálogo.

### Modified Capabilities

## Impact

- Backend: subpaquete `profesionales.profesional`, API interna y tabla `profesional` en PostgreSQL.
- Reutiliza seguridad y errores de C-01. No modifica el login ni requiere incorporar C-03, C-05 o C-13 para entregar el catálogo.

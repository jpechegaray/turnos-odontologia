# Proposal

## Why

Recepción necesita registrar ausencias de los profesionales para que esos intervalos no se ofrezcan para turnos. C-06 ya provee el catálogo; C-12 agrega los bloqueos y la consulta que usará la agenda para aplicar RN-04.

## What Changes

- Agregar alta, listado y eliminación de bloqueos por profesional (`POST` y `GET /api/profesionales/{id}/bloqueos`, `DELETE /api/profesionales/{id}/bloqueos/{bloqueoId}`).
- Permitir que `RECEPCION` y `ADMIN` gestionen los bloqueos de cualquier profesional; `ODONTOLOGO` solo puede listar los propios cuando su vínculo se resuelve del lado servidor (RN-33).
- Incorporar `BloqueoConsulta.solapa(profesionalId, Intervalo)` con intervalos semiabiertos: un candidato que toca un extremo sin superponerse no queda bloqueado (RN-02).
- Dejar fuera la edición de bloqueos, el frontend (C-22), los bloqueos por sillón (SU-08), la validación al crear turnos (C-19) y el rechazo de bloqueos que pisan turnos activos (RN-17, C-30).

## Capabilities

### New Capabilities

- `bloqueos-de-agenda`: administración de bloqueos por profesional y consulta de solapamiento para los consumidores de agenda.

### Modified Capabilities

Ninguna.

## Impact

Se agrega el subpaquete backend `profesionales.bloqueo`, la tabla `bloqueo` en PostgreSQL y tres operaciones protegidas por la sesión existente. Se reutiliza el catálogo de C-06 sin modificar sus endpoints ni cambiar la política de acceso global. C-13 aportará el vínculo de odontología; C-19, C-22 y C-30 consumirán esta capacidad.

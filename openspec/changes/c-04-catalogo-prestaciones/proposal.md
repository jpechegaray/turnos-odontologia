# Proposal

## Why

Para dar un turno hace falta saber qué prestación se hace y cuánto dura (RN-03). Hoy no existe ese catálogo. C-04 implementa US-003 y está en el camino crítico: C-10 (profesional-prestaciones), C-18, C-19 (crear turno), C-20 y C-30 dependen de él.

## What Changes

- Incorporar la prestación con nombre único, duración por defecto en minutos y estado activo.
- Validar la duración por defecto según RN-09: entero múltiplo de 5 entre 5 y 480. Una duración fuera de regla se rechaza con 422 y código `RN-09`.
- Exponer `/api/prestaciones` para el personal: listar (con filtro `soloActivas`), consultar por id, dar de alta, modificar y dar de baja lógica (RN-37). La baja nunca borra la fila.
- Permisos según la matriz RBAC: solo `ADMIN` escribe; `RECEPCION` y `ODONTOLOGO` solo leen y reciben 403 al intentar escribir (RN-33).
- Publicar un puerto de consulta de solo lectura para que otros módulos (C-10, C-19) obtengan una prestación y sepan si está activa, sin acceder a la entidad JPA.
- Fuera de alcance: el catálogo público `/api/public/prestaciones` para la reserva online (C-38); impedir la baja con turnos futuros (RN-37, se completa en C-30); la pantalla de administración (C-20); el seed de datos ficticios (C-33).

## Capabilities

### New Capabilities

- `catalogo-prestaciones`: alta, modificación, baja lógica y consulta de las prestaciones del consultorio, con su duración por defecto validada, los permisos por rol y la consulta para otros módulos.

### Modified Capabilities

- Ninguna. Se reutilizan sin cambios los requisitos de `control-acceso` (401 sin sesión, 403 por rol) y de `errores-api` (Problem Details).

## Impact

- `backend/`: nuevo módulo `ar.edu.utn.turnos.prestaciones` con sus capas `domain`, `application`, `infrastructure` y `web`, y sus tests. Hibernate crea la tabla `prestacion` (DD-08). No se tocan archivos compartidos (`pom.xml`, `application.yml`, `schema-constraints.sql`, `shared/`).
- API nueva: `GET /api/prestaciones`, `GET /api/prestaciones/{id}`, `POST /api/prestaciones`, `PUT /api/prestaciones/{id}` y `DELETE /api/prestaciones/{id}`. Es el contrato que consumirá el front en C-20.
- Gobernanza LOW: autonomía si los tests pasan.

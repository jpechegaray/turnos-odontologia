# Proposal

## Why

El paciente anónimo todavía no puede ver qué prestaciones, profesionales y horarios libres hay para reservar online (US-015, Flujo 4). Es la primera mitad de la reserva pública y desbloquea C-41 (crear la reserva) y C-48 (frontend). El núcleo de disponibilidad (C-26) ya existe y se documenta como reutilizable; falta exponerlo sin sesión, con los límites de RN-27 y sin filtrar datos internos.

## What Changes

- Tres endpoints públicos de solo lectura, sin sesión ni CSRF:
  - `GET /api/public/prestaciones`: prestaciones activas que realiza al menos un profesional activo (`id`, `nombre`, `duracionMin`).
  - `GET /api/public/profesionales?prestacionId=`: profesionales activos que realizan la prestación (`id`, `nombre`, `apellido`, `especialidad`; sin matrícula). Prestación inexistente o inactiva: `200 []`. Sin `prestacionId`: 400.
  - `GET /api/public/disponibilidad?profesionalId&prestacionId&desde&hasta`: horarios libres como `{inicio, fin}`, sin `sillonId`.
- La disponibilidad se recorta en silencio a `[ahora + anticipación mínima, ahora + horizonte]` (RN-27), leyendo la configuración en cada consulta, y usa la duración por defecto de la prestación.
- Errores del endpoint público: rango inválido, 422 `RANGO_DISPONIBILIDAD_INVALIDO`; profesional o prestación inexistente o inactivo, o profesional que no realiza la prestación, 404 `RECURSO_NO_DISPONIBLE` con el mismo mensaje en los tres casos; parámetros ausentes o mal formados, 400.
- Puertos de lectura nuevos: `ConsultaPrestaciones.listarActivas()`, `ProfesionalRealizaPrestacion.profesionalesQueRealizan(prestacionId)` y `prestacionesConProfesional(ids)`.
- `/api/agenda/disponibilidad` del personal no cambia (sigue con sus 422 detallados).

## Capabilities

### New Capabilities
- `reserva-publica`: consulta pública y anónima del catálogo reservable y de la disponibilidad (C-41 la amplía luego con la creación de la reserva).

### Modified Capabilities
<!-- Ninguna: el núcleo de agenda-disponibilidad y los endpoints del personal no cambian de comportamiento. -->

## Impact

- Backend: paquete nuevo `reservapublica.application` (fachada `DisponibilidadPublica`) y `reservapublica.web` (controlador y DTOs `record`); extensión de los puertos de `prestaciones` y `profesionales.prestacion` con JPQL parametrizado, sin N+1.
- Seguridad: `/api/public/**` ya es `permitAll` y está fuera de CSRF; el rate limit de C-17 ya lo cubre (solo se prueba el 429 con `Retry-After`).
- Fuera de alcance: crear la reserva, el tope por email y la asignación de sillón (C-41); "cualquier profesional" (PA-09 abierta, se usa SU-03); frontend (C-48).
- Governance HIGH: este change se propone y espera revisión antes de escribir código. Issue #38.

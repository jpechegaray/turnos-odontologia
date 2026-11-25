# Design

## Context

Ver proposal.md para la motivación. Estado actual observado en el código:

- `agenda.application.DisponibilidadService.consultar(profesionalId, prestacionId, desde, hasta, duracionMin)` valida rango (422 `RANGO_DISPONIBILIDAD_INVALIDO`), profesional, prestación y asignación (422 con códigos detallados), usa la duración por defecto si `duracionMin` es null y devuelve `HorarioDisponible(inicio, fin, sillonId)`. No tiene `@PreAuthorize`; el personal entra por `ConsultarDisponibilidadPersonal`.
- `/api/public/**` es `permitAll` y está fuera de CSRF (`SecurityConfig`); el filtro de C-17 ya aplica rate limit por IP.
- `ConfiguracionServicio` expone `horizonteReservaOnlineDias()` y `anticipacionMinimaReservaOnlineHoras()`.
- `ConsultaPrestaciones` solo tiene `buscarPorId`; `ProfesionalRealizaPrestacion` solo tiene `realiza`; `ProfesionalConsulta` tiene `listarActivos()` y `buscarPorId`.
- `ReglaNegocioException` admite `EstadoError.NO_ENCONTRADO_404` y `ManejadorErrores` la traduce a Problem Details con `codigo`.
- Tests con reloj fijo: `RelojFijoConfig` (`2026-12-01T12:00:00Z`).

## Goals / Non-Goals

**Goals:**
- Exponer catálogo reservable y disponibilidad sin sesión, reutilizando el núcleo sin duplicar cálculo.
- No revelar por qué un recurso no está disponible ni datos internos (sillón, matrícula).

**Non-Goals:**
- Crear la reserva, tope por email, elección de sillón (C-41); "cualquier profesional" (PA-09); frontend (C-48); cambiar el endpoint del personal.

## Decisions

1. **Fachada `reservapublica.application.DisponibilidadPublica` sobre `DisponibilidadService`**, sin `@PreAuthorize`. Alternativa descartada: agregar un modo "público" al núcleo; mezclaría reglas de exposición con el cálculo y arriesga el contrato del personal.
2. **Orden de la fachada**: (a) validar el rango pedido (fin posterior al inicio, máximo 31 días, 422 `RANGO_DISPONIBILIDAD_INVALIDO`); (b) validar recursos (profesional activo, prestación activa, asignación) **antes** del recorte; (c) recortar a `[ahora + anticipación, ahora + horizonte]` leyendo `ConfiguracionServicio` en cada consulta, y devolver `[]` si no queda rango; (d) llamar al núcleo con `duracionMin = null`; (e) mapear a `{inicio, fin}` sin `sillonId`. Validar recursos antes del recorte evita que un profesional inactivo con rango fuera del horizonte dé `200 []` en vez de 404. Como el núcleo también revalida el rango, la fachada hace su propio chequeo de (a) sobre el rango pedido y no sobre el recortado.
3. **404 uniforme (opción B) solo en el endpoint público.** La fachada resuelve los recursos con los puertos (`ProfesionalConsulta`, `ConsultaPrestaciones`, `ProfesionalRealizaPrestacion.realiza`) y lanza `ReglaNegocioException(RECURSO_NO_DISPONIBLE, NO_ENCONTRADO_404, <mensaje único>)` en los tres casos. Además traduce de forma defensiva los códigos de recurso que lance el núcleo (`PROFESIONAL_INEXISTENTE`, `PROFESIONAL_INACTIVO`, `PRESTACION_INEXISTENTE`, `PRESTACION_INACTIVA`, `RN-11`) al mismo 404, por si cambia el orden o hay una carrera. La separación es por endpoint, no por rol ni por sesión, para que un usuario logueado que use el endpoint público reciba lo mismo. `RANGO_DISPONIBILIDAD_INVALIDO` no se traduce.
4. **Puertos nuevos** (lectura, JPQL parametrizado, sin N+1): `ConsultaPrestaciones.listarActivas()`; `ProfesionalRealizaPrestacion.profesionalesQueRealizan(prestacionId)` y `prestacionesConProfesional(profesionalIds)`, que devuelven ids y no filtran por estado (como `realiza`). Así el módulo de asociaciones no hace JOIN con la tabla de otro módulo. `reservapublica.application.CatalogoPublico` combina los puertos con una cantidad fija de consultas: `GET /prestaciones` = `listarActivas()` ∩ `prestacionesConProfesional(ids de listarActivos())`; `GET /profesionales` resuelve la prestación (inactiva o inexistente → `[]`) y filtra `listarActivos()` por `profesionalesQueRealizan`.
5. **DTOs públicos como `record` nuevos en `reservapublica.web`** (`PrestacionPublicaDto`, `ProfesionalPublicoDto`, `HorarioPublicoDto`); nunca se exponen `HorarioDisponible`, `PrestacionResumen` ni `ProfesionalConsulta.Datos`, así que `sillonId` y matrícula no pueden filtrarse por agregar un campo al dominio.
6. **Parámetros**: `desde`/`hasta` como `OffsetDateTime` con ISO-8601 con zona; ausente o mal formado → 400 (excepción de binding de Spring), distinto del 422 de rango.
7. **Rate limit**: no se toca código; se agrega una clase IT con `app.ratelimit.max-solicitudes-por-ip=2` que prueba el 429 con `Retry-After`.
8. **Nombres**: capability `reserva-publica` nueva; C-41 la amplía. `agenda-disponibilidad` no se modifica.

## Risks / Trade-offs

- [Un cambio futuro del núcleo reordena sus validaciones] → la fachada valida recursos por su cuenta y traduce defensivamente los códigos del núcleo; un test cubre los tres 404.
- [Enumeración de ids por 404 distinguibles] → mismo código y mensaje en los tres casos; el listado de profesionales devuelve `200 []` para prestaciones no reservables (no distingue inexistente de inactiva).
- [Disponibilidad es una ayuda, no una garantía (RN-08)] → la validación definitiva y el 409 ocurren en C-41; aquí se documenta.
- [Recorte silencioso puede sorprender al cliente] → decisión tomada: un rango fuera del horizonte da `200 []`; el frontend (C-48) debe leer la configuración pública de horizonte si quiere avisar.
- [Lectura de configuración en cada consulta cuesta una consulta extra] → aceptado: garantiza que un cambio aplique a la siguiente consulta; el volumen está acotado por el rate limit.

## UI Mockups

- Decision: no (no UI) — 2026-11-25. Solo backend; el frontend de reserva es C-48.

## Plan de pruebas (resumen)

Horario ocupado no se ofrece; contiguo sí; recurso inactivo no aparece; JSON solo con `inicio`/`fin`; bordes de anticipación y horizonte con `RelojFijoConfig`; cambio de configuración aplica a la siguiente consulta; los tres 404 idénticos; `/api/agenda/disponibilidad` conserva sus 422; 429 con `Retry-After`. Cada test lleva `@DisplayName` con el nombre exacto del escenario.

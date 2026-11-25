# Design

## Context

Ver proposal.md. C-19 dejó Intervalo, TurnoOcupado y los puertos de recursos. HorarioConsulta y BloqueoConsulta solo contestan por intervalo: llamarlos para cada inicio genera demasiadas consultas.

## Goals / Non-Goals

**Goals:** cálculo puro con datos precargados y núcleo reutilizable sin sesión, sin editar archivos de C-27.
**Non-Goals:** reservar horarios en la consulta, implementar C-38 o modificar reglas de creación.

## Decisions

1. GET usa `profesionalId`, `prestacionId`, `desde`, `hasta` (ISO-8601 con offset) y `duracionMin` opcional. El turno completo cabe en el rango semiabierto; resultados JSON `{inicio, fin, sillonId}` en UTC. Máximo 31 días exactos por consulta para acotar trabajo; no es el horizonte de reserva de RN-27. El rango histórico devuelve vacío.
2. CalculadorDisponibilidad recibe Intervalo de búsqueda, duración, ahora, tramos materializados en instantes, bloqueos, TurnoOcupado y sillones activos. Recorre inicios de cinco minutos locales redondeando hacia arriba; exige inicio > ahora y contención en un único tramo y día. Elige el primer sillón libre por id, sin duplicar horarios. No contiene Spring ni acceso a datos.
3. Un puerto nuevo AgendaProfesionalConsulta en profesionales.application expone horarios y bloqueos como datos, implementado con las persistencias públicas del mismo módulo. Agenda solo depende de ese puerto; no accede a JPA ajena. Se descarta usar los servicios web de horarios porque exigen sesión y limitarían C-38.
4. ConsultaOcupacionDisponibilidad usa EntityManager y JPQL parametrizado dentro de agenda, proyectando solo ids de recursos, intervalo y estado, sobre turnos que solapan el rango y no están cancelados. No se cambia TurnoJpaRepository ni se consulta por cada inicio.
5. DisponibilidadService es el núcleo interno readOnly, validando rango, recursos y relación. ConsultarDisponibilidadPersonal agrega @PreAuthorize ADMIN/RECEPCION. El controller usa la fachada. No se modifica SecurityConfig; los tests verifican denegación con parámetros válidos y acceso directo al servicio del personal.
6. Sin cambios de esquema. Las listas son instantáneas orientativas: al crear se revalida y las restricciones EXCLUDE resuelven la carrera. Sin pacientes en la respuesta ni en la consulta de ocupación.
7. El calendario de entrada admite años locales de 1 a 9999. Se valida antes de llegar a PostgreSQL para devolver 422, en lugar de 500 por desbordamiento, con fechas extremas de ISO-8601. Se conserva el máximo de 31 días sin truncar fracciones.

## Risks / Trade-offs

- Cambio de agenda tras consultar → POST vuelve a validar y puede responder 409.
- Horarios de una semana se materializan por día en Buenos Aires, sin desfase fijo; no se fusionan tramos.
- Máximo 31 días y datos acotados de turnos evitan búsquedas ilimitadas. Se carga el catálogo de sillones activos y se ordena por id para resultado estable.

## Migration Plan

No aplica: archivos nuevos, sin migración ni configuración.

## UI Mockups

Decision: no (no UI).

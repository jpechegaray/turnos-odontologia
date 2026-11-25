# Design

## Context

C-40 ofrece la pantalla Agenda. C-07 permite buscar fichas activas. C-26 entrega una sugerencia de sillón por cada inicio disponible. C-19 valida el alta y conserva la garantía de concurrencia.

## Goals / Non-Goals

- Crear desde Agenda con paciente, profesional, prestación, fecha, duración, horario y sillón.
- Explicar cómo continuar ante conflictos y reglas de negocio.
- Dejar la reprogramación, cancelación y asistencia para C-46.

## Decisions

- El panel se abre desde Agenda para ADMIN y RECEPCION. Se integra con controles del kit UI y tokens existentes. El diseño del proyecto usa el modo system-only.
- El día de agenda inicia el selector de fecha. La consulta usa las medianoches locales de ese día y del siguiente en ISO con zona, con la duración efectiva. La búsqueda requiere una acción del usuario y cambiar los criterios invalida los resultados y la selección.
- La búsqueda explícita de pacientes usa un criterio del endpoint C-07. El formulario no guarda datos en almacenamiento persistente.
- El catálogo de prestaciones se filtra por las activas asignadas al profesional. Cada intervalo de disponibilidad propone un sillón activo y el usuario puede elegir otro. El backend valida la combinación final.
- El 409 distingue el recurso afectado y vuelve a consultar con los mismos criterios. Un 422 interpreta el código y, si la regla es RN-04, el motivo. Los datos del formulario se conservan para poder corregirlos.
- Al guardar, el formulario navega la agenda al día del turno en la vista vigente y refresca la consulta. Si un filtro excluiría el nuevo turno, se limpia ese filtro.
- Los selectores tienen etiquetas nativas y los cambios de estado se anuncian como mensajes de estado o alertas. Al cerrar se devuelve el foco a Nuevo turno.

## Risks / Trade-offs

La disponibilidad es una instantánea. El horario puede ocuparse entre la consulta y el alta, por lo que el servidor vuelve a validarlo. La consulta devuelve un único sillón por horario y la lista de opciones manual se limita al catálogo activo.

## Migration Plan

No aplica.

## Open Questions

Ninguna.

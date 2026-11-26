# inicio-frontend Specification

## Purpose

Da al personal un resumen del día con turnos, ocupación de sillones y accesos a las tareas frecuentes. Usa las consultas existentes de agenda, sillones y lista de espera.

## Requirements

### Requirement: Accesos a las secciones en inicio

Inicio SHALL mostrar junto al listado de turnos un panel de accesos a registrar pacientes, revisar la lista de espera y consultar bloqueos. Cada acceso MUST respetar la visibilidad de la sección para el rol.

#### Scenario: Inicio muestra accesos rápidos de las secciones permitidas
- **GIVEN** una sesión de `RECEPCION`
- **WHEN** el usuario abre inicio
- **THEN** ve accesos a registrar pacientes, revisar la lista de espera y consultar bloqueos

### Requirement: Resumen del día en inicio

La pantalla de inicio SHALL pedir `GET /api/agenda?vista=dia` sin `fecha` y mostrar la cantidad de turnos del día sin cancelados, confirmados y sin confirmar, con los atendidos en el detalle del total. Para los roles habilitados SHALL mostrar el total de entradas activas de lista de espera. La ocupación de cada sillón se calcula sobre la unión de las franjas de atención del consultorio. Mientras carga MUST mostrar un estado de carga.

#### Scenario: Inicio muestra las cifras del día
- **GIVEN** una agenda del día con dos turnos confirmados, uno reservado, uno atendido, uno cancelado y una entrada activa de lista de espera
- **WHEN** el usuario abre inicio
- **THEN** se ven 4 turnos del día, 2 confirmados, 1 sin confirmar y 1 entrada de lista de espera

### Requirement: Próximos turnos en inicio

Inicio SHALL listar, ordenados por hora, hasta cinco turnos no cancelados que todavía no empezaron, con hora, paciente y estado, y un enlace a la agenda. Si la API oculta el paciente MUST mostrar "Paciente de otro profesional". Sin turnos pendientes MUST mostrar un mensaje que lo diga.

#### Scenario: Inicio lista los próximos turnos
- **GIVEN** que son las 10:15 en Buenos Aires y la agenda del día tiene turnos a las 09:00, 10:30 y 11:00, y uno cancelado a las 12:00
- **WHEN** se muestra inicio
- **THEN** la lista de próximos turnos muestra, en orden, los de las 10:30 y 11:00 con su paciente y su estado, y un enlace "Ver agenda"

#### Scenario: Inicio sin turnos pendientes
- **GIVEN** una agenda del día sin turnos
- **WHEN** se muestra inicio
- **THEN** se ve el mensaje "No quedan turnos para hoy"

### Requirement: Error al cargar el resumen

Si la consulta de la agenda falla, inicio SHALL mostrar un error con la opción de reintentar, sin ocultar la navegación.

#### Scenario: Un fallo al cargar el resumen permite reintentar
- **GIVEN** que la primera consulta de la agenda responde `500` y la segunda responde la agenda del día
- **WHEN** el usuario elige "Reintentar"
- **THEN** desaparece el error y se ven las cifras del día

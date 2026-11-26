## Purpose

Da al personal un resumen del día al entrar a la SPA: cuántos turnos hay en cada estado y cuáles son los próximos. Los datos salen de la agenda que ya expone la API.

## ADDED Requirements

### Requirement: Accesos a las secciones en inicio

Inicio SHALL mostrar, en el centro de la pantalla, un recuadro por cada entrada del menú que el rol puede ver, salvo Inicio, con su ícono y su nombre, agrupados como en el menú. Cada recuadro MUST ser un enlace a su sección.

#### Scenario: Inicio muestra un recuadro por sección permitida
- **GIVEN** una sesión de `RECEPCION`
- **WHEN** el usuario abre inicio
- **THEN** ve recuadros que enlazan a Agenda, Pacientes y Configuración, y no ve el de Usuarios

### Requirement: Resumen del día en inicio

La pantalla de inicio SHALL pedir `GET /api/agenda?vista=dia` sin `fecha` y mostrar el día que responde el servidor con la cantidad de turnos del día, sin contar los cancelados, y la cantidad por estado (reservados, confirmados, atendidos y ausentes). Mientras carga MUST mostrar un estado de carga.

#### Scenario: Inicio muestra las cifras del día
- **GIVEN** una agenda del día con dos turnos confirmados, uno reservado, uno atendido y uno cancelado
- **WHEN** el usuario abre inicio
- **THEN** se ven 4 turnos del día, 2 confirmados, 1 reservado y 1 atendido

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

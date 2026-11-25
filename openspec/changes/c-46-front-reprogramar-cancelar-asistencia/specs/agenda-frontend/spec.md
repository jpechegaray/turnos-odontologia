# Spec Delta

## ADDED Requirements

### Requirement: Gestión de turnos del personal

La agenda diaria y semanal SHALL ofrecer a administración y recepción la gestión de los turnos activos. Antes del inicio MUST permitir reprogramar o cancelar. Desde el inicio MUST permitir registrar la asistencia o ausencia. Una marca final de asistencia MUST ser corregible solo por administración. Un turno cancelado o la vista del odontólogo MUST permanecer sin acciones de escritura.

#### Scenario: Recepción dispone de acciones según la hora y el estado

- **WHEN** recepción elige un turno activo futuro, un activo iniciado o un turno con marca final
- **THEN** puede cambiar o cancelar el futuro, registrar la asistencia desde el inicio y no puede corregir marcas finales ni modificar cancelados

#### Scenario: Administración puede corregir una asistencia

- **WHEN** administración cambia una marca ATENDIDO a AUSENTE
- **THEN** se pide la corrección y se refresca la agenda

#### Scenario: El odontólogo no puede gestionar turnos

- **WHEN** un odontólogo usa la agenda diaria o semanal
- **THEN** no se ofrece gestión ni se envían escrituras

### Requirement: Reprogramación y cancelación con aviso de plazo

La SPA SHALL consultar el plazo vigente para advertir antes de reprogramar o cancelar. MUST interpretar el plazo en relación con el inicio original. Cuando no lo obtiene SHALL informar que el servidor determinará la marca sin suponer un plazo. MUST enviar la fecha y hora elegidas en Buenos Aires en el cambio y el motivo opcional en la cancelación. MUST mostrar la marca confirmada por la API.

#### Scenario: La agenda semanal reprograma y muestra la marca tardía

- **WHEN** recepción reprograma un turno de la agenda semanal dentro del plazo y el servidor confirma la marca
- **THEN** se advierte antes de enviar con CSRF el horario convertido desde Buenos Aires y se actualiza la agenda mostrando la marca

#### Scenario: Recepción cancela con motivo y la marca del servidor

- **WHEN** recepción confirma la cancelación con motivo
- **THEN** se registra el motivo y la confirmación muestra la marca tardía devuelta por la API

#### Scenario: En el límite exacto no se anticipa una marca tardía

- **WHEN** restan exactamente las horas del plazo configurado
- **THEN** el aviso previo no califica la operación como tardía

#### Scenario: Si falla la consulta del plazo no se presume una marca tardía

- **WHEN** la configuración no se puede consultar
- **THEN** se informa que la API determinará la marca y se puede reintentar la consulta

### Requirement: Recuperación ante rechazos de gestión

La SPA SHALL mostrar permisos insuficientes, conflictos y reglas de negocio como mensajes corregibles. Un conflicto de horario MUST conservar la reprogramación pendiente. Un conflicto de estado o versión MUST ofrecer recargar la agenda.

#### Scenario: Un conflicto de horario conserva la reprogramación

- **WHEN** un sillón queda ocupado antes de completar la reprogramación
- **THEN** se explica el conflicto y se conserva la fecha y hora elegidas

#### Scenario: Un cambio concurrente permite actualizar la agenda

- **WHEN** una operación recibe un conflicto por estado o versión modificados
- **THEN** se informa que el turno cambió y se puede recargar la agenda

#### Scenario: Una escritura denegada informa la falta de permiso

- **WHEN** la API rechaza una solicitud con 403
- **THEN** se informa la denegación y no se anuncia éxito

#### Scenario: Una regla de negocio informa el ajuste necesario

- **WHEN** la API rechaza una acción por horario, duración o un turno que ya empezó
- **THEN** la interfaz explica qué se debe corregir o por qué se debe registrar asistencia

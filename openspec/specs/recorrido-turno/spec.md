# recorrido-turno Specification

## Purpose
Garantiza que el ciclo de vida de un turno funcione de punta a punta entre la reserva online, la agenda, la lista de espera y las notificaciones, con datos ficticios y en un entorno limpio.

## Requirements

### Requirement: Reserva online confirmada de punta a punta

Una reserva online confirmada SHALL dejar un turno en la agenda del personal, un email de confirmación con el enlace del turno y un enlace que permite ver y cancelar ese turno sin iniciar sesión (RN-07, RN-08, RN-25, RN-31).

#### Scenario: Un paciente reserva online y gestiona su turno por el enlace
- **GIVEN** un consultorio con un profesional, un sillón y una prestación reservables, y un horario libre dentro de la atención
- **WHEN** un paciente ficticio reserva ese horario por la API pública
- **THEN** recepción ve el turno en la agenda de ese día, el paciente recibe un solo email de confirmación con el enlace del turno, y con ese enlace puede ver el turno y cancelarlo

### Requirement: Cancelación que termina en un hueco tomado

Una cancelación confirmada SHALL avisar a la lista de espera, y la toma de la oferta SHALL crear un turno nuevo en el mismo hueco, con sus emails, sin intervención de recepción (RN-06, RN-23, RN-24, RN-38).

#### Scenario: Una cancelación termina con el hueco tomado desde la lista de espera
- **GIVEN** un turno futuro y una entrada activa compatible con su hueco, de un paciente con email
- **WHEN** recepción cancela el turno y el paciente de la entrada toma la oferta con el enlace del aviso
- **THEN** el paciente del turno cancelado recibe el aviso de cancelación, el de la entrada recibe el aviso de hueco y la confirmación de su nuevo turno, el hueco queda ocupado por ese turno y la entrada pasa a `ASIGNADA`

### Requirement: Reprogramación que libera un hueco para la lista de espera

Una reprogramación confirmada SHALL avisar a la lista de espera por cada tramo del horario anterior que quedó libre, y la oferta SHALL poder tomarse igual que la de una cancelación (RN-18, RN-23, RN-24).

#### Scenario: Una reprogramación termina con el hueco tomado desde la lista de espera
- **GIVEN** un turno futuro y una entrada activa compatible con su horario actual, de un paciente con email
- **WHEN** recepción reprograma el turno a otro día y el paciente de la entrada toma la oferta
- **THEN** el paciente reprogramado recibe el aviso de reprogramación, el de la entrada recibe el aviso de hueco y la confirmación de su nuevo turno, y el horario anterior queda ocupado por ese turno nuevo

### Requirement: Recordatorio único a lo largo del ciclo

Un turno vigente SHALL recibir un solo recordatorio aunque el planificador corra varias veces, y un turno cancelado MUST NOT recibirlo (RN-29, RN-30).

#### Scenario: El recordatorio llega una sola vez y no llega al turno cancelado
- **GIVEN** dos turnos dentro de la ventana de recordatorio, uno vigente y otro cancelado
- **WHEN** el planificador corre dos veces
- **THEN** solo el paciente del turno vigente recibe el recordatorio, una sola vez

### Requirement: Recorridos en un entorno limpio

Los recorridos SHALL ejecutarse en la build contra una base PostgreSQL real recién creada, con datos ficticios, un reloj fijo y correo en memoria, sin depender de la base local, de la hora del sistema ni de un servidor de correo.

#### Scenario: Los recorridos pasan en una build limpia
- **WHEN** se ejecuta la verificación del backend en una máquina con Docker y sin base local
- **THEN** todos los recorridos pasan y ninguno envía un email real

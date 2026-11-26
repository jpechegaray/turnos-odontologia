# Spec Delta

## MODIFIED Requirements

### Requirement: Aviso automático al liberarse un hueco

Cuando se confirma la cancelación de un turno, o una reprogramación que deja libre parte de su horario anterior, el sistema SHALL crear una oferta y enviar un email por cada entrada elegida por la selección de la lista de espera para cada hueco liberado (RN-06, RN-18, RN-23, SU-14). Sin operación confirmada, o con el hueco ya empezado, MUST NOT avisarse a nadie. Un fallo al avisar MUST NOT hacer fallar la operación.

#### Scenario: Tras cancelar solo reciben aviso las entradas compatibles
- **GIVEN** un turno futuro de 30 minutos de un profesional que realiza "Consulta" (30 minutos) y "Tratamiento de conducto" (90 minutos), y dos entradas activas con email, una de cada prestación, sin preferido ni franja y con la fecha del hueco dentro de su rango
- **WHEN** recepción cancela el turno
- **THEN** se crea una sola oferta, para la entrada de "Consulta", y solo ese paciente recibe un email de aviso

#### Scenario: El tope de avisos elige a las entradas más antiguas
- **GIVEN** un tope de avisos de 2 y tres entradas compatibles de pacientes distintos creadas en días distintos
- **WHEN** se cancela el turno que libera el hueco
- **THEN** se crean ofertas y se envían emails solo para las dos entradas más antiguas

#### Scenario: Un paciente con dos entradas compatibles recibe un solo aviso
- **GIVEN** un paciente con dos entradas compatibles con el hueco
- **WHEN** se cancela el turno que libera el hueco
- **THEN** se crea una sola oferta, para la entrada más antigua, y el paciente recibe un solo email

#### Scenario: Sin entradas compatibles no se avisa a nadie
- **GIVEN** un hueco sin entradas compatibles
- **WHEN** se cancela el turno
- **THEN** la cancelación responde con éxito, no se crean ofertas y no se envían emails

#### Scenario: Una cancelación revertida no avisa
- **GIVEN** una entrada compatible con el hueco de un turno
- **WHEN** la transacción de la cancelación falla y se revierte
- **THEN** no se crean ofertas y no se envían emails

#### Scenario: Un fallo al avisar no hace fallar la cancelación
- **GIVEN** una entrada compatible y un fallo inesperado al crear las ofertas
- **WHEN** recepción cancela el turno
- **THEN** la cancelación responde con éxito y el turno queda `CANCELADO`

#### Scenario: Una reprogramación avisa por el horario que dejó libre
- **GIVEN** un turno futuro y una entrada activa con email compatible con su horario actual
- **WHEN** recepción reprograma el turno a otro día
- **THEN** se crea una oferta para esa entrada con el hueco del horario anterior y el paciente recibe un email de aviso

#### Scenario: Una reprogramación que se superpone avisa solo por el tramo liberado
- **GIVEN** un turno futuro de 10:00 a 11:00 y una entrada activa con email compatible con un hueco de 30 minutos
- **WHEN** recepción reprograma el turno, con el mismo profesional, a 10:30 a 11:30
- **THEN** la oferta creada es para el hueco de 10:00 a 10:30

# fechas-frontend Specification

## Purpose
Garantiza que la SPA muestre y construya horarios siempre en la zona del consultorio (`America/Argentina/Buenos_Aires`), sin depender de la zona del navegador ni asumir un desfase fijo (RN-14), y que trate los intervalos como semiabiertos `[inicio, fin)` igual que el backend (RN-02).

## Requirements

### Requirement: Presentación en la zona de Buenos Aires
La SPA SHALL mostrar fecha (`dd/mm/aaaa`) y hora (`HH:mm`, 24 h) de un instante UTC en la zona `America/Argentina/Buenos_Aires`, con el mismo resultado sin importar la zona horaria configurada en el navegador.

#### Scenario: Un instante UTC se muestra en hora de Buenos Aires
- **GIVEN** el instante `2026-12-01T12:00:00Z`
- **WHEN** la SPA lo formatea
- **THEN** muestra la fecha `01/12/2026` y la hora `09:00`

#### Scenario: Un turno de noche se muestra en el día local anterior al día UTC
- **GIVEN** el instante `2026-12-02T02:30:00Z`
- **WHEN** la SPA lo formatea
- **THEN** muestra la fecha `01/12/2026` y la hora `23:30`

#### Scenario: La medianoche local se muestra como 00:00 del día local
- **GIVEN** el instante `2026-12-02T03:00:00Z`
- **WHEN** la SPA lo formatea
- **THEN** muestra la fecha `02/12/2026` y la hora `00:00`

### Requirement: Conversión de hora local a instante UTC
Dados una fecha y una hora locales de Buenos Aires, la SPA SHALL obtener el instante UTC correspondiente calculando el desfase de la zona para esa fecha, sin usar un desfase fijo en el código. Una fecha u hora mal formada MUST rechazarse.

#### Scenario: Una hora local se convierte al instante UTC
- **WHEN** la SPA convierte la fecha `2026-12-01` y la hora `09:00` locales
- **THEN** obtiene el instante `2026-12-01T12:00:00.000Z`

#### Scenario: Una hora local cercana a la medianoche cae en el día UTC siguiente
- **WHEN** la SPA convierte la fecha `2026-12-01` y la hora `23:30` locales
- **THEN** obtiene el instante `2026-12-02T02:30:00.000Z`

#### Scenario: Una hora local mal formada se rechaza
- **WHEN** la SPA convierte la fecha `2026-12-01` y la hora `25:00`
- **THEN** la conversión falla con un error

### Requirement: Intervalos semiabiertos
Un intervalo SHALL representarse como `[inicio, fin)` con `inicio` anterior a `fin`. Dos intervalos MUST considerarse solapados solo si `a.inicio < b.fin` y `b.inicio < a.fin`. Un instante MUST considerarse contenido si `inicio <= instante < fin`.

#### Scenario: Intervalos contiguos no se solapan
- **GIVEN** un intervalo de 10:00 a 10:30 y otro de 10:30 a 11:00
- **WHEN** la SPA evalúa si se solapan
- **THEN** el resultado es que no se solapan, en cualquier orden

#### Scenario: Intervalos que comparten un minuto se solapan
- **GIVEN** un intervalo de 10:00 a 10:30 y otro de 10:29 a 11:00
- **WHEN** la SPA evalúa si se solapan
- **THEN** el resultado es que se solapan

#### Scenario: Un intervalo contenido en otro se solapa
- **GIVEN** un intervalo de 10:00 a 11:00 y otro de 10:15 a 10:45
- **WHEN** la SPA evalúa si se solapan
- **THEN** el resultado es que se solapan, en cualquier orden

#### Scenario: El fin de un intervalo no está contenido en él
- **GIVEN** un intervalo de 10:00 a 10:30
- **WHEN** la SPA evalúa si contiene las 10:00 y las 10:30
- **THEN** contiene las 10:00 y no contiene las 10:30

#### Scenario: Un intervalo vacío o invertido se rechaza
- **WHEN** la SPA crea un intervalo cuyo fin es igual o anterior a su inicio
- **THEN** la creación falla con un error

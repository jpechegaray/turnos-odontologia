# reloj-del-sistema Specification

## Purpose
Provee una fuente única de la hora actual del sistema, en la zona `America/Argentina/Buenos_Aires` y reemplazable en pruebas. Así toda regla temporal (plazos, recordatorios, vencimiento de tokens) es determinista y no asume un desfase fijo (RN-14, DD-05).

## Requirements

### Requirement: Hora del sistema en la zona de Buenos Aires
El sistema SHALL obtener la hora actual de una única fuente configurada en la zona `America/Argentina/Buenos_Aires`, identificada por su nombre de zona y no por un desfase fijo.

#### Scenario: El reloj del sistema usa la zona de Buenos Aires
- **WHEN** se consulta la zona del reloj del sistema en la aplicación en ejecución
- **THEN** la zona es `America/Argentina/Buenos_Aires`
- **AND** no es un desfase fijo como `-03:00`

#### Scenario: Un instante UTC se interpreta en hora local de Buenos Aires
- **GIVEN** un reloj de la zona del sistema fijado en `2026-12-01T12:00:00Z`
- **WHEN** se obtiene la fecha y hora local actual con ese reloj
- **THEN** el resultado es `2026-12-01T09:00`

### Requirement: Reloj reemplazable en pruebas
Las pruebas SHALL poder reemplazar el reloj del sistema por uno fijo sin cambiar el código de producción, de modo que cualquier componente que dependa de la hora actual vea el instante fijado.

#### Scenario: Una prueba fija la hora actual del sistema
- **GIVEN** una prueba de integración que reemplaza el reloj del sistema por uno fijado en `2026-12-01T12:00:00Z`
- **WHEN** un componente de la aplicación consulta la hora actual
- **THEN** obtiene `2026-12-01T12:00:00Z`

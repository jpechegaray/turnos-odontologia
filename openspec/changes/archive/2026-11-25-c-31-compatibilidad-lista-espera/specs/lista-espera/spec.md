## ADDED Requirements

### Requirement: Compatibilidad entre un hueco y una entrada

Un hueco SHALL ser un profesional P con un intervalo `[inicio, fin)`, `inicio < fin`; el sillón es indistinto. Una entrada SHALL ser compatible solo si está `ACTIVA`, P realiza su prestación activa, el profesional preferido (si hay) es P, la duración por defecto cabe (`duración <= fin − inicio`) y cumple fechas y franja (RN-22). MUST ser una función pura del dominio; no verifica que el hueco esté libre.

#### Scenario: Una prestación de 90 minutos no cabe en un hueco de 30
- **GIVEN** un hueco de 30 minutos de un profesional que realiza "Tratamiento de conducto" (90 minutos) y "Consulta" (30 minutos)
- **WHEN** se evalúan una entrada de cada prestación, sin profesional preferido ni franja y con el hueco dentro de sus fechas
- **THEN** la entrada de "Tratamiento de conducto" no es compatible y la de "Consulta" sí

#### Scenario: Una prestación que dura exactamente el hueco es compatible
- **GIVEN** un hueco de 30 minutos y una entrada de una prestación de 30 minutos
- **WHEN** se evalúa la compatibilidad
- **THEN** la entrada es compatible

#### Scenario: El profesional del hueco debe realizar la prestación
- **GIVEN** un hueco de un profesional que no realiza la prestación de la entrada
- **WHEN** se evalúa la compatibilidad
- **THEN** la entrada no es compatible

#### Scenario: Una prestación inactiva no es compatible
- **GIVEN** una entrada cuya prestación fue desactivada después del alta
- **WHEN** se evalúa con un hueco donde cabe
- **THEN** la entrada no es compatible

#### Scenario: Se respeta el profesional preferido
- **GIVEN** una entrada con profesional preferido A
- **WHEN** se evalúa con un hueco de A y con un hueco de otro profesional B que también realiza la prestación
- **THEN** es compatible con el hueco de A y no con el de B

#### Scenario: Una entrada sin profesional preferido acepta a cualquiera que realice la prestación
- **GIVEN** una entrada sin profesional preferido
- **WHEN** se evalúa con un hueco de un profesional que realiza la prestación
- **THEN** la entrada es compatible

#### Scenario: Una entrada que no está activa no es compatible
- **GIVEN** entradas `ASIGNADA`, `CANCELADA` y `VENCIDA` que cumplirían el resto de las condiciones
- **WHEN** se evalúan con el hueco
- **THEN** ninguna es compatible

### Requirement: Fechas y franja de la entrada en la compatibilidad

La fecha del `inicio` del hueco en `America/Argentina/Buenos_Aires` SHALL estar dentro de `[fechaDesde, fechaHasta]`, con los extremos incluidos. Si la entrada tiene franja, el turno resultante `[inicio, inicio + duración)` en hora de Buenos Aires SHALL empezar en `franjaDesde` o después y terminar en `franjaHasta` o antes, el mismo día (RN-22).

#### Scenario: La fecha del hueco debe estar dentro del rango de la entrada
- **GIVEN** una entrada con `fechaDesde` 2030-06-10 y `fechaHasta` 2030-06-20
- **WHEN** se evalúa con huecos que empiezan el 2030-06-09, el 2030-06-10, el 2030-06-20 y el 2030-06-21 en Buenos Aires
- **THEN** solo son compatibles los del 2030-06-10 y el 2030-06-20

#### Scenario: La fecha se toma en hora de Buenos Aires
- **GIVEN** una entrada con `fechaHasta` 2030-06-12
- **WHEN** se evalúa con un hueco que empieza a las 22:00 del 2030-06-12 en Buenos Aires (01:00 UTC del 2030-06-13)
- **THEN** la entrada es compatible

#### Scenario: El turno resultante debe caer dentro de la franja
- **GIVEN** una entrada de una prestación de 30 minutos con franja de 09:00 a 12:00
- **WHEN** se evalúa con huecos de una hora que empiezan a las 08:30, a las 09:00, a las 11:30 y a las 11:45 en Buenos Aires
- **THEN** son compatibles los de las 09:00 y las 11:30, y no lo son los de las 08:30 y las 11:45

#### Scenario: Una entrada sin franja acepta cualquier hora
- **GIVEN** una entrada sin franja
- **WHEN** se evalúa con un hueco a las 19:00 en Buenos Aires
- **THEN** la entrada es compatible

### Requirement: Selección de las entradas a avisar

Entre las entradas compatibles con un hueco, el sistema SHALL elegir a quién avisar de la siguiente forma (RN-23, SU-14):

- las ordena por antigüedad: `creadoEn` ascendente y, si empatan, `id` ascendente;
- deja una sola entrada por paciente, la más antigua;
- corta la lista en `lista_espera.max_avisos_por_hueco`, contando pacientes distintos.

Si el momento actual es igual al `inicio` del hueco o posterior, la selección MUST ser vacía. La selección MUST ser una función pura del dominio.

#### Scenario: El tope de avisos elige a los más antiguos
- **GIVEN** 12 entradas compatibles de pacientes distintos y un tope de 10
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se eligen las 10 más antiguas, en orden de antigüedad

#### Scenario: Un paciente con dos entradas compatibles recibe un solo aviso
- **GIVEN** un paciente con dos entradas compatibles de prestaciones distintas, creadas en días distintos
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se elige solo la más antigua de ese paciente

#### Scenario: El paciente repetido no ocupa un lugar del tope
- **GIVEN** un tope de 2 y las entradas compatibles, de la más antigua a la más nueva, del paciente A, del paciente A y del paciente B
- **WHEN** se seleccionan las entradas a avisar
- **THEN** se eligen la primera entrada de A y la de B

#### Scenario: No se avisa por un hueco que ya empezó
- **GIVEN** un hueco cuyo inicio es igual al momento actual o anterior
- **WHEN** se seleccionan las entradas a avisar
- **THEN** la selección es vacía

### Requirement: Consulta de entradas compatibles con un hueco

`ADMIN` y `RECEPCION` SHALL poder consultar `GET /api/lista-espera/compatibles?profesionalId=&inicio=&fin=` (ISO-8601 con zona). La respuesta SHALL ser `200` con todas las entradas compatibles (RN-22), ordenadas por antigüedad, con la representación de la consulta de la lista, sin tope ni deduplicación: son sugerencias para recepción (SU-15). Si el hueco ya empezó, la lista MUST ser vacía.

#### Scenario: Recepción ve solo las entradas compatibles
- **GIVEN** un hueco futuro de 30 minutos de un profesional, una entrada activa compatible, una de una prestación de 90 minutos, una con otro profesional preferido y una cancelada
- **WHEN** `RECEPCION` consulta las entradas compatibles con ese hueco
- **THEN** recibe `200` con solo la entrada compatible

#### Scenario: Las sugerencias se ordenan por antigüedad y muestran todas las entradas del paciente
- **GIVEN** tres entradas compatibles, dos del mismo paciente, creadas en momentos distintos
- **WHEN** `ADMIN` consulta las entradas compatibles
- **THEN** recibe las tres, de la más antigua a la más nueva

#### Scenario: Un hueco que ya empezó no tiene sugerencias
- **GIVEN** un hueco cuyo inicio ya pasó y una entrada que cumpliría la compatibilidad
- **WHEN** `RECEPCION` consulta las entradas compatibles
- **THEN** recibe `200` con una lista vacía

### Requirement: Validación y acceso de la consulta de compatibles

La consulta MUST responder en Problem Details `400` si falta un parámetro, si una fecha está mal formada o si `fin` no es posterior a `inicio`, y `422` con `PROFESIONAL_INEXISTENTE` o `PROFESIONAL_INACTIVO` si el profesional no existe o está inactivo. `ODONTOLOGO` MUST recibir `403` y un cliente sin sesión, `401`.

#### Scenario: La consulta rechaza un hueco inválido
- **WHEN** `RECEPCION` consulta sin `profesionalId`, sin `inicio` o sin `fin`, con una fecha mal formada, o con `fin` igual o anterior a `inicio`
- **THEN** recibe `400` en Problem Details

#### Scenario: La consulta rechaza un profesional inexistente o inactivo
- **WHEN** `RECEPCION` consulta un hueco de un profesional inexistente o inactivo
- **THEN** recibe `422` en Problem Details con `PROFESIONAL_INEXISTENTE` o `PROFESIONAL_INACTIVO`

#### Scenario: El odontólogo no consulta las entradas compatibles
- **WHEN** un odontólogo consulta las entradas compatibles con un hueco
- **THEN** recibe `403` en Problem Details

#### Scenario: Sin sesión no se consultan las entradas compatibles
- **WHEN** un cliente sin sesión consulta las entradas compatibles con un hueco
- **THEN** recibe `401` en Problem Details

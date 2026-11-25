# Spec Delta

## Purpose

Encontrar inicios disponibles para una prestación y profesional, respetando la agenda y proponiendo un sillón activo libre antes de crear el turno.

## ADDED Requirements

### Requirement: Intervalos disponibles

La consulta SHALL devolver inicios ordenados en grilla de cinco minutos, estrictamente posteriores a ahora, con el turno completo dentro de `[desde, hasta)` y de un único tramo en hora de Buenos Aires. La duración MUST ser múltiplo de cinco entre 5 y 480 minutos.

#### Scenario: Un hueco exacto admite la prestación
- **WHEN** quedan 30 minutos de atención y la prestación dura 30
- **THEN** se ofrece el inicio con fin igual al límite

#### Scenario: Una prestación mayor al hueco no se ofrece
- **WHEN** quedan 30 minutos y la prestación dura 45
- **THEN** no se ofrece ese hueco

#### Scenario: Los tramos no se unen para completar una prestación
- **WHEN** un turno necesitaría cruzar dos tramos contiguos
- **THEN** ese inicio no se ofrece

#### Scenario: La búsqueda redondea hacia la próxima grilla
- **WHEN** el rango empieza a las 09:01:00.001
- **THEN** el primer inicio posible es a las 09:05

#### Scenario: No se ofrecen inicios pasados ni iguales a ahora
- **WHEN** ahora cae dentro del rango
- **THEN** solo se ofrecen inicios estrictamente posteriores

#### Scenario: Una duración inválida se rechaza
- **WHEN** se pide una duración fuera de rango o no múltiplo de cinco
- **THEN** se responde 422 con RN-09

### Requirement: Ocupación y sillones

Cada inicio SHALL tener un único sillón activo libre, elegido por id ascendente. Los turnos no cancelados y los bloqueos MUST excluir solapamientos con bordes semiabiertos por profesional y por sillón; los cancelados MUST ignorarse. Sin sillones activos libres SHALL devolverse una lista vacía.

#### Scenario: El borde contiguo de un turno queda disponible
- **WHEN** un turno ocupa 09:00 a 09:30
- **THEN** se ofrece 09:30 y no 09:25 para el mismo profesional

#### Scenario: Un bloqueo excluye solapamientos y permite el borde
- **WHEN** el profesional está bloqueado de 09:00 a 09:30
- **THEN** se ofrece 09:30 y no 09:25

#### Scenario: Se elige el primer sillón libre para todo el intervalo
- **WHEN** el primer sillón está ocupado y el segundo libre
- **THEN** se propone el segundo sin duplicar inicios

#### Scenario: Los turnos cancelados no quitan disponibilidad
- **WHEN** hay un turno cancelado en el intervalo
- **THEN** el horario sigue disponible

#### Scenario: Sin sillones libres no se ofrecen horarios
- **WHEN** todos los sillones activos están ocupados o no hay sillones activos
- **THEN** la lista es vacía

### Requirement: Consulta interna y recursos

`GET /api/agenda/disponibilidad` SHALL recibir profesionalId, prestacionId, desde y hasta con zona; duracionMin es opcional y usa el valor de la prestación. Los recursos MUST existir, estar activos y relacionados. Rango inválido, mayor a 31 días o años locales fuera de 1 a 9999 MUST responder 422; campos ausentes o mal formados MUST responder 400. La salida SHALL contener solo inicio, fin y sillonId.

#### Scenario: Recepción consulta disponibilidad y crea un turno sugerido
- **WHEN** recepción consulta un rango libre y crea un turno con el inicio y sillón devueltos
- **THEN** la consulta devuelve 200 sin datos de pacientes y la creación devuelve 201

#### Scenario: Se puede consultar una duración ajustada
- **WHEN** se pide duracionMin diferente de la duración por defecto
- **THEN** los resultados usan la duración solicitada

#### Scenario: Recursos inexistentes o inactivos se rechazan
- **WHEN** el profesional o la prestación no existen o están inactivos
- **THEN** la respuesta es 422 con el código del recurso

#### Scenario: Un profesional sin la prestación se rechaza
- **WHEN** la prestación no está asignada al profesional
- **THEN** la respuesta es 422 con RN-11

#### Scenario: Un rango inválido se rechaza
- **WHEN** hasta no es posterior a desde o el rango supera 31 días
- **THEN** la respuesta es 422 con RANGO_DISPONIBILIDAD_INVALIDO

#### Scenario: Parámetros obligatorios o con formato inválido
- **WHEN** faltan campos, los ids no son positivos o las fechas no tienen zona válida
- **THEN** la respuesta es 400

#### Scenario: Fechas fuera del calendario soportado se rechazan
- **WHEN** se consulta con un año local fuera de 1 a 9999
- **THEN** se responde 422 con RANGO_DISPONIBILIDAD_INVALIDO antes de consultar ocupaciones

#### Scenario: La búsqueda respeta el día de Buenos Aires
- **WHEN** el rango UTC empieza en un día distinto de su fecha local
- **THEN** se usa el horario del día local

### Requirement: Acceso del personal

Solo ADMIN y RECEPCION SHALL consultar la API y la fachada de aplicación del personal. Con parámetros válidos, ODONTOLOGO MUST recibir 403 y sin sesión MUST responder 401. La validación de campos ausentes o mal formados precede a la autorización de la fachada. El núcleo SHALL poder reutilizarse desde los futuros flujos internos de reserva pública sin exigir una sesión del personal.

#### Scenario: El administrador consulta disponibilidad
- **WHEN** ADMIN consulta un rango válido
- **THEN** recibe 200

#### Scenario: El odontólogo no consulta disponibilidad
- **WHEN** ODONTOLOGO consulta con parámetros válidos por API o por la fachada del personal
- **THEN** recibe 403 o AccessDeniedException respectivamente

#### Scenario: Sin sesión no se consulta disponibilidad
- **WHEN** se llama la API sin sesión
- **THEN** responde 401

#### Scenario: El núcleo de disponibilidad se reutiliza sin sesión del personal
- **WHEN** un flujo interno consulta el núcleo sin sesión del personal
- **THEN** obtiene los horarios ordenados y sujetos a las mismas reglas de disponibilidad

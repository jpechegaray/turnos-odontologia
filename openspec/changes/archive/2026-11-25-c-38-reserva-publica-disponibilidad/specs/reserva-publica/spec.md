# Spec Delta

## Purpose

Permitir que un paciente anónimo consulte, sin sesión, las prestaciones y profesionales reservables y los horarios libres, respetando los límites de la reserva online y sin exponer datos internos.

## ADDED Requirements

### Requirement: Prestaciones reservables públicas

`GET /api/public/prestaciones` SHALL responder sin sesión las prestaciones activas que realiza al menos un profesional activo, con `id`, `nombre` y `duracionMin`, y ningún otro campo.

#### Scenario: Se listan solo prestaciones activas con profesional activo
- **WHEN** existen una prestación activa con profesional activo, una inactiva y una activa sin profesional activo
- **THEN** la respuesta 200 contiene solo la primera, con `id`, `nombre` y `duracionMin`

#### Scenario: Un profesional inactivo no hace reservable una prestación
- **WHEN** el único profesional de una prestación activa está inactivo
- **THEN** esa prestación no aparece

### Requirement: Profesionales reservables por prestación

`GET /api/public/profesionales?prestacionId=` SHALL responder sin sesión los profesionales activos que realizan la prestación, con `id`, `nombre`, `apellido` y `especialidad`, sin matrícula. Sin `prestacionId` o con uno mal formado MUST responder 400.

#### Scenario: Se listan profesionales activos de la prestación sin matrícula
- **WHEN** se consulta una prestación que realizan un profesional activo y uno inactivo
- **THEN** la respuesta 200 contiene solo el activo, sin el campo matrícula

#### Scenario: Una prestación inexistente o inactiva devuelve lista vacía
- **WHEN** `prestacionId` no existe o la prestación está inactiva
- **THEN** la respuesta es 200 con `[]`

#### Scenario: Falta el parámetro
- **WHEN** se omite `prestacionId`
- **THEN** la respuesta es 400

### Requirement: Disponibilidad pública

`GET /api/public/disponibilidad` SHALL aceptar `profesionalId`, `prestacionId`, `desde` y `hasta` (con zona) sin sesión y devolver los horarios libres como objetos con solo `inicio` y `fin`, usando la duración por defecto de la prestación. La respuesta MUST NOT incluir sillón ni datos de otros pacientes.

#### Scenario: Un horario ocupado no se ofrece
- **WHEN** un turno activo ocupa un horario del profesional dentro del rango
- **THEN** ese horario no aparece en la respuesta

#### Scenario: Un horario contiguo a un turno sí se ofrece
- **WHEN** un turno termina exactamente cuando empieza un horario libre
- **THEN** ese horario aparece en la respuesta

#### Scenario: El JSON tiene solo inicio y fin
- **WHEN** hay horarios libres
- **THEN** cada elemento tiene únicamente `inicio` y `fin`

#### Scenario: Parámetros ausentes o mal formados
- **WHEN** falta un parámetro o una fecha no tiene formato válido
- **THEN** la respuesta es 400

### Requirement: Rango pedido de la disponibilidad pública

El rango pedido SHALL tener fin posterior al inicio y abarcar como máximo 31 días; si no, la respuesta MUST ser 422 con código `RANGO_DISPONIBILIDAD_INVALIDO`. Esta validación se aplica al rango pedido, antes del recorte por RN-27.

#### Scenario: Fin no posterior al inicio
- **WHEN** `hasta` es igual o anterior a `desde`
- **THEN** la respuesta es 422 con código `RANGO_DISPONIBILIDAD_INVALIDO`

#### Scenario: Rango de más de 31 días
- **WHEN** el rango abarca más de 31 días
- **THEN** la respuesta es 422 con código `RANGO_DISPONIBILIDAD_INVALIDO`

#### Scenario: Rango válido fuera del horizonte no es un error de rango
- **WHEN** el rango pedido es válido pero queda por completo fuera del horizonte
- **THEN** la respuesta es 200 con `[]`

### Requirement: Límites de la reserva online en la disponibilidad

La disponibilidad pública SHALL recortar en silencio el rango a `[ahora + reserva_online.anticipacion_minima_horas, ahora + reserva_online.horizonte_dias]` (RN-27) y, si no queda rango, responder 200 con `[]`. La configuración MUST leerse en cada consulta.

#### Scenario: Borde de anticipación mínima
- **WHEN** hay horarios libres justo antes y justo en el límite de la anticipación mínima
- **THEN** solo se ofrecen los que empiezan en el límite o después

#### Scenario: Borde del horizonte
- **WHEN** hay horarios libres que terminan justo en el horizonte y otros que lo exceden
- **THEN** solo se ofrecen los que quedan dentro del horizonte

#### Scenario: Un cambio de configuración aplica a la siguiente consulta
- **WHEN** se modifica la anticipación mínima o el horizonte entre dos consultas
- **THEN** la segunda consulta usa los valores nuevos sin reiniciar

### Requirement: Recurso no disponible indistinguible

Si el profesional o la prestación no existe o está inactivo, o el profesional no realiza la prestación, `GET /api/public/disponibilidad` SHALL responder 404 con código `RECURSO_NO_DISPONIBLE` y el mismo mensaje en los tres casos. Los recursos se validan antes del recorte por RN-27.

#### Scenario: Los tres casos dan la misma respuesta
- **WHEN** se consulta con profesional inexistente o inactivo, con prestación inexistente o inactiva, o con un profesional que no realiza la prestación
- **THEN** la respuesta es 404 con código `RECURSO_NO_DISPONIBLE` e idéntico mensaje en todos los casos

#### Scenario: Un profesional inactivo con rango fuera del horizonte da 404
- **WHEN** el profesional está inactivo y el rango pedido queda fuera del horizonte
- **THEN** la respuesta es 404 y no `200 []`

#### Scenario: La consulta del personal conserva sus errores detallados
- **WHEN** el personal consulta `/api/agenda/disponibilidad` con un profesional inactivo o una prestación que no realiza
- **THEN** la respuesta sigue siendo 422 con el código específico de cada caso

### Requirement: Límite de tasa en los endpoints públicos de reserva

Los endpoints de `reserva-publica` SHALL estar sujetos al límite de tasa por IP de los endpoints públicos y responder 429 con `Retry-After` al superarlo.

#### Scenario: Se supera el límite de solicitudes
- **WHEN** una misma IP supera el máximo configurado de solicitudes a estos endpoints
- **THEN** la respuesta es 429 con la cabecera `Retry-After`

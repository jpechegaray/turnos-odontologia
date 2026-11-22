# Spec Delta

## Purpose

Registrar qué prestaciones del catálogo realiza cada profesional, para que la agenda y la reserva online solo ofrezcan y acepten combinaciones profesional-prestación válidas (RN-11, SU-09).

## ADDED Requirements

### Requirement: Consulta de las prestaciones de un profesional
`GET /api/profesionales/{id}/prestaciones` SHALL devolver `200` con la lista de prestaciones que realiza el profesional, cada una con `id`, `nombre`, `duracionDefaultMin` y `activa`, ordenada por nombre sin distinguir mayúsculas y, ante empate, por `id`. La lista MUST incluir las prestaciones asociadas inactivas con `activa` igual a `false`, y MUST estar vacía si el profesional no tiene prestaciones asociadas.

#### Scenario: Se listan las prestaciones del profesional ordenadas por nombre
- **GIVEN** un profesional asociado a las prestaciones "Limpieza" y "Consulta", y otro profesional asociado a "Extracción"
- **WHEN** un usuario `ADMIN` consulta las prestaciones del primero
- **THEN** recibe `200` con "Consulta" y "Limpieza", en ese orden, cada una con su `id`, `duracionDefaultMin` y `activa`
- **AND** la lista no incluye "Extracción"

#### Scenario: Un profesional sin prestaciones devuelve una lista vacía
- **GIVEN** un profesional sin prestaciones asociadas
- **WHEN** un usuario `ADMIN` consulta sus prestaciones
- **THEN** recibe `200` con una lista vacía

#### Scenario: Una prestación asociada que se dio de baja se muestra inactiva
- **GIVEN** un profesional asociado a "Blanqueamiento" y esa prestación dada de baja después
- **WHEN** un usuario `ADMIN` consulta sus prestaciones
- **THEN** la lista incluye "Blanqueamiento" con `activa` igual a `false`

### Requirement: Profesional de la ruta inexistente o inactivo
La consulta y el reemplazo de prestaciones SHALL responder `404` en formato Problem Details con código `PROFESIONAL_NO_ENCONTRADO` cuando el id de profesional de la ruta no existe, para los roles con acceso a cualquier profesional. Ambas operaciones MUST funcionar igual para un profesional inactivo.

#### Scenario: Consultar las prestaciones de un profesional inexistente responde 404
- **WHEN** un usuario `ADMIN` consulta las prestaciones de un id de profesional que no existe
- **THEN** recibe `404` en formato Problem Details con código `PROFESIONAL_NO_ENCONTRADO`

#### Scenario: Reemplazar las prestaciones de un profesional inexistente responde 404
- **WHEN** `ADMIN` envía `PUT` para un id de profesional que no existe
- **THEN** recibe `404` en formato Problem Details con código `PROFESIONAL_NO_ENCONTRADO`

#### Scenario: Las prestaciones de un profesional inactivo se pueden consultar y reemplazar
- **GIVEN** un profesional inactivo y la prestación activa "Consulta"
- **WHEN** `ADMIN` envía `PUT` con el id de "Consulta" y luego consulta sus prestaciones
- **THEN** ambas respuestas son `200` y contienen "Consulta"

### Requirement: Reemplazo del conjunto de prestaciones de un profesional
`PUT /api/profesionales/{id}/prestaciones` SHALL recibir `{"prestacionIds": [...]}` y reemplazar el conjunto completo de prestaciones del profesional por el enviado, asociando las que faltan y desasociando las que no figuran en una sola operación atómica. Una lista vacía MUST dejar al profesional sin prestaciones. La respuesta MUST ser `200` con el conjunto resultante en el mismo formato y orden que la consulta.

#### Scenario: El administrador asocia prestaciones a un profesional
- **GIVEN** un profesional sin prestaciones y las prestaciones activas "Consulta" y "Limpieza"
- **WHEN** `ADMIN` envía `PUT` con los ids de ambas
- **THEN** recibe `200` con "Consulta" y "Limpieza"
- **AND** una consulta posterior devuelve las mismas dos prestaciones

#### Scenario: El reemplazo desasocia las prestaciones que no se envían
- **GIVEN** un profesional asociado a "Consulta" y "Limpieza"
- **WHEN** `ADMIN` envía `PUT` solo con el id de "Limpieza" y el de la prestación activa "Extracción"
- **THEN** recibe `200` con "Extracción" y "Limpieza"
- **AND** "Consulta" deja de estar asociada al profesional

#### Scenario: Una lista vacía deja al profesional sin prestaciones
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** `ADMIN` envía `PUT` con `prestacionIds` vacío
- **THEN** recibe `200` con una lista vacía y el profesional queda sin prestaciones

### Requirement: Pareja profesional-prestación única en el reemplazo
Los ids repetidos en `prestacionIds` SHALL tratarse como uno solo, de modo que cada pareja profesional-prestación exista a lo sumo una vez.

#### Scenario: Los ids repetidos se asocian una sola vez
- **GIVEN** un profesional sin prestaciones y la prestación activa "Consulta"
- **WHEN** `ADMIN` envía `PUT` con el id de "Consulta" dos veces
- **THEN** recibe `200` con "Consulta" una sola vez

### Requirement: Cuerpo del reemplazo válido
Un cuerpo sin `prestacionIds`, con `prestacionIds` nulo o con algún elemento nulo SHALL responder `400` en formato Problem Details y MUST NOT cambiar las asociaciones existentes.

#### Scenario: Un cuerpo inválido responde 400 sin cambios
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** `ADMIN` envía `PUT` sin `prestacionIds`, con `prestacionIds` nulo o con un elemento nulo
- **THEN** recibe `400` en formato Problem Details
- **AND** el profesional sigue asociado solo a "Consulta"

### Requirement: Solo se asocian prestaciones existentes
Cada id enviado en el reemplazo SHALL corresponder a una prestación existente del catálogo. Si alguno no existe, la operación MUST responder `422` con código `PRESTACION_INEXISTENTE` y MUST NOT cambiar ninguna asociación.

#### Scenario: Asociar una prestación inexistente responde 422
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** `ADMIN` envía `PUT` con el id de "Consulta" y un id de prestación que no existe
- **THEN** recibe `422` con código `PRESTACION_INEXISTENTE`
- **AND** el profesional sigue asociado solo a "Consulta"

### Requirement: Prestaciones inactivas en el reemplazo
Una prestación inactiva SHALL NOT asociarse por primera vez: si el conjunto enviado incluye una prestación inactiva que el profesional no tenía asociada, la operación MUST responder `422` con código `PRESTACION_INACTIVA` sin cambiar ninguna asociación. Una prestación inactiva ya asociada MUST poder conservarse sin error, para no perder historial (RN-37).

#### Scenario: Asociar una prestación inactiva responde 422
- **GIVEN** un profesional asociado a "Consulta" y la prestación inactiva "Blanqueamiento" no asociada
- **WHEN** `ADMIN` envía `PUT` con los ids de "Consulta" y "Blanqueamiento"
- **THEN** recibe `422` con código `PRESTACION_INACTIVA`
- **AND** el profesional sigue asociado solo a "Consulta"

#### Scenario: Se puede conservar una prestación ya asociada que se dio de baja
- **GIVEN** un profesional asociado a "Consulta" y a "Blanqueamiento", y "Blanqueamiento" dada de baja después
- **WHEN** `ADMIN` envía `PUT` con los ids de "Consulta" y "Blanqueamiento"
- **THEN** recibe `200` con ambas, y "Blanqueamiento" con `activa` igual a `false`

### Requirement: Lectura de prestaciones de un profesional según el rol
`ADMIN` y `RECEPCION` SHALL poder consultar las prestaciones de cualquier profesional. `ODONTOLOGO` SHALL poder consultar solo las de su propio profesional, resuelto del lado servidor. La consulta de otro profesional, exista o no, o la de un odontólogo sin vínculo MUST responder `403` en formato Problem Details sin revelar datos.

#### Scenario: Recepción consulta las prestaciones de cualquier profesional
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** un usuario `RECEPCION` consulta sus prestaciones
- **THEN** recibe `200` con "Consulta"

#### Scenario: Un odontólogo vinculado consulta solo sus propias prestaciones
- **GIVEN** un `ODONTOLOGO` vinculado a un profesional asociado a "Consulta" y otro profesional distinto
- **WHEN** consulta sus propias prestaciones, las del otro profesional y las de un id inexistente
- **THEN** recibe `200` con "Consulta" para las propias y `403` en las otras dos consultas

#### Scenario: Un odontólogo sin vínculo no puede consultar prestaciones de profesionales
- **GIVEN** un `ODONTOLOGO` sin vínculo resuelto
- **WHEN** consulta las prestaciones de un profesional existente
- **THEN** recibe `403` en formato Problem Details

### Requirement: Solo el administrador reemplaza prestaciones
Solo `ADMIN` SHALL poder reemplazar las prestaciones de un profesional. `RECEPCION` y `ODONTOLOGO` MUST recibir `403` en formato Problem Details sin cambios persistidos (RN-20, RN-33).

#### Scenario: Recepción y odontología no pueden reemplazar las prestaciones
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** un usuario `RECEPCION` u `ODONTOLOGO` envía `PUT` con CSRF válido y otro conjunto de prestaciones
- **THEN** recibe `403` en formato Problem Details
- **AND** el profesional sigue asociado solo a "Consulta"

### Requirement: Sesión obligatoria
La consulta y el reemplazo de prestaciones de un profesional SHALL requerir sesión; un cliente sin autenticar MUST recibir `401` en formato Problem Details.

#### Scenario: Sin sesión las prestaciones de un profesional responden 401
- **WHEN** un cliente sin autenticar consulta o reemplaza las prestaciones de un profesional
- **THEN** recibe `401` en formato Problem Details

### Requirement: Consulta interna de si un profesional realiza una prestación
El módulo SHALL ofrecer a agenda y reserva pública una consulta de solo lectura que, dados un id de profesional y uno de prestación, informe si existe la asociación. La consulta MUST NOT exigir sesión y, ante ids inexistentes, MUST responder que no la realiza sin lanzar errores. Los demás módulos MUST usar esta consulta en lugar de acceder al almacenamiento de la asociación.

#### Scenario: La consulta interna confirma un profesional que realiza la prestación
- **GIVEN** un profesional asociado a "Consulta"
- **WHEN** otro módulo pregunta si ese profesional realiza "Consulta"
- **THEN** la respuesta es afirmativa

#### Scenario: La consulta interna niega un profesional que no realiza la prestación
- **GIVEN** un profesional asociado a "Consulta" y otro profesional asociado a "Limpieza"
- **WHEN** otro módulo pregunta si el primero realiza "Limpieza"
- **THEN** la respuesta es negativa

#### Scenario: La consulta interna niega ids inexistentes sin error
- **WHEN** otro módulo pregunta por un profesional o una prestación que no existen
- **THEN** la respuesta es negativa y no se lanza ningún error

### Requirement: La consulta interna no evalúa la actividad
La consulta interna SHALL responder solo por la existencia de la asociación y MUST NOT evaluar si el profesional o la prestación están activos: cada consumidor valida la actividad por separado (RN-11).

#### Scenario: La consulta interna no depende de que la prestación esté activa
- **GIVEN** un profesional asociado a "Blanqueamiento" y esa prestación dada de baja después
- **WHEN** otro módulo pregunta si ese profesional realiza "Blanqueamiento"
- **THEN** la respuesta es afirmativa

### Requirement: Integridad de la asociación en la base de datos
La base de datos SHALL garantizar que cada pareja profesional-prestación aparezca a lo sumo una vez y que solo referencie profesionales y prestaciones existentes, aunque una escritura eluda la validación de la aplicación.

#### Scenario: La base rechaza una pareja profesional-prestación duplicada
- **GIVEN** un profesional ya asociado a "Consulta"
- **WHEN** se intenta insertar directamente en la base la misma pareja
- **THEN** la base rechaza la inserción por violación de unicidad

#### Scenario: La base rechaza una asociación con un profesional o una prestación inexistentes
- **WHEN** se intenta insertar directamente en la base una asociación con un id de profesional o de prestación que no existe
- **THEN** la base rechaza la inserción por violación de clave foránea

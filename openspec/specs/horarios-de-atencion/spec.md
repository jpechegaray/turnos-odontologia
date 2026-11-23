# horarios-de-atencion Specification

## Purpose
Definir los tramos semanales en los que atiende cada profesional y permitir que la agenda determine si un intervalo candidato cabe completo en uno de ellos, calculado en la zona horaria del consultorio.

## Requirements

### Requirement: Alta y listado de tramos de atención por profesional

`ADMIN` y `RECEPCION` SHALL poder crear (`POST`) y listar (`GET`) tramos en `/api/profesionales/{id}/horarios` para cualquier profesional existente. El alta MUST recibir `diaSemana` de 1 a 7 (ISO) y `horaInicio` y `horaFin` en `HH:mm`, con fin posterior al inicio; devuelve `201`. El listado devuelve `200` ordenado por día, hora de inicio e `id`. Una entrada inválida MUST devolver `400` en Problem Details sin persistir.

#### Scenario: Administración y recepción crean y listan tramos del profesional solicitado
- **GIVEN** dos profesionales existentes con tramos registrados
- **WHEN** `ADMIN` o `RECEPCION` crea un tramo válido con CSRF válido y lista los tramos de ese profesional
- **THEN** el alta devuelve `201` con ID generado y el listado `200` incluye solo tramos del profesional, ordenados por día, hora de inicio e `id`, cada uno con `id`, `profesionalId`, `diaSemana`, `horaInicio` y `horaFin` en formato `HH:mm`

#### Scenario: Un profesional sin tramos devuelve un listado vacío
- **WHEN** `ADMIN` o `RECEPCION` lista los tramos de un profesional existente sin registros
- **THEN** recibe `200` con `[]`

#### Scenario: El alta rechaza días u horas inválidos
- **WHEN** un usuario autorizado envía un alta con CSRF válido y falta un campo, el día está fuera de 1 a 7, una hora no respeta `HH:mm` o el fin es igual o anterior al inicio
- **THEN** recibe `400` en Problem Details y no se crea ningún tramo

#### Scenario: Gestionar tramos de un profesional inexistente devuelve 404
- **WHEN** `ADMIN` o `RECEPCION` lista, crea, modifica o elimina tramos de un profesional inexistente, con solicitudes por lo demás válidas
- **THEN** recibe `404` en Problem Details con código `PROFESIONAL_NO_ENCONTRADO`

### Requirement: Tramos del mismo día sin superposición

Los tramos de un mismo profesional y día SHALL NOT superponerse. Los tramos son semiabiertos `[horaInicio, horaFin)`, así que dos tramos se superponen únicamente si `inicioA < finB` y `finA > inicioB`. Un alta o una modificación que superponga un tramo con otro existente del mismo profesional y día MUST responder `409` en Problem Details con código `HORARIO_SUPERPUESTO`, sin persistir cambios. Los tramos de otros días o de otros profesionales no se comparan (RN-16).

#### Scenario: Se aceptan varios tramos contiguos o separados en el mismo día
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** un usuario autorizado agrega los tramos del lunes de 13:00 a 15:00 y de 16:00 a 20:00
- **THEN** ambas altas devuelven `201` y el lunes queda con tres tramos

#### Scenario: Un tramo superpuesto en el mismo día se rechaza
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** un usuario autorizado intenta agregar el tramo del lunes de 12:00 a 14:00 o de 10:00 a 11:00
- **THEN** recibe `409` con código `HORARIO_SUPERPUESTO` y el lunes conserva solo el tramo original

#### Scenario: Los tramos de otro día o de otro profesional no generan conflicto
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** se agrega el martes de 09:00 a 13:00 para ese profesional, o el lunes de 09:00 a 13:00 para otro profesional
- **THEN** ambas altas devuelven `201`

### Requirement: Modificación y eliminación de un tramo identificado por profesional

En `/api/profesionales/{id}/horarios/{horarioId}`, `ADMIN` y `RECEPCION` SHALL poder reemplazar un tramo con `PUT` (mismas validaciones del alta, excluyendo al propio tramo de la superposición; devuelve `200`) y eliminarlo con `DELETE` (devuelve `204` sin cuerpo). Si el tramo no existe o es de otro profesional, ambas MUST responder `404` con código `HORARIO_NO_ENCONTRADO` sin modificar registros.

#### Scenario: Modificar un tramo dentro de su propio rango se acepta
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** un usuario autorizado lo modifica a lunes de 10:00 a 14:00
- **THEN** recibe `200` con los nuevos valores y el listado refleja el cambio

#### Scenario: Modificar un tramo para superponerlo con otro se rechaza
- **GIVEN** un profesional con tramos el lunes de 09:00 a 13:00 y de 14:00 a 18:00
- **WHEN** un usuario autorizado modifica el segundo a lunes de 12:00 a 18:00
- **THEN** recibe `409` con código `HORARIO_SUPERPUESTO` y el tramo conserva sus valores

#### Scenario: Eliminar un tramo lo retira del listado y de la consulta de contención
- **GIVEN** un único tramo de un profesional el lunes de 09:00 a 13:00
- **WHEN** `ADMIN` o `RECEPCION` lo elimina con CSRF válido
- **THEN** recibe `204` sin cuerpo, el tramo no aparece en el listado y un candidato del lunes de 10:00 a 10:30 ya no queda contenido

#### Scenario: Un tramo ausente o de otro profesional no se puede modificar ni eliminar
- **GIVEN** un profesional existente
- **WHEN** un usuario autorizado intenta modificar o eliminar un ID de tramo inexistente o perteneciente a otro profesional
- **THEN** recibe `404` con código `HORARIO_NO_ENCONTRADO` y no cambia ningún registro

### Requirement: Acceso a horarios según rol y titularidad

Las rutas SHALL requerir sesión del personal. `ODONTOLOGO` SHALL poder listar únicamente los tramos de su propio profesional, identificado por un vínculo resuelto en el servidor. Sin vínculo o ante un identificador ajeno, MUST recibir `403` antes de comprobar si el profesional solicitado existe. Para `POST`, `PUT` y `DELETE` MUST recibir `403` aun con CSRF válido y con cuerpo malformado. Las escrituras de `ADMIN` y `RECEPCION` MUST exigir CSRF válido (RN-33).

#### Scenario: El odontólogo vinculado lista sus tramos pero no los ajenos
- **GIVEN** un odontólogo vinculado y tramos de más de un profesional
- **WHEN** consulta su listado, el de un tercero y el de un ID inexistente
- **THEN** recibe `200` con sus tramos para su propio ID y `403` en Problem Details para los otros dos

#### Scenario: El odontólogo sin vínculo no puede listar tramos
- **WHEN** un odontólogo sin vínculo resuelto solicita el listado de tramos
- **THEN** recibe `403` en Problem Details sin datos de tramos

#### Scenario: El odontólogo no puede crear, modificar ni eliminar tramos
- **WHEN** un odontólogo vinculado envía `POST`, `PUT` o `DELETE` a sus tramos con CSRF válido
- **THEN** recibe `403` en Problem Details y los registros no cambian

#### Scenario: Las escrituras de tramos requieren CSRF válido
- **WHEN** `ADMIN` o `RECEPCION` con sesión intenta crear, modificar o eliminar un tramo sin CSRF válido
- **THEN** recibe `403` en Problem Details y los registros no cambian

#### Scenario: Consultar o escribir tramos sin sesión devuelve 401
- **WHEN** un cliente sin sesión intenta listar, crear, modificar o eliminar un tramo
- **THEN** recibe `401` en Problem Details y no se modifican registros

### Requirement: Consulta de contención en un único tramo

La consulta interna de agenda SHALL indicar si un intervalo candidato `[inicio, fin)` cabe completo en un único tramo del profesional, con ambos extremos en hora local de `America/Argentina/Buenos_Aires`. Está contenido solo si los extremos caen en la misma fecha local y un tramo de ese día cumple `horaInicio <= inicioLocal` y `finLocal <= horaFin`. Cruzar un corte entre tramos o la medianoche MUST NOT quedar contenido (RN-04, RN-14, RN-16).

#### Scenario: Un turno que termina justo al cierre del tramo queda contenido
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** se consulta un candidato de un lunes de 12:30 a 13:00, hora de Buenos Aires
- **THEN** la consulta indica que está contenido

#### Scenario: Un turno que excede el cierre del tramo no queda contenido
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** se consulta un candidato de un lunes de 12:45 a 13:15, hora de Buenos Aires
- **THEN** la consulta indica que no está contenido

#### Scenario: Un turno que cruza el corte entre dos tramos no queda contenido
- **GIVEN** un profesional con tramos el lunes de 09:00 a 13:00 y de 13:00 a 17:00
- **WHEN** se consulta un candidato de un lunes de 12:45 a 13:15
- **THEN** la consulta indica que no está contenido

#### Scenario: Un turno en otro día o de otro profesional no queda contenido
- **GIVEN** un profesional con un tramo solo el lunes de 09:00 a 13:00
- **WHEN** se consulta un candidato de un martes de 10:00 a 10:30 para ese profesional, o de un lunes de 10:00 a 10:30 para otro profesional sin tramos
- **THEN** la consulta indica que no está contenido en ninguno de los casos

#### Scenario: La contención se calcula en hora de Buenos Aires
- **GIVEN** un profesional con un tramo el lunes de 09:00 a 13:00
- **WHEN** se consulta un candidato expresado en UTC de un lunes de 12:00Z a 12:30Z, que equivale a 09:00 a 09:30 en Buenos Aires
- **THEN** la consulta indica que está contenido

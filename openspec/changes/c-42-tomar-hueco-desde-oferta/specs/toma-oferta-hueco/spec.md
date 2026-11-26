## ADDED Requirements

### Requirement: Consulta pública de una oferta por su token

El sistema SHALL exponer `GET /api/public/ofertas/{token}` sin sesión, que encuentra la oferta comparando el hash SHA-256 del token en tiempo constante (RN-38). La respuesta `200` MUST incluir el inicio y el fin del turno que se crearía (inicio del hueco más la duración por defecto de la prestación de la entrada), el nombre del profesional, el de la prestación y el estado visible. MUST NOT incluir datos de pacientes ni ids internos, y la consulta MUST NOT cambiar el estado de la oferta (RN-07).

#### Scenario: Se consulta una oferta disponible
- **GIVEN** una oferta `ENVIADA` para un hueco del lunes a las 10:00 en Buenos Aires con una prestación de 30 minutos por defecto, y la entrada `ACTIVA`
- **WHEN** se consulta `GET /api/public/ofertas/{token}` con el token del enlace
- **THEN** la respuesta es `200` con estado `DISPONIBLE`, inicio el lunes a las 10:00, fin a las 10:30, el nombre del profesional y el de la prestación
- **AND** la respuesta no contiene nombre, email ni teléfono del paciente

### Requirement: Estado visible de la oferta

El estado visible SHALL ser:
- `TOMADA` si la oferta está `TOMADA`;
- `VENCIDA` si está `VENCIDA` o si el hueco ya empezó, aunque el vencimiento programado no la haya marcado;
- `NO_DISPONIBLE` si la entrada ya no está `ACTIVA`;
- `DISPONIBLE` en cualquier otro caso.

#### Scenario: Una oferta tomada informa que el hueco ya fue tomado
- **GIVEN** una oferta en estado `TOMADA`
- **WHEN** se la consulta con su token
- **THEN** la respuesta es `200` con estado `TOMADA`

#### Scenario: Una oferta cuyo hueco ya empezó informa que venció
- **GIVEN** una oferta `ENVIADA` cuyo hueco empezó hace un minuto y que el vencimiento programado todavía no procesó
- **WHEN** se la consulta con su token
- **THEN** la respuesta es `200` con estado `VENCIDA`

#### Scenario: Una oferta de una entrada que ya no está activa no está disponible
- **GIVEN** una oferta `ENVIADA` vigente cuya entrada fue cancelada por recepción
- **WHEN** se la consulta con su token
- **THEN** la respuesta es `200` con estado `NO_DISPONIBLE`

### Requirement: Rechazo uniforme de tokens de oferta inválidos

Ante un token de oferta inexistente o mal formado, `GET /api/public/ofertas/{token}` y `POST /api/public/ofertas/{token}/tomar` MUST responder `404` con el mismo cuerpo, sin distinguir la causa (RN-38). Un token de turno MUST NOT servir como token de oferta.

#### Scenario: Tokens inválidos responden el mismo 404
- **GIVEN** un token inexistente con formato válido, un token mal formado y el token de un turno
- **WHEN** se consulta y se intenta tomar la oferta con cada uno
- **THEN** todas las respuestas son `404` con el mismo código y el mismo mensaje
- **AND** no se crea ningún turno

### Requirement: Tomar el hueco ofrecido

El sistema SHALL exponer `POST /api/public/ofertas/{token}/tomar` sin sesión ni cuerpo (RN-38). Para una oferta disponible MUST crear un turno `PROGRAMADO` y `ONLINE` con el paciente y la prestación de la entrada, el profesional del hueco, inicio en el inicio del hueco y la duración por defecto de la prestación (RN-24). MUST responder `201` con los datos del turno y su token en claro, mostrado una única vez (RN-25).

#### Scenario: El paciente toma un hueco disponible
- **GIVEN** una oferta disponible para un hueco del lunes de 10:00 a 11:00, con una prestación de 30 minutos por defecto y el sillón sugerido libre
- **WHEN** se envía `POST /api/public/ofertas/{token}/tomar`
- **THEN** la respuesta es `201` con un turno `PROGRAMADO` del lunes de 10:00 a 10:30 y un token de turno de 43 caracteres
- **AND** el turno guardado tiene canal `ONLINE`, el paciente y la prestación de la entrada, el profesional del hueco y el sillón sugerido
- **AND** la entrada queda `ASIGNADA` con ese turno y la oferta queda `TOMADA`

### Requirement: Sillón del turno tomado

El turno tomado SHALL usar el sillón sugerido de la oferta si sigue activo y libre; si no, MUST usar el primer sillón activo libre por orden de id (SU-14). Si el hueco es más largo que la prestación, el resto queda libre sin un nuevo aviso.

#### Scenario: Si el sillón sugerido está ocupado se usa el primero activo libre
- **GIVEN** una oferta disponible cuyo sillón sugerido ya tiene un turno en ese horario, y otros dos sillones activos libres
- **WHEN** se toma la oferta
- **THEN** el turno se crea en el sillón libre de menor id

### Requirement: Efectos de la toma sobre la entrada y las ofertas

En la misma transacción que crea el turno, la entrada MUST pasar a `ASIGNADA` con el turno registrado, y la oferta y todas las demás ofertas `ENVIADA` del mismo profesional e intervalo MUST pasar a `TOMADA` (RN-24).

#### Scenario: Las demás ofertas del hueco pasan a tomadas
- **GIVEN** tres ofertas `ENVIADA` para el mismo hueco, enviadas a tres entradas distintas
- **WHEN** el paciente de una de ellas toma el hueco
- **THEN** las tres ofertas quedan `TOMADA`
- **AND** consultar cualquiera de las otras dos responde estado `TOMADA`

### Requirement: Solo uno se queda con el hueco ante confirmaciones simultáneas

Si varias confirmaciones del mismo hueco llegan a la vez, o si el hueco ya lo ocupó recepción o una reserva online, solo una operación SHALL crear el turno. Las demás MUST responder `409` con código `HUECO_YA_TOMADO` (RN-15, RN-24), con la restricción de exclusión de la base como garantía final. La oferta rechazada MUST quedar `TOMADA` y su entrada `ACTIVA`.

#### Scenario: Dos pacientes confirman a la vez y solo uno obtiene el turno
- **GIVEN** dos ofertas disponibles del mismo hueco para dos entradas distintas
- **WHEN** los dos pacientes envían `POST /api/public/ofertas/{token}/tomar` al mismo tiempo
- **THEN** una respuesta es `201` y la otra `409` con código `HUECO_YA_TOMADO`
- **AND** en la base queda un solo turno no cancelado para ese profesional en ese horario
- **AND** solo la entrada ganadora queda `ASIGNADA`; la otra sigue `ACTIVA`

#### Scenario: El hueco lo ocupó recepción antes de que el paciente confirme
- **GIVEN** una oferta disponible y un turno creado después por recepción para el mismo profesional en ese horario
- **WHEN** el paciente toma la oferta
- **THEN** la respuesta es `409` con código `HUECO_YA_TOMADO`
- **AND** la oferta queda `TOMADA` y la entrada sigue `ACTIVA`

#### Scenario: Volver a tomar una oferta ya tomada
- **GIVEN** una oferta que el mismo paciente ya tomó
- **WHEN** se vuelve a enviar `POST /api/public/ofertas/{token}/tomar`
- **THEN** la respuesta es `409` con código `HUECO_YA_TOMADO`
- **AND** no se crea un segundo turno

### Requirement: Oferta vencida al tomarla

Tomar una oferta `VENCIDA` o cuyo hueco ya empezó MUST responder `409` con código `OFERTA_VENCIDA` sin crear el turno. Si la oferta seguía `ENVIADA`, MUST quedar `VENCIDA` (RN-24, RN-38). La anticipación mínima de la reserva online (RN-27) MUST NOT aplicarse.

#### Scenario: Tomar una oferta cuyo hueco ya empezó
- **GIVEN** una oferta `ENVIADA` cuyo hueco empezó hace un minuto
- **WHEN** se intenta tomarla
- **THEN** la respuesta es `409` con código `OFERTA_VENCIDA`
- **AND** no se crea ningún turno y la oferta queda `VENCIDA`

#### Scenario: Se puede tomar un hueco que empieza en pocos minutos
- **GIVEN** una oferta disponible cuyo hueco empieza dentro de 10 minutos, menos que la anticipación mínima de la reserva online
- **WHEN** se toma la oferta
- **THEN** la respuesta es `201`

### Requirement: Oferta no disponible al tomarla

Tomar una oferta MUST responder `409` con código `OFERTA_NO_DISPONIBLE`, sin crear el turno y sin cambiar la entrada, en estos casos:
- la entrada ya no está `ACTIVA`;
- el hueco dejó de ser asignable por las reglas del alta: fuera de horario, bloqueo, recursos inactivos o ningún sillón activo libre.

#### Scenario: La entrada fue cancelada antes de tomar la oferta
- **GIVEN** una oferta `ENVIADA` vigente cuya entrada está `CANCELADA`
- **WHEN** se intenta tomarla
- **THEN** la respuesta es `409` con código `OFERTA_NO_DISPONIBLE`
- **AND** no se crea ningún turno

#### Scenario: El profesional bloqueó la agenda en el horario del hueco
- **GIVEN** una oferta disponible y un bloqueo de agenda del profesional creado después sobre el horario del hueco
- **WHEN** se intenta tomarla
- **THEN** la respuesta es `409` con código `OFERTA_NO_DISPONIBLE`
- **AND** la entrada sigue `ACTIVA`

### Requirement: Email de confirmación del turno tomado

Al tomar el hueco, el sistema SHALL emitir el token del turno nuevo (RN-25) y enviar al paciente un email `CONFIRMACION_RESERVA` con la fecha y hora, el profesional, la prestación y el enlace `{APP_PUBLIC_URL}/turno/{token}` (RN-31). El email MUST enviarse después de confirmar la transacción. Si falla, el turno MUST conservarse y la notificación MUST quedar `FALLIDA` (RN-30).

#### Scenario: El paciente recibe el enlace de su nuevo turno
- **GIVEN** una oferta disponible de un paciente con email
- **WHEN** el paciente toma la oferta
- **THEN** se envía un email `CONFIRMACION_RESERVA` a su dirección, con un enlace `/turno/{token}` cuyo token permite consultar el turno nuevo
- **AND** el registro de notificaciones tiene la confirmación `ENVIADA` asociada al turno

#### Scenario: Una falla de correo no pierde el turno
- **GIVEN** una oferta disponible y el servidor de correo rechazando los envíos
- **WHEN** el paciente toma la oferta
- **THEN** la respuesta es `201` y el turno queda guardado
- **AND** la notificación `CONFIRMACION_RESERVA` queda `FALLIDA` con el error registrado

### Requirement: Protección del token de la oferta en la ruta

Las respuestas de `/api/public/ofertas/{token}` y de sus subrutas, incluidos los errores, MUST llevar `Cache-Control: no-store`, y los cuerpos de error MUST NOT incluir la ruta con el token. Ni el token de la oferta ni el del turno nuevo MUST aparecer en los logs (RN-38, RN-25).

#### Scenario: Las respuestas no se guardan en caché
- **WHEN** se consulta una oferta, se la toma y se consulta un token inexistente
- **THEN** las tres respuestas tienen `Cache-Control: no-store`
- **AND** el cuerpo del `404` no contiene el token consultado

#### Scenario: Los tokens no aparecen en los logs
- **GIVEN** una oferta disponible
- **WHEN** se la consulta y se la toma con éxito
- **THEN** ni el token de la oferta ni el token del turno nuevo aparecen en la salida de logs

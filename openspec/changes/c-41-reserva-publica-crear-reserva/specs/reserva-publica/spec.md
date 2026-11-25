# Spec Delta

## ADDED Requirements

### Requirement: Datos de la reserva online

`POST /api/public/reservas` SHALL ser anónimo y recibir prestación, profesional, inicio, nombre, apellido, email, teléfono, obra social (texto libre), `aceptaTratamientoDatos` y `versionConsentimiento`. MUST NOT pedir DNI (RN-26). Un cuerpo mal formado, un dato faltante o en blanco o un email inválido SHALL responder 400 con el formato uniforme de validación, sin crear turno ni ficha. La aceptación y la versión no siguen esta regla: se validan como consentimiento.

#### Scenario: Una reserva válida responde 201 con el turno y el token
- **GIVEN** un horario libre que la disponibilidad pública ofrece para un profesional y una prestación activos
- **WHEN** un paciente anónimo envía la reserva con todos los datos obligatorios, la aceptación y la versión vigente
- **THEN** la respuesta es 201 con el inicio, el fin, la prestación, el profesional y el token del turno, y el turno queda `PROGRAMADO` con canal `ONLINE`

#### Scenario: Una reserva sin email o sin obra social responde 400
- **WHEN** un paciente envía la reserva sin email, sin teléfono, sin obra social o con el nombre en blanco
- **THEN** la respuesta es 400 y no se crea ningún turno ni ninguna ficha

#### Scenario: La reserva no pide ni guarda DNI
- **WHEN** un paciente nuevo reserva con todos los datos obligatorios
- **THEN** la ficha creada no tiene DNI y la reserva se acepta sin ese dato

### Requirement: Consentimiento obligatorio con la versión vigente

La reserva SHALL exigir `aceptaTratamientoDatos: true` y la versión vigente del texto de consentimiento (`2026-11-v1`). Si falta la aceptación o es `false`, MUST responder 422 con código `RN-26`. Si la versión no es la vigente, MUST responder 422 con código `CONSENTIMIENTO_VERSION_NO_VIGENTE`. En ambos casos no se crea turno, ficha ni consentimiento.

#### Scenario: Reservar sin aceptar el tratamiento de datos responde 422
- **WHEN** un paciente envía la reserva con `aceptaTratamientoDatos` en `false` o sin ese campo
- **THEN** la respuesta es 422 con código `RN-26` y no se crea turno ni ficha

#### Scenario: Reservar con una versión de consentimiento que no es la vigente responde 422
- **WHEN** un paciente envía la reserva aceptando el tratamiento de datos con una versión distinta de `2026-11-v1`
- **THEN** la respuesta es 422 con código `CONSENTIMIENTO_VERSION_NO_VIGENTE` y no se crea turno ni ficha

### Requirement: Registro del consentimiento

El sistema SHALL registrar el instante (reloj del sistema) y la versión del consentimiento de cada reserva aceptada. Si se crea una ficha nueva, MUST guardarlos en la ficha. Si se reutiliza una ficha existente, MUST guardarlos ligados al turno creado y MUST NOT modificar el consentimiento de la ficha.

#### Scenario: Reservar con una ficha nueva guarda el consentimiento en la ficha
- **GIVEN** que no existe ninguna ficha activa con el email de la reserva
- **WHEN** el paciente reserva aceptando el tratamiento de datos en el instante T con la versión vigente
- **THEN** la ficha nueva tiene `consentimientoDatosEn` igual a T y la versión `2026-11-v1`

#### Scenario: Reservar con una ficha existente guarda el consentimiento en el turno
- **GIVEN** una ficha activa con el email de la reserva y sin consentimiento registrado
- **WHEN** el paciente reserva aceptando el tratamiento de datos en el instante T con la versión vigente
- **THEN** el turno creado tiene registrado el consentimiento con T y `2026-11-v1`, y la ficha sigue sin consentimiento

### Requirement: Deduplicación del paciente sin modificar la ficha

La reserva SHALL buscar las fichas activas con el mismo email, sin distinguir mayúsculas ni espacios en los extremos (RN-28). Con una sola, la reutiliza. Con varias, elige la que coincide en teléfono (solo dígitos) y, si ninguna coincide, la más antigua. Sin coincidencias, crea una ficha nueva. Una ficha reutilizada MUST NOT cambiar ninguno de sus datos.

#### Scenario: Una reserva con una ficha existente no modifica la ficha
- **GIVEN** una ficha activa con email `ana.ficticia@example.com`, otro nombre, otro teléfono y otra obra social
- **WHEN** se reserva con el email `ANA.Ficticia@example.com` y datos distintos
- **THEN** el turno queda asociado a esa ficha, no se crea otra y todos los datos de la ficha quedan iguales

#### Scenario: Con varias fichas del mismo email se elige la que coincide en teléfono
- **GIVEN** dos fichas activas con el mismo email y teléfonos distintos
- **WHEN** se reserva con ese email y el teléfono de la segunda escrito con espacios y guiones
- **THEN** el turno queda asociado a la segunda ficha

#### Scenario: Con varias fichas del mismo email y ningún teléfono coincidente se elige la más antigua
- **GIVEN** dos fichas activas con el mismo email y teléfonos que no coinciden con el de la reserva
- **WHEN** se reserva con ese email
- **THEN** el turno queda asociado a la ficha creada primero y no se crea otra

#### Scenario: Un email nuevo crea una ficha con los datos de la reserva
- **GIVEN** que no existe ninguna ficha activa con el email de la reserva, aunque exista una dada de baja
- **WHEN** se reserva con ese email
- **THEN** se crea una ficha activa con nombre, apellido, email, teléfono y obra social de la reserva

#### Scenario: La respuesta no revela si la ficha ya existía
- **WHEN** se reserva una vez con un email existente y otra vez con un email nuevo
- **THEN** las dos respuestas 201 tienen los mismos campos y ninguna incluye datos de la ficha

### Requirement: Límites de la reserva online al reservar

La reserva SHALL rechazar con 422 y código `RN-27` un inicio anterior a la anticipación mínima o un turno cuyo fin queda después del horizonte vigente (`ahora + horizonte_dias`), con los bordes de la disponibilidad pública. También SHALL rechazarla cuando el email ya tiene tantos turnos futuros `PROGRAMADO` o `CONFIRMADO`, en todas sus fichas activas, como el tope configurado. La propiedad `limite` indica `ANTICIPACION`, `HORIZONTE` o `TOPE_EMAIL`.

#### Scenario: Se supera el tope de turnos activos por email
- **GIVEN** un tope de 3 y un email con 3 turnos futuros activos
- **WHEN** se reserva otro turno con ese email
- **THEN** la respuesta es 422 con código `RN-27` y `limite` `TOPE_EMAIL`, y no se crea el turno

#### Scenario: Los turnos cancelados o pasados no cuentan para el tope
- **GIVEN** un tope de 3 y un email con 2 turnos futuros activos, uno cancelado y uno ya pasado
- **WHEN** se reserva otro turno con ese email
- **THEN** la respuesta es 201

#### Scenario: Dos reservas simultáneas del mismo email no superan el tope
- **GIVEN** un tope de 3 y un email con 2 turnos futuros activos
- **WHEN** llegan al mismo tiempo dos reservas con ese email para horarios libres distintos
- **THEN** una responde 201 y la otra 422 con `limite` `TOPE_EMAIL`

#### Scenario: Un inicio antes de la anticipación mínima responde 422
- **GIVEN** una anticipación mínima de 2 horas
- **WHEN** se reserva un horario que empieza dentro de 1 hora
- **THEN** la respuesta es 422 con código `RN-27` y `limite` `ANTICIPACION`

#### Scenario: Un inicio después del horizonte responde 422
- **GIVEN** un horizonte de 60 días
- **WHEN** se reserva un horario que empieza dentro de 61 días
- **THEN** la respuesta es 422 con código `RN-27` y `limite` `HORIZONTE`

#### Scenario: Un horario ofrecido en el borde del horizonte se puede reservar
- **GIVEN** un horizonte de 60 días y el último horario que la disponibilidad pública ofrece antes de ese borde
- **WHEN** se reserva ese horario
- **THEN** la respuesta es 201

### Requirement: Recursos no disponibles en la reserva

La reserva SHALL responder el mismo 404 `RECURSO_NO_DISPONIBLE` de la disponibilidad pública cuando el profesional o la prestación no existen, están inactivos o el profesional no realiza la prestación, sin revelar la causa.

#### Scenario: Reservar con un profesional inactivo responde el 404 uniforme
- **WHEN** se reserva con un profesional inactivo, una prestación inexistente o una prestación que el profesional no realiza
- **THEN** la respuesta es 404 con código `RECURSO_NO_DISPONIBLE` en los tres casos

### Requirement: Turno creado con las reglas de la agenda

La reserva SHALL crear el turno con las mismas reglas que el alta del personal (RN-01 a RN-04, RN-09, RN-11, RN-13), con canal `ONLINE`, la duración por defecto de la prestación y sin usuario creador. El paciente MUST NOT elegir sillón: el sistema asigna el primer sillón activo libre en ese intervalo (SU-03).

#### Scenario: El sistema asigna el primer sillón activo libre
- **GIVEN** dos sillones activos y el primero ocupado en el horario pedido
- **WHEN** se reserva ese horario
- **THEN** el turno queda en el segundo sillón, con la duración por defecto de la prestación

#### Scenario: Un horario fuera de la atención del profesional responde 422
- **WHEN** se reserva un inicio fuera del horario de atención del profesional
- **THEN** la respuesta es 422 con el mismo código que da el alta del personal y no se crea el turno

### Requirement: Hueco ocupado con alternativas

Si el horario ya no está libre al reservar, por validación o por la restricción de la base, la reserva SHALL responder 409 con código `RN-01` y una propiedad `alternativas` con hasta 5 horarios libres del mismo profesional y prestación posteriores al pedido, dentro de los límites de la reserva online. MUST NOT indicar qué recurso chocó ni datos de otros turnos.

#### Scenario: Dos reservas simultáneas del mismo hueco: una responde 201 y la otra 409
- **GIVEN** un horario libre con un solo sillón activo
- **WHEN** dos pacientes con emails distintos envían al mismo tiempo la reserva de ese horario
- **THEN** una responde 201, la otra responde 409 con código `RN-01` y existe un único turno en ese horario

#### Scenario: Un hueco ocupado responde 409 con alternativas
- **GIVEN** un horario que se ocupó después de consultar la disponibilidad y otros horarios libres más tarde
- **WHEN** se reserva el horario ocupado
- **THEN** la respuesta es 409 con hasta 5 `alternativas` posteriores, cada una con inicio y fin, sin sillón ni la propiedad `recurso`

#### Scenario: Un 409 sin horarios libres devuelve alternativas vacías
- **GIVEN** un horario ocupado y ningún otro horario libre dentro del horizonte
- **WHEN** se reserva el horario ocupado
- **THEN** la respuesta es 409 con `alternativas` vacía

### Requirement: Token del turno reservado

Al crear la reserva, el sistema SHALL emitir el token del turno con las reglas de RN-25 y devolverlo en claro solo en la respuesta 201 y en el enlace del email. MUST NOT guardarlo en claro ni registrarlo en los logs.

#### Scenario: El token de la respuesta da acceso al turno reservado
- **WHEN** se crea una reserva y se consulta `GET /api/public/turnos/{token}` con el token de la respuesta
- **THEN** la consulta devuelve el turno reservado

#### Scenario: El token de la reserva no aparece en los logs
- **WHEN** se crea una reserva
- **THEN** los logs de la aplicación no contienen el token en claro

### Requirement: Email de confirmación de la reserva

Después de confirmar la reserva, el sistema SHALL registrar y enviar un email `CONFIRMACION_RESERVA` al email de la ficha, con el enlace `…/turno/{token}`. Si el envío falla, la reserva MUST quedar hecha, responder 201 con el token y dejar la notificación `FALLIDA` con reintento. Un reintento SHALL llevar un enlace nuevo mientras el turno siga vigente y SHALL descartarse si se canceló o ya empezó.

#### Scenario: La reserva envía el email de confirmación con el enlace
- **WHEN** se crea una reserva
- **THEN** se envía un único email `CONFIRMACION_RESERVA` al email de la ficha con el enlace que contiene el token de la respuesta, y la notificación queda `ENVIADA`

#### Scenario: Un fallo del email deja la reserva hecha y la notificación FALLIDA
- **GIVEN** un servidor de correo que falla
- **WHEN** se crea una reserva
- **THEN** la respuesta es 201 con el token, el turno existe y la notificación `CONFIRMACION_RESERVA` queda `FALLIDA` con un intento y un próximo intento programado

#### Scenario: El reintento de la confirmación lleva un enlace nuevo válido
- **GIVEN** una confirmación `FALLIDA` de un turno vigente
- **WHEN** el planificador la reintenta y el envío funciona
- **THEN** el email lleva un enlace con un token nuevo que da acceso al turno, y el token de la respuesta original sigue válido

#### Scenario: El reintento de un turno cancelado se descarta
- **GIVEN** una confirmación `FALLIDA` de un turno que después se canceló
- **WHEN** el planificador la procesa
- **THEN** no se envía el email y la notificación queda descartada

### Requirement: Datos públicos del consultorio

`GET /api/public/consultorio` SHALL ser anónimo y devolver el nombre, el domicilio, el email de contacto, los años de retención de datos y la versión vigente del consentimiento, para que el texto de consentimiento se arme con la configuración vigente. MUST NOT devolver ningún otro parámetro de configuración.

#### Scenario: El consultorio expone los datos del aviso de consentimiento
- **GIVEN** la configuración por defecto
- **WHEN** un cliente anónimo consulta `GET /api/public/consultorio`
- **THEN** recibe 200 con exactamente `nombre`, `domicilio`, `emailContacto`, `retencionAnios` igual a 2 y `versionConsentimiento` igual a `2026-11-v1`

#### Scenario: Los datos del consultorio reflejan la configuración vigente
- **GIVEN** que un administrador cambió `consultorio.domicilio`
- **WHEN** un cliente anónimo consulta `GET /api/public/consultorio`
- **THEN** recibe el domicilio nuevo

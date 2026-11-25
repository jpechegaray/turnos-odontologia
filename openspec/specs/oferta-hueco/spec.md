# oferta-hueco Specification

## Purpose
Avisa automáticamente por email a las entradas compatibles de la lista de espera cuando se libera un hueco. Cada aviso genera una oferta con su propio token seguro, que vence al inicio del hueco. Cubre la creación de las ofertas después de la cancelación, el reintento del aviso, el vencimiento y la búsqueda por token que usa la toma del hueco (RN-06, RN-23, RN-24, RN-38; SU-14; DD-10).

## Requirements

### Requirement: Oferta de hueco

Una oferta de hueco SHALL representar el aviso de un hueco `[hueco_inicio, hueco_fin)` de un profesional a una entrada de la lista de espera, con sillón sugerido, hash del token, expiración igual a `hueco_inicio` y estado `ENVIADA`, `TOMADA` o `VENCIDA` (RN-38). La base de datos MUST garantizar a lo sumo una oferta por `(entrada, profesional, hueco_inicio)` y `hueco_fin > hueco_inicio`.

#### Scenario: La base rechaza una segunda oferta para la misma entrada y el mismo hueco
- **GIVEN** una oferta guardada para una entrada, un profesional y un `hueco_inicio`
- **WHEN** se intenta guardar otra oferta con la misma entrada, el mismo profesional y el mismo `hueco_inicio`
- **THEN** la base de datos rechaza la inserción

#### Scenario: La base rechaza un hueco vacío o invertido
- **GIVEN** una oferta con `hueco_fin` igual o anterior a `hueco_inicio`
- **WHEN** se intenta guardar
- **THEN** la base de datos rechaza la inserción

#### Scenario: La oferta nueva queda enviada y vence al inicio del hueco
- **GIVEN** un hueco que empieza el lunes a las 10:00 en Buenos Aires
- **WHEN** se crea una oferta para ese hueco
- **THEN** la oferta queda `ENVIADA` y su expiración es el lunes a las 10:00 en Buenos Aires

### Requirement: Token de la oferta

Cada oferta SHALL tener su propio token, distinto del token de turno, generado con 256 bits aleatorios en base64url (RN-38, RN-25). Solo el hash SHA-256 del token MUST persistirse; el token en claro MUST NOT guardarse en la base, ni aparecer en logs, ni en el registro de notificaciones. El hash MUST ser único entre todas las ofertas.

#### Scenario: Solo se guarda el hash del token
- **GIVEN** una oferta recién creada y el enlace enviado al paciente
- **WHEN** se lee la fila de la oferta en la base
- **THEN** la columna del token tiene 64 caracteres hexadecimales que corresponden al SHA-256 del token del enlace, y el token en claro no aparece en ninguna columna

#### Scenario: El token no aparece en los logs
- **GIVEN** una cancelación que genera ofertas y envía avisos
- **WHEN** se revisa la salida de logs de todo el proceso
- **THEN** ningún token en claro de los enlaces enviados aparece en los logs

### Requirement: Aviso automático al liberarse un hueco

Cuando se confirma la cancelación de un turno, el sistema SHALL crear una oferta y enviar un email por cada entrada elegida por la selección de la lista de espera (RN-06, RN-23, SU-14). Sin cancelación confirmada, o con el hueco ya empezado, MUST NOT avisarse a nadie. Un fallo al avisar MUST NOT hacer fallar la cancelación.

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

### Requirement: Una sola tanda de avisos por hueco

El sistema SHALL avisar una sola vez por hueco (SU-14 b). Si el mismo hueco (mismo profesional y mismo inicio) vuelve a liberarse, una entrada que ya tiene oferta para ese hueco MUST NOT recibir otra oferta ni otro email.

#### Scenario: Liberar dos veces el mismo hueco no duplica ofertas
- **GIVEN** una entrada compatible que ya recibió una oferta por el hueco del lunes a las 10:00 de un profesional
- **WHEN** se crea otro turno en ese mismo horario y luego se cancela
- **THEN** la entrada sigue teniendo una sola oferta para ese hueco y su paciente recibió un solo email

### Requirement: Email de aviso de hueco

El email de aviso SHALL enviarse al email del paciente de la entrada, con tipo `OFERTA_HUECO`, e incluir la fecha y hora de inicio del hueco en Buenos Aires, la prestación, el profesional y un enlace `<url pública>/oferta/<token>` (RN-31, RN-38). MUST NOT incluir datos de otros pacientes. Una entrada cuyo paciente no tiene email MUST NOT generar oferta ni error. El aviso SHALL registrarse en el registro de notificaciones asociado a la oferta, de forma idempotente.

#### Scenario: El email lleva el enlace con el token de la oferta
- **GIVEN** una entrada compatible de un paciente con email y la URL pública `https://turnos.consultorio.test`
- **WHEN** se cancela el turno que libera el hueco
- **THEN** el paciente recibe un email `OFERTA_HUECO` con el inicio del hueco, la prestación, el profesional y un enlace `https://turnos.consultorio.test/oferta/<token>` cuyo token corresponde al hash de su oferta

#### Scenario: Un paciente sin email no genera oferta
- **GIVEN** una entrada compatible cuyo paciente ya no tiene email en su ficha
- **WHEN** se cancela el turno que libera el hueco
- **THEN** no se crea oferta para esa entrada, no se envía email y la cancelación responde con éxito

#### Scenario: El aviso queda registrado asociado a la oferta
- **GIVEN** una oferta creada por una cancelación
- **WHEN** se consulta el registro de notificaciones
- **THEN** hay un solo registro `OFERTA_HUECO` asociado a esa oferta, con el inicio del hueco como referencia

### Requirement: Reintento del aviso de hueco

Si el envío del aviso falla, el reintento SHALL enviar un enlace válido: como el token en claro no se guarda, el sistema SHALL generar un token nuevo para la oferta, reemplazar su hash y enviar el enlace nuevo; el enlace anterior MUST dejar de ser válido. Los reintentos MUST cesar cuando el hueco empieza (RN-38, SU-14 g) o cuando la oferta deja de estar `ENVIADA`.

#### Scenario: El reintento envía un enlace nuevo y válido
- **GIVEN** una oferta cuyo primer envío falló
- **WHEN** el planificador de notificaciones reintenta el aviso antes del inicio del hueco
- **THEN** el paciente recibe un enlace con un token distinto, el hash de la oferta corresponde al token nuevo y el token anterior ya no encuentra la oferta

#### Scenario: No se reintenta después de empezar el hueco
- **GIVEN** una oferta cuyo envío falló y un hueco que ya empezó
- **WHEN** corre el planificador de notificaciones
- **THEN** no se reintenta el aviso

#### Scenario: No se reintenta una oferta que ya no está enviada
- **GIVEN** una oferta `VENCIDA` o `TOMADA` cuyo aviso había fallado
- **WHEN** corre el planificador de notificaciones
- **THEN** no se envía el aviso

### Requirement: Vencimiento de las ofertas

Una oferta SHALL estar vigente hasta el inicio del hueco (RN-24, RN-38). Un planificador SHALL pasar a `VENCIDA` las ofertas `ENVIADA` cuya expiración es igual al momento actual o anterior. MUST ser idempotente y MUST NOT cambiar ofertas `TOMADA`. Cuando una oferta vence, el hueco queda libre para la reserva online y para recepción.

#### Scenario: Una oferta vence al inicio del hueco
- **GIVEN** una oferta `ENVIADA` de un hueco que empieza a las 10:00 y el reloj en las 10:00
- **WHEN** corre el planificador de vencimiento
- **THEN** la oferta queda `VENCIDA`

#### Scenario: Una oferta futura sigue vigente
- **GIVEN** una oferta `ENVIADA` de un hueco que empieza a las 10:00 y el reloj en las 09:59
- **WHEN** corre el planificador de vencimiento
- **THEN** la oferta sigue `ENVIADA`

#### Scenario: El vencimiento no toca ofertas tomadas y se puede repetir
- **GIVEN** una oferta `TOMADA` y una `VENCIDA`, ambas con expiración pasada
- **WHEN** el planificador corre dos veces
- **THEN** las dos ofertas conservan su estado

### Requirement: Búsqueda de una oferta vigente por token

El sistema SHALL encontrar una oferta a partir de su token en claro comparando por hash, y solo si está `ENVIADA` y vigente. Un token inexistente, mal formado, vencido o de una oferta `TOMADA` o `VENCIDA` MUST dar el mismo resultado de "no encontrada", sin distinguir la causa (RN-38).

#### Scenario: Un token vigente encuentra su oferta
- **GIVEN** una oferta `ENVIADA` cuyo hueco todavía no empezó
- **WHEN** se busca con el token del enlace
- **THEN** se obtiene esa oferta

#### Scenario: Tokens inválidos dan el mismo resultado
- **GIVEN** un token inexistente, uno mal formado, uno de una oferta vencida y uno de una oferta tomada
- **WHEN** se busca con cada uno
- **THEN** en los cuatro casos el resultado es "no encontrada", sin diferencia observable

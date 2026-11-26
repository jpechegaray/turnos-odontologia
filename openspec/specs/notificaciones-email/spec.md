# notificaciones-email Specification

## Purpose
Entrega la infraestructura común de notificaciones por email del consultorio: un puerto de envío con adaptador SMTP y adaptador en memoria, un registro idempotente de cada envío, reintentos con espera creciente y un planificador que retoma pendientes y fallidas sin perder turnos (RN-30, RN-31, DD-07).

## Requirements

### Requirement: Registro del resultado de cada envío

El sistema SHALL registrar cada intento de envío de email en `NotificacionEnviada` con su tipo, canal `EMAIL`, destinatario, referencias por id a turno, oferta de hueco y paciente, y un estado `PENDIENTE`, `ENVIADA` o `FALLIDA`. Un envío exitoso MUST quedar `ENVIADA` con la fecha de envío; un fallo MUST quedar `FALLIDA` con el último error y conservar la referencia al turno.

#### Scenario: Un envío exitoso queda ENVIADA con la fecha de envío
- **GIVEN** una notificación pendiente con destinatario y datos completos
- **WHEN** el adaptador de notificaciones confirma el envío
- **THEN** el registro queda en estado `ENVIADA`, con `enviada_en` igual al instante del reloj y `ultimo_error` vacío

#### Scenario: Un fallo de SMTP deja la notificación FALLIDA y conserva el turno
- **GIVEN** una notificación pendiente asociada a un turno existente
- **WHEN** el adaptador de notificaciones informa un error de SMTP
- **THEN** el registro queda en estado `FALLIDA` con el mensaje de error en `ultimo_error` y el turno sigue existiendo sin cambios

#### Scenario: Un paciente sin email no genera notificación ni error
- **GIVEN** un turno cuyo paciente no tiene email
- **WHEN** se solicita notificar ese turno
- **THEN** no se crea ninguna `NotificacionEnviada` y la operación termina sin error

#### Scenario: La notificación usa el nombre del consultorio y no expone otros pacientes
- **GIVEN** el parámetro `consultorio.nombre` con un valor vigente y un turno de un paciente
- **WHEN** se arma el email de ese turno
- **THEN** el asunto y el cuerpo incluyen el nombre del consultorio vigente y no incluyen datos de ningún otro paciente

### Requirement: Idempotencia de las notificaciones

El sistema SHALL admitir a lo sumo un registro por `(turno, tipo, inicio_referencia)` y a lo sumo uno por `(oferta_hueco, tipo)` cuando la oferta no es nula. Volver a solicitar el mismo aviso MUST NOT crear una segunda fila ni reenviar el email. Un turno reprogramado MUST generar un aviso nuevo porque cambia su `inicio_referencia`.

#### Scenario: No se reenvía un aviso que ya tiene registro
- **GIVEN** una `NotificacionEnviada` existente para un turno, un tipo y un `inicio_referencia`
- **WHEN** se vuelve a solicitar la misma notificación
- **THEN** no se crea una segunda fila y el adaptador no recibe un nuevo envío

#### Scenario: Dos inserciones iguales violan la restricción única
- **GIVEN** una fila de `NotificacionEnviada` para un turno, un tipo y un `inicio_referencia`
- **WHEN** se intenta insertar otra fila con esos mismos valores
- **THEN** la base de datos rechaza la inserción por la restricción única

#### Scenario: Un turno reprogramado genera un nuevo recordatorio
- **GIVEN** un recordatorio ya registrado para un turno y su `inicio_referencia` original
- **WHEN** el turno se reprograma y se solicita el recordatorio con el nuevo inicio
- **THEN** se crea una fila nueva y la anterior se conserva

#### Scenario: Los avisos de oferta no colisionan entre sí ni con los de turno
- **GIVEN** dos ofertas de hueco distintas y un turno, todos sin relación entre sí
- **WHEN** se registra un aviso `OFERTA_HUECO` por cada oferta y un aviso por el turno
- **THEN** las tres filas se guardan y solo se rechaza repetir el mismo par oferta–tipo o turno–tipo–inicio

### Requirement: Reintentos con espera creciente

El sistema SHALL reintentar una notificación `FALLIDA` hasta un máximo de 3 intentos en total, programando cada reintento con una espera mayor que la anterior. Al alcanzar el tercer intento fallido, MUST dejar de reintentar.

#### Scenario: Una notificación fallida se reintenta hasta tres veces con espera creciente
- **GIVEN** una notificación que falla en cada intento y un reloj controlado
- **WHEN** el planificador procesa los reintentos vencidos
- **THEN** se realizan exactamente 3 intentos y la espera entre intentos es estrictamente creciente

#### Scenario: Una notificación fallida no se reintenta después del tercer intento
- **GIVEN** una notificación `FALLIDA` con 3 intentos
- **WHEN** el planificador vuelve a ejecutarse después del último reintento programado
- **THEN** el adaptador no recibe un cuarto intento y la fila permanece `FALLIDA`

#### Scenario: El próximo intento se calcula con el reloj inyectable
- **GIVEN** un reloj fijo en un instante conocido
- **WHEN** una notificación falla
- **THEN** `programada_para` queda en el instante del reloj más la espera del intento siguiente

#### Scenario: Los reintentos de un aviso de oferta cesan al comenzar el hueco
- **GIVEN** un aviso `OFERTA_HUECO` fallido cuyo hueco comienza en un instante conocido
- **WHEN** el reloj alcanza o supera el inicio del hueco
- **THEN** el planificador no vuelve a intentar ese aviso

#### Scenario: Un reintento vuelve a armar el mensaje con los datos del consumidor
- **GIVEN** un consumidor que sabe reconstruir los datos de su notificación y un primer envío que falla
- **WHEN** el planificador reintenta la notificación
- **THEN** el email reintentado lleva el mismo nombre, prestación, profesional y enlace que el original, sin que la fila guarde el cuerpo

### Requirement: Planificador de envíos pendientes y fallidos

El sistema SHALL ejecutar un planificador periódico que envíe las notificaciones `PENDIENTE` con `programada_para` menor o igual al instante actual y reintente las `FALLIDA` con menos de 3 intentos cuyo próximo intento ya venció. Dos ejecuciones concurrentes MUST NOT producir un doble envío.

#### Scenario: El planificador envía las notificaciones pendientes vencidas
- **GIVEN** una notificación `PENDIENTE` con `programada_para` anterior al instante actual
- **WHEN** se ejecuta el planificador
- **THEN** el adaptador recibe el envío y la notificación queda `ENVIADA`

#### Scenario: El planificador reintenta las fallidas cuyo próximo intento venció
- **GIVEN** una notificación `FALLIDA` con menos de 3 intentos y `programada_para` anterior al instante actual
- **WHEN** se ejecuta el planificador
- **THEN** el adaptador recibe un nuevo intento y se actualiza el estado según el resultado

#### Scenario: El planificador no envía notificaciones programadas a futuro
- **GIVEN** una notificación `PENDIENTE` con `programada_para` posterior al instante actual
- **WHEN** se ejecuta el planificador
- **THEN** el adaptador no recibe el envío y la notificación sigue `PENDIENTE`

#### Scenario: Dos ejecuciones concurrentes del planificador no duplican un envío
- **GIVEN** una notificación `PENDIENTE` vencida
- **WHEN** dos ejecuciones del planificador la procesan a la vez
- **THEN** el adaptador recibe un solo envío y existe una sola fila para esa notificación

#### Scenario: Un reinicio del servicio retoma las pendientes en el siguiente ciclo
- **GIVEN** notificaciones `PENDIENTE` guardadas antes de reiniciar el servicio
- **WHEN** el planificador se ejecuta después del reinicio
- **THEN** esas notificaciones se envían sin crear filas nuevas

#### Scenario: Un envío en curso no se duplica con el planificador
- **GIVEN** una notificación recién registrada cuyo envío inmediato todavía no terminó
- **WHEN** el planificador se ejecuta en ese momento
- **THEN** no toma esa fila y el paciente recibe un solo email

### Requirement: Configuración de correo por entorno

El sistema SHALL tomar host, puerto, usuario, contraseña y remitente SMTP de variables de entorno y MUST NOT versionar credenciales en el repositorio. Con el canal SMTP activo, la ausencia de una credencial obligatoria MUST impedir el arranque. Las pruebas MUST usar el adaptador en memoria y no requerir un servidor SMTP.

#### Scenario: Las credenciales SMTP se toman solo del entorno
- **GIVEN** variables de entorno con host, puerto, usuario, contraseña y remitente
- **WHEN** arranca la aplicación con el canal SMTP activo
- **THEN** el adaptador SMTP usa esos valores sin leerlos de un archivo versionado

#### Scenario: El arranque falla si falta una credencial SMTP obligatoria
- **GIVEN** el canal SMTP activo y la variable `SMTP_PASSWORD` ausente
- **WHEN** la aplicación intenta arrancar
- **THEN** el arranque falla con un mensaje que no revela el valor de la credencial

#### Scenario: Las pruebas usan el adaptador en memoria
- **GIVEN** el perfil de pruebas
- **WHEN** se ejecuta un envío en un test
- **THEN** el adaptador en memoria registra el envío y no se abre ninguna conexión SMTP

#### Scenario: El repositorio no contiene credenciales SMTP
- **WHEN** se revisan los archivos versionados del repositorio
- **THEN** no aparece ningún valor real de usuario o contraseña SMTP, solo marcadores de ejemplo en `.env.example`

### Requirement: Avisos al confirmar cambios de turno

Tras confirmar una cancelación por el personal o una reprogramación, el sistema SHALL registrar y enviar el aviso por C-15 si el paciente tiene email (RN-31). El aviso SHALL indicar la fecha y hora del turno cancelado o el nuevo inicio para una reprogramación.

#### Scenario: La cancelación del personal envía un solo correo
- **WHEN** el personal cancela un turno de un paciente con email y se procesa dos veces el mismo evento confirmado
- **THEN** hay un registro `CANCELACION` y se envía un correo con el horario cancelado

#### Scenario: La reprogramación avisa el nuevo horario y descarta el anterior
- **WHEN** se confirma un cambio de inicio de un turno que tiene un recordatorio para el horario anterior
- **THEN** se registra y envía un correo `REPROGRAMACION` con el nuevo inicio y el recordatorio anterior queda descartado

#### Scenario: Un cambio sin email descarta el recordatorio sin enviar avisos
- **WHEN** se confirma la cancelación o el cambio de inicio de un turno de un paciente sin email
- **THEN** la operación termina correctamente, se descarta el recordatorio obsoleto y no se registra ni envía el aviso

#### Scenario: Una cancelación por token descarta el recordatorio sin enviar aviso
- **WHEN** el paciente cancela su turno por token
- **THEN** se descarta el recordatorio y no se registra ni envía el aviso de cancelación

#### Scenario: Una operación revertida no envía avisos ni descarta recordatorios
- **WHEN** la transacción de agenda publica el evento y después se revierte
- **THEN** no se registra ni envía el aviso y el recordatorio conserva su programación

#### Scenario: Un aviso fallido se reintenta con la misma fecha
- **WHEN** falla el primer envío y vence el plazo de reintento
- **THEN** la notificación se reintenta con la misma fecha de referencia y se registra el resultado

### Requirement: Vigencia del recordatorio tras cambiar un turno

El sistema SHALL descartar los recordatorios programados que pierdan vigencia por cancelación o cambio de inicio, conservar los ya enviados y permitir que el planificador genere el recordatorio del nuevo inicio (RN-30).

#### Scenario: Una reprogramación con el mismo inicio conserva su recordatorio
- **WHEN** se reprograma un turno sin modificar su inicio
- **THEN** el recordatorio para ese inicio conserva su programación

#### Scenario: Volver al inicio anterior permite retomar su recordatorio descartado
- **WHEN** el turno vuelve al inicio original y su recordatorio de ese inicio había sido descartado antes de enviarse
- **THEN** el próximo ciclo puede enviar el recordatorio conservando el mismo registro

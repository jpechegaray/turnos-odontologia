## ADDED Requirements

### Requirement: Reintento del email de confirmación de reserva

Al reintentar una notificación `CONFIRMACION_RESERVA` (RN-30), el sistema SHALL volver a armar el email con los datos actuales del turno: fecha y hora, profesional, prestación y un enlace `/turno/{token}` con un token nuevo del turno. El token en claro MUST NOT guardarse en el registro de la notificación. Si el turno está cancelado, ya empezó o cambió de inicio respecto del registrado, o si el paciente ya no tiene email, la notificación MUST descartarse sin enviarse.

#### Scenario: El reintento lleva un enlace válido
- **GIVEN** una notificación `CONFIRMACION_RESERVA` `FALLIDA` de un turno `PROGRAMADO` que todavía no empezó
- **WHEN** el planificador la reintenta y el servidor de correo acepta el envío
- **THEN** la notificación queda `ENVIADA`
- **AND** el email contiene un enlace `/turno/{token}` cuyo token permite consultar ese turno

#### Scenario: No se reintenta la confirmación de un turno cancelado
- **GIVEN** una notificación `CONFIRMACION_RESERVA` `FALLIDA` cuyo turno fue cancelado
- **WHEN** el planificador la procesa
- **THEN** no se envía ningún email y la notificación deja de reintentarse

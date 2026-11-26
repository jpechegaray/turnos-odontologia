# token-turno Specification

## Purpose

Vincular el acceso sin sesión a un único turno y limitar su vigencia al inicio o a la cancelación, sin almacenar el secreto en claro.

## Requirements

### Requirement: Emisión vinculada al turno y almacenamiento seguro

El sistema SHALL emitir el token únicamente para un turno existente, no cancelado y con inicio futuro, y fijar `expira_en` al inicio vigente de ese turno (RN-25). SHALL guardar la referencia al turno, el hash único y el instante de creación. `revocado_en` y `ultimo_uso_en` MUST quedar vacíos al emitir. El secreto en claro MUST entregarse solo como resultado de esa emisión y MUST NOT poder recuperarse desde lo persistido.

#### Scenario: La emisión entrega el secreto y guarda solo su hash
- **WHEN** se emite un token para un turno futuro no cancelado
- **THEN** la emisión entrega el valor en claro y la base guarda únicamente su hash como credencial, con el identificador del turno y su inicio como vencimiento
- **AND** la creación queda registrada y no hay revocación ni último uso

#### Scenario: No se emite para un turno inexistente, cancelado o iniciado
- **WHEN** se pide emitir para un turno inexistente, cancelado o cuyo inicio es igual o anterior a la hora actual
- **THEN** no se entrega un secreto ni se crea un token

#### Scenario: PostgreSQL no admite un hash repetido ni un turno inexistente
- **WHEN** se intenta persistir un segundo registro con un hash existente o un registro asociado a un turno inexistente
- **THEN** PostgreSQL rechaza la escritura

### Requirement: Validación acotada al turno y al instante anterior al inicio

La validación SHALL resolver únicamente el identificador del turno asociado si el token coincide, no está revocado y la hora actual es estrictamente anterior a su vencimiento. Si la operación espera un turno concreto, MUST rechazar cualquier discrepancia. Solo una validación exitosa SHALL registrar el instante de último uso, sin consumir el token (RN-07, RN-25).

#### Scenario: Un token válido identifica solo su turno y registra el uso
- **WHEN** se valida un token correspondiente a un turno antes de su inicio
- **THEN** se resuelve solo el identificador de ese turno y se registra el instante de uso
- **AND** el mismo token puede volver a validarse antes de vencer si no fue revocado

#### Scenario: El token vence exactamente al inicio del turno
- **WHEN** se valida un mismo token justo antes, exactamente al inicio y después del inicio del turno
- **THEN** solo el intento anterior al inicio es válido
- **AND** los intentos rechazados no cambian el último uso

#### Scenario: El token del turno A no abre el turno B
- **WHEN** se valida un token vigente del turno A para operar sobre el turno B
- **THEN** se rechaza con el mismo resultado que un token inexistente y no se actualiza el último uso

### Requirement: Rechazo uniforme y ausencia del secreto en registros

Un token malformado, inexistente, vencido, revocado o ajeno al turno solicitado SHALL producir un único error de negocio equivalente a `404`, con el mismo código y detalle no sensibles. El secreto en claro MUST NOT aparecer en mensajes de error, representaciones de texto ni logs de las operaciones de C-32 (RN-25).

#### Scenario: Los rechazos de token son indistinguibles
- **WHEN** se presentan tokens malformados, inexistentes, vencidos, revocados o de otro turno
- **THEN** cada intento devuelve el mismo estado de negocio `404`, código y detalle sin indicar la causa

#### Scenario: El secreto no aparece en logs ni en errores del servicio
- **WHEN** se capturan los logs de emisión, validación exitosa y rechazos con valores ficticios
- **THEN** no aparece ningún valor de token en claro en los logs ni en los mensajes de error

### Requirement: Revocación y vencimiento acompañan el ciclo del turno

Al procesar `TurnoCancelado`, el sistema SHALL revocar todos los tokens no revocados de ese turno con el instante de la cancelación. Al procesar `TurnoReprogramado`, SHALL actualizar la expiración de los tokens no revocados al nuevo inicio, conservando el secreto original. Reprocesar la cancelación MUST NOT cambiar una revocación previa y reprogramar MUST NOT rehabilitar tokens revocados. Los eventos MUST NOT afectar a otros turnos ni dejar cambios si la operación de agenda se revierte.

#### Scenario: Cancelar revoca solo los tokens del turno cancelado
- **WHEN** se procesa la cancelación de un turno con tokens emitidos
- **THEN** todos sus tokens no revocados quedan revocados al instante de la cancelación y devuelven el rechazo uniforme
- **AND** los tokens de otros turnos no cambian y repetir la cancelación no cambia el instante registrado

#### Scenario: Reprogramar conserva el secreto y cambia el instante de expiración
- **WHEN** se reprograma un turno con tokens emitidos a un inicio anterior o posterior al original
- **THEN** los mismos secretos no revocados siguen sirviendo exclusivamente para ese turno y vencen exactamente al nuevo inicio
- **AND** los tokens ya revocados y los de otros turnos no se modifican

#### Scenario: Los eventos de turnos sin token no generan credenciales
- **WHEN** se cancela o reprograma un turno que no tiene tokens
- **THEN** la operación no crea credenciales ni falla por esa ausencia

#### Scenario: Revertir la operación de agenda no cambia los tokens
- **WHEN** se publica la cancelación o reprogramación y luego se revierte su transacción
- **THEN** no quedan cambios de revocación ni de vencimiento en los tokens

### Requirement: Consulta pública limitada al turno del enlace

`GET /api/public/turnos/{token}` SHALL responder `200` con `inicio`, `fin` (ISO 8601 con zona o desplazamiento), `estado`, `profesional` y `prestacion` (nombres para mostrar), y `cancelacionSeriaTardia`. El indicador SHALL usar la política y el plazo vigentes: a plazo exacto es `false`. La ruta y sus acciones MUST funcionar sin sesión ni CSRF y resolver el turno exclusivamente por el token; MUST NOT aceptar otro selector de turno ni exponer el token, identificadores internos, datos de pacientes, notas, agenda ni datos de la lista de espera. No se ofrece una ruta pública de reprogramación.

#### Scenario: El enlace válido devuelve solo el detalle público de su turno
- **GIVEN** dos turnos distintos y un token vigente para el primero
- **WHEN** un cliente sin sesión consulta el primer turno con su enlace
- **THEN** recibe `200` con solo los campos públicos del primer turno

#### Scenario: El token del turno A no permite operar sobre el turno B
- **GIVEN** un token vigente del turno A y un turno B distinto
- **WHEN** se intenta utilizar ese token en una solicitud dirigida al turno B
- **THEN** la combinación no corresponde a una ruta pública válida, devuelve `404` y no lee ni modifica B

#### Scenario: La consulta distingue el plazo exacto de la cancelación tardía
- **GIVEN** un turno vigente y un plazo de cancelación configurado
- **WHEN** se consulta justo en el límite y nuevamente después de cruzarlo pero antes del inicio
- **THEN** `cancelacionSeriaTardia` es `false` primero y `true` después

#### Scenario: No existe reprogramación para el enlace del paciente
- **WHEN** un cliente presenta un token vigente a `POST /api/public/turnos/{token}/reprogramar`
- **THEN** recibe `404` y el turno no cambia

### Requirement: Confirmación pública del turno

`POST /api/public/turnos/{token}/confirmar` SHALL confirmar un turno `PROGRAMADO` con token vigente, sin requerir cuerpo, y devolver `200` con la misma proyección que la consulta y estado `CONFIRMADO`. Una transición inválida con token aún válido MUST devolver `409 TURNO_TRANSICION_INVALIDA`, sin cambiar el turno. El éxito MUST NOT consumir ni revocar el token.

#### Scenario: El paciente confirma su turno sin sesión ni CSRF
- **WHEN** un cliente sin sesión ni CSRF confirma un turno `PROGRAMADO` con su token vigente
- **THEN** recibe `200` con el estado `CONFIRMADO`, el cambio queda persistido y el enlace sigue permitiendo consultar el turno

#### Scenario: Volver a confirmar no modifica el turno confirmado
- **WHEN** se confirma un turno ya `CONFIRMADO` con su token vigente
- **THEN** la API responde `409 TURNO_TRANSICION_INVALIDA` sin cambiar el turno

### Requirement: Cancelación pública y liberación del intervalo

`POST /api/public/turnos/{token}/cancelar` SHALL aceptar un token vigente para un turno `PROGRAMADO` o `CONFIRMADO`, sin requerir cuerpo, y devolver `204`. SHALL registrar `PACIENTE_TOKEN`, el instante y la tardanza con la política vigente al cancelar, sin usuario de personal ni motivo. Tanto en plazo como fuera de plazo SHALL liberar el intervalo para profesional y sillón, publicar los eventos de cancelación de agenda y revocar todos los tokens del turno en la misma transacción que registra la cancelación.

#### Scenario: El paciente cancela en plazo y el intervalo se puede volver a reservar
- **WHEN** un paciente cancela un turno `PROGRAMADO` en plazo sin sesión ni CSRF
- **THEN** recibe `204`, se registra la cancelación en plazo por `PACIENTE_TOKEN` sin usuario ni motivo y puede reservarse el mismo intervalo con el mismo profesional y sillón

#### Scenario: Cancelar tarde un turno confirmado libera el intervalo y revoca sus enlaces
- **GIVEN** un turno `CONFIRMADO` con dos tokens vigentes
- **WHEN** un paciente lo cancela después del plazo pero antes de su inicio
- **THEN** recibe `204`, la cancelación queda marcada como tardía, se libera el intervalo y ambos enlaces pasan a responder con el `404` uniforme

#### Scenario: Dos cancelaciones públicas simultáneas liberan un solo hueco
- **WHEN** dos solicitudes con tokens vigentes del mismo turno intentan cancelarlo simultáneamente
- **THEN** solo una obtiene `204`, la otra obtiene el `404` uniforme y se publica un único evento de cada tipo

### Requirement: Rechazo uniforme y protección HTTP del enlace

Las tres operaciones SHALL devolver el mismo `404` Problem Details para token malformado, inexistente, vencido (inclusive al inicio) o revocado, con código `TOKEN_TURNO_NO_ENCONTRADO` y detalle `Token de turno no encontrado`. Las respuestas, incluidos errores, MUST NOT reflejar el token en ningún campo o cabecera; `instance`, si existe, MUST ser independiente del token. Las rutas MUST aplicar el límite por IP de `limite-tasa-publico` también a credenciales inválidas y MUST NOT registrar el secreto en logs de aplicación ni de acceso. Las respuestas de estas rutas SHALL indicar `Cache-Control: no-store`.

#### Scenario: Las tres operaciones devuelven un 404 indistinguible para credenciales rechazadas
- **WHEN** se invoca cada operación con un token malformado, desconocido, dado de baja o en el instante de inicio
- **THEN** cada caso devuelve el mismo `404`, código y detalle, sin `instance` con la URI original ni cambios en el turno

#### Scenario: El límite por IP cubre lecturas y mutaciones del enlace
- **WHEN** una IP agota el cupo público con solicitudes con token válido o inválido y llama a cualquiera de las tres operaciones
- **THEN** recibe `429` con `Retry-After` antes de ejecutar la operación

#### Scenario: Ninguna respuesta ni registro expone el token del enlace
- **WHEN** se ejercitan respuestas exitosas y errores `404`, `409`, `429` y un `500` controlado con tokens ficticios
- **THEN** ninguna respuesta ni registro de aplicación o acceso configurado contiene el token y las respuestas incluyen `Cache-Control: no-store`

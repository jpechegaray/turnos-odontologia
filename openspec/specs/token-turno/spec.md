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

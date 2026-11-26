# Spec Delta

## ADDED Requirements

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

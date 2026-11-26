# Spec Delta

## Purpose

Permite que recepción asigne manualmente un hueco libre a una entrada compatible de la lista de espera, creando el turno con todas las reglas del alta, cerrando la entrada y tomando las ofertas del hueco.

## ADDED Requirements

### Requirement: Asignación manual de un hueco

`ADMIN` y `RECEPCION` SHALL poder asignar un hueco a una entrada con `POST /api/lista-espera/{id}/asignar`, enviando `profesionalId`, `inicio` (ISO-8601 con zona) y, opcionalmente, `sillonId` y `notas`. La asignación MUST crear un turno normal con todas las reglas del alta, cerrar la entrada como `ASIGNADA` con su `turnoAsignadoId` y responder `201` con la representación del turno.

#### Scenario: Recepción asigna un hueco a una entrada compatible

- **GIVEN** una entrada `ACTIVA` compatible con un hueco futuro de un profesional activo que realiza su prestación, y el hueco libre
- **WHEN** `ADMIN` o `RECEPCION` envía la asignación con `profesionalId`, `inicio` del hueco y CSRF válido
- **THEN** recibe `201` con el turno creado (paciente, profesional y prestación de la entrada, `inicio` igual al del hueco y duración por defecto de la prestación) y la entrada queda `ASIGNADA` con ese `turnoAsignadoId`

#### Scenario: La asignación guarda las notas del turno

- **GIVEN** una entrada `ACTIVA` compatible con un hueco libre
- **WHEN** recepción asigna el hueco con `notas` opcionales
- **THEN** el turno creado conserva esas notas y la entrada queda `ASIGNADA`

#### Scenario: El cuerpo exige profesional e inicio con zona

- **WHEN** `RECEPCION` envía una asignación sin `profesionalId`, sin `inicio` o con un `inicio` sin zona horaria
- **THEN** recibe `400` en Problem Details, no se crea ningún turno y la entrada no cambia

### Requirement: Validación de la entrada

La entrada MUST existir (`404` con `ENTRADA_NO_ENCONTRADA`), estar `ACTIVA` (`409` con `ENTRADA_NO_ACTIVA`) y ser compatible con el hueco (RN-22; `422` con `ENTRADA_INCOMPATIBLE`) antes de crear el turno. Ningún rechazo MUST crear el turno ni cambiar el estado de la entrada.

#### Scenario: Una entrada inexistente responde 404

- **WHEN** un usuario autorizado asigna un hueco a un ID de entrada inexistente
- **THEN** recibe `404` con código `ENTRADA_NO_ENCONTRADA` y no se crea ningún turno

#### Scenario: Una entrada que ya no está activa responde 409

- **GIVEN** una entrada `ASIGNADA`, `CANCELADA` o `VENCIDA`
- **WHEN** un usuario autorizado intenta asignarle un hueco
- **THEN** recibe `409` con código `ENTRADA_NO_ACTIVA`, no se crea ningún turno y la entrada conserva su estado

#### Scenario: Una entrada incompatible con el hueco responde 422

- **GIVEN** una entrada `ACTIVA` cuya prestación no cabe en el hueco, o cuyo profesional preferido no es el del hueco, o cuya fecha o franja no incluyen el hueco
- **WHEN** un usuario autorizado intenta asignarle ese hueco
- **THEN** recibe `422` con código `ENTRADA_INCOMPATIBLE`, no se crea ningún turno y la entrada sigue `ACTIVA`

### Requirement: Duración y sillón del turno asignado

El turno SHALL empezar en `inicio`, durar la duración por defecto de la prestación de la entrada (RN-24, SU-14 d) y usar un sillón activo y libre. Si el cuerpo trae `sillonId`, MUST usarse; si no, MUST elegirse el sillón sugerido por una oferta del mismo hueco cuando sigue libre y, si no, el primero activo libre. La creación del turno revalida el sillón y el horario.

#### Scenario: La duración del turno es la de la prestación de la entrada

- **GIVEN** una entrada de una prestación de 30 minutos compatible con un hueco de 30 minutos o más
- **WHEN** recepción asigna el hueco sin indicar sillón
- **THEN** el turno creado dura 30 minutos y empieza en el `inicio` del hueco, y el resto del hueco, si sobra, queda libre

#### Scenario: Sin sillón se prefiere el sugerido por la oferta si sigue libre

- **GIVEN** un hueco con al menos una oferta cuyo sillón sugerido sigue activo y libre, y otros sillones activos
- **WHEN** recepción asigna el hueco sin indicar sillón
- **THEN** el turno se crea en el sillón sugerido por la oferta

#### Scenario: Sin sillón libre sugerido se usa el primero activo libre

- **GIVEN** un hueco cuyo sillón sugerido por la oferta está ocupado, y otros sillones activos libres
- **WHEN** recepción asigna el hueco sin indicar sillón
- **THEN** el turno se crea en el primero de los sillones activos libres y el sillón sugerido no se usa

#### Scenario: Un sillón indicado ocupado responde 409

- **GIVEN** un hueco libre para el profesional y un sillón indicado que ya tiene un turno que solapa
- **WHEN** recepción asigna el hueco indicando ese sillón
- **THEN** recibe `409` con código `RN-01`, no se crea el turno y la entrada sigue `ACTIVA`

### Requirement: Competición bajo RN-15 y cierre de ofertas

La asignación SHALL competir bajo RN-15: si otra operación ocupó el hueco, MUST responder `409` con `RN-01`. En la misma transacción, la entrada MUST quedar `ASIGNADA` y todas las ofertas `ENVIADA` del hueco (mismo profesional y mismo `huecoInicio`) MUST pasar a `TOMADA`. Una segunda asignación del mismo hueco MUST responder `409` y no crear un segundo turno.

#### Scenario: Una segunda asignación del mismo hueco responde 409

- **GIVEN** un hueco ya asignado a una entrada
- **WHEN** recepción asigna ese mismo hueco (mismo profesional e inicio) a otra entrada compatible
- **THEN** recibe `409` con código `RN-01`, no se crea un segundo turno y la segunda entrada sigue `ACTIVA`

#### Scenario: Las ofertas del hueco pasan a TOMADA

- **GIVEN** un hueco con ofertas `ENVIADA` de varias entradas compatibles
- **WHEN** recepción asigna el hueco a una de esas entradas
- **THEN** todas las ofertas del hueco con el mismo profesional y `huecoInicio` quedan `TOMADA` y no se envían avisos nuevos

#### Scenario: Sin ofertas la asignación igual funciona

- **GIVEN** un hueco sin ofertas generadas
- **WHEN** recepción asigna el hueco a una entrada compatible
- **THEN** recibe `201`, la entrada queda `ASIGNADA` y no se modifica ninguna oferta

### Requirement: Acceso y atomicidad de la asignación

Las peticiones MUST requerir sesión: `ODONTOLOGO` MUST recibir `403` y un cliente sin sesión `401`. `ADMIN` y `RECEPCION` MUST enviar CSRF válido. La asignación MUST ser atómica: si falla la creación del turno, la entrada MUST conservar su estado y las ofertas su estado.

#### Scenario: El odontólogo no asigna huecos

- **WHEN** un `ODONTOLOGO` intenta asignar un hueco, aun con CSRF válido y cuerpo malformado
- **THEN** recibe `403` en Problem Details, no se crea ningún turno y la entrada no cambia

#### Scenario: Sin sesión no se asigna un hueco

- **WHEN** un cliente sin sesión intenta asignar un hueco
- **THEN** recibe `401` en Problem Details y no se crea ningún turno

#### Scenario: Las escrituras requieren CSRF válido

- **WHEN** `ADMIN` o `RECEPCION` intenta asignar un hueco sin CSRF válido
- **THEN** recibe `403` en Problem Details y la entrada y las ofertas no cambian

#### Scenario: Un fallo al crear el turno no deja la entrada cerrada ni las ofertas tomadas

- **GIVEN** un hueco con ofertas `ENVIADA` y una entrada `ACTIVA` compatible, pero la creación del turno falla
- **WHEN** recepción intenta asignar el hueco
- **THEN** la asignación no deja turno creado, la entrada sigue `ACTIVA` sin `turnoAsignadoId` y las ofertas siguen `ENVIADA`

# Spec Delta

## Purpose

Define el formato uniforme de error de la API REST. Todo error usa Problem Details (RFC 7807) con el código de la regla de negocio incumplida, de modo que la SPA y los tests puedan reaccionar a cada caso sin exponer detalles internos ni datos personales.

## ADDED Requirements

### Requirement: Errores de regla de negocio en formato Problem Details
Cuando una operación incumple una regla de negocio, la API SHALL responder con el estado HTTP asociado a esa regla y un cuerpo `application/problem+json` con `status`, `title`, `detail` y una propiedad `codigo` con el código de la regla (por ejemplo `RN-04`).

#### Scenario: Un error de regla responde Problem Details con su código
- **GIVEN** una operación que incumple una regla con código `RN-04` y estado 422
- **WHEN** el cliente invoca esa operación
- **THEN** la respuesta tiene estado 422 y tipo de contenido `application/problem+json`
- **AND** el cuerpo incluye `status` 422, un `title`, un `detail` legible y `codigo` igual a `RN-04`

### Requirement: Estados admitidos para reglas de negocio
El formato de error SHALL admitir para las reglas de negocio al menos los estados 403, 404, 409 y 422. Cada estado MUST conservar el mismo formato, sin que haga falta un manejador nuevo para cada regla o módulo.

#### Scenario: Cada estado de regla responde con el mismo formato
- **WHEN** se incumplen reglas asociadas a los estados 403, 404, 409 y 422
- **THEN** cada respuesta tiene el estado correspondiente y el formato Problem Details con su `codigo`

#### Scenario: Una regla puede agregar datos de contexto al error
- **GIVEN** una regla de conflicto que informa el recurso en conflicto, por ejemplo `recurso` igual a `PROFESIONAL`
- **WHEN** se incumple esa regla
- **THEN** la respuesta 409 incluye la propiedad `recurso` además de `codigo`

### Requirement: Errores de validación de entrada en formato uniforme
Cuando el cuerpo de una petición no cumple sus validaciones de formato, la API SHALL responder con estado 400 en formato Problem Details e indicar qué campos fallaron, sin repetir el valor recibido.

#### Scenario: Una petición con campos inválidos responde 400 Problem Details
- **GIVEN** una petición cuyo cuerpo tiene un campo obligatorio vacío
- **WHEN** el cliente la envía
- **THEN** la respuesta tiene estado 400 y formato Problem Details
- **AND** el cuerpo lista el nombre del campo inválido

### Requirement: Errores inesperados sin detalles internos
Ante un error no previsto, la API SHALL responder con estado 500 en formato Problem Details y un mensaje genérico. La respuesta MUST NOT incluir trazas de pila, mensajes de excepciones internas, sentencias SQL ni datos personales (RN-35).

#### Scenario: Un error inesperado no expone detalles internos
- **GIVEN** una operación que falla con una excepción interna cuyo mensaje contiene texto sensible
- **WHEN** el cliente la invoca
- **THEN** la respuesta tiene estado 500 y formato Problem Details con un `detail` genérico
- **AND** el cuerpo no contiene el mensaje de la excepción ni una traza de pila

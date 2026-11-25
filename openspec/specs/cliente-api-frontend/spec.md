# cliente-api-frontend Specification

## Purpose
Define cómo la SPA se comunica con la API REST del backend: mismo origen con cookie de sesión, token CSRF en las escrituras y errores Problem Details traducidos a errores tipados que las pantallas pueden mostrar sin conocer HTTP.

## Requirements

### Requirement: Peticiones con la sesión del personal
Toda petición del cliente SHALL dirigirse a la base de la API configurada (por defecto `/api`) y MUST incluir las cookies del mismo origen, para que viaje la cookie de sesión. Los cuerpos MUST enviarse y recibirse como JSON.

#### Scenario: Una lectura usa la base de la API y envía la cookie de sesión
- **GIVEN** la base de la API por defecto
- **WHEN** una pantalla pide `GET /prestaciones` con el cliente
- **THEN** la petición va a `/api/prestaciones`, con las credenciales del mismo origen y aceptando JSON
- **AND** el cliente devuelve el cuerpo JSON ya interpretado

#### Scenario: Una respuesta 204 no devuelve cuerpo
- **WHEN** el backend responde 204 a una petición del cliente
- **THEN** el cliente la resuelve sin cuerpo y sin error

### Requirement: Token CSRF en las escrituras

En toda petición que no sea GET, HEAD ni OPTIONS y que esté fuera de `/public/`, el cliente SHALL enviar la cabecera `X-XSRF-TOKEN` con el valor actual de la cookie `XSRF-TOKEN`, leído al momento de enviar. Si la cookie no existe, el cliente MUST pedir `GET /api/auth/csrf` antes de la escritura. Las lecturas MUST NOT enviar esa cabecera. Las peticiones bajo el segmento `/public/` MUST NOT obtener ni adjuntar el token CSRF.

#### Scenario: Una escritura envía la cabecera con el token vigente
- **GIVEN** la cookie `XSRF-TOKEN` con un valor ficticio
- **WHEN** una pantalla envía `POST /prestaciones` con el cliente
- **THEN** la petición lleva `X-XSRF-TOKEN` con ese valor y el cuerpo en JSON

#### Scenario: Sin cookie CSRF el cliente obtiene el token antes de escribir
- **GIVEN** que no existe la cookie `XSRF-TOKEN`
- **WHEN** una pantalla envía una escritura con el cliente
- **THEN** el cliente primero pide `GET /api/auth/csrf` y después envía la escritura con el token recibido

#### Scenario: Una lectura no envía la cabecera CSRF
- **WHEN** una pantalla pide una lectura con el cliente
- **THEN** la petición no lleva la cabecera `X-XSRF-TOKEN`

#### Scenario: Una reserva pública se envía sin obtener ni adjuntar CSRF
- **WHEN** la pantalla envía `POST /public/reservas` con el cliente
- **THEN** la reserva usa el mismo cliente y no solicita ni envía el token CSRF aunque exista una cookie anterior

### Requirement: Errores de la API tipados
Ante una respuesta de error, el cliente SHALL rechazar con un error de la API que indique su tipo (`no-autenticado` 401, `prohibido` 403, `no-encontrado` 404, `conflicto` 409, `no-procesable` 422, `validacion` 400, `servidor` 5xx) y conserve el estado, el `detail`, el `codigo` de la regla, la lista de campos inválidos y otras propiedades de contexto como `recurso`.

#### Scenario: Un 409 se traduce a un error de conflicto con su código y recurso
- **GIVEN** que el backend responde 409 Problem Details con `codigo` igual a `RN-01` y `recurso` igual a `PROFESIONAL`
- **WHEN** el cliente recibe la respuesta
- **THEN** rechaza con un error de tipo `conflicto`, estado 409, `codigo` `RN-01`, recurso `PROFESIONAL` y el `detail` del backend

#### Scenario: Un 422 se traduce a un error no procesable con el código de la regla
- **GIVEN** que el backend responde 422 Problem Details con `codigo` igual a `RN-09`
- **WHEN** el cliente recibe la respuesta
- **THEN** rechaza con un error de tipo `no-procesable` y `codigo` `RN-09`

#### Scenario: Los errores 404 y 403 se traducen a sus tipos
- **WHEN** el backend responde 404 o 403 en formato Problem Details
- **THEN** el cliente rechaza con un error de tipo `no-encontrado` o `prohibido`, respectivamente

#### Scenario: Un 400 de validación conserva los campos inválidos
- **GIVEN** que el backend responde 400 con `errores` que listan el campo `nombre`
- **WHEN** el cliente recibe la respuesta
- **THEN** rechaza con un error de tipo `validacion` cuya lista de campos incluye `nombre` y su mensaje

#### Scenario: Un 401 se traduce a no autenticado
- **WHEN** el backend responde 401
- **THEN** el cliente rechaza con un error de tipo `no-autenticado`

#### Scenario: Una respuesta de error sin Problem Details igual se tipa por su estado
- **GIVEN** que la respuesta de error no es JSON, por ejemplo un 502 de un proxy
- **WHEN** el cliente recibe la respuesta
- **THEN** rechaza con un error de tipo `servidor`, estado 502 y un mensaje genérico

### Requirement: Fallo de red distinguible
Si la petición no llega a obtener respuesta, el cliente SHALL rechazar con un error de la API de tipo `red` y un mensaje genérico, sin exponer detalles técnicos.

#### Scenario: Un fallo de red se informa como error de red
- **WHEN** la petición falla sin respuesta del servidor
- **THEN** el cliente rechaza con un error de tipo `red` y un mensaje genérico

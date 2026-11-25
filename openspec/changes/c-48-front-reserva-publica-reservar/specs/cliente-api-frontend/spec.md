## MODIFIED Requirements

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

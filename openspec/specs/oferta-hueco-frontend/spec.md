# oferta-hueco-frontend Specification

## Purpose

Permitir que el paciente en lista de espera abra el enlace del aviso, vea el hueco ofrecido y lo tome sin cargar datos, y que sepa claramente cuándo ya no está disponible.

## Requirements

### Requirement: Consulta pública de la oferta

La SPA SHALL abrir `/oferta/:token` sin sesión y mostrar el horario del hueco en Buenos Aires, el profesional, la prestación y el estado de la oferta. La pantalla MUST NOT pedir datos personales.

#### Scenario: Una oferta disponible muestra el hueco y permite tomarlo
- **WHEN** una persona sin sesión abre `/oferta/{token}` y la API devuelve la oferta en estado disponible
- **THEN** la pantalla muestra el horario en Buenos Aires, el profesional y la prestación, y ofrece tomar el hueco sin pedir datos

#### Scenario: Una oferta tomada informa que el hueco ya fue tomado
- **WHEN** la API devuelve la oferta en estado tomada
- **THEN** la pantalla informa que el hueco ya fue tomado y no ofrece tomarlo

#### Scenario: Una oferta vencida informa que el horario ya empezó
- **WHEN** la API devuelve la oferta en estado vencida
- **THEN** la pantalla informa que la oferta venció y no ofrece tomarla

#### Scenario: Una oferta no disponible lo informa sin ofrecer tomarla
- **WHEN** la API devuelve la oferta en estado no disponible
- **THEN** la pantalla informa que la oferta ya no está disponible y no ofrece tomarla

### Requirement: Pantalla genérica ante un token de oferta inválido

Ante un 404 al consultar o tomar la oferta, la SPA SHALL mostrar la misma pantalla genérica del enlace del turno, sin distinguir el motivo ni repetir el token.

#### Scenario: Un token de oferta inválido muestra la pantalla genérica
- **WHEN** la API responde 404 al consultar la oferta
- **THEN** la pantalla muestra el aviso genérico de enlace no disponible, sin el token ni el motivo

### Requirement: Toma del hueco

Tomar el hueco SHALL enviar como máximo un pedido en curso, sin reintentos automáticos. Si la respuesta es 201, la SPA SHALL mostrar el turno creado (horario en Buenos Aires, profesional y prestación) y el enlace relativo `/turno/{token}` del nuevo turno.

#### Scenario: Tomar el hueco muestra el turno y su enlace
- **WHEN** la persona pulsa tomar y la API responde 201 con el turno
- **THEN** la pantalla muestra el turno en Buenos Aires y el enlace `/turno/{token}` del nuevo turno

#### Scenario: Un segundo clic en tomar no repite el pedido
- **WHEN** la persona pulsa tomar otra vez mientras el primer pedido está en curso
- **THEN** la SPA hace un solo POST de toma

### Requirement: Rechazos de la toma

Ante un 409, la SPA SHALL leer el código del error: `HUECO_YA_TOMADO` informa que otro tomó el hueco, `OFERTA_VENCIDA` informa que el horario ya empezó y cualquier otro código informa que la oferta ya no está disponible. En los tres casos la pantalla SHALL dejar de ofrecer la toma.

#### Scenario: Llegar tarde a la toma informa que el hueco ya fue tomado
- **WHEN** la API responde 409 con el código `HUECO_YA_TOMADO` al tomar
- **THEN** la pantalla informa que el hueco ya fue tomado y deja de ofrecer tomarlo

#### Scenario: Tomar un hueco que ya empezó informa que venció
- **WHEN** la API responde 409 con el código `OFERTA_VENCIDA` al tomar
- **THEN** la pantalla informa que la oferta venció y deja de ofrecer tomarla

#### Scenario: Otro rechazo de la toma informa que la oferta no está disponible
- **WHEN** la API responde 409 con el código `OFERTA_NO_DISPONIBLE` u otro código
- **THEN** la pantalla informa que la oferta ya no está disponible y deja de ofrecer tomarla

#### Scenario: El límite de solicitudes permite reintentar la toma
- **WHEN** la API responde 429 al tomar
- **THEN** la pantalla muestra un aviso, conserva la oferta y permite reintentar manualmente

### Requirement: Los tokens de la oferta no salen de la pantalla

La SPA MUST NOT guardar el token de la oferta ni el del turno creado en almacenamiento persistente ni en cookies, y MUST NOT incluirlos en el título del documento.

#### Scenario: Los tokens de la oferta y del turno no quedan en el navegador
- **WHEN** la persona abre `/oferta/{token}` y toma el hueco
- **THEN** ni `localStorage`, ni `sessionStorage`, ni las cookies, ni el título del documento contienen alguno de los dos tokens

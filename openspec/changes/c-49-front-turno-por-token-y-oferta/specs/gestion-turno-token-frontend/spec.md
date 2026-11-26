# Spec Delta

## Purpose

Permitir que el paciente, sin cuenta, abra el enlace de su turno para verlo, confirmarlo o cancelarlo desde el celular, sin que el token se filtre fuera de la pantalla.

## ADDED Requirements

### Requirement: Consulta pública del turno por enlace

La SPA SHALL abrir `/turno/:token` sin sesión y mostrar solo el turno del enlace: horario en Buenos Aires, profesional, prestación y estado. La pantalla MUST NOT ofrecer reprogramar el turno.

#### Scenario: El enlace válido muestra el turno sin pedir sesión
- **WHEN** una persona sin sesión abre `/turno/{token}` y la API devuelve el turno
- **THEN** la pantalla muestra el horario en Buenos Aires, el profesional, la prestación y el estado, y no ofrece reprogramar

#### Scenario: Un turno cancelado o iniciado no ofrece acciones
- **WHEN** la API devuelve el turno en un estado que ya no admite confirmar ni cancelar
- **THEN** la pantalla muestra el estado y no muestra los botones de confirmar ni de cancelar

### Requirement: Pantalla genérica ante un token inválido

Ante un 404 de la API, la SPA SHALL mostrar una pantalla genérica que no distinga entre token inexistente, vencido o revocado y que no repita el token. La misma pantalla SHALL usarse cuando una acción posterior responde 404.

#### Scenario: Un token inválido muestra la pantalla genérica sin pistas
- **WHEN** la API responde 404 al consultar el turno
- **THEN** la pantalla muestra el aviso genérico de enlace no disponible, sin el token ni el motivo

#### Scenario: Un token que vence durante la gestión muestra la pantalla genérica
- **WHEN** la API responde 404 al confirmar o cancelar
- **THEN** la SPA reemplaza la vista del turno por el aviso genérico

### Requirement: Confirmación del turno

La SPA SHALL permitir confirmar un turno `PROGRAMADO` con un único pedido en curso y SHALL mostrar el turno con el estado `CONFIRMADO` que devuelve la API.

#### Scenario: Confirmar pasa el turno a confirmado
- **WHEN** la persona pulsa confirmar y la API responde con el turno confirmado
- **THEN** la pantalla muestra el estado confirmado y deja de ofrecer confirmar

#### Scenario: Un segundo clic en confirmar no repite el pedido
- **WHEN** la persona pulsa confirmar otra vez mientras el primer pedido está en curso
- **THEN** la SPA hace un solo POST de confirmación

### Requirement: Cancelación con aviso previo de cancelación tardía

Cancelar SHALL pedir una confirmación explícita antes de enviar el pedido. Si la API informa que la cancelación sería tardía, el paso de confirmación SHALL avisarlo antes de que la persona confirme. Después de una cancelación exitosa, la pantalla SHALL informar que el turno quedó cancelado.

#### Scenario: Cancelar a tiempo pide confirmación y cancela el turno
- **WHEN** la persona pulsa cancelar, la cancelación no es tardía y confirma la cancelación
- **THEN** la SPA envía un solo POST de cancelación y muestra que el turno quedó cancelado

#### Scenario: Una cancelación tardía avisa antes de confirmar
- **WHEN** la persona pulsa cancelar y la API informó que la cancelación sería tardía
- **THEN** el paso de confirmación avisa que la cancelación es tardía antes de que la persona confirme

#### Scenario: Desistir de la cancelación no envía el pedido
- **WHEN** la persona pulsa cancelar y elige volver
- **THEN** la SPA no envía el POST y el turno sigue sin cambios

### Requirement: Errores recuperables de la gestión

Ante un 429 o un error de red al confirmar o cancelar, la SPA SHALL mostrar un aviso, conservar la vista del turno y permitir un nuevo intento manual, sin reintentar automáticamente.

#### Scenario: El límite de solicitudes permite reintentar la acción
- **WHEN** la API responde 429 al confirmar o cancelar
- **THEN** la pantalla muestra un aviso, conserva el turno y permite reintentar manualmente

### Requirement: El token no sale de la pantalla

La SPA MUST NOT guardar el token en almacenamiento persistente ni en cookies, MUST NOT incluirlo en el título del documento y MUST NOT enviarlo a otro origen que la API. La SPA SHALL declarar la política de referencia `no-referrer`.

#### Scenario: El token no queda en el almacenamiento del navegador
- **WHEN** la persona abre `/turno/{token}` y gestiona su turno
- **THEN** ni `localStorage`, ni `sessionStorage`, ni las cookies, ni el título del documento contienen el token

#### Scenario: La SPA declara la política de referencia sin referer
- **WHEN** se carga cualquier página de la SPA
- **THEN** el documento declara la política de referencia `no-referrer`

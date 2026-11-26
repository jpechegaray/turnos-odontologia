# Proposal

## Why

El backend ya permite gestionar un turno con su token (C-39: `/api/public/turnos/{token}`) y tomar un hueco desde la oferta de la lista de espera (C-42: `/api/public/ofertas/{token}`). Sin embargo, la SPA todavía no tiene pantallas para esos enlaces. El enlace `/turno/{token}` que muestra la reserva pública (C-48) y los que llegan por email terminan en una ruta inexistente. C-49 es el último eslabón funcional del camino crítico del MVP (US-016 y US-022).

## What Changes

- Nueva ruta pública `/turno/:token`. El paciente ve su turno (horario en Buenos Aires, profesional, prestación y estado) y puede confirmarlo o cancelarlo. Antes de cancelar se pide una confirmación que avisa si la cancelación será tardía (`cancelacionSeriaTardia`). No hay opción de reprogramar.
- Nueva ruta pública `/oferta/:token`. El paciente ve el hueco ofrecido y lo toma con un solo pedido. Se muestran los estados `TOMADA`, `VENCIDA` y `NO_DISPONIBLE` sin botón para tomar. Los rechazos 409 de la toma (`HUECO_YA_TOMADO`, `OFERTA_VENCIDA`, `OFERTA_NO_DISPONIBLE`) se informan con un mensaje claro. Si la toma sale bien, se muestra el turno y el enlace relativo `/turno/{token}`.
- Hay una única pantalla genérica para token inválido, vencido o revocado (404). Es la misma en las dos rutas y no da pistas sobre el motivo.
- Protección del token en la SPA. El token no se guarda en `localStorage`, `sessionStorage` ni cookies. No aparece en el título del documento ni se envía a servicios externos. La SPA declara `Referrer-Policy: no-referrer` mediante `<meta name="referrer">` en `index.html`.
- Se agregan tests de Vitest con los nombres exactos de los escenarios.

## Capabilities

### New Capabilities
- `gestion-turno-token-frontend`: pantalla pública con la que el paciente ve, confirma y cancela su turno desde el enlace con token.
- `oferta-hueco-frontend`: pantalla pública con la que el paciente ve y toma un hueco ofrecido por la lista de espera.

### Modified Capabilities
- (ninguna)

## Impact

- Frontend: nuevas vistas en `frontend/src/features/reserva-publica/` (subcarpetas `turno-token/` y `oferta/`) con su registro en `routes.ts`, y la etiqueta `<meta name="referrer">` en `frontend/index.html`.
- API: se consumen sin cambios los endpoints públicos de C-39 y C-42. No se toca el backend.
- Gobernanza HIGH: hay que revisar este proposal antes de implementar.
- Design system en modo `system-only`: no hay gate de mockups. Se usa el sistema aprobado (revisión 1) y sus tokens.

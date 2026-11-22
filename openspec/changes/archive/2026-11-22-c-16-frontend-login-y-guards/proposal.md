# Proposal

## Why

El backend autentica al personal (C-03) y la SPA ya tiene router, layout y cliente HTTP (C-09), pero no hay un flujo de ingreso ni navegación adaptada al rol. C-16 habilita esa base para las próximas pantallas del personal (US-001, RN-33 y RN-34).

## What Changes

- Incorporar `/login`, restauración de sesión con `/api/auth/me`, cierre de sesión y redirección a login cuando se pierde la sesión en la zona interna.
- Aplicar `RequireRole` a las rutas del personal y filtrar el menú con la misma regla; `ODONTOLOGO` dispone solo de opciones operativas de lectura. El backend conserva la autorización efectiva.
- Mostrar un único error de credenciales con ayuda general sobre reintentos, incluso si la cuenta está bloqueada. No inferir ni revelar el estado de la cuenta.
- Conservar el acceso público sin sesión; no implementar pantallas de agenda, configuración, usuarios ni reserva pública. Usar Inicio como destino tras el login mientras no exista la agenda.

## Capabilities

### New Capabilities

- `sesion-frontend-personal`: ingreso, restauración y cierre de sesión, navegación y mensajes del personal en la SPA.

### Modified Capabilities

Ninguna spec principal cambia: `control-acceso` sigue siendo el contrato de la API. La nueva capacidad agrega la política de acceso de la SPA sobre el registro de rutas de C-09, aún pendiente de archive.

## Impact

- Frontend: `features/auth`, composición del router y layout, cliente de C-09 y tests Vitest. Reutiliza React Router, el kit UI y los tokens existentes.
- Backend: consume el contrato de C-03 sin cambios en endpoints, cookies, política de bloqueo ni dependencias.
- Gobernanza HIGH: revisar la propuesta antes de implementar. Verificar el flujo completo con backend real en desarrollo y usuarios ficticios.

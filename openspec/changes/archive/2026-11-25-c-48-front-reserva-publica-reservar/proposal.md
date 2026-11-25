# Propuesta

## Why

US-015 requiere que el paciente pueda reservar desde su celular sin llamar al consultorio. C-38 y C-41 ya ofrecen las operaciones públicas de consulta y reserva.

## What Changes

- Agregar `/reservar` con pasos para elegir prestación, profesional, fecha y horario y completar los datos y el consentimiento de PA-08.
- Si el horario se ocupa, mostrar las alternativas del servidor y conservar los datos ingresados mientras el paciente elige otro horario.
- Confirmar el turno con su fecha y hora de Buenos Aires y el enlace de gestión que corresponde al token devuelto. La gestión del turno es alcance de C-49.
- Alinear el cliente de la SPA con la excepción de CSRF que ya establece el backend para las rutas públicas.

## Capabilities

### New Capabilities

- `reserva-publica-frontend` cubre el flujo de reserva anónima, el consentimiento y la confirmación.

### Modified Capabilities

- `cliente-api-frontend` documenta la excepción de CSRF para el espacio de rutas públicas de la API.

## Impact

Frontend de reserva pública, registro de rutas por feature y cliente compartido. Usa el kit y los tokens de C-09, la disponibilidad de C-38 y la API de reserva de C-41.

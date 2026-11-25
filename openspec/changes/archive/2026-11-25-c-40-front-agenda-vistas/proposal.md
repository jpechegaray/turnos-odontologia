# Proposal

## Why

El personal ya puede consultar la agenda por API (C-28), pero no tiene una pantalla para verla. El odontólogo necesita ver su día y su semana para saber a quién atiende y cuándo (US-013), en hora de Buenos Aires (RN-14) y sin ninguna acción de escritura (RN-33). C-40 crea la feature `agenda` del frontend que después continúan C-45 y C-46.

## What Changes

- Agregar la pantalla `/agenda` para `ADMIN`, `RECEPCION` y `ODONTOLOGO`, con una entrada de menú "Agenda".
- Vista diaria en grilla de 15 minutos, con una columna por profesional o por sillón, y vista semanal de lunes a domingo, con una columna por día. Ambas consumen `GET /api/agenda`.
- Navegar al día o semana anterior y siguiente, volver a hoy, elegir una fecha y filtrar por profesional y por sillón.
- Mostrar los turnos con el estado del design system (color, ícono y texto), los horarios de atención y los bloqueos. Toda hora se muestra en Buenos Aires.
- Abrir la ficha básica del paciente de un turno con `GET /api/pacientes/{id}`, en solo lectura. Si la API oculta el paciente (turnos de otro profesional para el odontólogo), no se ofrece la ficha.
- No hay acciones de escritura en esta pantalla para ningún rol. Alta de turnos desde huecos libres, cancelación y reprogramación quedan para C-45 y C-46.

## Capabilities

### New Capabilities

- `agenda-frontend`: pantalla de agenda diaria y semanal del personal, con filtros, estados, horarios, bloqueos y ficha básica del paciente.

### Modified Capabilities

Ninguna.

## Impact

Solo cambia el frontend, en `frontend/src/features/agenda/`, más dos funciones nuevas en `shared/fecha/zonaHoraria.ts` (fecha local y minutos del día en Buenos Aires). No cambia el backend ni la seguridad: la API ya responde `403` a toda escritura del odontólogo y oculta los pacientes ajenos. Se reutilizan el kit UI de C-09, las rutas por feature y la sesión del personal.

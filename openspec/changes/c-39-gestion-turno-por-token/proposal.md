# Proposal

## Why

El paciente necesita consultar, confirmar o cancelar su turno desde el enlace, sin cuenta ni intervención de recepción (US-016). C-17, C-27 y C-32 ya aportan el límite por IP, la cancelación de agenda y el ciclo de vida de la credencial; falta conectarlos a una API que no exponga otros datos.

## What Changes

- Incorporar `GET /api/public/turnos/{token}` y los dos `POST` bajo la misma ruta: `/confirmar` y `/cancelar`. El turno se identifica únicamente por el token, sin sesión ni CSRF.
- Informar en la consulta si cancelar ahora sería tardío. Permitir cancelar también fuera del plazo, registrar `PACIENTE_TOKEN`, liberar el intervalo y revocar los tokens del turno.
- Aplicar el límite por IP existente, conservar el rechazo uniforme `404` y evitar que el secreto llegue a respuestas, cachés o registros.
- No incluir reprogramación pública, emisión de tokens, reserva pública, cambios en el proceso de ofertas ni UI (C-49).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `token-turno`: contrato HTTP, proyección pública mínima, operaciones permitidas y protección del secreto en el límite HTTP.
- `agenda-turnos`: aclarar que sesión/CSRF y el conflicto entre cancelaciones del personal corresponden a la ruta interna; la ruta pública aplica las reglas de vigencia del token.

## Impact

- Backend: `reservapublica.gestiontoken`, agenda y protección de datos en el manejo HTTP de las nuevas rutas. Se reutilizan C-17, C-27 y C-32; sin cambios de esquema ni nuevas dependencias.
- C-49 consume la proyección pública y el aviso de tardanza; la cancelación sigue usando los eventos de agenda, sin devolver información de la lista de espera al paciente.
- Gobernanza CRITICAL: esta propuesta no autoriza implementar. Se requiere aprobación explícita de Juan Pablo antes de escribir código.

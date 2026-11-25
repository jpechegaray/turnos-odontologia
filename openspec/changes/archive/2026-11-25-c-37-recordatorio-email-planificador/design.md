# Design

## Context

C-15 registra PENDIENTE con una clave única y reclama reintentos con bloqueo de filas. Su reconstructor actual usa un resultado vacío para indicar que no reconoce la notificación, con un mensaje mínimo como alternativa. C-32 guarda solo hashes de tokens y permite más de un token por turno. La cancelación revoca los tokens del turno y la reprogramación actualiza su vencimiento.

## Goals / Non-Goals

Enviar un recordatorio por horario elegible y mantener los enlaces ya entregados. El cambio no implementa las pantallas ni los endpoints públicos de gestión por token.

## Decisions

1. El planificador programado de C-15 encola los recordatorios antes de procesar las notificaciones pendientes del mismo ciclo. Desde un Clock inyectable, consulta los turnos PROGRAMADO o CONFIRMADO con inicio futuro y límite superior inclusivo. Lee la anticipación de C-08 y omite pacientes sin email. PA-05 confirmada: 24 horas por defecto, configurable, un recordatorio por inicio.
2. Registrar PENDIENTE con la identidad de C-15 (turno, RECORDATORIO, inicio de referencia). Dejar que su procesamiento de vencidas reclame cada pendiente y aplique hasta tres intentos totales con espera creciente. La cola registra una sola fila por turno e inicio aun con ejecuciones concurrentes. Una respuesta ambigua de SMTP no garantiza entrega exactamente una vez.
3. Antes de cada intento, comprobar turno actual, estado, inicio de referencia, vencimiento y email. Distinguir explícitamente un recordatorio obsoleto de un tipo sin reconstructor. El obsoleto debe quedar terminal sin llamar a SMTP ni enviar el mensaje mínimo. El nuevo inicio puede tener su propio pendiente.
4. Cada intento válido emite un token nuevo con ServicioTokenTurno, también para turnos internos, y arma /turno/{token} en memoria. Juan Pablo confirmó esta política el 2026-11-25. C-32 no permite recuperar el secreto original. No se cambia el ciclo de vida de tokens existentes ni se persiste el enlace. Los errores del envío se guardan sin el enlace ni el secreto.
5. Probar sobre PostgreSQL con reloj controlado y correo en memoria. La emisión y la elegibilidad deben observar el mismo estado del turno, respetando los bloqueos que ya usan cancelación, reprogramación y C-32.

## Risks / Trade-offs

Un reintento puede generar otro token para ese turno. Esto evita almacenar secretos recuperables y conserva la vigencia de los enlaces ya enviados. Los tokens siguen sujetos a las reglas de C-32.

## Migration Plan

Reutilizar la cola y su clave única de C-15. Sin migración de datos.

## Open Questions

- Ninguna para C-37. El endpoint y la pantalla del enlace corresponden a C-39 y C-49.

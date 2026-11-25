# Design — Token de turno

## Context

Ver [proposal.md](proposal.md) y el [delta `token-turno`](specs/token-turno/spec.md). En `master`, C-14 aporta `ServicioToken` y `TokenGenerado` sin dependencias de frameworks. C-19 aporta `TurnoJpa`, `TurnosPersistencia` y los tipos `TurnoCancelado` (con `canceladoEn`) y `TurnoReprogramado` (con `nuevo().inicio()`). Hoy se publica `TurnoCreado`; C-27 y C-35 implementarán los productores de los otros eventos.

## Goals / Non-Goals

**Goals:** mantener la propiedad del turno en agenda, reutilizar el componente criptográfico compartido y hacer que los cambios de vigencia sean atómicos con los cambios de agenda.

**Non-Goals:** no diseñar en este change la recuperación o reemisión para recordatorios, el transporte del enlace por HTTP o correo, ni las políticas de confirmación, cancelación o reprogramación del turno.

## Decisions

1. **Persistencia en `reservapublica.token`.** `TokenTurno` tendrá FK obligatoria a `turno`, unicidad de `token_hash`, índice no único por `turno_id` y los instantes `Instant`/`timestamptz` de la KB. Hibernate administra el esquema según DD-08. La KB permite 0..N tokens por turno; asociar un token a un solo turno no implica una fila única por turno. Se descarta guardar el secreto o imponer una restricción de unicidad sobre `turno_id`.
2. **Entrada explícita y acotada.** `ServicioTokenTurno` emite solo bajo pedido y obtiene estado e inicio vigentes mediante un puerto de aplicación de agenda, sin exponer `TurnoJpa`. Reutiliza C-14 y entrega el secreto únicamente al consumidor de la emisión mediante un resultado cuya representación textual lo oculta. Se descarta emitir al escuchar `TurnoCreado`: ese flujo es interno y hoy no hay un destinatario autorizado para el secreto.
3. **Validar por hash y devolver solo el ID.** Comprobar el formato base64url de C-14, buscar por hash parametrizado y delegar la verificación de coincidencia en `ServicioToken`. Tomar un `Clock` inyectado y exigir `ahora < expira_en`; verificar revocación y, si se suministra, el turno esperado antes de actualizar `ultimo_uso_en`. Todos los rechazos de credencial usan `ReglaNegocioException` con `NO_ENCONTRADO_404`, código y detalle fijos sin propiedades variables; no crear un controlador ni capturar fallos de infraestructura como si fueran rechazos. C-39 deberá construir su operación desde el ID validado o contrastar el ID esperado dentro de su transacción.
4. **Una transacción para el turno y sus tokens.** Los escuchas síncronos se ejecutan en `BEFORE_COMMIT` solo con una transacción activa y participan en ella, sin `REQUIRES_NEW`. Actualizan únicamente las filas no revocadas del turno del evento; cancelación conserva su primera marca y reprogramación usa el nuevo inicio sin rotar el secreto. Se descarta `AFTER_COMMIT`: podría dejar cambios de agenda confirmados con credenciales desactualizadas si falla la escritura del token.
5. **Orden fijo para escrituras concurrentes.** Emisión y validación coordinan por fila de turno a través del puerto de agenda, con un lock de escritura. La primera búsqueda de validación solo proyecta el ID del turno; cargar el token y consultar el reloj después de tomar el lock evita decidir con datos anteriores a la espera. El orden es turno → token. C-27 y C-35 deberán mantener ese orden, adquirir el lock del turno antes de publicar y conservarlo hasta el commit. Esto evita emitir con un inicio ya reemplazado o perder una revocación al escribir el último uso. Usar una actualización de campos acotada y probar el contrato con PostgreSQL y productores simulados que respeten el orden.

## Risks / Trade-offs

- **Los productores aún no están en `master`** → probar los escuchas publicando los eventos reales en transacciones de prueba; C-27 y C-35 verifican la conexión con sus casos de uso reales.
- **El secreto no se puede recuperar para un recordatorio** → C-37 debe definir cómo construye el enlace para un turno con token previo, sin persistir el texto en claro ni cambiar el enlace por efecto de una reprogramación. No se agrega un mecanismo de recuperación en C-32.
- **Un token en la ruta puede aparecer en logs o en el campo `instance` de un error HTTP** → C-32 no agrega rutas y prueba que su servicio no filtra el secreto; C-39 deberá verificar y, si corresponde, sanear esos puntos al introducir rutas con token.
- **Serializar por turno agrega contención por fila** → mantener la transacción breve, sin llamadas a correo ni red mientras sostiene el lock.

## Migration Plan

Hibernate agrega `token_turno` a partir del mapping; no hay carga retroactiva ni cambios sobre las filas de `turno`. Validar el arranque en PostgreSQL 16 con los perfiles del repo. Si se revierte antes de desplegar consumidores, dejar la tabla aditiva sin uso y no eliminarla automáticamente.

# Verificación de C-44

**Veredicto:** listo para archivar tras el merge del PR de implementación.

## Validación estructural

`openspec validate c-44-emails-cancelacion-reprogramacion --strict` pasa.

## Tareas

4 de 4 completas.

## Matriz de escenarios

Todos los escenarios de la delta `notificaciones-email` se verifican con PostgreSQL y el adaptador de correo en memoria en `AvisosCambiosTurnoIT`.

| Escenario | Prueba |
|---|---|
| La cancelación del personal envía un solo correo | `cancelacionIdempotente` |
| La reprogramación avisa el nuevo horario y descarta el anterior | `reprogramacionConAvisoYRecordatorioNuevo` |
| Un cambio sin email descarta el recordatorio sin enviar avisos | `cambiosSinEmail` |
| Una cancelación por token descarta el recordatorio sin enviar aviso | `cancelacionPorToken` |
| Una operación revertida no envía avisos ni descarta recordatorios | `cancelacionRevertida` |
| Un aviso fallido se reintenta con la misma fecha | `reintento` |
| Una reprogramación con el mismo inicio conserva su recordatorio | `conservaRecordatorioConMismoInicio` |
| Volver al inicio anterior permite retomar su recordatorio descartado | `volverAlHorarioInicial` |

## Ejecución

- Ciclo inicial de TDD con `mvnw.cmd -q -Dit.test=AvisosCambiosTurnoIT verify`. Se observaron seis fallos por la ausencia del aviso o del descarte.
- Prueba dirigida tras la implementación. Ocho escenarios pasan.
- Backend con `mvnw.cmd -B -ntp clean verify`. Pasan 352 pruebas unitarias y 713 de integración, sin fallos ni omisiones.
- Frontend. Pasan `npm run lint`, las 369 pruebas de Vitest y `npm run build`.

## Reglas de negocio y diseño

- RN-30. Se reutiliza la clave única de C-15 y se conserva el historial de envíos. Un recordatorio del horario anterior deja de estar programado y el planificador puede generar el del nuevo horario.
- RN-31. La cancelación por el personal y la reprogramación con email generan avisos. La cancelación por token descarta el recordatorio según el flujo 3.
- El cambio usa datos ficticios y el adaptador en memoria. La entrega por SMTP real queda fuera de esta ejecución.
- El envío ocurre posterior al commit. Los avisos registrados pueden reintentarse con el planificador de C-15.

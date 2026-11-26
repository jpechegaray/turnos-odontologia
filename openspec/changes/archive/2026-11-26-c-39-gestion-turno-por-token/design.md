# Design

## Context

`ServicioTokenTurno.validar` resuelve el ID, bloquea la fila del turno, verifica vigencia y registra último uso. `CancelarTurnoService` ya separa la entrada del personal del núcleo que recibe `ActorCancelacion`; publica `TurnoCancelado` y `HuecoLiberado`. C-32 ya revoca los tokens mediante el evento de cancelación. C-17 ya filtra `/api/public/**`, y `SecurityConfig` admite esas rutas sin sesión ni CSRF.

## Goals / Non-Goals

**Objetivo:** conservar las reglas de agenda y el alcance de la credencial de extremo a extremo, con un contrato HTTP mínimo para C-49.

**Fuera de alcance:** cambios en las rutas internas, el almacenamiento de tokens, el flujo de ofertas o la UI.

## Decisions

1. **Adaptador en `reservapublica.gestiontoken` y proyección explícita.** Un servicio de aplicación define la frontera transaccional y pasa a agenda únicamente el ID devuelto por el validador. Un puerto de agenda permite consultar el turno y los nombres para mostrar sin serializar entidades ni el DTO del personal. No se incorpora un ID de turno a la URL ni al cuerpo: no hay un segundo selector que pueda contradecir el alcance del token.
2. **Una transacción por operación, también para el `GET`.** La validación escribe `ultimo_uso_en`, así que la transacción no es de solo lectura. El bloqueo existente acompaña validación, lectura o transición; no se autoriza en una transacción y se escribe en otra. Confirmar aplica `Turno.pasarA(CONFIRMADO)` con el mecanismo de persistencia de agenda. Cancelar delega al núcleo de C-27 con `PACIENTE_TOKEN` y sin usuario ni motivo. Si se cruza el inicio entre la validación y el control de la mutación, se rechaza con el `404` uniforme, sin cambios. Se mantiene el orden turno → tokens de C-32; no se introduce un segundo mecanismo de revocación.
3. **Horario y aviso con la misma política.** El `GET` calcula `cancelacionSeriaTardia` con `PoliticaCancelacion`, `ConfiguracionServicio` y `Clock`; el `POST` vuelve a calcular con el valor vigente. El aviso es informativo, no una autorización ni un valor enviado por el cliente.
4. **Protección en todas las salidas HTTP.** Colocar la protección `no-store` de la familia de rutas antes del limitador para que también cubra `429`. En errores MVC y de enrutamiento usar una referencia pública fija o eliminar `instance`; no confiar en el valor automático de Spring MVC. Para esta familia, evitar registrar la URI cruda o volcados de solicitud/excepciones que puedan contenerla. Mantener los diagnósticos no sensibles y el comportamiento de las otras rutas. Los registros de acceso habilitados en servidor o proxy deben omitir o enmascarar la ruta con credencial. No cambiar el token de lugar ni ampliar el límite de C-17.

## Risks / Trade-offs

- El `GET` toma el mismo bloqueo que las escrituras → mantener corta la transacción y evitar llamadas externas desde ella.
- El plazo puede cruzarse o cambiar entre la consulta y el `POST` → el registro usa exclusivamente el cálculo del servidor al cancelar.
- Un error o componente de infraestructura puede registrar la URI aun sin logs en el controlador → probar también rutas no mapeadas, rechazos de filtro y un fallo controlado; revisar la configuración de acceso habilitada antes de publicar.

## Migration Plan

No hay migración de datos. Desplegar con el control de caché y la protección de registros habilitados; verificar el contrato con tokens ficticios antes de publicar enlaces. Revertir el despliegue retira las rutas nuevas, pero no deshace confirmaciones o cancelaciones ya registradas.

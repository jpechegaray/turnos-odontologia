# Tasks

**Antes de aplicar:** obtener la aprobación explícita de Juan Pablo (gobernanza crítica). Usar TDD estricto, `Clock` controlado, datos ficticios y el título exacto de cada escenario en `@DisplayName`; PostgreSQL real con Testcontainers para persistencia y transacciones.

## 1. Persistencia y emisión

- [x] 1.1 Mapear `TokenTurno` y su repositorio con Hibernate; verificar en PostgreSQL los metadatos, el índice por turno y los rechazos por hash duplicado y FK inexistente.
- [x] 1.2 Crear el puerto de agenda para consultar y bloquear un turno por ID, sin exponer la entidad JPA, y usarlo al emitir mediante C-14. Verificar que se entrega el secreto solo al emitir, que se persiste su hash con los metadatos correctos y que no se emite para turnos inexistentes, cancelados o iniciados.

## 2. Validación y confidencialidad

- [x] 2.1 Resolver exclusivamente el turno del token y registrar `ultimo_uso_en` solo con validación exitosa. Verificar reutilización antes de vencer, límite exacto de inicio, rechazo de cruce de turnos y ausencia de cambios ante rechazo.
- [x] 2.2 Unificar rechazos por formato, inexistencia, vencimiento, revocación y turno ajeno. Verificar igualdad de estado `404`, código y detalle; capturar logs de emisión, uso válido y rechazo y comprobar que ni los logs ni los errores o representaciones de texto contienen el secreto ficticio.

## 3. Ciclo de vida

- [x] 3.1 Conectar `TurnoCancelado` y `TurnoReprogramado` con la revocación y el recálculo dentro de la transacción. Verificar con publicación de eventos en PostgreSQL el aislamiento por turno, la cancelación repetida, la conservación del enlace ante adelanto y postergación, la no rehabilitación y los turnos sin token.
- [x] 3.2 Verificar con PostgreSQL que revertir cualquiera de los eventos no cambia tokens y que una validación concurrente con cancelación no pisa la revocación, y que la emisión concurrente con reprogramación usa el inicio vigente.

## 4. Verificación transversal

- [x] 4.1 Ejecutar `mvn verify` en `backend/` y `openspec validate c-32-token-turno-emision-y-validacion --strict`; verificar regresión de C-14 y C-19 y confirmar que no se agregaron rutas ni se cambió el alta interna.

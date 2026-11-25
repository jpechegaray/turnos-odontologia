# Tasks

Previo a `/opsx:apply`: obtener la aprobación explícita de Juan Pablo. Para cada tarea aplicar TDD estricto, con `@DisplayName` igual al escenario de la delta spec.

## 1. Consulta y alcance

- [x] 1.1 Incorporar la consulta transaccional por token y la proyección de agenda con lista explícita de campos. Verificar por HTTP la consulta anónima, el aislamiento entre dos turnos, el rechazo de rutas no admitidas y el borde exacto de la tardanza con `Clock` inyectado.
- [x] 1.2 Reutilizar la validación de C-32 en la frontera pública. Verificar en el `GET` la igualdad del Problem Details para credenciales malformadas, inexistentes, vencidas exactamente al inicio y revocadas, y que un rechazo no cambie el último uso.

## 2. Confirmación y cancelación

- [x] 2.1 Implementar la confirmación sin sesión ni CSRF con la transición de agenda. Verificar persistencia de `CONFIRMADO`, reutilización del enlace, conflicto al reconfirmar y rechazo uniforme de tokens no vigentes.
- [x] 2.2 Delegar la cancelación pública a C-27 en la misma transacción de validación. Verificar con PostgreSQL la auditoría `PACIENTE_TOKEN`, la tardanza, la reutilización del intervalo al cancelar en plazo y fuera de plazo, y la revocación de todos los enlaces. Cubrir el cruce del inicio sin cambios y dos cancelaciones competidoras con un único par de eventos. Mantener verdes los casos de sesión, rol, CSRF y concurrencia de la ruta interna.

## 3. Protección HTTP y comprobación integrada

- [x] 3.1 Aplicar `no-store` y el tratamiento de rutas sensibles antes del límite C-17. Verificar que `429` con `Retry-After` se aplica a las tres operaciones sin ejecutarlas y que no se expone el token en respuestas, errores MVC, rutas no mapeadas ni registros de aplicación/acceso, incluyendo un `500` controlado con un token ficticio. Documentar el requisito para registros de proxy si se habilitan.
- [x] 3.2 Ejecutar la suite de backend con `mvn verify` y validar la propuesta con `openspec validate c-39-gestion-turno-por-token --strict`. Comprobar el contrato HTTP de las tres operaciones y el mapeo de escenarios antes de solicitar `openspec-verify`.

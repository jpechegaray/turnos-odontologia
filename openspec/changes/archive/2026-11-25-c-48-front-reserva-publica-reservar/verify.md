# Verificación de C-48

**Veredicto:** listo para revisión. Las seis tareas están completas.

## Validación estructural

`openspec validate c-48-front-reserva-publica-reservar --strict` pasa.

## Matriz de escenarios

Las pruebas de `PaginaReserva.test.tsx` montan `AppRutas` en `/reservar` y pasan por el cliente compartido. Cada nombre coincide con el escenario de OpenSpec.

| Escenario | Evidencia |
|---|---|
| Una persona sin sesión puede elegir un horario desde el celular | Vitest y Edge con la API real |
| Cambiar prestación o profesional limpia las selecciones dependientes | Vitest, incluida una respuesta tardía |
| Un rango sin horarios permite elegir otras fechas | Vitest |
| El formulario exige aceptar el aviso antes de reservar | Vitest y Edge con teclado y tacto |
| Una versión de consentimiento vencida exige volver a aceptar | Vitest |
| La pantalla bloquea el envío si falta el texto vigente | Vitest |
| Los errores de validación se muestran junto al campo correspondiente | Vitest |
| Un horario ocupado muestra alternativas y conserva el formulario | Vitest y Edge con una reserva concurrente real |
| Un conflicto sin alternativas permite volver a buscar | Vitest |
| Una reserva confirmada muestra el resumen y el enlace | Vitest y Edge con una respuesta 201 real |
| Un segundo envío queda bloqueado mientras se confirma la reserva | Vitest |
| El límite de solicitudes conserva el formulario y permite reintentar | Vitest |

`clienteApi.test.ts` cubre los cuatro escenarios de CSRF de la delta, con y sin cookie.

## Ejecución

- TDD. La prueba nueva del cliente detectó el pedido adicional de CSRF antes del cambio. Los escenarios de reserva se escribieron antes de agregar la ruta y la pantalla.
- Frontend. Lint y build correctos. Pasan 384 pruebas de Vitest en 42 archivos.
- Backend. Pasan 367 pruebas unitarias y 756 de integración con `mvnw.cmd -B -ntp clean verify`, sin fallos ni omisiones.
- Edge a 390 × 844 con el frontend compilado, Spring Boot y PostgreSQL 16 aislado. Se comprobó el foco visible por teclado, la interacción táctil, el error de consentimiento, un 409 real, la conservación del formulario, el 201 y el enlace. Las pantallas recorridas no presentaron desplazamiento horizontal.

## Reglas de negocio y diseño

- US-015 y PA-08. El flujo público omite DNI, exige la aceptación explícita y envía la versión del aviso mostrado. Las horas se presentan en Buenos Aires.
- La SPA mantiene los datos y el token en memoria. La prueba móvil confirma que el almacenamiento persistente queda vacío.
- Las pruebas usaron exclusivamente datos ficticios y adaptador de correo en memoria. La pantalla de gestión vinculada corresponde a C-49.

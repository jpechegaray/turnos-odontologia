# Tasks

## 1. Persistencia y lectura

- [x] 1.1 Con TDD estricto, implementar el catálogo de siete claves, la entidad, el repositorio y el inicializador. Verificar en PostgreSQL real el arranque en una base vacía y la reposición de una clave ausente sin alterar el valor ni la fecha de una clave editada.
- [x] 1.2 Exponer las siete lecturas tipadas sin caché en `ConfiguracionServicio`. Con un consumidor de prueba, verificar que lee los defaults y, tras cambiar valores persistidos, obtiene los vigentes sin modificar una decisión anterior.

## 2. Reemplazo y validación

- [x] 2.1 Implementar el reemplazo completo en una transacción, con bloqueo ordenado y `Clock` inyectado. Verificar en PostgreSQL real la persistencia de cambios, la idempotencia de valores y fechas y dos reemplazos concurrentes con modificaciones distintas respecto del estado inicial.
- [x] 2.2 Implementar la validación del catálogo de RN-36 y cubrir con pruebas parametrizadas los extremos válidos y sus vecinos inválidos, los límites positivos, fracciones, desbordes y nombre en blanco. Comprobar que combinar un valor válido y uno inválido no modifica valores ni fechas.

## 3. API y permisos

- [x] 3.1 Implementar `GET /api/configuracion` con el objeto JSON plano y verificar 200 para `ADMIN` y `RECEPCION`, 403 para `ODONTOLOGO` y 401 para anónimo con MockMvc y la cadena de seguridad real.
- [x] 3.2 Implementar `PUT /api/configuracion` con adaptación JSON local y seguridad por método. Verificar 200 y lectura posterior para `ADMIN` con CSRF, 403 para roles no autorizados o CSRF inválido, 400 para estructura inválida y 422 con `RN-36` para valores inválidos; comprobar que las solicitudes rechazadas no modifican la base.

## 4. Verificación integrada

- [x] 4.1 Ejecutar `.\mvnw.cmd verify` desde `backend/` con Docker disponible; verificar que la suite completa queda verde, cada escenario tiene su `@DisplayName` exacto y `openspec validate c-08-configuracion-parametros --strict` pasa.

## 5. Correcciones de la verificación

- [x] 5.1 Rechazar `U+0000` en el nombre antes de persistir. Verificar 422 `RN-36` y las siete filas intactas en un PUT con otro cambio válido.
- [x] 5.2 Verificar que un PUT actualiza las fechas de todas las claves modificadas y conserva las demás, y que un consumidor observa el reemplazo real sin alterar su decisión registrada.

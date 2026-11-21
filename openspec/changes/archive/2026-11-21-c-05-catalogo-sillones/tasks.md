# Tasks

Cada tarea de implementación empieza con un test que falle, continúa con el mínimo código que lo haga pasar y termina con triangulación (caso habitual y borde) y refactorización con la suite en verde. Antes de tocar código existente, correr los tests afectados y registrar la línea base. Usar JUnit 5, AssertJ y datos ficticios; cada escenario de la delta debe tener un `@DisplayName` con su nombre exacto.

## 1. Persistencia y consulta de agenda

- [x] 1.1 Crear primero tests `@DataJpaTest` sobre PostgreSQL 16 real, reutilizando `ContenedoresPrueba` y sin reemplazar el datasource. Implementar entidad y repositorio internos; verificar ID generado y unicidad de `uk_sillon_nombre` al forzar `flush`, tanto para nombres de activos como de inactivos, y que nombres distintos se admiten.
- [x] 1.2 Definir `SillonConsulta` y su adaptador con lecturas `readOnly`, filtrado en base y resultados sin objetos JPA. Verificar con Postgres los escenarios «Agenda deja de ofrecer un sillón desactivado», «Agenda vuelve a ofrecer un sillón reactivado» y «Agenda distingue un sillón inactivo de uno inexistente». Comprobar el resultado del puerto después de persistir cada transición.

## 2. Contrato REST y permisos

- [x] 2.1 Implementar DTOs y consultas REST del servicio con roles por método, tras crear pruebas MockMvc parametrizadas por rol con el servicio protegido y la configuración de seguridad compartida. Verificar «Los tres roles consultan sillones activos e inactivos» y «Consultar un ID inexistente devuelve 404».
- [x] 2.2 Implementar alta y actualización transaccionales, validación de DTOs, normalización de nombre y traducción de la restricción específica. Verificar con MockMvc alta `201` con ubicación, edición `200`, entrada inválida `400`, ID inexistente `404` y duplicados al crear o renombrar `409` conforme a sus escenarios. Verificar «Dos altas concurrentes del mismo nombre no crean duplicados» con PostgreSQL real y dos solicitudes en transacciones independientes; comprobar desde una tercera transacción que quedó una fila. Comprobar en el servicio que otras excepciones de integridad no se convierten en conflicto de nombre.
- [x] 2.3 Implementar y verificar «Desactivar un sillón conserva su registro» y «Reactivar un sillón conserva su registro» contra el servicio y PostgreSQL. Usar MockMvc para comprobar `PUT`, respuesta, listado y detalle, y consultar el puerto después de cada escritura para comprobar que el mismo ID sale y vuelve a entrar en el listado de activos. No agregar lógica de turnos futuros.
- [x] 2.4 Verificar «Recepción y odontología no pueden escribir en el catálogo», «Un cliente sin sesión no puede acceder al catálogo» y «Un administrador sin CSRF válido no puede escribir en el catálogo» mediante MockMvc y la cadena compartida. Parametrizar `POST` y `PUT`, incluyendo edición y cambios de estado, y verificar ausencia de efectos. Para aislar autorización por rol, enviar CSRF válido; para aislar autenticación sobre C-01, cubrir también escritura anónima con CSRF válido.

## 3. Verificación de integración

- [x] 3.1 Cuando el contrato C-03 esté disponible en una rama que incluya ambos changes, comprobar sobre la cadena real que un administrador con sesión escribe usando `XSRF-TOKEN` y `X-XSRF-TOKEN`, que sin token válido recibe `403` y que una escritura anónima sin token recibe `401`. Si C-03 aún no está disponible, reportar esta comprobación como pendiente sin reimplementar autenticación ni bloquear el desarrollo aislado.
- [x] 3.2 Correr `mvn verify` desde `backend/` con Docker disponible, verificar que la suite unitaria, ArchUnit y las integraciones PostgreSQL pasan, y mapear cada escenario de la delta a un test con su nombre exacto para `openspec-verify`. Distinguir las verificaciones de C-05 de las pendientes de integración con C-03 y de alcance de C-30.

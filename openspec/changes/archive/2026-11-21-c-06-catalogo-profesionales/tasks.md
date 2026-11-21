# Tasks

Para cada tarea de implementación, correr la línea base pertinente antes de tocar archivos existentes y seguir RED → GREEN → triangulación → refactor con TDD estricto. Los tests JUnit llevan `@DisplayName` con el nombre exacto del escenario de la [delta](specs/catalogo-profesionales/spec.md); los datos son ficticios.

## 1. Persistencia y consultas internas

- [x] 1.1 Crear la entidad `Profesional` y su persistencia encapsulada. Comprobar con PostgreSQL/Testcontainers la generación de ID, especialidad nullable y restricción única de matrícula también sobre filas inactivas.
- [x] 1.2 Publicar `ProfesionalConsulta` con proyecciones inmutables. Comprobar en integración «La consulta interna de candidatos excluye inactivos» (incluido orden y listado vacío) y «La consulta interna por ID distingue inactivo de inexistente».

## 2. Catálogo HTTP

- [x] 2.1 Implementar alta y actualización con DTOs, validación y autorización exclusiva de `ADMIN`. Cubrir con MockMvc y persistencia real alta `201` con `Location`, actualización `200` con mismo ID, validación `400` sin cambios y ambos escenarios de matrícula duplicada con rollback y código `409`.
- [x] 2.2 Implementar estados por `PUT` y dejar un punto de extensión para C-30. Verificar «El administrador desactiva y reactiva el mismo registro» y «El catálogo no permite borrar físicamente un profesional», confirmando el estado persistido.
- [x] 2.3 Implementar listado y detalle para `ADMIN` y `RECEPCION`, reutilizando la seguridad y Problem Details de C-01. Verificar ambos roles y estados, orden del listado, ausencia `404` en detalle y actualización, consulta anónima `401` y escritura `403` de `RECEPCION` y `ODONTOLOGO` con CSRF válido, sin cambios en los datos.
- [x] 2.4 Implementar el puerto de resolución de titularidad del lado servidor. Verificar ambos endpoints sin proveedor de vínculo y, con un doble de prueba, listado restringido, detalle propio y `403` para ID ajeno existente o inexistente. No implementar el vínculo de C-13.

## 3. Verificación integrada

- [x] 3.1 Ejecutar `.\mvnw.cmd -B verify` desde `backend/` con Docker disponible; comprobar que pasan arquitectura, unitarias e integración y que los 15 escenarios tienen pruebas con nombres exactos. Validar `openspec validate c-06-catalogo-profesionales --strict` y revisar que no se hayan tocado módulos ajenos.

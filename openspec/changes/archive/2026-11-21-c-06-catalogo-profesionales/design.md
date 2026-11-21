# Design

## Context

C-01 aporta Spring Security con autorización por método, Problem Details e Hibernate. No hay un vínculo entre el usuario autenticado y un profesional: C-13 lo incorporará del lado de `Usuario`, según el roadmap, sin editar la entidad del catálogo. C-05 es referencia de consistencia, no una dependencia.

## Goals / Non-Goals

**Goals:** mantener aislada la persistencia del catálogo y publicar un contrato de lectura estable para agenda, horarios, bloqueos y validación de vínculos futuros.

**Non-Goals:** cambiar seguridad global, acceder a tablas de usuarios o turnos desde el catálogo, o crear publicadores de datos para reserva online.

## Decisions

1. **Separar los límites del catálogo.** Usar `profesionales.profesional` con DTOs validados en `web`, casos de uso y puertos en `application`, y entidad y repositorio internos en `infrastructure`. El punto de entrada REST no devuelve entidades y los módulos consumidores no importan repositorios. Generar el ID `BIGINT` por identidad en la tabla `profesional`, con columnas no nulas para nombre, apellido, matrícula y activo, y especialidad nullable. Validar longitudes en concordancia con las columnas y usar `Boolean` obligatorio para `activo` en el DTO de actualización.
2. **Resolver la unicidad en PostgreSQL.** Recortar los extremos de la matrícula antes de persistir y usar una restricción única nombrada (`uk_profesional_matricula`) sin filtro por estado. A diferencia de una preconsulta, la restricción también cubre transacciones concurrentes. Forzar `flush` dentro de la escritura transaccional y traducir solo esa restricción a `ReglaNegocioException` con `409`; reutilizar Problem Details compartido para ese conflicto, validación y `404`.
3. **Separar permisos REST del puerto de lectura.** Usar `@PreAuthorize` por caso de uso REST y conservar las reglas compartidas de sesión y CSRF. Para `ODONTOLOGO`, resolver su ID profesional mediante un puerto de aplicación que recibe la identidad autenticada del servidor y devuelve un opcional. Si no hay proveedor del vínculo, el resultado es vacío y se deniega el acceso. Si lo hay, para el listado consultar solo ese ID y, para el detalle, comparar antes de consultar. No confiar en IDs de la petición como prueba de titularidad ni agregar una dependencia con C-13. Probar el contrato vinculado con un doble del puerto; C-13 proveerá y probará el adaptador real.
4. **Publicar lecturas sin acoplar consumidores.** `ProfesionalConsulta` expone `listarActivos()` y `buscarPorId(Long id)` con proyecciones inmutables; la segunda devuelve un `Optional` y conserva el estado. Filtrar y ordenar el listado de candidatos en PostgreSQL mediante consulta derivada o JPQL parametrizado. El puerto no depende de HTTP ni de la sesión: cada consumidor controla sus permisos y decide si un inactivo es utilizable para su operación.
5. **Conservar la fila en los cambios de estado.** Diferenciar en el caso de uso la transición activo → inactivo para que C-30 pueda insertar la guarda antes de persistirla. No agregar un endpoint `DELETE` ni usar borrado del repositorio.

## Risks / Trade-offs

- Conflictos de matrícula pueden invalidar la transacción → comprobar rollback y estado persistido desde fuera de la transacción fallida.
- No existe vinculación real en C-06 → comprobar denegación con el comportamiento por defecto y titularidad con un doble; no presentar ese doble como prueba de C-13.
- Cambios locales de C-03 y C-05 en el mismo repositorio → limitar el trabajo al subpaquete propio y sus pruebas; verificar sobre C-01 con usuarios de prueba de Spring Security, sin reproducir login ni modificar configuración global.
- RN-37 queda parcial → no considerar implementada la guarda de turnos futuros hasta C-30 ni confundir «activo» con «disponible».

## Migration Plan

Hibernate crea la tabla y su restricción con `update` en desarrollo y `create-drop` en pruebas. No se requieren seeds, migradores ni cambios al SQL de exclusiones. Si se revierte el código en desarrollo, no borrar automáticamente el catálogo cargado.

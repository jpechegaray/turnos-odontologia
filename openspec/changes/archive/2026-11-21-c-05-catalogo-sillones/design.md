# Design

## Context

C-01 aporta autorización por método, Problem Details y el esquema administrado por Hibernate, pero no el login del personal. C-05 agrega la primera persistencia de sillones y un contrato de lectura entre el catálogo y agenda. El comportamiento observable está en [la especificación](specs/catalogo-sillones/spec.md).

## Goals / Non-Goals

**Goals:** aislar la persistencia y las escrituras del catálogo, y dar a agenda un contrato de lectura que no dependa de JPA ni de una sesión HTTP.

**Non-Goals:** modificar la seguridad global, consultar turnos desde el catálogo o implementar selección por disponibilidad en agenda.

## Decisions

1. **Separar API, aplicación y persistencia.** En `profesionales.sillon`, la capa `web` usa DTOs de alta, actualización y respuesta con Bean Validation; `application` coordina las operaciones; `infrastructure` contiene la entidad JPA `Sillon` y el repositorio interno. Se descarta devolver entidades o compartir el repositorio. Persistir en `sillon` con ID `BIGINT` generado por identidad, columnas `snake_case`, nombre y estado no nulos y descripción nullable. Usar `Boolean` obligatorio en el DTO de actualización para distinguir omisión de `false`.
2. **Garantizar la unicidad en PostgreSQL.** Usar una restricción única nombrada (`uk_sillon_nombre`) generada por Hibernate para todos los estados. No decidir unicidad por preconsulta: dos transacciones podrían leer el mismo nombre libre. Quitar espacios de los extremos antes de persistir y conservar la distinción entre mayúsculas y minúsculas; no incorporar equivalencias nuevas. Guardar y forzar el `flush` dentro de la escritura transaccional; traducir únicamente esa restricción a `ReglaNegocioException` con `409` y código `SILLON_NOMBRE_DUPLICADO`. Para el detalle o la actualización de un ID ausente usar `404` y `SILLON_NO_ENCONTRADO`. Los errores ajenos a la restricción conservan el tratamiento compartido.
3. **Separar el catálogo completo del puerto de agenda.** El servicio REST lista ambos estados, ordenados por ID. `SillonConsulta` en `application` ofrece `listarActivos()` y `buscarPorId(Long id)` mediante valores inmutables de lectura, con ausencia explícita por ID. La consulta por ID no filtra estados; el listado activo filtra en la base de datos. Usar métodos derivados o JPQL parametrizado y transacciones `readOnly` para lecturas. No agregarle autorización HTTP al puerto: cada caso de uso consumidor controla sus permisos; el catálogo no depende de `agenda`.
4. **Proteger los casos de uso REST con seguridad compartida.** Declarar `@PreAuthorize` en los métodos del servicio de catálogo: lectura para los tres roles y escritura para `ADMIN`. No agregar filtros, rutas públicas ni un manejador de errores local. Sobre C-01, MockMvc cubre roles, solicitudes anónimas de lectura y CSRF de administradores con la configuración existente; usar CSRF válido en los intentos de roles no autorizados y en el caso anónimo de escritura que aísle autenticación. Verificar posteriormente la sesión real, la precedencia de `401` ante escritura anónima sin token y el envío por cookie/cabecera con C-03 disponible. No reproducir el login ni modificar seguridad para adelantar esa comprobación.
5. **Cambiar el estado en la misma fila.** `PUT` reemplaza los campos editables y conserva el ID. Dar un punto explícito al cambio de activo a inactivo en el servicio, para que C-30 pueda aplicar la guarda de turnos futuros sin reescribir el contrato del catálogo. No aceptar `DELETE` ni usar una operación de repositorio que borre.

## Risks / Trade-offs

- Una violación de unicidad puede marcar la transacción para rollback → propagar la excepción traducida y comprobar la respuesta y el estado persistido desde fuera de la transacción fallida.
- Disponibilidad de C-03 durante el desarrollo → probar el catálogo sobre C-01; dejar la prueba conjunta de sesión/CSRF para una rama con ambos changes, sin iniciar trabajo en C-03 ni agregar una dependencia dura.
- RN-37 está implementada parcialmente → no presentar la baja como verificada contra turnos futuros hasta C-30; no hacer que agenda interprete «activo» como «libre».

## Migration Plan

Agregar la tabla y su restricción vía `ddl-auto` (`update` en desarrollo, `create-drop` en pruebas). No requiere datos iniciales, Flyway/Liquibase ni cambios al script de exclusiones. Un rollback del código no debe borrar automáticamente la tabla ni el catálogo que ya se haya cargado.

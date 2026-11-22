# Design

## Context

C-06 ya ofrece `ProfesionalConsulta.buscarPorId` y `ServicioProfesionales.detalle`, que valida titularidad de `ODONTOLOGO` con `ProfesionalVinculadoConsulta` antes de buscar el registro. En `master` no hay todavía un adaptador productivo de ese vínculo ni un `Intervalo` compartido. La seguridad de C-03 ya exige sesión y CSRF para estas rutas y devuelve Problem Details ante errores de acceso.

## Goals / Non-Goals

**Goals:** separar el contrato interno de lectura de la API y de la persistencia, mantener la política de titularidad existente y proteger la integridad de los intervalos en PostgreSQL.

**Non-Goals:** reemplazar el catálogo, modificar la seguridad global o depender del módulo de turnos para desplegar C-12.

## Decisions

1. **Subrecurso y servicio propios.** Agregar controlador, DTOs, servicio y repositorio en `profesionales.bloqueo`, sin modificar `Profesional` ni su controlador. La alta toma el profesional de la ruta, no del cuerpo. La eliminación se resuelve por el par `(profesionalId, bloqueoId)` dentro de una transacción; buscar o borrar por el ID de bloqueo solo permitiría afectar al profesional equivocado. El listado va por `(inicio, id)` y no requiere cargar entidades de profesionales.
2. **Reutilizar el acceso del catálogo.** Proteger `POST` y `DELETE` de bloqueos por ruta para `ADMIN` y `RECEPCION`, así la falta de permiso devuelve `403` antes de la validación de MVC, incluso ante una solicitud malformada. Mantener `@PreAuthorize` en los métodos de gestión y consultar la existencia con `ProfesionalConsulta`. Para listar, proteger el método para los tres roles y llamar al `ServicioProfesionales.detalle` antes de consultar bloqueos. Este método ya deniega el acceso ajeno o sin vínculo sin revelar si existe el ID. No cambian la sesión, CSRF ni los permisos de otras rutas. C-13 resolverá el vínculo productivo; en C-12 se prueba con un adaptador de test.
3. **Persistencia con identidad por ID.** Mapear `bloqueo` con `profesional_id`, instantes no nulos (`timestamptz`), motivo obligatorio y un índice `(profesional_id, inicio)`. Usar `CHECK (fin > inicio)` de Hibernate como en el catálogo de prestaciones y una FK sin cascada en `schema-constraints.sql` (`^^^`, creación idempotente posterior al DDL). Se mantiene el vínculo por ID sin abrir la entidad JPA de C-06 ni acoplar entidades entre subpaquetes. No se impone exclusión entre bloqueos: pueden representar ausencias que se superponen; la consulta solo necesita saber si existe alguna.
4. **Intervalo compartido y consulta en BD.** Introducir un `Intervalo` puro y mínimo, con `Instant` y extremos ordenados, en `shared.tiempo`; C-11 y C-19 pueden usar el mismo tipo en lugar de crear uno por módulo. Exponer `BloqueoConsulta.solapa(Long profesionalId, Intervalo intervalo)` desde `profesionales.bloqueo.application`; el adaptador ejecuta una consulta JPQL parametrizada por profesional y por ambos extremos. Las desigualdades estrictas implementan `[inicio, fin)` sin traer filas a Java para comparar. El puerto no depende de sesión ni de controladores: está destinado a consumidores internos.
5. **Contrato temporal y errores.** Validar el offset explícito y un máximo de seis decimales al leer, normalizar a `Instant` y comprobar `inicio < fin` antes del alta. Así los extremos se representan sin redondeo en PostgreSQL y la respuesta de alta coincide con el listado. Usar Bean Validation y el manejador existente para `400`; reutilizar el código de C-06 para el profesional ausente y definir `BLOQUEO_NO_ENCONTRADO` para la eliminación. Como no se incorpora un endpoint de detalle, el alta devuelve el DTO sin prometer un `Location` consultable.

## Risks / Trade-offs

- C-11 o C-19 podrían introducir otro `Intervalo` → coordinar el tipo compartido al integrar las ramas; conservar una única convención semiabierta.
- Todavía no se impide crear un bloqueo sobre un turno activo → mantener explícita la exclusión de RN-17 en C-12 y agregar la guarda en C-30.
- Un odontólogo puede tener sesión válida sin vínculo → devolver `403` y no ofrecer una búsqueda por correo ni por un ID indicado por el cliente.

## Migration Plan

El cambio es aditivo: Hibernate crea la tabla en desarrollo y tests y el script existente agrega la FK después. No requiere mover ni rellenar datos existentes. En un entorno con `ddl-auto=validate`, disponer de la tabla antes de arrancar la nueva versión. Para volver a la versión anterior, conservar la tabla y sus datos en lugar de eliminarlos automáticamente.

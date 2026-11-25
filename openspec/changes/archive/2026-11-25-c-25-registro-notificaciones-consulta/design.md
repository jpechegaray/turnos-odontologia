# Design

## Context

La motivación está en `proposal.md`, sección Why.

- C-15 dejó el módulo `shared/notificacion` con una arquitectura hexagonal:
  - El record de dominio `NotificacionEnviada` (inmutable).
  - El puerto `NotificacionRepositorio`, que hoy tiene `guardar`, `porId` y `reclamarVencidas`.
  - El adaptador `NotificacionRepositorioAdapter`, sobre `NotificacionEnviadaJpaRepository` (Spring Data).
  - La tabla `notificacion_enviada`, que no tiene columna de fecha de creación. El `id` es `IDENTITY` y crece con cada registro.
- `ultimoError` siempre lo escribe `ServicioNotificaciones` con el mensaje de `ErrorEnvioNotificacion`. Los adaptadores construyen ese mensaje con textos fijos («No se pudo enviar la notificación por email»), así que no lleva trazas ni la excepción de SMTP.
- No hay ningún endpoint paginado en el proyecto: este es el primero.
- Los listados existentes, como `GET /api/lista-espera`, usan:
  - `@RequestParam` opcionales.
  - JPQL con `(:p is null or campo = :p)`.
  - `@PreAuthorize` en el servicio de aplicación y la regla equivalente en `SecurityConfig`.
  - Records `Respuesta*` con un método `de(...)`.
- `ManejadorErrores` extiende `ResponseEntityExceptionHandler`. Con Spring Boot 3.5, `TypeMismatchException` (un enum desconocido) y `HandlerMethodValidationException` (`@Min` o `@Max` en un `@RequestParam`) ya salen como `400` Problem Details sin código extra.

## Goals / Non-Goals

**Goals:**
- Consulta de solo lectura, sin tocar el ciclo de envío, reintento ni planificación de C-15.
- Dejar un formato de página reutilizable y documentado para los próximos listados paginados.
- Que el dominio y el puerto sigan sin depender de Spring Data.

**Non-Goals:**
- Ordenamiento elegido por el cliente (`sort`): el orden es fijo.
- Paginación por cursor.
- Agregar índices nuevos o cambiar el esquema.

## Decisions

### D1. Formato de página propio, no el `Page` de Spring Data
Se crea un record genérico `Pagina<T>(List<T> contenido, int pagina, int tamanio, long totalElementos)`, con `totalPaginas()` calculado y `map(Function)` para pasar de dominio a DTO. Va en el paquete nuevo `ar.edu.utn.turnos.shared.paginacion`, porque la usan el dominio, el puerto y la web.
- **Alternativa descartada:** serializar `org.springframework.data.domain.Page`. Su JSON expone detalles internos (`pageable`, `sort` y otros), Spring Data advierte que ese formato no es estable y obligaría al puerto de dominio a depender de Spring Data.
- **Alternativa descartada:** `PagedModel` (Spring Data web). Mantiene la dependencia y usa nombres en inglés (`content`, `page.totalElements`), contra la regla de nombres en español.
- Los nombres `contenido`, `pagina`, `tamanio`, `totalElementos` y `totalPaginas` quedan como convención para la API. C-34 los va a consumir.

### D2. Parámetros y límites
- `pagina` (por defecto 0) y `tamanio` (por defecto 20) se validan en el controller con `@Min(0)`, `@Min(1)` y `@Max(100)` sobre los `@RequestParam`, **sin** `@Validated` en la clase. Así los valida Spring MVC 6.1+, que lanza `HandlerMethodValidationException`, y el manejo de errores existente lo traduce a `400`.
  - Con `@Validated`, la validación la hace el proxy AOP, que lanza `ConstraintViolationException`, y termina en el `500` genérico. Lo detectó el test «Parámetros de paginación fuera de rango responden 400» durante el apply.
- `estado` se recibe como `EstadoNotificacion`, así que un valor desconocido da `400` por conversión de tipo.
- `turnoId` es `Long` opcional. No se comprueba que el turno exista: la tabla no tiene FK a `turno`, y una página vacía alcanza para el uso de recepción.
- Se elige `tamanio` en lugar de `size` o `tamano` para mantener nombres en español sin la «ñ».

### D3. Orden fijo por `id` descendente
Las más recientes van primero. Como no hay fecha de creación y el `id` `IDENTITY` crece con cada inserción, alcanza como desempate estable, y además garantiza que no haya repetidos entre páginas.
- **Alternativa descartada:** ordenar por `programada_para`. Cambia con cada reintento, así que una notificación podría saltar de página mientras se recorre el listado.
- **Alternativa descartada:** ordenar por `enviada_en`. Es nula en las pendientes y en las fallidas.

### D4. Puerto y adaptador
- `NotificacionRepositorio` suma `Pagina<NotificacionEnviada> buscar(EstadoNotificacion estado, Long turnoId, int pagina, int tamanio)`.
- El adaptador lo implementa con una consulta JPQL de Spring Data, con filtros nulos opcionales, `order by n.id desc` y `PageRequest.of(pagina, tamanio)`, y convierte el `Page` a `Pagina`.
- Spring Data hace el `count` de los totales con los mismos filtros.
- `Pageable` queda encerrado en `infrastructure`.

### D5. Caso de uso y seguridad
- `ConsultaNotificaciones` (application) tiene un método `listar(...)` con `@Transactional(readOnly = true)` y `@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")`.
- `SecurityConfig` suma `requestMatchers(HttpMethod.GET, "/api/notificaciones").hasAnyRole("ADMIN", "RECEPCION")`. Es defensa en profundidad, como en lista de espera, y el bloque se agrega al final con un comentario.
- Los `401` y `403` los produce el manejo de seguridad existente, en Problem Details.

### D6. Web
- `NotificacionController` vive en `shared/notificacion/web`, con `@RequestMapping("/api/notificaciones")` y un `@GetMapping`.
- `RespuestaNotificacion` es un record con un método `de(NotificacionEnviada)` y expone los 13 campos del spec. No expone la entidad JPA (regla dura 9).
- Los instantes se serializan como `Instant`, en ISO-8601 UTC, igual que el resto de la API.

### D7. Tests
- `ConsultaNotificacionesApiIT` extiende `PruebaIntegracionBase`, con PostgreSQL de Testcontainers, `@Transactional` y MockMvc con `user(...).roles(...)`.
- Prepara las notificaciones con el `NotificacionRepositorio` real, usando `NotificacionEnviada.pendiente(...)` y las transiciones `conEnvioExitoso` y `conIntentoFallido`. Así los datos de prueba respetan las invariantes del dominio.
- Todos los datos son ficticios (`@correo.test`).
- Un test unitario `PaginaTest` cubre `totalPaginas()` (0 elementos, división exacta y resto) y `map`.
- Cada escenario del spec tiene su `@DisplayName` exacto, con `@ParameterizedTest` sobre `ADMIN` y `RECEPCION` donde el escenario nombra a ambos.

## UI Mockups

- Decision: no (no UI), 2026-11-25. C-25 es solo backend; la pantalla llega en C-34.

## Risks / Trade-offs

- [`SecurityConfig` se toca en casi todos los changes] → Bloque nuevo al final con un comentario. Si hay conflicto de merge, se conservan ambos bloques.
- [`destinatario` es el email del paciente, un dato personal] → Solo lo ven `ADMIN` y `RECEPCION`, que ya acceden a la ficha del paciente (US-005). `ODONTOLOGO` no tiene acceso.
- [El `count` crece con la tabla] → Con el volumen de un consultorio de 2 a 5 profesionales no es un problema. Si hiciera falta, se agregan índices sin cambiar el contrato.
- [Primer formato de página del proyecto] → Queda en un tipo compartido y en el spec. Si C-34 necesita otro campo, se agrega sin romper los que ya existen.

## Migration Plan

- No hay migración: no cambia el esquema y el endpoint es nuevo.
- Para volver atrás alcanza con revertir el commit.

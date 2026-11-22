# Design

## Context

Estado actual del módulo `seguridad` (C-03):
- `Usuario` es una entidad package-private con email normalizado único, hash BCrypt validado en el constructor, rol, `activo`, contador de fallos y `bloqueado_hasta`.
- `UsuarioJpaRepository.findByEmail` usa `PESSIMISTIC_WRITE`, que necesita el login.
- `AutenticacionPersonal` ya rechaza usuarios inactivos.
- El login guarda en la sesión un `UsernamePasswordAuthenticationToken` cuyo principal es el record `IdentidadPersonal(id, email, rol)`. Como el record no implementa `Principal`, `Authentication.getName()` devuelve su `toString()`.
- `SecurityConfig` deniega por defecto. C-12 sumó reglas por ruta para cortar con 403 antes de validar el cuerpo.

C-06 dejó `ServicioProfesionales`, que inyecta `Optional<ProfesionalVinculadoConsulta>` y llama `buscarIdPorIdentidad(authentication.getName())`. Hoy ningún bean lo implementa, así que un odontólogo recibe 403. `ProfesionalConsulta.buscarPorId` devuelve `Datos(..., activo)`.

Requisitos en `specs/`. Motivación en `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Cubrir US-002 sin abrir caminos nuevos de ingreso: el login de C-03 no cambia.
- Tocar de lo existente solo lo indispensable, todo dentro de `seguridad`: `Usuario`, `UsuarioJpaRepository`, `IdentidadPersonal` y `SecurityConfig`.

**Non-Goals:**
- Restablecer o cambiar la contraseña, desbloquear manualmente, auditoría de cambios (PA-11) y gestión de sesiones concurrentes. Ver Open Questions.

## Decisions

### D1. Estructura
- `domain/PoliticaUsuario`: funciones puras que validan el vínculo según el rol y la regla del último administrador.
- `domain/ClaveUsuario`: largo de 8 caracteres a 72 bytes UTF-8, el límite de BCrypt que ya aplica el login.
- `application/UsuarioResumen`: el DTO interno, sin credenciales.
- `infrastructure/GestionUsuarios`: el servicio, con `@PreAuthorize("hasRole('ADMIN')")` en todos sus métodos.
- `infrastructure/VinculoProfesionalUsuario`: implementa `ProfesionalVinculadoConsulta`.
- `infrastructure/VerificacionSesionVigente`: el filtro.
- `web/UsuariosController`, con los DTOs `UsuarioAlta`, `UsuarioModificacion` y `UsuarioRespuesta`.

### D2. Dónde vive el vínculo: `usuario.profesional_id`
Se agrega una columna `profesional_id bigint` nullable, con `UNIQUE` generado por Hibernate (DD-08) y sin FK hacia la tabla de otro módulo (KB 08: entre módulos se referencia por id). La existencia y el estado activo del profesional se validan con `ProfesionalConsulta` al vincular.
- *Alternativa descartada:* `profesional.usuario_id`, como dibuja KB 04. Obligaría a editar `Profesional` de C-06, en el módulo `profesionales`, y a que `profesionales` conozca a `seguridad`. La ficha de C-13 ya eligió `profesional_id` en `Usuario`.
- **Efecto:** el `UNIQUE` cuenta también a los usuarios inactivos. Para pasarle un profesional a otro usuario, el administrador primero le cambia el vínculo al usuario anterior. Es un caso raro y queda documentado.

### D3. Invalidar sesiones: revalidar en cada request
`VerificacionSesionVigente` es un `OncePerRequestFilter` que se agrega en la cadena antes de `AuthorizationFilter`. Si el principal es `IdentidadPersonal`, busca el usuario por id con una lectura simple por clave primaria, sin bloqueo. Si no existe, está inactivo, o cambió su rol o su email, invalida la `HttpSession`, limpia el `SecurityContext` y deja seguir el request como anónimo. Entonces el entry point de C-03 responde 401 con el formato de siempre.
- *Alternativa A (descartada):* `SessionRegistry` con `SessionInformation.expireNow()`. Exige registrar las sesiones en el login de C-03, sumar `HttpSessionEventPublisher` y `ConcurrentSessionFilter`, y no cubre el cambio de rol. Es más configuración sobre un flujo CRITICAL que ya está probado.
- *Alternativa B (descartada):* guardar en `Usuario` un número de versión de credenciales y compararlo. Igual necesita leer la base en cada request, y suma una columna más.
- **Costo:** una lectura por clave primaria en cada request autenticado. Es aceptable para menos de 50 usuarios en una sola instancia (SU-10), y de paso resuelve el cambio de rol.

### D4. Identidad de la sesión para C-06
`IdentidadPersonal` pasa a implementar `java.security.Principal`, con `getName()` igual a `String.valueOf(id)` y anotado `@JsonIgnore`, así el JSON de `/api/auth/me` y del login sigue siendo exactamente `{id, email, rol}` (contrato de C-16). Se usa el id y no el email porque el email ahora se puede modificar.

`VinculoProfesionalUsuario.buscarIdPorIdentidad(String)` parsea el id. Si el texto no es un número, devuelve vacío: así no rompe los tests de C-06, que usan `@WithMockUser` con nombres arbitrarios y un bean de prueba propio. Si el usuario existe, está activo y tiene vínculo, devuelve su `profesionalId`.

**Implementado (2026-11-22):** el bean no es `@Component`. Lo registra `VinculoProfesionalAutoConfiguration`, una `@AutoConfiguration` listada en `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, con `@ConditionalOnMissingBean`. Spring evalúa las autoconfiguraciones después de las configuraciones de la aplicación y de los tests, así que el bean de prueba de `ProfesionalesTitularidadIT` y `BloqueosApiIT` lo reemplaza sin tocar esos tests. Con un `@Component` había `NoUniqueBeanDefinitionException`, y un `@ConditionalOnMissingBean` en una configuración común no alcanza, porque se evalúa antes que las `@TestConfiguration` anidadas. La alternativa más simple, `@Primary` en los dos beans de prueba (el patrón de `RelojFijoConfig`), queda descartada para no editar tests de otros changes.

### D5. Último administrador
Antes de desactivar a un `ADMIN` activo o de cambiarle el rol, se cuentan los `ADMIN` activos. Si es el último, responde 409 `USUARIO_ULTIMO_ADMIN`. Para que dos administradores no se desactiven mutuamente al mismo tiempo, la operación toma `PESSIMISTIC_WRITE` sobre las filas `ADMIN` activas dentro de la transacción. Son pocas filas y una operación infrecuente.

### D6. Contrato HTTP
| Operación | Respuesta |
|---|---|
| `GET /api/usuarios` | 200, ordenado por email |
| `GET /api/usuarios/{id}` | 200 / 404 |
| `POST /api/usuarios` `{email, nombre, rol, password, profesionalId?}` | 201 + `Location` |
| `PUT /api/usuarios/{id}` `{email, nombre, rol, profesionalId?, activo}` | 200 / 404; reemplazo completo sin contraseña, permite reactivar |
| `DELETE /api/usuarios/{id}` | 204 (idempotente) / 404 / 409 si es el último ADMIN |

La respuesta es `{id, email, nombre, rol, profesionalId, activo}`. Bean Validation da 400 por formato: `@Email`, `@NotBlank`, `@Size` y largo de la contraseña. Las reglas de negocio dan 422 o 409 con códigos que siguen la convención mayoritaria del repo (guion bajo): `USUARIO_NO_ENCONTRADO`, `USUARIO_EMAIL_DUPLICADO`, `USUARIO_VINCULO_INVALIDO`, `USUARIO_PROFESIONAL_YA_VINCULADO` y `USUARIO_ULTIMO_ADMIN`. Se usa la misma `ReglaNegocioException`, así `ManejadorErrores` no cambia. Una carrera sobre un `UNIQUE`, de email o de profesional, se traduce al 409 que corresponda según la restricción violada.

### D7. Autorización en dos capas
`SecurityConfig` suma `.requestMatchers("/api/usuarios", "/api/usuarios/**").hasRole("ADMIN")`, el mismo patrón que C-12, para responder 403 antes de validar el cuerpo. Además, el servicio lleva `@PreAuthorize`. CSRF sigue igual: las escrituras necesitan el token de C-03.

### D8. Tests
- Unitarios: `PoliticaUsuario` y `ClaveUsuario`.
- Integración (Testcontainers): `UsuarioIT` para la columna y su `UNIQUE`, `GestionUsuariosIT` para el servicio, el último admin y las carreras, y `UsuariosApiIT` con MockMvc, que cubre cada escenario con `@DisplayName` exacto y un CSRF real (el helper de C-04).
- `SesionVigenteIT`: login real, desactivar, repetir con la misma cookie y esperar 401.
- `VinculoProfesionalIT`: odontólogo vinculado contra `GET /api/profesionales` real.
- Regresión: tienen que seguir en verde los tests de C-03 y de C-06, incluidos los que mockean el vínculo.

## Risks / Trade-offs

- [Se modifican cuatro archivos de C-03] → son del mismo módulo, la ficha lo prevé y lo cubren sus propios tests de regresión, que tienen que quedar en verde sin cambios.
- [`ProfesionalesTitularidadIT` (C-06) y `BloqueosApiIT` (C-12) ya definen un `ProfesionalVinculadoConsulta` de prueba] → con un bean real, el contexto de esos tests tendría dos y la inyección `Optional<...>` fallaría. Mitigación preferida, sin tocar esos tests: registrar el bean real con `@ConditionalOnMissingBean` en una configuración que se evalúe después de la de los tests. Si Spring no lo garantiza, el plan B es agregar `@Primary` al bean de prueba de esos dos tests (una línea en cada uno). Se resuelve en apply y, si hace falta el plan B, se avisa antes de editar tests de otros changes.
- [Una lectura por request] → aceptable con la escala del MVP. Si algún día molesta, se puede cachear unos segundos.
- [Contraseña inicial elegida por el administrador] → la transmite fuera del sistema. No hay cambio de contraseña en el MVP (ver Open Questions).
- [Discrepancia con KB 04] → si se aprueba, después del merge hay que actualizar KB 04: `profesional_id` en `Usuario` y sacar `usuario_id` de `Profesional`.

## Open Questions

Preguntas para Juan Pablo que no cambian las specs si se responden "no por ahora":
1. ¿Hace falta que el administrador pueda restablecer la contraseña de un usuario en el MVP? La recomendación es un change aparte, porque implica invalidar sesiones por cambio de credencial.
2. ¿Hace falta que el usuario cambie su propia contraseña? Hoy no está en US-002.

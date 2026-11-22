# Tasks

## 1. Aprobación previa (CRITICAL)

- [ ] 1.1 Registrar la aprobación explícita de Juan Pablo de esta propuesta antes de escribir código. Verificar que cubra: `profesional_id` en `Usuario` (contra KB 04), la revalidación de la sesión en cada request (D3), `IdentidadPersonal` como `Principal` con el id (D4), la regla del último ADMIN y que restablecer la contraseña queda fuera de alcance.

## 2. Reglas de dominio

- [ ] 2.1 Implementar por TDD `PoliticaUsuario` (vínculo según el rol, último ADMIN) y `ClaveUsuario` (8 caracteres a 72 bytes UTF-8); verificar con tests unitarios puros y `ArquitecturaTest` en verde.

## 3. Persistencia

- [ ] 3.1 Extender `Usuario` con `profesional_id` único y nullable y con los métodos de modificación y desactivación, y agregar a `UsuarioJpaRepository` las consultas sin bloqueo y el bloqueo de los ADMIN activos; verificar en PostgreSQL el `UNIQUE` del vínculo y que los tests de C-03 (`UsuarioIT`, `AutenticacionPersonalIT`) siguen en verde sin cambios.

## 4. Servicio, vínculo y sesión

- [ ] 4.1 Implementar `GestionUsuarios` (alta, modificación, desactivación, listado y consulta, con `@PreAuthorize` ADMIN) con los códigos `USUARIO_*` y la traducción de carreras sobre los `UNIQUE`; verificar con `GestionUsuariosIT` los escenarios de alta, email único, vínculo, modificación, desactivación y último ADMIN.
- [ ] 4.2 Hacer que `IdentidadPersonal` implemente `Principal` (`getName()` = id, `@JsonIgnore`) e implementar `VinculoProfesionalUsuario`; verificar los escenarios "Una identidad sin vínculo se resuelve sin profesional" y "Un odontólogo vinculado ve solo su propio profesional", que el JSON de `/api/auth/me` no cambia, y que `ProfesionalesTitularidadIT` y `BloqueosApiIT` siguen en verde (si se necesita el plan B del design, avisar antes).
- [ ] 4.3 Implementar `VerificacionSesionVigente` y registrarlo en `SecurityConfig`; verificar con `SesionVigenteIT` (login real y misma cookie) los tres escenarios de `control-acceso`.

## 5. API REST

- [ ] 5.1 Implementar `UsuariosController` con sus DTOs y la regla por ruta `/api/usuarios/**` solo para ADMIN en `SecurityConfig`; verificar con `UsuariosApiIT` (CSRF real) cada escenario de `administracion-usuarios`, incluidos 403 para RECEPCION y ODONTOLOGO, 401 sin sesión, y que ninguna respuesta incluye el hash.

## 6. Verificación de integración

- [ ] 6.1 Ejecutar `mvnw.cmd verify` desde `backend/` con Docker; comprobar que pasan C-01, C-03, C-04, C-05, C-06, C-08, C-12, C-17 y C-13, y que los únicos archivos existentes modificados son `Usuario.java`, `UsuarioJpaRepository.java`, `IdentidadPersonal.java` y `SecurityConfig.java` (más los dos tests del plan B, si se aprobó).

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- PR `C-13 administrar-usuarios` de `valentino` a `master` (lo revisa Juan Pablo). Preguntar antes de cada commit.
- Después del merge: `/opsx:archive` y proponer la actualización de KB 04 (vínculo en `Usuario`).

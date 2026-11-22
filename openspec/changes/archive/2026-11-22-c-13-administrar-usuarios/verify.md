# Verify — c-13-administrar-usuarios (2026-11-22)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; archivar después del merge del PR)

## Validación estructural

`openspec validate c-13-administrar-usuarios --strict` → `Change 'c-13-administrar-usuarios' is valid`.

## Tareas

10/10 completas. La 1.1 registra la aprobación de Juan Pablo (gobernanza CRITICAL), dada antes de escribir código.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/seguridad/`. Cada test tiene `@DisplayName` igual al nombre del escenario. Se leyó cada test y se confirmó que afirma el THEN.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| administracion-usuarios | El administrador da de alta un usuario de recepción | Cubierto | infrastructure/UsuariosApiIT.java:57 | 201, `Location`, email normalizado, `profesionalId` nulo |
| administracion-usuarios | El usuario dado de alta puede iniciar sesión con su contraseña | Cubierto | infrastructure/UsuariosApiIT.java:71 | Login real: 200 con email y rol |
| administracion-usuarios | La contraseña se guarda solo como hash BCrypt | Cubierto | infrastructure/UsuariosApiIT.java:84 | El hash verifica la contraseña, y no hay ninguna fila con la contraseña en claro |
| administracion-usuarios | Una contraseña fuera de largo responde 400 | Cubierto | infrastructure/UsuariosApiIT.java:95 | Corta (6 caracteres) y 74 bytes UTF-8. La respuesta no repite el valor |
| administracion-usuarios | Un email repetido con otra capitalización responde 409 | Cubierto | infrastructure/UsuariosApiIT.java:111 | También en la modificación (GestionUsuariosIT) |
| administracion-usuarios | Un odontólogo se da de alta vinculado a su profesional | Cubierto | infrastructure/UsuariosApiIT.java:123 | |
| administracion-usuarios | Un odontólogo sin profesional responde 422 | Cubierto | infrastructure/UsuariosApiIT.java:133 | |
| administracion-usuarios | Un rol que no es odontólogo con profesional responde 422 | Cubierto | infrastructure/UsuariosApiIT.java:139 | |
| administracion-usuarios | Un profesional inexistente o inactivo responde 422 | Cubierto | infrastructure/UsuariosApiIT.java:145 | Los dos casos |
| administracion-usuarios | Un profesional ya vinculado responde 409 | Cubierto | infrastructure/UsuariosApiIT.java:152 | Además: la carrera sobre el `UNIQUE` (GestionUsuariosTest) y el `UNIQUE` en PostgreSQL (UsuarioVinculoIT) |
| administracion-usuarios | El administrador cambia el rol de un usuario | Cubierto | infrastructure/UsuariosApiIT.java:165 | |
| administracion-usuarios | Modificar un usuario inexistente responde 404 | Cubierto | infrastructure/UsuariosApiIT.java:175 | |
| administracion-usuarios | Un usuario desactivado no puede iniciar sesión | Cubierto | infrastructure/UsuariosApiIT.java:183 | 204 y luego login 401 con el mensaje genérico de C-03 |
| administracion-usuarios | Repetir la desactivación responde 204 | Cubierto | infrastructure/UsuariosApiIT.java:199 | |
| administracion-usuarios | Un usuario reactivado vuelve a iniciar sesión | Cubierto | infrastructure/UsuariosApiIT.java:208 | |
| administracion-usuarios | No se puede desactivar al último administrador activo | Cubierto | infrastructure/UsuariosApiIT.java:227; infrastructure/GestionUsuariosIT.java:120 | Desactivar, cambiar el rol y PUT con `activo=false`. Sigue ADMIN activo, y con un segundo ADMIN sí se puede |
| administracion-usuarios | Nunca se devuelve el hash de la contraseña | Cubierto | infrastructure/UsuariosApiIT.java:246 | Claves exactas del JSON en el listado y el detalle. No aparecen `password`, `passwordHash`, `intentosFallidos`, `bloqueadoHasta` ni `$2` |
| administracion-usuarios | El listado se ordena por email | Cubierto | infrastructure/UsuariosApiIT.java:266; infrastructure/GestionUsuariosIT.java:138 | |
| administracion-usuarios | Consultar un usuario inexistente responde 404 | Cubierto | infrastructure/UsuariosApiIT.java:276 | |
| administracion-usuarios | Recepción no accede a la gestión de usuarios | Cubierto | infrastructure/UsuariosApiIT.java:285 | Las 5 operaciones dan 403 Problem Details, sin datos ni cambios sobre un usuario real |
| administracion-usuarios | El odontólogo no accede a la gestión de usuarios | Cubierto | infrastructure/UsuariosApiIT.java:292 | Ídem |
| administracion-usuarios | Sin sesión la gestión de usuarios responde 401 | Cubierto | infrastructure/UsuariosApiIT.java:298 | |
| administracion-usuarios | Un odontólogo vinculado ve solo su propio profesional | Cubierto | infrastructure/VinculoProfesionalIT.java:33 | Login real contra `GET /api/profesionales` de C-06; el detalle ajeno da 403 |
| administracion-usuarios | Una identidad sin vínculo se resuelve sin profesional | Cubierto | infrastructure/VinculoProfesionalIT.java:49 | RECEPCION, inactivo, inexistente, identidad no numérica y nula |
| control-acceso | La sesión de un usuario desactivado deja de ser válida | Cubierto | infrastructure/SesionVigenteIT.java:29 | Misma cookie: 200, luego 401 Problem Details. Sigue en 401 aunque se reactive |
| control-acceso | La sesión de un usuario al que se le cambió el rol deja de ser válida | Cubierto | infrastructure/SesionVigenteIT.java:48 | Tras volver a iniciar sesión, `/me` informa ADMIN |
| control-acceso | La sesión de un usuario sin cambios sigue vigente | Cubierto | infrastructure/SesionVigenteIT.java:64 | Además: el cambio de email también invalida la sesión |

27/27 escenarios cubiertos.

## Ejecución de tests

- Comando: `./mvnw -q clean verify` desde `backend/`, con Docker Desktop 27.2.0.
- Resultado: BUILD OK (exit 0). Surefire: **100 tests**, 0 fallas, 0 errores, 0 saltados. Failsafe (Testcontainers, PostgreSQL 16): **227 tests**, 0 fallas, 0 errores, 0 saltados.
- Regresión en verde y sin modificar sus tests: C-01, C-03 (`SesionPersonalIT`, que exige el JSON de 3 campos del login; `AutenticacionPersonalIT`; `UsuarioIT`; `AdminDesarrolloIT`; `CookieSesionIT`; `SecurityConfigTest`), C-04, C-05, C-06 (`ProfesionalesTitularidadIT`), C-08, C-12 (`BloqueosApiIT`) y C-17.
- Frontend: no ejecutado, porque el change no toca `frontend/`.

## Reglas de negocio y reglas duras

| Regla | Cobertura |
|---|---|
| RN-33 (permisos por rol; el odontólogo es solo lectura) | Regla por ruta `/api/usuarios/**` solo para ADMIN, más `@PreAuthorize` en el servicio. Se prueban 403 y 401 |
| RN-34 (BCrypt; inactivo no ingresa) | Hash BCrypt verificado. El usuario desactivado da login 401 genérico, y su sesión abierta da 401 |
| US-002 | Los 4 criterios (solo ADMIN, email único, vínculo del odontólogo, BCrypt) cubiertos |

Reglas duras:
- Regla 1: datos ficticios (emails `@consultorio.test`, matrículas `C13-*`).
- Regla 2: sin secretos. La contraseña de los fixtures es un literal de test, y `UsuarioAlta.toString()` la redacta.
- Regla 4: ODONTOLOGO no escribe (403).
- Regla 7: sin Flyway. La columna y el `UNIQUE` los genera Hibernate.
- Regla 8: sin JWT, sesión de servidor.
- Regla 9: la API usa DTOs y no expone la entidad. JPQL parametrizado (`@Param`).
- Regla 10: sin H2.
- Regla 11: `@DisplayName` exactos.
- Archivos existentes modificados: solo los cuatro previstos en la propuesta, todos del módulo `seguridad`: `Usuario.java`, `UsuarioJpaRepository.java`, `IdentidadPersonal.java` y `SecurityConfig.java` (80 líneas agregadas y 3 quitadas). No se modificó ningún test de otro change.

## Diferencias con design.md

- **Menor (documentada en D4):** el vínculo se registra con una `@AutoConfiguration` con `@ConditionalOnMissingBean` (archivo nuevo `META-INF/spring/...AutoConfiguration.imports`) en lugar de un `@Component`. Era la mitigación preferida del design: evita el choque con los beans de prueba de C-06 y C-12 sin editarlos.
- **Menor:** `SecurityConfig` recibe el verificador como `ObjectProvider`, para que los slices `@WebMvcTest` sin JPA sigan funcionando, y el filtro no es un bean (si lo fuera, Spring Boot también lo registraría fuera de la cadena de seguridad).
- **Menor:** se agregó `SesionRealPrueba`, un helper de test para el login real y el CSRF.

No hay diferencias mayores ni bloqueantes.

## Qué NO se pudo verificar

- **Concurrencia real del último ADMIN** (dos administradores que se desactivan mutuamente al mismo tiempo): el bloqueo `PESSIMISTIC_WRITE` sobre los ADMIN activos está implementado, pero no hay un test con dos transacciones simultáneas.
- **La carrera real sobre los `UNIQUE`**: la traducción a 409 se probó con un mock, y los `UNIQUE` contra PostgreSQL por separado.
- **El arranque manual con el perfil `dev`** contra la base de `docker compose`: `ddl-auto: update` tiene que agregar `profesional_id` y su `UNIQUE` a una tabla `usuario` que ya existe. Si falla, se recrea la base ficticia (PA-10).

## Próximo paso

1. Commit y PR `C-13 administrar-usuarios` de `valentino` a `master`, que revisa Juan Pablo.
2. Después del merge: `/opsx:archive c-13-administrar-usuarios` y actualizar KB 04 (el vínculo queda en `Usuario.profesional_id`, y se saca `usuario_id` de `Profesional`).

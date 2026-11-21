# Verify — c-03-seguridad-login-sesion-csrf (2026-11-21)

**Veredicto: LISTO PARA ARCHIVAR tras el merge — verificado en el alcance aislado de C-03.**

## Validación estructural

- `openspec validate c-03-seguridad-login-sesion-csrf --strict`: válido.
- Alcance: cambios locales de C-03 sobre `origin/master`, incluyendo archivos nuevos sin seguimiento. El diff entre commits no muestra esos archivos; se revisaron también el árbol de trabajo y los archivos nuevos de C-03. Se excluyó C-05.

## Tareas

10/10 marcadas en `tasks.md`. La aprobación está registrada allí. Los checks no sustituyen la comprobación del resultado esperado en cada escenario.

## Matriz escenario → test

Todos pertenecen a `specs/control-acceso/spec.md`. Los archivos de la tabla están bajo `backend/src/test/java/ar/edu/utn/turnos/seguridad/`.

| Escenario | Estado | Test | Evidencia o brecha |
|---|---|---|---|
| Una petición anónima a una ruta protegida recibe 401 | Cubierto | `infrastructure/SecurityConfigTest.java:103` | Comprueba 401, tipo Problem Details y campos de error. |
| Una ruta protegida inexistente también recibe 401 | Cubierto | `infrastructure/SecurityConfigTest.java:114` | Comprueba 401 en lugar de 404. |
| Consultar la sesión sin autenticar devuelve 401 | Cubierto | `infrastructure/SesionPersonalIT.java:93` | Comprueba 401 y tipo Problem Details. |
| Un usuario activo inicia sesión y consulta su identidad | Cubierto | `infrastructure/SesionPersonalIT.java:43`, `infrastructure/CookieSesionIT.java:31` | Email con otra capitalización, DTO de tres campos, reutilización de la sesión y atributos de `JSESSIONID` en HTTP real. |
| El inicio de sesión no reutiliza una sesión anterior | Cubierto | `infrastructure/SesionPersonalIT.java:61` | Compara identificadores antes y después de un login exitoso. |
| El rechazo de login no revela el estado de la cuenta | Cubierto | `infrastructure/SesionPersonalIT.java:75` | Cuatro casos con el mismo 401, tipo y cuerpo; no se crea sesión. |
| Cerrar sesión invalida el acceso posterior | Cubierto | `infrastructure/CookieSesionIT.java:48` | La misma cookie recibe 200 antes del logout, el logout devuelve 204, y luego la cookie recibe 401 Problem Details en un servidor real. |
| Los registros no contienen credenciales ni cookies | Cubierto | `infrastructure/SesionPersonalIT.java:166` | Captura logs de login exitoso, login fallido y logout; comprueba ausencia de clave, hash, valores CSRF y cabecera de sesión. |
| El quinto fallo bloquea hasta el vencimiento exacto | Cubierto | `domain/PoliticaBloqueoTest.java:18`, `infrastructure/AutenticacionPersonalIT.java:40` | Cinco fallos en PostgreSQL; la misma credencial válida se rechaza a un milisegundo del vencimiento y se acepta exactamente al vencer. |
| Un ingreso correcto reinicia los intentos consecutivos | Cubierto | `domain/PoliticaBloqueoTest.java:29`, `infrastructure/AutenticacionPersonalIT.java:68` | La integración prueba el reinicio persistido tras un éxito; el unitario prueba el primer fallo luego del reinicio. |
| Después del vencimiento se inicia una nueva serie | Cubierto | `domain/PoliticaBloqueoTest.java:38`, `infrastructure/AutenticacionPersonalIT.java:55` | Al vencer, un intento incorrecto deja el contador persistido en uno; inmediatamente después, una credencial válida ingresa. |
| La SPA obtiene CSRF sin sesión y lo renueva al cambiar de sesión | Cubierto | `infrastructure/SesionPersonalIT.java:114`, `infrastructure/SecurityConfigTest.java:67` | Obtención anónima sin cookie de sesión; invalidación del token anterior y uso de tokens nuevos en login y logout. |
| Una operación que requiere CSRF rechaza tokens faltantes o inválidos | Cubierto | `infrastructure/SesionPersonalIT.java:133` | Comprueba 403 Problem Details, que el login no crea sesión, que logout conserva la sesión y que una escritura existente no cambia estado hasta recibir un token válido. |
| Solo el origen configurado puede usar CORS con credenciales | Cubierto | `infrastructure/SecurityConfigTest.java:77` | Preflight permitido con origen y credenciales; el otro origen recibe 403 sin autorización CORS. |
| El administrador de desarrollo se inicializa una sola vez | Cubierto | `infrastructure/AdminDesarrolloIT.java:29` | Invoca dos veces el inicializador sobre PostgreSQL y comprueba rol y conservación del hash pese al cambio de clave configurada. |
| El administrador inicial no se crea fuera de desarrollo | Cubierto | `infrastructure/AdminDesarrolloIT.java:44` | Con el secreto presente, el inicializador no se registra fuera de `dev`; sí se registra en `dev`. |

Resultado: **16 cubiertos, 0 parciales, 0 sin test**. El refuerzo de cobertura no requirió modificar código de producción.

## Ejecución de tests

- Backend focalizado: `mvnw.cmd -B -ntp -Dtest=AutenticacionPersonalIT,CookieSesionIT,SesionPersonalIT test` pasó: 15 de integración, 0 fallidas, 0 saltadas.
- Backend en el árbol compartido: `mvnw.cmd -B -ntp verify` se detuvo en `testCompile` porque `SillonConsultaIT` de C-05 referenciaba `SillonConsulta` y `SillonesPersistencia`, aún ausentes en esa ejecución. No se modificó C-05.
- Backend aislado: `mvnw.cmd -B -ntp verify` pasó con 36 pruebas unitarias/web y 26 de integración con PostgreSQL/Testcontainers, 0 fallidas, 0 saltadas. Se usó una copia temporal de los archivos versionados actuales del backend más los archivos nuevos de seguridad de C-03; no se copiaron cambios sin seguimiento de C-05.
- Frontend: fuera del alcance de C-03. `npx --no-install vitest run` pasó: 6 archivos, 8 pruebas, 0 fallidas, 0 saltadas.

## Reglas de negocio y reglas duras

- **RN-33:** `SecurityConfigTest.java:170` y `:180` comprueban que, con CSRF válido, odontología recibe 403 y recepción puede escribir en una operación declarada para su rol. Los permisos de endpoints de otros changes quedan fuera del alcance.
- **RN-34:** BCrypt y unicidad comprobados en `UsuarioIT.java:26` y `:36`; rechazo uniforme en `SesionPersonalIT.java:75`; bloqueo, vencimiento e inactividad en `AutenticacionPersonalIT.java:40`, `:55`, `:81` y `:106`.
- Solo se observaron datos ficticios en los tests e inicializador. `.env` está ignorado y no se encontraron credenciales en claro en la configuración versionada de C-03. No introduce JWT, H2 ni scripts de migración; sí usa PostgreSQL/Testcontainers y DTOs.

## Diferencias con design.md

- **Menor:** el controlador MVC orquesta la autenticación JSON y llama directamente al servicio ubicado en `infrastructure`. Conserva los filtros, el guardado explícito del contexto y la política pura en `domain`, pero concentra parte de la orquestación en la capa web en lugar de delegarla por una interfaz de aplicación.
- **Menor:** `SecurityConfig` conserva el `UserDetailsService` vacío para evitar el usuario automático de Boot. No autentica al personal; ese camino usa PostgreSQL. No contradice el comportamiento observable.

## Qué NO se pudo verificar

- La suite con el trabajo en curso de C-05 no se pudo ejecutar en el árbol compartido.
- No se ejecutó una interfaz de login; pertenece a C-16. No se verificaron despliegues ni reinstalaciones fuera del entorno local de pruebas.

## Próximo paso

Preparar un commit y un PR solo de C-03, sin incluir los archivos de C-05. Ejecutar `/opsx:archive` después del merge.

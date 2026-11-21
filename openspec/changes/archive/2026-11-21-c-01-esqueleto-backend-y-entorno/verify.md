# Verify — c-01-esqueleto-backend-y-entorno (2026-11-21)

**Veredicto:** LISTO (actualizado 2026-11-21). Las tareas 1.2 y 8.2 quedaron confirmadas a mano por el usuario. No hay fallas técnicas: todo lo ejecutable pasa.

## Validación estructural
`openspec validate c-01-esqueleto-backend-y-entorno --strict` → válido.

## Tareas
35/35 completas. Confirmadas a mano por el usuario:
- 1.2 (`.env.example` con las 16 variables de KB 08): el usuario comparó las claves con KB 08: 16 de 16, sin diferencias (el verificador no puede leer archivos `.env*`).
- 8.2 (verificación de secretos y comparación de claves con KB 08): confirmada por el usuario; lo verificable sin leer `.env*` también pasa (ver "Reglas duras").

La 7.1 (README tal cual) se marcó `[x]` tras seguirlo (ver "Ejecución").

## Matriz escenario → test
24 escenarios: 19 cubiertos por test automático, 5 de verificación manual.

| Spec | Escenario | Estado | Test | Nota |
|---|---|---|---|---|
| control-acceso | Una petición anónima a una ruta protegida recibe 401 | Cubierto | SecurityConfigTest:51 | |
| control-acceso | Una ruta protegida inexistente también recibe 401 | Cubierto | SecurityConfigTest:62 | |
| control-acceso | Una ruta pública responde sin autenticación | Cubierto | SecurityConfigTest:76 | |
| control-acceso | Una escritura pública no exige token CSRF | Cubierto | SecurityConfigTest:83 | |
| control-acceso | Un usuario sin el rol requerido recibe 403 Problem Details | Cubierto | SecurityConfigTest:98 | |
| control-acceso | Un usuario con el rol requerido accede a la operación | Cubierto | SecurityConfigTest:109 | |
| entorno-desarrollo | El entorno levanta PostgreSQL 16 desde cero | Manual OK | Tarea 8.1 | `docker compose down -v && up -d --wait` → healthy, PostgreSQL 16.16 |
| entorno-desarrollo | La extensión btree_gist está disponible en la base local | Manual OK | Tarea 8.1 | extensión disponible |
| entorno-desarrollo | La extensión btree_gist queda instalada al arrancar la aplicación | Cubierto | ExtensionBtreeGistIT:16 | |
| entorno-desarrollo | El script de restricciones es idempotente | Cubierto | ScriptRestriccionesIT:23 | |
| entorno-desarrollo | El archivo .env no se versiona | Manual OK | Tarea 8.2 | `git check-ignore .env` → ignorado; solo `.env.example` es versionable. Confirmado por el usuario |
| entorno-desarrollo | El archivo .env.example declara todas las variables del entorno | Manual OK | Tareas 1.2 y 8.2 | Comparación de claves con KB 08 hecha por el usuario: 16 de 16 |
| entorno-desarrollo | El perfil test arranca con un esquema limpio | Cubierto | ContextoAplicacionIT:27 | |
| entorno-desarrollo | El perfil dev conserva el esquema entre arranques | Cubierto | PerfilDevTest:22 | |
| entorno-desarrollo | El contexto de la aplicación carga con el perfil test sobre PostgreSQL real | Cubierto | ContextoAplicacionIT:20 | |
| entorno-desarrollo | La build completa pasa en una máquina limpia | Manual OK | Apply | `./mvnw clean verify` con repo Maven vacío: BUILD SUCCESS (27 unitarios + 6 IT) |
| errores-api | Un error de regla responde Problem Details con su código | Cubierto | ManejadorErroresTest:75 | |
| errores-api | Cada estado de regla responde con el mismo formato | Cubierto | ManejadorErroresTest:88 | |
| errores-api | Una regla puede agregar datos de contexto al error | Cubierto | ManejadorErroresTest:99 | |
| errores-api | Una petición con campos inválidos responde 400 Problem Details | Cubierto | ManejadorErroresTest:108 | |
| errores-api | Un error inesperado no expone detalles internos | Cubierto | ManejadorErroresTest:133 | |
| reloj-del-sistema | El reloj del sistema usa la zona de Buenos Aires | Cubierto | ConfiguracionRelojTest:18, ConfiguracionRelojIT:18 | |
| reloj-del-sistema | Un instante UTC se interpreta en hora local de Buenos Aires | Cubierto | ConfiguracionRelojTest:27 (+ :35 triangulación) | |
| reloj-del-sistema | Una prueba fija la hora actual del sistema | Cubierto | RelojFijoIT:43 | |

Excepción aceptada: `ArquitecturaTest` (regla `..domain..` sin Spring/JPA/Hibernate) usa `.as(...)` en lugar de `@DisplayName`, porque los campos `@ArchTest` no admiten la anotación. No corresponde a ningún escenario de las specs.

## Ejecución de tests
- `./mvnw verify` (JDK 21, Docker encendido): **BUILD SUCCESS**. Unitarios: 27 pasados, 0 fallas, 0 saltados. Integración (Testcontainers PostgreSQL): 6 pasados, 0 fallas, 0 saltados.
- Tarea 7.1 (README al pie de la letra): `docker compose up -d` (ya arriba, healthy), `./mvnw verify` OK, `./mvnw spring-boot:run` arrancó en 4,5 s con perfil dev y `GET /api/cualquiera` → **401**; proceso detenido (el puerto 8080 quedó libre). `docker compose` sigue corriendo.

## Reglas de negocio y reglas duras
- RN-14: reloj con `ZoneId` America/Argentina/Buenos_Aires e inyectable, con tests. `LocalDateTime` aparece solo en aserciones de un test, no se persiste.
- RN-35: error 500 sin detalles internos, cubierto por test.
- Sin Flyway/Liquibase/JWT/H2 en `pom.xml`, código ni configuración.
- Sin secretos en `application*.yml`: la contraseña es `${POSTGRES_PASSWORD}`. Sin credenciales en el código versionable.
- `.env` ignorado (`git check-ignore .env`); `git ls-files` solo lista `.env.example` (y `.env.example` aparece sin trackear en `git status`, pendiente de commit).
- No se leyó el contenido de ningún `.env*` (restricción de permisos).

## Diferencias con design.md
Ninguna detectada (bloqueante / mayor / menor: 0). No se hizo una lectura línea a línea del diseño; la estructura de paquetes de los tests (`seguridad.infrastructure`, `shared.error`, `shared.web`, `shared.tiempo`) es coherente con la arquitectura hexagonal de KB 08.

## Qué NO se pudo verificar
- Los valores de `.env.example` no los leyó el verificador (permisos); la comparación de claves la hizo el usuario: 16 de 16.
- Observación: el README dice "Hace falta JDK 21" pero no indica fijar `JAVA_HOME`; con otro JDK por defecto en el PATH (aquí era el 26) hay que exportarlo. No se modificó el README.

## Próximo paso
Sin bloqueantes. Commit en la rama `santino`, PR a `master` (revisa Juan Pablo) y, después del merge, `/opsx:archive c-01-esqueleto-backend-y-entorno`.

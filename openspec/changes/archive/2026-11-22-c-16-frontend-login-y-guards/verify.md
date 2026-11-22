# Verify — c-16-frontend-login-y-guards (2026-11-22)

**Veredicto: LISTO PARA ARCHIVAR (0 bloqueantes).** Los cuatro hallazgos de la primera verificación se atendieron en los tests. La matriz actual cubre 14/14 escenarios.

## Validación estructural

`openspec status --change c-16-frontend-login-y-guards --json`: 4/4 artefactos. `openspec validate c-16-frontend-login-y-guards --strict`: válido.

## Tareas

5/5 marcadas en `tasks.md`. La evidencia de integración registrada durante `apply` incluye cuatro rechazos uniformes, login real de `ODONTOLOGO`, recarga, logout `204` y `/me` posterior `401`, contra C-03 y PostgreSQL 16 aislado. No se cambió el estado de ninguna tarea.

## Matriz escenario → test

Todos los escenarios pertenecen a `sesion-frontend-personal`. Las rutas abreviadas de tests son relativas a `frontend/src/`.

| Escenario | Estado | Test | Nota |
|---|---|---|---|
| El personal inicia sesión y llega a Inicio | Cubierto | `features/auth/Login.test.tsx:29` y `features/auth/FinSesion.test.tsx:107` | Comprueba Inicio, POST y cabecera CSRF, almacenamiento vacío y acceso al Inicio protegido tras volver a ingresar. |
| Un envío pendiente no duplica el login | Cubierto | `features/auth/Login.test.tsx:41` | Reenvía el formulario con el pedido pendiente y comprueba una sola llamada de login además de `/me`. |
| Todos los rechazos de credenciales muestran el mismo mensaje | Cubierto | `features/auth/Login.test.tsx:50` | Las cuatro variantes muestran el mismo mensaje y ayuda sin el detalle de la causa. |
| Un fallo técnico de ingreso no se presenta como credenciales inválidas | Cubierto | `features/auth/Login.test.tsx:61` | Red, `403` y `500`: afirma el mensaje de ingreso no completado, la ausencia del texto de credenciales y el botón habilitado para reintentar. |
| Una recarga recupera la identidad desde el servidor | Cubierto | `features/auth/Sesion.test.tsx:31` y `app/AppRutas.test.tsx:23` | Retiene el contenido hasta resolver `/me`, abre la vista solicitada y aplica al menú y al acceso directo el rol recibido. |
| Entrar al área interna sin sesión lleva a login | Cubierto | `features/auth/Sesion.test.tsx:43` | Comprueba que se monta el login con acción `REPLACE` y no aparece contenido interno. |
| Una operación interna con sesión vencida lleva a login | Cubierto | `features/auth/FinSesion.test.tsx:83` | Afirma `/login REPLACE` y desaparición del contenido interno tras el `401`. |
| Un 403 interno no cierra la sesión | Cubierto | `features/auth/FinSesion.test.tsx:91` | La consulta es rechazada y la pantalla interna sigue visible. |
| Un fallo técnico al restaurar sesión permite reintentar | Cubierto | `features/auth/Sesion.test.tsx:50` | Comprueba error de red sin contenido interno ni login, y una segunda consulta exitosa. |
| Cerrar sesión lleva a login y no deja volver al contenido interno | Cubierto | `features/auth/FinSesion.test.tsx:60` | Espera la consulta inicial simulada; verifica `204` y `401`, POST con CSRF, vuelta al login y retroceso sin contenido interno. |
| Un fallo de logout permite reintentar sin anunciar el cierre | Cubierto | `features/auth/FinSesion.test.tsx:73` | `403` y `500`: exige mensaje de cierre no confirmado, sesión local conservada y reintento que llega a login. |
| El menú y el acceso directo respetan el mismo permiso | Cubierto | `app/AppRutas.test.tsx:23` | Compara los tres roles, entrada y página activa para `ADMIN`, y acceso denegado sin salir del layout para los demás. |
| El odontólogo ve lectura pero no acciones operativas de escritura | Cubierto | `app/AppRutas.test.tsx:38` y `:23` | Comprueba vista de lectura, ausencia de menú/control de escritura y denegación al entrar directamente. |
| Una ruta pública no depende de la sesión del personal | Cubierto | `features/auth/FinSesion.test.tsx:98` | Un `401` público no lleva a login ni muestra menú interno; verifica que no se consulta `/me`. |

## Ejecución de tests

- Backend, `.\mvnw.cmd -q verify` con PostgreSQL 16 en Testcontainers: **95 unitarios + 186 de integración aprobados**, 0 fallidos, 0 errores y 0 omitidos (reportes Surefire y Failsafe).
- Frontend, `npx vitest run` tras el apply correctivo: **87 aprobados, 0 fallidos, 0 omitidos** (18 archivos), con concurrencia por defecto y mientras corría Maven.
- Frontend, `npm run lint` y `npm run build`: exitosos.

## Reglas de negocio y reglas duras

- **RN-33:** el control del frontend es de navegación y presentación, no de autorización. Las pruebas distinguen `ADMIN`, `RECEPCION` y `ODONTOLOGO`; la API no cambia. El rechazo de escritura para `ODONTOLOGO` está cubierto también por `SecurityConfigTest.java:171`.
- **RN-34:** el frontend no infiere el bloqueo, no diferencia el contenido de los `401` y no guarda identidad ni credenciales en almacenamiento web. C-03 mantiene la validación del servidor: `SesionPersonalIT.java:75` comprueba rechazo uniforme y `AutenticacionPersonalIT.java:50` el límite de bloqueo.
- `git diff origin/master...HEAD` está vacío porque no hay commit de C-16. Se revisaron el diff local y los archivos nuevos: sin cambios a backend, `.env`, dependencias, CSS ni datos reales; los valores de tests son ficticios. `git diff --check` pasó.

## Diferencias con design.md

No se encontraron contradicciones de implementación. El proveedor vive bajo el router, `/login` y las rutas públicas quedan fuera del layout interno, y el guard y el menú comparten `tieneRol`. La SPA no persiste identidad, usa `Navigate` con `replace` y descarta `401` de ciclos de sesión anteriores. Bajo la política `system-only`, se comprobó que el formulario y logout reutilizan el kit y sus tokens, sin estilos fijos nuevos.

## Qué NO se pudo verificar

- El fallo de espera inicial del botón de logout de la primera verificación no se reprodujo tras sincronizar la consulta inicial en el test. No se estableció la causa original.
- La prueba real de navegador está registrada en `tasks.md`; no se repitió en la reverificación, que siguió a un cambio exclusivo de tests.

## Próximo paso

Continuar con el PR del change a `master`, con revisión de Juan Pablo. Una vez integrado, sincronizar y archivar con `/opsx:archive c-16-frontend-login-y-guards`.

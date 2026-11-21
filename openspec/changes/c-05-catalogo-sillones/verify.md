# Verify — c-05-catalogo-sillones (2026-11-21)

**Veredicto: LISTO PARA ARCHIVAR (0 bloqueantes).**

**Árbol objetivo:** `origin/master` en `21b13ee610a19d2c174cb68890659241179bc9f1` (PR #55, C-03 integrado) + exclusivamente C-05. Esta verificación reemplaza la anterior, que se hizo sobre otro árbol.

## Validación estructural

`openspec status --change c-05-catalogo-sillones --json`: `spec-driven`, 4/4 artefactos completos. `openspec validate c-05-catalogo-sillones --strict`: válido.

## Tareas

8/8 marcadas en [`tasks.md`](tasks.md); ninguna pendiente en el checklist. El marcado y [`cobertura.md`](cobertura.md) no se tomaron como evidencia de ejecución. No se modificó el checklist.

## Matriz escenario → test

Los 16 escenarios están cubiertos y sus pruebas pasaron. Los títulos coinciden exactamente con sus `@DisplayName`. Las rutas se leen a partir de `backend/src/test/java/ar/edu/utn/turnos/profesionales/sillon/`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| Catálogo | Los tres roles consultan sillones activos e inactivos | Cubierto | `web/SillonesControllerTest.java:37` | Parametriza los tres roles. Verifica `200` y los cuatro campos en el listado y el detalle de ambos sillones. |
| Catálogo | Consultar un ID inexistente devuelve 404 | Cubierto | `web/SillonesControllerTest.java:67` | Verifica `404`, `application/problem+json` y `SILLON_NO_ENCONTRADO` con los tres roles. |
| Catálogo | El administrador da de alta un sillón activo | Cubierto | `web/SillonesApiIT.java:54–68` | Afirma `201`, nombre normalizado, descripción nula y `activo=true`. El ID entero del cuerpo se compara exactamente con el ID generado que se lee de PostgreSQL; `Location` debe ser `/api/sillones/{id}` con ese mismo ID. |
| Catálogo | El administrador edita un sillón sin cambiar su identidad | Cubierto | `web/SillonesApiIT.java:72` | Afirma `200`, mismo ID, nombre y descripción actualizados, y contrasta cambios en la fila. |
| Catálogo | El nombre duplicado devuelve 409 incluso si el otro sillón está inactivo | Cubierto | `web/SillonesApiIT.java:89` | `POST` y `PUT`, con espacios alrededor y contra ambos estados; comprueba `409` Problem Details, un único registro con el nombre y las dos filas intactas. |
| Catálogo | Dos altas concurrentes del mismo nombre no crean duplicados | Cubierto | `web/SillonesApiIT.java:116` | Dos solicitudes liberadas por barrera; exige un `201`, un `409` Problem Details y una fila, consultada desde otra transacción. |
| Catálogo | Una escritura con campos inválidos devuelve 400 | Cubierto | `web/SillonesApiIT.java:145` | Nombre vacío en alta y edición; `activo` omitido o textual. Afirma `400` Problem Details con el campo correcto, total y fila inicial intactos. |
| Catálogo | Actualizar un ID inexistente devuelve 404 | Cubierto | `web/SillonesApiIT.java:168` | Afirma `404` Problem Details y que no se creó una fila con el nombre enviado. |
| Catálogo | Desactivar un sillón conserva su registro | Cubierto | `web/SillonesApiIT.java:180`, `:191` | Verifica `200`, mismo ID, detalle y listado con `activo=false`, una fila y exclusión del puerto. |
| Catálogo | Reactivar un sillón conserva su registro | Cubierto | `web/SillonesApiIT.java:186`, `:191` | Verifica `200`, mismo ID, detalle y listado con `activo=true`, una fila y regreso al puerto. |
| Catálogo | Recepción y odontología no pueden escribir en el catálogo | Cubierto | `web/SillonesApiIT.java:219` | Ambos roles intentan alta, edición, baja y reactivación con CSRF válido. Afirma `403` Problem Details y ausencia de efectos. |
| Catálogo | Un cliente sin sesión no puede acceder al catálogo | Cubierto | `web/SillonesApiIT.java:239`, `web/SillonesSesionIT.java:81` | Listado y detalle, las cuatro escrituras con CSRF válido y `POST` y `PUT` sin CSRF. Afirman `401` Problem Details. |
| Catálogo | Un administrador sin CSRF válido no puede escribir en el catálogo | Cubierto | `web/SillonesApiIT.java:262`, `web/SillonesSesionIT.java:65` | Las cuatro escrituras con CSRF ausente o inválido; `403` Problem Details y sin efectos. La prueba de sesión real repite el rechazo de alta. |
| Catálogo | Agenda deja de ofrecer un sillón desactivado | Cubierto | `infrastructure/SillonConsultaIT.java:29` | Contrasta dos activos antes y el otro como único candidato después; conserva el inactivo en consulta por ID. |
| Catálogo | Agenda vuelve a ofrecer un sillón reactivado | Cubierto | `infrastructure/SillonConsultaIT.java:42` | Pasa de lista vacía a incluirlo con ID original y estado activo. |
| Catálogo | Agenda distingue un sillón inactivo de uno inexistente | Cubierto | `infrastructure/SillonConsultaIT.java:53` | Afirma el estado de los dos existentes y ausencia solo para el ID inexistente. |

## Ejecución de tests

| Comando en la copia de la base publicada + C-05 | Pasados / fallidos / omitidos | Resultado |
|---|---|---|
| `backend/`: `.\mvnw.cmd -B verify` | 113 / 0 / 0 | `BUILD SUCCESS`, salida 0. Surefire: 44 (incluye ArchUnit). Failsafe: 69 con Testcontainers y PostgreSQL 16.16. |
| `frontend/`: `npm.cmd ci --no-audit --no-fund`, luego `npx.cmd vitest run` | 8 / 0 / 0 | 6 archivos, Vitest 1.6.1. C-05 no modifica frontend; es una comprobación de regresión de la base. |

Se usó `git archive origin/master` en una carpeta temporal y se superpusieron 18 archivos (12 de código/tests de `profesionales/sillon` y 6 artefactos de C-05, sin el informe anterior), sin colisiones y con hashes verificados. No se copiaron C-06, C-08, ajustes locales ajenos ni `.env`. La copia conserva solamente el `.env.example` versionado de la base.

Los reportes de Failsafe confirman `SillonesApiIT` 34/34, incluida la nueva aserción de `daDeAlta`, y `SillonesSesionIT` 3/3. Se verificaron el `401` Problem Details en `POST` y `PUT` anónimos sin CSRF, el `403` con sesión ADMIN y CSRF ausente o inválido, y el alta y la edición con sesión ADMIN, cookie `XSRF-TOKEN` y cabecera `X-XSRF-TOKEN`.

## Reglas de negocio y reglas duras

- **RN-37 / US-004:** la unicidad se comprueba en API y PostgreSQL; baja y reactivación mantienen la misma fila y el puerto devuelve solo candidatos activos. C-30 agrega la guarda de turnos futuros activos. El puerto no verifica disponibilidad ni prueba su uso por reserva pública o creación de turnos.
- **RN-01, RN-02 y RN-07:** el catálogo no crea turnos, no valida solapamientos/bordes y no trata el token del paciente. La concurrencia cubierta es la de altas de sillón, no la de reservas. SU-08 / PA-12: no se agregaron bloqueos de sillón por intervalo.
- **RBAC:** coincide con `knowledge-base/03_actores_y_roles.md`: solo ADMIN escribe. Los tests intentan las cuatro escrituras como `RECEPCION` y `ODONTOLOGO`.
- **Alcance del diff:** se revisó la superposición de C-05, incluidos sus archivos sin seguimiento. C-05 no agrega datos reales de pacientes, secretos, archivos de configuración ni entidades expuestas por REST. Las credenciales de `SillonesSesionIT` son fixtures: dominio `.test`, clave ficticia y hash BCrypt en la fila de prueba.

## Diferencias con design.md

- C-01 continúa como única dependencia declarada; el diseño prevé verificar sesión real y precedencia de `401` sin CSRF cuando esté C-03. Esa cadena ya está en la base publicada y C-05 no modifica la seguridad global.
- Los slices publicados de `SecurityConfigTest` (13/13) y `ManejadorErroresTest` (9/9) declaran controladores explícitos: no descubren el controlador de sillones y pasaron sin ajustes locales. `SillonesControllerTest` (6/6) sí carga el controlador y servicio de sillones con seguridad compartida.
- No se detectaron discrepancias de diseño en la revisión: DTOs con validación, servicio transaccional con permisos por método, repositorio interno, ID de identidad y `uk_sillon_nombre` gestionados por Hibernate, `flush` y traducción específica, lecturas `readOnly` y puerto sin dependencia HTTP, baja mediante `PUT` sin `DELETE` ni migrador adicional. No hay UI ni mockups que verificar.

## Qué NO se pudo verificar

- El árbol compartido `nico` con C-06 y C-08 no se probó en esta re-verificación. Tampoco se volvió a probar la base histórica de C-01 sin C-03: no son los árboles objetivo.
- La guarda de C-30 y el uso del puerto por la agenda/reserva no forman parte del change. Tampoco se reconstruyó el historial RED/GREEN a partir del checklist.

## Próximo paso

Continuar con el PR exclusivo de C-05 para revisión de Juan Pablo. Tras la integración, ejecutar `/opsx:archive c-05-catalogo-sillones`. Esta verificación no creó commit, push ni archivo.

# Verify — c-06-catalogo-profesionales (2026-11-21)

**Veredicto:** LISTO PARA ARCHIVAR

**B1 — Resuelto.** Antes del ajuste, un alta con nombre numérico devolvía `201`. Ahora POST y PUT rechazan tipos no textuales para nombre, apellido y matrícula; PUT también rechaza valores no booleanos en `activo`. Las pruebas HTTP comprueban `400` con el campo inválido y que no se persisten cambios. La validación estricta se limita a los DTO de C-06, sin cambiar Jackson globalmente.

## Validación estructural

`openspec status --change c-06-catalogo-profesionales --json`: existen los cuatro artefactos. `openspec validate c-06-catalogo-profesionales --strict`: válido.

## Tareas

[7/7 marcadas](tasks.md), sin tareas abiertas. Se reverificó la validación de entrada de 2.1 después de corregir B1, sin cambiar los checkboxes.

## Matriz escenario → test

Los estados reflejan las aserciones leídas. Los 15 escenarios tienen `@DisplayName` exacto.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| `catalogo-profesionales` | El administrador da de alta un profesional activo | Cubierto | `web/ProfesionalesApiIT.java:38` | 201, ID, Location, especialidad nula y activo persistido. |
| `catalogo-profesionales` | El administrador modifica los datos de un profesional | Cubierto | `web/ProfesionalesApiIT.java:57` | 200, mismo ID, datos nuevos y persistencia. |
| `catalogo-profesionales` | El catálogo rechaza datos obligatorios inválidos | Cubierto | `web/ProfesionalesApiIT.java:73` | Blancos, números, booleanos y objetos en campos textuales obligatorios de POST y PUT; estado omitido, textual, numérico o nulo en PUT. 400 Problem Details, campo y datos intactos. |
| `catalogo-profesionales` | Un alta no puede reutilizar la matrícula de un profesional inactivo | Cubierto | `web/ProfesionalesApiIT.java:101` | Conflicto 409 con código, matrícula recortada y ninguna fila nueva. |
| `catalogo-profesionales` | Una actualización no puede tomar la matrícula de otro profesional | Cubierto | `web/ProfesionalesApiIT.java:114` | Conflicto 409 con código y ambas filas intactas. |
| `catalogo-profesionales` | El administrador desactiva y reactiva el mismo registro | Cubierto | `web/ProfesionalesApiIT.java:128` | Ambos PUT y detalles confirman 200, mismo ID y estado; se comprueba la fila. |
| `catalogo-profesionales` | El catálogo no permite borrar físicamente un profesional | Cubierto | `web/ProfesionalesApiIT.java:145` | DELETE con CSRF válido devuelve 405 y conserva la fila. |
| `catalogo-profesionales` | Administración y recepción consultan activos e inactivos | Cubierto | `web/ProfesionalesApiIT.java:156` | Ambos roles y estados, detalle y listado ordenado por ID. |
| `catalogo-profesionales` | Consultar o actualizar un profesional inexistente responde 404 | Cubierto | `web/ProfesionalesApiIT.java:178` | 404 con código en detalle y actualización, sin nuevas filas. |
| `catalogo-profesionales` | Un odontólogo sin vínculo no puede leer el catálogo | Cubierto | `web/ProfesionalesApiIT.java:226` | Ambos GET devuelven 403 Problem Details sin exponer matrícula. |
| `catalogo-profesionales` | Un odontólogo vinculado solo puede consultar su propio registro | Cubierto | `web/ProfesionalesTitularidadIT.java:31` | Con doble: un único resultado, detalle propio, 403 para ID ajeno existente e inexistente. |
| `catalogo-profesionales` | Recepción y odontología no pueden escribir en el catálogo | Cubierto | `web/ProfesionalesApiIT.java:196` | Ambos roles, POST y PUT con CSRF válido: 403 Problem Details, sin modificaciones. |
| `catalogo-profesionales` | Una consulta anónima del catálogo requiere autenticación | Cubierto | `web/ProfesionalesApiIT.java:216` | Listado y detalle devuelven 401 Problem Details. |
| `catalogo-profesionales` | La consulta interna de candidatos excluye inactivos | Cubierto | `infrastructure/ProfesionalConsultaIT.java:25` | Sin activos da vacío; con mezcla devuelve solo los activos en orden por ID. |
| `catalogo-profesionales` | La consulta interna por ID distingue inactivo de inexistente | Cubierto | `infrastructure/ProfesionalConsultaIT.java:37` | Verifica estado activo, inactivo y ausencia explícita. |

Rutas de la matriz relativas a `backend/src/test/java/ar/edu/utn/turnos/profesionales/profesional/`.

## Ejecución de tests

- Backend en la rama exclusiva de C-06, sobre `origin/master` (`21b13ee`, incluye C-03 pero no C-05): `.\mvnw.cmd -B verify`, 36 pruebas unitarias/web y 46 de integración con PostgreSQL, 0 fallos, 0 errores, 0 saltos.
- La verificación previa sobre la base C-01 (`d3480a1`), con el alcance de dos tests MVC acotado localmente, también pasó: 27 pruebas unitarias/web y 26 de integración.
- Backend en la copia original con C-03 y C-05, verificado previamente: el mismo comando pasó 44 pruebas unitarias/web y 89 de integración con PostgreSQL, 0 fallos, 0 errores, 0 saltos.
- Frontend: `npx --no-install vitest run` en `frontend/`: 8 pasaron, 0 fallos, 0 saltos (6 archivos). C-06 no modifica frontend.
- Regresión B1: `.\mvnw.cmd -B -Dtest=ArquitecturaTest '-Dit.test=ProfesionalesApiIT#rechazaObligatoriosInvalidos' verify` confirmó RED HTTP (`201` en vez de `400`). La suite completa posterior a la corrección confirma GREEN, incluidos los casos adicionales de tipos incorrectos.

## Reglas de negocio y reglas duras

- **RN-37:** la baja conserva fila e ID, no hay borrado físico y el puerto de candidatos excluye inactivos. La guarda ante turnos futuros corresponde a C-30; integración con la reserva pública y creación de turnos no entra en este change. RN-01, RN-02 y RN-07 no aplican.
- **US-005:** cubierto el catálogo; prestaciones y vínculo real de usuario quedan en C-10 y C-13.
- El diff de la rama contra `origin/master` contiene solo archivos de C-06. Los ajustes previos al alcance de `SecurityConfigTest` y `ManejadorErroresTest` ya están en `master`. No se identificaron secretos, datos reales de pacientes ni entidades JPA expuestas en C-06. Las consultas usan repositorio derivado o parametrización; los tests de persistencia usan PostgreSQL. `git diff --check` sin observaciones.

## Diferencias con design.md

- B1 corregido sin cambios transversales: los DTO de C-06 verifican el tipo antes de delegar en Bean Validation, que conserva el formato compartido de errores.
- Se comprobó el aislamiento de la entidad y el repositorio, la restricción única nombrada y la traducción selectiva a 409, la consulta de activos en PostgreSQL, la verificación de titularidad antes de consultar un ID ajeno y el punto de extensión antes de la baja. No se observaron otras diferencias mayores.

## Qué NO se pudo verificar

- Vínculo real de C-13, guarda de turnos futuros de C-30 y consumidores finales de agenda/reserva: fuera del alcance.

## Próximo paso

Presentar el cambio para revisión de Juan Pablo y, cuando corresponda, usar `/opsx:archive c-06-catalogo-profesionales`.

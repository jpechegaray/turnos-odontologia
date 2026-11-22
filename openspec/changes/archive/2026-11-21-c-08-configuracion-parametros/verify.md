# Verify — c-08-configuracion-parametros (2026-11-21)

**Veredicto: LISTO PARA ARCHIVAR.** Bloqueante y coberturas parciales resueltos en la segunda pasada.

## Validación estructural

`openspec status` confirmó propuesta, diseño, tareas y delta. `openspec validate c-08-configuracion-parametros --strict` pasó.

## Tareas

9/9 marcadas; ninguna pendiente ni diferida en `tasks.md`.

## Matriz escenario → test

Rutas de la tabla relativas a `backend/src/test/java/ar/edu/utn/turnos/configuracion/`. Los 13 nombres coinciden exactamente con `@DisplayName`.

| Spec | Escenario | Estado | Test | Nota |
|---|---|---|---|---|
| configuracion-parametros | El primer arranque inicializa los siete valores por defecto | Cubierto | `ConfiguracionInicialIT.java:25` | Lee PostgreSQL tras el arranque y compara exactamente las siete claves y valores. |
| configuracion-parametros | La inicialización repone solo claves faltantes | Cubierto | `ConfiguracionInicialIT.java:43` | Elimina una fila, edita otra y comprueba recuperación, cantidad, valor y fecha preservados. |
| configuracion-parametros | Admin y Recepción consultan los parámetros vigentes | Cubierto | `ConfiguracionApiIT.java:59` | Ambos roles reciben 200 y el objeto plano estricto; el GET posterior a una edición también comprueba valores nuevos. |
| configuracion-parametros | Odontólogo no puede consultar configuración | Cubierto | `ConfiguracionApiIT.java:67` | Afirma 403 y Problem Details. |
| configuracion-parametros | Un cliente anónimo no puede consultar configuración | Cubierto | `ConfiguracionApiIT.java:75` | Afirma 401 y Problem Details. |
| configuracion-parametros | Admin reemplaza toda la configuración con límites válidos | Cubierto | `ConfiguracionApiIT.java:85`; `ConfiguracionReemplazoIT.java:74` | Afirma 200, respuesta completa, GET posterior y extremos admitidos. Verifica las siete fechas: nuevo instante común en las tres claves modificadas, sin cambio en las demás. |
| configuracion-parametros | Repetir el mismo reemplazo no cambia las fechas | Cubierto | `ConfiguracionApiIT.java:112` | Afirma 200, JSON y filas completas idénticas, incluidas fechas. |
| configuracion-parametros | Dos reemplazos concurrentes no mezclan valores | Cubierto | `ConfiguracionReemplazoIT.java:113` | Dos transacciones terminan con éxito y las dos claves que distinguen los reemplazos coinciden con el segundo conjunto, no con una mezcla. |
| configuracion-parametros | Recepción y Odontólogo no pueden editar configuración | Cubierto | `ConfiguracionApiIT.java:123` | Ambos roles usan CSRF válido: 403 Problem Details y filas intactas. |
| configuracion-parametros | Admin no puede editar configuración sin CSRF válido | Cubierto | `ConfiguracionApiIT.java:134` | Prueba token ausente e inválido: 403 Problem Details y filas intactas. |
| configuracion-parametros | Un cuerpo que no coincide con el contrato devuelve 400 | Cubierto | `ConfiguracionApiIT.java:153` | Cubre JSON mal formado, claves faltantes/extra/repetidas, valores `null` y tipos incorrectos, además de cuerpo vacío y documento extra. Afirma 400 y filas intactas. |
| configuracion-parametros | Un valor inválido devuelve RN-36 sin guardar los demás | Cubierto | `ConfiguracionApiIT.java:164`; `ParametroConfiguracionTest.java:31` | Verifica 422, `RN-36`, valores y fechas intactos, incluido un nombre con `U+0000` junto con otro cambio válido. |
| configuracion-parametros | El consumidor lee valores nuevos sin alterar su resultado anterior | Cubierto | `ConfiguracionServicioIT.java:27` | Ejecuta el reemplazo productivo, comprueba las siete lecturas tipadas y consulta las decisiones anterior y nueva por separado en el consumidor de prueba. |

## Ejecución de tests

| Comando | Pasaron | Fallaron | Omitidas |
|---|---:|---:|---:|
| `.\mvnw.cmd verify` desde `backend/` | 195 (71 unitarias/web y 124 de integración con Docker y PostgreSQL 16) | 0 | 0 |
| `npx vitest run` desde `frontend/` | 8 (6 archivos) | 0 | 0 |

La suite se ejecutó en el árbol de trabajo actual, sobre `nico-c-05-catalogo-sillones` (`0f61658`), con el código local de C-06 y el ajuste autorizado del reloj de prueba de C-03.

Para preparar el PR se repitió `.\mvnw.cmd verify` desde el árbol aislado `nico-c-08-configuracion-parametros`, basado en `origin/master` (`2d3c3a5`). Pasaron nuevamente las 195 pruebas del backend, sin fallos ni omitidas; también pasó la validación estricta de OpenSpec. No se copiaron cambios locales de C-05 ni C-06.

## Reglas de negocio y reglas duras

- **RN-05 y RN-29:** se verifican los defaults de 24 h, extremos y edición del plazo y de la anticipación. Cancelaciones reales y envío de recordatorios quedan fuera; PA-05 sigue para C-37. Los defaults y validación de los parámetros de RN-23 y RN-27 también están probados, no sus flujos consumidores.
- **RN-36:** acceso de escritura solo a Admin con CSRF, rangos inclusivos, enteros positivos, rechazo de fracciones, desborde y nombre en blanco o con `U+0000`, y ausencia de cambios parciales en los casos probados.
- No se encontraron datos reales de pacientes, credenciales ni cambios de configuración con secretos en los archivos atribuibles a C-08. No hay DTO de respuesta que exponga entidades JPA ni SQL concatenado. `git diff master...HEAD` incluye otros changes y no contiene los archivos no rastreados de C-08; por eso se inspeccionaron esos archivos directamente y se revisó por separado el diff de `AutenticacionPersonalIT`.

## Diferencias con design.md

- **Hallazgo resuelto:** la prueba nueva reprodujo HTTP 500 antes de corregir el catálogo. La segunda ejecución confirma HTTP 422 `RN-36` y filas intactas. La spec y el diseño explicitan la restricción de PostgreSQL.
- No quedaron contradicciones mayores: catálogo cerrado, persistencia con Hibernate, lecturas sin caché, bloqueo ordenado dentro de la transacción, validación JSON local y permisos en el adaptador web. El ajuste de C-03 solo afecta el reloj de un test.

## Qué NO se pudo verificar

- No se verificó contra un árbol aislado basado únicamente en C-01, ni el aprovisionamiento de producción con `ddl-auto=validate`. No corresponde verificación visual: C-08 no incluye UI.

## Próximo paso

El change está listo para revisión y posterior `/opsx:archive c-08-configuracion-parametros`, según el flujo del proyecto.

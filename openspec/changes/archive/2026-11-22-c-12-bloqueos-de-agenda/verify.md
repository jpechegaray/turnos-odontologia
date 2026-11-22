# Verify — c-12-bloqueos-de-agenda (2026-11-21)

**Veredicto: LISTO PARA ARCHIVAR.** Sin bloqueantes; 14/14 escenarios cubiertos.

## Validación estructural

`openspec validate c-12-bloqueos-de-agenda --strict`: válido. `openspec status` confirma propuesta, diseño, delta spec y tareas.

## Tareas

7/7 completas en [tasks.md](tasks.md); ninguna pendiente.

## Matriz escenario → test

Spec: [bloqueos-de-agenda](specs/bloqueos-de-agenda/spec.md). Las líneas indicadas corresponden al `@DisplayName` exacto y se revisaron las aserciones de cada escenario.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| bloqueos-de-agenda | Administración y recepción crean y listan bloqueos del profesional solicitado | Cubierto | [BloqueosApiIT:62][api62] | Ambos roles: 201 e ID generado, extremos en UTC, listado 200 filtrado y ordenado con empate. |
| bloqueos-de-agenda | Un profesional sin bloqueos devuelve un listado vacío | Cubierto | [BloqueosApiIT:86][api86] | Ambos roles reciben 200 y `[]`. |
| bloqueos-de-agenda | El alta rechaza fechas o motivos inválidos | Cubierto | [BloqueosApiIT:94][api94] | Fechas ausentes, sin offset, exceso de precisión y orden inválido; motivo ausente, blanco o largo. 400 Problem Details y sin inserción. |
| bloqueos-de-agenda | Gestionar bloqueos de un profesional inexistente devuelve 404 | Cubierto | [BloqueosApiIT:112][api112] | GET, POST y DELETE devuelven 404 Problem Details y `PROFESIONAL_NO_ENCONTRADO` para ambos roles. |
| bloqueos-de-agenda | Eliminar un bloqueo lo retira del listado y de la consulta de solapamiento | Cubierto | [BloqueosApiIT:124][api124] | 204 sin cuerpo, listado vacío y consulta interna de verdadero a falso. |
| bloqueos-de-agenda | Un bloqueo ausente o de otro profesional no se puede eliminar | Cubierto | [BloqueosApiIT:140][api140] | Mismo 404 Problem Details y código en ambos casos; se conservan los registros de ambos profesionales. |
| bloqueos-de-agenda | El odontólogo vinculado lista sus bloqueos pero no los ajenos | Cubierto | [BloqueosApiIT:156][api156] | 200 con su único bloqueo y 403 Problem Details tanto para el ajeno como para el ID inexistente. |
| bloqueos-de-agenda | El odontólogo sin vínculo no puede listar bloqueos | Cubierto | [BloqueosApiIT:174][api174] | 403 Problem Details sin motivo del bloqueo. |
| bloqueos-de-agenda | El odontólogo no puede crear ni eliminar bloqueos | Cubierto | [BloqueosApiIT:184][api184] | POST y DELETE con CSRF válido, incluso con cuerpo o ID malformado: 403 y registro intacto. |
| bloqueos-de-agenda | Las escrituras de bloqueos requieren CSRF válido | Cubierto | [BloqueosApiIT:199][api199] | Para ambos roles y métodos, CSRF ausente o inválido devuelve 403 y no modifica datos. |
| bloqueos-de-agenda | Consultar o escribir bloqueos sin sesión devuelve 401 | Cubierto | [BloqueosApiIT:217][api217] | Los tres métodos devuelven 401 Problem Details y no modifican datos. |
| bloqueos-de-agenda | El candidato que cruza un bloqueo del mismo profesional solapa | Cubierto | [BloqueoConsultaIT:38][consulta38] | 10:30–11:30 contra 10:00–11:00 devuelve verdadero en PostgreSQL. |
| bloqueos-de-agenda | El candidato contiguo a cualquiera de los extremos no solapa | Cubierto | [BloqueoConsultaIT:44][consulta44] | Devuelve falso en los dos bordes. |
| bloqueos-de-agenda | Los bloqueos de otro profesional no afectan la consulta | Cubierto | [BloqueoConsultaIT:51][consulta51] | Mismo intervalo para otro profesional devuelve falso. |

## Ejecución de tests

- Backend: `.\mvnw.cmd -q verify` en `backend`. **270 aprobados** (85 unitarios y 185 de integración), 0 fallidos, 0 errores y 0 omitidos según Surefire y Failsafe. Se ejecutó con Docker y PostgreSQL 16 mediante Testcontainers.
- Frontend de regresión: `npx --no-install vitest run` en `frontend`, tras `npm ci`. **64 aprobados**, 0 fallidos, 0 omitidos, en 14 archivos. C-12 no agrega frontend.

## Reglas de negocio y reglas duras

- **RN-02:** están probados el cruce real y ambos bordes contiguos de `[inicio, fin)`.
- **RN-04:** está probado el puerto de bloqueos por profesional que consumirá la agenda. Esta entrega no implementa validación de horarios ni rechazo de un alta de turno (C-19).
- **RN-33:** gestión probada para `ADMIN` y `RECEPCION`; para `ODONTOLOGO`, lectura propia con vínculo y escritura denegada. CSRF y solicitudes sin sesión se verificaron por separado.
- **RN-17:** el rechazo de bloqueos sobre turnos activos queda en C-30, explícitamente fuera de C-12. RN-01/RN-15 (turnos concurrentes y `EXCLUDE`) y RN-07 (token de paciente) no aplican.
- Se revisaron los archivos modificados y los nuevos respecto de la base del change (`HEAD` y `origin/master`, `ada1f0e` al verificar); `master` local está atrasado y C-12 aún no tiene commit. No se encontraron datos reales de pacientes, secretos ni cambios de `.env` o de configuración de credenciales. Los fixtures son ficticios. Se usan DTOs, validación de entrada, JPQL parametrizado, `Instant` con persistencia en `timestamptz` y Testcontainers, sin H2 ni migradores nuevos.

## Diferencias con design.md

Sin diferencias mayores ni menores observadas. Se verificaron los tres endpoints, el borrado por par de IDs y la autorización de escritura antes de MVC y en servicio. La lectura reutiliza titularidad de C-06. `Bloqueo` declara el índice y el `CHECK`; `BloqueoJpaIT` verifica `timestamptz`, el rechazo de rangos inválidos y la FK sin duplicados al repetir el script. El puerto usa extremos estrictos y no depende de la sesión.

## Qué NO se pudo verificar

- El vínculo productivo de odontología corresponde a C-13; aquí se verificó con el adaptador ficticio previsto en el diseño.
- No se probó un despliegue con `ddl-auto=validate` ni se aprovisionó una base de producción. La creación y las restricciones se probaron en PostgreSQL de integración.
- No corresponde verificación visual: C-12 no modifica la UI.

## Próximo paso

Preparar el PR a `master` para revisión de Juan Pablo (con autorización previa del commit). Una vez integrado, ejecutar `/opsx:archive c-12-bloqueos-de-agenda`.

[api62]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L62
[api86]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L86
[api94]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L94
[api112]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L112
[api124]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L124
[api140]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L140
[api156]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L156
[api174]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L174
[api184]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L184
[api199]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L199
[api217]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/web/BloqueosApiIT.java#L217
[consulta38]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/infrastructure/BloqueoConsultaIT.java#L38
[consulta44]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/infrastructure/BloqueoConsultaIT.java#L44
[consulta51]: ../../../backend/src/test/java/ar/edu/utn/turnos/profesionales/bloqueo/infrastructure/BloqueoConsultaIT.java#L51

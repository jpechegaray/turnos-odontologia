# Verify — c-39-gestion-turno-por-token (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes).

## Validación estructural

`openspec validate c-39-gestion-turno-por-token --strict`: válido. Se verificó el árbol de trabajo, incluidos los archivos nuevos aún sin commit, contra su base `7388755`; `git diff --check` no informó errores en las modificaciones rastreadas. `origin/master` ya incluye el archivo de C-35: su delta de `agenda-turnos` agrega requisitos de reprogramación, no modifica los tres requisitos que toca C-39 y no trae cambios de código respecto de la base probada.

## Tareas

6/6 marcadas como completas; sin tareas diferidas. Las dos brechas de cobertura de la primera revisión se corrigieron solo en pruebas.

## Matriz escenario → test

Los archivos `*IT.java` citados están en `backend/src/test/java/ar/edu/utn/turnos/`: `GestionTurnoApiIT`, `CancelarTurnoPorTokenIT` y `ProteccionEnlaceTurnoIT` en `reservapublica/gestiontoken/`; las dos clases de cancelación del personal en `agenda/web/`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| `agenda-turnos` | Un turno ya cancelado no se vuelve a cancelar | Cubierto | `TurnosCancelacionApiIT.java:187` | `409`, código y fila de la primera cancelación sin cambios. |
| `agenda-turnos` | Un turno atendido no se puede cancelar | Cubierto | `TurnosCancelacionApiIT.java:201` | `409` con código de transición inválida; sigue atendido. |
| `agenda-turnos` | Un turno que ya empezó no se puede cancelar | Cubierto | `TurnosCancelacionApiIT.java:212` | Inicio igual al reloj; `422 RN-05`, sigue programado. |
| `agenda-turnos` | Dos cancelaciones simultáneas del mismo turno | Cubierto | `TurnosCancelacionConcurrenciaIT.java:91` | `200` y `409`; un único evento de hueco liberado. |
| `agenda-turnos` | El odontólogo no puede cancelar turnos | Cubierto | `TurnosCancelacionApiIT.java:332` | `403` incluso con cuerpo inválido; no cambia el turno ni libera hueco. |
| `agenda-turnos` | Sin sesión no se puede cancelar un turno | Cubierto | `TurnosCancelacionApiIT.java:363` | `401` también con un token de turno válido; estado intacto. |
| `agenda-turnos` | Sin CSRF válido no se puede cancelar un turno | Cubierto | `TurnosCancelacionApiIT.java:375` | `403` con sesión y cabecera incorrecta; estado intacto. |
| `token-turno` | El enlace válido devuelve solo el detalle público de su turno | Cubierto | `GestionTurnoApiIT.java:48` | `200`, seis campos exactos y datos del primero de dos turnos. |
| `token-turno` | El token del turno A no permite operar sobre el turno B | Cubierto | `GestionTurnoApiIT.java:67` | Las tres rutas con selector ajeno dan `404`; ambos turnos siguen programados. |
| `token-turno` | La consulta distingue el plazo exacto de la cancelación tardía | Cubierto | `GestionTurnoApiIT.java:82` | Plazo configurado de 12 h. El mismo token devuelve el mismo inicio y pasa de `false` a `true` al avanzar el `Clock` un segundo. |
| `token-turno` | No existe reprogramación para el enlace del paciente | Cubierto | `GestionTurnoApiIT.java:100` | Envía un horario alternativo, recibe `404` y verifica en PostgreSQL que inicio, fin, estado y versión no cambian. |
| `token-turno` | El paciente confirma su turno sin sesión ni CSRF | Cubierto | `GestionTurnoApiIT.java:150` | `200`, estado guardado en PostgreSQL y enlace reutilizable. |
| `token-turno` | Volver a confirmar no modifica el turno confirmado | Cubierto | `GestionTurnoApiIT.java:163` | `409` con código; mismo estado y versión. |
| `token-turno` | El paciente cancela en plazo y el intervalo se puede volver a reservar | Cubierto | `CancelarTurnoPorTokenIT.java:60` | `204`, auditoría y reinserción del intervalo con los mismos recursos en PostgreSQL. |
| `token-turno` | Cancelar tarde un turno confirmado libera el intervalo y revoca sus enlaces | Cubierto | `CancelarTurnoPorTokenIT.java:72`; `GestionTurnoApiIT.java:113` | `204`, tardanza y reinserción; revoca las dos credenciales y ambas dan el `404` estándar. |
| `token-turno` | Dos cancelaciones públicas simultáneas liberan un solo hueco | Cubierto | `CancelarTurnoPorTokenIT.java:109`; `GestionTurnoApiIT.java:113` | `204` y `404` con código uniforme; un evento de cada tipo. |
| `token-turno` | Las tres operaciones devuelven un 404 indistinguible para credenciales rechazadas | Cubierto | `GestionTurnoApiIT.java:113` | Cuatro clases de rechazo por cada método; compara todo el cuerpo, comprueba que no lleva el secreto y que no hay cambios de estado ni uso. |
| `token-turno` | El límite por IP cubre lecturas y mutaciones del enlace | Cubierto | `GestionTurnoApiIT.java:177` | Agota cupo con tokens válidos e inválidos; los tres métodos devuelven `429` con `Retry-After` y no actualizan estado ni último uso. |
| `token-turno` | Ninguna respuesta ni registro expone el token del enlace | Cubierto | `ProteccionEnlaceTurnoIT.java:89` | Tomcat real: sin token en cuerpo, cabeceras, logs de aplicación ni archivos de acceso; `no-store` en éxitos y errores. |

**Balance:** 19 cubiertos, 0 parciales, 0 sin prueba.

## Ejecución de tests

- Backend, reverificación `mvn -B -ntp verify` con JDK 21 y Docker: **BUILD SUCCESS**, 302 unitarias y 613 de integración, 0 fallidas u omitidas. La corrida dirigida posterior al correctivo también pasó: 302 unitarias y 8 integraciones de `GestionTurnoApiIT`.
- Frontend: `npm ci --no-audit --no-fund` y `npx vitest run`: 273 pasaron, 17 fallaron, 0 omitidas (31 de 34 archivos verdes). Los fallos están en `FinSesion.test.tsx` (1), `Pacientes.test.tsx` (7) y `Usuarios.test.tsx` (9): 16 excedieron 5 s y «El administrador cambia el rol de un usuario» no encontró la fila esperada. La corrida coincidió con Maven.
- Frontend, repetición `npx vitest run --maxWorkers=2 --minWorkers=1` (sin cambiar los timeouts): 289 pasaron, 1 falló, 0 omitidas. Los 17 casos de la primera corrida pasaron. Falló por timeout de 5 s «El personal inicia sesión y llega a Inicio» (`frontend/src/features/auth/Login.test.tsx:29`), que había pasado antes. El frontend es idéntico al de la base de C-39; la inestabilidad observada no se atribuye al change, pero la suite no quedó verde.
- Frontend, reverificación `npx vitest run --maxWorkers=2 --minWorkers=1`, antes de iniciar Maven y sin cambiar los timeouts: **290/290 pasaron**, 0 fallidas u omitidas.

<details>
<summary>Pruebas fallidas en la primera corrida de Vitest</summary>

- `FinSesion.test.tsx`: Cerrar sesión lleva a login y no deja volver al contenido interno.
- `Pacientes.test.tsx`: Una ficha con solo teléfono se acepta; La obra social se envía como texto libre; Un DNI duplicado se muestra en el campo DNI sin perder los datos; Un contacto faltante del backend se muestra en los campos de contacto; Un campo inválido del backend se muestra en su campo; Un rechazo por permiso se muestra como alerta; Recepción edita una ficha y la lista se refresca.
- `Usuarios.test.tsx`: El administrador da de alta un usuario de recepción y la lista se refresca; El administrador da de alta un odontólogo vinculado a su profesional; Cambiar a un rol que no es odontólogo descarta el vínculo; Un email duplicado se muestra como alerta sin perder los datos; Un profesional ya vinculado se muestra como alerta; Un vínculo inválido se muestra como alerta; Una contraseña rechazada por la API marca el campo; Un 403 al escribir un usuario se muestra como permiso insuficiente; El administrador cambia el rol de un usuario.

</details>

## Reglas de negocio y reglas duras

- **RN-05:** el código lee `ConfiguracionServicio.plazoCancelacionHoras()` y aplica `PoliticaCancelacion` con `Clock`. Se comprueba el cruce del plazo de 12 horas con el mismo enlace, además de la política de tardanza al registrar la cancelación.
- **RN-06/RN-19:** se conserva C-27. La auditoría almacena actor `PACIENTE_TOKEN`, instante y tardanza, sin usuario ni motivo. PostgreSQL admite ocupar el mismo intervalo tras cancelar, tarde o no. Se publican eventos de agenda, sin enviar ofertas en C-39.
- **RN-07/RN-12:** el enlace resuelve un único turno, no acepta un ID alternativo y no reprograma. Solo `PROGRAMADO` transiciona a `CONFIRMADO`. La ruta interna mantiene sesión, rol y CSRF.
- **RN-25:** C-32 persiste el hash, no el secreto (`ServicioTokenTurnoIT.java:51`); C-39 reusa su rechazo uniforme y revoca antes del commit junto con la cancelación. Se cubre el inicio exacto, la revocación y la ausencia de secretos en los registros probados.
- En los cambios revisados no se encontraron datos reales ni credenciales configuradas: los valores de prueba se generan en ejecución y los pacientes son ficticios. No hay cambios de esquema, exposición de entidades JPA ni sustitución de PostgreSQL por H2.

## Diferencias con design.md

- Sin desviaciones mayores en rutas, fronteras de transacción, bloqueo ni revocación. El límite por IP no fue modificado; el filtro de caché se ejecuta antes del limitador.
- **Menor:** cuando se habilita el access log de Tomcat, el patrón seguro omite URL y cabeceras para **todas** las rutas, no solo para la familia de C-39. No cambia los contratos de esas rutas.

## Qué NO se pudo verificar

- La configuración de un proxy o sistema de observabilidad externo no forma parte del entorno probado. El `README.md` documenta enmascarar la ruta y evitar `Referer` y volcados de solicitudes antes de publicar enlaces.

## Próximo paso

Presentar el change a revisión de Juan Pablo y abrir el PR a `master`. Una vez integrado, ejecutar `/opsx:archive c-39-gestion-turno-por-token`.

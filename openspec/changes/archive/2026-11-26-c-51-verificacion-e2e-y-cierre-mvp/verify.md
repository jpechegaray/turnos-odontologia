# Verify — c-51-verificacion-e2e-y-cierre-mvp (2026-11-26)

**Veredicto:** LISTO PARA ARCHIVAR (una vez mergeado el PR). Queda una sola comprobación manual: el recorrido del README sobre una base vacía.

## Validación estructural

`openspec validate c-51-verificacion-e2e-y-cierre-mvp --strict` dio: válido.

## Tareas

Hay 17 de 17 tareas completas. En la 4.5 queda manual la corrida en una base vacía (ver más abajo).

## Ejecuciones

| Comando | Resultado |
|---|---|
| `./mvnw verify` (backend, Testcontainers con PostgreSQL 16) | Exit 0. Según los reportes de Surefire y Failsafe: 371 unitarios y 821 de integración, sin fallas ni errores. |
| `npx vitest run` (frontend) | 48 archivos y 448 tests en verde |
| `npx tsc -b` | Sin errores |
| `npx eslint src` | Sin errores |
| `node scripts/verificar-matriz-rn.mjs` | 38 reglas y 74 filas; todos los tests existen |

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/`.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| recorrido-turno | Un paciente reserva online y gestiona su turno por el enlace | Cubierto | recorridos/RecorridoTurnoIT.java:125 | Toma el token del email de confirmación |
| recorrido-turno | Una cancelación termina con el hueco tomado desde la lista de espera | Cubierto | recorridos/RecorridoTurnoIT.java:150 | La entrada queda `ASIGNADA` con `turno_asignado_id` |
| recorrido-turno | Una reprogramación termina con el hueco tomado desde la lista de espera | Cubierto | recorridos/RecorridoTurnoIT.java:165 | |
| recorrido-turno | El recordatorio llega una sola vez y no llega al turno cancelado | Cubierto | recorridos/RecorridoTurnoIT.java:182 | Corre el planificador dos veces |
| recorrido-turno | Los recorridos pasan en una build limpia | Cubierto | `./mvnw verify` | Corrida completa con Testcontainers y correo en memoria |
| oferta-hueco | Una reprogramación avisa por el horario que dejó libre | Cubierto | listaespera/oferta/application/AvisoHuecoReprogramacionIT.java:41 | |
| oferta-hueco | Una reprogramación que se superpone avisa solo por el tramo liberado | Cubierto | listaespera/oferta/application/AvisoHuecoReprogramacionIT.java:59 | La oferta cubre solo el tramo liberado |
| control-acceso | Recepción no administra usuarios ni modifica la configuración | Cubierto | seguridad/web/MatrizPermisosIT.java:263 | 403 sin cambios de filas |
| control-acceso | Recepción no modifica los catálogos | Cubierto | seguridad/web/MatrizPermisosIT.java:272 | Manda cuerpos válidos (ver design, hallazgos) |
| control-acceso | El odontólogo no lee usuarios, configuración ni el registro de notificaciones | Cubierto | seguridad/web/MatrizPermisosIT.java:283 | |
| control-acceso | Cada rol lee lo que la matriz le permite | Cubierto | seguridad/web/MatrizPermisosIT.java:291 | |
| control-acceso | Toda ruta del personal sin sesión responde 401 | Cubierto | seguridad/web/MatrizPermisosIT.java:297 | |
| control-acceso | Una ruta sin fila en la matriz hace fallar la verificación | Cubierto | seguridad/web/MatrizPermisosIT.java:248 | Se probó quitando una fila a propósito |
| reglas-arquitectura | Una dependencia del dominio hacia el framework rompe la build | Cubierto | arquitectura/ReglasArquitecturaTest.java:29 | |
| reglas-arquitectura | Usar el repositorio de otro módulo rompe la build | Cubierto | arquitectura/ReglasArquitecturaTest.java:17 | |
| reglas-arquitectura | El código actual cumple las reglas de dependencia | Cubierto | ArquitecturaTest.java:14 | |
| trazabilidad-reglas | Cada regla tiene al menos un test que existe | Cubierto | `scripts/verificar-matriz-rn.mjs` | Revisa `docs/matriz-rn-tests.md` |
| trazabilidad-reglas | El checklist cubre el MVP y excluye el backlog futuro | Cubierto | `docs/cierre-mvp.md` | Las búsquedas del backlog no encontraron nada |
| trazabilidad-reglas | La búsqueda de secretos no encuentra nada versionado | Cubierto | `docs/cierre-mvp.md` | Comandos y resultados registrados |
| trazabilidad-reglas | Un integrante nuevo arranca la demo siguiendo el README | Manual | `README.md`, "Recorrido de demostración" | Necesita el `.env` local; no se automatizó |

Los otros escenarios de la spec delta `oferta-hueco` ya existían y siguen en verde: `AvisoHuecoLiberadoIT`, `AvisoHuecoFalloIT` y `SelectorDeAvisosTest`.

## Checklist manual pendiente

1. Con una base vacía, `docker compose up -d` y el `.env` armado desde `.env.example`.
2. Backend con el perfil `dev` (seed) y frontend con `npm run dev`.
3. Seguir los 6 pasos del "Recorrido de demostración" del README y anotar cualquier diferencia.

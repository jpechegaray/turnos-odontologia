# Verify — c-42-tomar-hueco-desde-oferta (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (después del merge del PR)

## Validación estructural

`openspec validate c-42-tomar-hueco-desde-oferta --strict` → `Change 'c-42-tomar-hueco-desde-oferta' is valid`.

## Tareas

24/24 completas. No hay pendientes ni diferidas.

## Matriz escenario → test

Rutas relativas a `backend/src/test/java/ar/edu/utn/turnos/`. Todos los tests llevan `@DisplayName` con el nombre exacto del escenario y afirman el THEN.

| Spec | Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|---|
| toma-oferta-hueco | Se consulta una oferta disponible | Cubierto | listaespera/oferta/web/ConsultaOfertaPublicaIT.java:16 | Verifica 5 campos exactos y que no aparezcan email, nombre ni teléfono del paciente |
| toma-oferta-hueco | Una oferta tomada informa que el hueco ya fue tomado | Cubierto | listaespera/oferta/web/ConsultaOfertaPublicaIT.java:36 | |
| toma-oferta-hueco | Una oferta cuyo hueco ya empezó informa que venció | Cubierto | listaespera/oferta/web/ConsultaOfertaPublicaIT.java:45 | |
| toma-oferta-hueco | Una oferta de una entrada que ya no está activa no está disponible | Cubierto | listaespera/oferta/web/ConsultaOfertaPublicaIT.java:57 | |
| toma-oferta-hueco | Tokens inválidos responden el mismo 404 | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:178 | GET y POST con token inexistente, mal formado y de turno; cuerpos idénticos; no se crea turno |
| toma-oferta-hueco | El paciente toma un hueco disponible | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:45 | Incluye canal ONLINE, sillón, entrada ASIGNADA, oferta TOMADA y token válido en C-39 |
| toma-oferta-hueco | Si el sillón sugerido está ocupado se usa el primero activo libre | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:75 | |
| toma-oferta-hueco | Las demás ofertas del hueco pasan a tomadas | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:90 | |
| toma-oferta-hueco | Dos pacientes confirman a la vez y solo uno obtiene el turno | Cubierto | listaespera/oferta/web/TomaOfertaConcurrenciaIT.java:43 | `@RepeatedTest(5)` contra PostgreSQL real |
| toma-oferta-hueco | El hueco lo ocupó recepción antes de que el paciente confirme | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:116 | |
| toma-oferta-hueco | Volver a tomar una oferta ya tomada | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:105 | |
| toma-oferta-hueco | Tomar una oferta cuyo hueco ya empezó | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:129 | |
| toma-oferta-hueco | Se puede tomar un hueco que empieza en pocos minutos | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:143 | |
| toma-oferta-hueco | La entrada fue cancelada antes de tomar la oferta | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:152 | |
| toma-oferta-hueco | El profesional bloqueó la agenda en el horario del hueco | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:165 | |
| toma-oferta-hueco | El paciente recibe el enlace de su nuevo turno | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:201 | Abre el enlace del email en `/api/public/turnos/{token}` |
| toma-oferta-hueco | Una falla de correo no pierde el turno | Cubierto | listaespera/oferta/web/TomaOfertaIT.java:226 | |
| toma-oferta-hueco | Las respuestas no se guardan en caché | Cubierto | listaespera/oferta/web/ProteccionEnlaceOfertaIT.java:49 | Servidor real; también revisa `instance` del 404 |
| toma-oferta-hueco | Los tokens no aparecen en los logs | Cubierto | listaespera/oferta/web/ProteccionEnlaceOfertaIT.java:65 | Revisa logs de la aplicación y access log de Tomcat |
| agenda-turnos | Alta online en el sillón preferido | Cubierto | agenda/application/CrearTurnoOnlineIT.java:87 | |
| agenda-turnos | Alta online sin sillón preferido libre | Cubierto | agenda/application/CrearTurnoOnlineIT.java:107 | |
| agenda-turnos | Alta online sin ningún sillón libre | Cubierto | agenda/application/CrearTurnoOnlineIT.java:118 | |
| agenda-turnos | Alta online solapada con otro turno del profesional | Cubierto | agenda/application/CrearTurnoOnlineIT.java:133 | |
| notificaciones-email | El reintento lleva un enlace válido | Cubierto | shared/notificacion/application/ReintentoConfirmacionReservaIT.java:75 | |
| notificaciones-email | No se reintenta la confirmación de un turno cancelado | Cubierto | shared/notificacion/application/ReintentoConfirmacionReservaIT.java:89 | |

25/25 cubiertos. Tests extra sin escenario propio: "Un turno online nuevo queda PROGRAMADO, con canal ONLINE y sin usuario creador" (`TurnoTest`) y "Una toma y un alta simultánea de recepción sobre el mismo hueco no se bloquean entre sí" (`TomaOfertaConcurrenciaIT`, `@RepeatedTest(3)`), que cubre el deadlock corregido en D3.

## Ejecución de tests

| Suite | Comando | Resultado |
|---|---|---|
| Backend | `./mvnw -q verify` (Docker 27.2.0 activo) | Exit 0. Surefire: 353 tests, 0 fallas, 0 errores, 0 saltados. Failsafe: 737 tests, 0 fallas, 0 errores, 0 saltados |
| Frontend | `npx vitest run` | 41 archivos, 369 tests pasados (sin cambios de frontend en este change) |

Fallidos: ninguno.

## Reglas de negocio y reglas duras

- **RN-01 / RN-02 (sin solapamientos):** el alta online pasa por `CrearTurnoService.crearOnline` (de C-41), con los mismos locks y `ValidadorTurno` que el alta interna, y la garantía final sigue siendo `EXCLUDE USING gist`. Hay test de solapamiento real con el profesional y de carrera concurrente con PostgreSQL. El borde `[inicio, fin)` ya lo cubren los tests de C-19; este change no modifica esa lógica.
- **RN-13 (bloqueos de agenda):** "El profesional bloqueó la agenda en el horario del hueco".
- **RN-15 / RN-24 (lista de espera y oferta):** escenarios de toma, carrera, vencida y no disponible.
- **RN-07 / RN-25 / RN-38 (tokens):** el token de la oferta se compara por hash en tiempo constante; 404 uniforme; `no-store`; sin tokens en logs ni en `instance`. El turno nuevo recibe un token propio.
- **RN-30 (reintentos):** `ReintentoConfirmacionReservaIT`.
- **RN-03, RN-04, RN-09, RN-11, RN-27, RN-31:** se heredan de `ValidadorTurno` (horario de atención, prestación activa, duración) y del registro de notificaciones existentes, cubiertos por sus changes de origen.
- **Reglas duras revisadas en el diff:** sin datos reales (solo dominios `.test` y nombres "Ficticio"); sin secretos ni cambios en `application*.yml` o `.env`; sin `LocalDateTime`, Flyway, Liquibase, JWT ni H2; DTOs (`OfertaPublica`, `TurnoTomado`) en lugar de entidades; JPQL parametrizado. Los endpoints nuevos son públicos por token (`/api/public/**`), no de personal: la regla 4 (odontólogo solo lectura) no se ve afectada.

## Diferencias con design.md

Ninguna. D1–D8 coinciden con el código. D3 ya registra el orden de locks definitivo (profesional → oferta → entrada → sillón), agregado durante el apply por el deadlock que encontró la prueba de concurrencia.

## Qué NO se pudo verificar

- En la prueba manual contra el backend de desarrollo la confirmación quedó `FALLIDA`: el perfil local intentó SMTP en lugar del canal en memoria. Es configuración del entorno local, no del change; el envío y el reintento están cubiertos por tests de integración.
- No hay UI en este change: no aplica verificación visual.

## Próximo paso

Commit en `valentino`, push y PR a `master` (revisa Juan Pablo). Después del merge: `/opsx:archive c-42-tomar-hueco-desde-oferta`. 

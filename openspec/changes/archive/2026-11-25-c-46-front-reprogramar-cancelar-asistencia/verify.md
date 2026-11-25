# Verify — c-46-front-reprogramar-cancelar-asistencia (2026-11-25)

**Veredicto:** LISTO PARA REVISIÓN Y ARCHIVO TRAS EL MERGE.

## Validación estructural

`openspec validate c-46-front-reprogramar-cancelar-asistencia --strict` verificó la propuesta, el diseño, la delta de `agenda-frontend` y las tareas. Se completaron las cuatro tareas.

## Matriz escenario → test

Las referencias corresponden a `frontend/src/features/agenda/gestion/GestionTurnos.test.tsx`.

| Escenario | Estado | Test |
|---|---|---|
| Recepción dispone de acciones según la hora y el estado | Cubierto | L105. Para un turno futuro ofrece el cambio y la cancelación. En el instante exacto de inicio ofrece la asistencia. Las marcas finales y el cancelado quedan fuera de las acciones de recepción. |
| Administración puede corregir una asistencia | Cubierto | L124. Envía AUSENTE desde un turno ATENDIDO y recarga la agenda. |
| El odontólogo no puede gestionar turnos | Cubierto | L136. Sin acción de gestión ni POST en la vista diaria o semanal. |
| La agenda semanal reprograma y muestra la marca tardía | Cubierto | L147. Consulta el plazo real, avisa antes del POST con CSRF y envía la hora de Buenos Aires en UTC, con profesional, sillón y duración elegidos. La respuesta actualiza la semana y determina la marca visible. |
| Recepción cancela con motivo y la marca del servidor | Cubierto | L170. Envía el motivo y muestra la marca de la API aunque el plazo leído no anticipara demora. |
| En el límite exacto no se anticipa una marca tardía | Cubierto | L188. Si queda una hora y el plazo es de una hora, no se marca como tardía en el aviso previo. |
| Si falla la consulta del plazo no se presume una marca tardía | Cubierto | L196. Un 503 de configuración deja el plazo indeterminado. Tras reintentar, aparece el aviso al conocerse un plazo de una hora. |
| Un conflicto de horario conserva la reprogramación | Cubierto | L208. Un 409 por sillón ocupado muestra el recurso afectado y conserva fecha y hora. |
| Un cambio concurrente permite actualizar la agenda | Cubierto | L219. Un 409 `TURNO_MODIFICADO` impide repetir el POST con el formulario anterior y permite actualizar. |
| Una escritura denegada informa la falta de permiso | Cubierto | L233. Un 403 informa la denegación y no muestra una confirmación de guardado. |
| Una regla de negocio informa el ajuste necesario | Cubierto | L242. Los mensajes de RN-04, RN-05, RN-09 y RN-20 indican el ajuste aplicable o el paso a asistencia. |

## Ejecución de tests

- **Rojo:** `npm test -- GestionTurnos.test.tsx` falló inicialmente por faltar el módulo de acciones. Con el módulo pero sin panel, nueve pruebas fallaron y dos pasaron.
- **Verde dirigido:** `npm test -- GestionTurnos.test.tsx` pasó las once pruebas.
- **Frontend 10797a3 + C-46:** `npm run lint`, `npm run build` y `npm test` en secuencia. TypeScript y Vite aprobaron con 164 módulos. Vitest aprobó 339 tests en 39 archivos.
- **Frontend 05c0198 + C-46:** se actualizó la rama con el archivo de C-45 y la interfaz de C-34. Lint, build (172 módulos) y Vitest (369 tests en 41 archivos) volvieron a pasar. El backend quedó idéntico entre ambas bases.
- **Backend 10797a3:** `mvnw.cmd -B -ntp verify` aprobó 342 unitarios y 671 de integración, 1.013 tests en total, sin fallos, errores ni omitidos. C-46 solo modifica frontend y OpenSpec.
- **Git:** `git diff --check` y comprobación de espacios de los archivos nuevos sin observaciones.

## Reglas de negocio y reglas duras

- RN-05 y RN-18. La advertencia evalúa el inicio original y el plazo vigente con límite inclusivo. El backend mantiene la regla efectiva. La reprogramación manual permite probar un desplazamiento de 15 minutos dentro del intervalo propio. La API valida recursos, duración, horarios y otros turnos.
- RN-19 y RN-20. El motivo de cancelación es opcional, con el límite de 255 caracteres del DTO. Los turnos activos pasan a permitir asistencia desde su inicio. Solo ADMIN puede corregir una marca de asistencia, ofreciendo el estado contrario.
- RN-33. El odontólogo conserva lectura. Todos los POST pasan por el cliente de sesión y CSRF. Los fixtures contienen solo datos ficticios.
- La agenda de C-28 no devuelve las marcas históricas de tardanza. El aviso posterior a cada operación se basa en las marcas de la respuesta de escritura y no se guarda en el navegador.

## Diferencias con design.md

Ninguna. Se mantienen día y semana. El panel evita controles recortados en turnos cortos y utiliza el kit UI con los tokens aprobados.

## Qué NO se pudo verificar

No se ejecutó una sesión manual con la API de desarrollo. Las pruebas de interfaz montan la agenda y el cliente real con HTTP simulado. La suite del backend valida los endpoints por separado. El modo system-only no requiere mockups para este change.

## Próximo paso

Revisión y autorización del commit antes de abrir el PR.

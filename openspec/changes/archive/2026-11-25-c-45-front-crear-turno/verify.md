# Verify — c-45-front-crear-turno (2026-11-25)

**Veredicto:** LISTO PARA REVISIÓN Y ARCHIVO TRAS EL MERGE.

## Validación estructural

`openspec validate c-45-front-crear-turno --strict` aprobó la propuesta, el diseño, la delta de `agenda-frontend` y las tareas. Las cuatro tareas están completas.

## Matriz escenario → test

Las referencias corresponden a `frontend/src/features/agenda/crear/CrearTurno.test.tsx`.

| Escenario | Estado | Test |
|---|---|---|
| Recepción crea el turno y actualiza la agenda | Cubierto | L115. Busca una ficha activa con C-07, selecciona un hueco, cambia su sillón, confirma el POST con CSRF y comprueba el refresco de agenda. |
| Administración puede abrir el formulario de alta | Cubierto | L136. ADMIN abre el panel con un formulario sin completar. |
| El odontólogo no ve el alta de turno | Cubierto | L142. Conserva su vista sin botón de alta ni consultas de disponibilidad o de escritura. |
| La duración ajustada vuelve a consultar el día de Buenos Aires | Cubierto | L150. Propone 30 minutos, invalida el horario cuando cambia la duración y consulta con medianoches de Buenos Aires y 45 minutos. |
| Un cambio de fecha descarta la disponibilidad anterior | Cubierto | L168. Resuelve la petición de la fecha anterior después del cambio y comprueba que no aparece. |
| Un conflicto del profesional actualiza las alternativas | Cubierto | L183. Explica el recurso en conflicto, conserva el paciente, consulta con los mismos criterios y presenta otro horario sin enviar otra alta. |
| Un conflicto del sillón permite elegir otro sillón | Cubierto | L200. Explica el conflicto, ofrece el mismo inicio en un sillón distinto y lo propone al elegirlo. |
| Un bloqueo del profesional indica cambiar el horario | Cubierto | L216. El 422 de RN-04 con BLOQUEO muestra una acción clara y conserva el paciente. |
| Los códigos de regla indican cómo corregir el turno | Cubierto | L226. Verifica el mapeo de horario, duración, pasado, asignación y recursos inactivos o inexistentes. |

## Ejecución de tests

- **Rojo:** `npm test -- CrearTurno.test.tsx` falló inicialmente porque no existía el mapeo de errores. Con el mapeo agregado y el panel aún ausente, siete pruebas de interfaz fallaron y dos pasaron.
- **Verde dirigido:** `npm test -- CrearTurno.test.tsx` aprobó las nueve pruebas.
- **Frontend completo:** lint aprobado. `npm run build` aprobó TypeScript y Vite, con 158 módulos. `npm test` aprobó los 328 tests de 38 archivos, sin omitidos. Una corrida concurrente con lint y build expiró en un caso de interacción y lint leyó un archivo temporal de Vite mientras se borraba. En la verificación final secuencial pasaron los tres comandos.
- **Backend base c3e5d8d:** `mvnw.cmd -B -ntp verify` aprobó 325 tests unitarios y 634 de integración, 959 en total, sin fallos, errores ni omitidos. C-45 no cambia Java ni sus contratos.
- **Git:** archivos revisados con `git diff --check` y comprobación de espacios sobre los nuevos archivos.

## Reglas de negocio y reglas duras

- RN-01, RN-02 y RN-15. El 409 identifica el recurso cuando la API lo informa. El formulario obtiene disponibilidad actualizada. Las restricciones PostgreSQL del backend siguen siendo la garantía ante solapamientos.
- RN-03, RN-09 y RN-14. Duración por defecto ajustable de 5 a 480 minutos y múltiplo de 5. Las fechas y horas se forman con la utilidad de Buenos Aires y la API recibe instantes con zona.
- RN-04, RN-11 y RN-13. El formulario explica el bloqueo, el fuera de horario, el pasado y los recursos inválidos con instrucciones de corrección. El backend conserva su validación.
- RN-33 y RN-35. Solo ADMIN y RECEPCION acceden al alta. Se usa el cliente API con sesión y CSRF. Los datos de pacientes de prueba son ficticios y el formulario no usa almacenamiento persistente.
- El panel usa el kit UI, selectores accesibles con etiquetas y tokens aprobados. El selector preexistente ahora vive en `shared/ui` y también se usa en Usuarios.

## Diferencias con design.md

Ninguna. Se mantiene el control de acceso efectivo en la API, el formulario integrado y la vista de agenda vigente. La consulta conserva los filtros que permiten ver el turno creado.

## Qué NO se pudo verificar

No se ejecutó una sesión manual en navegador con la API de desarrollo. Las pruebas de interacción montan la ruta real y el cliente HTTP con respuestas simuladas. La suite del backend valida los servicios por separado. El modo system-only no requiere mockups por change.

## Próximo paso

Revisar los cambios y confirmar el commit antes de abrir el PR.

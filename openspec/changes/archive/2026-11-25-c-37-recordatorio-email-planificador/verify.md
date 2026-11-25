# Verify — c-37-recordatorio-email-planificador (2026-11-25)

**Veredicto**: listo para revisión y para archivar después del merge.

## Validación estructural y tareas

OpenSpec pasó la validación estricta. Se completaron las 5 tareas.

## Matriz escenario a test

Las líneas corresponden a PlanificadorRecordatoriosIT.java.

| Escenario | Estado | Test |
|---|---|---|
| Dos ciclos no duplican el recordatorio | Cubierto | L66 |
| Dos ciclos concurrentes encolan y envían un solo recordatorio | Cubierto | L76 |
| La ventana configurable incluye exactamente su límite superior | Cubierto | L90 |
| Un turno creado con poca anticipación entra en el siguiente ciclo | Cubierto | L108 |
| Un paciente sin email se omite | Cubierto | L118 |
| Un turno reprogramado recibe recordatorio para su nuevo inicio | Cubierto | L128 |
| Un turno cancelado o iniciado no recibe un pendiente | Cubierto | L144 |
| El recordatorio de un turno interno incluye un enlace válido | Cubierto | L161 |
| Un fallo de correo se reintenta hasta el tope | Cubierto | L180 |

## Ejecución de tests

- Rojo inicial: Maven falló al compilar PlanificadorRecordatoriosIT porque todavía no existía el planificador.
- Verde inicial: la prueba de dos ciclos pasó junto a nueve pruebas de ServicioNotificacionesTest.
- Backend: mvnw.cmd -B -ntp verify aprobó 302 tests unitarios y 610 de integración con PostgreSQL en Testcontainers. Pasaron los nueve escenarios de C-37.
- Frontend: npm ci, npm run lint, npm test y npm run build aprobaron. Vitest ejecutó 290 tests en 34 archivos.
- Se actualizó la base a 784ff32. El avance de master solo incorporaba documentación y OpenSpec de C-35.

## Reglas de negocio y diseño

RN-29: se seleccionan turnos pendientes o confirmados con email, inicio futuro y límite superior inclusivo. RN-30: PostgreSQL impide duplicados por turno, tipo e inicio, y C-15 reclama y reintenta. Se descartan horarios obsoletos sin llamar al adaptador de correo. RN-31: el paciente sin email se omite y el mensaje usa la plantilla de C-15.

C-32 provee tokens nuevos por intento. Los enlaces ya emitidos conservan su vigencia. Se comprueba el alcance del token con el servicio de validación. Solamente se guarda el hash. La cola no almacena cuerpo ni enlace. El fallo de transporte de un recordatorio se guarda con un mensaje fijo y las representaciones de texto del correo omiten su contenido.

La tarea programada de C-15 ejecuta la planificación antes de reclamar pendientes en el mismo ciclo. El descarte conserva la fila como terminal sin consumir un intento.

## Qué no se verificó

Las pruebas usaron el adaptador de correo en memoria. No se enviaron emails reales. Abrir el enlace desde el portal depende del endpoint C-39 y la pantalla C-49, fuera de C-37.

## Próximo paso

Confirmar el commit, abrir el PR y verificar los checks de GitHub.

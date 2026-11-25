# Verify — c-40-front-agenda-vistas (2026-11-25)

**Veredicto:** LISTO PARA ARCHIVAR (0 bloqueantes; 15 de 15 escenarios cubiertos; sin desvíos del design)

## Validación estructural

`openspec validate c-40-front-agenda-vistas --strict` → `Change 'c-40-front-agenda-vistas' is valid`.

## Tareas

9/9 completas. La 3.1 se completa con esta verificación.

## Matriz escenario → test

Spec `agenda-frontend` (delta): 8 requisitos y 15 escenarios. Las rutas son relativas a `frontend/src/features/agenda/`. Cada `it(...)` lleva el nombre exacto del escenario (regla 11).

| Escenario | Estado | Test (archivo:línea) | Nota |
|---|---|---|---|
| Al entrar se muestra la agenda de hoy | Cubierto | Agenda.test.tsx:135 | El primer pedido va sin `fecha` |
| Un fallo al cargar la agenda permite reintentar | Cubierto | Agenda.test.tsx:149 | |
| La vista diaria ubica cada turno en la columna de su profesional | Cubierto | Agenda.test.tsx:162 | |
| La vista diaria por sillón agrupa los turnos por sillón | Cubierto | Agenda.test.tsx:189 | |
| Se muestran los horarios de atención y los bloqueos del profesional | Cubierto | Agenda.test.tsx:215 | También "No atiende" sin horario |
| La vista semanal muestra cada turno en su día | Cubierto | Agenda.test.tsx:234 | Siete columnas, de lunes a domingo |
| Un bloqueo de varios días aparece en cada día que toca | Cubierto | Agenda.test.tsx:264 | El recorte por día, en grilla.test.ts:36 |
| Ir al día siguiente pide la fecha siguiente | Cubierto | Agenda.test.tsx:286 | También "Hoy" vuelve a pedir sin `fecha` |
| En la vista semanal se avanza una semana | Cubierto | Agenda.test.tsx:303 | |
| Filtrar por profesional y por sillón envía los dos filtros | Cubierto | Agenda.test.tsx:323 | |
| Cada turno muestra el badge de su estado | Cubierto | Agenda.test.tsx:340 | Incluye la leyenda; el tachado en estilosAgenda.test.ts:27 |
| Un instante en UTC se muestra en hora de Buenos Aires | Cubierto | Agenda.test.tsx:377 | Hora y fila de la grilla; funciones en shared/fecha/zonaHoraria.test.ts:87 y :95 |
| Se abre la ficha básica del paciente de un turno | Cubierto | Agenda.test.tsx:392 | Sin campos editables; el error, en la línea 414 |
| Un turno de otro profesional no ofrece la ficha | Cubierto | Agenda.test.tsx:426 | |
| El odontólogo ve su agenda sin acciones de escritura | Cubierto | Agenda.test.tsx:440 | Recorre día, semana y ficha; ningún `POST`, `PUT`, `PATCH` ni `DELETE` |

Además: `grilla.test.ts` (9 tests: fechas civiles, turno que cruza la medianoche, rango de 08:00 a 20:00 y filas) y `estilosAgenda.test.ts` (la hoja solo usa tokens de `tokens.css`, regla 13).

## Pruebas de mutación

- Si siempre se ofrece la ficha (aunque la API oculte al paciente), falla "Un turno de otro profesional no ofrece la ficha".
- Si los bordes del día se calculan en UTC en lugar de Buenos Aires, fallan 4 tests (grilla y hora de Buenos Aires).

## Ejecución

- `npx vitest run`: 37 archivos, 319 tests, todos verdes (25 nuevos de la agenda y 2 de `zonaHoraria`).
- `npm run lint`: sin errores ni advertencias.
- `npm run build`: compila (`tsc -b` y Vite).

## Verificación visual

Design system revisión 1, política `system-only` (sin mockup por change). Se revisó la pantalla en el navegador integrado con una página temporal de vista previa (datos ficticios y respuestas simuladas de la API; no se versionó). No se levantó el backend: entrar necesita la clave de demostración de `.env`, que el agente no lee.

- **1440x900:** la grilla diaria por profesional coincide con la sección "Agenda diaria por sillón" de `components.html` (columnas con encabezado, filas de 15 minutos, colores de estado, bloqueo rayado y leyenda). La semana muestra siete columnas con horarios, bloqueos de varios días y turnos.
- **390x844:** la página no se desborda; la grilla se desplaza en horizontal dentro de su contenedor, como en el design system. La ficha se lee completa.
- **Defectos corregidos durante la revisión:**
  - En los turnos de 30 minutos se ocultaba el nombre del paciente, porque el elemento se encogía. Ahora el badge y el sillón comparten línea.
  - En la semana, los turnos del día más cargado se salían de su columna.
  - En móvil, la grilla ensanchaba toda la página.
- **Menor, sin corregir:** en móvil, el borde inferior del badge de un turno de 30 minutos queda apenas recortado.
- **Consola:** un `500` al abrir `/` sin backend (el proxy de `/api/auth/me`), ajeno a la agenda; ningún error de la pantalla de agenda.

## Riesgos que siguen

- La API de agenda no informa la cancelación tardía: se muestra "Cancelado" (design, riesgo 1).
- C-45 y C-46 continúan `features/agenda` (RM-09).

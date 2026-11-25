# Design

## Context

C-28 expone `GET /api/agenda?vista=dia|semana&fecha=AAAA-MM-DD&profesionalId&sillonId`. La respuesta trae `vista`, `desde`, `hasta`, `turnos`, `horarios` y `bloqueos`, con instantes en hora de Buenos Aires con desfase. Para el odontólogo, la API oculta `paciente`, `notas` y `motivo` de los turnos y bloqueos de otros profesionales, y responde `403` a toda escritura. `GET /api/profesionales`, `GET /api/sillones` y `GET /api/prestaciones` están abiertos a todo el personal autenticado. `GET /api/pacientes/{id}` deja al odontólogo leer solo la ficha de un paciente con turnos de su profesional. C-09 aporta el kit UI (`BadgeEstadoTurno`, `Alerta`, `Boton`, `Tarjeta`), `shared/fecha` y las rutas por feature. `features/agenda/` está vacía (`.gitkeep`).

## Goals / Non-Goals

**Goals:** ver el día y la semana por profesional y por sillón, con estados, horarios, bloqueos y hora de Buenos Aires. Abrir la ficha básica. Dejar una estructura que C-45 y C-46 puedan extender con acciones.

**Non-Goals:** acciones de escritura (alta desde huecos libres, cancelar, reprogramar, asistencia: C-45 y C-46). Cambios en el backend. Vista mensual.

## Decisions

1. **"Hoy" lo decide el servidor.** La primera consulta va sin `fecha` y la pantalla toma `desde` de la respuesta como fecha vigente. Así el front no depende del reloj ni de la zona del navegador, y los tests no necesitan simular la hora. "Hoy" repite la consulta sin `fecha`. "Anterior" y "Siguiente" suman ±1 o ±7 días a la fecha vigente con aritmética de fechas civiles (`Date.UTC`), sin pasar por instantes.
2. **Cálculos de grilla en Buenos Aires.** Se agregan a `shared/fecha/zonaHoraria.ts` `fechaLocal(instante)` (`AAAA-MM-DD`) y `minutosDelDia(instante)`. Cada elemento se recorta al día mostrado (`[00:00, 24:00)` local) y se ubica en filas de 15 minutos con `grid-row`. Las funciones de recorte son puras (`grilla.ts`) y se prueban aparte. Un turno que cruza la medianoche se muestra hasta las 24:00 del día.
3. **Columnas.** Vista diaria por profesional: una columna por cada profesional que figura en el catálogo (o solo el filtrado), con sus horarios como franja de fondo "Atiende" y sus bloqueos como `slot--bloqueo`. Por sillón: una columna por sillón y solo turnos, porque horarios y bloqueos son del profesional (SU-08). Los horarios y bloqueos del día se ven igual en la vista por profesional. Vista semanal: siete columnas (lunes a domingo, desde `desde`) con la lista ordenada de horarios, bloqueos y turnos del día. Así se evita una grilla de 7 × N columnas, que no entraría en pantalla.
4. **Catálogos para nombres y filtros.** Un hook carga profesionales, sillones y prestaciones en paralelo una sola vez. Si alguno falla, la agenda igual se muestra con nombres de respaldo ("Profesional 3"), porque el criterio principal es ver los turnos.
5. **Ficha en un panel de solo lectura.** El nombre del paciente es un botón que abre un panel (`Tarjeta`) con la ficha de `GET /api/pacientes/{id}`, presentada como lista de definiciones, con "Cerrar". Si `paciente` es nulo, se muestra "Paciente de otro profesional" sin botón.
6. **Sin acciones de escritura para nadie.** Ningún rol tiene botones de escritura en C-40, así que no hace falta ramificar por rol. El test del odontólogo verifica que no se envía nada distinto de `GET`. C-45 agregará las acciones detrás de un chequeo de rol.
7. **Estilos con tokens.** `features/agenda/agenda.css` toma de `design-system/components.html` las clases `agenda`, `slot` y `leyenda`, con variables de `tokens.css`. La cantidad de columnas y filas se pasa como variables CSS en línea (`--agenda-columnas`, `--agenda-filas`). La posición de cada elemento va en `grid-row` y `grid-column`. Un test verifica que la hoja no usa colores fijos ni variables sin definir (regla 13).

## Risks / Trade-offs

- La API no informa la cancelación tardía en la agenda → se muestra "Cancelado". `CANCELADO_TARDE` queda disponible para cuando C-28 lo exponga.
- Muchos profesionales hacen ancha la grilla → el contenedor se desplaza en horizontal, como en el design system.
- C-45 y C-46 continúan esta feature (RM-09) → se separan componentes chicos (`GrillaDia`, `VistaSemana`, `TarjetaTurno`, `FichaPaciente`) para sumar acciones sin reescribir.

## UI Mockups

- Design system revision: 1
- Decision: no (project policy) — 2026-11-25. La política del proyecto es `system-only`: se usa el sistema aprobado (sección "Agenda diaria por sillón" de `design-system/components.html`) sin mockup por change.
- Mockups: no aplica
- Approval: no aplica
- Preview: no aplica
- Visual verify: pendiente (en verify se revisa la pantalla en el navegador si hay uno disponible)

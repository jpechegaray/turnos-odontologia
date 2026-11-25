# Tasks

Aplicar TDD estricto por tarea: primero el test de Vitest en rojo, después el código. Usar datos ficticios y el nombre exacto del escenario en `it(...)` cuando el test cubra la spec.

## 1. Fechas y grilla

- [ ] 1.1 Agregar `fechaLocal` y `minutosDelDia` a `shared/fecha/zonaHoraria.ts`; verificar con instantes en UTC y con desfase, y el borde de las 00:00 y las 23:30 en Buenos Aires.
- [ ] 1.2 Implementar en `features/agenda/grilla.ts` la aritmética de fechas civiles (sumar días, días de la semana) y el recorte y la ubicación de intervalos en filas de 15 minutos, con el rango de 08:00 a 20:00 sin datos; verificar el turno que cruza la medianoche y el bloqueo de varios días.

## 2. Pantalla y vistas

- [ ] 2.1 Crear `routes.ts`, los tipos de la respuesta, `useAgenda` y `useCatalogosAgenda`, y la pantalla con carga, error y reintento; verificar "Al entrar se muestra la agenda de hoy" y "Un fallo al cargar la agenda permite reintentar".
- [ ] 2.2 Implementar la vista diaria por profesional y por sillón con horarios, bloqueos, estados y leyenda; verificar los escenarios de vista diaria, estados y hora de Buenos Aires.
- [ ] 2.3 Implementar la vista semanal; verificar sus dos escenarios.
- [ ] 2.4 Implementar la navegación y los filtros; verificar sus tres escenarios.
- [ ] 2.5 Implementar la ficha básica del paciente; verificar sus dos escenarios y "El odontólogo ve su agenda sin acciones de escritura".
- [ ] 2.6 Agregar `agenda.css` con tokens y el test que rechaza colores fijos y variables no definidas.

## 3. Verificación

- [ ] 3.1 Ejecutar `npm run lint`, `npm test` y `npm run build` en `frontend`, y `openspec validate c-40-front-agenda-vistas --strict` en la raíz. Revisar la pantalla en el navegador a 390x844 y 1440x900 si hay uno disponible.

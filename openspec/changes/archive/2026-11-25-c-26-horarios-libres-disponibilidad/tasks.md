# Tasks

TDD estricto: RED ejecutado antes de producción, GREEN y triangulación antes de refactor. Datos ficticios, @DisplayName exactos y PostgreSQL real. Ningún commit sin confirmación.

## 1. Cálculo puro

- [x] 1.1 Crear CalculadorDisponibilidad y HorarioDisponible con tests JUnit de hueco exacto, duración mayor, tramos, grilla, pasado y duración inválida; verificar rojo y verde.
- [x] 1.2 Cubrir ocupación por ambos recursos, bordes de bloqueo y turno, cancelados y sillones vacíos; verificar tests unitarios en verde.

## 2. Consulta y API

- [x] 2.1 Agregar puerto de horarios y bloqueos, consulta JPQL de ocupación y DisponibilidadService; verificar con Testcontainers recursos, duración, rango y día local.
- [x] 2.2 Agregar fachada y GET /api/agenda/disponibilidad con DTO validado; verificar permisos por API y servicio y creación de un turno sugerido con 201.

## 3. Verificación

- [x] 3.1 Ejecutar openspec validate --strict, backend mvnw verify y frontend lint/tests/build; escribir verify.md con matriz escenario/test y evidencia TDD.

## Workflow follow-up

- Con autorización, commit de C-26 y PR a master con Closes #26; CI verde antes de merge.
- Tras merge, archivar, actualizar CHANGES.md, cerrar issue y tablero Hecho.

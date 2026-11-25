# Tasks

Aplicar TDD estricto por tarea. Usar solo datos ficticios (`@correo.test`, nombres inventados) y, cuando el test cubra la spec, poner el nombre exacto del escenario en `@DisplayName`.

## 1. Dominio puro: período y proyección de horarios

- [x] 1.1 Crear `VistaAgenda` (`DIA` y `SEMANA`, con conversión sin distinguir mayúsculas) y `PeriodoAgenda.de(vista, fecha)` en `agenda.domain` (design D2). Se verifica con `PeriodoAgendaTest`, sin Spring:
  - el día del 2026-12-07 va de `2026-12-07T03:00Z` a `2026-12-08T03:00Z`;
  - la semana de un miércoles, de un lunes y de un domingo va de lunes a domingo;
  - un valor de vista desconocido se rechaza.
  `ArquitecturaTest` sigue en verde.
- [x] 1.2 Crear `ProyeccionHorarios.proyectar(tramos, periodo)` en `agenda.domain` (D6). Se verifica con `ProyeccionHorariosTest`:
  - lunes y miércoles en una semana dan dos fechas, con inicio y fin con desfase `-03:00`;
  - un día sin tramo no genera horarios;
  - el resultado sale ordenado por fecha, profesional e inicio.

## 2. Puertos de lectura en profesionales y pacientes

- [x] 2.1 Sumar `HorarioConsulta.tramos(profesionalId)` y `BloqueoConsulta.queSolapan(profesionalId, intervalo)`, con `null` como «todos», implementados en `HorariosPersistencia` y `BloqueosPersistencia` (D6). Se verifica en los IT de persistencia de horarios y bloqueos con PostgreSQL real:
  - el filtro por profesional y sin filtro;
  - los bloqueos que tocan el borde del intervalo entran o no según `[inicio, fin)`.
- [x] 2.2 Sumar `PacienteConsulta.porIds(ids)` → `Map<Long, Paciente>`, con `PacienteRepositorio.porIds` y su adaptador JPA (D5). Se verifica en un IT de pacientes: devuelve solo los ids pedidos, ignora los inexistentes y un conjunto vacío no consulta la base.

## 3. Agenda: consulta, visibilidad y endpoint

- [x] 3.1 Crear `AgendaLecturaJpaRepository` y `AgendaLecturaPersistencia` en `agenda.infrastructure`, con turnos del período, filtros opcionales, todos los estados y `existeTurno(pacienteId, profesionalId)` (D1, D5 y D8), sin tocar `TurnoJpaRepository`. Se verifica en `AgendaLecturaPersistenciaIT`:
  - los bordes 23:30 y 00:00 en hora local;
  - un turno contiguo al período queda afuera;
  - los filtros por profesional, por sillón y combinados;
  - `existeTurno` da verdadero o falso según el caso, incluso con un turno `CANCELADO`.
- [x] 3.2 Crear `ConsultaAgenda` (D7) y `AgendaController` con sus DTO en `OffsetDateTime` de Buenos Aires (D3 y D4). Se verifica en `AgendaApiIT` con estos escenarios:
  - «La vista diaria devuelve turnos, horarios y bloqueos del día»;
  - «La vista semanal va de lunes a domingo»;
  - «Sin parámetros se consulta el día de hoy»;
  - «Parámetros inválidos responden 400»;
  - «Un turno a las 23:30 hora local pertenece a ese día»;
  - «Un turno a las 00:00 hora local pertenece al día que empieza»;
  - «Un bloqueo de varios días aparece en cada día que toca»;
  - «Los turnos de cualquier estado se muestran con su estado»;
  - «Un horario semanal se proyecta en cada fecha del período».
- [x] 3.3 Cubrir filtros y visibilidad por rol en `AgendaApiIT`. Se verifica con estos escenarios:
  - «Filtrar por profesional muestra solo su agenda»;
  - «Filtrar por sillón muestra solo los turnos de ese sillón»;
  - «Los filtros de profesional y sillón se combinan»;
  - «Un profesional sin datos devuelve listas vacías»;
  - «Recepción ve el paciente de todos los turnos» (`ADMIN` y `RECEPCION`);
  - «El odontólogo ve el paciente solo en sus turnos», con un usuario ficticio vinculado e `IdentidadPersonal`;
  - «Un odontólogo sin profesional vinculado recibe 403»;
  - «Sin sesión la agenda responde 401».

## 4. Ficha básica para el odontólogo

- [x] 4.1 Crear el puerto `TurnosConPacienteConsulta` en `pacientes.application`, implementado por `AgendaLecturaPersistencia`. Abrir `GET /api/pacientes/{id}` a `ODONTOLOGO`, con la autorización en `PacienteServicio.obtener` antes de buscar el paciente (D8). Se verifica en `PacienteFichaOdontologoIT` con estos escenarios:
  - «El odontólogo consulta la ficha de un paciente de sus turnos» (200);
  - «El odontólogo no consulta la ficha de un paciente de otro profesional» (403 sin datos);
  - un id inexistente da 403.
  Además, el escenario existente «El odontólogo no accede a las fichas» de `PacienteGestionIT` sigue en verde, y el resto de `PacienteGestionIT` también.

## 5. Escrituras del odontólogo

- [x] 5.1 Agregar en `SecurityConfig` la regla general de escrituras sobre `/api/**` para `ADMIN` y `RECEPCION`, después de las reglas específicas (D9), y crear `EscriturasOdontologoIT`. Se verifica con el escenario «Toda escritura de un odontólogo responde 403»:
  - recorre los mappings de escritura registrados, que no pueden ser cero y deben incluir `POST /api/turnos`;
  - cada uno responde 403 con CSRF válido;
  - la cantidad de filas de las tablas de negocio no cambia.
  Los IT existentes de ADMIN y RECEPCION siguen en verde.

## 6. Verificación integral

- [x] 6.1 Correr `.\mvnw.cmd verify` en `backend/` con Docker encendido. Se verifica que terminen en verde todos los tests unitarios y de integración, incluidos los de C-11, C-12, C-07 y C-19, y que `openspec validate c-28-consultar-agenda-dia-semana --strict` pase.

## Workflow follow-up

- Correr `openspec-verify` y dejar `verify.md` con el veredicto.
- Commit en la rama `valentino-c-28-consultar-agenda-dia-semana` (el agente pregunta antes). Push y PR a `master`, que revisa Juan Pablo; el push y el PR los hace Valentino.
- Una vez mergeado, archivar el change sincronizando `consulta-agenda` y `gestion-pacientes`, y marcar C-28 como hecho en `CHANGES.md`.

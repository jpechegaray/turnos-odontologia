# Design

## Context

La motivación está en `proposal.md`, sección Why.

- **Turnos.** C-19 dejó en `agenda` el record `Turno`, la entidad `TurnoJpa` (package-private, tabla `turno`) y `TurnoJpaRepository`, que solo tiene la consulta de solapamiento. Los índices `(profesional_id, inicio)`, `(sillon_id, inicio)`, `(paciente_id, inicio)` y `(estado, inicio)` ya existen. La ficha de C-28 pide no modificar `TurnoJpaRepository`.
- **Horarios y bloqueos.** `profesionales` expone `HorarioConsulta.contiene` y `BloqueoConsulta.solapa`, que devuelven solo booleanos, junto con los records `HorarioDatos` (día ISO y horas locales) y `BloqueoDatos` (instantes). En sus endpoints, el odontólogo solo ve lo de su profesional: `ServicioProfesionales.detalle` le responde 403 con cualquier otro.
- **Vínculo con el profesional.** `ProfesionalVinculadoConsulta.buscarIdPorIdentidad(nombre)` resuelve el profesional del odontólogo autenticado; el nombre es el id del usuario. `ServicioProfesionales` muestra cómo distinguir la lectura global (`ROLE_ADMIN` o `ROLE_RECEPCION`) de la del odontólogo.
- **Pacientes.** `pacientes` expone `PacienteConsulta.porId` y `porEmail`. `PacienteController` tiene `@PreAuthorize` con `RECEPCION` y `ADMIN` en cada método.
- **Dependencias entre módulos.** KB 08: `agenda` lee de `profesionales` y `pacientes` por interfaces de `application`, nunca al revés. Ya hay un antecedente de dependencia invertida: `profesionales` define `ProfesionalVinculadoConsulta` y la implementa `seguridad`.
- **Seguridad de las escrituras.** Varios endpoints de escritura solo tienen `@PreAuthorize`. Spring resuelve y valida el cuerpo (`@Valid`) antes de invocar el método protegido, así que hoy un odontólogo con un cuerpo inválido recibe `400` en vez de `403`. Por eso `SecurityConfig` ya tiene reglas «403 antes de validar el cuerpo» para algunas rutas.
- **Tiempo.** Hay un `Clock` inyectable con zona `ZonaHoraria.BUENOS_AIRES`, y `Intervalo` es semiabierto `[inicio, fin)`.

## Goals / Non-Goals

**Goals:**
- Modelo de lectura en una sola llamada para la agenda del front (C-40), sin N+1 de pacientes.
- Que la decisión de qué datos ve cada rol esté en la capa de aplicación, en un solo lugar y probada.
- Que el 403 del odontólogo a toda escritura no dependa de recordar agregar reglas por endpoint.

**Non-Goals:**
- Paginación de la agenda: el período es de un día o una semana como máximo.
- Calcular huecos libres (C-26).
- Agregar índices nuevos o cambiar el esquema.

## Decisions

### D1. Ubicación y capas
- **Domain (puro):**
  - `agenda.domain.VistaAgenda`: `DIA` y `SEMANA`.
  - `agenda.domain.PeriodoAgenda`: record con `desde`, `hasta` e `intervalo()`.
  - `agenda.domain.ProyeccionHorarios`: tramos semanales → horarios por fecha.
- **Application:** `agenda.application.ConsultaAgenda`, con `@Transactional(readOnly = true)` y `@PreAuthorize` para los tres roles. Arma el `AgendaLeida`, que es el modelo de lectura con los datos ya filtrados por rol.
- **Infrastructure:**
  - `agenda.infrastructure.AgendaLecturaJpaRepository`: repositorio Spring Data nuevo sobre `TurnoJpa`. Comparte el paquete, así que ve la entidad sin modificar `TurnoJpaRepository`.
  - `AgendaLecturaPersistencia`: lo envuelve y devuelve records de dominio.
- **Web:** `agenda.web.AgendaController` en `GET /api/agenda`, con los DTO `RespuestaAgenda`, `RespuestaTurnoAgenda`, `RespuestaHorarioAgenda` y `RespuestaBloqueoAgenda`.
- **Alternativa descartada:** sumar la consulta a `TurnoJpaRepository`. Lo prohíbe la ficha, para no chocar con C-26, C-27 y C-29, que trabajan sobre ese archivo en la misma ola.

### D2. Período en hora de Buenos Aires
- `PeriodoAgenda.de(vista, fecha)` calcula:
  - **Día:** `desde = hasta = fecha`.
  - **Semana:** `desde` es el lunes de la semana de `fecha` (`TemporalAdjusters.previousOrSame(MONDAY)`) y `hasta` es `desde + 6`.
- El intervalo es `[desde.atStartOfDay(BA), hasta.plusDays(1).atStartOfDay(BA))`, en `Instant`. Se usa `ZoneId` y nunca un desfase fijo (RN-14).
- `fecha` por defecto es `LocalDate.now(clock)`.
- Un turno o bloqueo entra si `inicio < fin_periodo` y `fin > inicio_periodo`, la misma comparación estricta de `Intervalo.solapaCon`.
- Así un turno de 23:30 a 23:55 queda en su día, y uno que empieza a las 00:00 queda solo en el día que empieza.

### D3. Formato de los instantes
- Los DTO usan `OffsetDateTime` en la zona de Buenos Aires (`instante.atZone(BA).toOffsetDateTime()`), que se serializa como `2026-12-07T23:30:00-03:00`.
- Sigue siendo un instante inequívoco y cumple «en hora de Buenos Aires» del criterio de salida.
- El resto de la API sigue en UTC; la agenda es un modelo de lectura pensado para mostrarse.
- Un test de integración fija el formato, para detectar si la configuración de Jackson lo cambia.
- **Alternativa descartada:** `Instant` en UTC y que el front convierta. El criterio de salida pide la hora local, y la agrupación por día en el front sería más frágil.

### D4. Parámetros
- `vista` llega como `String` y se convierte sin distinguir mayúsculas (`dia` o `semana`). Un valor desconocido lanza `ResponseStatusException(BAD_REQUEST)`, que `ManejadorErrores` (por `ResponseEntityExceptionHandler`) devuelve como Problem Details `400`. No se usa la conversión automática a enum porque distingue mayúsculas y obligaría a enviar `DIA`.
- `fecha` llega como `@DateTimeFormat(iso = DATE) LocalDate`; un formato inválido da `400` por conversión de tipo.
- `profesionalId` y `sillonId` son `Long` opcionales. No se comprueba que existan: un id sin datos devuelve listas vacías. Validarlo con `ServicioProfesionales.detalle` le daría 403 al odontólogo con otros profesionales, contra la lectura global.

### D5. Turnos
- `AgendaLecturaJpaRepository.enPeriodo(inicio, fin, profesionalId, sillonId)` usa JPQL con filtros nulos opcionales, todos los estados y `order by t.inicio, t.id`.
- Los pacientes se resuelven en una sola llamada con `PacienteConsulta.porIds(Set<Long>)`, un método nuevo que devuelve un `Map<Long, Paciente>`. `PacienteRepositorio` y su adaptador suman `porIds` (JPQL `where id in :ids`).

### D6. Horarios y bloqueos
- `HorarioConsulta` suma `List<HorarioDatos> tramos(Long profesionalId)`, donde `null` significa todos los profesionales.
- `BloqueoConsulta` suma `List<BloqueoDatos> queSolapan(Long profesionalId, Intervalo intervalo)`, con el mismo criterio para `null`.
- Los implementan `HorariosPersistencia` y `BloqueosPersistencia`, con métodos nuevos en sus repositorios JPA.
- `ProyeccionHorarios.proyectar(tramos, periodo)` recorre cada fecha del período y, para cada tramo cuyo `diaSemana` coincide, arma `fecha`, `inicio` (`fecha.atTime(horaInicio).atZone(BA)`) y `fin`. Se ordena por fecha, profesional e inicio.
- `sillonId` no filtra horarios ni bloqueos, porque pertenecen al profesional.

### D7. Visibilidad por rol
- `ConsultaAgenda` resuelve quién mira. Con `ROLE_ADMIN` o `ROLE_RECEPCION` hay lectura completa. Si no, toma el profesional propio con `ProfesionalVinculadoConsulta`; sin vínculo, lanza `AccessDeniedException` (403).
- Para cada turno o bloqueo, los datos sensibles (`paciente`, `notas` y `motivo`) se completan si la lectura es completa o si `profesionalId` es el propio. Si no, quedan `null`.
- `pacienteId` y los horarios siempre se envían, porque la lectura de la agenda es global (matriz RBAC). Así lo decidió Valentino el 2026-11-25: los bloqueos ajenos van sin motivo.
- Los nombres de los pacientes ajenos ni siquiera se buscan: `porIds` recibe solo los ids visibles.

### D8. Ficha básica para el odontólogo
- `pacientes.application` define el puerto `TurnosConPacienteConsulta { boolean existeTurno(Long pacienteId, Long profesionalId); }`. Lo implementa `agenda.infrastructure.AgendaLecturaPersistencia` con un `exists` sobre `turno` que usa el índice `(paciente_id, inicio)`, en cualquier estado.
- Así `pacientes` no depende de `agenda`: la dependencia queda invertida, como pasa con `ProfesionalVinculadoConsulta`.
- `GET /api/pacientes/{id}` pasa a `hasAnyRole('RECEPCION','ADMIN','ODONTOLOGO')`. `PacienteServicio.obtener` primero autoriza: si quien llama es odontólogo, resuelve su profesional y pide `existeTurno`. Si no hay vínculo o no hay turno, lanza `AccessDeniedException` antes de buscar el paciente. Por eso un id inexistente también da 403 y no revela si el paciente existe.
- Los demás métodos del controller no cambian, así que el odontólogo sigue recibiendo 403 al buscar, crear, editar o dar de baja.
- Los puertos se inyectan como `Optional<>`, igual que `vinculos` en `ServicioProfesionales`, para que los slices de test que no cargan `agenda` sigan arrancando.

### D9. Escrituras del odontólogo: regla general y prueba transversal
- `SecurityConfig` suma, después de todas las reglas específicas y antes de `anyRequest()`, cuatro matchers: `POST`, `PUT`, `PATCH` y `DELETE` sobre `/api/**`, con `hasAnyRole("ADMIN", "RECEPCION")`.
- Las reglas anteriores (`RUTAS_PUBLICAS`, login y las más estrictas de ADMIN) siguen ganando por orden. Logout lo atiende `LogoutFilter` antes de la autorización.
- Las reglas solo-ADMIN existentes, `@PreAuthorize` incluido, siguen aplicando a `RECEPCION`.
- `EscriturasOdontologoIT` obtiene todos los mappings de `RequestMappingHandlerMapping`. Toma los de `POST`, `PUT`, `PATCH` o `DELETE` cuyo patrón empieza por `/api/` y no es login, logout ni `/api/public/**`. Reemplaza cada variable de ruta por `1` y envía un cuerpo JSON vacío (`{}`), con un `ODONTOLOGO` autenticado y un CSRF válido (`csrfValido()`). Afirma `403` para cada uno, y además que la lista no está vacía e incluye `POST /api/turnos`, para que el test no pase vacío.
- **Alternativa descartada:** listar los endpoints a mano en el test. Un endpoint nuevo quedaría sin cubrir.
- **Ajuste durante el apply:**
  - Sin la regla general, el test transversal encontró 9 endpoints que le respondían 400 al odontólogo: `POST` y `PUT` de pacientes, prestaciones, profesionales, sillones y prestaciones por profesional.
  - Con la regla, el test de C-03 «Una operación que requiere CSRF rechaza tokens faltantes o inválidos» (`SesionPersonalIT`) empezó a fallar. Usaba un `ODONTOLOGO` para probar una escritura con CSRF válido y esperaba 200.
  - Como ese test verifica CSRF y no permisos por rol, ahora usa un usuario `RECEPCION`.
  - El fallo de «Los registros no contienen credenciales ni cookies» era consecuencia del anterior: MockMvc imprimía el request del test fallido y la salida capturada contenía la contraseña de prueba.

### D10. Tests
- **Unitarios sin Spring:**
  - `PeriodoAgendaTest`: día, semana con `fecha` en lunes y en domingo, y bordes de los instantes.
  - `ProyeccionHorariosTest`.
- **`AgendaApiIT`** (`PruebaIntegracionBase` con `RelojFijoConfig` en 2026-12-01 y `@Transactional`):
  - Arma profesionales, sillones, prestaciones, pacientes, turnos, horarios y bloqueos ficticios por JDBC, como hacen los IT de C-19.
  - Para el odontólogo vinculado, inserta un `usuario` ficticio con `profesional_id` y se autentica con `IdentidadPersonal`. Así `VinculoProfesionalUsuario` lo resuelve sin declarar un bean propio, que crearía otro contexto de Spring.
- **`PacienteFichaOdontologoIT`:** ficha propia (200), ficha ajena (403), id inexistente (403) y búsqueda (403).
- **`EscriturasOdontologoIT`:** la prueba transversal de D9.
- Cada escenario lleva su `@DisplayName` exacto.

## UI Mockups

- Decision: no (no UI), 2026-11-25. C-28 es solo backend; la pantalla llega en C-40.

## Risks / Trade-offs

- **[La regla general de escrituras también afecta a endpoints futuros]** → Es lo buscado (RN-33). Si algún día el odontólogo necesita escribir, como cambiar su propia contraseña, se agrega una regla específica antes de la general.
- **[`SecurityConfig`, `PacienteController` y los puertos de `profesionales` y `pacientes` se tocan desde agenda]** → Los cambios son aditivos: métodos nuevos y un rol más en un `@PreAuthorize`. Si hay conflicto de merge, se conservan los dos bloques.
- **[Desfase `-03:00` en vez de `Z`]** → Lo consume solo C-40. Queda en el spec y en un test.
- **[El odontólogo ve `pacienteId` de turnos ajenos]** → No puede abrir esas fichas (D8: 403), así que el id solo indica que hay un turno.
- **[Semana con muchos turnos]** → Para 2 a 5 profesionales son a lo sumo unos cientos de filas. Las consultas usan los índices existentes y hay una sola consulta de pacientes.

## Migration Plan

- No hay migración: no cambia el esquema.
- Para volver atrás alcanza con revertir el commit. La regla general de escrituras se puede quitar sola si causara problemas.

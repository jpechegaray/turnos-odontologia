# Proposal

## Why

Desde C-19 los turnos se crean, pero no hay forma de consultarlos. El odontólogo necesita ver su agenda del día y de la semana para saber a quién atiende y cuándo. Recepción necesita la agenda general, por profesional y por sillón, para coordinar (US-013). Este change entrega el modelo de lectura que va a usar el front de agenda (C-40). También deja probado, de forma transversal, que el odontólogo es solo lectura (RN-33).

## What Changes

- **`GET /api/agenda`**, accesible para `ADMIN`, `RECEPCION` y `ODONTOLOGO`. Recibe `vista` (`dia` o `semana`, por defecto `dia`), `fecha` (`AAAA-MM-DD`, por defecto hoy en Buenos Aires) y los filtros opcionales `profesionalId` y `sillonId`. Devuelve:
  - **Turnos** de cualquier estado, con profesional, sillón, prestación y paciente.
  - **Horarios de atención**, llevados a cada fecha del período.
  - **Bloqueos** que tocan el período.
- **Período y hora local.**
  - El día va de 00:00 a 24:00 en hora de Buenos Aires. La semana va de lunes a domingo e incluye `fecha`.
  - Los instantes salen en ISO-8601 con el desfase de Buenos Aires (RN-14).
  - Un turno que empieza a las 23:30 hora local pertenece a ese día, aunque en UTC caiga al día siguiente.
- **Lectura global con datos según el rol.** Todos los roles ven los turnos, horarios y bloqueos de todos los profesionales. Lo que cambia es qué datos llegan:
  - `ADMIN` y `RECEPCION` ven el nombre y apellido del paciente, las notas del turno y el motivo de cada bloqueo.
  - `ODONTOLOGO` ve esos datos solo en los turnos y bloqueos de su propio profesional. En los de otros profesionales recibe `pacienteId` sin nombre ni notas, y bloqueos sin motivo.
  - Un odontólogo sin profesional vinculado recibe `403`.
- **Ficha básica para el odontólogo.** `GET /api/pacientes/{id}` pasa a admitir a `ODONTOLOGO`, pero solo para pacientes con algún turno de su profesional. Para cualquier otro paciente recibe `403`, y sigue sin poder buscar, crear, editar ni dar de baja fichas.
- **Prueba transversal de RN-33.** Un test recorre todos los endpoints de escritura registrados (POST, PUT, PATCH y DELETE), salvo login, logout y la zona pública, y comprueba que un `ODONTOLOGO` recibe `403` en cada uno. Así un endpoint nuevo queda cubierto sin tener que acordarse de agregarlo.
- **Consultas nuevas.** La consulta de la agenda va en archivos nuevos: no se modifica `TurnoJpaRepository`.

## Capabilities

### New Capabilities
- `consulta-agenda`: consulta de la agenda diaria y semanal, con turnos, horarios de atención y bloqueos en hora de Buenos Aires, filtros por profesional y sillón, y visibilidad de datos según el rol.

### Modified Capabilities
- `gestion-pacientes`: el requisito «Permisos por rol en la ficha de paciente» cambia para que el odontólogo pueda consultar la ficha de los pacientes de sus turnos. Para todo lo demás sigue recibiendo `403`.

## Impact

- **Backend**:
  - `agenda`:
    - Caso de uso de consulta.
    - Repositorio JPA de lectura nuevo sobre la tabla `turno`.
    - Controller `GET /api/agenda` y sus DTO.
    - Implementación del puerto de acceso a fichas.
  - `profesionales`: `HorarioConsulta` y `BloqueoConsulta` suman métodos de listado para varios profesionales y un rango.
  - `pacientes`:
    - `PacienteConsulta` suma `porIds`, para resolver nombres sin N+1.
    - Puerto nuevo `TurnosConPacienteConsulta` (lo implementa `agenda`, sin invertir la dependencia entre módulos).
    - `PacienteController` y su servicio admiten al odontólogo en la consulta por id.
  - `seguridad`: `SecurityConfig` suma, después de las reglas específicas, una regla general: `POST`, `PUT`, `PATCH` y `DELETE` sobre `/api/**` solo para `ADMIN` y `RECEPCION`. Así el odontólogo recibe `403` antes de que se valide el cuerpo, en los endpoints actuales y en los que vengan. Login, logout y `/api/public/**` quedan afuera. `GET /api/agenda` ya queda autenticado por `anyRequest().authenticated()`.
- **API**:
  - Un endpoint nuevo, de solo lectura.
  - `GET /api/pacientes/{id}` amplía su acceso. No cambia el formato de su respuesta.
- **Base de datos**: sin cambios de esquema. Los índices de `turno` por profesional, sillón e inicio que creó C-19 sirven para la consulta.
- **Tests**: integración con PostgreSQL real (Testcontainers) para vistas, bordes de medianoche, filtros, visibilidad por rol, ficha del odontólogo y la prueba transversal de escrituras.
- **Dependencias**: C-19, ya mergeado.
- **Fuera de alcance**:
  - La pantalla de agenda (C-40).
  - Los horarios libres sugeridos (C-26).
  - Cancelar, reprogramar o marcar asistencia (C-27 y C-29).

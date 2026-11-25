# Spec Delta

## MODIFIED Requirements

### Requirement: Permisos por rol en la ficha de paciente

La API SHALL permitir crear, editar, consultar y buscar fichas a los roles `RECEPCION` y `ADMIN`, y dar de baja solo a `ADMIN`. El rol `ODONTOLOGO` SHALL consultar por id solo la ficha de un paciente con algún turno de su profesional vinculado, en cualquier estado (RN-33). En cualquier otra operación de este módulo, o con otro paciente, MUST recibir 403. Una petición anónima MUST recibir 401.

#### Scenario: Recepción crea, edita y busca fichas

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente y luego lo edita y lo busca
- **THEN** las tres operaciones responden con estado 2xx

#### Scenario: Recepción no puede dar de baja una ficha

- **GIVEN** una recepcionista autenticada y un paciente activo
- **WHEN** intenta dar de baja la ficha
- **THEN** la respuesta tiene estado 403 y formato Problem Details
- **AND** la ficha sigue activa

#### Scenario: Administrador da de baja una ficha

- **GIVEN** un administrador autenticado y un paciente activo
- **WHEN** da de baja la ficha
- **THEN** la respuesta tiene estado 204

#### Scenario: El odontólogo no accede a las fichas

- **GIVEN** un odontólogo autenticado y un paciente sin turnos con su profesional
- **WHEN** intenta consultar esa ficha, buscar fichas o crear una
- **THEN** cada operación responde con estado 403 y formato Problem Details

#### Scenario: El odontólogo consulta la ficha de un paciente de sus turnos

- **GIVEN** un odontólogo vinculado a un profesional y un paciente con un turno de ese profesional
- **WHEN** consulta `GET /api/pacientes/{id}` de ese paciente
- **THEN** la respuesta tiene estado 200 con la ficha del paciente

#### Scenario: El odontólogo no consulta la ficha de un paciente de otro profesional

- **GIVEN** un odontólogo vinculado a un profesional y un paciente que solo tiene turnos con otro profesional
- **WHEN** consulta `GET /api/pacientes/{id}` de ese paciente
- **THEN** la respuesta tiene estado 403 y formato Problem Details, sin datos del paciente

#### Scenario: Una petición anónima recibe 401

- **GIVEN** un cliente sin sesión
- **WHEN** intenta consultar una ficha
- **THEN** la respuesta tiene estado 401 y formato Problem Details

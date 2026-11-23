# Spec Delta

## Purpose

Define la ficha básica de paciente que usa el consultorio: qué datos guarda, cómo se busca, cómo se da de baja y quién puede operarla. Es la base que consumen la agenda, la lista de espera y la reserva pública para asignar turnos sin duplicar pacientes.

## ADDED Requirements

### Requirement: Ficha de paciente con datos mínimos

La API SHALL crear fichas de paciente con nombre y apellido obligatorios, y permitir email, teléfono, DNI, obra social, número de afiliado y notas. La obra social MUST ser texto libre, sin validación ni cálculo de cobertura. La respuesta MUST incluir el identificador de la ficha y su estado activo.

#### Scenario: Alta con email y sin teléfono

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente con nombre, apellido y email ficticio, sin teléfono
- **THEN** la respuesta tiene estado 201
- **AND** el cuerpo devuelve un identificador y `activo` igual a `true`

#### Scenario: Alta con teléfono y sin email

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente con nombre, apellido y teléfono ficticio, sin email
- **THEN** la respuesta tiene estado 201
- **AND** el cuerpo devuelve el teléfono informado

#### Scenario: La obra social se guarda como texto libre

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente con obra social `Obra Social Ficticia Nivel A`
- **THEN** la respuesta tiene estado 201
- **AND** el cuerpo devuelve la obra social exactamente como se informó

#### Scenario: Alta sin nombre o apellido se rechaza

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente sin apellido
- **THEN** la respuesta tiene estado 400 y formato Problem Details
- **AND** el cuerpo lista el campo `apellido` como inválido

### Requirement: Medio de contacto obligatorio

Toda ficha SHALL tener al menos un medio de contacto: email o teléfono. Una petición que intente crear o editar una ficha sin ninguno de los dos MUST rechazarse con estado 422, formato Problem Details y `codigo` igual a `RN-32`.

#### Scenario: Alta sin email ni teléfono se rechaza

- **GIVEN** una recepcionista autenticada
- **WHEN** crea un paciente con nombre y apellido pero sin email ni teléfono
- **THEN** la respuesta tiene estado 422 y formato Problem Details
- **AND** el cuerpo incluye `codigo` igual a `RN-32`

#### Scenario: Edición que quita el único medio de contacto se rechaza

- **GIVEN** un paciente activo cuyo único medio de contacto es el email
- **WHEN** una recepcionista edita la ficha y deja email y teléfono vacíos
- **THEN** la respuesta tiene estado 422 y formato Problem Details
- **AND** la ficha conserva el email original

#### Scenario: Edición que reemplaza email por teléfono se acepta

- **GIVEN** un paciente activo cuyo único medio de contacto es el email
- **WHEN** una recepcionista edita la ficha, borra el email e informa un teléfono
- **THEN** la respuesta tiene estado 200
- **AND** la ficha queda con el teléfono y sin email

### Requirement: DNI opcional y único

El DNI SHALL ser opcional. Cuando se informa, MUST ser único entre todas las fichas, incluidas las inactivas, y un DNI repetido MUST rechazarse con estado 409, formato Problem Details y `codigo` igual a `RN-32`. Un DNI vacío o con solo espacios MUST tratarse como no informado.

#### Scenario: Un DNI repetido devuelve 409

- **GIVEN** un paciente existente con DNI ficticio `00000001`
- **WHEN** una recepcionista crea otro paciente con el mismo DNI
- **THEN** la respuesta tiene estado 409 y formato Problem Details
- **AND** el cuerpo incluye `codigo` igual a `RN-32`

#### Scenario: Varios pacientes pueden no tener DNI

- **GIVEN** dos pacientes creados sin DNI
- **WHEN** una recepcionista crea un tercer paciente sin DNI
- **THEN** la respuesta tiene estado 201

#### Scenario: El DNI de una ficha inactiva sigue ocupado

- **GIVEN** un paciente dado de baja lógicamente con DNI ficticio `00000002`
- **WHEN** una recepcionista intenta crear otro paciente con ese DNI
- **THEN** la respuesta tiene estado 409 y formato Problem Details

#### Scenario: Un DNI en blanco se trata como no informado

- **GIVEN** un paciente existente sin DNI
- **WHEN** una recepcionista crea otro paciente con DNI vacío
- **THEN** la respuesta tiene estado 201

### Requirement: Búsqueda de pacientes

La API SHALL buscar pacientes por apellido, nombre, DNI o email mediante filtros opcionales y combinables. Los filtros de apellido, nombre y email MUST ignorar mayúsculas y minúsculas; apellido y nombre MUST admitir coincidencia parcial; el DNI MUST coincidir de forma exacta. La búsqueda MUST devolver solo fichas activas y MUST devolver una lista vacía cuando no hay coincidencias.

#### Scenario: Búsqueda por apellido sin distinguir mayúsculas

- **GIVEN** un paciente activo de apellido `Gomez`
- **WHEN** una recepcionista busca con el filtro `apellido=gomez`
- **THEN** la respuesta tiene estado 200 e incluye esa ficha

#### Scenario: Búsqueda por nombre con coincidencia parcial

- **GIVEN** un paciente activo de nombre `Mariana`
- **WHEN** una recepcionista busca con el filtro `nombre=ana`
- **THEN** la respuesta tiene estado 200 e incluye esa ficha

#### Scenario: Búsqueda por DNI exacto

- **GIVEN** un paciente activo con DNI ficticio `00000003`
- **WHEN** una recepcionista busca con el filtro `dni=00000003`
- **THEN** la respuesta tiene estado 200 e incluye solo las fichas con ese DNI

#### Scenario: Búsqueda por email sin distinguir mayúsculas

- **GIVEN** un paciente activo con email `paciente1@example.test`
- **WHEN** una recepcionista busca con el filtro `email=PACIENTE1@EXAMPLE.TEST`
- **THEN** la respuesta tiene estado 200 e incluye esa ficha

#### Scenario: Los filtros se combinan

- **GIVEN** dos pacientes activos de apellido `Gomez` con nombres distintos
- **WHEN** una recepcionista busca con `apellido=Gomez` y `nombre=Ana`
- **THEN** la respuesta tiene estado 200 e incluye solo la ficha que cumple ambos filtros

#### Scenario: Una búsqueda sin filtros devuelve las fichas activas

- **GIVEN** dos pacientes activos y uno dado de baja lógicamente
- **WHEN** una recepcionista busca sin filtros
- **THEN** la respuesta tiene estado 200 e incluye solo las dos fichas activas

#### Scenario: Una búsqueda sin coincidencias devuelve una lista vacía

- **GIVEN** ningún paciente con el DNI ficticio `99999999`
- **WHEN** una recepcionista busca con el filtro `dni=99999999`
- **THEN** la respuesta tiene estado 200
- **AND** el cuerpo es una lista vacía

#### Scenario: Un paciente inactivo no aparece en la búsqueda

- **GIVEN** un paciente de apellido `Gomez` dado de baja lógicamente
- **WHEN** una recepcionista busca con el filtro `apellido=Gomez`
- **THEN** la respuesta tiene estado 200
- **AND** la lista no incluye esa ficha

### Requirement: Baja lógica de la ficha

La API SHALL dar de baja una ficha marcándola como inactiva, sin eliminar la fila. La ficha inactiva MUST dejar de aparecer en la búsqueda y MUST seguir siendo consultable por identificador con su estado inactivo. Ninguna operación SHALL eliminar físicamente una ficha.

#### Scenario: La baja deja la ficha inactiva y consultable

- **GIVEN** un paciente activo y un administrador autenticado
- **WHEN** el administrador da de baja la ficha
- **THEN** la respuesta tiene estado 204
- **AND** la consulta por identificador devuelve la misma ficha con `activo` igual a `false`

#### Scenario: Una ficha dada de baja no se puede consultar como activa

- **GIVEN** un paciente dado de baja lógicamente
- **WHEN** una recepcionista consulta la ficha por identificador
- **THEN** la respuesta tiene estado 200
- **AND** el cuerpo informa `activo` igual a `false`

#### Scenario: Dar de baja una ficha inexistente devuelve 404

- **GIVEN** un administrador autenticado y un identificador que no existe
- **WHEN** el administrador intenta dar de baja esa ficha
- **THEN** la respuesta tiene estado 404 y formato Problem Details

### Requirement: Permisos por rol en la ficha de paciente

La API SHALL permitir crear, editar, consultar y buscar fichas a los roles `RECEPCION` y `ADMIN`, y dar de baja solo a `ADMIN`. El rol `ODONTOLOGO` MUST recibir 403 en todas las operaciones de este módulo, y una petición anónima MUST recibir 401.

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

- **GIVEN** un odontólogo autenticado
- **WHEN** intenta consultar, buscar o crear una ficha
- **THEN** cada operación responde con estado 403 y formato Problem Details

#### Scenario: Una petición anónima recibe 401

- **GIVEN** un cliente sin sesión
- **WHEN** intenta consultar una ficha
- **THEN** la respuesta tiene estado 401 y formato Problem Details

### Requirement: Consulta de ficha para otros módulos

El módulo SHALL ofrecer una consulta de solo lectura por identificador y por email que devuelva la ficha con su estado activo, para que la agenda, la lista de espera y la reserva pública la usen sin acceder a la persistencia. La consulta por email MUST ignorar mayúsculas y minúsculas y MUST devolver vacío cuando no existe una ficha.

#### Scenario: La consulta por identificador devuelve la ficha

- **GIVEN** un paciente activo con identificador conocido
- **WHEN** otro módulo consulta la ficha por ese identificador
- **THEN** obtiene sus datos y `activo` igual a `true`

#### Scenario: La consulta por email ignora mayúsculas

- **GIVEN** un paciente activo con email `paciente2@example.test`
- **WHEN** otro módulo consulta por `PACIENTE2@EXAMPLE.TEST`
- **THEN** obtiene la ficha de ese paciente

#### Scenario: La consulta por identificador devuelve también una ficha inactiva

- **GIVEN** un paciente dado de baja lógicamente
- **WHEN** otro módulo consulta la ficha por su identificador
- **THEN** obtiene la ficha con `activo` igual a `false`

#### Scenario: La consulta de una ficha inexistente devuelve vacío

- **GIVEN** un identificador o un email que no corresponde a ninguna ficha
- **WHEN** otro módulo consulta la ficha
- **THEN** la consulta devuelve vacío

### Requirement: Errores sin datos de otros pacientes

Los mensajes de error del módulo MUST NOT incluir nombre, apellido, DNI, email, teléfono ni notas de otra ficha (RN-35). Un conflicto de DNI MUST informar solo que el DNI ya está registrado.

#### Scenario: Un DNI duplicado no expone los datos del otro paciente

- **GIVEN** un paciente existente con DNI ficticio `00000001`, email y teléfono cargados
- **WHEN** una recepcionista intenta crear otro paciente con ese DNI
- **THEN** la respuesta 409 incluye `codigo` igual a `RN-32`
- **AND** el cuerpo no contiene el nombre, el apellido, el email ni el teléfono del paciente existente
